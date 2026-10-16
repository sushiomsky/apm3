package com.sushiomsky.proxmark.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ClearAll
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sushiomsky.proxmark.protocol.TerminalLine
import com.sushiomsky.proxmark.protocol.TerminalLineType

private val terminalBg = Color(0xFF101512)
private val sentColor = Color(0xFF00BCD4)
private val receivedColor = Color(0xFF4ADE80)
private val errorColor = Color(0xFFFF5252)
private val systemColor = Color(0xFF9E9E9E)

@Composable
fun TerminalScreen(
    terminalLines: List<TerminalLine>,
    connected: Boolean,
    advancedMode: Boolean = false,
    onSendCommand: (String) -> Unit,
    onClear: () -> Unit,
    onShowAdvancedPanel: () -> Unit = {},
    onExportLog: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var commandInput by rememberSaveable { mutableStateOf("") }
    val listState = rememberLazyListState()

    // Follow output only when the user is already at the bottom —
    // don't yank the list away while they're reading earlier lines.
    LaunchedEffect(terminalLines.size) {
        if (terminalLines.isNotEmpty()) {
            val lastVisible = listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            if (lastVisible >= terminalLines.size - 3) {
                listState.animateScrollToItem(terminalLines.size - 1)
            }
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Terminal output area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(terminalBg)
        ) {
            if (terminalLines.isEmpty()) {
                Text(
                    text = "Terminal ready. Connect PM3 to begin.",
                    color = systemColor,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    modifier = Modifier.padding(12.dp)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                    verticalArrangement = Arrangement.spacedBy(1.dp)
                ) {
                    items(terminalLines) { line ->
                        TerminalLineRow(line)
                    }
                }
            }

            if (advancedMode) {
                FloatingActionButton(
                    onClick = onShowAdvancedPanel,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Icon(Icons.Default.Terminal, contentDescription = "Advanced Mode")
                }
            }
        }

        // Command input bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant)
                .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "pm3>",
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = sentColor,
                modifier = Modifier.padding(end = 4.dp)
            )
            OutlinedTextField(
                value = commandInput,
                onValueChange = { commandInput = it },
                modifier = Modifier.weight(1f),
                placeholder = { Text("Enter command…", fontSize = 13.sp) },
                singleLine = true,
                enabled = connected,
                textStyle = MaterialTheme.typography.bodySmall.copy(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp
                ),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = {
                    if (commandInput.isNotBlank()) {
                        onSendCommand(commandInput.trim())
                        commandInput = ""
                    }
                })
            )
            IconButton(onClick = onExportLog, enabled = terminalLines.isNotEmpty()) {
                Icon(Icons.Filled.Share, contentDescription = "Export log")
            }
            IconButton(onClick = onClear, enabled = terminalLines.isNotEmpty()) {
                Icon(Icons.Filled.ClearAll, contentDescription = "Clear terminal")
            }
        }
    }
}

@Composable
private fun TerminalLineRow(line: TerminalLine) {
    val prefix: String
    val color: Color
    when (line.type) {
        TerminalLineType.SENT -> { prefix = "» "; color = sentColor }
        TerminalLineType.RECEIVED -> { prefix = "  "; color = receivedColor }
        TerminalLineType.ERROR -> { prefix = "✖ "; color = errorColor }
        TerminalLineType.SYSTEM -> { prefix = "• "; color = systemColor }
    }

    Text(
        text = "$prefix${stripAnsi(line.text)}",
        fontFamily = FontFamily.Monospace,
        fontSize = 12.sp,
        color = color,
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(vertical = 1.dp),
        maxLines = 10
    )
}

/** Strip ANSI escape sequences (color codes, cursor moves) from PM3 output. */
private val ansiRegex = Regex("\u001B\\[[0-9;]*[A-Za-z]")
fun stripAnsi(text: String): String = ansiRegex.replace(text, "")
