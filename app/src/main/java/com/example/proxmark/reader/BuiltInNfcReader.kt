package com.example.proxmark.reader

import android.app.Activity
import android.nfc.NdefMessage
import android.nfc.NdefRecord
import android.nfc.NfcAdapter
import android.nfc.Tag
import android.nfc.tech.IsoDep
import android.nfc.tech.MifareClassic
import android.nfc.tech.MifareUltralight
import android.nfc.tech.Ndef
import android.nfc.tech.NdefFormatable
import android.nfc.tech.NfcA
import android.nfc.tech.NfcB
import android.nfc.tech.NfcF
import android.nfc.tech.NfcV
import com.example.proxmark.repository.TagInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * CardReader using the Android device's built-in NFC hardware.
 *
 * Supports reading, NDEF write, raw APDU transceive, and Mifare Classic block read.
 * Must be enabled/disabled from an Activity context (reader mode).
 */
class BuiltInNfcReader : CardReader {

    override val readerType = ReaderType.BUILTIN_NFC

    private val _connectionState = MutableStateFlow<ReaderConnectionState>(ReaderConnectionState.Disconnected)
    override val connectionState: StateFlow<ReaderConnectionState> = _connectionState.asStateFlow()

    private val _lastDiscoveredTag = MutableStateFlow<TagInfo?>(null)
    val lastDiscoveredTag: StateFlow<TagInfo?> = _lastDiscoveredTag.asStateFlow()

    private var nfcAdapter: NfcAdapter? = null
    private var boundActivity: Activity? = null

    /** The raw Android NFC Tag object from last discovery (used for write operations). */
    @Volatile
    private var lastRawTag: Tag? = null

    fun isAvailable(): Boolean = nfcAdapter != null
    fun isEnabled(): Boolean = nfcAdapter?.isEnabled == true

    fun enableForegroundDispatch(activity: Activity) {
        nfcAdapter = NfcAdapter.getDefaultAdapter(activity)
        boundActivity = activity

        val adapter = nfcAdapter ?: run {
            _connectionState.value = ReaderConnectionState.Error("NFC not available on this device")
            return
        }

        if (!adapter.isEnabled) {
            _connectionState.value = ReaderConnectionState.Error("NFC is disabled in system settings")
            return
        }

        adapter.enableReaderMode(
            activity,
            { tag -> onTagDiscovered(tag) },
            NfcAdapter.FLAG_READER_NFC_A or
                NfcAdapter.FLAG_READER_NFC_B or
                NfcAdapter.FLAG_READER_NFC_F or
                NfcAdapter.FLAG_READER_NFC_V or
                NfcAdapter.FLAG_READER_NO_PLATFORM_SOUNDS,
            null
        )
        _connectionState.value = ReaderConnectionState.Connected
    }

    fun disableForegroundDispatch(activity: Activity) {
        nfcAdapter?.disableReaderMode(activity)
        _connectionState.value = ReaderConnectionState.Disconnected
        boundActivity = null
        lastRawTag = null
    }

    override fun connect() {
        // Managed via enableForegroundDispatch from Activity
    }

    override fun disconnect() {
        boundActivity?.let { disableForegroundDispatch(it) }
    }

    override suspend fun readTag(): TagInfo? = _lastDiscoveredTag.value

    override fun close() {
        disconnect()
    }

    // ── NFC Operations ──

    /** Read all NDEF records from the tag as a formatted string. */
    fun readNdef(): String {
        val tag = lastRawTag ?: throw IllegalStateException("No tag present. Hold tag near phone.")
        val ndef = Ndef.get(tag) ?: throw IllegalStateException("Tag does not support NDEF")
        return try {
            ndef.connect()
            val message = ndef.ndefMessage
                ?: return "Tag is NDEF-formatted but empty (no records)"
            buildString {
                appendLine("NDEF Message: ${message.records.size} record(s)")
                message.records.forEachIndexed { i, record ->
                    appendLine("--- Record ${i + 1} ---")
                    appendLine("  TNF: ${tnfToString(record.tnf)}")
                    appendLine("  Type: ${String(record.type)}")
                    val payload = record.payload
                    if (record.tnf == NdefRecord.TNF_WELL_KNOWN) {
                        if (record.type.contentEquals(NdefRecord.RTD_TEXT)) {
                            val langLen = payload[0].toInt() and 0x3F
                            val text = String(payload, 1 + langLen, payload.size - 1 - langLen, Charsets.UTF_8)
                            appendLine("  Text: $text")
                        } else if (record.type.contentEquals(NdefRecord.RTD_URI)) {
                            val prefix = uriPrefixes.getOrElse(payload[0].toInt()) { "" }
                            val uri = prefix + String(payload, 1, payload.size - 1, Charsets.UTF_8)
                            appendLine("  URI: $uri")
                        } else {
                            appendLine("  Payload: ${payload.toHexString()}")
                        }
                    } else {
                        appendLine("  Payload: ${payload.toHexString()}")
                    }
                }
            }
        } finally {
            runCatching { ndef.close() }
        }
    }

    /** Write an NDEF text record. */
    fun writeNdefText(text: String) {
        val tag = lastRawTag ?: throw IllegalStateException("No tag present. Hold tag near phone.")
        val record = NdefRecord.createTextRecord("en", text)
        val message = NdefMessage(arrayOf(record))
        writeNdefMessage(tag, message)
    }

    /** Write an NDEF URI record. */
    fun writeNdefUrl(url: String) {
        val tag = lastRawTag ?: throw IllegalStateException("No tag present. Hold tag near phone.")
        val record = NdefRecord.createUri(url)
        val message = NdefMessage(arrayOf(record))
        writeNdefMessage(tag, message)
    }

    /** Format tag for NDEF. */
    fun formatNdef() {
        val tag = lastRawTag ?: throw IllegalStateException("No tag present. Hold tag near phone.")
        val formatable = NdefFormatable.get(tag)
            ?: throw IllegalStateException("Tag cannot be formatted for NDEF")
        try {
            formatable.connect()
            val emptyRecord = NdefRecord(NdefRecord.TNF_EMPTY, ByteArray(0), ByteArray(0), ByteArray(0))
            formatable.format(NdefMessage(arrayOf(emptyRecord)))
        } finally {
            runCatching { formatable.close() }
        }
    }

    /** Send a raw APDU via IsoDep and return the response as hex. */
    fun transceiveApdu(apduHex: String): String {
        val tag = lastRawTag ?: throw IllegalStateException("No tag present. Hold tag near phone.")
        val isoDep = IsoDep.get(tag)
            ?: throw IllegalStateException("Tag does not support ISO-DEP (not an IsoDep tag)")
        val apduBytes = apduHex.hexToBytes()
        return try {
            isoDep.connect()
            isoDep.timeout = 5000
            val response = isoDep.transceive(apduBytes)
            response.toHexString()
        } finally {
            runCatching { isoDep.close() }
        }
    }

    /** Read a Mifare Classic block with key auth. */
    fun readMifareBlock(blockIndex: Int, keyType: String, keyHex: String): String {
        val tag = lastRawTag ?: throw IllegalStateException("No tag present. Hold tag near phone.")
        val mfc = MifareClassic.get(tag)
            ?: throw IllegalStateException("Tag is not a Mifare Classic")
        val keyBytes = keyHex.hexToBytes()
        if (keyBytes.size != 6) throw IllegalArgumentException("Key must be 6 bytes (12 hex chars)")
        val sectorIndex = mfc.blockToSector(blockIndex)
        return try {
            mfc.connect()
            val authOk = if (keyType.equals("B", ignoreCase = true)) {
                mfc.authenticateSectorWithKeyB(sectorIndex, keyBytes)
            } else {
                mfc.authenticateSectorWithKeyA(sectorIndex, keyBytes)
            }
            if (!authOk) throw SecurityException("Authentication failed with key $keyType")
            val data = mfc.readBlock(blockIndex)
            "Block $blockIndex: ${data.toHexString()}"
        } finally {
            runCatching { mfc.close() }
        }
    }

    // ── Private helpers ──

    private fun onTagDiscovered(tag: Tag) {
        lastRawTag = tag
        val uid = tag.id.toHexString()
        val techList = tag.techList.map { it.substringAfterLast('.') }
        val type = identifyTagType(tag, techList)
        val atqa = extractAtqa(tag)
        val sak = extractSak(tag)

        _lastDiscoveredTag.value = TagInfo(uid = uid, type = type, atqa = atqa, sak = sak)
    }

    private fun writeNdefMessage(tag: Tag, message: NdefMessage) {
        val ndef = Ndef.get(tag)
        if (ndef != null) {
            try {
                ndef.connect()
                if (!ndef.isWritable) throw IllegalStateException("Tag is read-only")
                if (message.toByteArray().size > ndef.maxSize) {
                    throw IllegalStateException("Message too large (${message.toByteArray().size} bytes, max ${ndef.maxSize})")
                }
                ndef.writeNdefMessage(message)
            } finally {
                runCatching { ndef.close() }
            }
        } else {
            val formatable = NdefFormatable.get(tag)
                ?: throw IllegalStateException("Tag does not support NDEF and cannot be formatted")
            try {
                formatable.connect()
                formatable.format(message)
            } finally {
                runCatching { formatable.close() }
            }
        }
    }

    private fun identifyTagType(tag: Tag, techList: List<String>): String {
        tag.techList.forEach { tech ->
            when {
                tech.endsWith("MifareClassic") -> {
                    val mfc = MifareClassic.get(tag) ?: return "Mifare Classic"
                    return when (mfc.type) {
                        MifareClassic.TYPE_CLASSIC -> "Mifare Classic ${mfc.size}B"
                        MifareClassic.TYPE_PLUS -> "Mifare Plus"
                        MifareClassic.TYPE_PRO -> "Mifare Pro"
                        else -> "Mifare Classic"
                    }
                }
                tech.endsWith("MifareUltralight") -> {
                    val mfu = MifareUltralight.get(tag) ?: return "Mifare Ultralight"
                    return when (mfu.type) {
                        MifareUltralight.TYPE_ULTRALIGHT -> "Mifare Ultralight"
                        MifareUltralight.TYPE_ULTRALIGHT_C -> "Mifare Ultralight C"
                        else -> "NTAG / Mifare Ultralight"
                    }
                }
                tech.endsWith("IsoDep") -> return "ISO-DEP (ISO 14443-4)"
                tech.endsWith("NfcV") -> return "ISO 15693 (NFC-V)"
                tech.endsWith("NfcF") -> return "FeliCa (NFC-F)"
                tech.endsWith("NfcB") -> return "ISO 14443-B (NFC-B)"
            }
        }
        return if (techList.contains("NfcA")) "ISO 14443-A (NFC-A)" else "Unknown (${techList.joinToString()})"
    }

    private fun extractAtqa(tag: Tag): String? {
        val nfcA = NfcA.get(tag) ?: return null
        return nfcA.atqa.toHexString()
    }

    private fun extractSak(tag: Tag): String? {
        val nfcA = NfcA.get(tag) ?: return null
        return String.format("%02X", nfcA.sak)
    }

    private fun ByteArray.toHexString(): String =
        joinToString("") { String.format("%02X", it) }

    private fun String.hexToBytes(): ByteArray {
        val hex = this.replace(" ", "")
        require(hex.length % 2 == 0) { "Hex string must have even length" }
        return ByteArray(hex.length / 2) { i ->
            hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }

    private fun tnfToString(tnf: Short): String = when (tnf) {
        NdefRecord.TNF_EMPTY -> "Empty"
        NdefRecord.TNF_WELL_KNOWN -> "Well-Known"
        NdefRecord.TNF_MIME_MEDIA -> "MIME"
        NdefRecord.TNF_ABSOLUTE_URI -> "Absolute URI"
        NdefRecord.TNF_EXTERNAL_TYPE -> "External"
        NdefRecord.TNF_UNKNOWN -> "Unknown"
        else -> "Reserved ($tnf)"
    }

    companion object {
        private val uriPrefixes = mapOf(
            0x00 to "", 0x01 to "http://www.", 0x02 to "https://www.",
            0x03 to "http://", 0x04 to "https://", 0x05 to "tel:",
            0x06 to "mailto:", 0x07 to "ftp://anonymous:anonymous@",
            0x08 to "ftp://ftp.", 0x09 to "ftps://", 0x0A to "sftp://",
            0x0D to "ftp://", 0x0E to "ftps://", 0x0F to "sftp://"
        )
    }
}
