package com.example.proxmark.reader

import android.content.Context
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import com.example.proxmark.repository.TagInfo
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext

/**
 * CardReader for ACR USB smart card readers (ACR122U, ACR1252U, etc.)
 * using CCID bulk transfer protocol with PC/SC-lite APDU wrapping.
 */
class AcrUsbReader(private val context: Context) : CardReader {

    override val readerType = ReaderType.ACR_USB

    companion object {
        /** Known ACR reader USB vendor/product IDs */
        val SUPPORTED_IDS: Set<Pair<Int, Int>> = setOf(
            0x072F to 0x2200, // ACR122U
            0x072F to 0x2100, // ACR1251U
            0x072F to 0x0901, // ACR1252U (dual)
            0x072F to 0x1252, // ACR1252U (alt)
            0x072F to 0x8201, // ACR1255U-J1
        )

        // PC/SC pseudo-APDU: GET UID via ACR firmware command
        private val CMD_GET_UID = byteArrayOf(
            0xFF.toByte(), 0xCA.toByte(), 0x00, 0x00, 0x00
        )

        // PC/SC pseudo-APDU: GET ATS (for 14443-4 cards)
        private val CMD_GET_ATS = byteArrayOf(
            0xFF.toByte(), 0xCA.toByte(), 0x01, 0x00, 0x00
        )

        // CCID message types
        private const val PC_TO_RDR_XFRBLOCK: Byte = 0x6F
        private const val RDR_TO_PC_DATABLOCK: Byte = 0x80.toByte()
    }

    private val _connectionState = MutableStateFlow<ReaderConnectionState>(ReaderConnectionState.Disconnected)
    override val connectionState: StateFlow<ReaderConnectionState> = _connectionState.asStateFlow()

    private val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    private val ioLock = Any()

    private var currentDevice: UsbDevice? = null
    private var currentConnection: UsbDeviceConnection? = null
    private var currentInterface: UsbInterface? = null
    private var inEndpoint: UsbEndpoint? = null
    private var outEndpoint: UsbEndpoint? = null
    private var ccidSequence: Byte = 0

    fun isAvailable(): Boolean = findAcrDevice() != null

    override fun connect() {
        _connectionState.value = ReaderConnectionState.Connecting
        val device = findAcrDevice()
        if (device == null) {
            _connectionState.value = ReaderConnectionState.Error("ACR reader not found")
            return
        }

        if (!usbManager.hasPermission(device)) {
            _connectionState.value = ReaderConnectionState.Error("USB permission required for ACR reader")
            return
        }

        openConnection(device)
    }

    override fun disconnect() {
        synchronized(ioLock) {
            currentConnection?.let { conn ->
                currentInterface?.let { conn.releaseInterface(it) }
                conn.close()
            }
            currentDevice = null
            currentConnection = null
            currentInterface = null
            inEndpoint = null
            outEndpoint = null
            _connectionState.value = ReaderConnectionState.Disconnected
        }
    }

    override suspend fun readTag(): TagInfo? = withContext(Dispatchers.IO) {
        try {
            val uidResponse = transceiveApdu(CMD_GET_UID) ?: return@withContext null
            if (uidResponse.size < 2) return@withContext null

            // Last 2 bytes are SW1 SW2
            val sw1 = uidResponse[uidResponse.size - 2].toInt() and 0xFF
            val sw2 = uidResponse[uidResponse.size - 1].toInt() and 0xFF
            if (sw1 != 0x90 || sw2 != 0x00) return@withContext null

            val uidBytes = uidResponse.copyOfRange(0, uidResponse.size - 2)
            val uid = uidBytes.joinToString("") { String.format("%02X", it) }

            TagInfo(
                uid = uid,
                type = "Smart Card (ACR)"
            )
        } catch (_: IOException) {
            null
        }
    }

    override fun close() = disconnect()

    /**
     * Send a PC/SC APDU through the CCID bulk pipe and read the response.
     */
    @Throws(IOException::class)
    private fun transceiveApdu(apdu: ByteArray): ByteArray? {
        synchronized(ioLock) {
            val conn = currentConnection ?: throw IOException("ACR not connected")
            val outEp = outEndpoint ?: throw IOException("No OUT endpoint")
            val inEp = inEndpoint ?: throw IOException("No IN endpoint")

            // Build CCID PC_to_RDR_XfrBlock message
            val ccidMsg = buildCcidXfrBlock(apdu)
            val sent = conn.bulkTransfer(outEp, ccidMsg, ccidMsg.size, 2000)
            if (sent <= 0) throw IOException("CCID write failed")

            // Read CCID response
            val response = ByteArray(512)
            val received = conn.bulkTransfer(inEp, response, response.size, 3000)
            if (received < 10) return null

            // Validate CCID response header
            if (response[0] != RDR_TO_PC_DATABLOCK) return null

            // Data length (little-endian 4 bytes at offset 1)
            val dataLen = (response[1].toInt() and 0xFF) or
                ((response[2].toInt() and 0xFF) shl 8) or
                ((response[3].toInt() and 0xFF) shl 16) or
                ((response[4].toInt() and 0xFF) shl 24)

            if (dataLen <= 0 || 10 + dataLen > received) return null

            return response.copyOfRange(10, 10 + dataLen)
        }
    }

    private fun buildCcidXfrBlock(apdu: ByteArray): ByteArray {
        val len = apdu.size
        val msg = ByteArray(10 + len)
        msg[0] = PC_TO_RDR_XFRBLOCK
        // Data length (little-endian)
        msg[1] = (len and 0xFF).toByte()
        msg[2] = ((len shr 8) and 0xFF).toByte()
        msg[3] = ((len shr 16) and 0xFF).toByte()
        msg[4] = ((len shr 24) and 0xFF).toByte()
        // Slot number
        msg[5] = 0
        // Sequence number
        msg[6] = ccidSequence++
        // BWI + reserved
        msg[7] = 0; msg[8] = 0; msg[9] = 0

        System.arraycopy(apdu, 0, msg, 10, len)
        return msg
    }

    private fun findAcrDevice(): UsbDevice? {
        return usbManager.deviceList.values.firstOrNull { device ->
            SUPPORTED_IDS.contains(device.vendorId to device.productId)
        }
    }

    private fun openConnection(device: UsbDevice) {
        synchronized(ioLock) {
            val endpoints = selectBulkEndpoints(device)
            if (endpoints == null) {
                _connectionState.value = ReaderConnectionState.Error("ACR bulk endpoints not found")
                return
            }

            val connection = usbManager.openDevice(device)
            if (connection == null) {
                _connectionState.value = ReaderConnectionState.Error("Cannot open ACR USB device")
                return
            }

            if (!connection.claimInterface(endpoints.first, true)) {
                connection.close()
                _connectionState.value = ReaderConnectionState.Error("Cannot claim ACR USB interface")
                return
            }

            currentDevice = device
            currentConnection = connection
            currentInterface = endpoints.first
            inEndpoint = endpoints.second
            outEndpoint = endpoints.third
            ccidSequence = 0
            _connectionState.value = ReaderConnectionState.Connected
        }
    }

    private fun selectBulkEndpoints(device: UsbDevice): Triple<UsbInterface, UsbEndpoint, UsbEndpoint>? {
        for (i in 0 until device.interfaceCount) {
            val iface = device.getInterface(i)
            var inEp: UsbEndpoint? = null
            var outEp: UsbEndpoint? = null

            for (j in 0 until iface.endpointCount) {
                val ep = iface.getEndpoint(j)
                if (ep.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    if (ep.direction == UsbConstants.USB_DIR_IN && inEp == null) inEp = ep
                    if (ep.direction == UsbConstants.USB_DIR_OUT && outEp == null) outEp = ep
                }
            }

            if (inEp != null && outEp != null) {
                return Triple(iface, inEp, outEp)
            }
        }
        return null
    }
}
