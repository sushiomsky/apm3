package com.example.proxmark.usb

sealed class UsbConnectionState {
    data object Disconnected : UsbConnectionState()
    data object Connecting : UsbConnectionState()
    data object Connected : UsbConnectionState()
    data object PermissionDenied : UsbConnectionState()
    data class Error(val message: String) : UsbConnectionState()
}
