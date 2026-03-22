package com.example.proxmark.protocol

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProxmarkOutputParserTest {

    @Test
    fun `extractUid normalizes separated uid`() {
        val output = "UID : 04 9a 1B 2c 3d"

        val uid = ProxmarkOutputParser.extractUid(output)

        assertEquals("049A1B2C3D", uid)
    }

    @Test
    fun `extractUid supports TAG ID pattern`() {
        val output = "Tag ID=deadbeef01"

        val uid = ProxmarkOutputParser.extractUid(output)

        assertEquals("DEADBEEF01", uid)
    }

    @Test
    fun `extractUid returns null when no uid is present`() {
        val output = "No tag found"

        val uid = ProxmarkOutputParser.extractUid(output)

        assertNull(uid)
    }

    @Test
    fun `parseTagInfo extracts uid and mifare classic type`() {
        val output = """
            [=] some data
            UID: 04 A1 B2 C3 D4
            [=] type: MIFARE CLASSIC
        """.trimIndent()

        val parsed = ProxmarkOutputParser.parseTagInfo(output)

        requireNotNull(parsed)
        assertEquals("04A1B2C3D4", parsed.uid)
        assertEquals("Mifare Classic 1K", parsed.type)
    }

    @Test
    fun `parseTagInfo defaults unknown type`() {
        val output = """
            UID: 11223344
            [=] protocol: custom
        """.trimIndent()

        val parsed = ProxmarkOutputParser.parseTagInfo(output)

        requireNotNull(parsed)
        assertEquals("11223344", parsed.uid)
        assertEquals("Unknown", parsed.type)
    }

    @Test
    fun `extractAtqa parses ATQA value`() {
        val output = "ATQA: 00 04"
        val atqa = ProxmarkOutputParser.extractAtqa(output)
        assertEquals("0004", atqa)
    }

    @Test
    fun `extractSak parses SAK value`() {
        val output = "SAK: 08"
        val sak = ProxmarkOutputParser.extractSak(output)
        assertEquals("08", sak)
    }

    @Test
    fun `parseTagInfo includes atqa and sak when present`() {
        val output = """
            UID: 04 A1 B2 C3 D4 E5 F6
            ATQA: 00 44
            SAK: 08
            TYPE: MIFARE Classic 1K
        """.trimIndent()

        val parsed = ProxmarkOutputParser.parseTagInfo(output)

        requireNotNull(parsed)
        assertEquals("04A1B2C3D4E5F6", parsed.uid)
        assertEquals("Mifare Classic 1K", parsed.type)
        assertEquals("0044", parsed.atqa)
        assertEquals("08", parsed.sak)
    }

    @Test
    fun `extractTagType detects additional types`() {
        assertEquals("NTAG", ProxmarkOutputParser.extractTagType("NTAG215 detected"))
        assertEquals("Mifare Plus", ProxmarkOutputParser.extractTagType("Mifare Plus 2K"))
        assertEquals("iCLASS", ProxmarkOutputParser.extractTagType("iCLASS Standard"))
        assertEquals("FeliCa", ProxmarkOutputParser.extractTagType("FeliCa Lite-S"))
        assertEquals("ISO15693", ProxmarkOutputParser.extractTagType("ISO15693 tag found"))
        assertEquals("Indala", ProxmarkOutputParser.extractTagType("Indala 26-bit"))
    }

    @Test
    fun `extractAtqa returns null when not present`() {
        assertNull(ProxmarkOutputParser.extractAtqa("UID: 04 A1 B2 C3"))
    }

    @Test
    fun `extractSak returns null when not present`() {
        assertNull(ProxmarkOutputParser.extractSak("UID: 04 A1 B2 C3"))
    }

    // ── Key Recovery Parsing ──────────────────────────────────────────

    @Test
    fun `parseKeyLine detects sector key line`() {
        val line = "[=] Sector  7 keyA = A0A1A2A3A4A5"
        val result = ProxmarkOutputParser.parseKeyLine(line)

        requireNotNull(result)
        assertEquals(7, result.sector)
        assertEquals("A", result.keyType)
        assertEquals("A0A1A2A3A4A5", result.keyHex)
    }

    @Test
    fun `parseKeyLine detects key B`() {
        val line = "[=] Sector 12 keyB = 3E65E4FB65B3"
        val result = ProxmarkOutputParser.parseKeyLine(line)

        requireNotNull(result)
        assertEquals(12, result.sector)
        assertEquals("B", result.keyType)
        assertEquals("3E65E4FB65B3", result.keyHex)
    }

    @Test
    fun `parseKeyLine parses table row with both keys`() {
        val line = "[+]  007 | 031 | A0A1A2A3A4A5 | 1 | 00008627C10A | 1"
        val result = ProxmarkOutputParser.parseKeyLine(line)

        requireNotNull(result)
        assertEquals(7, result.sector)
        assertEquals("A", result.keyType)
        assertEquals("A0A1A2A3A4A5", result.keyHex)
        assertEquals("00008627C10A", result.otherKeyHex)
    }

    @Test
    fun `parseKeyLine parses default key table row`() {
        val line = "[+]  000 | 003 | FFFFFFFFFFFF | 1 | FFFFFFFFFFFF | 1 "
        val result = ProxmarkOutputParser.parseKeyLine(line)

        requireNotNull(result)
        assertEquals(0, result.sector)
        assertEquals("A", result.keyType)
        assertEquals("FFFFFFFFFFFF", result.keyHex)
        assertEquals("FFFFFFFFFFFF", result.otherKeyHex)
    }

    @Test
    fun `parseKeyLine returns null for non-key lines`() {
        assertNull(ProxmarkOutputParser.parseKeyLine("[=] some other output"))
        assertNull(ProxmarkOutputParser.parseKeyLine("-----+-----+--------------+---+--------------+----"))
        assertNull(ProxmarkOutputParser.parseKeyLine(""))
    }

    // ── Fudan / Chip Detection ──────────────────────────────────────────

    @Test
    fun `extractTagType detects Fudan FM11RF08S`() {
        assertEquals("FM11RF08S (Fudan)", ProxmarkOutputParser.extractTagType("Fudan FM11RF08S 0490"))
    }

    @Test
    fun `isFudanChip detects fingerprint line`() {
        val output = "[+] Fudan FM11RF08S 0490"
        assertEquals(true, ProxmarkOutputParser.isFudanChip(output))
    }

    @Test
    fun `isFudanChip returns false for non-Fudan`() {
        assertEquals(false, ProxmarkOutputParser.isFudanChip("MIFARE Classic 1K"))
    }

    @Test
    fun `extractBackdoorKey finds known backdoor`() {
        val output = "[+] Backdoor key..... A396EFA4E24F"
        assertEquals("A396EFA4E24F", ProxmarkOutputParser.extractBackdoorKey(output))
    }

    @Test
    fun `hasWeakPrng detects weak PRNG`() {
        assertEquals(true, ProxmarkOutputParser.hasWeakPrng("[+] Prng....... weak"))
        assertEquals(false, ProxmarkOutputParser.hasWeakPrng("[+] Prng....... hard"))
    }

    @Test
    fun `extractTagType detects Mifare Classic 1K vs 4K`() {
        assertEquals("Mifare Classic 1K", ProxmarkOutputParser.extractTagType("MIFARE Classic 1K"))
        assertEquals("Mifare Classic 4K", ProxmarkOutputParser.extractTagType("MIFARE Classic 4K"))
        assertEquals("Mifare Classic 1K", ProxmarkOutputParser.extractTagType("mifare classic"))
    }
}
