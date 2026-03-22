package com.sushiomsky.proxmark.reader

enum class ReaderType(val label: String) {
    PROXMARK3("Proxmark3 (USB)"),
    BUILTIN_NFC("Built-in NFC"),
    ACR_USB("ACR Reader (USB)")
}
