package com.sushiomsky.proxmark.ui

import android.text.format.DateUtils
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sushiomsky.proxmark.features.FeatureExecutionState
import com.sushiomsky.proxmark.features.FeatureRunOutcome
import com.sushiomsky.proxmark.features.FeatureRunRecord
import com.sushiomsky.proxmark.features.ProxmarkFeature
import com.sushiomsky.proxmark.persistence.AppSettings
import com.sushiomsky.proxmark.reader.ReaderConnectionState
import com.sushiomsky.proxmark.reader.ReaderType
import com.sushiomsky.proxmark.repository.TagInfo
import com.sushiomsky.proxmark.ui.components.FeatureExecutionCard
import com.sushiomsky.proxmark.ui.components.ReaderSelectorCard
import com.sushiomsky.proxmark.ui.components.SettingsSection
import com.sushiomsky.proxmark.ui.components.TagInfoRow
import com.sushiomsky.proxmark.ui.components.readerStateColor
import com.sushiomsky.proxmark.persistence.ThemeMode
import com.sushiomsky.proxmark.usb.UsbConnectionState

@OptIn(ExperimentalMaterial3Api::class)
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
    isFeatureEnabled: (ProxmarkFeature) -> Boolean = { true },
    modifier: Modifier = Modifier
) {
    val connected = connectionState is UsbConnectionState.Connected
    val activeReaderState = readerStates[activeReaderType]
    val activeReaderConnected = activeReaderState is ReaderConnectionState.Connected
    val activeReaderConnecting = activeReaderState is ReaderConnectionState.Connecting
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var lastScanExpanded by rememberSaveable { mutableStateOf(false) }
    var historyExpanded by rememberSaveable { mutableStateOf(false) }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Compact top bar: title + connection dot + reader name + gear icon
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "PM3 Companion",
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.weight(1f))
                val activeState = readerStates[activeReaderType]
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(readerStateColor(activeState))
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = activeReaderType.label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                IconButton(onClick = { showSettings = true }) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Reader selector
        item {
            ReaderSelectorCard(
                activeReader = activeReaderType,
                readerStates = readerStates,
                onSelectReader = onSetActiveReader
            )
        }

        // Connect/Disconnect buttons
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Button(
                    onClick = onConnect,
                    modifier = Modifier.weight(1f),
                    enabled = !activeReaderConnected && !activeReaderConnecting
                ) { Text("Connect", style = MaterialTheme.typography.bodySmall) }
                OutlinedButton(
                    onClick = onDisconnect,
                    modifier = Modifier.weight(1f),
                    enabled = activeReaderConnected
                ) { Text("Disconnect", style = MaterialTheme.typography.bodySmall) }
                OutlinedButton(
                    onClick = onReconnect,
                    modifier = Modifier.weight(1f),
                    enabled = !activeReaderConnecting
                ) { Text("Reconnect", style = MaterialTheme.typography.bodySmall) }
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
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("⚠ Confirmations disabled", style = MaterialTheme.typography.bodySmall, color = Color(0xFFE65100))
                        OutlinedButton(onClick = { onSetSkipConfirmations(false) }) {
                            Text("Re-enable", style = MaterialTheme.typography.labelSmall)
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

        // Favorites as horizontal scrollable chips
        if (favoriteFeatures.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("★ Favorites", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        favoriteFeatures.forEach { feature ->
                            AssistChip(
                                onClick = { onRunFeature(feature) },
                                label = { Text(feature.title, style = MaterialTheme.typography.bodySmall) },
                                enabled = isFeatureEnabled(feature)
                            )
                        }
                    }
                }
            }
        }

        // Quick actions as horizontal scrollable chips
        if (quickFeatures.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("Quick Actions", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        quickFeatures.forEach { feature ->
                            AssistChip(
                                onClick = { onRunFeature(feature) },
                                label = { Text(feature.title, style = MaterialTheme.typography.bodySmall) },
                                enabled = isFeatureEnabled(feature)
                            )
                        }
                    }
                }
            }
        }

        // Last scan - compact inline row, expandable on tap
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text("Last Scan", style = MaterialTheme.typography.labelMedium)
                    if (lastScan != null) {
                        Text(
                            lastScan.uid,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            lastScan.type,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text("—", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.weight(1f))
                    Text(
                        if (lastScanExpanded) "▲" else "▼",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                AnimatedVisibility(visible = lastScanExpanded && lastScan != null) {
                    lastScan?.let { tag ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 4.dp)
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                TagInfoRow(tag = tag, onClick = {})
                            }
                        }
                    }
                }
            }
        }

        // Recent operations - show 3 by default, expandable
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Recent Operations", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (featureRunHistory.size > 3) {
                                TextButton(onClick = { historyExpanded = !historyExpanded }) {
                                    Text(
                                        if (historyExpanded) "Less" else "All (" + featureRunHistory.size + ")",
                                        style = MaterialTheme.typography.labelSmall
                                    )
                                }
                            }
                            TextButton(
                                onClick = onClearFeatureHistory,
                                enabled = featureRunHistory.isNotEmpty()
                            ) { Text("Clear", style = MaterialTheme.typography.labelSmall) }
                        }
                    }
                    if (featureRunHistory.isEmpty()) {
                        Text(
                            "No operations yet",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        val visible = if (historyExpanded) featureRunHistory.take(10) else featureRunHistory.take(3)
                        visible.forEach { run ->
                            val color = if (run.outcome == FeatureRunOutcome.SUCCESS) Color(0xFF2E7D32)
                            else MaterialTheme.colorScheme.error
                            val relTime = DateUtils.getRelativeTimeSpanString(
                                run.timestamp, System.currentTimeMillis(), DateUtils.SECOND_IN_MILLIS
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    run.title + ": " + run.detail,
                                    color = color,
                                    style = MaterialTheme.typography.bodySmall,
                                    modifier = Modifier.weight(1f)
                                )
                                Text(
                                    relTime.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }

    // Settings bottom sheet
    if (showSettings) {
        ModalBottomSheet(
            onDismissRequest = { showSettings = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
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
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
