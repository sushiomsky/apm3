package com.example.proxmark.reader

import com.example.proxmark.repository.TagInfo
import kotlinx.coroutines.flow.StateFlow

/**
 * Common connection states across all reader types.
 */
sealed interface ReaderConnectionState {
    data object Disconnected : ReaderConnectionState
    data object Connecting : ReaderConnectionState
    data object Connected : ReaderConnectionState
    data class Error(val message: String) : ReaderConnectionState
}

/**
 * Abstraction for any tag/card reader device.
 * Implementations: Proxmark3 (USB), Android built-in NFC, ACR USB readers.
 */
interface CardReader {

    val readerType: ReaderType

    val connectionState: StateFlow<ReaderConnectionState>

    /** Connect to the reader hardware. */
    fun connect()

    /** Disconnect from the reader hardware. */
    fun disconnect()

    /** Whether this reader supports executing arbitrary text commands (PM3 only). */
    val supportsCommands: Boolean get() = false

    /**
     * Execute a text command and return the response.
     * Only supported by PM3. Others throw [UnsupportedOperationException].
     */
    suspend fun executeCommand(command: String, timeoutMs: Long = 4_000): String {
        throw UnsupportedOperationException("${readerType.label} does not support text commands")
    }

    /**
     * Read the tag currently on/near the reader.
     * Returns null if no tag is detected.
     */
    suspend fun readTag(): TagInfo?

    /** Release all resources. */
    fun close()
}
