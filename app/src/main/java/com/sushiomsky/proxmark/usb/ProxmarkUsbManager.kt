package com.sushiomsky.proxmark.usb

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbConstants
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbEndpoint
import android.hardware.usb.UsbInterface
import android.hardware.usb.UsbManager
import android.hardware.usb.UsbRequest
import android.os.Build
import androidx.core.content.ContextCompat
import java.io.IOException
import java.nio.ByteBuffer
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

class ProxmarkUsbManager(private val context: Context) {

    companion object {
        const val ACTION_USB_PERMISSION = "com.sushiomsky.proxmark.USB_PERMISSION"
        private val SUPPORTED_IDS: Set<Pair<Int, Int>> = setOf(
            0x2D2D to 0x504D,  // Proxmark3 RDV4
            0x9AC4 to 0x4B8F   // Proxmark3 Easy
        )
    }

    private data class EndpointSet(
        val usbInterface: UsbInterface,
        val inEndpoint: UsbEndpoint,
        val outEndpoint: UsbEndpoint
    )

    private val appContext = context.applicationContext
    private val usbManager = appContext.getSystemService(Context.USB_SERVICE) as UsbManager

    private val _connectionState = MutableStateFlow<UsbConnectionState>(UsbConnectionState.Disconnected)
    val connectionState: StateFlow<UsbConnectionState> = _connectionState.asStateFlow()

    private val _errors = MutableSharedFlow<String>(extraBufferCapacity = 32)
    val errors: SharedFlow<String> = _errors.asSharedFlow()

    private val ioLock = Any()
    
    /**
     * I/O fallback chain for CDC devices (PM3 Easy clones):
     *  1. Sync bulkTransfer (standard Android API)
     *  2. Async UsbRequest (URB-based, works on some quirky devices)
     *  3. libusb via JNI (bypasses Android USB Host API entirely)
     */
    private enum class IoMode { SYNC, ASYNC_REQUEST, LIBUSB }
    @Volatile
    private var ioMode = IoMode.SYNC
    private var consecutiveReadErrors = 0
    private val ASYNC_FALLBACK_THRESHOLD = 3
    private val LIBUSB_FALLBACK_THRESHOLD = 6
    
    // libusb JNI instance (lazy, only created if needed)
    private var libusb: UsbLibusb? = null

    private var receiverRegistered = false
    private var currentDevice: UsbDevice? = null
    private var currentConnection: UsbDeviceConnection? = null
    private var currentInterface: UsbInterface? = null
    private var inEndpoint: UsbEndpoint? = null
    private var outEndpoint: UsbEndpoint? = null

    private val permissionIntentFlags: Int
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE
        } else {
            PendingIntent.FLAG_UPDATE_CURRENT
        }

    private val permissionIntent: PendingIntent by lazy {
        PendingIntent.getBroadcast(
            appContext,
            0,
            Intent(ACTION_USB_PERMISSION).setPackage(appContext.packageName),
            permissionIntentFlags
        )
    }

    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            when (intent.action) {
                ACTION_USB_PERMISSION -> {
                    val device = intent.parcelableDevice()
                    val granted = intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)
                    if (granted && device != null) {
                        openConnection(device)
                    } else {
                        _connectionState.value = UsbConnectionState.PermissionDenied
                        _errors.tryEmit("USB permission denied")
                    }
                }

                UsbManager.ACTION_USB_DEVICE_ATTACHED -> {
                    val device = intent.parcelableDevice()
                    if (device != null && isSupported(device)) {
                        connect()
                    }
                }

                UsbManager.ACTION_USB_DEVICE_DETACHED -> {
                    val device = intent.parcelableDevice()
                    if (device != null && currentDevice?.deviceId == device.deviceId) {
                        disconnectInternal("Device disconnected")
                    }
                }
            }
        }
    }

    fun start() {
        if (!receiverRegistered) {
            val filter = IntentFilter().apply {
                addAction(ACTION_USB_PERMISSION)
                addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
                addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
            }
            ContextCompat.registerReceiver(
                appContext,
                usbReceiver,
                filter,
                ContextCompat.RECEIVER_NOT_EXPORTED
            )
            receiverRegistered = true
        }
        connect()
    }

    fun stop() {
        disconnect()
        if (receiverRegistered) {
            try {
                appContext.unregisterReceiver(usbReceiver)
            } catch (_: IllegalArgumentException) {
                // Receiver can already be unregistered by platform edge cases.
            }
            receiverRegistered = false
        }
    }

    fun connect() {
        _connectionState.value = UsbConnectionState.Connecting

        val device = findSupportedDevice()
        if (device == null) {
            _connectionState.value = UsbConnectionState.Disconnected
            _errors.tryEmit("Proxmark3 device not found")
            return
        }

        if (!usbManager.hasPermission(device)) {
            usbManager.requestPermission(device, permissionIntent)
            return
        }

        openConnection(device)
    }

    fun reconnect() {
        disconnect()
        connect()
    }

    fun disconnect() {
        disconnectInternal(reason = null)
    }

    fun isConnected(): Boolean = connectionState.value is UsbConnectionState.Connected

    @Throws(IOException::class)
    fun write(data: ByteArray, timeoutMs: Int = 2_000): Int {
        synchronized(ioLock) {
            val connection = currentConnection ?: throw IOException("USB connection is closed")
            val endpoint = outEndpoint ?: throw IOException("USB OUT endpoint not available")
            android.util.Log.d("ProxmarkUSB", "Writing ${data.size} bytes to endpoint 0x${endpoint.address.toString(16)} (mode=$ioMode)")
            
            val result = when (ioMode) {
                IoMode.LIBUSB -> {
                    val usb = libusb ?: throw IOException("libusb not initialized")
                    usb.bulkWrite(endpoint.address, data, data.size, timeoutMs)
                }
                IoMode.ASYNC_REQUEST -> writeAsync(connection, endpoint, data, timeoutMs)
                IoMode.SYNC -> connection.bulkTransfer(endpoint, data, data.size, timeoutMs)
            }
            
            android.util.Log.d("ProxmarkUSB", "Write result: $result")
            if (result <= 0) {
                throw IOException("USB bulk write failed (result=$result, mode=$ioMode)")
            }
            return result
        }
    }

    /**
     * Async USB write using UsbRequest. Used in async fallback mode.
     */
    private fun writeAsync(connection: UsbDeviceConnection, endpoint: UsbEndpoint, data: ByteArray, timeoutMs: Int): Int {
        val request = UsbRequest()
        try {
            if (!request.initialize(connection, endpoint)) {
                android.util.Log.e("ProxmarkUSB", "writeAsync: Failed to initialize UsbRequest")
                return -1
            }
            
            val byteBuffer = ByteBuffer.allocate(data.size)
            byteBuffer.put(data)
            byteBuffer.flip()
            
            if (!request.queue(byteBuffer)) {
                android.util.Log.e("ProxmarkUSB", "writeAsync: Failed to queue UsbRequest")
                request.close()
                return -1
            }
            
            val completed = connection.requestWait(timeoutMs.toLong())
            request.close()
            
            return if (completed != null) data.size else -1
        } catch (e: Exception) {
            android.util.Log.e("ProxmarkUSB", "writeAsync exception: ${e.message}")
            try { request.close() } catch (_: Exception) {}
            return -1
        }
    }

    @Throws(IOException::class)
    fun read(buffer: ByteArray, timeoutMs: Int = 250): Int {
        synchronized(ioLock) {
            val connection = currentConnection ?: throw IOException("USB connection is closed")
            val endpoint = inEndpoint ?: throw IOException("USB IN endpoint not available")
            
            val result = when (ioMode) {
                IoMode.LIBUSB -> {
                    val usb = libusb ?: throw IOException("libusb not initialized")
                    val r = usb.bulkRead(endpoint.address, buffer, buffer.size, timeoutMs)
                    // Log every result in libusb mode for debugging
                    if (r != 0) {
                        android.util.Log.d("ProxmarkUSB", "libusb read: result=$r ep=0x${endpoint.address.toString(16)}")
                    }
                    r
                }
                IoMode.ASYNC_REQUEST -> readAsync(connection, endpoint, buffer, timeoutMs)
                IoMode.SYNC -> {
                    val r = connection.bulkTransfer(endpoint, buffer, buffer.size, timeoutMs)
                    if (r < 0) {
                        consecutiveReadErrors++
                        if (consecutiveReadErrors >= ASYNC_FALLBACK_THRESHOLD) {
                            android.util.Log.w("ProxmarkUSB", "Sync read failing ($consecutiveReadErrors errors), switching to ASYNC_REQUEST mode")
                            ioMode = IoMode.ASYNC_REQUEST
                            return readAsync(connection, endpoint, buffer, timeoutMs)
                        }
                    } else if (r > 0) {
                        consecutiveReadErrors = 0
                    }
                    r
                }
            }
            
            // If async mode is also failing, escalate to libusb
            if (ioMode == IoMode.ASYNC_REQUEST && result < 0) {
                consecutiveReadErrors++
                if (consecutiveReadErrors >= LIBUSB_FALLBACK_THRESHOLD && UsbLibusb.isAvailable) {
                    android.util.Log.w("ProxmarkUSB", "Async read also failing, switching to LIBUSB mode")
                    if (initLibusbFallback(connection, endpoint)) {
                        ioMode = IoMode.LIBUSB
                        val usb = libusb!!
                        return usb.bulkRead(endpoint.address, buffer, buffer.size, timeoutMs)
                    }
                }
            }
            
            if (result > 0) {
                consecutiveReadErrors = 0
                android.util.Log.d("ProxmarkUSB", "Read $result bytes (mode=$ioMode)")
            }
            return result
        }
    }

    /**
     * Initialize libusb from the existing Android USB connection.
     * When used as exclusive mode, libusb handles all interface claiming and CDC init.
     */
    @Suppress("UNUSED_PARAMETER")
    private fun initLibusbFallback(connection: UsbDeviceConnection, endpoint: UsbEndpoint): Boolean {
        try {
            val fd = connection.fileDescriptor
            val inAddr = inEndpoint?.address ?: -1
            val outAddr = outEndpoint?.address ?: -1
            android.util.Log.i("ProxmarkUSB", "Initializing libusb with fd=$fd, IN=0x${inAddr.toString(16)}, OUT=0x${outAddr.toString(16)}")
            
            val usb = UsbLibusb()
            if (!usb.init(fd)) {
                android.util.Log.e("ProxmarkUSB", "libusb init failed")
                return false
            }
            
            val device = currentDevice
            val dataIfaceNum = currentInterface?.id ?: 1
            
            // For CDC devices: claim control interface (0), do CDC init, then claim data interface
            if (device != null && device.interfaceCount >= 2 && 
                device.getInterface(0).interfaceClass == UsbConstants.USB_CLASS_COMM) {
                
                // Claim control interface
                if (!usb.claimInterface(0)) {
                    android.util.Log.w("ProxmarkUSB", "libusb claim control interface 0 failed (non-fatal)")
                }
                
                // CDC init via libusb
                usb.initCdc(0)
            }
            
            // Claim data interface
            if (!usb.claimInterface(dataIfaceNum)) {
                android.util.Log.e("ProxmarkUSB", "libusb claim data interface $dataIfaceNum failed")
                usb.close()
                return false
            }
            
            // Log all endpoints for diagnostics
            val iface = currentInterface
            if (iface != null) {
                for (i in 0 until iface.endpointCount) {
                    val ep = iface.getEndpoint(i)
                    val dir = if (ep.direction == UsbConstants.USB_DIR_IN) "IN" else "OUT"
                    val type = when (ep.type) {
                        UsbConstants.USB_ENDPOINT_XFER_BULK -> "BULK"
                        UsbConstants.USB_ENDPOINT_XFER_INT -> "INT"
                        else -> "OTHER(${ep.type})"
                    }
                    android.util.Log.i("ProxmarkUSB", "  Endpoint: 0x${ep.address.toString(16).uppercase()} $dir $type maxPkt=${ep.maxPacketSize}")
                }
            }
            
            libusb = usb
            android.util.Log.i("ProxmarkUSB", "libusb active! (control=0, data=$dataIfaceNum)")

            return true
        } catch (e: Exception) {
            android.util.Log.e("ProxmarkUSB", "libusb init error: ${e.message}")
            return false
        }
    }

    /**
     * Async USB read using UsbRequest. Fallback for devices where synchronous
     * bulkTransfer returns -1 (common on PM3 Easy clones).
     */
    private fun readAsync(connection: UsbDeviceConnection, endpoint: UsbEndpoint, buffer: ByteArray, timeoutMs: Int): Int {
        val request = UsbRequest()
        try {
            if (!request.initialize(connection, endpoint)) {
                android.util.Log.e("ProxmarkUSB", "readAsync: Failed to initialize UsbRequest for ep=0x${endpoint.address.toString(16)}")
                return -1
            }
            
            val byteBuffer = ByteBuffer.allocate(buffer.size)
            if (!request.queue(byteBuffer)) {
                android.util.Log.e("ProxmarkUSB", "readAsync: Failed to queue UsbRequest")
                request.close()
                return -1
            }
            
            val completed = connection.requestWait(timeoutMs.toLong())
            if (completed == null) {
                // Timeout - no data
                request.cancel()
                request.close()
                return 0
            }
            
            byteBuffer.flip()
            val bytesRead = byteBuffer.remaining()
            if (bytesRead > 0) {
                byteBuffer.get(buffer, 0, bytesRead)
                android.util.Log.d("ProxmarkUSB", "readAsync: $bytesRead bytes from endpoint 0x${Integer.toHexString(endpoint.address.toInt())}")
            }
            request.close()
            return bytesRead
        } catch (e: Exception) {
            android.util.Log.e("ProxmarkUSB", "readAsync exception: ${e.javaClass.simpleName}: ${e.message}", e)
            try { request.close() } catch (_: Exception) {}
            return -1
        }
    }

    fun getConnectedDevicesInfo(): String {
        val devices = usbManager.deviceList
        
        if (devices.isEmpty()) {
            return "No USB devices detected.\n\nPossible causes:\n" +
                "• Proxmark3 not connected\n" +
                "• USB OTG not enabled\n" +
                "• Charge-only cable (need data cable)"
        }
        
        return devices.values.joinToString("\n\n") { device ->
            val supported = isSupported(device)
            val hasPermission = usbManager.hasPermission(device)
            val endpointInfo = getEndpointInfo(device)
            
            "Device: ${device.deviceName}\n" +
            "Vendor ID: 0x${device.vendorId.toString(16).uppercase()} (${device.vendorId})\n" +
            "Product ID: 0x${device.productId.toString(16).uppercase()} (${device.productId})\n" +
            "Interface Count: ${device.interfaceCount}\n" +
            "Supported: ${if (supported) "✓ YES" else "✗ NO (unsupported device)"}\n" +
            "Has Permission: ${if (hasPermission) "✓ YES" else "✗ NO"}\n" +
            endpointInfo
        } + "\n\nSupported IDs:\n" + SUPPORTED_IDS.joinToString("\n") {
            "  0x${it.first.toString(16).uppercase()} / 0x${it.second.toString(16).uppercase()}"
        }
    }

    private fun getEndpointInfo(device: UsbDevice): String {
        val info = StringBuilder("\n--- Endpoint Details ---\n")
        
        for (ifaceIdx in 0 until device.interfaceCount) {
            val iface = device.getInterface(ifaceIdx)
            info.append("Interface $ifaceIdx:\n")
            info.append("  Class: ${iface.interfaceClass}\n")
            info.append("  Subclass: ${iface.interfaceSubclass}\n")
            info.append("  Protocol: ${iface.interfaceProtocol}\n")
            info.append("  Endpoints: ${iface.endpointCount}\n")
            
            for (epIdx in 0 until iface.endpointCount) {
                val ep = iface.getEndpoint(epIdx)
                val type = when (ep.type) {
                    UsbConstants.USB_ENDPOINT_XFER_BULK -> "BULK"
                    UsbConstants.USB_ENDPOINT_XFER_INT -> "INTERRUPT"
                    UsbConstants.USB_ENDPOINT_XFER_CONTROL -> "CONTROL"
                    UsbConstants.USB_ENDPOINT_XFER_ISOC -> "ISOCHRONOUS"
                    else -> "UNKNOWN(${ep.type})"
                }
                val dir = if (ep.direction == UsbConstants.USB_DIR_IN) "IN" else "OUT"
                info.append("    EP $epIdx: $type $dir (addr=0x${ep.address.toString(16).uppercase()})\n")
            }
        }
        
        val endpoints = selectBulkEndpoints(device)
        info.append("\nBulk Endpoints Found: ${if (endpoints != null) "✓ YES" else "✗ NO"}\n")
        if (endpoints != null) {
            info.append("  IN:  0x${endpoints.inEndpoint.address.toString(16).uppercase()}\n")
            info.append("  OUT: 0x${endpoints.outEndpoint.address.toString(16).uppercase()}\n")
        }
        
        return info.toString()
    }

    private fun findSupportedDevice(): UsbDevice? {
        return usbManager.deviceList.values.firstOrNull { isSupported(it) }
    }

    private fun isSupported(device: UsbDevice): Boolean {
        return SUPPORTED_IDS.contains(device.vendorId to device.productId)
    }

    private fun selectBulkEndpoints(device: UsbDevice): EndpointSet? {
        for (index in 0 until device.interfaceCount) {
            val usbInterface = device.getInterface(index)
            var inEp: UsbEndpoint? = null
            var outEp: UsbEndpoint? = null

            for (epIndex in 0 until usbInterface.endpointCount) {
                val endpoint = usbInterface.getEndpoint(epIndex)
                if (endpoint.type == UsbConstants.USB_ENDPOINT_XFER_BULK) {
                    if (endpoint.direction == UsbConstants.USB_DIR_IN && inEp == null) {
                        inEp = endpoint
                    }
                    if (endpoint.direction == UsbConstants.USB_DIR_OUT && outEp == null) {
                        outEp = endpoint
                    }
                }
            }

            if (inEp != null && outEp != null) {
                return EndpointSet(usbInterface, inEp, outEp)
            }
        }
        return null
    }

    private fun openConnection(device: UsbDevice) {
        synchronized(ioLock) {
            val endpoints = selectBulkEndpoints(device)
            if (endpoints == null) {
                _connectionState.value = UsbConnectionState.Error("Bulk endpoints not found")
                _errors.tryEmit("No usable USB bulk endpoints were found")
                return
            }

            val connection = usbManager.openDevice(device)
            if (connection == null) {
                _connectionState.value = UsbConnectionState.Error("Unable to open USB device")
                _errors.tryEmit("Failed to open Proxmark3 USB device")
                return
            }

            val isCdcDevice = endpoints.usbInterface.interfaceClass == 10

            // For CDC devices: test both Android API and libusb, use whichever works
            if (isCdcDevice) {
                currentConnection?.close()
                currentDevice = device
                currentConnection = connection
                currentInterface = endpoints.usbInterface
                inEndpoint = endpoints.inEndpoint
                outEndpoint = endpoints.outEndpoint

                // Use Android API: claim interfaces + CDC init
                android.util.Log.i("ProxmarkUSB", "CDC device: testing all I/O methods")
                claimAndInitCdc(device, connection, endpoints)

                // Test 1: Android SYNC bulk I/O
                val pingNG = com.sushiomsky.proxmark.protocol.Pm3Protocol.buildPing()
                val wr1 = connection.bulkTransfer(endpoints.outEndpoint, pingNG, pingNG.size, 2000)
                android.util.Log.i("ProxmarkUSB", "Test1 SYNC write NG PING: $wr1")
                val testBuf = ByteArray(512)
                val rd1 = connection.bulkTransfer(endpoints.inEndpoint, testBuf, testBuf.size, 2000)
                android.util.Log.i("ProxmarkUSB", "Test1 SYNC read: $rd1")

                // Test 2: Try OLD protocol PING (544 bytes, bootrom compatible)
                val pingOLD = com.sushiomsky.proxmark.protocol.Pm3Protocol.encodeCommand(
                    com.sushiomsky.proxmark.protocol.Pm3Protocol.Pm3Command(
                        com.sushiomsky.proxmark.protocol.Pm3Protocol.CMD_PING,
                        ByteArray(0), ng = false))
                val wr2 = connection.bulkTransfer(endpoints.outEndpoint, pingOLD, pingOLD.size, 2000)
                android.util.Log.i("ProxmarkUSB", "Test2 SYNC write OLD PING (${pingOLD.size}B): $wr2")
                val rd2 = connection.bulkTransfer(endpoints.inEndpoint, testBuf, testBuf.size, 2000)
                android.util.Log.i("ProxmarkUSB", "Test2 SYNC read: $rd2")

                // Test 3: Try claiming ONLY data interface (release control first)
                // Some devices fail reads when control iface is claimed
                android.util.Log.i("ProxmarkUSB", "Test3: Release control interface, retry")
                try {
                    connection.releaseInterface(device.getInterface(0))
                } catch (_: Exception) {}
                val wr3 = connection.bulkTransfer(endpoints.outEndpoint, pingNG, pingNG.size, 2000)
                android.util.Log.i("ProxmarkUSB", "Test3 write: $wr3")
                val rd3 = connection.bulkTransfer(endpoints.inEndpoint, testBuf, testBuf.size, 2000)
                android.util.Log.i("ProxmarkUSB", "Test3 read: $rd3")

                // Re-claim control for CDC
                connection.claimInterface(device.getInterface(0), true)

                // Final: use libusb for the read loop (returns clean 0 on timeout, not -1)
                if (UsbLibusb.isAvailable) {
                    // Release Android claims first to avoid conflicts
                    connection.releaseInterface(endpoints.usbInterface)
                    connection.releaseInterface(device.getInterface(0))
                    if (initLibusbFallback(connection, endpoints.inEndpoint)) {
                        ioMode = IoMode.LIBUSB
                        android.util.Log.i("ProxmarkUSB", "Using libusb for read loop")
                    } else {
                        // Re-claim via Android
                        claimAndInitCdc(device, connection, endpoints)
                        ioMode = IoMode.ASYNC_REQUEST
                    }
                } else {
                    ioMode = IoMode.ASYNC_REQUEST
                }
            } else if (isCdcDevice) {
                claimAndInitCdc(device, connection, endpoints)
                currentConnection?.close()
                currentDevice = device
                currentConnection = connection
                currentInterface = endpoints.usbInterface
                inEndpoint = endpoints.inEndpoint
                outEndpoint = endpoints.outEndpoint
                ioMode = IoMode.ASYNC_REQUEST
                android.util.Log.i("ProxmarkUSB", "CDC device → async UsbRequest (libusb unavailable)")
            } else {
                // Non-CDC device, standard Android claiming
                if (!connection.claimInterface(endpoints.usbInterface, true)) {
                    connection.close()
                    _connectionState.value = UsbConnectionState.Error("Cannot claim USB interface")
                    _errors.tryEmit("Failed to claim USB interface")
                    return
                }
                currentConnection?.close()
                currentDevice = device
                currentConnection = connection
                currentInterface = endpoints.usbInterface
                inEndpoint = endpoints.inEndpoint
                outEndpoint = endpoints.outEndpoint
                ioMode = IoMode.SYNC
            }

            android.util.Log.d("ProxmarkUSB", "openConnection() - Connected (io=$ioMode)")
            _connectionState.value = UsbConnectionState.Connected
        }
    }

    /**
     * Claim interfaces and run CDC initialization via Android Java API.
     * Used as fallback when libusb is unavailable.
     */
    private fun claimAndInitCdc(device: UsbDevice, connection: UsbDeviceConnection, endpoints: EndpointSet) {
        if (device.interfaceCount >= 2) {
            val controlInterface = device.getInterface(0)
            connection.claimInterface(controlInterface, true)
            android.util.Log.d("ProxmarkUSB", "CDC control interface claimed (Android)")
            initializeCdcDevice(connection)
            connection.claimInterface(endpoints.usbInterface, true)
            android.util.Log.d("ProxmarkUSB", "CDC data interface claimed (Android)")
        } else {
            connection.claimInterface(endpoints.usbInterface, true)
        }
    }

    private fun initializeCdcDevice(connection: UsbDeviceConnection) {
        // CDC ACM control requests - set line coding for serial communication
        // This is needed for PM3 Easy which uses CDC mode
        
        android.util.Log.d("ProxmarkUSB", "Starting CDC initialization...")
        
        // SET_LINE_CODING (0x20) - configure baud rate, stop bits, parity, data bits
        val lineCoding = ByteArray(7).apply {
            // 115200 baud rate (little-endian): 0x0001C200 = 115200
            this[0] = 0x00
            this[1] = 0xC2.toByte()
            this[2] = 0x01
            this[3] = 0x00
            // 1 stop bit
            this[4] = 0x00
            // No parity
            this[5] = 0x00
            // 8 data bits
            this[6] = 0x08
        }
        
        val result1 = connection.controlTransfer(
            0x21, // bmRequestType: Host to Device, Class, Interface
            0x20, // bRequest: SET_LINE_CODING
            0,    // wValue
            0,    // wIndex: interface 0 (control interface)
            lineCoding,
            lineCoding.size,
            1000
        )
        
        // SET_CONTROL_LINE_STATE (0x22) - deactivate DTR/RTS first (reset)
        val result2 = connection.controlTransfer(
            0x21, // bmRequestType
            0x22, // bRequest: SET_CONTROL_LINE_STATE
            0x00, // wValue: DTR=0, RTS=0
            0,    // wIndex: interface 0
            null,
            0,
            1000
        )
        
        // Small delay for device to process reset
        Thread.sleep(50)
        
        // SET_CONTROL_LINE_STATE (0x22) - activate DTR and RTS
        val result3 = connection.controlTransfer(
            0x21, // bmRequestType
            0x22, // bRequest: SET_CONTROL_LINE_STATE
            0x03, // wValue: DTR=1, RTS=1
            0,    // wIndex: interface 0
            null,
            0,
            1000
        )
        
        // Give device time to stabilize after CDC init (critical for some devices!)
        Thread.sleep(200)
        
        // Log results for debugging
        android.util.Log.d("ProxmarkUSB", "CDC initialization complete: SET_LINE_CODING=$result1, DTR_OFF=$result2, DTR_ON=$result3")
    }

    private fun disconnectInternal(reason: String?) {
        synchronized(ioLock) {
            libusb?.close()
            libusb = null
            
            currentConnection?.let { connection ->
                currentInterface?.let { usbInterface ->
                    connection.releaseInterface(usbInterface)
                }
                connection.close()
            }

            currentDevice = null
            currentConnection = null
            currentInterface = null
            inEndpoint = null
            outEndpoint = null
            ioMode = IoMode.SYNC
            consecutiveReadErrors = 0
            _connectionState.value = UsbConnectionState.Disconnected
        }

        if (reason != null) {
            _errors.tryEmit(reason)
        }
    }

    @Suppress("DEPRECATION")
    private fun Intent.parcelableDevice(): UsbDevice? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            getParcelableExtra(UsbManager.EXTRA_DEVICE, UsbDevice::class.java)
        } else {
            getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }
    }
}
