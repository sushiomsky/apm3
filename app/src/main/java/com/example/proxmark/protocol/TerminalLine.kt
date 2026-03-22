package com.example.proxmark.protocol

enum class TerminalLineType {
    SENT,
    RECEIVED,
    ERROR,
    SYSTEM
}

data class TerminalLine(
    val text: String,
    val type: TerminalLineType,
    val timestamp: Long = System.currentTimeMillis()
)
