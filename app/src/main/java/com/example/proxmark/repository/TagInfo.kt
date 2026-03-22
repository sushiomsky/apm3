package com.example.proxmark.repository

data class TagInfo(
    val uid: String,
    val type: String,
    val atqa: String? = null,
    val sak: String? = null,
    val timestamp: Long = System.currentTimeMillis(),
    val note: String? = null
)
