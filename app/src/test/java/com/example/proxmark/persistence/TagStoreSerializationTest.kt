package com.example.proxmark.persistence

import com.example.proxmark.repository.TagGroup
import com.example.proxmark.repository.TagInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TagStoreSerializationTest {

    @Test
    fun `round-trip serialization preserves all fields`() {
        val tags = listOf(
            TagInfo(uid = "04A1B2C3D4", type = "Mifare Classic", atqa = "0004", sak = "08", timestamp = 1000L),
            TagInfo(uid = "DEADBEEF01", type = "EM4100", atqa = null, sak = null, timestamp = 2000L)
        )

        val serialized = TagStore.serializeTags(tags)
        val deserialized = TagStore.deserializeTags(serialized)

        assertEquals(2, deserialized.size)
        assertEquals("04A1B2C3D4", deserialized[0].uid)
        assertEquals("Mifare Classic", deserialized[0].type)
        assertEquals("0004", deserialized[0].atqa)
        assertEquals("08", deserialized[0].sak)
        assertEquals(1000L, deserialized[0].timestamp)

        assertEquals("DEADBEEF01", deserialized[1].uid)
        assertEquals("EM4100", deserialized[1].type)
        assertNull(deserialized[1].atqa)
        assertNull(deserialized[1].sak)
        assertEquals(2000L, deserialized[1].timestamp)
    }

    @Test
    fun `empty list serializes to empty string`() {
        val serialized = TagStore.serializeTags(emptyList())
        assertEquals("", serialized)
    }

    @Test
    fun `empty string deserializes to empty list`() {
        val deserialized = TagStore.deserializeTags("")
        assertEquals(emptyList<TagInfo>(), deserialized)
    }

    @Test
    fun `blank string deserializes to empty list`() {
        val deserialized = TagStore.deserializeTags("   ")
        assertEquals(emptyList<TagInfo>(), deserialized)
    }

    @Test
    fun `single tag round-trips correctly`() {
        val tags = listOf(
            TagInfo(uid = "AABB", type = "Unknown", atqa = null, sak = null, timestamp = 500L)
        )

        val deserialized = TagStore.deserializeTags(TagStore.serializeTags(tags))

        assertEquals(1, deserialized.size)
        assertEquals("AABB", deserialized[0].uid)
        assertEquals("Unknown", deserialized[0].type)
    }

    @Test
    fun `tag note round-trips correctly`() {
        val tags = listOf(
            TagInfo(uid = "04A1B2C3D4", type = "Mifare Classic", timestamp = 1000L, note = "My front door key"),
            TagInfo(uid = "DEADBEEF01", type = "EM4100", timestamp = 2000L, note = null)
        )

        val deserialized = TagStore.deserializeTags(TagStore.serializeTags(tags))

        assertEquals(2, deserialized.size)
        assertEquals("My front door key", deserialized[0].note)
        assertNull(deserialized[1].note)
    }

    @Test
    fun `legacy data without note field deserializes with null note`() {
        // Simulate old format: uid, type, atqa, sak, timestamp (no note field)
        val legacyData = "04AB\u001FMifare Classic\u001F0004\u001F08\u001F1000"
        val deserialized = TagStore.deserializeTags(legacyData)

        assertEquals(1, deserialized.size)
        assertEquals("04AB", deserialized[0].uid)
        assertNull(deserialized[0].note)
    }

    @Test
    fun `group round-trip serialization preserves all fields`() {
        val groups = listOf(
            TagGroup(id = "group-1", name = "Home keys", tagKeys = setOf("AA|Mifare", "BB|EM4100")),
            TagGroup(id = "group-2", name = "Work", tagKeys = emptySet())
        )

        val serialized = TagStore.serializeGroups(groups)
        val deserialized = TagStore.deserializeGroups(serialized)

        assertEquals(2, deserialized.size)
        assertEquals("group-1", deserialized[0].id)
        assertEquals("Home keys", deserialized[0].name)
        assertEquals(setOf("AA|Mifare", "BB|EM4100"), deserialized[0].tagKeys)
        assertEquals("group-2", deserialized[1].id)
        assertEquals("Work", deserialized[1].name)
        assertEquals(emptySet<String>(), deserialized[1].tagKeys)
    }

    @Test
    fun `empty group list serializes and deserializes correctly`() {
        val serialized = TagStore.serializeGroups(emptyList())
        assertEquals("", serialized)
        val deserialized = TagStore.deserializeGroups("")
        assertEquals(emptyList<TagGroup>(), deserialized)
    }
}
