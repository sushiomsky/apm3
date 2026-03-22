package com.sushiomsky.proxmark.reader

import com.sushiomsky.proxmark.protocol.ProxmarkClient
import com.sushiomsky.proxmark.protocol.ProxmarkOutputParser
import com.sushiomsky.proxmark.repository.TagInfo
import com.sushiomsky.proxmark.usb.UsbConnectionState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * CardReader adapter wrapping the existing ProxmarkClient/UsbManager stack.
 */
class Proxmark3Reader(
    private val client: ProxmarkClient
) : CardReader {

    override val readerType = ReaderType.PROXMARK3
    override val supportsCommands = true

    private val _connectionState = MutableStateFlow<ReaderConnectionState>(ReaderConnectionState.Disconnected)
    override val connectionState: StateFlow<ReaderConnectionState> = _connectionState.asStateFlow()

    init {
        // Mirror USB connection state into the unified ReaderConnectionState
        // This is collected externally by the repository
    }

    fun syncConnectionState(usbState: UsbConnectionState) {
        _connectionState.value = when (usbState) {
            UsbConnectionState.Connected -> ReaderConnectionState.Connected
            UsbConnectionState.Connecting -> ReaderConnectionState.Connecting
            UsbConnectionState.Disconnected -> ReaderConnectionState.Disconnected
            UsbConnectionState.PermissionDenied -> ReaderConnectionState.Error("USB permission denied")
            is UsbConnectionState.Error -> ReaderConnectionState.Error(usbState.message)
        }
    }

    override fun connect() = client.connect()
    override fun disconnect() = client.disconnect()

    override suspend fun executeCommand(command: String, timeoutMs: Long): String {
        return client.executeCommand(command, timeoutMs)
    }

    override suspend fun readTag(): TagInfo? {
        val output = try {
            client.executeCommand("hf search", timeoutMs = 5_000)
        } catch (_: Exception) {
            return null
        }
        return ProxmarkOutputParser.parseTagInfo(output)
    }

    fun start() = client.start()

    override fun close() = client.close()
}
