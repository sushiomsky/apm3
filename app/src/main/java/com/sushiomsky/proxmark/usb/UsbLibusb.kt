package com.sushiomsky.proxmark.usb

import android.util.Log

/**
 * JNI bridge to libusb for direct USB I/O.
 *
 * Used as a fallback when Android's UsbDeviceConnection.bulkTransfer()
 * and UsbRequest fail for certain CDC devices (e.g., PM3 Easy clones).
 *
 * Usage:
 *   1. Call [init] with the file descriptor from UsbDeviceConnection.fileDescriptor
 *   2. Call [claimInterface] for the data interface
 *   3. Call [initCdc] for CDC line coding + DTR
 *   4. Use [bulkWrite] / [bulkRead] for I/O
 *   5. Call [close] when done
 */
class UsbLibusb {

    companion object {
        private const val TAG = "UsbLibusb"

        /** Returns true if the native library loaded successfully. */
        val isAvailable: Boolean by lazy {
            try {
                System.loadLibrary("proxmark_native")
                Log.i(TAG, "proxmark_native library loaded")
                true
            } catch (e: UnsatisfiedLinkError) {
                Log.e(TAG, "Failed to load proxmark_native: ${e.message}")
                false
            }
        }
    }

    private var initialized = false

    /**
     * Wrap an Android USB file descriptor into a libusb handle.
     * @return true on success
     */
    fun init(fileDescriptor: Int): Boolean {
        if (!isAvailable) return false
        val rc = nativeInit(fileDescriptor)
        initialized = rc == 0
        if (!initialized) {
            Log.e(TAG, "init(fd=$fileDescriptor) failed: rc=$rc")
        }
        return initialized
    }

    /**
     * Claim a USB interface.
     * @return true on success
     */
    fun claimInterface(interfaceNum: Int): Boolean {
        if (!initialized) return false
        return nativeClaimInterface(interfaceNum) == 0
    }

    /**
     * Send CDC SET_LINE_CODING + DTR/RTS toggle.
     */
    fun initCdc(controlInterface: Int, baudRate: Int = 115200): Boolean {
        if (!initialized) return false

        val lcResult = nativeSetLineCoding(controlInterface, baudRate)
        if (lcResult < 0) {
            Log.e(TAG, "SET_LINE_CODING failed: rc=$lcResult")
            return false
        }

        // DTR off → on
        nativeSetControlLineState(controlInterface, dtr = false, rts = false)
        Thread.sleep(50)
        nativeSetControlLineState(controlInterface, dtr = true, rts = true)
        Thread.sleep(200)

        Log.i(TAG, "CDC init complete (baud=$baudRate)")
        return true
    }

    /**
     * Synchronous bulk write.
     * @return bytes transferred, or negative error code
     */
    fun bulkWrite(endpoint: Int, data: ByteArray, length: Int, timeoutMs: Int): Int {
        if (!initialized) return -99
        return nativeBulkWrite(endpoint, data, length, timeoutMs)
    }

    /**
     * Synchronous bulk read.
     * @return bytes transferred (0 = timeout), or negative error code
     */
    fun bulkRead(endpoint: Int, buffer: ByteArray, length: Int, timeoutMs: Int): Int {
        if (!initialized) return -99
        return nativeBulkRead(endpoint, buffer, length, timeoutMs)
    }

    fun close() {
        if (initialized) {
            nativeClose()
            initialized = false
        }
    }

    // --- JNI declarations ---
    private external fun nativeInit(fd: Int): Int
    private external fun nativeClaimInterface(interfaceNum: Int): Int
    private external fun nativeSetLineCoding(interfaceNum: Int, baudRate: Int): Int
    private external fun nativeSetControlLineState(interfaceNum: Int, dtr: Boolean, rts: Boolean): Int
    private external fun nativeBulkWrite(endpoint: Int, data: ByteArray, length: Int, timeoutMs: Int): Int
    private external fun nativeBulkRead(endpoint: Int, buffer: ByteArray, length: Int, timeoutMs: Int): Int
    private external fun nativeResetDevice(): Int
    private external fun nativeInterruptRead(endpoint: Int, data: ByteArray, length: Int, timeoutMs: Int): Int
    private external fun nativeDiagnostic(outEp: Int, inEp: Int, intEp: Int, pingPacket: ByteArray, pingLen: Int): Int
    private external fun nativeClose()

    fun resetDevice(): Int {
        if (!initialized) return -99
        return nativeResetDevice()
    }

    fun interruptRead(endpoint: Int, buffer: ByteArray, length: Int, timeoutMs: Int): Int {
        if (!initialized) return -99
        return nativeInterruptRead(endpoint, buffer, length, timeoutMs)
    }

    fun runDiagnostic(outEp: Int, inEp: Int, intEp: Int, pingPacket: ByteArray): Int {
        if (!initialized) return -99
        return nativeDiagnostic(outEp, inEp, intEp, pingPacket, pingPacket.size)
    }
}
