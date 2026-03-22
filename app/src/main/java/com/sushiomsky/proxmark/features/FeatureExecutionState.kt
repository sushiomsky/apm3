package com.sushiomsky.proxmark.features

sealed class FeatureExecutionState {
    data object Idle : FeatureExecutionState()
    data class Running(val featureId: String, val title: String) : FeatureExecutionState()
    data class Success(val featureId: String, val title: String, val output: String?) : FeatureExecutionState()
    data class Failed(val featureId: String, val title: String, val message: String) : FeatureExecutionState()
}
