package com.example.proxmark.features

import com.example.proxmark.reader.ReaderType

data class ProxmarkFeature(
    val id: String,
    val title: String,
    val description: String,
    val category: ProxmarkFeatureCategory,
    val command: String,
    val safetyLevel: FeatureSafetyLevel = FeatureSafetyLevel.SAFE,
    val prerequisites: List<String> = emptyList(),
    val warningMessage: String? = null,
    /** Parameters the user must supply. Placeholders `{param.id}` in [command] are replaced. */
    val parameters: List<FeatureParameter> = emptyList(),
    /** If set, this feature targets a specific reader type (null = PM3 default). */
    val targetReader: ReaderType? = null
)
