package com.sushiomsky.proxmark.features

/**
 * Describes a user-supplied input parameter for a feature.
 *
 * The placeholder `{id}` in the feature command string will be replaced
 * with the user-supplied value at execution time.
 */
data class FeatureParameter(
    val id: String,
    val label: String,
    val hint: String = "",
    val type: ParameterType = ParameterType.TEXT,
    val defaultValue: String = "",
    val required: Boolean = true
)

enum class ParameterType {
    /** Free-form text input. */
    TEXT,
    /** Hex data (validated: [0-9A-Fa-f]+). */
    HEX,
    /** Integer input. */
    NUMBER,
    /** Single choice from [FeatureParameter.hint] (pipe-separated: "A|B"). */
    CHOICE
}
