package com.sushiomsky.proxmark.protocol

import com.sushiomsky.proxmark.usb.ProxmarkUsbManager
import com.sushiomsky.proxmark.usb.UsbConnectionState
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class ProxmarkClient(
    private val usbManager: ProxmarkUsbManager,
    private val scope: CoroutineScope
) {

    private val responseQueue = LinkedBlockingQueue<String>()

    // Binary protocol receive buffer
    private val rxBuffer = ByteArray(4096)
    private var rxBufferLen = 0
    private val rxLock = Any()

    private val _terminalLines = MutableSharedFlow<TerminalLine>(extraBufferCapacity = 512)
    val terminalLines: SharedFlow<TerminalLine> = _terminalLines.asSharedFlow()

    val connectionState: StateFlow<UsbConnectionState> = usbManager.connectionState

    private var readJob: Job? = null
    // Track which command we last sent so we can format the response
    @Volatile
    private var pendingCommandId: Short = 0

    init {
        scope.launch {
            usbManager.errors.collect { error ->
                _terminalLines.emit(TerminalLine(error, TerminalLineType.ERROR))
            }
        }

        scope.launch {
            connectionState.collect { state ->
                when (state) {
                    UsbConnectionState.Connected -> {
                        _terminalLines.emit(TerminalLine("USB connected", TerminalLineType.SYSTEM))
                        startReadLoop()
                        // Send PING + VERSION to verify binary protocol communication
                        scope.launch(Dispatchers.IO) {
                            delay(300) // let read loop start
                            try {
                                android.util.Log.i("ProxmarkClient", "Sending PING to verify communication")
                                usbManager.write(Pm3Protocol.buildPing())
                                delay(200)
                                android.util.Log.i("ProxmarkClient", "Sending VERSION request")
                                usbManager.write(Pm3Protocol.buildVersion())
                            } catch (e: Exception) {
                                android.util.Log.e("ProxmarkClient", "Auto-probe failed: ${e.message}")
                            }
                        }
                    }

                    UsbConnectionState.Connecting -> {
                        _terminalLines.emit(TerminalLine("Connecting to Proxmark3...", TerminalLineType.SYSTEM))
                    }

                    UsbConnectionState.Disconnected -> {
                        _terminalLines.emit(TerminalLine("USB disconnected", TerminalLineType.SYSTEM))
                        stopReadLoop()
                    }

                    UsbConnectionState.PermissionDenied -> {
                        _terminalLines.emit(TerminalLine("USB permission denied", TerminalLineType.ERROR))
                        stopReadLoop()
                    }

                    is UsbConnectionState.Error -> {
                        _terminalLines.emit(TerminalLine(state.message, TerminalLineType.ERROR))
                        stopReadLoop()
                    }
                }
            }
        }
    }

    fun start() {
        usbManager.start()
    }

    fun connect() {
        usbManager.connect()
    }

    fun reconnect() {
        usbManager.reconnect()
    }

    fun disconnect() {
        usbManager.disconnect()
    }

    fun sendCommand(cmd: String) {
        android.util.Log.d("ProxmarkClient", "sendCommand() called with: '$cmd'")
        val normalized = cmd.trim()
        if (normalized.isEmpty()) return

        // Clear stale responses from queue before sending new command
        responseQueue.clear()

        // Translate text command to PM3 binary packets
        val packets = Pm3Protocol.textCommandToPackets(normalized)
        if (packets == null) {
            android.util.Log.w("ProxmarkClient", "Unsupported command: '$normalized' — sending as raw text fallback")
            sendRawText(normalized)
            return
        }

        try {
            for (packet in packets) {
                // Extract command ID from packet for response routing
                // NG preamble: magic(4) + length_ng(2) + cmd(2) → cmd at offset 6
                if (packet.size >= 8) {
                    pendingCommandId = Pm3Protocol.getInt16LE(packet, 6)
                }
                android.util.Log.d("ProxmarkClient", "sendCommand() - sending binary packet (${packet.size} bytes) for '$normalized'")
                usbManager.write(packet)
            }
            _terminalLines.tryEmit(TerminalLine(normalized, TerminalLineType.SENT))
        } catch (io: IOException) {
            android.util.Log.e("ProxmarkClient", "sendCommand() - write failed: ${io.message}")
            _terminalLines.tryEmit(
                TerminalLine("Failed to send command: ${io.message}", TerminalLineType.ERROR)
            )
        }
    }

    /**
     * Fallback: send raw text for commands not yet mapped to binary protocol.
     */
    private fun sendRawText(cmd: String) {
        val payload = if (cmd.endsWith("\n")) cmd else "$cmd\n"
        try {
            usbManager.write(payload.toByteArray(Charsets.UTF_8))
            _terminalLines.tryEmit(TerminalLine(cmd, TerminalLineType.SENT))
        } catch (io: IOException) {
            _terminalLines.tryEmit(
                TerminalLine("Failed to send command: ${io.message}", TerminalLineType.ERROR)
            )
        }
    }

    @Throws(SocketTimeoutException::class)
    fun readResponse(timeoutMs: Long = 2_000): String {
        // Wait for first response line
        val first = responseQueue.poll(timeoutMs, TimeUnit.MILLISECONDS)
            ?: throw SocketTimeoutException("Timed out waiting for Proxmark response")

        // Drain additional lines that may be part of the same response
        val lines = mutableListOf(first)
        while (true) {
            val next = responseQueue.poll(150, TimeUnit.MILLISECONDS) ?: break
            lines.add(next)
        }
        return lines.joinToString("\n")
    }

    fun hfSearch() {
        sendCommand("hf search")
    }

    fun lfSearch() {
        sendCommand("lf search")
    }

    @Throws(SocketTimeoutException::class)
    fun executeCommand(command: String, timeoutMs: Long = 4_000): String {
        android.util.Log.d("ProxmarkClient", "executeCommand() called: '$command', timeout=${timeoutMs}ms")
        sendCommand(command)
        android.util.Log.d("ProxmarkClient", "executeCommand() - waiting for response...")
        return readResponse(timeoutMs)
    }

    fun close() {
        stopReadLoop()
        usbManager.stop()
    }

    private fun startReadLoop() {
        if (readJob?.isActive == true) {
            android.util.Log.d("ProxmarkClient", "startReadLoop() - already running, skipping")
            return
        }

        android.util.Log.d("ProxmarkClient", "startReadLoop() - starting new read loop")
        readJob = scope.launch(Dispatchers.IO) {
            val usbBuffer = ByteArray(512)
            android.util.Log.d("ProxmarkClient", "startReadLoop() - read loop launched, entering while loop")

            while (true) {
                val connected = usbManager.isConnected()
                if (!connected) {
                    delay(100)
                    continue
                }

                val readCount = try {
                    usbManager.read(usbBuffer, timeoutMs = 250)
                } catch (e: IOException) {
                    android.util.Log.e("ProxmarkClient", "Read exception: ${e.message}")
                    _terminalLines.tryEmit(
                        TerminalLine("USB read failed; waiting for reconnect", TerminalLineType.ERROR)
                    )
                    delay(250)
                    continue
                }

                if (readCount > 0) {
                    val hexDump = usbBuffer.take(minOf(readCount, 32)).joinToString(" ") { "%02x".format(it) }
                    android.util.Log.i("ProxmarkClient", "Read $readCount bytes: $hexDump")
                    processBinaryData(usbBuffer, readCount)
                } else {
                    delay(10)
                }
            }
        }
    }

    /**
     * Accumulate incoming bytes and try to decode PM3 binary responses.
     * Also handles debug_print_string messages which carry text output.
     */
    private fun processBinaryData(data: ByteArray, length: Int) {
        synchronized(rxLock) {
            // Append to receive buffer
            val spaceLeft = rxBuffer.size - rxBufferLen
            val toCopy = minOf(length, spaceLeft)
            data.copyInto(rxBuffer, rxBufferLen, 0, toCopy)
            rxBufferLen += toCopy

            // Try to decode complete packets
            var consumed = 0
            while (consumed < rxBufferLen) {
                val result = Pm3Protocol.decodeResponse(rxBuffer, consumed, rxBufferLen)
                if (result == null) break

                val (response, packetSize) = result
                consumed += packetSize

                android.util.Log.i("ProxmarkClient", "PM3 response: cmd=0x${(response.cmd.toInt() and 0xFFFF).toString(16)}, status=${response.status}, data=${response.data.size} bytes")

                // Convert binary response to text for terminal/parser
                val text = formatResponse(response)
                if (text.isNotBlank()) {
                    for (line in text.lines()) {
                        if (line.isNotBlank()) {
                            responseQueue.offer(line)
                            _terminalLines.tryEmit(TerminalLine(line, TerminalLineType.RECEIVED))
                        }
                    }
                }
            }

            // Compact buffer
            if (consumed > 0) {
                rxBuffer.copyInto(rxBuffer, 0, consumed, rxBufferLen)
                rxBufferLen -= consumed
            }

            // Prevent buffer overflow
            if (rxBufferLen >= rxBuffer.size - 512) {
                android.util.Log.w("ProxmarkClient", "RX buffer near full ($rxBufferLen bytes), discarding oldest data")
                val keep = rxBufferLen / 2
                rxBuffer.copyInto(rxBuffer, 0, rxBufferLen - keep, rxBufferLen)
                rxBufferLen = keep
            }
        }
    }

    /**
     * Convert a PM3 binary response into human-readable text.
     */
    private fun formatResponse(response: Pm3Protocol.Pm3Response): String {
        return when (response.cmd) {
            Pm3Protocol.CMD_VERSION -> Pm3Protocol.parseVersionResponse(response)
            Pm3Protocol.CMD_PING -> Pm3Protocol.parsePingResponse(response)
            Pm3Protocol.CMD_STATUS -> Pm3Protocol.parseStatusResponse(response)
            Pm3Protocol.CMD_CAPABILITIES -> Pm3Protocol.parseCapabilitiesResponse(response)
            Pm3Protocol.CMD_HF_ISO14443A_READER -> Pm3Protocol.parseHf14aResponse(response)
            Pm3Protocol.CMD_HF_MIFARE_READBL -> Pm3Protocol.parseMfReadBlockResponse(response)
            Pm3Protocol.CMD_HF_MIFARE_WRITEBL -> Pm3Protocol.parseMfWriteBlockResponse(response)
            Pm3Protocol.CMD_HF_MIFARE_CSETBL -> Pm3Protocol.parseMfCSetBlockResponse(response)
            Pm3Protocol.CMD_HW_TUNE_ANTENNA -> Pm3Protocol.parseTuneResponse(response)
            Pm3Protocol.CMD_DEBUG_PRINT_STRING -> Pm3Protocol.parseDebugString(response)
            Pm3Protocol.CMD_ACK -> {
                if (response.data.isNotEmpty()) {
                    "ACK: ${response.data.joinToString("") { "%02X".format(it) }}"
                } else {
                    "ACK"
                }
            }
            Pm3Protocol.CMD_WTX -> "" // Wait time extension, suppress
            else -> {
                val cmdHex = (response.cmd.toInt() and 0xFFFF).toString(16)
                if (response.isSuccess) {
                    "Response [0x$cmdHex]: ${response.data.size} bytes"
                } else {
                    "Response [0x$cmdHex]: status=${response.status}, ${response.data.size} bytes"
                }
            }
        }
    }

    private fun stopReadLoop() {
        readJob?.cancel()
        readJob = null
        synchronized(rxLock) {
            rxBufferLen = 0
        }
    }

    fun getConnectedDevicesInfo(): String = usbManager.getConnectedDevicesInfo()
}
