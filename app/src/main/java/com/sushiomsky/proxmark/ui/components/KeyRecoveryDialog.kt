package com.sushiomsky.proxmark.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sushiomsky.proxmark.features.FeatureExecutionState
import com.sushiomsky.proxmark.mifare.*
import com.sushiomsky.proxmark.protocol.TerminalLine
import com.sushiomsky.proxmark.protocol.TerminalLineType

/**
 * Full-screen key recovery dialog with:
 * - Sector map visualization
 * - Attack method selector
 * - Live progress and terminal output
 * - Sector detail on tap
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun KeyRecoveryDialog(
    keyState: MifareKeyState,
    selectedSector: Int?,
    terminalLines: List<TerminalLine>,
    connected: Boolean,
    executionState: FeatureExecutionState = FeatureExecutionState.Idle,
    tagType: String? = null,
    onSectorClick: (Int) -> Unit,
    onStartDictionary: () -> Unit,
    onStartScan: () -> Unit,
    onRunCommand: ((String, String) -> Unit)? = null,
    onReset: (CardType) -> Unit,
    onExportKeys: () -> Unit = {},
    onDismiss: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Top bar
                TopAppBar(
                    title = {
                        Column {
                            Text("Key Recovery", fontWeight = FontWeight.Bold)
                            Text(
                                "${keyState.cardType.label} • ${keyState.foundKeysCount}/${keyState.totalKeysNeeded} keys",
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, "Close")
                        }
                    },
                    actions = {
                        // Export keys
                        if (keyState.foundKeysCount > 0) {
                            IconButton(onClick = onExportKeys) {
                                Icon(Icons.Default.Share, "Export Keys")
                            }
                        }
                        // Card type selector
                        var showCardMenu by remember { mutableStateOf(false) }
                        Box {
                            IconButton(onClick = { showCardMenu = true }) {
                                Icon(Icons.Default.CreditCard, "Card Type")
                            }
                            DropdownMenu(
                                expanded = showCardMenu,
                                onDismissRequest = { showCardMenu = false }
                            ) {
                                CardType.entries.forEach { type ->
                                    DropdownMenuItem(
                                        text = { Text(type.label) },
                                        onClick = {
                                            onReset(type)
                                            showCardMenu = false
                                        },
                                        leadingIcon = {
                                            if (type == keyState.cardType) {
                                                Icon(Icons.Default.Check, null, tint = MaterialTheme.colorScheme.primary)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Progress header
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                        KeyRecoveryProgress(keyState = keyState)
                    }

                    // Sector map
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                            )
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "Sector Map",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                SectorMapGrid(
                                    keyState = keyState,
                                    onSectorClick = onSectorClick
                                )
                            }
                        }
                    }

                    // Sector detail
                    if (selectedSector != null && selectedSector in keyState.sectors.indices) {
                        item {
                            SectorDetailPanel(sector = keyState.sectors[selectedSector])
                        }
                    }

                    // Attack controls
                    if (onRunCommand != null) {
                        // Enhanced attack pipeline
                        item {
                            AttackPipelineCard(
                                keyState = keyState,
                                tagType = tagType,
                                executionState = executionState,
                                connected = connected,
                                onRunCommand = onRunCommand
                            )
                        }
                        item {
                            KeyRecoveryStatsCard(keyState = keyState)
                        }
                    } else {
                        // Legacy action buttons
                        item {
                            ActionButtonsRow(
                                keyState = keyState,
                                connected = connected,
                                onStartScan = onStartScan,
                                onStartDictionary = onStartDictionary
                            )
                        }
                    }

                    // Recent terminal output (last 20 lines relevant to MIFARE)
                    val recentLines = terminalLines.takeLast(20)
                    if (recentLines.isNotEmpty()) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(
                                    containerColor = MaterialTheme.colorScheme.inverseSurface.copy(alpha = 0.9f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        "Output",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.inverseOnSurface,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    recentLines.forEach { line ->
                                        val color = when (line.type) {
                                            TerminalLineType.SENT -> MaterialTheme.colorScheme.primary
                                            TerminalLineType.RECEIVED -> MaterialTheme.colorScheme.tertiary
                                            TerminalLineType.ERROR -> MaterialTheme.colorScheme.error
                                            TerminalLineType.SYSTEM -> MaterialTheme.colorScheme.outline
                                        }
                                        Text(
                                            line.text,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 10.sp,
                                            color = color,
                                            maxLines = 2
                                        )
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(modifier = Modifier.height(16.dp)) }
                }
            }
        }
    }
}

@Composable
private fun ActionButtonsRow(
    keyState: MifareKeyState,
    connected: Boolean,
    onStartScan: () -> Unit,
    onStartDictionary: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            "Actions",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold
        )

        // Step 1: Scan tag
        Button(
            onClick = onStartScan,
            modifier = Modifier.fillMaxWidth(),
            enabled = connected && !keyState.isRunning
        ) {
            Icon(Icons.Default.Nfc, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("1. Scan Tag (hf 14a reader)")
        }

        // Step 2: Dictionary check
        Button(
            onClick = onStartDictionary,
            modifier = Modifier.fillMaxWidth(),
            enabled = connected && !keyState.isRunning,
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary
            )
        ) {
            Icon(Icons.Default.Key, null, modifier = Modifier.size(18.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("2. Try Default Keys")
        }

        if (keyState.isRunning) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                    Text(
                        "${keyState.attackMethod.label} in progress...",
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}
