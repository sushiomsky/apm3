package com.sushiomsky.proxmark.mifare

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MifareKeyStateTest {

    // ── Default / factory state ──

    @Test
    fun `default state has 16 sectors for CLASSIC_1K`() {
        val state = MifareKeyState()

        assertEquals(CardType.CLASSIC_1K, state.cardType)
        assertEquals(16, state.sectors.size)
        assertEquals(32, state.totalKeysNeeded)
    }

    @Test
    fun `forCardType CLASSIC_1K creates 16 sectors`() {
        val state = MifareKeyState.forCardType(CardType.CLASSIC_1K)

        assertEquals(16, state.sectors.size)
        assertEquals(32, state.totalKeysNeeded)
        assertEquals(CardType.CLASSIC_1K, state.cardType)
    }

    @Test
    fun `forCardType CLASSIC_4K creates 40 sectors`() {
        val state = MifareKeyState.forCardType(CardType.CLASSIC_4K)

        assertEquals(40, state.sectors.size)
        assertEquals(80, state.totalKeysNeeded)
        assertEquals(CardType.CLASSIC_4K, state.cardType)
    }

    @Test
    fun `forCardType CLASSIC_MINI creates 5 sectors`() {
        val state = MifareKeyState.forCardType(CardType.CLASSIC_MINI)

        assertEquals(5, state.sectors.size)
        assertEquals(10, state.totalKeysNeeded)
        assertEquals(CardType.CLASSIC_MINI, state.cardType)
    }

    @Test
    fun `sectorCount returns correct value for each card type`() {
        assertEquals(16, MifareKeyState.forCardType(CardType.CLASSIC_1K).sectorCount())
        assertEquals(40, MifareKeyState.forCardType(CardType.CLASSIC_4K).sectorCount())
        assertEquals(5, MifareKeyState.forCardType(CardType.CLASSIC_MINI).sectorCount())
    }

    @Test
    fun `default state sectors all have UNKNOWN status`() {
        val state = MifareKeyState()

        state.sectors.forEach { sector ->
            assertEquals(KeyStatus.UNKNOWN, sector.keyAStatus)
            assertEquals(KeyStatus.UNKNOWN, sector.keyBStatus)
            assertNull(sector.keyA)
            assertNull(sector.keyB)
            assertEquals(SectorStatus.UNKNOWN, sector.overallStatus)
        }
    }

    @Test
    fun `default state has zero found keys`() {
        val state = MifareKeyState()

        assertEquals(0, state.foundKeysCount)
    }

    @Test
    fun `default state attack method is IDLE`() {
        val state = MifareKeyState()

        assertEquals(AttackMethod.IDLE, state.attackMethod)
    }

    // ── withKeyFound ──

    @Test
    fun `withKeyFound updates KEY_A for correct sector`() {
        val key = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
        val state = MifareKeyState().withKeyFound(0, KeyType.KEY_A, key)

        assertEquals("FFFFFFFFFFFF", state.sectors[0].keyA)
        assertEquals(KeyStatus.FOUND, state.sectors[0].keyAStatus)
        assertEquals(KeyStatus.UNKNOWN, state.sectors[0].keyBStatus)
    }

    @Test
    fun `withKeyFound updates KEY_B for correct sector`() {
        val key = byteArrayOf(0xA0.toByte(), 0xB1.toByte(), 0xC2.toByte(), 0xD3.toByte(), 0xE4.toByte(), 0xF5.toByte())
        val state = MifareKeyState().withKeyFound(5, KeyType.KEY_B, key)

        assertEquals("A0B1C2D3E4F5", state.sectors[5].keyB)
        assertEquals(KeyStatus.FOUND, state.sectors[5].keyBStatus)
        assertEquals(KeyStatus.UNKNOWN, state.sectors[5].keyAStatus)
    }

    @Test
    fun `withKeyFound increments foundKeysCount`() {
        val key = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())

        val state1 = MifareKeyState().withKeyFound(0, KeyType.KEY_A, key)
        assertEquals(1, state1.foundKeysCount)

        val state2 = state1.withKeyFound(0, KeyType.KEY_B, key)
        assertEquals(2, state2.foundKeysCount)

        val state3 = state2.withKeyFound(1, KeyType.KEY_A, key)
        assertEquals(3, state3.foundKeysCount)
    }

    @Test
    fun `withKeyFound does not change other sectors`() {
        val key = byteArrayOf(0xAA.toByte(), 0xBB.toByte(), 0xCC.toByte(), 0xDD.toByte(), 0xEE.toByte(), 0xFF.toByte())
        val state = MifareKeyState().withKeyFound(3, KeyType.KEY_A, key)

        for (i in state.sectors.indices) {
            if (i != 3) {
                assertEquals(KeyStatus.UNKNOWN, state.sectors[i].keyAStatus)
                assertEquals(KeyStatus.UNKNOWN, state.sectors[i].keyBStatus)
            }
        }
    }

    @Test
    fun `withKeyFound for both KEY_A and KEY_B sets sector status COMPLETE`() {
        val key = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())

        val state = MifareKeyState()
            .withKeyFound(2, KeyType.KEY_A, key)
            .withKeyFound(2, KeyType.KEY_B, key)

        assertEquals(SectorStatus.COMPLETE, state.sectors[2].overallStatus)
        assertEquals(2, state.foundKeysCount)
    }

    @Test
    fun `withKeyFound for only KEY_A sets sector status PARTIAL`() {
        val key = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
        val state = MifareKeyState().withKeyFound(0, KeyType.KEY_A, key)

        assertEquals(SectorStatus.PARTIAL, state.sectors[0].overallStatus)
    }

    @Test
    fun `withKeyFound out of bounds does not crash`() {
        val key = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
        val state = MifareKeyState().withKeyFound(99, KeyType.KEY_A, key)

        assertEquals(0, state.foundKeysCount)
    }

    // ── withSectorInProgress ──

    @Test
    fun `withSectorInProgress sets sector status to ATTACKING`() {
        val state = MifareKeyState().withSectorInProgress(4)

        assertEquals(KeyStatus.IN_PROGRESS, state.sectors[4].keyAStatus)
        assertEquals(KeyStatus.IN_PROGRESS, state.sectors[4].keyBStatus)
        assertEquals(SectorStatus.ATTACKING, state.sectors[4].overallStatus)
    }

    @Test
    fun `withSectorInProgress updates currentSector`() {
        val state = MifareKeyState().withSectorInProgress(7)

        assertEquals(7, state.currentSector)
    }

    @Test
    fun `withSectorInProgress does not overwrite already FOUND keys`() {
        val key = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
        val state = MifareKeyState()
            .withKeyFound(3, KeyType.KEY_A, key)
            .withSectorInProgress(3)

        assertEquals(KeyStatus.FOUND, state.sectors[3].keyAStatus)
        assertEquals(KeyStatus.IN_PROGRESS, state.sectors[3].keyBStatus)
    }

    @Test
    fun `withSectorInProgress out of bounds does not crash`() {
        val state = MifareKeyState().withSectorInProgress(99)

        assertEquals(99, state.currentSector)
    }

    // ── withDefaultKeys ──

    @Test
    fun `withDefaultKeys fills all sectors with FFFFFFFFFFFF`() {
        val state = MifareKeyState.withDefaultKeys()

        assertEquals(16, state.sectors.size)
        state.sectors.forEach { sector ->
            assertEquals("FFFFFFFFFFFF", sector.keyA)
            assertEquals("FFFFFFFFFFFF", sector.keyB)
            assertEquals(KeyStatus.FOUND, sector.keyAStatus)
            assertEquals(KeyStatus.FOUND, sector.keyBStatus)
            assertEquals(SectorStatus.COMPLETE, sector.overallStatus)
        }
    }

    @Test
    fun `withDefaultKeys sets foundKeysCount to totalKeysNeeded`() {
        val state = MifareKeyState.withDefaultKeys()

        assertEquals(state.totalKeysNeeded, state.foundKeysCount)
    }

    @Test
    fun `withDefaultKeys for CLASSIC_4K fills all 40 sectors`() {
        val state = MifareKeyState.withDefaultKeys(CardType.CLASSIC_4K)

        assertEquals(40, state.sectors.size)
        assertEquals(80, state.foundKeysCount)
        assertEquals(80, state.totalKeysNeeded)
        state.sectors.forEach { sector ->
            assertEquals("FFFFFFFFFFFF", sector.keyA)
            assertEquals("FFFFFFFFFFFF", sector.keyB)
        }
    }

    @Test
    fun `withDefaultKeys for CLASSIC_MINI fills all 5 sectors`() {
        val state = MifareKeyState.withDefaultKeys(CardType.CLASSIC_MINI)

        assertEquals(5, state.sectors.size)
        assertEquals(10, state.foundKeysCount)
    }

    // ── progress ──

    @Test
    fun `progress is zero when no keys found`() {
        val state = MifareKeyState()

        assertEquals(0f, state.progress, 0.001f)
    }

    @Test
    fun `progress is correct after finding some keys`() {
        val key = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
        val state = MifareKeyState()
            .withKeyFound(0, KeyType.KEY_A, key)
            .withKeyFound(0, KeyType.KEY_B, key)

        // 2 keys found out of 32 total
        assertEquals(2f / 32f, state.progress, 0.001f)
    }

    @Test
    fun `progress is 1 when all keys found`() {
        val state = MifareKeyState.withDefaultKeys()

        assertEquals(1f, state.progress, 0.001f)
    }

    // ── isComplete ──

    @Test
    fun `isComplete is false when no keys found`() {
        assertFalse(MifareKeyState().isComplete)
    }

    @Test
    fun `isComplete is false when partially recovered`() {
        val key = byteArrayOf(0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte(), 0xFF.toByte())
        val state = MifareKeyState().withKeyFound(0, KeyType.KEY_A, key)

        assertFalse(state.isComplete)
    }

    @Test
    fun `isComplete is true when all keys found`() {
        assertTrue(MifareKeyState.withDefaultKeys().isComplete)
    }

    // ── isRunning ──

    @Test
    fun `isRunning is false when attackMethod is IDLE`() {
        assertFalse(MifareKeyState().isRunning)
    }

    @Test
    fun `isRunning is true for DICTIONARY attack`() {
        assertTrue(MifareKeyState(attackMethod = AttackMethod.DICTIONARY).isRunning)
    }

    @Test
    fun `isRunning is true for NESTED attack`() {
        assertTrue(MifareKeyState(attackMethod = AttackMethod.NESTED).isRunning)
    }

    @Test
    fun `isRunning is true for HARDNESTED attack`() {
        assertTrue(MifareKeyState(attackMethod = AttackMethod.HARDNESTED).isRunning)
    }

    @Test
    fun `isRunning is true for DARKSIDE attack`() {
        assertTrue(MifareKeyState(attackMethod = AttackMethod.DARKSIDE).isRunning)
    }

    @Test
    fun `isRunning is true for STATIC_NESTED attack`() {
        assertTrue(MifareKeyState(attackMethod = AttackMethod.STATIC_NESTED).isRunning)
    }

    // ── SectorKeyState overallStatus ──

    @Test
    fun `sector with both keys FAILED has LOCKED status`() {
        val sector = SectorKeyState(0, keyAStatus = KeyStatus.FAILED, keyBStatus = KeyStatus.FAILED)

        assertEquals(SectorStatus.LOCKED, sector.overallStatus)
    }

    @Test
    fun `sector with one key IN_PROGRESS has ATTACKING status`() {
        val sector = SectorKeyState(0, keyAStatus = KeyStatus.IN_PROGRESS)

        assertEquals(SectorStatus.ATTACKING, sector.overallStatus)
    }

    // ── CardType enum values ──

    @Test
    fun `CardType CLASSIC_1K has 64 blocks`() {
        assertEquals(64, CardType.CLASSIC_1K.totalBlocks)
    }

    @Test
    fun `CardType CLASSIC_4K has 256 blocks`() {
        assertEquals(256, CardType.CLASSIC_4K.totalBlocks)
    }

    @Test
    fun `CardType CLASSIC_MINI has 20 blocks`() {
        assertEquals(20, CardType.CLASSIC_MINI.totalBlocks)
    }

    // ── SectorKeyState sectorNumber preserved ──

    @Test
    fun `forCardType sectors have sequential sector numbers`() {
        val state = MifareKeyState.forCardType(CardType.CLASSIC_1K)

        state.sectors.forEachIndexed { index, sector ->
            assertEquals(index, sector.sectorNumber)
        }
    }
}
