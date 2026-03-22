package com.example.proxmark.features

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ProxmarkFeatureCatalogTest {

    @Test
    fun `catalog has at least 70 features`() {
        assertTrue(
            "Expected >= 70 features, got ${ProxmarkFeatureCatalog.allFeatures.size}",
            ProxmarkFeatureCatalog.allFeatures.size >= 70
        )
    }

    @Test
    fun `all feature ids are unique`() {
        val ids = ProxmarkFeatureCatalog.allFeatures.map { it.id }
        assertEquals("Duplicate feature IDs found", ids.size, ids.toSet().size)
    }

    @Test
    fun `all categories have at least one feature`() {
        val grouped = ProxmarkFeatureCatalog.groupedByCategory
        ProxmarkFeatureCategory.entries.forEach { cat ->
            assertTrue(
                "Category ${cat.name} has no features",
                grouped[cat]?.isNotEmpty() == true
            )
        }
    }

    @Test
    fun `destructive features all have warning messages`() {
        ProxmarkFeatureCatalog.allFeatures
            .filter { it.safetyLevel == FeatureSafetyLevel.DESTRUCTIVE }
            .forEach { feature ->
                assertFalse(
                    "DESTRUCTIVE feature '${feature.id}' missing warningMessage",
                    feature.warningMessage.isNullOrBlank()
                )
            }
    }

    @Test
    fun `advanced features all have warning messages`() {
        ProxmarkFeatureCatalog.allFeatures
            .filter { it.safetyLevel == FeatureSafetyLevel.ADVANCED }
            .forEach { feature ->
                assertFalse(
                    "ADVANCED feature '${feature.id}' missing warningMessage",
                    feature.warningMessage.isNullOrBlank()
                )
            }
    }

    @Test
    fun `features with parameters have valid placeholders in command`() {
        ProxmarkFeatureCatalog.allFeatures
            .filter { it.parameters.isNotEmpty() }
            .forEach { feature ->
                feature.parameters.forEach { param ->
                    assertTrue(
                        "Feature '${feature.id}' command doesn't contain placeholder {${param.id}}. Command: ${feature.command}",
                        feature.command.contains("{${param.id}}")
                    )
                }
            }
    }

    @Test
    fun `parameter interpolation replaces all placeholders`() {
        val feature = ProxmarkFeature(
            id = "test",
            title = "Test",
            description = "Test",
            category = ProxmarkFeatureCategory.UTILITIES,
            command = "hf mf rdbl --blk {block} -k {key}",
            parameters = listOf(
                FeatureParameter(id = "block", label = "Block", type = ParameterType.NUMBER),
                FeatureParameter(id = "key", label = "Key", type = ParameterType.HEX, defaultValue = "FFFFFFFFFFFF")
            )
        )

        val params = mapOf("block" to "3", "key" to "A0B1C2D3E4F5")
        var command = feature.command
        for ((key, value) in params) {
            command = command.replace("{$key}", value)
        }

        assertEquals("hf mf rdbl --blk 3 -k A0B1C2D3E4F5", command)
    }

    @Test
    fun `NFC features target BUILTIN_NFC reader`() {
        val nfcFeatures = ProxmarkFeatureCatalog.allFeatures
            .filter { it.category == ProxmarkFeatureCategory.NFC_OPERATIONS }

        assertTrue("Expected NFC features", nfcFeatures.isNotEmpty())
        nfcFeatures.forEach { feature ->
            assertEquals(
                "NFC feature '${feature.id}' should target BUILTIN_NFC",
                com.example.proxmark.reader.ReaderType.BUILTIN_NFC,
                feature.targetReader
            )
        }
    }

    @Test
    fun `NFC feature commands start with nfc colon`() {
        ProxmarkFeatureCatalog.allFeatures
            .filter { it.category == ProxmarkFeatureCategory.NFC_OPERATIONS }
            .forEach { feature ->
                assertTrue(
                    "NFC feature '${feature.id}' command should start with 'nfc:'. Got: ${feature.command}",
                    feature.command.startsWith("nfc:")
                )
            }
    }

    @Test
    fun `CHOICE parameters have pipe-separated hints`() {
        ProxmarkFeatureCatalog.allFeatures
            .flatMap { it.parameters }
            .filter { it.type == ParameterType.CHOICE }
            .forEach { param ->
                assertTrue(
                    "CHOICE param '${param.id}' hint should contain '|'. Got: ${param.hint}",
                    param.hint.contains("|")
                )
            }
    }

    @Test
    fun `safe features do not require confirmation`() {
        assertFalse(FeatureSafetyLevel.SAFE.requiresConfirmation)
        assertTrue(FeatureSafetyLevel.ADVANCED.requiresConfirmation)
        assertTrue(FeatureSafetyLevel.DESTRUCTIVE.requiresConfirmation)
    }
}
