package com.example.proxmark.ui

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.proxmark.features.FeatureExecutionState
import com.example.proxmark.features.FeatureRunOutcome
import com.example.proxmark.features.FeatureRunRecord
import com.example.proxmark.features.ProxmarkFeature
import com.example.proxmark.features.ProxmarkFeatureCategory
import com.example.proxmark.persistence.AppSettings
import com.example.proxmark.reader.ReaderConnectionState
import com.example.proxmark.reader.ReaderType
import com.example.proxmark.repository.TagInfo
import com.example.proxmark.ui.components.FeatureExecutionCard
import com.example.proxmark.ui.components.ReaderSelectorCard
import com.example.proxmark.ui.components.SettingsSection
import com.example.proxmark.ui.components.TagInfoRow
import com.example.proxmark.ui.components.readerStateColor
import com.example.proxmark.ui.components.readerStateText
import com.example.proxmark.persistence.ThemeMode
import com.example.proxmark.usb.UsbConnectionState

@Composable
fun HomeScreen(
    connectionState: UsbConnectionState,
    activeReaderType: ReaderType,
    readerStates: Map<ReaderType, ReaderConnectionState>,
    featureExecutionState: FeatureExecutionState,
    featureRunHistory: List<FeatureRunRecord>,
    quickFeatures: List<ProxmarkFeature>,
    favoriteFeatures: List<ProxmarkFeature>,
    lastScan: TagInfo?,
    skipConfirmations: Boolean,
    settings: AppSettings,
    autoScanEnabled: Boolean,
    rawCommandMode: Boolean,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onReconnect: () -> Unit,
    onRunFeature: (ProxmarkFeature) -> Unit,
    onSetActiveReader: (ReaderType) -> Unit,
    onReadAcrTag: () -> Unit,
    onClearFeatureExecutionState: () -> Unit,
    onClearFeatureHistory: () -> Unit,
    onSetSkipConfirmations: (Boolean) -> Unit,
    onUpdateTimeout: (Int) -> Unit,
    onUpdateThemeMode: (ThemeMode) -> Unit,
    onSetAutoScanEnabled: (Boolean) -> Unit,
    onUpdateAutoScanInterval: (Int) -> Unit,
    onToggleRawCommandMode: () -> Unit,
    onGetUsbDebugInfo: () -> String,
    modifier: Modifier = Modifier
) {
    val connected = connectionState is UsbConnectionState.Connected

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "PM3 Companion",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(top = 12.dp)
            )
        }

        // Reader selector
        item {
            ReaderSelectorCard(
                activeReader = activeReaderType,
                readerStates = readerStates,
                onSelectReader = onSetActiveReader
            )
        }

        // Connection status
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    val activeState = readerStates[activeReaderType]
                    Text(text = "${activeReaderType.label} status")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = readerStateText(activeState),
                        color = readerStateColor(activeState)
                    )
                }
            }
        }

        // Connect/Disconnect buttons
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onConnect,
                    modifier = Modifier.weight(1f),
                    enabled = !connected && connectionState !is UsbConnectionState.Connecting
                ) { Text("Connect") }
                OutlinedButton(
                    onClick = onDisconnect,
                    modifier = Modifier.weight(1f),
                    enabled = connected
                ) { Text("Disconnect") }
                OutlinedButton(
                    onClick = onReconnect,
                    modifier = Modifier.weight(1f),
                    enabled = connectionState !is UsbConnectionState.Connecting
                ) { Text("Reconnect") }
            }
        }

        // ACR manual read
        if (activeReaderType == ReaderType.ACR_USB) {
            item {
                val acrConnected = readerStates[ReaderType.ACR_USB] is ReaderConnectionState.Connected
                Button(onClick = onReadAcrTag, enabled = acrConnected, modifier = Modifier.fillMaxWidth()) {
                    Text("Read card on ACR reader")
                }
            }
        }

        // Skip confirmations warning
        if (skipConfirmations) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚠ Confirmations disabled", style = MaterialTheme.typography.bodySmall, color = Color(0xFFE65100))
                        OutlinedButton(onClick = { onSetSkipConfirmations(false) }) {
                            Text("Re-enable", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        // Favorites
        if (favoriteFeatures.isNotEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("★ Favorites", style = MaterialTheme.typography.titleMedium)
                        favoriteFeatures.forEach { feature ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Button(
                                    onClick = { onRunFeature(feature) },
                                    enabled = connected,
                                    modifier = Modifier.weight(1f)
                                ) { Text(feature.title) }
                            }
                        }
                    }
                }
            }
        }

        // Quick actions
        if (quickFeatures.isNotEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Quick Actions", style = MaterialTheme.typography.titleMedium)
                        quickFeatures.forEach { feature ->
                            Button(
                                onClick = { onRunFeature(feature) },
                                enabled = connected,
                                modifier = Modifier.fillMaxWidth()
                            ) { Text(feature.title) }
                        }
                    }
                }
            }
        }

        // Feature execution state
        item {
            FeatureExecutionCard(
                state = featureExecutionState,
                onDismiss = onClearFeatureExecutionState
            )
        }

        // Last scan
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Last Scan", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    if (lastScan == null) {
                        Text("No tag detected yet")
                    } else {
                        TagInfoRow(tag = lastScan, onClick = {})
                    }
                }
            }
        }

        // Recent operations
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Recent Operations", style = MaterialTheme.typography.titleMedium)
                        OutlinedButton(
                            onClick = onClearFeatureHistory,
                            enabled = featureRunHistory.isNotEmpty()
                        ) { Text("Clear") }
                    }
                    if (featureRunHistory.isEmpty()) {
                        Text("No operations run yet")
                    } else {
                        featureRunHistory.take(10).forEach { run ->
                            val color = if (run.outcome == FeatureRunOutcome.SUCCESS) Color(0xFF2E7D32)
                            else MaterialTheme.colorScheme.error
                            val relTime = DateUtils.getRelativeTimeSpanString(
                                run.timestamp, System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS
                            )
                            Column {
                                Text("${run.title}: ${run.detail}", color = color, style = MaterialTheme.typography.bodySmall)
                                Text(relTime.toString(), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }

        // Settings
        item {
            SettingsSection(
                settings = settings,
                connected = connected,
                autoScanEnabled = autoScanEnabled,
                rawCommandMode = rawCommandMode,
                onUpdateTimeout = onUpdateTimeout,
                onUpdateThemeMode = onUpdateThemeMode,
                onSetAutoScanEnabled = onSetAutoScanEnabled,
                onUpdateAutoScanInterval = onUpdateAutoScanInterval,
                onToggleRawCommandMode = onToggleRawCommandMode,
                onShowUsbDebugInfo = onGetUsbDebugInfo
            )
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}
