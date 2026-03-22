package com.example.proxmark.ui.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.proxmark.features.ProxmarkFeature
import com.example.proxmark.viewmodel.CommandMetrics

private val pm3Prefixes = listOf(
    "hf", "lf", "hw", "mem", "data", "script", "trace", "analyse", "wiegand", "usart"
)

private data class QuickCommand(val label: String, val command: String)

private val quickCommands = listOf(
    QuickCommand("hw status", "hw status"),
    QuickCommand("hw tune", "hw tune"),
    QuickCommand("hw version", "hw version"),
    QuickCommand("hf 14a info", "hf 14a info"),
    QuickCommand("hf 14a reader", "hf 14a reader"),
    QuickCommand("hf 14a sim", "hf 14a sim"),
    QuickCommand("hf mf rdbl", "hf mf rdbl"),
    QuickCommand("hf mf wrbl", "hf mf wrbl"),
    QuickCommand("hf mf dump", "hf mf dump"),
    QuickCommand("lf search", "lf search"),
    QuickCommand("lf read", "lf read"),
    QuickCommand("lf sim", "lf sim"),
    QuickCommand("data plot", "data plot"),
    QuickCommand("data save", "data save"),
    QuickCommand("trace list", "trace list")
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun AdvancedModePanel(
    connected: Boolean,
    commandHistory: List<String>,
    commandMetrics: CommandMetrics,
    showProtocolInspector: Boolean,
    features: List<ProxmarkFeature>,
    onSendCommand: (String) -> Unit,
    onToggleProtocolInspector: () -> Unit,
    onClearHistory: () -> Unit,
    onDismiss: () -> Unit
) {
    var commandInput by remember { mutableStateOf("") }
    var showHistoryDropdown by remember { mutableStateOf(false) }
    var showSuggestions by remember { mutableStateOf(false) }

    val suggestions = remember(commandInput, features) {
        if (commandInput.isBlank()) {
            emptyList()
        } else {
            val typed = commandInput.trim().lowercase()
            // Match PM3 prefixes
            val prefixMatches = pm3Prefixes.filter { it.startsWith(typed) && it != typed }
            // Match feature commands from catalog
            val catalogMatches = features
                .mapNotNull { it.command }
                .filter { it.startsWith(typed, ignoreCase = true) && !it.equals(typed, ignoreCase = true) }
                .distinct()
                .take(8)
            (prefixMatches + catalogMatches).distinct().take(10)
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.surface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                TopAppBar(
                    title = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                Icons.Default.Terminal,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Advanced Mode", fontWeight = FontWeight.Bold)
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onDismiss) {
                            Icon(Icons.Default.Close, "Close")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                )

                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // ── Raw Command Input ──
                    item {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = Color(0xFF1A1A2E)
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Raw Command",
                                        style = MaterialTheme.typography.titleSmall,
                                        color = Color(0xFF00BCD4),
                                        fontFamily = FontFamily.Monospace
                                    )
                                    Row {
                                        Box {
                                            IconButton(
                                                onClick = { showHistoryDropdown = true },
                                                enabled = commandHistory.isNotEmpty()
                                            ) {
                                                Icon(
                                                    Icons.Default.History,
                                                    "History",
                                                    tint = if (commandHistory.isNotEmpty())
                                                        Color(0xFF00BCD4) else Color.Gray
                                                )
                                            }
                                            DropdownMenu(
                                                expanded = showHistoryDropdown,
                                                onDismissRequest = { showHistoryDropdown = false }
                                            ) {
                                                commandHistory.forEach { cmd ->
                                                    DropdownMenuItem(
                                                        text = {
                                                            Text(
                                                                cmd,
                                                                fontFamily = FontFamily.Monospace,
                                                                fontSize = 13.sp
                                                            )
                                                        },
                                                        onClick = {
                                                            commandInput = cmd
                                                            showHistoryDropdown = false
                                                        }
                                                    )
                                                }
                                                if (commandHistory.isNotEmpty()) {
                                                    DropdownMenuItem(
                                                        text = {
                                                            Text(
                                                                "Clear history",
                                                                color = MaterialTheme.colorScheme.error,
                                                                fontSize = 13.sp
                                                            )
                                                        },
                                                        leadingIcon = {
                                                            Icon(
                                                                Icons.Default.Delete,
                                                                null,
                                                                tint = MaterialTheme.colorScheme.error
                                                            )
                                                        },
                                                        onClick = {
                                                            onClearHistory()
                                                            showHistoryDropdown = false
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        "pm3>",
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 14.sp,
                                        color = Color(0xFF00BCD4)
                                    )
                                    OutlinedTextField(
                                        value = commandInput,
                                        onValueChange = {
                                            commandInput = it
                                            showSuggestions = it.isNotBlank()
                                        },
                                        modifier = Modifier.weight(1f),
                                        placeholder = {
                                            Text(
                                                "Enter PM3 command…",
                                                fontSize = 13.sp,
                                                color = Color.Gray
                                            )
                                        },
                                        singleLine = true,
                                        enabled = connected,
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 14.sp,
                                            color = Color(0xFF4CAF50)
                                        ),
                                        colors = OutlinedTextFieldDefaults.colors(
                                            focusedBorderColor = Color(0xFF00BCD4),
                                            unfocusedBorderColor = Color(0xFF333355),
                                            cursorColor = Color(0xFF00BCD4)
                                        ),
                                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                                        keyboardActions = KeyboardActions(onSend = {
                                            if (commandInput.isNotBlank()) {
                                                onSendCommand(commandInput.trim())
                                                commandInput = ""
                                                showSuggestions = false
                                            }
                                        })
                                    )
                                    IconButton(
                                        onClick = {
                                            if (commandInput.isNotBlank()) {
                                                onSendCommand(commandInput.trim())
                                                commandInput = ""
                                                showSuggestions = false
                                            }
                                        },
                                        enabled = connected && commandInput.isNotBlank()
                                    ) {
                                        Icon(
                                            Icons.Default.Send,
                                            "Send",
                                            tint = if (connected && commandInput.isNotBlank())
                                                Color(0xFF00BCD4) else Color.Gray
                                        )
                                    }
                                }

                                // Autocomplete suggestions
                                if (showSuggestions && suggestions.isNotEmpty()) {
                                    Spacer(Modifier.height(4.dp))
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState()),
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        suggestions.forEach { suggestion ->
                                            AssistChip(
                                                onClick = {
                                                    commandInput = suggestion
                                                    showSuggestions = false
                                                },
                                                label = {
                                                    Text(
                                                        suggestion,
                                                        fontFamily = FontFamily.Monospace,
                                                        fontSize = 11.sp
                                                    )
                                                },
                                                colors = AssistChipDefaults.assistChipColors(
                                                    containerColor = Color(0xFF252545)
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    // ── Quick Commands ──
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Text(
                                    "⚡ Quick Commands",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(Modifier.height(8.dp))
                                FlowRow(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    quickCommands.forEach { qc ->
                                        AssistChip(
                                            onClick = {
                                                if (connected) {
                                                    onSendCommand(qc.command)
                                                }
                                            },
                                            label = {
                                                Text(
                                                    qc.label,
                                                    fontFamily = FontFamily.Monospace,
                                                    fontSize = 11.sp
                                                )
                                            },
                                            enabled = connected
                                        )
                                    }
                                }
                            }
                        }
                    }

                    // ── Timing & Performance Metrics ──
                    item {
                        MetricsCard(commandMetrics)
                    }

                    // ── Protocol Inspector Toggle ──
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            Icons.Default.Science,
                                            contentDescription = null,
                                            modifier = Modifier.size(20.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(Modifier.width(8.dp))
                                        Column {
                                            Text(
                                                "Protocol Inspector",
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.Bold
                                            )
                                            Text(
                                                "Show raw hex frames in terminal",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                    Switch(
                                        checked = showProtocolInspector,
                                        onCheckedChange = { onToggleProtocolInspector() }
                                    )
                                }

                                if (showProtocolInspector) {
                                    Spacer(Modifier.height(8.dp))
                                    Card(
                                        colors = CardDefaults.cardColors(
                                            containerColor = Color(0xFF1A1A2E)
                                        ),
                                        shape = RoundedCornerShape(8.dp)
                                    ) {
                                        Column(modifier = Modifier.padding(8.dp)) {
                                            Text(
                                                "Packet Structure:",
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp,
                                                color = Color(0xFF9E9E9E)
                                            )
                                            Spacer(Modifier.height(4.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(2.dp)
                                            ) {
                                                ProtocolField("MAGIC", Color(0xFF00BCD4))
                                                ProtocolField("LEN", Color(0xFFFFC107))
                                                ProtocolField("CMD", Color(0xFF4CAF50))
                                                ProtocolField("DATA", Color(0xFFFF9800))
                                                ProtocolField("CRC", Color(0xFFFF5252))
                                            }
                                            Spacer(Modifier.height(4.dp))
                                            Row(
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                LegendDot(Color(0xFF00BCD4), "Sent")
                                                LegendDot(Color(0xFF4CAF50), "Received")
                                                LegendDot(Color(0xFFFF5252), "Error")
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }

                    item { Spacer(Modifier.height(12.dp)) }
                }
            }
        }
    }
}

@Composable
private fun MetricsCard(metrics: CommandMetrics) {
    val animatedSent by animateIntAsState(
        targetValue = metrics.commandsSent,
        label = "sentCount"
    )
    val animatedAvg by animateIntAsState(
        targetValue = metrics.averageResponseTimeMs.toInt(),
        label = "avgTime"
    )
    val animatedLast by animateIntAsState(
        targetValue = metrics.lastCommandTimeMs.toInt(),
        label = "lastTime"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Speed,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Performance Metrics",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                MetricItem("Commands", "$animatedSent")
                MetricItem("Last", "${animatedLast}ms")
                MetricItem("Avg", "${animatedAvg}ms")
                MetricItem("Uptime", formatUptime(metrics.uptimeMs))
            }
        }
    }
}

@Composable
private fun MetricItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp,
            color = MaterialTheme.colorScheme.primary
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ProtocolField(label: String, color: Color) {
    Box(
        modifier = Modifier
            .background(color.copy(alpha = 0.2f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            label,
            fontFamily = FontFamily.Monospace,
            fontSize = 10.sp,
            color = color,
            fontWeight = FontWeight.Bold
        )
    }
}

@Composable
private fun LegendDot(color: Color, label: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, RoundedCornerShape(4.dp))
        )
        Spacer(Modifier.width(4.dp))
        Text(label, fontSize = 10.sp, color = Color(0xFF9E9E9E))
    }
}

private fun formatUptime(ms: Long): String {
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "${hours}h${minutes}m" else "${minutes}m${seconds}s"
}
