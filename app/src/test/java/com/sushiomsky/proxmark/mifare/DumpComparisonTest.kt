package com.sushiomsky.proxmark.mifare

import org.junit.Assert.*
import org.junit.Test

class DumpComparisonTest {

    @Test
    fun `compare identical dumps`() {
        val hexDump = "8C25D199E1080400".padEnd(2048, '0')
        val dump1 = MifareClassicDump.fromHexString(hexDump)
        val dump2 = MifareClassicDump.fromHexString(hexDump)
        
        assertNotNull(dump1)
        assertNotNull(dump2)
        
        val comparison = DumpComparison.compare(dump1!!, dump2!!)
        
        assertFalse(comparison.hasDifferences)
        assertEquals(0, comparison.changedBlockCount)
        assertEquals(0, comparison.changedSectors.size)
        assertEquals("Dumps are identical", comparison.getSummary())
    }

    @Test
    fun `detect single block change`() {
        val hexBytes1 = ByteArray(1024) { 0 }
        val hexBytes2 = ByteArray(1024) { 0 }
        
        // Set UIDs
        hexBytes1[0] = 0xAA.toByte()
        hexBytes2[0] = 0xAA.toByte()
        
        // Change one byte in block 4 (sector 1, block 0)
        hexBytes2[64] = 0x42.toByte() // Block 4 starts at offset 64
        
        val dump1 = MifareClassicDump.fromHexString(hexBytes1.joinToString("") { "%02X".format(it) })
        val dump2 = MifareClassicDump.fromHexString(hexBytes2.joinToString("") { "%02X".format(it) })
        
        assertNotNull(dump1)
        assertNotNull(dump2)
        
        val comparison = DumpComparison.compare(dump1!!, dump2!!)
        
        assertTrue(comparison.hasDifferences)
        assertEquals(1, comparison.changedBlockCount)
        assertEquals(1, comparison.changedSectors.size)
        assertTrue(comparison.changedSectors.contains(1))
        
        val diff = comparison.differences[0]
        assertEquals(1, diff.sectorNumber)
        assertEquals(0, diff.blockNumber)
        assertEquals(4, diff.absoluteBlockNumber)
        assertEquals(1, diff.byteDifferences.size)
        assertEquals(0, diff.byteDifferences[0].offset)
    }

    @Test
    fun `detect multiple sector changes`() {
        val hexBytes1 = ByteArray(1024) { 0 }
        val hexBytes2 = ByteArray(1024) { 0 }
        
        hexBytes1[0] = 0xAA.toByte()
        hexBytes2[0] = 0xAA.toByte()
        
        // Change sector 0, block 1
        hexBytes2[16] = 0x11.toByte()
        // Change sector 7, block 0
        hexBytes2[28 * 16] = 0x77.toByte()
        
        val dump1 = MifareClassicDump.fromHexString(hexBytes1.joinToString("") { "%02X".format(it) })
        val dump2 = MifareClassicDump.fromHexString(hexBytes2.joinToString("") { "%02X".format(it) })
        
        assertNotNull(dump1)
        assertNotNull(dump2)
        
        val comparison = DumpComparison.compare(dump1!!, dump2!!)
        
        assertTrue(comparison.hasDifferences)
        assertEquals(2, comparison.changedBlockCount)
        assertEquals(2, comparison.changedSectors.size)
        assertTrue(comparison.changedSectors.contains(0))
        assertTrue(comparison.changedSectors.contains(7))
    }

    @Test
    fun `trailer changes are detected separately`() {
        val hexBytes1 = ByteArray(1024) { 0 }
        val hexBytes2 = ByteArray(1024) { 0 }
        
        hexBytes1[0] = 0xAA.toByte()
        hexBytes2[0] = 0xAA.toByte()
        
        // Change sector 0 trailer (block 3)
        val trailerOffset = 3 * 16
        hexBytes2[trailerOffset] = 0xFF.toByte()
        
        val dump1 = MifareClassicDump.fromHexString(hexBytes1.joinToString("") { "%02X".format(it) })
        val dump2 = MifareClassicDump.fromHexString(hexBytes2.joinToString("") { "%02X".format(it) })
        
        assertNotNull(dump1)
        assertNotNull(dump2)
        
        val comparison = DumpComparison.compare(dump1!!, dump2!!)
        
        assertTrue(comparison.hasDifferences)
        assertEquals(1, comparison.changedBlockCount)
        
        val diff = comparison.differences[0]
        assertTrue(diff.isTrailer)
        assertEquals(3, diff.blockNumber)
    }

    @Test
    fun `getDifferencesBySector groups correctly`() {
        val hexBytes1 = ByteArray(1024) { 0 }
        val hexBytes2 = ByteArray(1024) { 0 }
        
        hexBytes1[0] = 0xAA.toByte()
        hexBytes2[0] = 0xAA.toByte()
        
        // Change sector 0, blocks 1 and 2
        hexBytes2[16] = 0x11.toByte()
        hexBytes2[32] = 0x22.toByte()
        // Change sector 1, block 0
        hexBytes2[64] = 0x33.toByte()
        
        val dump1 = MifareClassicDump.fromHexString(hexBytes1.joinToString("") { "%02X".format(it) })
        val dump2 = MifareClassicDump.fromHexString(hexBytes2.joinToString("") { "%02X".format(it) })
        
        assertNotNull(dump1)
        assertNotNull(dump2)
        
        val comparison = DumpComparison.compare(dump1!!, dump2!!)
        val grouped = comparison.getDifferencesBySector()
        
        assertEquals(2, grouped.size)
        assertEquals(2, grouped[0]?.size)
        assertEquals(1, grouped[1]?.size)
    }
}
