package com.sushiomsky.proxmark.features

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FeatureCatalogTest {

    private val allFeatures = ProxmarkFeatureCatalog.allFeatures

    // ── Unique IDs ──

    @Test
    fun `all features have unique IDs`() {
        val ids = allFeatures.map { it.id }
        val duplicates = ids.groupBy { it }.filter { it.value.size > 1 }.keys

        assertEquals("Duplicate feature IDs found: $duplicates", ids.size, ids.toSet().size)
    }

    // ── Non-empty fields ──

    @Test
    fun `all features have non-empty title`() {
        allFeatures.forEach { feature ->
            assertTrue(
                "Feature '${feature.id}' has empty title",
                feature.title.isNotBlank()
            )
        }
    }

    @Test
    fun `all features have non-empty description`() {
        allFeatures.forEach { feature ->
            assertTrue(
                "Feature '${feature.id}' has empty description",
                feature.description.isNotBlank()
            )
        }
    }

    @Test
    fun `all features have non-empty command`() {
        allFeatures.forEach { feature ->
            assertTrue(
                "Feature '${feature.id}' has empty command",
                feature.command.isNotBlank()
            )
        }
    }

    @Test
    fun `all features have non-empty ID`() {
        allFeatures.forEach { feature ->
            assertTrue(
                "Feature has empty ID (title: '${feature.title}')",
                feature.id.isNotBlank()
            )
        }
    }

    // ── Safety conventions ──

    @Test
    fun `all ADVANCED features have non-null warningMessage`() {
        val violators = allFeatures.filter {
            it.safetyLevel == FeatureSafetyLevel.ADVANCED && it.warningMessage == null
        }

        assertTrue(
            "ADVANCED features missing warningMessage: ${violators.map { it.id }}",
            violators.isEmpty()
        )
    }

    @Test
    fun `all DESTRUCTIVE features have non-null warningMessage`() {
        val violators = allFeatures.filter {
            it.safetyLevel == FeatureSafetyLevel.DESTRUCTIVE && it.warningMessage == null
        }

        assertTrue(
            "DESTRUCTIVE features missing warningMessage: ${violators.map { it.id }}",
            violators.isEmpty()
        )
    }

    @Test
    fun `SAFE features do not require confirmation`() {
        assertFalse(FeatureSafetyLevel.SAFE.requiresConfirmation)
    }

    @Test
    fun `ADVANCED features require confirmation`() {
        assertTrue(FeatureSafetyLevel.ADVANCED.requiresConfirmation)
    }

    @Test
    fun `DESTRUCTIVE features require confirmation`() {
        assertTrue(FeatureSafetyLevel.DESTRUCTIVE.requiresConfirmation)
    }

    // ── Parameter placeholders ──

    @Test
    fun `all features with parameters have matching placeholders in command`() {
        allFeatures.filter { it.parameters.isNotEmpty() }.forEach { feature ->
            feature.parameters.forEach { param ->
                assertTrue(
                    "Feature '${feature.id}' has parameter '${param.id}' but command '${feature.command}' " +
                        "does not contain placeholder '{${param.id}}'",
                    feature.command.contains("{${param.id}}")
                )
            }
        }
    }

    @Test
    fun `all command placeholders have matching parameters`() {
        val placeholderRegex = Regex("""\{(\w+)\}""")

        allFeatures.filter { it.parameters.isNotEmpty() }.forEach { feature ->
            val placeholders = placeholderRegex.findAll(feature.command).map { it.groupValues[1] }.toSet()
            val paramIds = feature.parameters.map { it.id }.toSet()

            placeholders.forEach { placeholder ->
                assertTrue(
                    "Feature '${feature.id}' command has placeholder '{$placeholder}' " +
                        "but no matching parameter. Params: $paramIds",
                    paramIds.contains(placeholder)
                )
            }
        }
    }

    // ── ID convention ──

    @Test
    fun `all feature IDs follow kebab-case convention`() {
        val kebabCaseRegex = Regex("^[a-z0-9]+(-[a-z0-9]+)*$")

        allFeatures.forEach { feature ->
            assertTrue(
                "Feature ID '${feature.id}' does not follow kebab-case convention",
                kebabCaseRegex.matches(feature.id)
            )
        }
    }

    // ── Category coverage ──

    @Test
    fun `every ProxmarkFeatureCategory enum value is used by at least one feature`() {
        val usedCategories = allFeatures.map { it.category }.toSet()

        ProxmarkFeatureCategory.entries.forEach { category ->
            assertTrue(
                "Category '${category.name}' has no features in the catalog",
                usedCategories.contains(category)
            )
        }
    }

    @Test
    fun `each category has at least one feature`() {
        val grouped = ProxmarkFeatureCatalog.groupedByCategory

        ProxmarkFeatureCategory.entries.forEach { category ->
            val features = grouped[category]
            assertNotNull("Category '${category.name}' is missing from groupedByCategory", features)
            assertTrue(
                "Category '${category.name}' has zero features",
                features!!.isNotEmpty()
            )
        }
    }

    // ── Catalog is not empty ──

    @Test
    fun `catalog has features`() {
        assertTrue("Feature catalog is empty", allFeatures.isNotEmpty())
    }

    @Test
    fun `catalog has significant number of features`() {
        assertTrue(
            "Expected at least 30 features, found ${allFeatures.size}",
            allFeatures.size >= 30
        )
    }

    // ── groupedByCategory matches allFeatures ──

    @Test
    fun `groupedByCategory contains all features from allFeatures`() {
        val fromGrouped = ProxmarkFeatureCatalog.groupedByCategory.values.flatten().toSet()
        val fromAll = allFeatures.toSet()

        assertEquals(fromAll, fromGrouped)
    }

    // ── Parameter quality checks ──

    @Test
    fun `all feature parameters have non-empty ID`() {
        allFeatures.flatMap { f -> f.parameters.map { f.id to it } }.forEach { (featureId, param) ->
            assertTrue(
                "Feature '$featureId' has a parameter with empty ID",
                param.id.isNotBlank()
            )
        }
    }

    @Test
    fun `all feature parameters have non-empty label`() {
        allFeatures.flatMap { f -> f.parameters.map { f.id to it } }.forEach { (featureId, param) ->
            assertTrue(
                "Feature '$featureId' parameter '${param.id}' has empty label",
                param.label.isNotBlank()
            )
        }
    }
}
