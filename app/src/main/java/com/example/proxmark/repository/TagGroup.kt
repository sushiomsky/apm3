package com.example.proxmark.repository

/**
 * A named group/folder for organizing saved tags.
 */
data class TagGroup(
    val id: String,
    val name: String,
    /** Tag keys in format "uid|type" that belong to this group. */
    val tagKeys: Set<String> = emptySet()
)
