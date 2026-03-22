package com.sushiomsky.proxmark.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.sushiomsky.proxmark.reader.ReaderConnectionState
import com.sushiomsky.proxmark.reader.ReaderType
import com.sushiomsky.proxmark.usb.UsbConnectionState

@Composable
internal fun ReaderSelectorCard(
    activeReader: ReaderType,
    readerStates: Map<ReaderType, ReaderConnectionState>,
    onSelectReader: (ReaderType) -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Reader", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ReaderType.entries.forEach { type ->
                    val state = readerStates[type]
                    val stateIcon = when (state) {
                        ReaderConnectionState.Connected -> "●"
                        ReaderConnectionState.Connecting -> "◐"
                        is ReaderConnectionState.Error -> "✕"
                        else -> "○"
                    }
                    val label = when (type) {
                        ReaderType.PROXMARK3 -> "PM3"
                        ReaderType.BUILTIN_NFC -> "NFC"
                        ReaderType.ACR_USB -> "ACR"
                    }
                    if (type == activeReader) {
                        Button(
                            onClick = {},
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("$stateIcon $label")
                        }
                    } else {
                        OutlinedButton(
                            onClick = { onSelectReader(type) },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("$stateIcon $label")
                        }
                    }
                }
            }
        }
    }
}

internal fun connectionText(state: UsbConnectionState): String {
    return when (state) {
        UsbConnectionState.Connected -> "Connected"
        UsbConnectionState.Connecting -> "Connecting..."
        UsbConnectionState.Disconnected -> "Disconnected"
        UsbConnectionState.PermissionDenied -> "Permission denied"
        is UsbConnectionState.Error -> "Error: ${state.message}"
    }
}

internal fun connectionColor(state: UsbConnectionState): Color {
    return when (state) {
        UsbConnectionState.Connected -> Color(0xFF2E7D32)
        UsbConnectionState.Connecting -> Color(0xFFEF6C00)
        UsbConnectionState.Disconnected -> Color(0xFF616161)
        UsbConnectionState.PermissionDenied -> Color(0xFFC62828)
        is UsbConnectionState.Error -> Color(0xFFC62828)
    }
}

internal fun readerStateText(state: ReaderConnectionState?): String {
    return when (state) {
        ReaderConnectionState.Connected -> "Connected"
        ReaderConnectionState.Connecting -> "Connecting..."
        ReaderConnectionState.Disconnected -> "Disconnected"
        is ReaderConnectionState.Error -> "Error: ${state.message}"
        null -> "Unknown"
    }
}

internal fun readerStateColor(state: ReaderConnectionState?): Color {
    return when (state) {
        ReaderConnectionState.Connected -> Color(0xFF2E7D32)
        ReaderConnectionState.Connecting -> Color(0xFFEF6C00)
        ReaderConnectionState.Disconnected -> Color(0xFF616161)
        is ReaderConnectionState.Error -> Color(0xFFC62828)
        null -> Color(0xFF616161)
    }
}
