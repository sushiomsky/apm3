package com.sushiomsky.proxmark.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sushiomsky.proxmark.features.ProxmarkFeature
import com.sushiomsky.proxmark.features.ProxmarkFeatureCatalog
import com.sushiomsky.proxmark.export.DataExporter
import com.sushiomsky.proxmark.mifare.DumpComparison
import com.sushiomsky.proxmark.mifare.MifareClassicDump
import com.sushiomsky.proxmark.mifare.MifareWorkflow
import com.sushiomsky.proxmark.persistence.ThemeMode
import com.sushiomsky.proxmark.plugins.CustomWorkflow
import com.sushiomsky.proxmark.plugins.Plugin
import com.sushiomsky.proxmark.plugins.PluginCatalog
import com.sushiomsky.proxmark.plugins.ScriptFile
import com.sushiomsky.proxmark.reader.ReaderType
import com.sushiomsky.proxmark.repository.ProxmarkRepository
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MainViewModel(private val repository: ProxmarkRepository) : ViewModel() {

    private val _uiState = MutableStateFlow(
        MainUiState(
            availableFeatures = ProxmarkFeatureCatalog.allFeatures
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _shareEvent = MutableSharedFlow<Pair<String, String>>()
    val shareEvent: SharedFlow<Pair<String, String>> = _shareEvent.asSharedFlow()

    init {
        repository.start()

        viewModelScope.launch {
            repository.connectionState.collect { state ->
                _uiState.update { it.copy(connectionState = state) }
            }
        }

        viewModelScope.launch {
            repository.lastScan.collect { scan ->
                _uiState.update { it.copy(lastScan = scan) }
            }
        }

        viewModelScope.launch {
            repository.savedTags.collect { tags ->
                _uiState.update { it.copy(savedTags = tags) }
            }
        }

        viewModelScope.launch {
            repository.errorMessage.collect { message ->
                _uiState.update { it.copy(errorMessage = message) }
            }
        }

        viewModelScope.launch {
            repository.featureExecutionState.collect { executionState ->
                _uiState.update { it.copy(featureExecutionState = executionState) }
            }
        }

        viewModelScope.launch {
            repository.featureRunHistory.collect { runHistory ->
                _uiState.update { it.copy(featureRunHistory = runHistory) }
            }
        }

        viewModelScope.launch {
            repository.settings.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }

        viewModelScope.launch {
            repository.autoScanEnabled.collect { enabled ->
                _uiState.update { it.copy(autoScanEnabled = enabled) }
            }
        }

        viewModelScope.launch {
            repository.activeReaderType.collect { type ->
                _uiState.update { it.copy(activeReaderType = type) }
            }
        }

        viewModelScope.launch {
            repository.readerStates.collect { states ->
                _uiState.update { it.copy(readerStates = states) }
            }
        }

        viewModelScope.launch {
            repository.tagGroups.collect { groups ->
                _uiState.update { it.copy(tagGroups = groups) }
            }
        }

        viewModelScope.launch {
            repository.terminalLines.collect { line ->
                _uiState.update { state ->
                    val lines = (state.terminalLines + line).takeLast(500)
                    state.copy(terminalLines = lines)
                }
            }
        }

        // Live sector map updates from key recovery parsing
        viewModelScope.launch {
            repository.mifareKeyState.collect { keyState ->
                _uiState.update { it.copy(mifareKeyState = keyState) }
            }
        }
    }

    fun connect() = repository.connect()

    fun disconnect() = repository.disconnect()

    fun reconnect() = repository.reconnect()

    fun runFeature(feature: ProxmarkFeature, paramValues: Map<String, String> = emptyMap()) =
        repository.runFeature(feature, paramValues)

    // ── Pending feature confirmation (rotation-safe) ────────────────────
    // Decides whether a feature needs a parameter/confirmation dialog first.
    // The pending id lives in uiState so an open dialog survives rotation.

    fun requestRunFeature(feature: ProxmarkFeature) {
        if (feature.parameters.isNotEmpty() ||
            (feature.safetyLevel.requiresConfirmation && !uiState.value.skipConfirmations)
        ) {
            _uiState.update { it.copy(pendingFeatureId = feature.id) }
        } else {
            runFeature(feature)
        }
    }

    fun confirmPendingFeature(paramValues: Map<String, String> = emptyMap()) {
        val pending = uiState.value.pendingFeatureId?.let { id ->
            uiState.value.availableFeatures.find { it.id == id }
        } ?: return
        _uiState.update { it.copy(pendingFeatureId = null) }
        runFeature(pending, paramValues)
    }

    fun dismissPendingFeature() {
        _uiState.update { it.copy(pendingFeatureId = null) }
    }

    fun clearError() = repository.clearError()

    fun clearSavedTags() = repository.clearSavedTags()

    fun clearFeatureHistory() = repository.clearFeatureRunHistory()

    fun clearFeatureExecutionState() = repository.clearFeatureExecutionState()

    fun getUsbDeviceDebugInfo(): String = repository.getUsbDeviceDebugInfo()

    fun setSkipConfirmations(skip: Boolean) {
        _uiState.update { it.copy(skipConfirmations = skip) }
    }

    fun setTagSearchQuery(query: String) {
        _uiState.update { it.copy(tagSearchQuery = query) }
    }

    fun setFeatureSearchQuery(query: String) {
        _uiState.update { it.copy(featureSearchQuery = query) }
    }

    fun toggleRawCommandMode() {
        _uiState.update { it.copy(rawCommandMode = !it.rawCommandMode) }
    }

    fun sendRawCommand(command: String) {
        val startTime = System.currentTimeMillis()
        repository.sendRawCommand(command)
        val elapsed = System.currentTimeMillis() - startTime
        _uiState.update { state ->
            val history = (listOf(command) + state.commandHistory)
                .distinct()
                .take(20)
            val metrics = state.commandMetrics
            state.copy(
                commandHistory = history,
                commandMetrics = metrics.copy(
                    commandsSent = metrics.commandsSent + 1,
                    lastCommandTimeMs = elapsed,
                    totalResponseTimeMs = metrics.totalResponseTimeMs + elapsed
                )
            )
        }
    }

    fun toggleTagSelectionMode() {
        _uiState.update {
            if (it.tagSelectionMode) {
                it.copy(tagSelectionMode = false, selectedTagIds = emptySet())
            } else {
                it.copy(tagSelectionMode = true)
            }
        }
    }

    fun toggleTagSelected(tagKey: String) {
        _uiState.update { state ->
            val newSet = if (tagKey in state.selectedTagIds) {
                state.selectedTagIds - tagKey
            } else {
                state.selectedTagIds + tagKey
            }
            state.copy(selectedTagIds = newSet)
        }
    }

    fun deleteSelectedTags() {
        val selected = _uiState.value.selectedTagIds
        repository.deleteTags(selected)
        _uiState.update { it.copy(tagSelectionMode = false, selectedTagIds = emptySet()) }
    }

    fun updateTimeout(timeoutMs: Int) = repository.updateTimeout(timeoutMs)

    fun toggleFavorite(featureId: String) = repository.toggleFavorite(featureId)

    fun updateThemeMode(mode: ThemeMode) = repository.updateThemeMode(mode)

    fun setAutoScanEnabled(enabled: Boolean) = repository.setAutoScanEnabled(enabled)

    fun updateAutoScanInterval(intervalMs: Int) = repository.updateAutoScanInterval(intervalMs)

    fun updateTagNote(uid: String, type: String, note: String?) = repository.updateTagNote(uid, type, note)

    fun setActiveReader(type: ReaderType) = repository.setActiveReader(type)

    fun readAcrTag() = repository.readAcrTag()

    fun createTagGroup(name: String) = repository.createTagGroup(name)

    fun renameTagGroup(groupId: String, newName: String) = repository.renameTagGroup(groupId, newName)

    fun deleteTagGroup(groupId: String) = repository.deleteTagGroup(groupId)

    fun addTagToGroup(groupId: String, tagKey: String) = repository.addTagToGroup(groupId, tagKey)

    fun removeTagFromGroup(groupId: String, tagKey: String) = repository.removeTagFromGroup(groupId, tagKey)

    fun parseDumpFromHex(hexString: String) {
        val dump = MifareClassicDump.fromHexString(hexString)
        _uiState.update { it.copy(mifareDumpToView = dump, showDumpViewer = dump != null) }
    }

    fun closeDumpViewer() {
        _uiState.update { it.copy(showDumpViewer = false) }
    }

    fun setDump1(hexString: String) {
        val dump = MifareClassicDump.fromHexString(hexString)
        _uiState.update { state ->
            val newState = state.copy(mifareDump1 = dump)
            if (newState.mifareDump1 != null && newState.mifareDump2 != null) {
                val comparison = DumpComparison.compare(newState.mifareDump1, newState.mifareDump2)
                newState.copy(dumpComparison = comparison, showDumpComparison = true)
            } else {
                newState
            }
        }
    }

    fun setDump2(hexString: String) {
        val dump = MifareClassicDump.fromHexString(hexString)
        _uiState.update { state ->
            val newState = state.copy(mifareDump2 = dump)
            if (newState.mifareDump1 != null && newState.mifareDump2 != null) {
                val comparison = DumpComparison.compare(newState.mifareDump1, newState.mifareDump2)
                newState.copy(dumpComparison = comparison, showDumpComparison = true)
            } else {
                newState
            }
        }
    }

    fun clearDumpComparison() {
        _uiState.update { it.copy(mifareDump1 = null, mifareDump2 = null, dumpComparison = null, showDumpComparison = false) }
    }

    fun closeDumpComparison() {
        _uiState.update { it.copy(showDumpComparison = false) }
    }

    fun showWorkflow(workflow: MifareWorkflow) {
        _uiState.update { it.copy(workflowToView = workflow, showWorkflowViewer = true) }
    }

    fun closeWorkflowViewer() {
        _uiState.update { it.copy(showWorkflowViewer = false) }
    }

    fun clearTerminal() {
        _uiState.update { it.copy(terminalLines = emptyList()) }
    }

    // ── Key Recovery ──────────────────────────────────────────────────

    fun showKeyRecovery() {
        _uiState.update { it.copy(showKeyRecovery = true) }
    }

    fun hideKeyRecovery() {
        _uiState.update { it.copy(showKeyRecovery = false) }
    }

    fun selectSector(sector: Int?) {
        _uiState.update { it.copy(selectedSector = sector) }
    }

    fun updateMifareKeyState(keyState: com.sushiomsky.proxmark.mifare.MifareKeyState) {
        _uiState.update { it.copy(mifareKeyState = keyState) }
    }

    fun resetKeyRecovery(cardType: com.sushiomsky.proxmark.mifare.CardType = com.sushiomsky.proxmark.mifare.CardType.CLASSIC_1K) {
        repository.resetMifareKeyState(cardType)
        _uiState.update { it.copy(selectedSector = null) }
    }

    /**
     * Start a dictionary attack — tries default keys on all sectors.
     * This is the first phase of any key recovery workflow.
     */
    fun startDictionaryCheck() {
        val currentState = _uiState.value.mifareKeyState
        _uiState.update { it.copy(
            mifareKeyState = currentState.copy(
                attackMethod = com.sushiomsky.proxmark.mifare.AttackMethod.DICTIONARY
            ),
            showKeyRecovery = true
        ) }

        viewModelScope.launch {
            // Run hf mf chk command (checks default keys against all sectors)
            repository.runFeatureByCommand("hf mf chk --1k", "Dictionary Check")
        }
    }

    // ── Gym Locker Workflow ──────────────────────────────────────────

    fun showGymLockerWorkflow() {
        _uiState.update { it.copy(showGymLockerWorkflow = true) }
    }

    fun hideGymLockerWorkflow() {
        _uiState.update { it.copy(showGymLockerWorkflow = false) }
    }

    fun runGymWorkflowCommand(command: String, title: String) {
        viewModelScope.launch {
            repository.runFeatureByCommand(command, title)
        }
    }

    // ── Plugin System ────────────────────────────────────────────────

    fun showPluginBrowser() {
        _uiState.update { it.copy(showPluginBrowser = true) }
    }

    fun hidePluginBrowser() {
        _uiState.update { it.copy(showPluginBrowser = false) }
    }

    fun installPlugin(pluginId: String) {
        _uiState.update { state ->
            val updated = getAllPlugins(state).map { p ->
                if (p.id == pluginId) p.copy(installed = true, enabled = true) else p
            }
            state.copy(installedPlugins = updated.filter { it.installed })
        }
    }

    fun uninstallPlugin(pluginId: String) {
        _uiState.update { state ->
            val updated = getAllPlugins(state).map { p ->
                if (p.id == pluginId) p.copy(installed = false, enabled = false) else p
            }
            state.copy(installedPlugins = updated.filter { it.installed })
        }
    }

    fun togglePluginEnabled(pluginId: String) {
        _uiState.update { state ->
            state.copy(
                installedPlugins = state.installedPlugins.map { p ->
                    if (p.id == pluginId) p.copy(enabled = !p.enabled) else p
                }
            )
        }
    }

    /** Merge catalog defaults with current installed state. */
    fun getAllPluginsForBrowser(): List<Plugin> = getAllPlugins(_uiState.value)

    private fun getAllPlugins(state: MainUiState): List<Plugin> {
        val installedMap = state.installedPlugins.associateBy { it.id }
        return PluginCatalog.builtInPlugins.map { catalogPlugin ->
            installedMap[catalogPlugin.id] ?: catalogPlugin
        }
    }

    fun showPluginWorkflowBuilder(workflow: CustomWorkflow? = null) {
        _uiState.update { it.copy(showWorkflowBuilder = true, editingWorkflow = workflow) }
    }

    fun hidePluginWorkflowBuilder() {
        _uiState.update { it.copy(showWorkflowBuilder = false, editingWorkflow = null) }
    }

    fun saveWorkflow(workflow: CustomWorkflow) {
        _uiState.update { state ->
            val existing = state.customWorkflows.indexOfFirst { it.id == workflow.id }
            val updated = if (existing >= 0) {
                state.customWorkflows.toMutableList().apply { set(existing, workflow) }
            } else {
                state.customWorkflows + workflow
            }
            state.copy(customWorkflows = updated, showWorkflowBuilder = false, editingWorkflow = null)
        }
    }

    fun deleteWorkflow(workflowId: String) {
        _uiState.update { state ->
            state.copy(customWorkflows = state.customWorkflows.filter { it.id != workflowId })
        }
    }

    fun runWorkflow(workflow: CustomWorkflow) {
        viewModelScope.launch {
            workflow.steps.forEach { step ->
                if (step.command.isNotBlank()) {
                    repository.runFeatureByCommand(step.command, step.title)
                    if (step.waitForResult) {
                        delay(2000)
                    }
                }
            }
        }
    }

    fun showPluginScriptEditor(script: ScriptFile? = null) {
        _uiState.update { it.copy(showScriptEditor = true, editingScript = script) }
    }

    fun hidePluginScriptEditor() {
        _uiState.update { it.copy(showScriptEditor = false, editingScript = null) }
    }

    fun saveScript(script: ScriptFile) {
        _uiState.update { state ->
            val existing = state.savedScripts.indexOfFirst { it.id == script.id }
            val updated = if (existing >= 0) {
                state.savedScripts.toMutableList().apply { set(existing, script) }
            } else {
                state.savedScripts + script
            }
            state.copy(savedScripts = updated)
        }
    }

    fun deleteScript(scriptId: String) {
        _uiState.update { state ->
            state.copy(savedScripts = state.savedScripts.filter { it.id != scriptId })
        }
    }

    fun runScript(script: ScriptFile) {
        viewModelScope.launch {
            script.content.lines()
                .map { it.trim() }
                .filter { it.isNotBlank() && !it.startsWith("#") && !it.startsWith("--") }
                .forEach { line ->
                    repository.runFeatureByCommand(line, "Script: ${script.name}")
                    delay(1500)
                }
        }
    }

    // ── Export & Share ──────────────────────────────────────────────

    fun exportTags() {
        val tags = _uiState.value.savedTags
        if (tags.isEmpty()) return
        val json = DataExporter.exportTagsAsJson(tags)
        shareText(json, "Saved Tags")
    }

    fun exportDump() {
        val dump = _uiState.value.mifareDumpToView ?: return
        val hex = DataExporter.exportDumpAsHex(dump)
        shareText(hex, "MIFARE Dump")
    }

    fun exportKeys() {
        val keyState = _uiState.value.mifareKeyState
        if (keyState.foundKeysCount == 0) return
        val text = DataExporter.exportKeysAsText(keyState)
        shareText(text, "MIFARE Keys")
    }

    fun exportTerminalLog() {
        val lines = _uiState.value.terminalLines
        if (lines.isEmpty()) return
        val text = DataExporter.exportTerminalLog(lines)
        shareText(text, "Terminal Log")
    }

    fun shareText(text: String, title: String) {
        viewModelScope.launch {
            _shareEvent.emit(text to title)
        }
    }

    // ── Advanced Mode ──────────────────────────────────────────────

    fun toggleAdvancedMode() {
        _uiState.update { it.copy(advancedMode = !it.advancedMode) }
    }

    fun showAdvancedPanel() {
        _uiState.update { it.copy(showAdvancedPanel = true) }
    }

    fun hideAdvancedPanel() {
        _uiState.update { it.copy(showAdvancedPanel = false) }
    }

    fun toggleProtocolInspector() {
        _uiState.update { it.copy(showProtocolInspector = !it.showProtocolInspector) }
    }

    fun clearCommandHistory() {
        _uiState.update { it.copy(commandHistory = emptyList()) }
    }

    override fun onCleared() {
        super.onCleared()
        repository.close()
    }
}
