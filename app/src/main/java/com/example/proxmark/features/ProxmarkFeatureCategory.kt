package com.example.proxmark.features

enum class ProxmarkFeatureCategory(val label: String) {
    QUICK_START("Quick start"),
    DEVICE("Device"),
    FIRMWARE("Firmware & Bootloader"),
    HIGH_FREQUENCY("High frequency (13.56 MHz)"),
    LOW_FREQUENCY("Low frequency (125 kHz)"),
    MIFARE("Mifare Classic"),
    MIFARE_ULTRA("Mifare Ultralight / NTAG"),
    DESFIRE("DESFire"),
    ICLASS("iCLASS / Picopass"),
    EM_HID("EM4x / HID / Indala"),
    T55XX("T55xx"),
    EMULATION("Emulation & simulation"),
    ATTACKS("Attacks & key recovery"),
    SNIFFING("Sniffing & tracing"),
    UTILITIES("Utilities"),
    SCRIPTS("Scripts & automation"),
    NFC_OPERATIONS("Built-in NFC")
}
