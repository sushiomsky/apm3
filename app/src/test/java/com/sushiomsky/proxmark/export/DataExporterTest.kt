package com.sushiomsky.proxmark.export

import com.sushiomsky.proxmark.mifare.CardType
import com.sushiomsky.proxmark.mifare.KeyStatus
import com.sushiomsky.proxmark.mifare.MifareBlock
import com.sushiomsky.proxmark.mifare.MifareClassicDump
import com.sushiomsky.proxmark.mifare.MifareKeyState
import com.sushiomsky.proxmark.mifare.MifareSector
import com.sushiomsky.proxmark.mifare.SectorKeyState
import com.sushiomsky.proxmark.protocol.TerminalLine
import com.sushiomsky.proxmark.protocol.TerminalLineType
import com.sushiomsky.proxmark.repository.TagInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DataExporterTest {

    // ── exportTagsAsJson ──

    @Test
    fun `exportTagsAsJson with empty list returns empty array`() {
        assertEquals("[]", DataExporter.exportTagsAsJson(emptyList()))
    }

    @Test
    fun `exportTagsAsJson produces valid JSON structure`() {
        val tags = listOf(
            TagInfo(uid = "AABBCCDD", type = "Mifare Classic 1K", atqa = "0004", sak = "08", timestamp = 1000L),
            TagInfo(uid = "11223344", type = "NTAG215", timestamp = 2000L, note = "Test tag")
        )
        val json = DataExporter.exportTagsAsJson(tags)
        assertTrue(json.startsWith("["))
        assertTrue(json.endsWith("]"))
        assertTrue(json.contains("\"uid\":\"AABBCCDD\""))
        assertTrue(json.contains("\"type\":\"Mifare Classic 1K\""))
        assertTrue(json.contains("\"atqa\":\"0004\""))
        assertTrue(json.contains("\"sak\":\"08\""))
        assertTrue(json.contains("\"timestamp\":1000"))
        assertTrue(json.contains("\"uid\":\"11223344\""))
        assertTrue(json.contains("\"note\":\"Test tag\""))
    }

    @Test
    fun `exportTagsAsJson escapes special characters`() {
        val tags = listOf(
            TagInfo(uid = "AABB", type = "Test \"type\"", timestamp = 1000L, note = "Line1\nLine2")
        )
        val json = DataExporter.exportTagsAsJson(tags)
        assertTrue(json.contains("\\\"type\\\""))
        assertTrue(json.contains("\\n"))
    }

    // ── exportTagsAsCsv ──

    @Test
    fun `exportTagsAsCsv produces header and rows`() {
        val tags = listOf(
            TagInfo(uid = "AABBCCDD", type = "Mifare Classic 1K", atqa = "0004", sak = "08", timestamp = 1000L)
        )
        val csv = DataExporter.exportTagsAsCsv(tags)
        val lines = csv.lines().filter { it.isNotBlank() }
        assertEquals("uid,type,atqa,sak,timestamp,note", lines[0])
        assertTrue(lines[1].startsWith("AABBCCDD,Mifare Classic 1K,0004,08,1000"))
    }

    @Test
    fun `exportTagsAsCsv escapes commas in values`() {
        val tags = listOf(
            TagInfo(uid = "AA", type = "Type, with comma", timestamp = 1000L)
        )
        val csv = DataExporter.exportTagsAsCsv(tags)
        assertTrue(csv.contains("\"Type, with comma\""))
    }

    // ── exportDumpAsHex ──

    @Test
    fun `exportDumpAsHex contains UID and sector headers`() {
        val blocks = (0..3).map { MifareBlock(it, ByteArray(16) { 0xFF.toByte() }) }
        val sector = MifareSector(
            number = 0, blocks = blocks,
            keyA = "FFFFFFFFFFFF", keyB = "FFFFFFFFFFFF",
            accessBits = "FF0780",
            isDefaultKeyA = true, isDefaultKeyB = true
        )
        val dump = MifareClassicDump(uid = "AABBCCDD", sectors = listOf(sector))
        val hex = DataExporter.exportDumpAsHex(dump)
        assertTrue(hex.contains("UID: AABBCCDD"))
        assertTrue(hex.contains("Sector 0"))
        assertTrue(hex.contains("FFFFFFFFFFFFFFFFFFFFFFFFFFFFFFFF"))
    }

    // ── exportKeysAsText ──

    @Test
    fun `exportKeysAsText lists found keys`() {
        val sectors = List(16) { i ->
            if (i == 0) {
                SectorKeyState(i, keyA = "A0A1A2A3A4A5", keyB = "B0B1B2B3B4B5",
                    keyAStatus = KeyStatus.FOUND, keyBStatus = KeyStatus.FOUND)
            } else {
                SectorKeyState(i)
            }
        }
        val state = MifareKeyState(sectors = sectors, foundKeysCount = 2)
        val text = DataExporter.exportKeysAsText(state)
        assertTrue(text.contains("A0A1A2A3A4A5"))
        assertTrue(text.contains("B0B1B2B3B4B5"))
        assertTrue(text.contains("Keys found: 2/32"))
        // Unknown keys show as question marks
        assertTrue(text.contains("????????????"))
    }

    // ── exportTerminalLog ──

    @Test
    fun `exportTerminalLog formats lines with correct prefixes`() {
        val lines = listOf(
            TerminalLine("hf search", TerminalLineType.SENT),
            TerminalLine("ISO14443A Tag Found", TerminalLineType.RECEIVED),
            TerminalLine("Connection error", TerminalLineType.ERROR),
            TerminalLine("System ready", TerminalLineType.SYSTEM)
        )
        val text = DataExporter.exportTerminalLog(lines)
        assertTrue(text.contains("» hf search"))
        assertTrue(text.contains("  ISO14443A Tag Found"))
        assertTrue(text.contains("✖ Connection error"))
        assertTrue(text.contains("• System ready"))
        assertTrue(text.contains("Lines: 4"))
    }

    @Test
    fun `exportTerminalLog with empty list returns header only`() {
        val text = DataExporter.exportTerminalLog(emptyList())
        assertTrue(text.contains("Lines: 0"))
    }
}
