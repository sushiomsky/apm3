package com.sushiomsky.proxmark.protocol

import com.sushiomsky.proxmark.repository.TagInfo

object ProxmarkOutputParser {
    private val uidPatterns = listOf(
        Regex("""(?i)UID\s*[:=]\s*([0-9A-F]{2}(?:[\s:-]?[0-9A-F]{2}){3,9})"""),
        Regex("""(?i)(?:TAG\s*ID|ID)\s*[:=]\s*([0-9A-F]{6,24})""")
    )

    private val atqaPattern = Regex("""(?i)ATQA\s*[:=]\s*([0-9A-Fa-f]{2}\s*[0-9A-Fa-f]{2})""")
    private val sakPattern = Regex("""(?i)SAK\s*[:=]\s*([0-9A-Fa-f]{2})""")

    // Key recovery patterns — matches PM3 output like:
    // "[=] Sector  7 keyA = A0A1A2A3A4A5"
    // "[+]  007 | 031 | A0A1A2A3A4A5 | 1 | 00008627C10A | 1"
    private val keyLinePattern = Regex(
        """(?i)Sector\s+(\d+)\s+key([AB])\s*=\s*([0-9A-Fa-f]{12})"""
    )
    private val keyTablePattern = Regex(
        """^\s*\[.\]\s+0*(\d+)\s+\|\s+\d+\s+\|\s+([0-9A-Fa-f]{12})\s+\|\s+(\d)\s+\|\s+([0-9A-Fa-f]{12})\s+\|\s+(\d)"""
    )

    // Chip fingerprint patterns
    private val fudanPattern = Regex("""(?i)Fudan\s+FM11RF08""")
    private val weakPrngPattern = Regex("""(?i)Prng\.*\s*weak""")
    private val staticNoncePattern = Regex("""(?i)Static\s+enc\s+nonce\.*\s*yes""")
    private val backdoorKeyPattern = Regex("""(?i)Backdoor\s+key\.*\s*([0-9A-Fa-f]{12})""")

    fun parseTagInfo(output: String): TagInfo? {
        val uid = extractUid(output) ?: return null
        val type = extractTagType(output) ?: "Unknown"
        val atqa = extractAtqa(output)
        val sak = extractSak(output)
        return TagInfo(uid = uid, type = type, atqa = atqa, sak = sak)
    }

    fun extractUid(output: String): String? {
        for (pattern in uidPatterns) {
            val match = pattern.find(output) ?: continue
            return normalizeUid(match.groupValues[1])
        }
        return null
    }

    fun extractTagType(output: String): String? {
        val text = output.lowercase()
        return when {
            fudanPattern.containsMatchIn(output) -> "FM11RF08S (Fudan)"
            "mifare classic 4k" in text -> "Mifare Classic 4K"
            "mifare classic 1k" in text || "mifare classic" in text -> "Mifare Classic 1K"
            "mifare ultralight" in text -> "Mifare Ultralight"
            "mifare desfire" in text -> "Mifare DESFire"
            "mifare plus" in text -> "Mifare Plus"
            "ntag" in text -> "NTAG"
            "mifare" in text || "iso14443a" in text -> "Mifare"
            "em4100" in text || "em410x" in text -> "EM4100"
            "hid prox" in text -> "HID Prox"
            "indala" in text -> "Indala"
            "hitag" in text -> "Hitag"
            "t55" in text -> "T55xx"
            "iclass" in text || "picopass" in text -> "iCLASS"
            "felica" in text -> "FeliCa"
            "iso15693" in text || "icode" in text -> "ISO15693"
            else -> null
        }
    }

    fun extractAtqa(output: String): String? {
        val match = atqaPattern.find(output) ?: return null
        return match.groupValues[1].replace(" ", "").uppercase()
    }

    fun extractSak(output: String): String? {
        val match = sakPattern.find(output) ?: return null
        return match.groupValues[1].uppercase()
    }

    /**
     * Parse a single terminal line for MIFARE key recovery data.
     * Returns (sector, keyType, keyHex) if a key is found, null otherwise.
     */
    fun parseKeyLine(line: String): KeyRecoveryLine? {
        // Pattern 1: "Sector 7 keyA = A0A1A2A3A4A5"
        keyLinePattern.find(line)?.let { match ->
            val sector = match.groupValues[1].toIntOrNull() ?: return null
            val keyType = match.groupValues[2].uppercase()
            val keyHex = match.groupValues[3].uppercase()
            return KeyRecoveryLine(sector, keyType, keyHex)
        }

        // Pattern 2: Table row "007 | 031 | A0A1A2A3A4A5 | 1 | 00008627C10A | 1"
        keyTablePattern.find(line)?.let { match ->
            val sector = match.groupValues[1].toIntOrNull() ?: return null
            val keyA = match.groupValues[2].uppercase()
            val keyARes = match.groupValues[3]
            val keyB = match.groupValues[4].uppercase()
            val keyBRes = match.groupValues[5]
            // Return key A first; caller will parse both
            if (keyARes == "1") {
                return KeyRecoveryLine(sector, "A", keyA, keyB.takeIf { keyBRes == "1" })
            } else if (keyBRes == "1") {
                return KeyRecoveryLine(sector, "B", keyB)
            }
        }

        return null
    }

    /** Detect if output indicates a Fudan FM11RF08S chip. */
    fun isFudanChip(output: String): Boolean = fudanPattern.containsMatchIn(output)

    /** Detect weak PRNG. */
    fun hasWeakPrng(output: String): Boolean = weakPrngPattern.containsMatchIn(output)

    /** Extract backdoor key if present. */
    fun extractBackdoorKey(output: String): String? =
        backdoorKeyPattern.find(output)?.groupValues?.get(1)?.uppercase()

    private fun normalizeUid(raw: String): String {
        val cleaned = raw.replace(" ", "").replace("-", "").replace(":", "")
        return cleaned.uppercase()
    }
}

/** Parsed key recovery line from PM3 output. */
data class KeyRecoveryLine(
    val sector: Int,
    val keyType: String, // "A" or "B"
    val keyHex: String,
    val otherKeyHex: String? = null // For table rows that include both keys
)
