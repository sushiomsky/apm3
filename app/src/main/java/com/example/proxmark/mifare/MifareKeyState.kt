package com.example.proxmark.mifare

/**
 * Tracks MIFARE Classic key recovery state across all sectors.
 * Used by the sector map visualization and key recovery workflows.
 */
data class MifareKeyState(
    val cardType: CardType = CardType.CLASSIC_1K,
    val sectors: List<SectorKeyState> = List(16) { SectorKeyState(it) },
    val attackMethod: AttackMethod = AttackMethod.IDLE,
    val currentSector: Int = -1,
    val foundKeysCount: Int = 0,
    val totalKeysNeeded: Int = 32 // 16 sectors × 2 keys (A+B)
) {
    val progress: Float get() = if (totalKeysNeeded > 0) foundKeysCount.toFloat() / totalKeysNeeded else 0f
    val isComplete: Boolean get() = foundKeysCount >= totalKeysNeeded
    val isRunning: Boolean get() = attackMethod != AttackMethod.IDLE

    fun sectorCount(): Int = when (cardType) {
        CardType.CLASSIC_1K -> 16
        CardType.CLASSIC_4K -> 40
        CardType.CLASSIC_MINI -> 5
    }

    fun withKeyFound(sector: Int, keyType: KeyType, key: ByteArray): MifareKeyState {
        val updated = sectors.toMutableList()
        if (sector in updated.indices) {
            updated[sector] = when (keyType) {
                KeyType.KEY_A -> updated[sector].copy(
                    keyA = key.toHexStr(),
                    keyAStatus = KeyStatus.FOUND
                )
                KeyType.KEY_B -> updated[sector].copy(
                    keyB = key.toHexStr(),
                    keyBStatus = KeyStatus.FOUND
                )
            }
        }
        return copy(
            sectors = updated,
            foundKeysCount = updated.sumOf {
                (if (it.keyAStatus == KeyStatus.FOUND) 1 else 0) +
                (if (it.keyBStatus == KeyStatus.FOUND) 1 else 0)
            }
        )
    }

    fun withSectorInProgress(sector: Int): MifareKeyState {
        val updated = sectors.toMutableList()
        if (sector in updated.indices) {
            val s = updated[sector]
            updated[sector] = s.copy(
                keyAStatus = if (s.keyAStatus == KeyStatus.UNKNOWN) KeyStatus.IN_PROGRESS else s.keyAStatus,
                keyBStatus = if (s.keyBStatus == KeyStatus.UNKNOWN) KeyStatus.IN_PROGRESS else s.keyBStatus
            )
        }
        return copy(sectors = updated, currentSector = sector)
    }

    companion object {
        fun forCardType(type: CardType): MifareKeyState {
            val sectorCount = when (type) {
                CardType.CLASSIC_1K -> 16
                CardType.CLASSIC_4K -> 40
                CardType.CLASSIC_MINI -> 5
            }
            return MifareKeyState(
                cardType = type,
                sectors = List(sectorCount) { SectorKeyState(it) },
                totalKeysNeeded = sectorCount * 2
            )
        }

        /** Pre-fill with default keys (FFFFFFFFFFFF) */
        fun withDefaultKeys(type: CardType = CardType.CLASSIC_1K): MifareKeyState {
            val state = forCardType(type)
            return state.copy(
                sectors = state.sectors.map {
                    it.copy(
                        keyA = "FFFFFFFFFFFF",
                        keyAStatus = KeyStatus.FOUND,
                        keyB = "FFFFFFFFFFFF",
                        keyBStatus = KeyStatus.FOUND
                    )
                },
                foundKeysCount = state.totalKeysNeeded
            )
        }
    }
}

data class SectorKeyState(
    val sectorNumber: Int,
    val keyA: String? = null,
    val keyB: String? = null,
    val keyAStatus: KeyStatus = KeyStatus.UNKNOWN,
    val keyBStatus: KeyStatus = KeyStatus.UNKNOWN,
    val hasData: Boolean = false
) {
    val overallStatus: SectorStatus get() = when {
        keyAStatus == KeyStatus.FOUND && keyBStatus == KeyStatus.FOUND -> SectorStatus.COMPLETE
        keyAStatus == KeyStatus.FOUND || keyBStatus == KeyStatus.FOUND -> SectorStatus.PARTIAL
        keyAStatus == KeyStatus.IN_PROGRESS || keyBStatus == KeyStatus.IN_PROGRESS -> SectorStatus.ATTACKING
        keyAStatus == KeyStatus.FAILED && keyBStatus == KeyStatus.FAILED -> SectorStatus.LOCKED
        else -> SectorStatus.UNKNOWN
    }
}

enum class KeyStatus { UNKNOWN, IN_PROGRESS, FOUND, FAILED }

enum class SectorStatus { UNKNOWN, ATTACKING, PARTIAL, COMPLETE, LOCKED }

enum class KeyType { KEY_A, KEY_B }

enum class CardType(val label: String, val totalBlocks: Int) {
    CLASSIC_1K("MIFARE Classic 1K", 64),
    CLASSIC_4K("MIFARE Classic 4K", 256),
    CLASSIC_MINI("MIFARE Mini", 20)
}

enum class AttackMethod(val label: String) {
    IDLE("Idle"),
    DICTIONARY("Dictionary Attack"),
    NESTED("Nested Attack"),
    HARDNESTED("Hardnested Attack"),
    DARKSIDE("Darkside Attack"),
    STATIC_NESTED("Static Nested")
}

private fun ByteArray.toHexStr(): String = joinToString("") { "%02X".format(it) }
