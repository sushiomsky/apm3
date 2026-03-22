package com.sushiomsky.proxmark.mifare

data class DumpComparison(
    val dump1: MifareClassicDump,
    val dump2: MifareClassicDump,
    val differences: List<BlockDifference>
) {
    val hasDifferences: Boolean = differences.isNotEmpty()
    val changedSectors: Set<Int> = differences.map { it.sectorNumber }.toSet()
    val changedBlockCount: Int = differences.size

    companion object {
        fun compare(dump1: MifareClassicDump, dump2: MifareClassicDump): DumpComparison {
            val differences = mutableListOf<BlockDifference>()

            for (sectorNum in dump1.sectors.indices) {
                val sector1 = dump1.sectors[sectorNum]
                val sector2 = dump2.sectors[sectorNum]

                for (blockNum in 0..2) { // Compare data blocks only (0-2), skip trailer (3)
                    val block1 = sector1.blocks[blockNum]
                    val block2 = sector2.blocks[blockNum]

                    if (!block1.data.contentEquals(block2.data)) {
                        differences.add(
                            BlockDifference(
                                sectorNumber = sectorNum,
                                blockNumber = blockNum,
                                absoluteBlockNumber = block1.number,
                                oldData = block1.data,
                                newData = block2.data,
                                byteDifferences = findByteDifferences(block1.data, block2.data)
                            )
                        )
                    }
                }

                // Compare trailer separately (keys and access bits)
                val trailer1 = sector1.blocks[3]
                val trailer2 = sector2.blocks[3]
                
                if (!trailer1.data.contentEquals(trailer2.data)) {
                    val trailerDiff = BlockDifference(
                        sectorNumber = sectorNum,
                        blockNumber = 3,
                        absoluteBlockNumber = trailer1.number,
                        oldData = trailer1.data,
                        newData = trailer2.data,
                        byteDifferences = findByteDifferences(trailer1.data, trailer2.data),
                        isTrailer = true
                    )
                    differences.add(trailerDiff)
                }
            }

            return DumpComparison(dump1, dump2, differences)
        }

        private fun findByteDifferences(data1: ByteArray, data2: ByteArray): List<ByteDifference> {
            val diffs = mutableListOf<ByteDifference>()
            for (i in data1.indices) {
                if (data1[i] != data2[i]) {
                    diffs.add(
                        ByteDifference(
                            offset = i,
                            oldValue = data1[i],
                            newValue = data2[i]
                        )
                    )
                }
            }
            return diffs
        }
    }

    fun getSummary(): String {
        if (!hasDifferences) return "Dumps are identical"
        
        val sectorText = if (changedSectors.size == 1) "1 sector" else "${changedSectors.size} sectors"
        val blockText = if (changedBlockCount == 1) "1 block" else "$changedBlockCount blocks"
        return "Changed: $sectorText, $blockText"
    }

    fun getDifferencesBySector(): Map<Int, List<BlockDifference>> {
        return differences.groupBy { it.sectorNumber }
    }
}

data class BlockDifference(
    val sectorNumber: Int,
    val blockNumber: Int,
    val absoluteBlockNumber: Int,
    val oldData: ByteArray,
    val newData: ByteArray,
    val byteDifferences: List<ByteDifference>,
    val isTrailer: Boolean = false
) {
    val changeCount: Int = byteDifferences.size

    fun getOldHex(): String = oldData.joinToString(" ") { "%02X".format(it) }
    fun getNewHex(): String = newData.joinToString(" ") { "%02X".format(it) }

    fun getChangeSummary(): String {
        if (isTrailer) {
            return "Trailer changed (${changeCount} bytes)"
        }
        return "Data changed (${changeCount} bytes)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as BlockDifference
        return absoluteBlockNumber == other.absoluteBlockNumber &&
                oldData.contentEquals(other.oldData) &&
                newData.contentEquals(other.newData)
    }

    override fun hashCode(): Int {
        var result = absoluteBlockNumber
        result = 31 * result + oldData.contentHashCode()
        result = 31 * result + newData.contentHashCode()
        return result
    }
}

data class ByteDifference(
    val offset: Int,
    val oldValue: Byte,
    val newValue: Byte
) {
    fun getOldHex(): String = "%02X".format(oldValue)
    fun getNewHex(): String = "%02X".format(newValue)
}
