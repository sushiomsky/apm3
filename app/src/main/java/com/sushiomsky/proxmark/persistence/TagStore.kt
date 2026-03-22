package com.sushiomsky.proxmark.persistence

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.sushiomsky.proxmark.repository.TagGroup
import com.sushiomsky.proxmark.repository.TagInfo
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "proxmark_tags")

class TagStore(context: Context) {

    private val dataStore = context.applicationContext.dataStore
    private val tagsKey = stringPreferencesKey("saved_tags")
    private val groupsKey = stringPreferencesKey("tag_groups")

    val savedTags: Flow<List<TagInfo>> = dataStore.data.map { prefs ->
        val json = prefs[tagsKey] ?: return@map emptyList()
        deserializeTags(json)
    }

    val tagGroups: Flow<List<TagGroup>> = dataStore.data.map { prefs ->
        val data = prefs[groupsKey] ?: return@map emptyList()
        deserializeGroups(data)
    }

    suspend fun saveTags(tags: List<TagInfo>) {
        dataStore.edit { prefs ->
            prefs[tagsKey] = serializeTags(tags)
        }
    }

    suspend fun saveGroups(groups: List<TagGroup>) {
        dataStore.edit { prefs ->
            prefs[groupsKey] = serializeGroups(groups)
        }
    }

    companion object {
        internal fun serializeTags(tags: List<TagInfo>): String {
            return tags.joinToString(RECORD_SEP) { tag ->
                listOfNotNull(
                    tag.uid,
                    tag.type,
                    tag.atqa ?: "",
                    tag.sak ?: "",
                    tag.timestamp.toString(),
                    tag.note ?: ""
                ).joinToString(FIELD_SEP)
            }
        }

        internal fun deserializeTags(data: String): List<TagInfo> {
            if (data.isBlank()) return emptyList()
            return data.split(RECORD_SEP).mapNotNull { record ->
                val fields = record.split(FIELD_SEP)
                if (fields.size < 2) return@mapNotNull null
                TagInfo(
                    uid = fields[0],
                    type = fields[1],
                    atqa = fields.getOrNull(2)?.ifEmpty { null },
                    sak = fields.getOrNull(3)?.ifEmpty { null },
                    timestamp = fields.getOrNull(4)?.toLongOrNull() ?: System.currentTimeMillis(),
                    note = fields.getOrNull(5)?.ifEmpty { null }
                )
            }
        }

        internal fun serializeGroups(groups: List<TagGroup>): String {
            // Format: id|name|key1,key2,key3 separated by RECORD_SEP
            return groups.joinToString(RECORD_SEP) { group ->
                listOf(
                    group.id,
                    group.name,
                    group.tagKeys.joinToString(",")
                ).joinToString(FIELD_SEP)
            }
        }

        internal fun deserializeGroups(data: String): List<TagGroup> {
            if (data.isBlank()) return emptyList()
            return data.split(RECORD_SEP).mapNotNull { record ->
                val fields = record.split(FIELD_SEP)
                if (fields.size < 2) return@mapNotNull null
                TagGroup(
                    id = fields[0],
                    name = fields[1],
                    tagKeys = fields.getOrNull(2)?.split(",")?.filter { it.isNotEmpty() }?.toSet() ?: emptySet()
                )
            }
        }

        private const val RECORD_SEP = "\u001E"
        private const val FIELD_SEP = "\u001F"
    }
}
