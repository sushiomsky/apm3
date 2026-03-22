package com.example.proxmark.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
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
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.proxmark.persistence.AppSettings
import com.example.proxmark.persistence.ThemeMode

@Composable
internal fun SettingsSection(
    settings: AppSettings,
    connected: Boolean,
    autoScanEnabled: Boolean,
    rawCommandMode: Boolean,
    onUpdateTimeout: (Int) -> Unit,
    onUpdateThemeMode: (ThemeMode) -> Unit,
    onSetAutoScanEnabled: (Boolean) -> Unit,
    onUpdateAutoScanInterval: (Int) -> Unit,
    onToggleRawCommandMode: () -> Unit,
    onShowUsbDebugInfo: () -> String = { "" }
) {
    var expanded by remember { mutableStateOf(false) }
    var showUsbDebugDialog by remember { mutableStateOf(false) }
    var usbDebugInfo by remember { mutableStateOf("") }
    
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Settings", style = MaterialTheme.typography.titleMedium)
                Text(
                    text = if (expanded) "▲" else "▼",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (expanded) {
                // Theme toggle
                Text("Theme", style = MaterialTheme.typography.bodySmall)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ThemeMode.entries.forEach { mode ->
                        val label = when (mode) {
                            ThemeMode.SYSTEM -> "System"
                            ThemeMode.LIGHT -> "Light"
                            ThemeMode.DARK -> "Dark"
                        }
                        if (settings.themeMode == mode) {
                            Button(onClick = {}) { Text(label) }
                        } else {
                            OutlinedButton(onClick = { onUpdateThemeMode(mode) }) { Text(label) }
                        }
                    }
                }

                HorizontalDivider()

                // Command timeout
                Text(
                    "Command timeout: ${settings.commandTimeoutMs / 1000}s",
                    style = MaterialTheme.typography.bodySmall
                )
                Slider(
                    value = settings.commandTimeoutMs.toFloat(),
                    onValueChange = { onUpdateTimeout(it.toInt()) },
                    valueRange = AppSettings.MIN_TIMEOUT_MS.toFloat()..AppSettings.MAX_TIMEOUT_MS.toFloat(),
                    steps = ((AppSettings.MAX_TIMEOUT_MS - AppSettings.MIN_TIMEOUT_MS) / 1000) - 1
                )
                Text(
                    "Adjust how long to wait for a PM3 response before timing out.",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                HorizontalDivider()

                // Auto-scan toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Auto-scan", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "Continuously scan for tags when connected",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = autoScanEnabled,
                        onCheckedChange = onSetAutoScanEnabled,
                        enabled = connected
                    )
                }
                if (autoScanEnabled) {
                    Text(
                        "Scan interval: ${settings.autoScanIntervalMs / 1000}s",
                        style = MaterialTheme.typography.bodySmall
                    )
                    Slider(
                        value = settings.autoScanIntervalMs.toFloat(),
                        onValueChange = { onUpdateAutoScanInterval(it.toInt()) },
                        valueRange = AppSettings.MIN_AUTO_SCAN_INTERVAL_MS.toFloat()..AppSettings.MAX_AUTO_SCAN_INTERVAL_MS.toFloat(),
                        steps = ((AppSettings.MAX_AUTO_SCAN_INTERVAL_MS - AppSettings.MIN_AUTO_SCAN_INTERVAL_MS) / 1000) - 1
                    )
                }

                HorizontalDivider()

                // Raw command mode toggle
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Raw command mode", style = MaterialTheme.typography.bodySmall)
                        Text(
                            "Send arbitrary PM3 text commands (advanced)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = rawCommandMode,
                        onCheckedChange = { onToggleRawCommandMode() }
                    )
                }

                HorizontalDivider()

                // USB Debug Info
                OutlinedButton(
                    onClick = {
                        usbDebugInfo = onShowUsbDebugInfo()
                        showUsbDebugDialog = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("🔍 Show USB Devices (Debug)")
                }
                Text(
                    "Display connected USB devices and their details",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
    
    if (showUsbDebugDialog) {
        AlertDialog(
            onDismissRequest = { showUsbDebugDialog = false },
            title = { Text("USB Device Information") },
            text = {
                Text(
                    text = usbDebugInfo,
                    fontFamily = FontFamily.Monospace,
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                TextButton(onClick = { showUsbDebugDialog = false }) {
                    Text("Close")
                }
            }
        )
    }
}

@Composable
internal fun RawCommandCard(
    enabled: Boolean,
    onSend: (String) -> Unit,
    onClose: () -> Unit
) {
    var command by remember { mutableStateOf("") }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Raw Command", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = onClose) { Text("✕") }
            }
            Text(
                "Send arbitrary PM3 text commands. Use with caution.",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            OutlinedTextField(
                value = command,
                onValueChange = { command = it },
                label = { Text("e.g. hw version") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                enabled = enabled
            )
            Button(
                onClick = {
                    if (command.isNotBlank()) {
                        onSend(command.trim())
                        command = ""
                    }
                },
                enabled = enabled && command.isNotBlank()
            ) {
                Text("Send")
            }
        }
    }
}
