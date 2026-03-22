package com.sushiomsky.proxmark.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sushiomsky.proxmark.features.ProxmarkFeature
import com.sushiomsky.proxmark.features.ProxmarkFeatureCategory
import com.sushiomsky.proxmark.mifare.MifareClassicDump
import com.sushiomsky.proxmark.mifare.MifareWorkflow
import com.sushiomsky.proxmark.ui.components.FeatureCategoryCard
import com.sushiomsky.proxmark.ui.components.MifareWorkflowsCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ToolsScreen(
    features: List<ProxmarkFeature>,
    featureSearchQuery: String,
    connected: Boolean,
    favoriteIds: Set<String>,
    mifareDump1: MifareClassicDump?,
    mifareDump2: MifareClassicDump?,
    advancedMode: Boolean,
    isFeatureEnabled: (ProxmarkFeature) -> Boolean,
    onRunFeature: (ProxmarkFeature) -> Unit,
    onSetFeatureSearchQuery: (String) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onParseDump: (String) -> Unit,
    onSetDump1: (String) -> Unit,
    onSetDump2: (String) -> Unit,
    onClearDumpComparison: () -> Unit,
    onShowWorkflow: (MifareWorkflow) -> Unit,
    onShowKeyRecovery: () -> Unit,
    onShowGymLockerWorkflow: () -> Unit,
    onToggleAdvancedMode: () -> Unit,
    onShowPluginBrowser: () -> Unit,
    onShowWorkflowBuilder: () -> Unit,
    onShowScriptEditor: () -> Unit,
    modifier: Modifier = Modifier
) {
    val groupedFeatures = features.groupBy { it.category }
    val categories = ProxmarkFeatureCategory.entries.filter {
        groupedFeatures[it].orEmpty().isNotEmpty()
    }
    var selectedCategory by remember { mutableStateOf<ProxmarkFeatureCategory?>(null) }
    var mifareExpanded by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Search bar
        item {
            OutlinedTextField(
                value = featureSearchQuery,
                onValueChange = onSetFeatureSearchQuery,
                label = { Text("Search features\u2026") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp)
            )
        }

        // Category filter chips
        item {
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                FilterChip(
                    selected = selectedCategory == null,
                    onClick = { selectedCategory = null },
                    label = { Text("All", style = MaterialTheme.typography.bodySmall) }
                )
                categories.filter { it != ProxmarkFeatureCategory.QUICK_START }.forEach { cat ->
                    FilterChip(
                        selected = selectedCategory == cat,
                        onClick = { selectedCategory = if (selectedCategory == cat) null else cat },
                        label = { Text(cat.label, style = MaterialTheme.typography.bodySmall) }
                    )
                }
            }
        }

        // Gym Locker + Key Recovery side by side
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.tertiaryContainer
                    ),
                    onClick = onShowGymLockerWorkflow
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "\uD83C\uDFCB\uFE0F Gym Locker",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            "Scan \u2192 Crack \u2192 Clone",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
                Card(
                    modifier = Modifier.weight(1f),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    ),
                    onClick = onShowKeyRecovery
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "\uD83D\uDD11 Key Recovery",
                            style = MaterialTheme.typography.titleSmall
                        )
                        Text(
                            "Sector map & attacks",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        // MIFARE Workflows
        item {
            MifareWorkflowsCard(onSelectWorkflow = onShowWorkflow)
        }

        // MIFARE Classic Analysis (collapsed by default)
        item {
            MifareAnalysisCard(
                expanded = mifareExpanded,
                onToggleExpanded = { mifareExpanded = !mifareExpanded },
                mifareDump1 = mifareDump1,
                mifareDump2 = mifareDump2,
                onParseDump = onParseDump,
                onSetDump1 = onSetDump1,
                onSetDump2 = onSetDump2,
                onClearDumpComparison = onClearDumpComparison
            )
        }

        // Plugins & Scripts - compact row of 3 buttons
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                OutlinedButton(
                    onClick = onShowPluginBrowser,
                    modifier = Modifier.weight(1f)
                ) { Text("\uD83E\uDDE9 Plugins", style = MaterialTheme.typography.bodySmall) }
                OutlinedButton(
                    onClick = onShowWorkflowBuilder,
                    modifier = Modifier.weight(1f)
                ) { Text("\uD83D\uDD27 Workflows", style = MaterialTheme.typography.bodySmall) }
                OutlinedButton(
                    onClick = onShowScriptEditor,
                    modifier = Modifier.weight(1f)
                ) { Text("\uD83D\uDCDD Scripts", style = MaterialTheme.typography.bodySmall) }
            }
        }

        // Feature categories
        val filteredCategories = if (featureSearchQuery.isBlank()) {
            categories.filter { cat ->
                cat != ProxmarkFeatureCategory.QUICK_START &&
                    (selectedCategory == null || cat == selectedCategory)
            }
        } else {
            categories.filter { cat ->
                cat != ProxmarkFeatureCategory.QUICK_START &&
                    (selectedCategory == null || cat == selectedCategory) &&
                    groupedFeatures[cat].orEmpty().any { f ->
                        f.title.contains(featureSearchQuery, ignoreCase = true) ||
                            f.description.contains(featureSearchQuery, ignoreCase = true)
                    }
            }
        }

        items(filteredCategories, key = { it.name }) { category ->
            val feats = if (featureSearchQuery.isBlank()) {
                groupedFeatures[category].orEmpty()
            } else {
                groupedFeatures[category].orEmpty().filter { f ->
                    f.title.contains(featureSearchQuery, ignoreCase = true) ||
                        f.description.contains(featureSearchQuery, ignoreCase = true)
                }
            }
            FeatureCategoryCard(
                category = category,
                features = feats,
                isFeatureEnabled = isFeatureEnabled,
                onRunFeature = onRunFeature,
                favoriteIds = favoriteIds,
                onToggleFavorite = onToggleFavorite
            )
        }

        // Advanced Mode toggle - compact row
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "\u2699\uFE0F Advanced Mode",
                            style = MaterialTheme.typography.bodySmall
                        )
                        Text(
                            "Direct PM3 command access",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                        )
                    }
                    Switch(
                        checked = advancedMode,
                        onCheckedChange = { onToggleAdvancedMode() }
                    )
                }
            }
        }

        item { Spacer(Modifier.height(8.dp)) }
    }
}

@Composable
private fun MifareAnalysisCard(
    expanded: Boolean,
    onToggleExpanded: () -> Unit,
    mifareDump1: MifareClassicDump?,
    mifareDump2: MifareClassicDump?,
    onParseDump: (String) -> Unit,
    onSetDump1: (String) -> Unit,
    onSetDump2: (String) -> Unit,
    onClearDumpComparison: () -> Unit
) {
    var dumpInput by remember { mutableStateOf("") }
    var showInput by remember { mutableStateOf(false) }
    var showCompareMode by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggleExpanded() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("MIFARE Classic Analysis", style = MaterialTheme.typography.titleSmall)
                Text(if (expanded) "\u25B2" else "\u25BC", style = MaterialTheme.typography.labelSmall)
            }

            AnimatedVisibility(visible = expanded) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { showCompareMode = !showCompareMode }) {
                            Text(
                                if (showCompareMode) "View Mode" else "Compare Mode",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }

                    if (!showCompareMode) {
                        Text(
                            "Paste hex output from 'hf mf dump'.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (showInput) {
                            OutlinedTextField(
                                value = dumpInput,
                                onValueChange = { dumpInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Paste hex dump (2048 chars for 1K card)...") },
                                minLines = 3,
                                maxLines = 6
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        if (dumpInput.isNotBlank()) {
                                            onParseDump(dumpInput)
                                            dumpInput = ""
                                            showInput = false
                                        }
                                    },
                                    enabled = dumpInput.length >= 2048
                                ) { Text("Analyze") }
                                OutlinedButton(onClick = { showInput = false }) { Text("Cancel") }
                            }
                        } else {
                            Button(onClick = { showInput = true }, modifier = Modifier.fillMaxWidth()) {
                                Text("Paste & Analyze Dump")
                            }
                        }
                    } else {
                        Text(
                            "Compare two dumps to detect changes.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            DumpInputColumn("Dump 1", mifareDump1, onSetDump1, Modifier.weight(1f))
                            DumpInputColumn("Dump 2", mifareDump2, onSetDump2, Modifier.weight(1f))
                        }
                        if (mifareDump1 != null || mifareDump2 != null) {
                            OutlinedButton(onClick = onClearDumpComparison, modifier = Modifier.fillMaxWidth()) {
                                Text("Clear Both Dumps")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DumpInputColumn(
    label: String,
    dump: MifareClassicDump?,
    onSet: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var input by remember { mutableStateOf("") }
    var showField by remember { mutableStateOf(false) }

    Column(modifier = modifier) {
        if (showField) {
            OutlinedTextField(
                value = input,
                onValueChange = { input = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("$label...") },
                minLines = 2,
                maxLines = 4
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Button(
                    onClick = { onSet(input); showField = false },
                    enabled = input.length >= 2048,
                    modifier = Modifier.weight(1f)
                ) { Text("Set", style = MaterialTheme.typography.labelSmall) }
                OutlinedButton(
                    onClick = { showField = false },
                    modifier = Modifier.weight(1f)
                ) { Text("Cancel", style = MaterialTheme.typography.labelSmall) }
            }
        } else {
            OutlinedButton(onClick = { showField = true }, modifier = Modifier.fillMaxWidth()) {
                Text(
                    if (dump != null) "$label \u2713" else "Paste $label",
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
    }
}
