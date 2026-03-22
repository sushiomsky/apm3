package com.sushiomsky.proxmark.export

import com.sushiomsky.proxmark.mifare.KeyStatus
import com.sushiomsky.proxmark.mifare.MifareClassicDump
import com.sushiomsky.proxmark.mifare.MifareKeyState
import com.sushiomsky.proxmark.protocol.TerminalLine
import com.sushiomsky.proxmark.protocol.TerminalLineType
import com.sushiomsky.proxmark.repository.TagInfo

object DataExporter {

    /** Export tag list as JSON. */
    fun exportTagsAsJson(tags: List<TagInfo>): String {
        if (tags.isEmpty()) return "[]"
        val entries = tags.joinToString(",\n  ") { tag ->
            buildString {
                append("{\"uid\":\"${tag.uid}\",\"type\":\"${escapeJson(tag.type)}\"")
                tag.atqa?.let { append(",\"atqa\":\"$it\"") }
                tag.sak?.let { append(",\"sak\":\"$it\"") }
                append(",\"timestamp\":${tag.timestamp}")
                tag.note?.let { append(",\"note\":\"${escapeJson(it)}\"") }
                append("}")
            }
        }
        return "[\n  $entries\n]"
    }

    /** Export tag list as CSV. */
    fun exportTagsAsCsv(tags: List<TagInfo>): String {
        val sb = StringBuilder()
        sb.appendLine("uid,type,atqa,sak,timestamp,note")
        for (tag in tags) {
            sb.appendLine("${tag.uid},${escapeCsv(tag.type)},${tag.atqa ?: ""},${tag.sak ?: ""},${tag.timestamp},${escapeCsv(tag.note ?: "")}")
        }
        return sb.toString()
    }

    /** Export MIFARE dump as hex text file (each block on its own line). */
    fun exportDumpAsHex(dump: MifareClassicDump): String {
        val sb = StringBuilder()
        sb.appendLine("# MIFARE Classic Dump")
        sb.appendLine("# UID: ${dump.uid}")
        sb.appendLine("# Sectors: ${dump.sectors.size}")
        sb.appendLine()
        for (sector in dump.sectors) {
            sb.appendLine("# Sector ${sector.number}")
            for (block in sector.blocks) {
                sb.appendLine(block.data.joinToString("") { "%02X".format(it) })
            }
        }
        return sb.toString()
    }

    /** Export found MIFARE keys as text. */
    fun exportKeysAsText(keyState: MifareKeyState): String {
        val sb = StringBuilder()
        sb.appendLine("# MIFARE Key Recovery Results")
        sb.appendLine("# Card type: ${keyState.cardType.label}")
        sb.appendLine("# Keys found: ${keyState.foundKeysCount}/${keyState.totalKeysNeeded}")
        sb.appendLine()
        sb.appendLine("Sector | Key A        | Key B")
        sb.appendLine("-------|--------------|-------------")
        for (sector in keyState.sectors) {
            val keyA = if (sector.keyAStatus == KeyStatus.FOUND) (sector.keyA ?: "????????????") else "????????????"
            val keyB = if (sector.keyBStatus == KeyStatus.FOUND) (sector.keyB ?: "????????????") else "????????????"
            sb.appendLine("%6d | %s | %s".format(sector.sectorNumber, keyA, keyB))
        }
        return sb.toString()
    }

    /** Export terminal log as plain text. */
    fun exportTerminalLog(lines: List<TerminalLine>): String {
        val sb = StringBuilder()
        sb.appendLine("# Proxmark3 Terminal Log")
        sb.appendLine("# Lines: ${lines.size}")
        sb.appendLine()
        for (line in lines) {
            val prefix = when (line.type) {
                TerminalLineType.SENT -> "» "
                TerminalLineType.RECEIVED -> "  "
                TerminalLineType.ERROR -> "✖ "
                TerminalLineType.SYSTEM -> "• "
            }
            sb.appendLine("$prefix${line.text}")
        }
        return sb.toString()
    }

    private fun escapeJson(s: String): String =
        s.replace("\\", "\\\\").replace("\"", "\\\"").replace("\n", "\\n")

    private fun escapeCsv(s: String): String =
        if (s.contains(',') || s.contains('"') || s.contains('\n'))
            "\"${s.replace("\"", "\"\"")}\""
        else s
}
