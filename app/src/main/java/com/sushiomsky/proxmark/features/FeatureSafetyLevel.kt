package com.sushiomsky.proxmark.features

enum class FeatureSafetyLevel(
    val requiresConfirmation: Boolean,
    val label: String
) {
    SAFE(
        requiresConfirmation = false,
        label = "Safe"
    ),
    ADVANCED(
        requiresConfirmation = true,
        label = "Advanced"
    ),
    DESTRUCTIVE(
        requiresConfirmation = true,
        label = "Destructive"
    )
}
