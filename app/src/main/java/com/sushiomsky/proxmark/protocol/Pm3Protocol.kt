package com.sushiomsky.proxmark.protocol

/**
 * Proxmark3 NG binary protocol implementation.
 *
 * The PM3 firmware communicates using binary packets, NOT text commands.
 * The desktop `proxmark3` client translates text ("hw status") into binary
 * packets.  This class handles that translation for Android.
 *
 * Command preamble (host → device, 8 bytes):
 *   [magic:4] [length(15)|ng(1):2] [cmd:2] [data:N] [crc:2]
 *
 * Response preamble (device → host, 10 bytes):
 *   [magic:4] [length(15)|ng(1):2] [status:1] [reason:1] [cmd:2] [data:N] [crc:2]
 *
 * Command magic  = 0x61334D50 ("PM3a")
 * Response magic = 0x62334D50 ("PM3b")
 * CRC = ISO 14443-A CRC, or postamble magic placeholder (0x3361 / 0x3362)
 */
object Pm3Protocol {

    private const val PM3A_MAGIC: Int = 0x61334D50  // "PM3a" — command preamble
    private const val PM3B_MAGIC: Int = 0x62334D50  // "PM3b" — response preamble
    private const val CMD_POSTAMBLE_MAGIC: Int = 0x3361   // "a3" — accepted as CRC placeholder
    private const val RESP_POSTAMBLE_MAGIC: Int = 0x3362  // "b3" — response CRC placeholder

    // Maximum data size in a single packet
    const val PM3_CMD_DATA_SIZE = 512

    // ── Command IDs (from pm3_cmd.h) ──────────────────────────────────

    // Device
    const val CMD_VERSION        = 0x0107.toShort()
    const val CMD_STATUS         = 0x0108.toShort()
    const val CMD_PING           = 0x0109.toShort()
    const val CMD_CAPABILITIES   = 0x0112.toShort()

    // HF ISO 14443-A
    const val CMD_HF_ISO14443A_READER     = 0x0385.toShort()
    const val CMD_HF_ISO14443A_SNIFF      = 0x0386.toShort()
    const val CMD_HF_ISO14443A_SIMULATE   = 0x0387.toShort()

    // HF MIFARE Classic (verified against pm3_cmd.h)
    const val CMD_HF_MIFARE_READBL        = 0x0620.toShort()
    const val CMD_HF_MIFARE_READSC        = 0x0621.toShort()
    const val CMD_HF_MIFARE_WRITEBL       = 0x0622.toShort()
    const val CMD_HF_MIFARE_CHKKEYS       = 0x0623.toShort()
    const val CMD_HF_MIFARE_CHKKEYS_FAST  = 0x0625.toShort()
    const val CMD_HF_MIFARE_EML_MEMCLR    = 0x0601.toShort()
    const val CMD_HF_MIFARE_EML_MEMSET    = 0x0602.toShort()
    const val CMD_HF_MIFARE_EML_MEMGET    = 0x0603.toShort()
    const val CMD_HF_MIFARE_CSETBL        = 0x0605.toShort()
    const val CMD_HF_MIFARE_CGETBL        = 0x0606.toShort()
    const val CMD_HF_MIFARE_CIDENT        = 0x0607.toShort()
    const val CMD_HF_MIFARE_SIM           = 0x0610.toShort()
    const val CMD_HF_MIFARE_READER        = 0x0611.toShort()
    const val CMD_HF_MIFARE_NESTED        = 0x0612.toShort()
    const val CMD_HF_MIFARE_ACQ_ENCRYPTED_NONCES = 0x0613.toShort()
    const val CMD_HF_MIFARE_ACQ_NONCES    = 0x0614.toShort()
    const val CMD_HF_MIFARE_SNIFF         = 0x0630.toShort()

    // HF MIFARE Ultralight
    const val CMD_HF_MIFAREU_READBL       = 0x0720.toShort()
    const val CMD_HF_MIFAREU_READCARD     = 0x0721.toShort()
    const val CMD_HF_MIFAREU_WRITEBL      = 0x0722.toShort()

    // HF DESFire
    const val CMD_HF_DESFIRE_INFO         = 0x069A.toShort()

    // LF
    const val CMD_LF_SNIFF_RAW_ADC        = 0x0360.toShort()
    const val CMD_LF_COMMAND_READ         = 0x0106.toShort()
    const val CMD_LF_EM410X_DEMOD         = 0x0510.toShort()
    const val CMD_LF_HID_DEMOD            = 0x0520.toShort()
    const val CMD_LF_T55XX_READBL         = 0x0214.toShort()

    // HW
    const val CMD_HW_TUNE_ANTENNA         = 0x0290.toShort()

    // ACK/NACK
    const val CMD_ACK                     = 0x00FF.toShort()
    const val CMD_NACK                    = 0x00FE.toShort()
    const val CMD_WTX                     = 0x0116.toShort()  // Wait time extension
    const val CMD_DEBUG_PRINT_STRING      = 0x0100.toShort()

    // Status codes
    const val PM3_SUCCESS          = 0.toShort()
    const val PM3_EFAILED          = (-1).toShort()
    const val PM3_ETIMEOUT         = (-6).toShort()

    // ISO14443A reader flags
    const val ISO14A_CONNECT       = 0x0001
    const val ISO14A_NO_DISCONNECT = 0x0002
    const val ISO14A_NO_RATS       = 0x0080

    // ── Packet types ──────────────────────────────────────────────────

    data class Pm3Command(
        val cmd: Short,
        val data: ByteArray = ByteArray(0),
        val ng: Boolean = true
    ) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Pm3Command) return false
            return cmd == other.cmd && data.contentEquals(other.data) && ng == other.ng
        }
        override fun hashCode(): Int = 31 * cmd.hashCode() + data.contentHashCode()
    }

    data class Pm3Response(
        val cmd: Short,
        val status: Short,
        val data: ByteArray,
        val ng: Boolean
    ) {
        val isSuccess: Boolean get() = status == PM3_SUCCESS
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Pm3Response) return false
            return cmd == other.cmd && status == other.status && data.contentEquals(other.data)
        }
        override fun hashCode(): Int = 31 * (31 * cmd.hashCode() + status) + data.contentHashCode()
    }

    // ── Encoding ──────────────────────────────────────────────────────

    /**
     * Encode a command into a PM3 NG binary packet.
     */
    fun encodeCommand(command: Pm3Command): ByteArray {
        if (command.ng) {
            return encodeNG(command)
        }
        return encodeOLD(command)
    }

    private fun encodeNG(command: Pm3Command): ByteArray {
        val dataLen = command.data.size
        // Preamble: magic(4) + length_ng(2) + cmd(2) = 8
        // Postamble: crc(2)
        val packetSize = 8 + dataLen + 2
        val buf = ByteArray(packetSize)

        // Magic (little-endian)
        putUint32LE(buf, 0, PM3A_MAGIC)
        // Length (15 bits) + NG flag (bit 15), packed into single uint16
        val lengthNg = (dataLen and 0x7FFF) or (if (command.ng) 0x8000 else 0)
        putUint16LE(buf, 4, lengthNg)
        // Command ID
        putUint16LE(buf, 6, command.cmd.toInt() and 0xFFFF)
        // Data
        if (dataLen > 0) {
            command.data.copyInto(buf, 8)
        }

        // Use postamble magic as CRC placeholder (firmware accepts it)
        putUint16LE(buf, 8 + dataLen, CMD_POSTAMBLE_MAGIC)

        return buf
    }

    /**
     * Encode using OLD protocol (fixed 544 bytes) for compatibility.
     */
    private fun encodeOLD(command: Pm3Command): ByteArray {
        // OLD format: cmd(8) + arg0(8) + arg1(8) + arg2(8) + data(512) = 544 bytes
        val buf = ByteArray(544)
        putUint64LE(buf, 0, command.cmd.toLong() and 0xFFFF)
        // args and data from command.data if present
        if (command.data.size >= 24) {
            // First 24 bytes are arg0, arg1, arg2
            command.data.copyInto(buf, 8, 0, minOf(24, command.data.size))
            if (command.data.size > 24) {
                command.data.copyInto(buf, 32, 24, minOf(command.data.size, 24 + 512))
            }
        }
        return buf
    }

    // ── Decoding ──────────────────────────────────────────────────────

    /**
     * Try to find and decode a PM3 response in a byte buffer.
     * Searches for PM3b magic (response preamble). Returns the response
     * and number of bytes consumed, or null if no complete packet found.
     */
    fun decodeResponse(buffer: ByteArray, offset: Int = 0, length: Int = buffer.size): Pair<Pm3Response, Int>? {
        // Minimum NG response: preamble(10) + crc(2) = 12 bytes (0 data)
        if (length - offset < 12) return null

        var pos = offset
        while (pos <= length - 12) {
            val magic = getUint32LE(buffer, pos)
            if (magic == PM3B_MAGIC) {
                val result = tryDecodeNG(buffer, pos, length)
                if (result != null) return result
            }
            pos++
        }
        return null
    }

    private fun tryDecodeNG(buffer: ByteArray, offset: Int, bufferLen: Int): Pair<Pm3Response, Int>? {
        // Response preamble (10 bytes):
        //   magic(4) + length_ng(2) + status(1) + reason(1) + cmd(2)
        // Then: data(length) + crc(2)
        if (bufferLen - offset < 12) return null

        val lengthNg = getUint16LE(buffer, offset + 4)
        val ng = (lengthNg and 0x8000) != 0
        val dataLen = lengthNg and 0x7FFF

        if (!ng) {
            // MIX response (ng=false): data contains arg[3] + payload
            // Same wire format, just interpret data differently
        }

        if (dataLen > PM3_CMD_DATA_SIZE) {
            android.util.Log.w("Pm3Protocol", "Response data too large: $dataLen")
            return null
        }

        // Full packet: preamble(10) + data(dataLen) + crc(2)
        val packetSize = 10 + dataLen + 2
        if (bufferLen - offset < packetSize) return null

        val status = buffer[offset + 6].toInt().toShort()
        @Suppress("UNUSED_VARIABLE")
        val reason = buffer[offset + 7] // reserved for future use
        val cmd = getInt16LE(buffer, offset + 8)

        val data = if (dataLen > 0) {
            buffer.copyOfRange(offset + 10, offset + 10 + dataLen)
        } else {
            ByteArray(0)
        }

        // Verify CRC (accept postamble magic as valid)
        val actualCrc = getUint16LE(buffer, offset + 10 + dataLen)
        if (actualCrc != RESP_POSTAMBLE_MAGIC) {
            // Compute CRC over preamble + data (from magic through end of data)
            val crc = crc14443a(buffer, offset, 10 + dataLen)
            val crcSwapped = ((crc and 0xFF) shl 8) or ((crc shr 8) and 0xFF)
            if (crcSwapped != actualCrc) {
                android.util.Log.w("Pm3Protocol", "Response CRC mismatch: computed=0x${crcSwapped.toString(16)}, actual=0x${actualCrc.toString(16)}")
                // Accept anyway — USB has its own error checking
            }
        }

        if (!ng && dataLen >= 24) {
            // MIX response: first 24 bytes are arg[3], rest is payload
            return Pm3Response(cmd, status, data, ng = false) to packetSize
        }

        return Pm3Response(cmd, status, data, ng = true) to packetSize
    }

    // ── Command builders ──────────────────────────────────────────────

    /** Build a CMD_PING packet (device echoes the data back). */
    fun buildPing(data: ByteArray = byteArrayOf(0xDE.toByte(), 0xAD.toByte(), 0xC0.toByte(), 0xDE.toByte())): ByteArray {
        return encodeCommand(Pm3Command(CMD_PING, data))
    }

    /** Build a CMD_VERSION packet (returns firmware info). */
    fun buildVersion(): ByteArray {
        return encodeCommand(Pm3Command(CMD_VERSION))
    }

    /** Build a CMD_STATUS packet (returns hardware status). */
    fun buildStatus(): ByteArray {
        return encodeCommand(Pm3Command(CMD_STATUS))
    }

    /** Build a CMD_HW_TUNE_ANTENNA packet. */
    fun buildTuneAntenna(): ByteArray {
        return encodeCommand(Pm3Command(CMD_HW_TUNE_ANTENNA))
    }

    /** Build ISO14443A reader command (detect + get UID/ATQA/SAK). */
    fun buildHf14aReader(): ByteArray {
        val data = ByteArray(4)
        putUint16LE(data, 0, ISO14A_CONNECT or ISO14A_NO_RATS)
        return encodeCommand(Pm3Command(CMD_HF_ISO14443A_READER, data))
    }

    /** Build CMD_CAPABILITIES request. */
    fun buildCapabilities(): ByteArray {
        return encodeCommand(Pm3Command(CMD_CAPABILITIES))
    }

    /**
     * Build MIFARE Classic read block command.
     * Firmware expects mf_readblock_t: blockno(1) + keytype(1) + key(6) = 8 bytes
     * keytype: 0=KeyA, 1=KeyB
     */
    fun buildMfReadBlock(blockNo: Int, keyType: Int = 0, key: ByteArray = DEFAULT_MF_KEY): ByteArray {
        val data = ByteArray(8)
        data[0] = blockNo.toByte()
        data[1] = keyType.toByte()
        key.copyInto(data, 2, 0, minOf(key.size, 6))
        return encodeCommand(Pm3Command(CMD_HF_MIFARE_READBL, data))
    }

    /**
     * Build MIFARE Classic check keys command.
     * Used for dictionary attacks - checks a batch of keys against a sector.
     */
    fun buildMfChkKeys(sectorCount: Int = 16, keyType: Int = 0, keys: List<ByteArray>): ByteArray {
        // keys packed sequentially after header
        val data = ByteArray(3 + keys.size * 6)
        data[0] = sectorCount.toByte()
        data[1] = keyType.toByte()
        data[2] = keys.size.toByte()
        for ((i, k) in keys.withIndex()) {
            k.copyInto(data, 3 + i * 6, 0, minOf(k.size, 6))
        }
        return encodeCommand(Pm3Command(CMD_HF_MIFARE_CHKKEYS, data))
    }

    /**
     * Build MIFARE nested attack command.
     * Firmware expects: block(1) + keytype(1) + target_block(1) + target_keytype(1) + calibrate(1) + key(6)
     */
    fun buildMfNested(knownBlock: Int, knownKeyType: Int, targetBlock: Int, targetKeyType: Int,
                      calibrate: Boolean, key: ByteArray): ByteArray {
        val data = ByteArray(11)
        data[0] = knownBlock.toByte()
        data[1] = knownKeyType.toByte()
        data[2] = targetBlock.toByte()
        data[3] = targetKeyType.toByte()
        data[4] = if (calibrate) 1 else 0
        key.copyInto(data, 5, 0, minOf(key.size, 6))
        return encodeCommand(Pm3Command(CMD_HF_MIFARE_NESTED, data))
    }

    /**
     * Build MIFARE Classic write block command.
     * Firmware expects mf_writeblock_t: blockno(1) + keytype(1) + key(6) + data(16) = 24 bytes
     * keytype: 0=KeyA, 1=KeyB
     */
    fun buildMfWriteBlock(blockNo: Int, keyType: Int = 0, key: ByteArray = DEFAULT_MF_KEY, blockData: ByteArray): ByteArray {
        val data = ByteArray(24)
        data[0] = blockNo.toByte()
        data[1] = keyType.toByte()
        key.copyInto(data, 2, 0, minOf(key.size, 6))
        blockData.copyInto(data, 8, 0, minOf(blockData.size, 16))
        return encodeCommand(Pm3Command(CMD_HF_MIFARE_WRITEBL, data))
    }

    /**
     * Build MIFARE Classic magic card set block (Gen1a backdoor write).
     * Firmware expects: blockno(1) + data(16) = 17 bytes
     */
    fun buildMfCSetBlock(blockNo: Int, blockData: ByteArray): ByteArray {
        val data = ByteArray(17)
        data[0] = blockNo.toByte()
        blockData.copyInto(data, 1, 0, minOf(blockData.size, 16))
        return encodeCommand(Pm3Command(CMD_HF_MIFARE_CSETBL, data))
    }

    /**
     * Build MIFARE Classic sniff command.
     */
    fun buildMfSniff(): ByteArray {
        return encodeCommand(Pm3Command(CMD_HF_MIFARE_SNIFF))
    }

    /**
     * Build ISO14443A sniff command.
     */
    fun buildHf14aSniff(): ByteArray {
        return encodeCommand(Pm3Command(CMD_HF_ISO14443A_SNIFF))
    }

    /**
     * Build LF sniff raw ADC command.
     */
    fun buildLfSniff(): ByteArray {
        return encodeCommand(Pm3Command(CMD_LF_SNIFF_RAW_ADC))
    }

    val DEFAULT_MF_KEY = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(),
                                     0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())

    // ── Text command → binary mapping ─────────────────────────────────

    /**
     * Map a text command (as used in the feature catalog) to binary packet(s).
     * Returns null for unsupported commands — caller should use sendRawText fallback.
     */
    fun textCommandToPackets(textCommand: String): List<ByteArray>? {
        val cmd = textCommand.trim().lowercase()

        return when {
            // Device commands
            cmd == "hw ping" -> listOf(buildPing())
            cmd == "version" -> listOf(buildVersion())
            cmd == "hw status" -> listOf(buildStatus())
            cmd == "hw tune" -> listOf(buildTuneAntenna())
            cmd == "hw reboot" -> listOf(encodeCommand(Pm3Command(0x0400.toShort())))
            cmd == "hw capabilities" || cmd == "hw cap" -> listOf(buildCapabilities())
            cmd == "mem info" -> listOf(buildStatus())

            // HF tag detection
            cmd == "hf search" || cmd == "hf 14a reader" || cmd == "hf 14a info" ->
                listOf(buildHf14aReader())
            cmd == "hf mf info" -> listOf(buildHf14aReader())

            // HF MIFARE Classic read block (parse parameters)
            cmd.startsWith("hf mf rdbl") || cmd.startsWith("hf mf readbl") ->
                parseMfReadBlockCmd(cmd)

            // HF MIFARE Classic write block (parse parameters)
            cmd.startsWith("hf mf wrbl") || cmd.startsWith("hf mf writebl") ->
                parseMfWriteBlockCmd(cmd)

            // HF MIFARE Classic magic card set block
            cmd.startsWith("hf mf csetblk") || cmd.startsWith("hf mf csetbl") ->
                parseMfCSetBlockCmd(cmd)

            // HF sniffing
            cmd == "hf 14a sniff" || cmd == "hf 14a snoop" -> listOf(buildHf14aSniff())
            cmd == "hf mf sniff" || cmd == "hf mf snoop" -> listOf(buildMfSniff())

            // LF commands
            cmd == "lf search" || cmd == "lf read" -> listOf(buildLfSniff())
            cmd == "lf sniff" || cmd == "lf snoop" -> listOf(buildLfSniff())

            // MIFARE Classic block 0 quick read (default key)
            cmd == "hf mf rdbl --blk 0 -k ffffffffffff" ->
                listOf(buildMfReadBlock(0))

            // Unsupported → fallback to sendRawText (may not work with binary-only firmware)
            else -> null
        }
    }

    /**
     * Parse "hf mf rdbl --blk N -k KEY --keyX" style commands.
     */
    private fun parseMfReadBlockCmd(cmd: String): List<ByteArray>? {
        val blkMatch = Regex("--blk\\s+(\\d+)").find(cmd) ?: return null
        val blockNo = blkMatch.groupValues[1].toIntOrNull() ?: return null
        val keyMatch = Regex("-k\\s+([0-9a-fA-F]{12})").find(cmd)
        val key = if (keyMatch != null) hexToBytes(keyMatch.groupValues[1]) else DEFAULT_MF_KEY
        val keyType = if (cmd.contains("--keyb") || cmd.contains("keyb")) 1 else 0
        return listOf(buildMfReadBlock(blockNo, keyType, key))
    }

    /**
     * Parse "hf mf wrbl --blk N -k KEY -d DATA" style commands.
     */
    private fun parseMfWriteBlockCmd(cmd: String): List<ByteArray>? {
        val blkMatch = Regex("--blk\\s+(\\d+)").find(cmd) ?: return null
        val blockNo = blkMatch.groupValues[1].toIntOrNull() ?: return null
        val keyMatch = Regex("-k\\s+([0-9a-fA-F]{12})").find(cmd)
        val key = if (keyMatch != null) hexToBytes(keyMatch.groupValues[1]) else DEFAULT_MF_KEY
        val keyType = if (cmd.contains("--keyb") || cmd.contains("keyb")) 1 else 0
        val dataMatch = Regex("-d\\s+([0-9a-fA-F]{32})").find(cmd) ?: return null
        val blockData = hexToBytes(dataMatch.groupValues[1])
        return listOf(buildMfWriteBlock(blockNo, keyType, key, blockData))
    }

    /**
     * Parse "hf mf csetblk --blk N -d DATA" style commands.
     */
    private fun parseMfCSetBlockCmd(cmd: String): List<ByteArray>? {
        val blkMatch = Regex("--blk\\s+(\\d+)").find(cmd) ?: return null
        val blockNo = blkMatch.groupValues[1].toIntOrNull() ?: return null
        val dataMatch = Regex("-d\\s+([0-9a-fA-F]{32})").find(cmd) ?: return null
        val blockData = hexToBytes(dataMatch.groupValues[1])
        return listOf(buildMfCSetBlock(blockNo, blockData))
    }

    /** Convert hex string to ByteArray. */
    fun hexToBytes(hex: String): ByteArray {
        val len = hex.length / 2
        return ByteArray(len) { i ->
            hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
    }

    // ── Response parsing helpers ──────────────────────────────────────

    /**
     * Decode a VERSION response into human-readable text.
     * Payload: id(4) + section_size(4) + versionstr_len(4) + versionstr(N)
     */
    fun parseVersionResponse(response: Pm3Response): String {
        if (response.data.size < 12) {
            return "[No version data received]"
        }

        val sb = StringBuilder()
        try {
            val chipId = getUint32LE(response.data, 0)
            val sectionSize = getUint32LE(response.data, 4)
            val versionStrLen = getUint32LE(response.data, 8)
            val strBytes = response.data.copyOfRange(12, minOf(12 + versionStrLen, response.data.size))
            val raw = String(strBytes, Charsets.UTF_8).trimEnd('\u0000')
            val text = raw.replace(Regex("\u001B\\[[0-9;]*[A-Za-z]"), "").trim()

            sb.appendLine("Chip ID: 0x${"%08X".format(chipId)}")
            sb.appendLine("Flash used: ${sectionSize / 1024} KB")
            if (text.isNotBlank()) {
                sb.appendLine(text)
            }
        } catch (_: Exception) {
            sb.appendLine("Version data: ${response.data.toHexString()}")
        }
        return sb.toString().trim()
    }

    /**
     * Decode an ISO14443A reader response.
     * Returns UID, ATQA, SAK if a tag was found.
     */
    fun parseHf14aResponse(response: Pm3Response): String {
        if (!response.isSuccess || response.data.size < 10) {
            return "No ISO14443A tag found"
        }

        val sb = StringBuilder()
        try {
            // Response data layout depends on firmware version
            // Typical: UID(variable) + ATQA(2) + SAK(1) + ATS(variable)
            val uidLen = response.data[0].toInt() and 0xFF
            if (uidLen in 4..10 && response.data.size >= uidLen + 4) {
                val uid = response.data.copyOfRange(1, 1 + uidLen)
                val atqa = response.data.copyOfRange(1 + uidLen, 1 + uidLen + 2)
                val sak = response.data[1 + uidLen + 2]

                sb.appendLine("ISO14443A Tag Found:")
                sb.appendLine("  UID: ${uid.toHexString()}")
                sb.appendLine("  ATQA: ${atqa.toHexString()}")
                sb.appendLine("  SAK: 0x${String.format("%02X", sak)}")

                // Identify tag type from SAK
                val tagType = when (sak.toInt() and 0xFF) {
                    0x08 -> "MIFARE Classic 1K"
                    0x09 -> "MIFARE Mini"
                    0x18 -> "MIFARE Classic 4K"
                    0x00 -> "MIFARE Ultralight / NTAG"
                    0x20 -> "MIFARE DESFire / Plus"
                    0x28 -> "JCOP / SmartMX"
                    0x04 -> "MIFARE Classic 1K (NUID)"
                    else -> "Unknown (SAK=0x${String.format("%02X", sak)})"
                }
                sb.appendLine("  Type: $tagType")
            } else {
                sb.appendLine("Tag data: ${response.data.toHexString()}")
            }
        } catch (e: Exception) {
            sb.appendLine("Parse error: ${e.message}")
            sb.appendLine("Raw: ${response.data.toHexString()}")
        }
        return sb.toString()
    }

    /**
     * Parse a PING response (should echo our data back).
     */
    fun parsePingResponse(response: Pm3Response): String {
        return if (response.isSuccess) {
            "PING OK (${response.data.size} bytes echoed)"
        } else {
            "PING failed: status=${response.status}"
        }
    }

    /**
     * Generic response-to-text for debug_print_string messages.
     */
    fun parseDebugString(response: Pm3Response): String {
        val raw = String(response.data, Charsets.UTF_8).trimEnd('\u0000')
        return raw.replace(Regex("\u001B\\[[0-9;]*[A-Za-z]"), "").trim()
    }

    /**
     * Parse MIFARE Classic read block response.
     * Success response contains 16 bytes of block data.
     */
    fun parseMfReadBlockResponse(response: Pm3Response): String {
        if (!response.isSuccess) {
            return "Read block failed: ${statusToString(response.status)}"
        }
        if (response.data.size < 16) {
            return "Read block: unexpected data size (${response.data.size} bytes)"
        }
        val hexData = response.data.copyOfRange(0, 16).toHexString()
        val ascii = response.data.copyOfRange(0, 16).map { b ->
            val c = b.toInt() and 0xFF
            if (c in 0x20..0x7E) c.toChar() else '.'
        }.joinToString("")
        return "Block data: $hexData  |$ascii|"
    }

    /**
     * Parse MIFARE Classic write block response.
     */
    fun parseMfWriteBlockResponse(response: Pm3Response): String {
        return if (response.isSuccess) {
            "Write block: OK"
        } else {
            "Write block failed: ${statusToString(response.status)}"
        }
    }

    /**
     * Parse MIFARE Classic magic card set block response.
     */
    fun parseMfCSetBlockResponse(response: Pm3Response): String {
        return if (response.isSuccess) {
            "Magic card write block: OK"
        } else {
            "Magic card write block failed: ${statusToString(response.status)}"
        }
    }

    /**
     * Parse HW_TUNE_ANTENNA response — contains antenna tuning measurements.
     */
    fun parseTuneResponse(response: Pm3Response): String {
        if (!response.isSuccess) return "Antenna tune failed"
        if (response.data.size < 8) return "Antenna tune: ${response.data.size} bytes"
        val hf = getUint32LE(response.data, 0)
        val lf = if (response.data.size >= 8) getUint32LE(response.data, 4) else 0
        val sb = StringBuilder("Antenna Tuning:\n")
        sb.appendLine("  HF antenna: ${hf / 1000}.${(hf % 1000) / 100} V")
        if (lf > 0) sb.appendLine("  LF antenna: ${lf / 1000}.${(lf % 1000) / 100} V")
        return sb.toString().trim()
    }

    /**
     * Parse STATUS response.
     */
    fun parseStatusResponse(response: Pm3Response): String {
        return if (response.isSuccess) "Device status: OK" else "Device status: Error (${response.status})"
    }

    /**
     * Parse CAPABILITIES response.
     */
    fun parseCapabilitiesResponse(response: Pm3Response): String {
        if (!response.isSuccess || response.data.isEmpty()) return "Capabilities query failed"
        return "Device capabilities: ${response.data.size} bytes\n  Data: ${response.data.copyOfRange(0, minOf(32, response.data.size)).toHexString()}"
    }

    /**
     * Human-readable status code.
     */
    private fun statusToString(status: Short): String {
        return when (status) {
            PM3_SUCCESS -> "success"
            PM3_EFAILED -> "operation failed"
            PM3_ETIMEOUT -> "timeout"
            (-2).toShort() -> "not implemented"
            (-3).toShort() -> "bad arguments"
            (-4).toShort() -> "no data"
            (-5).toShort() -> "device not found"
            else -> "error $status"
        }
    }

    // ── CRC-14443-A ───────────────────────────────────────────────────

    fun crc14443a(data: ByteArray, offset: Int, length: Int): Int {
        var crc = 0x6363
        for (i in offset until offset + length) {
            var b = (data[i].toInt() and 0xFF) xor (crc and 0xFF)
            b = b xor ((b shl 4) and 0xFF)
            crc = (crc ushr 8) xor (b shl 8) xor (b shl 3) xor (b ushr 4)
            crc = crc and 0xFFFF
        }
        return crc
    }

    // ── Byte-level helpers (little-endian) ────────────────────────────

    private fun putUint16LE(buf: ByteArray, offset: Int, value: Int) {
        buf[offset]     = (value and 0xFF).toByte()
        buf[offset + 1] = ((value shr 8) and 0xFF).toByte()
    }

    private fun putUint32LE(buf: ByteArray, offset: Int, value: Int) {
        buf[offset]     = (value and 0xFF).toByte()
        buf[offset + 1] = ((value shr 8) and 0xFF).toByte()
        buf[offset + 2] = ((value shr 16) and 0xFF).toByte()
        buf[offset + 3] = ((value shr 24) and 0xFF).toByte()
    }

    private fun putUint64LE(buf: ByteArray, offset: Int, value: Long) {
        for (i in 0..7) {
            buf[offset + i] = ((value shr (i * 8)) and 0xFF).toByte()
        }
    }

    fun getUint16LE(buf: ByteArray, offset: Int): Int {
        return (buf[offset].toInt() and 0xFF) or
               ((buf[offset + 1].toInt() and 0xFF) shl 8)
    }

    fun getInt16LE(buf: ByteArray, offset: Int): Short {
        return getUint16LE(buf, offset).toShort()
    }

    fun getUint32LE(buf: ByteArray, offset: Int): Int {
        return (buf[offset].toInt() and 0xFF) or
               ((buf[offset + 1].toInt() and 0xFF) shl 8) or
               ((buf[offset + 2].toInt() and 0xFF) shl 16) or
               ((buf[offset + 3].toInt() and 0xFF) shl 24)
    }

    private fun getUint64LE(buf: ByteArray, offset: Int): Long {
        var result = 0L
        for (i in 0..7) {
            result = result or ((buf[offset + i].toLong() and 0xFF) shl (i * 8))
        }
        return result
    }

    private fun ByteArray.toHexString(): String =
        joinToString("") { "%02X".format(it) }
}
