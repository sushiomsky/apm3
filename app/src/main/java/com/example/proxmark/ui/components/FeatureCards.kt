package com.example.proxmark.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.proxmark.features.FeatureExecutionState
import com.example.proxmark.features.FeatureSafetyLevel
import com.example.proxmark.features.ParameterType
import com.example.proxmark.features.ProxmarkFeature
import com.example.proxmark.features.ProxmarkFeatureCategory

@Composable
internal fun FeatureExecutionCard(
    state: FeatureExecutionState,
    onDismiss: () -> Unit
) {
    val title: String
    val body: String
    val color: Color
    when (state) {
        FeatureExecutionState.Idle -> {
            title = "Ready"
            body = "Select an action to run on the connected PM3."
            color = Color(0xFF455A64)
        }

        is FeatureExecutionState.Running -> {
            title = "Running"
            body = "Executing ${state.title}..."
            color = Color(0xFFEF6C00)
        }

        is FeatureExecutionState.Success -> {
            title = "Completed"
            body = state.output ?: "${state.title} completed."
            color = Color(0xFF2E7D32)
        }

        is FeatureExecutionState.Failed -> {
            title = "Failed"
            body = state.message
            color = Color(0xFFC62828)
        }
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(title, color = color, style = MaterialTheme.typography.titleMedium)
            Text(body, style = MaterialTheme.typography.bodySmall)
            if (state !is FeatureExecutionState.Idle && state !is FeatureExecutionState.Running) {
                OutlinedButton(onClick = onDismiss) {
                    Text("Dismiss")
                }
            }
        }
    }
}

@Composable
internal fun FeatureCategoryCard(
    category: ProxmarkFeatureCategory,
    features: List<ProxmarkFeature>,
    isFeatureEnabled: (ProxmarkFeature) -> Boolean,
    onRunFeature: (ProxmarkFeature) -> Unit,
    favoriteIds: Set<String> = emptySet(),
    onToggleFavorite: (String) -> Unit = {}
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(category.label, style = MaterialTheme.typography.titleMedium)
            features.forEach { feature ->
                val isFavorite = feature.id in favoriteIds
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        val label = buildString {
                            append(feature.title)
                            if (feature.parameters.isNotEmpty()) append(" ⚙")
                            append(" • ${feature.safetyLevel.label}")
                        }
                        OutlinedButton(
                            onClick = { onRunFeature(feature) },
                            enabled = isFeatureEnabled(feature),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(label)
                        }
                        TextButton(onClick = { onToggleFavorite(feature.id) }) {
                            Text(if (isFavorite) "★" else "☆")
                        }
                    }
                    Text(
                        text = feature.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (feature.prerequisites.isNotEmpty()) {
                        Text(
                            text = "Prerequisites: ${feature.prerequisites.joinToString(" • ")}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun FeatureConfirmationDialog(
    feature: ProxmarkFeature,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    onSkipFutureConfirmations: () -> Unit
) {
    val title = when (feature.safetyLevel) {
        FeatureSafetyLevel.SAFE -> "Run action"
        FeatureSafetyLevel.ADVANCED -> "Confirm advanced action"
        FeatureSafetyLevel.DESTRUCTIVE -> "Confirm destructive action"
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Action: ${feature.title}")
                Text(feature.description, style = MaterialTheme.typography.bodySmall)
                feature.warningMessage?.let { warning ->
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (feature.safetyLevel == FeatureSafetyLevel.DESTRUCTIVE)
                                Color(0xFFFFEBEE) else Color(0xFFFFF3E0)
                        )
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Text(
                                text = "Why is this risky?",
                                style = MaterialTheme.typography.labelMedium,
                                color = if (feature.safetyLevel == FeatureSafetyLevel.DESTRUCTIVE)
                                    Color(0xFFC62828) else Color(0xFFE65100)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = warning,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                }
                if (feature.prerequisites.isNotEmpty()) {
                    Text(
                        "Prerequisites:\n- ${feature.prerequisites.joinToString("\n- ")}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Column(horizontalAlignment = Alignment.End) {
                Button(onClick = onConfirm) {
                    Text("Run")
                }
                Spacer(modifier = Modifier.height(4.dp))
                TextButton(onClick = onSkipFutureConfirmations) {
                    Text(
                        "Run & don't ask again this session",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        dismissButton = {
            OutlinedButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
internal fun FeatureParameterDialog(
    feature: ProxmarkFeature,
    onRun: (Map<String, String>) -> Unit,
    onDismiss: () -> Unit
) {
    val paramValues = remember(feature.id) {
        mutableMapOf<String, String>().apply {
            feature.parameters.forEach { p -> put(p.id, p.defaultValue) }
        }
    }
    var version by remember { mutableStateOf(0) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(feature.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(feature.description, style = MaterialTheme.typography.bodySmall)
                if (feature.warningMessage != null) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))) {
                        Text(
                            text = "⚠ ${feature.warningMessage}",
                            modifier = Modifier.padding(8.dp),
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                }
                version.let { _ ->
                    feature.parameters.forEach { param ->
                        when (param.type) {
                            ParameterType.CHOICE -> {
                                val choices = param.hint.split("|")
                                Text(param.label, style = MaterialTheme.typography.labelSmall)
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    choices.forEach { choice ->
                                        val selected = paramValues[param.id] == choice
                                        if (selected) {
                                            Button(onClick = {}) { Text(choice) }
                                        } else {
                                            OutlinedButton(onClick = {
                                                paramValues[param.id] = choice
                                                version++
                                            }) { Text(choice) }
                                        }
                                    }
                                }
                            }
                            else -> {
                                OutlinedTextField(
                                    value = paramValues[param.id] ?: "",
                                    onValueChange = {
                                        paramValues[param.id] = it
                                        version++
                                    },
                                    label = { Text(param.label) },
                                    placeholder = { Text(param.hint) },
                                    singleLine = true,
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            val allFilled = feature.parameters.filter { it.required }.all {
                paramValues[it.id]?.isNotBlank() == true
            }
            Button(
                onClick = { onRun(paramValues.toMap()) },
                enabled = allFilled
            ) {
                Text("Run")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
