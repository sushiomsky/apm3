package com.example.proxmark.repository

import com.example.proxmark.features.FeatureExecutionState
import com.example.proxmark.features.FeatureRunOutcome
import com.example.proxmark.features.FeatureRunRecord
import com.example.proxmark.features.ProxmarkFeature
import com.example.proxmark.persistence.AppSettings
import com.example.proxmark.persistence.SettingsStore
import com.example.proxmark.persistence.TagStore
import com.example.proxmark.protocol.ProxmarkClient
import com.example.proxmark.protocol.ProxmarkOutputParser
import com.example.proxmark.protocol.TerminalLineType
import com.example.proxmark.reader.AcrUsbReader
import com.example.proxmark.reader.BuiltInNfcReader
import com.example.proxmark.reader.Proxmark3Reader
import com.example.proxmark.reader.ReaderConnectionState
import com.example.proxmark.reader.ReaderType
import com.example.proxmark.usb.UsbConnectionState
import java.io.IOException
import java.net.SocketTimeoutException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class ProxmarkRepository(
    private val proxmarkClient: ProxmarkClient,
    private val scope: CoroutineScope,
    private val tagStore: TagStore,
    private val settingsStore: SettingsStore,
    private val proxmark3Reader: Proxmark3Reader,
    private val builtInNfcReader: BuiltInNfcReader,
    private val acrUsbReader: AcrUsbReader
) {

    private val _lastScan = MutableStateFlow<TagInfo?>(null)
    val lastScan: StateFlow<TagInfo?> = _lastScan.asStateFlow()

    private val _savedTags = MutableStateFlow<List<TagInfo>>(emptyList())
    val savedTags: StateFlow<List<TagInfo>> = _savedTags.asStateFlow()

    private val _tagGroups = MutableStateFlow<List<TagGroup>>(emptyList())
    val tagGroups: StateFlow<List<TagGroup>> = _tagGroups.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _featureExecutionState = MutableStateFlow<FeatureExecutionState>(FeatureExecutionState.Idle)
    val featureExecutionState: StateFlow<FeatureExecutionState> = _featureExecutionState.asStateFlow()

    private val _featureRunHistory = MutableStateFlow<List<FeatureRunRecord>>(emptyList())
    val featureRunHistory: StateFlow<List<FeatureRunRecord>> = _featureRunHistory.asStateFlow()

    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    private val _autoScanEnabled = MutableStateFlow(false)
    val autoScanEnabled: StateFlow<Boolean> = _autoScanEnabled.asStateFlow()

    private val _activeReaderType = MutableStateFlow(ReaderType.PROXMARK3)
    val activeReaderType: StateFlow<ReaderType> = _activeReaderType.asStateFlow()

    private val _readerStates = MutableStateFlow(
        mapOf(
            ReaderType.PROXMARK3 to ReaderConnectionState.Disconnected as ReaderConnectionState,
            ReaderType.BUILTIN_NFC to ReaderConnectionState.Disconnected as ReaderConnectionState,
            ReaderType.ACR_USB to ReaderConnectionState.Disconnected as ReaderConnectionState
        )
    )
    val readerStates: StateFlow<Map<ReaderType, ReaderConnectionState>> = _readerStates.asStateFlow()

    val connectionState: StateFlow<UsbConnectionState> = proxmarkClient.connectionState

    val terminalLines = proxmarkClient.terminalLines

    private val _mifareKeyState = MutableStateFlow(com.example.proxmark.mifare.MifareKeyState())
    val mifareKeyState: StateFlow<com.example.proxmark.mifare.MifareKeyState> = _mifareKeyState.asStateFlow()

    private val parserWindow = ArrayDeque<String>()
    private val featureMutex = Mutex()
    private var autoScanJob: Job? = null

    init {
        // Collect PM3 terminal lines for tag parsing + key recovery
        scope.launch {
            proxmarkClient.terminalLines.collect { line ->
                if (line.type == TerminalLineType.ERROR) {
                    _errorMessage.value = line.text
                }
                if (line.type == TerminalLineType.RECEIVED) {
                    updateScanState(line.text)
                    parseKeyRecoveryLine(line.text)
                }
            }
        }

        // Load persisted tags
        scope.launch {
            val persisted = tagStore.savedTags.first()
            if (persisted.isNotEmpty()) {
                _savedTags.value = persisted
                _lastScan.value = persisted.firstOrNull()
            }
        }

        // Load persisted tag groups
        scope.launch {
            val groups = tagStore.tagGroups.first()
            if (groups.isNotEmpty()) {
                _tagGroups.value = groups
            }
        }

        // Collect settings
        scope.launch {
            settingsStore.settings.collect { s ->
                _settings.value = s
            }
        }

        // Mirror PM3 USB state into reader states and Proxmark3Reader
        scope.launch {
            proxmarkClient.connectionState.collect { usbState ->
                proxmark3Reader.syncConnectionState(usbState)
                _readerStates.update { map ->
                    map + (ReaderType.PROXMARK3 to proxmark3Reader.connectionState.value)
                }
            }
        }

        // Mirror NFC reader state
        scope.launch {
            builtInNfcReader.connectionState.collect { state ->
                _readerStates.update { map -> map + (ReaderType.BUILTIN_NFC to state) }
            }
        }

        // Mirror NFC tag discoveries into the tag pipeline
        scope.launch {
            builtInNfcReader.lastDiscoveredTag.collect { tag ->
                if (tag != null && _activeReaderType.value == ReaderType.BUILTIN_NFC) {
                    addDiscoveredTag(tag)
                }
            }
        }

        // Mirror ACR reader state
        scope.launch {
            acrUsbReader.connectionState.collect { state ->
                _readerStates.update { map -> map + (ReaderType.ACR_USB to state) }
            }
        }

        // Auto-scan: restart loop whenever enabled/connected/interval changes
        scope.launch {
            combine(
                _autoScanEnabled,
                connectionState,
                settingsStore.settings
            ) { enabled, connState, settings ->
                Triple(enabled, connState is UsbConnectionState.Connected, settings.autoScanIntervalMs.toLong())
            }.collect { (enabled, connected, intervalMs) ->
                autoScanJob?.cancel()
                if (enabled && connected && _activeReaderType.value == ReaderType.PROXMARK3) {
                    autoScanJob = scope.launch {
                        while (true) {
                            try {
                                featureMutex.withLock {
                                    proxmarkClient.executeCommand("hf search", timeoutMs = _settings.value.commandTimeoutMs.toLong())
                                }
                            } catch (_: CancellationException) {
                                break
                            } catch (_: Exception) {
                                // scan failed, keep retrying
                            }
                            delay(intervalMs)
                        }
                    }
                } else if (enabled && !connected) {
                    _autoScanEnabled.value = false
                }
            }
        }
    }

    fun start() {
        proxmark3Reader.start()
    }

    fun connect() {
        when (_activeReaderType.value) {
            ReaderType.PROXMARK3 -> proxmarkClient.connect()
            ReaderType.BUILTIN_NFC -> builtInNfcReader.connect()
            ReaderType.ACR_USB -> acrUsbReader.connect()
        }
    }

    fun reconnect() {
        when (_activeReaderType.value) {
            ReaderType.PROXMARK3 -> proxmarkClient.reconnect()
            ReaderType.BUILTIN_NFC -> {
                builtInNfcReader.disconnect()
                builtInNfcReader.connect()
            }
            ReaderType.ACR_USB -> {
                acrUsbReader.disconnect()
                acrUsbReader.connect()
            }
        }
    }

    fun disconnect() {
        when (_activeReaderType.value) {
            ReaderType.PROXMARK3 -> proxmarkClient.disconnect()
            ReaderType.BUILTIN_NFC -> builtInNfcReader.disconnect()
            ReaderType.ACR_USB -> acrUsbReader.disconnect()
        }
    }

    fun setActiveReader(type: ReaderType) {
        _activeReaderType.value = type
    }

    fun clearError() {
        _errorMessage.value = null
    }

    fun clearSavedTags() {
        _savedTags.value = emptyList()
        scope.launch { tagStore.saveTags(emptyList()) }
    }

    fun updateTimeout(timeoutMs: Int) {
        scope.launch { settingsStore.updateTimeout(timeoutMs) }
    }

    fun toggleFavorite(featureId: String) {
        scope.launch { settingsStore.toggleFavorite(featureId) }
    }

    fun updateThemeMode(mode: com.example.proxmark.persistence.ThemeMode) {
        scope.launch { settingsStore.updateThemeMode(mode) }
    }

    fun updateAutoScanInterval(intervalMs: Int) {
        scope.launch { settingsStore.updateAutoScanInterval(intervalMs) }
    }

    fun setAutoScanEnabled(enabled: Boolean) {
        _autoScanEnabled.value = enabled
    }

    fun updateTagNote(uid: String, type: String, note: String?) {
        _savedTags.update { tags ->
            val updated = tags.map { tag ->
                if (tag.uid == uid && tag.type == type) tag.copy(note = note) else tag
            }
            scope.launch { tagStore.saveTags(updated) }
            updated
        }
        _lastScan.update { scan ->
            if (scan != null && scan.uid == uid && scan.type == type) scan.copy(note = note) else scan
        }
    }

    /**
     * Run a feature, optionally with user-supplied parameter values.
     * NFC features (command starts with "nfc:") route to the built-in NFC reader.
     * PM3 features route to the Proxmark3 client.
     */
    fun runFeature(feature: ProxmarkFeature, paramValues: Map<String, String> = emptyMap()) {
        // Interpolate parameters into the command template
        var command = feature.command
        for ((key, value) in paramValues) {
            command = command.replace("{$key}", value)
        }

        // Route NFC features to built-in reader
        if (command.startsWith("nfc:")) {
            runNfcFeature(feature, command)
            return
        }

        // PM3 features require Proxmark3 reader
        val activeReader = _activeReaderType.value
        if (activeReader != ReaderType.PROXMARK3) {
            _featureExecutionState.value = FeatureExecutionState.Failed(
                featureId = feature.id,
                title = feature.title,
                message = "PM3 commands require Proxmark3 reader. Active reader: ${activeReader.label}"
            )
            return
        }

        if (connectionState.value !is UsbConnectionState.Connected) {
            val message = "Cannot run ${feature.title}: device not connected"
            _featureExecutionState.value = FeatureExecutionState.Failed(
                featureId = feature.id, title = feature.title, message = message
            )
            _errorMessage.value = message
            appendRunRecord(FeatureRunRecord(
                featureId = feature.id, title = feature.title,
                outcome = FeatureRunOutcome.FAILED, detail = message
            ))
            return
        }

        _featureExecutionState.value = FeatureExecutionState.Running(
            featureId = feature.id, title = feature.title
        )
        android.util.Log.d("ProxmarkRepository", "runFeature() - launching coroutine for: ${feature.id}, command: '$command'")
        scope.launch {
            android.util.Log.d("ProxmarkRepository", "runFeature() - inside coroutine, acquiring mutex")
            featureMutex.withLock {
                val timeoutMs = _settings.value.commandTimeoutMs.toLong()
                android.util.Log.d("ProxmarkRepository", "runFeature() - mutex acquired, calling executeCommand with timeout=${timeoutMs}ms")
                try {
                    val output = proxmarkClient.executeCommand(command, timeoutMs = timeoutMs)
                    val trimmedOutput = output.ifBlank { null }
                    _featureExecutionState.value = FeatureExecutionState.Success(
                        featureId = feature.id, title = feature.title, output = trimmedOutput
                    )
                    appendRunRecord(FeatureRunRecord(
                        featureId = feature.id, title = feature.title,
                        outcome = FeatureRunOutcome.SUCCESS, detail = trimmedOutput ?: "Command completed"
                    ))
                } catch (_: SocketTimeoutException) {
                    val message = "${feature.title} timed out waiting for response"
                    _featureExecutionState.value = FeatureExecutionState.Failed(
                        featureId = feature.id, title = feature.title, message = message
                    )
                    _errorMessage.value = message
                    appendRunRecord(FeatureRunRecord(
                        featureId = feature.id, title = feature.title,
                        outcome = FeatureRunOutcome.FAILED, detail = message
                    ))
                } catch (io: IOException) {
                    val message = "${feature.title} failed: ${io.message ?: "I/O error"}"
                    _featureExecutionState.value = FeatureExecutionState.Failed(
                        featureId = feature.id, title = feature.title, message = message
                    )
                    _errorMessage.value = message
                    appendRunRecord(FeatureRunRecord(
                        featureId = feature.id, title = feature.title,
                        outcome = FeatureRunOutcome.FAILED, detail = message
                    ))
                }
            }
        }
    }

    /** Execute NFC-specific commands via built-in NFC reader. */
    private fun runNfcFeature(feature: ProxmarkFeature, command: String) {
        if (builtInNfcReader.connectionState.value !is ReaderConnectionState.Connected) {
            val msg = "NFC reader not active. Enable NFC and select Built-in NFC reader."
            _featureExecutionState.value = FeatureExecutionState.Failed(
                featureId = feature.id, title = feature.title, message = msg
            )
            _errorMessage.value = msg
            return
        }

        _featureExecutionState.value = FeatureExecutionState.Running(
            featureId = feature.id, title = feature.title
        )

        scope.launch {
            featureMutex.withLock {
                try {
                    val nfcCmd = command.removePrefix("nfc:")
                    val output = when {
                        nfcCmd == "read" -> {
                            val tag = builtInNfcReader.readTag()
                            tag?.let { "UID: ${it.uid}\nType: ${it.type}\nATQA: ${it.atqa ?: "—"}\nSAK: ${it.sak ?: "—"}" }
                                ?: "No tag detected"
                        }
                        nfcCmd == "read-ndef" -> builtInNfcReader.readNdef()
                        nfcCmd.startsWith("ndef-text:") -> {
                            val text = nfcCmd.removePrefix("ndef-text:")
                            builtInNfcReader.writeNdefText(text)
                            "NDEF text written: $text"
                        }
                        nfcCmd.startsWith("ndef-url:") -> {
                            val url = nfcCmd.removePrefix("ndef-url:")
                            builtInNfcReader.writeNdefUrl(url)
                            "NDEF URL written: $url"
                        }
                        nfcCmd == "format-ndef" -> {
                            builtInNfcReader.formatNdef()
                            "Tag formatted for NDEF"
                        }
                        nfcCmd.startsWith("apdu:") -> {
                            val apdu = nfcCmd.removePrefix("apdu:")
                            val resp = builtInNfcReader.transceiveApdu(apdu)
                            "APDU Response: $resp"
                        }
                        nfcCmd.startsWith("mfc-read:") -> {
                            val parts = nfcCmd.removePrefix("mfc-read:").split(":")
                            if (parts.size != 3) throw IllegalArgumentException("Expected block:keyType:key")
                            builtInNfcReader.readMifareBlock(parts[0].toInt(), parts[1], parts[2])
                        }
                        else -> throw IllegalArgumentException("Unknown NFC command: $nfcCmd")
                    }
                    _featureExecutionState.value = FeatureExecutionState.Success(
                        featureId = feature.id, title = feature.title, output = output
                    )
                    appendRunRecord(FeatureRunRecord(
                        featureId = feature.id, title = feature.title,
                        outcome = FeatureRunOutcome.SUCCESS, detail = output
                    ))
                } catch (e: Exception) {
                    val msg = e.message ?: "NFC operation failed"
                    _featureExecutionState.value = FeatureExecutionState.Failed(
                        featureId = feature.id, title = feature.title, message = msg
                    )
                    _errorMessage.value = msg
                    appendRunRecord(FeatureRunRecord(
                        featureId = feature.id, title = feature.title,
                        outcome = FeatureRunOutcome.FAILED, detail = msg
                    ))
                }
            }
        }
    }

    /** Read a tag from the ACR reader on-demand. */
    fun readAcrTag() {
        scope.launch {
            val tag = acrUsbReader.readTag()
            if (tag != null) {
                addDiscoveredTag(tag)
            } else {
                _errorMessage.value = "No card detected on ACR reader"
            }
        }
    }

    fun clearFeatureRunHistory() {
        _featureRunHistory.value = emptyList()
    }

    fun clearFeatureExecutionState() {
        _featureExecutionState.value = FeatureExecutionState.Idle
    }

    fun sendRawCommand(command: String) {
        if (_activeReaderType.value != ReaderType.PROXMARK3) {
            _errorMessage.value = "Raw commands require Proxmark3 reader"
            return
        }
        if (connectionState.value !is UsbConnectionState.Connected) {
            _errorMessage.value = "PM3 not connected"
            return
        }
        _featureExecutionState.value = FeatureExecutionState.Running(
            featureId = "raw-cmd",
            title = "Raw: $command"
        )
        scope.launch {
            featureMutex.withLock {
                val timeoutMs = _settings.value.commandTimeoutMs.toLong()
                try {
                    val output = proxmarkClient.executeCommand(command, timeoutMs = timeoutMs)
                    _featureExecutionState.value = FeatureExecutionState.Success(
                        featureId = "raw-cmd",
                        title = "Raw: $command",
                        output = output.ifBlank { null }
                    )
                    appendRunRecord(FeatureRunRecord(
                        featureId = "raw-cmd", title = "Raw: $command",
                        outcome = FeatureRunOutcome.SUCCESS, detail = output.ifBlank { "OK" }
                    ))
                } catch (e: Exception) {
                    val msg = e.message ?: "Command failed"
                    _featureExecutionState.value = FeatureExecutionState.Failed(
                        featureId = "raw-cmd", title = "Raw: $command", message = msg
                    )
                    _errorMessage.value = msg
                    appendRunRecord(FeatureRunRecord(
                        featureId = "raw-cmd", title = "Raw: $command",
                        outcome = FeatureRunOutcome.FAILED, detail = msg
                    ))
                }
            }
        }
    }

    /** Delete tags by composite key "uid|type". */
    fun deleteTags(tagKeys: Set<String>) {
        _savedTags.update { tags ->
            val updated = tags.filterNot { "${it.uid}|${it.type}" in tagKeys }
            scope.launch { tagStore.saveTags(updated) }
            updated
        }
    }

    // ── Tag Group Management ──

    fun createTagGroup(name: String) {
        val id = "group-${System.currentTimeMillis()}"
        _tagGroups.update { groups ->
            val updated = groups + TagGroup(id = id, name = name)
            scope.launch { tagStore.saveGroups(updated) }
            updated
        }
    }

    fun renameTagGroup(groupId: String, newName: String) {
        _tagGroups.update { groups ->
            val updated = groups.map { if (it.id == groupId) it.copy(name = newName) else it }
            scope.launch { tagStore.saveGroups(updated) }
            updated
        }
    }

    fun deleteTagGroup(groupId: String) {
        _tagGroups.update { groups ->
            val updated = groups.filterNot { it.id == groupId }
            scope.launch { tagStore.saveGroups(updated) }
            updated
        }
    }

    fun addTagToGroup(groupId: String, tagKey: String) {
        _tagGroups.update { groups ->
            val updated = groups.map { group ->
                if (group.id == groupId) group.copy(tagKeys = group.tagKeys + tagKey) else group
            }
            scope.launch { tagStore.saveGroups(updated) }
            updated
        }
    }

    fun removeTagFromGroup(groupId: String, tagKey: String) {
        _tagGroups.update { groups ->
            val updated = groups.map { group ->
                if (group.id == groupId) group.copy(tagKeys = group.tagKeys - tagKey) else group
            }
            scope.launch { tagStore.saveGroups(updated) }
            updated
        }
    }

    fun getUsbDeviceDebugInfo(): String {
        return proxmarkClient.getConnectedDevicesInfo()
    }

    fun close() {
        proxmarkClient.close()
        builtInNfcReader.close()
        acrUsbReader.close()
    }

    /**
     * Run a PM3 command directly (not tied to a feature catalog entry).
     * Used by workflow automation and key recovery.
     */
    fun runFeatureByCommand(command: String, title: String = "Command") {
        if (connectionState.value !is UsbConnectionState.Connected) {
            _featureExecutionState.value = FeatureExecutionState.Failed(
                featureId = "direct-cmd", title = title,
                message = "Device not connected"
            )
            return
        }

        _featureExecutionState.value = FeatureExecutionState.Running(
            featureId = "direct-cmd", title = title
        )
        scope.launch {
            featureMutex.withLock {
                val timeoutMs = _settings.value.commandTimeoutMs.toLong()
                try {
                    val output = proxmarkClient.executeCommand(command, timeoutMs = timeoutMs)
                    _featureExecutionState.value = FeatureExecutionState.Success(
                        featureId = "direct-cmd", title = title,
                        output = output.ifBlank { null }
                    )
                    appendRunRecord(FeatureRunRecord(
                        featureId = "direct-cmd", title = title,
                        outcome = FeatureRunOutcome.SUCCESS,
                        detail = output.ifBlank { "Completed" }
                    ))
                } catch (_: SocketTimeoutException) {
                    _featureExecutionState.value = FeatureExecutionState.Failed(
                        featureId = "direct-cmd", title = title,
                        message = "$title timed out"
                    )
                } catch (io: IOException) {
                    _featureExecutionState.value = FeatureExecutionState.Failed(
                        featureId = "direct-cmd", title = title,
                        message = "$title failed: ${io.message}"
                    )
                }
            }
        }
    }

    private fun addDiscoveredTag(tag: TagInfo) {
        _lastScan.value = tag
        _savedTags.update { tags ->
            val withoutSame = tags.filterNot { it.uid == tag.uid && it.type == tag.type }
            val updated = (listOf(tag) + withoutSame).take(100)
            scope.launch { tagStore.saveTags(updated) }
            updated
        }
    }

    private fun updateScanState(line: String) {
        parserWindow.addLast(line)
        while (parserWindow.size > 50) {
            parserWindow.removeFirst()
        }

        val parsed = ProxmarkOutputParser.parseTagInfo(parserWindow.joinToString("\n")) ?: return
        val current = _lastScan.value
        if (current != null && current.uid == parsed.uid && current.type == parsed.type) {
            return
        }

        addDiscoveredTag(parsed)
    }

    /**
     * Parse a terminal line for MIFARE key recovery data and update sector map.
     * Recognizes both "Sector N keyX = XXXX" and table-format lines.
     */
    private fun parseKeyRecoveryLine(line: String) {
        val parsed = ProxmarkOutputParser.parseKeyLine(line) ?: return
        val keyBytes = parsed.keyHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()

        _mifareKeyState.update { state ->
            var updated = state.withKeyFound(
                parsed.sector,
                if (parsed.keyType == "A") com.example.proxmark.mifare.KeyType.KEY_A
                else com.example.proxmark.mifare.KeyType.KEY_B,
                keyBytes
            )
            // Table rows may include both keys
            if (parsed.otherKeyHex != null) {
                val otherBytes = parsed.otherKeyHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
                updated = updated.withKeyFound(
                    parsed.sector,
                    if (parsed.keyType == "A") com.example.proxmark.mifare.KeyType.KEY_B
                    else com.example.proxmark.mifare.KeyType.KEY_A,
                    otherBytes
                )
            }
            updated
        }
    }

    /** Reset the MIFARE key state (e.g., for a new card). */
    fun resetMifareKeyState(cardType: com.example.proxmark.mifare.CardType = com.example.proxmark.mifare.CardType.CLASSIC_1K) {
        _mifareKeyState.value = com.example.proxmark.mifare.MifareKeyState.forCardType(cardType)
    }

    private fun appendRunRecord(record: FeatureRunRecord) {
        _featureRunHistory.update { current ->
            (listOf(record) + current).take(100)
        }
    }
}
