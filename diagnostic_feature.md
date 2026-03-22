# USB Connection Diagnostic Feature

## Quick Addition to Help Debug:

Add to ProxmarkRepository.kt:
```kotlin
fun getConnectedUsbDevices(): String {
    val usbManager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    val devices = usbManager.deviceList
    
    if (devices.isEmpty()) {
        return "No USB devices found"
    }
    
    return devices.values.joinToString("\n") { device ->
        "Device: ${device.deviceName}\n" +
        "Vendor ID: 0x${device.vendorId.toString(16)} (${device.vendorId})\n" +
        "Product ID: 0x${device.productId.toString(16)} (${device.productId})\n" +
        "Supported: ${isSupported(device)}"
    }
}
```

Add button to DashboardScreen to show this info in a dialog.

## Expected Output for PM3:
```
Device: /dev/bus/usb/001/002
Vendor ID: 0x2d2d (11565)
Product ID: 0x504d (20557)
Supported: true
```

## If Nothing Shows:
1. PM3 not plugged in
2. USB OTG not working
3. Cable is charge-only

## If Shows But Supported=false:
1. Vendor/Product ID mismatch
2. Need to add device to SUPPORTED_IDS
