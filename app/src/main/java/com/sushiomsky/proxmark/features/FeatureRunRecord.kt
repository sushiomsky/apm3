package com.sushiomsky.proxmark.features

enum class FeatureRunOutcome {
    SUCCESS,
    FAILED
}

data class FeatureRunRecord(
    val featureId: String,
    val title: String,
    val outcome: FeatureRunOutcome,
    val detail: String,
    val timestamp: Long = System.currentTimeMillis()
)
