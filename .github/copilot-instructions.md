# Copilot instructions for this repository

## Build, test, and lint

Run from repo root:

- Build debug APK: `./gradlew app:assembleDebug`
- Build/check module: `./gradlew app:build`
- Lint: `./gradlew app:lint` or `./gradlew app:lintDebug`
- Unit tests: `./gradlew app:test` or `./gradlew app:testDebugUnitTest`
- Single unit test class: `./gradlew app:testDebugUnitTest --tests "com.sushiomsky.proxmark.YourTestClass"`
- Single unit test method: `./gradlew app:testDebugUnitTest --tests "com.sushiomsky.proxmark.YourTestClass.yourTestMethod"`
- Instrumented tests: `./gradlew app:connectedDebugAndroidTest`
- Single instrumented test class: `./gradlew app:connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.sushiomsky.proxmark.YourInstrumentedTest`

Notes:

- `app/src/test` contains JVM unit tests for `ProxmarkOutputParser`, `MifareClassicDump`, `DumpComparison`, `TagStoreSerialization`, `ProxmarkFeatureCatalog`.
- Java/Kotlin target: 17. Compose compiler extension: `1.5.14`.
- NDK 26.1.10909125, CMake 3.22.1 (for libusb JNI layer).

## High-level architecture

Single-module Android app (`:app`) with Compose and manual wiring (no Navigation, 4-tab layout):

- `ProxmarkApplication` owns `AppContainer`.
- `AppContainer` wires `ProxmarkUsbManager` -> `ProxmarkClient` -> `ProxmarkRepository` on an app-level IO scope.
- `MainActivity` creates `MainViewModel` via `MainViewModelFactory`.
- `MainViewModel` collects repository flows into `MainUiState` and exposes UI actions.
- `ui/ProxmarkApp.kt` hosts 4-tab navigation: Home / Tools / Tags / Terminal.

Device data path:

1. UI -> `MainViewModel`
2. `MainViewModel` -> `ProxmarkRepository`
3. `ProxmarkRepository` -> `ProxmarkClient`
4. `ProxmarkClient` -> `ProxmarkUsbManager` (USB bulk I/O with libusb JNI fallback)
5. Incoming bytes decoded via PM3 NG binary protocol (`Pm3Protocol.decodeResponse`)
6. Decoded responses formatted to text and emitted as `TerminalLine`
7. Repository parses tags + MIFARE key recovery lines from terminal output
8. `MainUiState` (50+ fields) drives Compose UI

USB specifics:

- Manifest enables USB host + attach intent with `@xml/device_filter`.
- Keep `res/xml/device_filter.xml` and `ProxmarkUsbManager.SUPPORTED_IDS` in sync.
- 3-tier USB I/O: SYNC -> ASYNC_REQUEST -> LIBUSB JNI fallback.
- PM3 NG protocol: magic-based framing (PM3a=0x61334d50 for commands, PM3b=0x62334d50 for responses).

## Feature catalog system

The app uses a commandless UX — users never type PM3 commands. All actions are exposed as tappable cards:

- `features/ProxmarkFeature.kt`: Data model with id, title, description, category, command, safetyLevel, prerequisites, warningMessage, parameters, targetReader.
- `features/ProxmarkFeatureCategory.kt`: 17-category enum.
- `features/ProxmarkFeatureCatalog.kt`: Central catalog of 94 PM3 actions. Add new features here.
- `features/FeatureSafetyLevel.kt`: SAFE (one-tap), ADVANCED (confirmation required), DESTRUCTIVE (confirmation required, red warning).
- `features/FeatureExecutionState.kt`: Sealed class tracking Idle/Running/Success/Failed.
- `features/FeatureRunRecord.kt`: History record with outcome enum.
- `features/FeatureParameter.kt`: Typed parameters (TEXT, NUMBER, BOOLEAN, CHOICE) interpolated into command strings.

Safety conventions:

- SAFE actions run immediately on tap.
- ADVANCED/DESTRUCTIVE actions show a confirmation dialog with description, prerequisites, and "why risky?" warning.
- Session-scoped "don't ask again" toggle bypasses confirmations; re-enable via dashboard banner.
- Every ADVANCED/DESTRUCTIVE feature must have a non-null `warningMessage`.

## MIFARE analysis system

- `mifare/MifareKeyState.kt`: Per-sector key tracking (UNKNOWN/IN_PROGRESS/FOUND/FAILED), attack method, progress.
- `mifare/MifareClassicDump.kt`: 1K/4K dump parser, hex viewer, sector/block extraction.
- `mifare/DumpComparison.kt`: Side-by-side dump diff.
- `mifare/MifareWorkflow.kt`: 5 guided workflows (gym locker, hotel key, transit, parking, door access).
- `ui/components/SectorMapVisualization.kt`: Color-coded sector grid with animated attack indicators.
- `ui/components/KeyRecoveryDialog.kt`: Key recovery with sector map, dictionary attack, terminal output.
- `ui/components/GymLockerWorkflow.kt`: 5-phase interactive gym locker analysis (identify → recover → dump → analyze → clone).
- `ui/components/WorkflowViewer.kt`: Generic workflow step-by-step viewer.
- `protocol/ProxmarkOutputParser.kt`: Parses UID, tag type, ATQA, SAK + MIFARE key recovery lines + Fudan FM11RF08S detection.
- Repository collects terminal lines and auto-updates `mifareKeyState` as keys are discovered.

## Key conventions in this codebase

- Keep business logic out of composables; state lives in `MainUiState`.
- Prefer `StateFlow`/`SharedFlow` between layers.
- Handle all `UsbConnectionState` branches explicitly.
- Emit terminal lines with correct type (`SENT`, `RECEIVED`, `ERROR`, `SYSTEM`) at source.
- Preserve bounded collections unless behavior intentionally changes:
  - parser window: last 50 received lines
  - terminal lines: last 500 in UI
  - saved tags: last 100 unique `(uid, type)`
  - run history: last 100 records
- `ProxmarkClient.sendCommand`: clear response queue before send, append newline if missing.
- Parser/repository identity behavior is intentional:
  - `ProxmarkOutputParser` outputs uppercase UID without separators
  - repository dedupes by `(uid, type)` and keeps newest first
- Read-loop error policy: emit error, delay, and keep waiting for reconnect.
- Lifecycle: `repository.start()` in `MainViewModel` init; `repository.close()` in `onCleared()`.
- `runFeatureByCommand()` for workflow automation (bypasses feature catalog).

## Feature placement map

- USB transport/permissions/endpoints: `usb/ProxmarkUsbManager.kt`
- Libusb JNI bridge: `cpp/usb_native.c`
- PM3 binary protocol (NG): `protocol/Pm3Protocol.kt`
- Proxmark command wrappers + terminal emissions: `protocol/ProxmarkClient.kt`
- Output parsing (tags, keys, chip detection): `protocol/ProxmarkOutputParser.kt`
- Feature definitions + catalog: `features/` package
- MIFARE state + workflows + dumps: `mifare/` package
- State transitions, key recovery parsing, timeout/error policy: `repository/ProxmarkRepository.kt`
- Persistence (tags, settings, groups): `persistence/` package
- Multi-reader support: `reader/` package (Proxmark3Reader, BuiltInNfcReader, AcrUsbReader)
- UI actions + UI contract: `viewmodel/MainViewModel.kt`, `viewmodel/MainUiState.kt`
- 4-tab navigation: `ui/ProxmarkApp.kt` (Home / Tools / Tags / Terminal)
- Home screen: `ui/HomeScreen.kt`
- Tools & features: `ui/ToolsScreen.kt`
- Tags list: `ui/TagsScreen.kt`
- Terminal: `ui/TerminalScreen.kt`
- Composable components: `ui/components/` (dialogs, cards, visualizations, workflows)
- New USB device IDs must be added in both `res/xml/device_filter.xml` and `SUPPORTED_IDS`
- New PM3 actions must be added to `ProxmarkFeatureCatalog.kt` with appropriate safety level and warning
- New PM3 commands must be mapped in `Pm3Protocol.textCommandToPackets()`
