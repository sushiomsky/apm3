# Settings Screen Design Document
**Proxmark3 Control App - Settings Interface**

Version: 1.0  
Last Updated: 2024

---

## 1. Settings Screen Layout

### 1.1 Overall Structure

```
┌─────────────────────────────────────┐
│ ← Settings            [Search] [?] │  ← Top Bar
├─────────────────────────────────────┤
│ [Search settings...]                │  ← Search Field (sticky)
├─────────────────────────────────────┤
│                                     │
│ ▼ CONNECTION                        │  ← Category Header (collapsible)
│   • Auto-reconnect                  │
│   • Connection timeout              │
│   • USB power settings              │
│                                     │
│ ▼ COMMANDS                          │
│   • Command timeout                 │
│   • Command history size            │
│   • Show terminal output            │
│                                     │
│ ▼ USER INTERFACE                    │
│   • Theme                           │
│   • Display mode                    │
│   • Animations                      │
│   • Feature card size               │
│                                     │
│ ▼ SCANNING                          │
│   • Auto-scan interval              │
│   • Auto-save tags                  │
│   • Duplicate handling              │
│                                     │
│ ▼ LOGGING                           │
│   • Log level                       │
│   • Auto-save logs                  │
│   • Export location                 │
│                                     │
│ ▼ ADVANCED                          │
│   • Raw command mode                │
│   • Debug mode                      │
│   • Experimental features           │
│   • USB debug info                  │
│                                     │
│ ▼ DATA MANAGEMENT                   │
│   • Export settings                 │
│   • Import settings                 │
│   • Reset to defaults               │
│   • Clear all data                  │
│                                     │
│ ▼ ABOUT                             │
│   • Version info                    │
│   • Open source licenses            │
│   • Credits & contributors          │
│   • Check for updates               │
│                                     │
└─────────────────────────────────────┘
```

### 1.2 Navigation Pattern

- **Entry**: Gear icon in top-right of Dashboard → Full-screen settings
- **Exit**: Back button or close icon → Return to Dashboard
- **Deep Links**: Support direct links to specific settings (e.g., `settings://connection`)

---

## 2. Category Design

### 2.1 Category Header Component

```
┌─────────────────────────────────────┐
│ ▼ CONNECTION                    [5] │  ← Chevron, Title, Item Count
│   Quick access to device settings   │  ← Subtitle
└─────────────────────────────────────┘
```

**Properties:**
- **Icon**: Category-specific icon (optional)
- **Title**: Bold, 16sp
- **Subtitle**: Light gray, 14sp
- **Item Count**: Badge showing number of settings
- **Collapsible**: Tap to expand/collapse
- **State Persistence**: Remember collapsed state per session

### 2.2 Category Order (Priority)

1. **CONNECTION** - Most critical for device operation
2. **COMMANDS** - Frequently adjusted during use
3. **USER INTERFACE** - Visual customization
4. **SCANNING** - Tag operation settings
5. **LOGGING** - Data recording
6. **ADVANCED** - Power user features
7. **DATA MANAGEMENT** - Import/export/reset
8. **ABOUT** - App information

---

## 3. Setting Item Designs

### 3.1 Toggle Setting (Switch)

```
┌─────────────────────────────────────┐
│ Auto-reconnect on disconnect    [◉] │  ← Title + Switch
│ Automatically reconnect to          │
│ Proxmark3 when connection is lost   │  ← Description
└─────────────────────────────────────┘
```

**Components:**
- Title (bold, 15sp)
- Description (gray, 13sp, wraps)
- Switch (Material3 Switch component)
- Optional badge: "Recommended", "Beta", "Pro"

**States:**
- Enabled/Disabled
- Requires restart (show icon)
- Requires permission (show lock icon)

### 3.2 Slider Setting

```
┌─────────────────────────────────────┐
│ Connection timeout                  │  ← Title
│ How long to wait before timeout     │  ← Description
│                                     │
│ [════●══════════] 10s               │  ← Slider + Value Display
│ 1s                            30s   │  ← Min/Max labels
└─────────────────────────────────────┘
```

**Components:**
- Title & description
- Slider with thumb
- Current value display (updates in real-time)
- Min/Max value labels
- Unit indicator (s, ms, MB, etc.)

**Variants:**
- Continuous (smooth)
- Discrete (steps)

### 3.3 Dropdown/Selector Setting

```
┌─────────────────────────────────────┐
│ Theme                               │  ← Title
│ Choose app theme                    │  ← Description
│                                     │
│ [System default          ▼]        │  ← Dropdown
└─────────────────────────────────────┘

On tap:
┌─────────────────────────────────────┐
│ ◉ System default                    │  ← Selected
│ ○ Light theme                       │
│ ○ Dark theme                        │
│ ○ AMOLED black                      │
└─────────────────────────────────────┘
```

**Components:**
- Title & description
- Current selection display
- Chevron indicator
- Bottom sheet or dialog picker
- Radio button selection

### 3.4 Text Input Setting

```
┌─────────────────────────────────────┐
│ Export directory path               │  ← Title
│ Where to save exported files        │  ← Description
│                                     │
│ [/storage/emulated/0/Proxmark  📁]  │  ← Input + Browse Button
└─────────────────────────────────────┘
```

**Components:**
- Title & description
- Text field (OutlinedTextField)
- Optional validation indicator
- Optional action button (browse, test, etc.)

### 3.5 Action Setting (Button)

```
┌─────────────────────────────────────┐
│ Export settings profile             │  ← Title
│ Save current settings to file       │  ← Description
│                                     │
│          [Export Settings]          │  ← Button (centered)
└─────────────────────────────────────┘
```

**Components:**
- Title & description
- Action button (outlined or filled)
- Optional icon on button
- Loading state during action

### 3.6 Info/Link Setting

```
┌─────────────────────────────────────┐
│ App version                    >    │  ← Title + Chevron
│ 1.0.0 (Build 1)                     │  ← Value/Subtitle
└─────────────────────────────────────┘
```

**Components:**
- Title (bold)
- Value/subtitle (gray)
- Chevron (indicates tappable)
- Ripple effect on tap

---

## 4. Setting with Preview

### 4.1 Theme Preview Setting

```
┌─────────────────────────────────────┐
│ Theme                               │
│ Choose app appearance               │
│                                     │
│ [System default          ▼]        │
│                                     │
│ ┌─────────────────────────────────┐ │
│ │ PREVIEW                         │ │  ← Live Preview Card
│ │ ┌─────┐ ┌─────┐ ┌─────┐        │ │
│ │ │  🔵 │ │  🟢 │ │  🔴 │        │ │  ← Feature Cards
│ │ └─────┘ └─────┘ └─────┘        │ │
│ │                                 │ │
│ │ Background • Surface • Primary  │ │  ← Color Labels
│ └─────────────────────────────────┘ │
└─────────────────────────────────────┘
```

**Features:**
- Real-time preview updates
- Shows key UI elements in selected theme
- Demonstrates color scheme
- Optional: Animate transitions between themes

### 4.2 Display Mode Preview

```
┌─────────────────────────────────────┐
│ Display mode                        │
│ Feature card information density    │
│                                     │
│ ○ Compact    ◉ Standard    ○ Detailed│  ← Segmented Button
│                                     │
│ ┌─────────────────────────────────┐ │
│ │ PREVIEW                         │ │
│ │ ┌───────────────────┐           │ │
│ │ │ Read Tag          │ ← Standard│ │
│ │ │ Scan for RFID tag │           │ │
│ │ │ [▶]               │           │ │
│ │ └───────────────────┘           │ │
│ └─────────────────────────────────┘ │
└─────────────────────────────────────┘
```

---

## 5. Import/Export UI

### 5.1 Export Settings Flow

**Step 1: Export Button**
```
┌─────────────────────────────────────┐
│ Export settings profile             │
│ Save current settings to file       │
│                                     │
│        [📤 Export Settings]         │
└─────────────────────────────────────┘
```

**Step 2: Export Dialog**
```
┌─────────────────────────────────────┐
│ Export Settings                  ✕  │
├─────────────────────────────────────┤
│                                     │
│ Profile name                        │
│ [My Proxmark Config            ]    │
│                                     │
│ Include:                            │
│ ☑ Connection settings               │
│ ☑ Command settings                  │
│ ☑ UI preferences                    │
│ ☑ Logging configuration             │
│ ☐ Favorite features                 │
│ ☐ Saved tags (sensitive data)       │
│                                     │
│ Export format:                      │
│ ◉ JSON   ○ ZIP (with tags)          │
│                                     │
│        [Cancel]  [Export]           │
└─────────────────────────────────────┘
```

**Step 3: Share Sheet**
- System share dialog
- Options: Save to Files, Share via...
- Default filename: `proxmark_settings_YYYYMMDD.json`

### 5.2 Import Settings Flow

**Step 1: Import Button**
```
┌─────────────────────────────────────┐
│ Import settings profile             │
│ Load settings from backup file      │
│                                     │
│        [📥 Import Settings]         │
└─────────────────────────────────────┘
```

**Step 2: File Picker**
- Android file picker
- Filter: `.json`, `.zip`
- Show recent files

**Step 3: Import Preview**
```
┌─────────────────────────────────────┐
│ Import Settings                  ✕  │
├─────────────────────────────────────┤
│                                     │
│ Profile: My Proxmark Config         │
│ Exported: 2024-01-15 14:30          │
│                                     │
│ Will import:                        │
│ ✓ Connection settings (5 items)    │
│ ✓ Command settings (3 items)       │
│ ✓ UI preferences (8 items)         │
│ ✓ Logging configuration (4 items)  │
│ ⚠ Favorite features (0 items)      │
│                                     │
│ Conflicts:                          │
│ • Theme: System → Dark              │
│ • Timeout: 5s → 10s                 │
│                                     │
│ ☐ Keep current favorites            │
│                                     │
│        [Cancel]  [Import]           │
└─────────────────────────────────────┘
```

**Step 4: Confirmation**
```
┌─────────────────────────────────────┐
│         Settings Imported           │
│                                     │
│        ✓ Successfully imported      │
│          20 settings                │
│                                     │
│ Some changes require app restart.   │
│                                     │
│    [Later]  [Restart Now]           │
└─────────────────────────────────────┘
```

---

## 6. About Screen Design

### 6.1 About Screen Layout

```
┌─────────────────────────────────────┐
│ ← About                             │
├─────────────────────────────────────┤
│                                     │
│          [📱 App Icon]              │
│                                     │
│       Proxmark3 Control             │  ← App Name (bold, 20sp)
│         Version 1.0.0               │  ← Version (gray, 14sp)
│         Build 1 (Debug)             │  ← Build info
│                                     │
├─────────────────────────────────────┤
│ App Information                     │  ← Section
├─────────────────────────────────────┤
│ App version              >          │
│ 1.0.0 (Build 1)                     │
│                                     │
│ Latest version           >          │
│ You're up to date ✓                 │
│                                     │
│ Release notes            >          │
│ What's new in this version          │
│                                     │
│ Device info              >          │
│ Android 14, API 34                  │
│                                     │
├─────────────────────────────────────┤
│ Legal & Licenses                    │
├─────────────────────────────────────┤
│ Open source licenses     >          │
│ View all open source licenses       │
│                                     │
│ Privacy policy           >          │
│ How we handle your data             │
│                                     │
│ Terms of service         >          │
│ Terms and conditions                │
│                                     │
├─────────────────────────────────────┤
│ Credits                             │
├─────────────────────────────────────┤
│ Development team         >          │
│ Meet the contributors               │
│                                     │
│ Proxmark3 project        >          │
│ Official Proxmark3 project          │
│                                     │
│ Report a bug             >          │
│ GitHub issue tracker                │
│                                     │
├─────────────────────────────────────┤
│ Advanced                            │
├─────────────────────────────────────┤
│ Diagnostics              >          │
│ Run system diagnostics              │
│                                     │
│ Reset app                >          │
│ Clear all data and settings         │
│                                     │
└─────────────────────────────────────┘
```

### 6.2 Version Detail Screen

```
┌─────────────────────────────────────┐
│ ← Version Information               │
├─────────────────────────────────────┤
│                                     │
│ Version name                        │
│ 1.0.0                              │  ← Copyable text
│                                     │
│ Version code                        │
│ 1                                   │
│                                     │
│ Build type                          │
│ Debug                               │
│                                     │
│ Build date                          │
│ 2024-01-15 14:30:00 UTC            │
│                                     │
│ Git commit                          │
│ abc123f (main)                      │
│                                     │
│ ─────────────────────────           │
│                                     │
│ Dependencies                        │
│ • Jetpack Compose 1.5.14            │
│ • Kotlin 1.9.24                     │
│ • Android SDK 34                    │
│ • Material 3 1.2.1                  │
│                                     │
│        [📋 Copy All Info]           │
│                                     │
└─────────────────────────────────────┘
```

### 6.3 Open Source Licenses Screen

```
┌─────────────────────────────────────┐
│ ← Open Source Licenses              │
├─────────────────────────────────────┤
│ [Search licenses...]                │
├─────────────────────────────────────┤
│                                     │
│ ▼ Jetpack Compose                   │  ← Collapsible
│   Version: 1.5.14                   │
│   License: Apache 2.0               │
│   [View Full License]               │
│                                     │
│ ▼ Kotlin Standard Library           │
│   Version: 1.9.24                   │
│   License: Apache 2.0               │
│   [View Full License]               │
│                                     │
│ ▼ Material Components               │
│   Version: 1.2.1                    │
│   License: Apache 2.0               │
│   [View Full License]               │
│                                     │
│ ▼ OkHttp (if used)                  │
│   Version: 4.x.x                    │
│   License: Apache 2.0               │
│   [View Full License]               │
│                                     │
│        [Export All Licenses]        │
│                                     │
└─────────────────────────────────────┘
```

---

## 7. Search Settings Functionality

### 7.1 Search Field (Sticky Header)

```
┌─────────────────────────────────────┐
│ [🔍 Search settings...]        [✕] │  ← Always visible at top
└─────────────────────────────────────┘
```

**Behavior:**
- Sticky at top (scrolls with content initially, then sticks)
- Real-time filtering (debounced 300ms)
- Searches: titles, descriptions, category names
- Clear button appears when text entered
- Highlights matching text in results

### 7.2 Search Results Display

```
┌─────────────────────────────────────┐
│ [🔍 "timeout"...]              [✕] │
├─────────────────────────────────────┤
│ 3 results                           │  ← Result count
├─────────────────────────────────────┤
│ CONNECTION                          │  ← Category badge
│ Connection timeout                  │  ← Matching title (bold)
│ How long to wait before timeout     │  ← "timeout" highlighted
│                                     │
│ COMMANDS                            │
│ Command timeout                     │
│ Maximum time for command execution  │
│                                     │
│ SCANNING                            │
│ Scan timeout                        │
│ How long to wait for tag response   │
│                                     │
└─────────────────────────────────────┘
```

**Features:**
- Show category badge for each result
- Highlight matching terms
- Show full description
- Tap to jump to setting
- Categories auto-expand to show match

### 7.3 No Results State

```
┌─────────────────────────────────────┐
│ [🔍 "proxmark4"...]           [✕] │
├─────────────────────────────────────┤
│                                     │
│           🔍                        │
│     No settings found               │
│                                     │
│   Try different keywords or         │
│   browse by category                │
│                                     │
│        [Clear Search]               │
│                                     │
└─────────────────────────────────────┘
```

---

## 8. Complete Settings Catalog

### 8.1 CONNECTION Category

| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| **Auto-reconnect** | Toggle | ON | Automatically reconnect to Proxmark3 when connection is lost |
| **Connection timeout** | Slider | 5s | How long to wait before marking device as disconnected (1-30s) |
| **Reconnect delay** | Slider | 2s | Delay between reconnection attempts (1-10s) |
| **Max reconnect attempts** | Slider | 5 | Maximum reconnection attempts before giving up (1-20) |
| **USB power on connect** | Toggle | OFF | Send USB power-on command when connecting |
| **Persist connection** | Toggle | ON | Keep USB connection active in background |

### 8.2 COMMANDS Category

| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| **Command timeout** | Slider | 5s | Maximum time to wait for command response (1-30s) |
| **Command history size** | Slider | 100 | Number of commands to keep in history (10-500) |
| **Show terminal output** | Toggle | OFF | Display raw Proxmark3 terminal output |
| **Command confirmation** | Toggle | OFF | Require confirmation for all commands |
| **Skip safety confirmations** | Toggle | OFF | Skip confirmation dialogs for safe commands |
| **Auto-scroll terminal** | Toggle | ON | Automatically scroll to latest terminal output |

### 8.3 USER INTERFACE Category

| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| **Theme** | Dropdown | System | App theme: System, Light, Dark, AMOLED Black |
| **Dynamic colors** | Toggle | ON | Use Material You dynamic colors (Android 12+) |
| **Display mode** | Segmented | Standard | Feature card density: Compact, Standard, Detailed |
| **Feature card size** | Slider | Medium | Size of feature cards: Small, Medium, Large |
| **Animations** | Toggle | ON | Enable UI animations and transitions |
| **Animation speed** | Slider | 1.0x | Animation playback speed (0.5x-2.0x) |
| **Show category icons** | Toggle | ON | Display icons next to category names |
| **Haptic feedback** | Toggle | ON | Vibration feedback for interactions |
| **Font scale** | Slider | 1.0x | Text size multiplier (0.8x-1.5x) |

### 8.4 SCANNING Category

| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| **Auto-scan enabled** | Toggle | OFF | Continuously scan for tags in background |
| **Auto-scan interval** | Slider | 3s | Time between automatic scans (1-15s) |
| **Auto-save tags** | Toggle | ON | Automatically save discovered tags |
| **Duplicate handling** | Dropdown | Update | How to handle duplicate tags: Skip, Update, Keep All |
| **Tag history size** | Slider | 100 | Maximum saved tags (10-1000) |
| **Notify on tag found** | Toggle | OFF | Show notification when new tag is discovered |
| **Scan timeout** | Slider | 3s | Maximum time to wait for tag response (1-10s) |

### 8.5 LOGGING Category

| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| **Log level** | Dropdown | Info | Minimum log level: Debug, Info, Warning, Error |
| **Auto-save logs** | Toggle | OFF | Automatically save session logs to file |
| **Log retention days** | Slider | 7 | Days to keep old log files (1-90) |
| **Export location** | Text Input | /Proxmark | Default directory for exported files |
| **Include timestamps** | Toggle | ON | Add timestamps to log entries |
| **Log USB traffic** | Toggle | OFF | Record raw USB communication (debug) |
| **Max log file size** | Slider | 10MB | Maximum size per log file (1-100MB) |

### 8.6 ADVANCED Category

| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| **Raw command mode** | Toggle | OFF | Enable manual command entry (expert mode) |
| **Debug mode** | Toggle | OFF | Show debug information and diagnostics |
| **Experimental features** | Toggle | OFF | Enable beta/experimental functionality |
| **USB debug info** | Action | - | View detailed USB connection information |
| **Crash reports** | Toggle | ON | Automatically send crash reports (anonymous) |
| **Performance mode** | Toggle | OFF | Optimize for performance over features |
| **Developer options** | Action | - | Advanced developer settings (hidden by default) |

### 8.7 DATA MANAGEMENT Category

| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| **Export settings** | Action | - | Export current settings to JSON file |
| **Import settings** | Action | - | Import settings from backup file |
| **Backup saved tags** | Action | - | Export all saved tags to file |
| **Restore tags** | Action | - | Import tags from backup file |
| **Reset to defaults** | Action | - | Reset all settings to factory defaults |
| **Clear all data** | Action | - | Delete all tags, history, and settings (destructive) |

### 8.8 ABOUT Category

| Setting | Type | Default | Description |
|---------|------|---------|-------------|
| **App version** | Info | 1.0.0 | Current app version and build number |
| **Check for updates** | Action | - | Check for app updates |
| **Release notes** | Action | - | View changelog for current version |
| **Open source licenses** | Action | - | View all open source licenses |
| **Privacy policy** | Link | - | View privacy policy (opens browser) |
| **Terms of service** | Link | - | View terms and conditions |
| **Development team** | Action | - | View contributors and credits |
| **Proxmark3 project** | Link | - | Visit official Proxmark3 website |
| **Report a bug** | Link | - | Open GitHub issues page |
| **Device info** | Info | - | View device and OS information |

---

## 9. Compose Implementation Structure

### 9.1 Navigation Structure

```kotlin
sealed class SettingsDestination {
    object Main : SettingsDestination()
    object About : SettingsDestination()
    object Licenses : SettingsDestination()
    data class Category(val categoryId: String) : SettingsDestination()
}

@Composable
fun SettingsNavHost(
    navController: NavHostController,
    settings: AppSettings,
    onUpdateSettings: (AppSettings) -> Unit
) {
    NavHost(
        navController = navController,
        startDestination = "settings_main"
    ) {
        composable("settings_main") {
            SettingsScreen(
                settings = settings,
                onNavigate = { navController.navigate(it) },
                onUpdateSettings = onUpdateSettings
            )
        }
        composable("about") {
            AboutScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
        composable("licenses") {
            LicensesScreen(
                onNavigateBack = { navController.popBackStack() }
            )
        }
    }
}
```

### 9.2 Settings Data Model

```kotlin
// Expanded AppSettings model
data class AppSettings(
    // Connection
    val autoReconnect: Boolean = true,
    val connectionTimeoutMs: Int = 5_000,
    val reconnectDelayMs: Int = 2_000,
    val maxReconnectAttempts: Int = 5,
    val usbPowerOnConnect: Boolean = false,
    val persistConnection: Boolean = true,
    
    // Commands
    val commandTimeoutMs: Int = 5_000,
    val commandHistorySize: Int = 100,
    val showTerminalOutput: Boolean = false,
    val requireCommandConfirmation: Boolean = false,
    val skipSafetyConfirmations: Boolean = false,
    val autoScrollTerminal: Boolean = true,
    
    // UI
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColors: Boolean = true,
    val displayMode: DisplayMode = DisplayMode.STANDARD,
    val featureCardSize: FeatureCardSize = FeatureCardSize.MEDIUM,
    val animationsEnabled: Boolean = true,
    val animationSpeed: Float = 1.0f,
    val showCategoryIcons: Boolean = true,
    val hapticFeedback: Boolean = true,
    val fontScale: Float = 1.0f,
    
    // Scanning
    val autoScanEnabled: Boolean = false,
    val autoScanIntervalMs: Int = 3_000,
    val autoSaveTags: Boolean = true,
    val duplicateHandling: DuplicateHandling = DuplicateHandling.UPDATE,
    val tagHistorySize: Int = 100,
    val notifyOnTagFound: Boolean = false,
    val scanTimeoutMs: Int = 3_000,
    
    // Logging
    val logLevel: LogLevel = LogLevel.INFO,
    val autoSaveLogs: Boolean = false,
    val logRetentionDays: Int = 7,
    val exportLocation: String = "/storage/emulated/0/Proxmark",
    val includeTimestamps: Boolean = true,
    val logUsbTraffic: Boolean = false,
    val maxLogFileSizeMb: Int = 10,
    
    // Advanced
    val rawCommandMode: Boolean = false,
    val debugMode: Boolean = false,
    val experimentalFeatures: Boolean = false,
    val crashReports: Boolean = true,
    val performanceMode: Boolean = false,
    
    // Favorites (existing)
    val favoriteFeatureIds: Set<String> = emptySet()
)

enum class ThemeMode { SYSTEM, LIGHT, DARK, AMOLED }
enum class DisplayMode { COMPACT, STANDARD, DETAILED }
enum class FeatureCardSize { SMALL, MEDIUM, LARGE }
enum class DuplicateHandling { SKIP, UPDATE, KEEP_ALL }
enum class LogLevel { DEBUG, INFO, WARNING, ERROR }
```

### 9.3 Settings Categories

```kotlin
enum class SettingsCategory(
    val id: String,
    val title: String,
    val icon: ImageVector,
    val description: String
) {
    CONNECTION(
        "connection",
        "Connection",
        Icons.Default.Cable,
        "Device connection and USB settings"
    ),
    COMMANDS(
        "commands",
        "Commands",
        Icons.Default.Terminal,
        "Command execution and history"
    ),
    USER_INTERFACE(
        "ui",
        "User Interface",
        Icons.Default.Palette,
        "Theme, layout, and visual settings"
    ),
    SCANNING(
        "scanning",
        "Scanning",
        Icons.Default.Radar,
        "Tag scanning and auto-scan settings"
    ),
    LOGGING(
        "logging",
        "Logging",
        Icons.Default.Description,
        "Log levels and export settings"
    ),
    ADVANCED(
        "advanced",
        "Advanced",
        Icons.Default.Code,
        "Debug and experimental features"
    ),
    DATA_MANAGEMENT(
        "data",
        "Data Management",
        Icons.Default.Storage,
        "Import, export, and reset"
    ),
    ABOUT(
        "about",
        "About",
        Icons.Default.Info,
        "App information and licenses"
    )
}
```

---

## 10. Example Compose Code

### 10.1 Settings Screen (Main)

```kotlin
@Composable
fun SettingsScreen(
    settings: AppSettings,
    onUpdateSettings: (AppSettings) -> Unit,
    onNavigate: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var expandedCategories by remember { 
        mutableStateOf(SettingsCategory.entries.map { it.id }.toSet()) 
    }
    
    Scaffold(
        topBar = {
            SettingsTopBar(
                onNavigateBack = { /* Pop back */ },
                onShowHelp = { /* Show help dialog */ }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search field
            SettingsSearchField(
                query = searchQuery,
                onQueryChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            )
            
            // Settings list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SettingsCategory.entries.forEach { category ->
                    val isExpanded = category.id in expandedCategories
                    val matchesSearch = searchQuery.isEmpty() || 
                        category.title.contains(searchQuery, ignoreCase = true)
                    
                    if (matchesSearch) {
                        item(key = "${category.id}_header") {
                            CategoryHeader(
                                category = category,
                                isExpanded = isExpanded,
                                itemCount = getCategoryItemCount(category),
                                onClick = {
                                    expandedCategories = if (isExpanded) {
                                        expandedCategories - category.id
                                    } else {
                                        expandedCategories + category.id
                                    }
                                }
                            )
                        }
                        
                        if (isExpanded) {
                            items(getSettingsForCategory(category, settings)) { setting ->
                                SettingItem(
                                    setting = setting,
                                    onUpdate = { /* Update specific setting */ }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
```

### 10.2 Category Header Component

```kotlin
@Composable
fun CategoryHeader(
    category: SettingsCategory,
    isExpanded: Boolean,
    itemCount: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.fillMaxWidth(),
        color = MaterialTheme.colorScheme.surfaceVariant,
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = if (isExpanded) 
                        Icons.Default.ExpandMore 
                    else 
                        Icons.Default.ChevronRight,
                    contentDescription = if (isExpanded) "Collapse" else "Expand"
                )
                
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                
                Column {
                    Text(
                        text = category.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = category.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            
            Badge {
                Text(text = itemCount.toString())
            }
        }
    }
}
```

### 10.3 Toggle Setting Component

```kotlin
@Composable
fun ToggleSetting(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    badge: String? = null
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        onClick = { if (enabled) onCheckedChange(!checked) },
        enabled = enabled
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Medium
                    )
                    
                    badge?.let {
                        Badge(
                            containerColor = when (it) {
                                "Beta" -> MaterialTheme.colorScheme.tertiary
                                "Recommended" -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.secondary
                            }
                        ) {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.labelSmall
                            )
                        }
                    }
                }
                
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Switch(
                checked = checked,
                onCheckedChange = null, // Handled by Surface
                enabled = enabled
            )
        }
    }
}
```

### 10.4 Slider Setting Component

```kotlin
@Composable
fun SliderSetting(
    title: String,
    description: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    steps: Int = 0,
    valueLabel: (Float) -> String,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Slider(
                value = value,
                onValueChange = onValueChange,
                valueRange = valueRange,
                steps = steps,
                modifier = Modifier.weight(1f)
            )
            
            Text(
                text = valueLabel(value),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(start = 16.dp)
            )
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = valueLabel(valueRange.start),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Text(
                text = valueLabel(valueRange.endInclusive),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
```

### 10.5 Dropdown Setting Component

```kotlin
@Composable
fun <T> DropdownSetting(
    title: String,
    description: String,
    currentValue: T,
    options: List<T>,
    optionLabel: (T) -> String,
    onValueChange: (T) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        ExposedDropdownMenuBox(
            expanded = expanded,
            onExpandedChange = { expanded = it }
        ) {
            OutlinedTextField(
                value = optionLabel(currentValue),
                onValueChange = {},
                readOnly = true,
                trailingIcon = {
                    Icon(
                        imageVector = if (expanded) 
                            Icons.Default.ExpandLess 
                        else 
                            Icons.Default.ExpandMore,
                        contentDescription = null
                    )
                },
                modifier = Modifier
                    .menuAnchor()
                    .fillMaxWidth()
            )
            
            ExposedDropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(optionLabel(option)) },
                        onClick = {
                            onValueChange(option)
                            expanded = false
                        },
                        leadingIcon = if (option == currentValue) {
                            {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected"
                                )
                            }
                        } else null
                    )
                }
            }
        }
    }
}
```

### 10.6 Action Setting Component

```kotlin
@Composable
fun ActionSetting(
    title: String,
    description: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
    destructive: Boolean = false
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = title,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium
        )
        
        Text(
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Button(
            onClick = onAction,
            modifier = Modifier.fillMaxWidth(),
            colors = if (destructive) {
                ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error
                )
            } else {
                ButtonDefaults.buttonColors()
            }
        ) {
            Text(text = actionLabel)
        }
    }
}
```

### 10.7 Theme Preview Component

```kotlin
@Composable
fun ThemePreviewSetting(
    currentTheme: ThemeMode,
    onThemeChange: (ThemeMode) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        DropdownSetting(
            title = "Theme",
            description = "Choose app appearance",
            currentValue = currentTheme,
            options = ThemeMode.entries,
            optionLabel = { 
                when (it) {
                    ThemeMode.SYSTEM -> "System default"
                    ThemeMode.LIGHT -> "Light theme"
                    ThemeMode.DARK -> "Dark theme"
                    ThemeMode.AMOLED -> "AMOLED black"
                }
            },
            onValueChange = onThemeChange
        )
        
        // Preview card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = when (currentTheme) {
                    ThemeMode.LIGHT -> Color(0xFFFFFBFF)
                    ThemeMode.DARK -> Color(0xFF1C1B1F)
                    ThemeMode.AMOLED -> Color.Black
                    ThemeMode.SYSTEM -> MaterialTheme.colorScheme.surface
                }
            )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "PREVIEW",
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold
                )
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(3) { index ->
                        Card(
                            modifier = Modifier
                                .weight(1f)
                                .height(60.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = when (index) {
                                    0 -> MaterialTheme.colorScheme.primaryContainer
                                    1 -> MaterialTheme.colorScheme.secondaryContainer
                                    else -> MaterialTheme.colorScheme.tertiaryContainer
                                }
                            )
                        ) {}
                    }
                }
                
                Text(
                    text = "Background • Surface • Primary",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }
        }
    }
}
```

### 10.8 Search Field Component

```kotlin
@Composable
fun SettingsSearchField(
    query: String,
    onQueryChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedTextField(
        value = query,
        onValueChange = onQueryChange,
        modifier = modifier,
        placeholder = { Text("Search settings...") },
        leadingIcon = {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "Search"
            )
        },
        trailingIcon = if (query.isNotEmpty()) {
            {
                IconButton(onClick = { onQueryChange("") }) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear search"
                    )
                }
            }
        } else null,
        singleLine = true,
        shape = RoundedCornerShape(24.dp)
    )
}
```

---

## 11. Implementation Guidelines

### 11.1 State Management

- **Use ViewModel**: Create `SettingsViewModel` to handle all settings operations
- **DataStore**: Persist settings using `SettingsStore` (already exists)
- **Flow**: Expose settings as `StateFlow` for reactive UI updates
- **Validation**: Validate all inputs before saving (ranges, formats, etc.)

### 11.2 User Experience

- **Instant Feedback**: Update UI immediately on change
- **Confirmation Dialogs**: Show for destructive actions (reset, clear data)
- **Loading States**: Show progress for async operations (export, import)
- **Error Handling**: Display clear error messages with recovery options
- **Accessibility**: Full TalkBack support, semantic labels, touch targets ≥48dp

### 11.3 Performance

- **Lazy Loading**: Use `LazyColumn` for settings list
- **Debouncing**: Debounce search input (300ms)
- **State Hoisting**: Keep state in ViewModel, not composables
- **Recomposition**: Minimize by using `remember` and `derivedStateOf`

### 11.4 Testing

- **Unit Tests**: Test ViewModel logic and state transformations
- **UI Tests**: Test search, filtering, navigation
- **Integration Tests**: Test import/export functionality
- **Accessibility Tests**: Verify TalkBack navigation

---

## 12. Future Enhancements

### 12.1 Phase 2 Features

- **Settings Profiles**: Multiple named profiles (Work, Personal, Lab)
- **Cloud Sync**: Sync settings across devices
- **Smart Defaults**: ML-based setting recommendations
- **Gesture Shortcuts**: Swipe actions on settings items
- **Voice Control**: "Set timeout to 10 seconds"

### 12.2 Advanced Features

- **Settings Backup Schedule**: Auto-backup to cloud
- **Setting History**: View and restore previous values
- **Conditional Settings**: Hide/show based on device capabilities
- **A/B Testing**: Test different default values
- **Analytics**: Track which settings users change most

---

## 13. Visual Design Specifications

### 13.1 Typography

- **Category Header**: Title Medium, Bold (16sp)
- **Setting Title**: Body Large, Medium (15sp)
- **Setting Description**: Body Medium (13sp), 60% opacity
- **Value Display**: Body Large, Bold (15sp)

### 13.2 Spacing

- **Section Padding**: 16dp horizontal, 12dp vertical
- **Item Spacing**: 8dp between settings
- **Category Spacing**: 16dp between categories
- **Content Padding**: 16dp all sides

### 13.3 Colors

- **Primary Actions**: Primary color
- **Destructive Actions**: Error color
- **Disabled Items**: 38% opacity
- **Dividers**: 12% opacity

### 13.4 Elevation

- **Category Headers**: Surface variant (no elevation)
- **Setting Items**: Surface (0dp elevation)
- **Dialogs**: 6dp elevation
- **Bottom Sheets**: 8dp elevation

---

## 14. Accessibility Requirements

### 14.1 TalkBack Support

- All settings have descriptive labels
- State changes announced ("Auto-reconnect enabled")
- Current values announced ("Timeout: 5 seconds")
- Action results announced ("Settings exported successfully")

### 14.2 Touch Targets

- Minimum 48dp × 48dp for all interactive elements
- Adequate spacing between adjacent targets
- Clear focus indicators

### 14.3 Color Contrast

- Text: minimum 4.5:1 contrast ratio
- Icons: minimum 3:1 contrast ratio
- Works in high contrast mode

### 14.4 Font Scaling

- Support up to 200% font scaling
- Text doesn't truncate at large sizes
- Layouts adapt to text size

---

## Summary

This design document provides a comprehensive settings system for the Proxmark3 app with:

✅ **8 organized categories** with 50+ settings  
✅ **Multiple setting types**: toggle, slider, dropdown, input, action  
✅ **Visual previews** for theme and display settings  
✅ **Import/export** functionality with conflict resolution  
✅ **Search** with real-time filtering  
✅ **About screen** with version info and licenses  
✅ **Complete Compose code** examples  
✅ **Accessibility** and performance guidelines  

The design prioritizes:
- **Discoverability**: Search and categorization
- **Safety**: Confirmations for destructive actions
- **Flexibility**: Import/export for power users
- **Accessibility**: Full TalkBack and scaling support

**Next Steps:**
1. Review design with stakeholders
2. Implement core settings infrastructure (ViewModel, DataStore expansion)
3. Build reusable setting components
4. Implement category by category
5. Add import/export functionality
6. Create About screen
7. Add search functionality
8. Test with real users
