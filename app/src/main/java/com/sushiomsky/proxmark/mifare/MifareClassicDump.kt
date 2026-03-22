package com.sushiomsky.proxmark.mifare

import java.io.File

data class MifareClassicDump(
    val uid: String,
    val sectors: List<MifareSector>
) {
    companion object {
        fun fromBinaryFile(file: File): MifareClassicDump? {
            val bytes = file.readBytes()
            if (bytes.size != 1024) return null
            
            val uid = bytes.slice(0..3).joinToString("") { "%02X".format(it) }
            val sectors = mutableListOf<MifareSector>()
            
            for (sectorNum in 0..15) {
                val startBlock = sectorNum * 4
                val blocks = (0..3).map { blockOffset ->
                    val blockStart = (startBlock + blockOffset) * 16
                    MifareBlock(
                        number = startBlock + blockOffset,
                        data = bytes.sliceArray(blockStart until blockStart + 16)
                    )
                }
                
                val trailer = blocks[3]
                val keyA = trailer.data.sliceArray(0..5).toHexString()
                val keyB = trailer.data.sliceArray(10..15).toHexString()
                val accessBits = trailer.data.sliceArray(6..8)
                
                sectors.add(
                    MifareSector(
                        number = sectorNum,
                        blocks = blocks,
                        keyA = keyA,
                        keyB = keyB,
                        accessBits = accessBits.toHexString(),
                        isDefaultKeyA = keyA.equals("FFFFFFFFFFFF", ignoreCase = true),
                        isDefaultKeyB = keyB.equals("FFFFFFFFFFFF", ignoreCase = true)
                    )
                )
            }
            
            return MifareClassicDump(uid, sectors)
        }
        
        fun fromHexString(hex: String): MifareClassicDump? {
            val cleanHex = hex.replace(Regex("[^0-9A-Fa-f]"), "")
            if (cleanHex.length != 2048) return null
            
            val bytes = cleanHex.chunked(2).map { it.toInt(16).toByte() }.toByteArray()
            
            val uid = bytes.slice(0..3).joinToString("") { "%02X".format(it) }
            val sectors = mutableListOf<MifareSector>()
            
            for (sectorNum in 0..15) {
                val startBlock = sectorNum * 4
                val blocks = (0..3).map { blockOffset ->
                    val blockStart = (startBlock + blockOffset) * 16
                    MifareBlock(
                        number = startBlock + blockOffset,
                        data = bytes.sliceArray(blockStart until blockStart + 16)
                    )
                }
                
                val trailer = blocks[3]
                val keyA = trailer.data.sliceArray(0..5).toHexString()
                val keyB = trailer.data.sliceArray(10..15).toHexString()
                val accessBits = trailer.data.sliceArray(6..8)
                
                sectors.add(
                    MifareSector(
                        number = sectorNum,
                        blocks = blocks,
                        keyA = keyA,
                        keyB = keyB,
                        accessBits = accessBits.toHexString(),
                        isDefaultKeyA = keyA.equals("FFFFFFFFFFFF", ignoreCase = true),
                        isDefaultKeyB = keyB.equals("FFFFFFFFFFFF", ignoreCase = true)
                    )
                )
            }
            
            return MifareClassicDump(uid, sectors)
        }
    }
    
    fun hasNonEmptyData(): Boolean = sectors.any { sector ->
        sector.blocks.dropLast(1).any { block ->
            block.data.any { it != 0.toByte() }
        }
    }
    
    fun getActiveSectors(): List<MifareSector> = sectors.filter { sector ->
        sector.blocks.dropLast(1).any { block ->
            block.data.any { it != 0.toByte() }
        } || !sector.isDefaultKeyA || !sector.isDefaultKeyB
    }
    
    fun getCustomKeySectors(): List<MifareSector> = sectors.filter {
        !it.isDefaultKeyA || !it.isDefaultKeyB
    }
}

data class MifareSector(
    val number: Int,
    val blocks: List<MifareBlock>,
    val keyA: String,
    val keyB: String,
    val accessBits: String,
    val isDefaultKeyA: Boolean,
    val isDefaultKeyB: Boolean
) {
    fun hasNonEmptyData(): Boolean = blocks.dropLast(1).any { block ->
        block.data.any { it != 0.toByte() }
    }
    
    fun isActive(): Boolean = hasNonEmptyData() || !isDefaultKeyA || !isDefaultKeyB
}

data class MifareBlock(
    val number: Int,
    val data: ByteArray
) {
    fun toHexString(): String = data.joinToString(" ") { "%02X".format(it) }
    
    fun toAsciiString(): String = data.map { byte ->
        if (byte in 32..126) byte.toInt().toChar() else '.'
    }.joinToString("")
    
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        
        other as MifareBlock
        
        if (number != other.number) return false
        if (!data.contentEquals(other.data)) return false
        
        return true
    }
    
    override fun hashCode(): Int {
        var result = number
        result = 31 * result + data.contentHashCode()
        return result
    }
}

private fun ByteArray.toHexString(): String = joinToString("") { "%02X".format(it) }
