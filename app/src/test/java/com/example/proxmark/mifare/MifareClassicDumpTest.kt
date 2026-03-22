package com.example.proxmark.mifare

import org.junit.Assert.*
import org.junit.Test

class MifareClassicDumpTest {

    @Test
    fun `parse valid 1K hex dump`() {
        val hexDump = "8C25D199E1080400".padEnd(2048, '0')
        val dump = MifareClassicDump.fromHexString(hexDump)
        
        assertNotNull(dump)
        assertEquals("8C25D199", dump!!.uid)
        assertEquals(16, dump.sectors.size)
    }

    @Test
    fun `identify custom key sectors`() {
        val hexBytes = ByteArray(1024) { 0 }
        hexBytes[0] = 0x8C.toByte()
        hexBytes[1] = 0x25.toByte()
        hexBytes[2] = 0xD1.toByte()
        hexBytes[3] = 0x99.toByte()
        
        val trailerOffset = 31 * 16
        hexBytes[trailerOffset] = 0xA0.toByte()
        hexBytes[trailerOffset + 1] = 0xA1.toByte()
        hexBytes[trailerOffset + 2] = 0xA2.toByte()
        hexBytes[trailerOffset + 3] = 0xA3.toByte()
        hexBytes[trailerOffset + 4] = 0xA4.toByte()
        hexBytes[trailerOffset + 5] = 0xA5.toByte()
        hexBytes[trailerOffset + 10] = 0x12.toByte()
        hexBytes[trailerOffset + 11] = 0x34.toByte()
        hexBytes[trailerOffset + 12] = 0x56.toByte()
        hexBytes[trailerOffset + 13] = 0x78.toByte()
        hexBytes[trailerOffset + 14] = 0x9A.toByte()
        hexBytes[trailerOffset + 15] = 0xBC.toByte()
        
        val hexString = hexBytes.joinToString("") { "%02X".format(it) }
        val dump = MifareClassicDump.fromHexString(hexString)
        
        assertNotNull(dump)
        // All sectors with zeroed keys (000000000000) will be considered custom (non-default)
        // since default is FFFFFFFFFFFF. So we should have 16 custom key sectors,
        // including sector 7 which has A0A1A2A3A4A5/123456789ABC
        val customKeySectors = dump!!.getCustomKeySectors()
        assertTrue("Should have custom key sectors", customKeySectors.isNotEmpty())
        
        // Check sector 7 specifically
        val sector7 = dump.sectors[7]
        assertEquals("A0A1A2A3A4A5", sector7.keyA)
        assertEquals("123456789ABC", sector7.keyB)
        assertFalse(sector7.isDefaultKeyA)
        assertFalse(sector7.isDefaultKeyB)
    }

    @Test
    fun `reject invalid length dumps`() {
        val shortHex = "8C25D199E108"
        val dump = MifareClassicDump.fromHexString(shortHex)
        assertNull(dump)
    }

    @Test
    fun `default keys are detected correctly`() {
        // All zeros except UID -> all trailers should be 000000000000 (not default FFFFFFFFFFFF)
        val hexBytes = ByteArray(1024) { 0 }
        hexBytes[0] = 0xAA.toByte()
        hexBytes[1] = 0xBB.toByte()
        hexBytes[2] = 0xCC.toByte()
        hexBytes[3] = 0xDD.toByte()
        
        val hexString = hexBytes.joinToString("") { "%02X".format(it) }
        val dump = MifareClassicDump.fromHexString(hexString)
        
        assertNotNull(dump)
        val sector0 = dump!!.sectors[0]
        assertEquals("000000000000", sector0.keyA)
        assertEquals("000000000000", sector0.keyB)
        // Both should NOT be default (default is FFFFFFFFFFFF)
        assertFalse(sector0.isDefaultKeyA)
        assertFalse(sector0.isDefaultKeyB)
    }

    @Test
    fun `block toAsciiString converts printable chars`() {
        val data = byteArrayOf(0x48, 0x65, 0x6C, 0x6C, 0x6F, 0x20, 0x00, 0x01)
        val block = MifareBlock(0, data)
        val ascii = block.toAsciiString()
        assertTrue(ascii.contains("Hello"))
    }
}
