package com.sushiomsky.proxmark.features

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureParameterTest {

    // ── Basic FeatureParameter properties ──

    @Test
    fun `parameter has correct defaults`() {
        val param = FeatureParameter(id = "test", label = "Test")

        assertEquals("test", param.id)
        assertEquals("Test", param.label)
        assertEquals("", param.hint)
        assertEquals(ParameterType.TEXT, param.type)
        assertEquals("", param.defaultValue)
        assertTrue(param.required)
    }

    @Test
    fun `parameter preserves all custom values`() {
        val param = FeatureParameter(
            id = "block",
            label = "Block number",
            hint = "0-63",
            type = ParameterType.NUMBER,
            defaultValue = "0",
            required = false
        )

        assertEquals("block", param.id)
        assertEquals("Block number", param.label)
        assertEquals("0-63", param.hint)
        assertEquals(ParameterType.NUMBER, param.type)
        assertEquals("0", param.defaultValue)
        assertFalse(param.required)
    }

    // ── ParameterType enum ──

    @Test
    fun `ParameterType has all expected values`() {
        val types = ParameterType.entries

        assertEquals(4, types.size)
        assertTrue(types.contains(ParameterType.TEXT))
        assertTrue(types.contains(ParameterType.HEX))
        assertTrue(types.contains(ParameterType.NUMBER))
        assertTrue(types.contains(ParameterType.CHOICE))
    }

    // ── Command interpolation logic ──

    @Test
    fun `simple placeholder replacement works`() {
        val command = "hf mf rdbl --blk {block}"
        val paramValues = mapOf("block" to "4")

        var interpolated = command
        for ((key, value) in paramValues) {
            interpolated = interpolated.replace("{$key}", value)
        }

        assertEquals("hf mf rdbl --blk 4", interpolated)
    }

    @Test
    fun `multiple placeholders are replaced`() {
        val command = "hf mf rdbl --blk {block} -k {key} --key{keyType}"
        val paramValues = mapOf(
            "block" to "0",
            "key" to "FFFFFFFFFFFF",
            "keyType" to "A"
        )

        var interpolated = command
        for ((key, value) in paramValues) {
            interpolated = interpolated.replace("{$key}", value)
        }

        assertEquals("hf mf rdbl --blk 0 -k FFFFFFFFFFFF --keyA", interpolated)
    }

    @Test
    fun `nfc command placeholder replacement works`() {
        val command = "nfc:ndef-text:{text}"
        val paramValues = mapOf("text" to "Hello World")

        var interpolated = command
        for ((key, value) in paramValues) {
            interpolated = interpolated.replace("{$key}", value)
        }

        assertEquals("nfc:ndef-text:Hello World", interpolated)
    }

    @Test
    fun `command without placeholders is unchanged`() {
        val command = "hf search"
        val paramValues = emptyMap<String, String>()

        var interpolated = command
        for ((key, value) in paramValues) {
            interpolated = interpolated.replace("{$key}", value)
        }

        assertEquals("hf search", interpolated)
    }

    @Test
    fun `unreplaced placeholders remain in command`() {
        val command = "hf mf rdbl --blk {block} -k {key}"
        val paramValues = mapOf("block" to "4")

        var interpolated = command
        for ((key, value) in paramValues) {
            interpolated = interpolated.replace("{$key}", value)
        }

        assertEquals("hf mf rdbl --blk 4 -k {key}", interpolated)
    }

    // ── Catalog features parameter consistency ──

    @Test
    fun `features without parameters have no placeholders in command`() {
        val placeholderRegex = Regex("""\{(\w+)\}""")

        ProxmarkFeatureCatalog.allFeatures
            .filter { it.parameters.isEmpty() }
            .forEach { feature ->
                val placeholders = placeholderRegex.findAll(feature.command).toList()
                assertTrue(
                    "Feature '${feature.id}' has no parameters but command contains " +
                        "placeholders: ${placeholders.map { it.value }}",
                    placeholders.isEmpty()
                )
            }
    }

    @Test
    fun `all CHOICE parameters have pipe-separated options in hint`() {
        ProxmarkFeatureCatalog.allFeatures
            .flatMap { f -> f.parameters.map { f.id to it } }
            .filter { (_, param) -> param.type == ParameterType.CHOICE }
            .forEach { (featureId, param) ->
                assertTrue(
                    "Feature '$featureId' CHOICE parameter '${param.id}' " +
                        "has hint '${param.hint}' without pipe-separated options",
                    param.hint.contains("|")
                )
            }
    }

    @Test
    fun `HEX parameters exist in catalog`() {
        val hexParams = ProxmarkFeatureCatalog.allFeatures
            .flatMap { it.parameters }
            .filter { it.type == ParameterType.HEX }

        assertTrue("Expected at least one HEX parameter in catalog", hexParams.isNotEmpty())
    }

    @Test
    fun `placeholder count matches parameter count for parameterized features`() {
        val placeholderRegex = Regex("""\{(\w+)\}""")

        ProxmarkFeatureCatalog.allFeatures
            .filter { it.parameters.isNotEmpty() }
            .forEach { feature ->
                val placeholderIds = placeholderRegex.findAll(feature.command)
                    .map { it.groupValues[1] }.toSet()
                val paramIds = feature.parameters.map { it.id }.toSet()

                assertEquals(
                    "Feature '${feature.id}' has mismatched placeholder/parameter counts. " +
                        "Placeholders: $placeholderIds, Params: $paramIds",
                    paramIds, placeholderIds
                )
            }
    }
}
