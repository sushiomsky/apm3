<div align="center">

# 🛡️ PM3 Companion

### The Professional Android App for Proxmark3

**Turn your phone into a powerful RFID/NFC security toolkit.**

[![Android CI](https://github.com/sushiomsky/apm3/actions/workflows/android.yml/badge.svg)](https://github.com/sushiomsky/apm3/actions)
[![API 26+](https://img.shields.io/badge/API-26%2B-brightgreen.svg)](https://android-arsenal.com/api?level=26)
[![License](https://img.shields.io/badge/license-Proprietary-red.svg)]()

[Download APK](#download) · [Features](#features) · [Screenshots](#screenshots) · [Getting Started](#getting-started)

</div>

---

## Why PM3 Companion?

Every existing Proxmark3 app forces you to memorize arcane terminal commands. **PM3 Companion doesn't.** Every operation is a tappable card with safety ratings, live feedback, and guided workflows.

| Problem | PM3 Companion Solution |
|---------|----------------------|
| 🤯 Memorize `hf mf autopwn -s 0 -a -k FFFFFFFFFFFF` | ✅ Tap "Key Recovery" → watch the sector map fill in |
| 💀 Accidentally brick a tag with wrong write command | ✅ Destructive actions require double confirmation |
| 📖 No idea which attack to use on unknown tags | ✅ AI-powered attack recommendation based on chip type |
| 🔌 Carry a laptop everywhere for PM3 | ✅ Full PM3 control from your phone via USB OTG |

---

## Features

### 🎯 94 One-Tap Actions
Every Proxmark3 command reimagined as a tappable card with safety levels:
- **✅ Safe** — one tap to execute (scans, reads, device info)
- **⚠️ Advanced** — confirmation required (key recovery, simulation)
- **🔴 Destructive** — double confirm with warning (writes, flashing)

### 🏋️ Gym Locker Workflow
The world's first **5-phase guided gym locker analysis** on mobile:
1. **Identify** — auto-detect Fudan FM11RF08S, extract backdoor keys
2. **Recover** — smart attack pipeline (Dictionary → Fudan → Nested → Darkside)
3. **Dump** — full memory extraction with progress tracking
4. **Analyze** — identify Gantner GL7p access patterns, decode sector data
5. **Clone** — step-by-step emulation with legal compliance warnings

### 🔐 Live Key Recovery
Watch keys appear in real-time on an animated sector map:
- Color-coded sectors (🟢 found · 🟡 attacking · 🔴 locked · ⚫ unknown)
- Sequential attack pipeline with automatic escalation
- FM11RF08S backdoor key extraction (A396EFA4E24F)
- Export recovered keys as text

### 📡 Multi-Reader Support
| Reader | Capabilities |
|--------|-------------|
| **Proxmark3** (USB OTG) | Full: read, write, emulate, attack, clone |
| **Built-in NFC** | Read tags, write NDEF, MIFARE Classic auth |
| **ACR USB** (CCID) | Read smart cards, extract UIDs |

### 🧩 Plugin System
- **12 built-in plugins** — analyzers, cloners, decoders
- **Workflow Builder** — chain PM3 commands into reusable sequences
- **Script Editor** — PM3/Lua scripting with syntax highlighting

### 🖥️ Advanced Mode
For power users who want raw access:
- Direct PM3 command input with autocomplete
- Protocol inspector (raw hex frames)
- Command execution metrics & timing
- Full terminal with ANSI color support

### 📤 Data Export
- Tags → JSON / CSV
- MIFARE dumps → hex text
- Recovered keys → formatted table
- Terminal logs → timestamped text
- Android Share intent for instant forwarding

---

## Screenshots

<div align="center">

| Home Dashboard | Feature Catalog | Live Terminal |
|:-:|:-:|:-:|
| Connection status, quick actions, last scan | 94 features with search, safety labels, favorites | Real-time PM3 output with color coding |

| Gym Workflow | Sector Map | Attack Pipeline |
|:-:|:-:|:-:|
| 5-phase guided analysis wizard | Live key recovery visualization | Smart attack recommendation engine |

</div>

---

## Getting Started

### What You Need
- Android phone with **USB OTG** support (Android 8.0+)
- **Proxmark3** device (Easy, RDV2, RDV4, or clone)
- USB OTG adapter (USB-C or Micro-USB)

### Quick Start
1. **Install** the APK on your phone
2. **Connect** Proxmark3 via USB OTG adapter
3. **Grant** USB permission when prompted
4. **Tap** "Scan HF Tag" — hold a card to the PM3 antenna
5. **Done** — tag info appears instantly

### Supported Hardware
| Device | VID:PID | Status |
|--------|---------|--------|
| PM3 Easy (clone) | 9AC4:4B8F | ✅ Tested |
| PM3 RDV2 | 9AC4:4B8F | ✅ Supported |
| PM3 RDV4 | 9AC4:4B8F | ✅ Supported |
| ACR122U | 072F:2200 | ✅ Supported |
| ACR1252U | 072F:0901 | ✅ Supported |

---

## Build from Source

```bash
# Clone
git clone https://github.com/sushiomsky/apm3.git
cd apm3

# Build debug APK
./gradlew app:assembleDebug

# Run tests (125 unit tests)
./gradlew app:testDebugUnitTest

# Install on device
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Requirements
- JDK 17
- Android SDK 34
- NDK 26.1.10909125 (for libusb JNI)
- CMake 3.22.1

---

## Architecture

```
UI (Compose) → ViewModel (StateFlow) → Repository → Protocol Layer → USB
```

- **Single-module** Compose app with manual DI
- **PM3 NG binary protocol** with CRC validation
- **libusb JNI** fallback for reliable USB bulk transfers
- **125 unit tests** covering parsers, state machines, and catalog invariants

<details>
<summary>📁 Project Structure</summary>

```
app/src/main/java/com/sushiomsky/proxmark/
├── features/        # 94-feature catalog with safety levels
├── mifare/          # Key state, dumps, workflows, comparison
├── persistence/     # DataStore (tags, settings)
├── plugins/         # Plugin models and built-in catalog
├── protocol/        # PM3 NG protocol, output parser
├── reader/          # Multi-reader abstraction
├── repository/      # Business logic, state management
├── export/          # JSON/CSV/hex data export
├── ui/              # Compose screens + 15 component dialogs
├── usb/             # USB host + libusb JNI bridge
└── viewmodel/       # UI state (80+ fields) and actions
```
</details>

---

## Download

> 🚧 **Coming soon to Google Play**
>
> In the meantime, grab the latest APK from [Releases](https://github.com/sushiomsky/apm3/releases).

---

## Contributing

This is a proprietary project. For feature requests or bug reports, please open an issue.

---

<div align="center">
  <sub>Built with ❤️ for the RFID security research community</sub>
</div>
