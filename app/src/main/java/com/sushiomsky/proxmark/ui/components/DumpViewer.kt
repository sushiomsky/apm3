package com.sushiomsky.proxmark.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sushiomsky.proxmark.mifare.MifareClassicDump
import com.sushiomsky.proxmark.mifare.MifareSector

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun DumpViewerDialog(
    dump: MifareClassicDump,
    onExportDump: () -> Unit = {},
    onDismiss: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    var selectedSector by remember { mutableStateOf<Int?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                TopAppBar(
                    title = { Text("MIFARE Classic Dump") },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, "Close")
                        }
                    },
                    actions = {
                        IconButton(onClick = onExportDump) {
                            Icon(Icons.Default.ContentCopy, "Export Dump")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            "UID: ${dump.uid}",
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            Text("Active sectors: ${dump.getActiveSectors().size}/16")
                            Text("Custom keys: ${dump.getCustomKeySectors().size}")
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(dump.sectors) { sector ->
                        SectorCard(
                            sector = sector,
                            isExpanded = selectedSector == sector.number,
                            onToggleExpand = {
                                selectedSector = if (selectedSector == sector.number) null else sector.number
                            },
                            onCopyKey = { key ->
                                clipboardManager.setText(AnnotatedString(key))
                            }
                        )
                    }
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectorCard(
    sector: MifareSector,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onCopyKey: (String) -> Unit
) {
    val isActive = sector.isActive()
    val bgColor = if (isActive) {
        MaterialTheme.colorScheme.secondaryContainer
    } else {
        MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = bgColor)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggleExpand() }
                .padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Sector ${sector.number}",
                    style = MaterialTheme.typography.titleSmall
                )
                if (isActive) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = MaterialTheme.colorScheme.primary
                    ) {
                        Text(
                            "ACTIVE",
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                }
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(12.dp))

                sector.blocks.forEachIndexed { idx, block ->
                    if (idx == 3) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Column(modifier = Modifier.padding(8.dp)) {
                                Text(
                                    "Block ${block.number} (Trailer)",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "Key A: ",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                            Text(
                                                sector.keyA,
                                                fontFamily = FontFamily.Monospace,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (sector.isDefaultKeyA) {
                                                    MaterialTheme.colorScheme.outline
                                                } else {
                                                    MaterialTheme.colorScheme.error
                                                }
                                            )
                                            IconButton(
                                                onClick = { onCopyKey(sector.keyA) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.ContentCopy,
                                                    contentDescription = "Copy Key A",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                "Key B: ",
                                                style = MaterialTheme.typography.labelSmall
                                            )
                                            Text(
                                                sector.keyB,
                                                fontFamily = FontFamily.Monospace,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = if (sector.isDefaultKeyB) {
                                                    MaterialTheme.colorScheme.outline
                                                } else {
                                                    MaterialTheme.colorScheme.error
                                                }
                                            )
                                            IconButton(
                                                onClick = { onCopyKey(sector.keyB) },
                                                modifier = Modifier.size(24.dp)
                                            ) {
                                                Icon(
                                                    Icons.Default.ContentCopy,
                                                    contentDescription = "Copy Key B",
                                                    modifier = Modifier.size(16.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            "Access: ${sector.accessBits}",
                                            fontFamily = FontFamily.Monospace,
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                    } else {
                        Column(modifier = Modifier.padding(vertical = 4.dp)) {
                            Text(
                                "Block ${block.number}",
                                style = MaterialTheme.typography.labelSmall
                            )
                            Text(
                                block.toHexString(),
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.bodySmall
                            )
                            Text(
                                block.toAsciiString(),
                                fontFamily = FontFamily.Monospace,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.outline
                            )
                        }
                    }

                    if (idx < 3) {
                        Divider(modifier = Modifier.padding(vertical = 4.dp))
                    }
                }
            }
        }
    }
}
