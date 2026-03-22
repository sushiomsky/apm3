# PM3 Companion

<p align="center">
  <strong>Professional Android companion app for Proxmark3 devices</strong><br>
  Commandless UX • USB OTG • MIFARE Analysis • Multi-Reader Support
</p>

---

## Overview

PM3 Companion transforms complex RFID/NFC security operations into a tappable, guided experience. No command-line knowledge required — every Proxmark3 action is exposed as a card with safety levels, prerequisites, and one-tap execution.

**Supports:** Proxmark3 Easy, RDV2, RDV4 via USB OTG  
**Also works with:** Built-in Android NFC and ACR USB readers  
**Target:** Android 8.0+ (API 26), tested on Samsung Galaxy S9+ (Android 10)

## Features

### 🎯 Commandless UX
- **94 PM3 features** organized into 17 categories
- Safety levels: ✅ Safe (one-tap) · ⚠️ Advanced (confirmation) · 🔴 Destructive (double confirm)
- Typed parameters with validation (text, number, boolean, choice)
- Feature search and favorites

### 📡 USB Communication
- PM3 NG binary protocol with automatic packet framing
- 3-tier USB I/O: Sync → Async → libusb JNI fallback
- Real-time terminal with color-coded output
- Protocol inspector for raw hex frames

### 🔐 MIFARE Analysis
- **Live sector map** — color-coded grid updates as keys are discovered
- **Attack pipeline** — Dictionary → Fudan → Nested → Hardnested → Darkside
- **FM11RF08S detection** — auto-identifies Fudan chips with backdoor key extraction
- **Dump viewer** — hex view with sector/block navigation
- **Dump comparison** — side-by-side diff of two dumps

### 🏋️ Gym Locker Workflow
5-phase interactive wizard for access control analysis:
1. **Identify** — tag type, UID, ATQA/SAK, vulnerability assessment
2. **Recover Keys** — smart attack recommendation based on chip type
3. **Read Dump** — full memory extraction with one-tap commands
4. **Analyze** — access pattern detection, Gantner GL7p identification
5. **Clone** — emulate/write workflow with legal warnings

### 🧩 Plugin System
- Plugin browser with 12 built-in analysis tools
- Custom workflow builder with step-by-step command sequences
- Script editor with PM3/Lua support and syntax highlighting

### 📱 Multi-Reader Support
| Reader | Tag Read | Tag Write | Commands | Key Recovery |
|--------|----------|-----------|----------|--------------|
| Proxmark3 (USB) | ✅ | ✅ | ✅ Full | ✅ |
| Built-in NFC | ✅ | ✅ NDEF | ❌ | ❌ |
| ACR USB (CCID) | ✅ | ❌ | ❌ | ❌ |

### 📤 Data Export
- Export tags as JSON/CSV
- Export MIFARE dumps as hex
- Export recovered keys
- Share terminal logs
- Android share intent integration

## Screenshots

| Home | Tools | Terminal | Gym Workflow |
|------|-------|----------|--------------|
| Dashboard with connection status, quick actions, last scan | Feature catalog with search, MIFARE tools, plugins | Live PM3 output with command input | 5-phase guided analysis |

## Build

### Prerequisites
- Android Studio Hedgehog or later
- JDK 17
- Android SDK 34
- NDK 26.1.10909125
- CMake 3.22.1

### Build Debug APK
```bash
./gradlew app:assembleDebug
```

### Run Tests
```bash
./gradlew app:testDebugUnitTest
```

### Lint
```bash
./gradlew app:lintDebug
```

## Architecture

```
┌─────────────┐     ┌──────────────┐     ┌────────────────┐
│  Compose UI │────▶│ MainViewModel│────▶│ProxmarkRepository│
│  (4 tabs)   │     │  (StateFlow) │     │  (business)     │
└─────────────┘     └──────────────┘     └────────┬────────┘
                                                   │
                    ┌──────────────────────────────┤
                    │                    │          │
              ┌─────▼─────┐    ┌────────▼──┐  ┌───▼────────┐
              │ProxmarkClient│  │NFC Reader │  │ACR Reader  │
              │(PM3 protocol)│  │(Android)  │  │(CCID/USB)  │
              └──────┬──────┘  └───────────┘  └────────────┘
                     │
              ┌──────▼──────┐
              │UsbManager   │
              │(libusb JNI) │
              └─────────────┘
```

**Key patterns:**
- Single-module app with manual DI via `AppContainer`
- `MainUiState` data class drives all Compose UI
- `StateFlow`/`SharedFlow` between layers
- Feature execution serialized via mutex
- Terminal lines buffered (last 500)

## Project Structure

```
app/src/main/java/com/sushiomsky/proxmark/
├── features/          # Feature catalog, parameters, safety levels
├── mifare/            # Key state, dumps, workflows, comparison
├── persistence/       # DataStore for tags and settings
├── plugins/           # Plugin models and catalog
├── protocol/          # PM3 NG protocol, client, output parser
├── reader/            # Multi-reader abstraction (PM3, NFC, ACR)
├── repository/        # Business logic, state management
├── ui/                # Compose screens and components
│   ├── components/    # Dialogs, cards, visualizations
│   └── theme/         # Material 3 dark theme
├── usb/               # USB host, connection state, libusb bridge
└── viewmodel/         # UI state and actions
```

## License

Private repository. All rights reserved.

## Acknowledgments

- [Proxmark3 Iceman Fork](https://github.com/RfidResearchGroup/proxmark3) — firmware and protocol reference
- [libusb](https://libusb.info/) — USB host library for Android JNI
