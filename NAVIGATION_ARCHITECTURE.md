# Navigation Architecture Design
## Proxmark3 Control App - UX/UI Specification

**Version:** 1.0  
**Last Updated:** 2024  
**Target:** Zero-friction, fast-access mobile UX

---

## 1. Bottom Navigation Structure

### Five Primary Sections

```
┌──────────────────────────────────────────────────────────────┐
│  [🏠]      [📡]      [💻]      [🔧]      [⚙️]               │
│Dashboard    Scan   Terminal   Tools   Settings              │
└──────────────────────────────────────────────────────────────┘
```

| Section | Icon | Label | Purpose |
|---------|------|-------|---------|
| **Dashboard** | 🏠 Home | Dashboard | Overview, quick actions, recent scans, favorites |
| **Scan** | 📡 Radar | Scan | Dedicated scanning interface with live feedback |
| **Terminal** | 💻 Terminal | Terminal | Raw PM3 command line with history |
| **Tools** | 🔧 Wrench | Tools | Feature catalog browser by category |
| **Settings** | ⚙️ Gear | Settings | App preferences, USB config, about |

**Material Icon Suggestions:**
- Dashboard: `Icons.Filled.Home` or `Icons.Filled.Dashboard`
- Scan: `Icons.Filled.Radar` or `Icons.Filled.Nfc`
- Terminal: `Icons.Filled.Terminal` or `Icons.Filled.Code`
- Tools: `Icons.Filled.Build` or `Icons.Filled.Construction`
- Settings: `Icons.Filled.Settings`

---

## 2. Screen Hierarchy Map

```
Root (MainActivity)
│
├─ Bottom Nav Host
   │
   ├─ 🏠 DASHBOARD (Default)
   │   ├─ Connection Status Banner
   │   ├─ Quick Actions (Favorites)
   │   ├─ Recent Scans (Last 5)
   │   ├─ Device Status Card
   │   └─ [Modal: Feature Confirmation Dialog]
   │
   ├─ 📡 SCAN
   │   ├─ Scan Controls (Start/Stop)
   │   ├─ Live Feedback (Antenna animation)
   │   ├─ Scan History (Session)
   │   ├─ Tag Details Viewer
   │   ├─ [Sheet: Scan Options]
   │   ├─ [Sheet: Tag Actions]
   │   └─ [Sheet: Auto-scan Settings]
   │
   ├─ 💻 TERMINAL
   │   ├─ Command Input Field
   │   ├─ Terminal Output (Scrollable)
   │   ├─ Command History Chips
   │   ├─ [Sheet: Command Templates]
   │   ├─ [Sheet: Output Filters]
   │   └─ [Sheet: Export Log]
   │
   ├─ 🔧 TOOLS
   │   ├─ Category Tabs (Horizontal scroll)
   │   │   ├─ Quick Start
   │   │   ├─ High Frequency
   │   │   ├─ Low Frequency
   │   │   ├─ Mifare
   │   │   ├─ Attacks
   │   │   ├─ Emulation
   │   │   ├─ Utilities
   │   │   └─ Scripts
   │   ├─ Feature Cards (Grid/List)
   │   ├─ Search Bar (Sticky)
   │   ├─ [Sheet: Feature Details]
   │   ├─ [Sheet: Feature Parameters]
   │   └─ [Sheet: Run History]
   │
   └─ ⚙️ SETTINGS
       ├─ Connection Settings
       ├─ Appearance (Theme, Font)
       ├─ Behavior (Auto-scan, Timeouts)
       ├─ Storage (Tag DB, Logs)
       ├─ Safety (Confirmations)
       ├─ Advanced (USB Debug)
       ├─ About (Version, Licenses)
       └─ [Dialog: Clear Data Confirmation]

Overlays (Non-navigation)
├─ Tag Detail View (Full screen overlay)
├─ Dump Viewer (Full screen overlay)
├─ Dump Comparison (Full screen overlay)
└─ Workflow Viewer (Full screen overlay)
```

---

## 3. FAB Behavior Per Screen

Context-sensitive FAB changes based on active screen:

| Screen | FAB Icon | FAB Label | Action | Visibility |
|--------|----------|-----------|--------|------------|
| **Dashboard** | 📡 | "Quick Scan" | Trigger auto-scan or navigate to Scan screen | Always |
| **Scan** | 🚀 / ⏹️ | "Scan" / "Stop" | Start/Stop active scan | Toggle state |
| **Terminal** | ⏎ | "Send" | Submit command from input field | When input not empty |
| **Tools** | ⭐ | "Favorites" | Show/filter favorite features | Always |
| **Settings** | 🔄 | "Reconnect" | Force USB reconnect | When disconnected |

**FAB States:**
- **Idle:** Primary color, enabled
- **Running:** Accent color, pulsing animation
- **Disabled:** Grey, when connection lost
- **Extended:** Show label on scroll-up, hide on scroll-down

**Implementation:**
```kotlin
@Composable
fun AdaptiveFab(
    currentRoute: String,
    uiState: MainUiState,
    extended: Boolean,
    onAction: () -> Unit
)
```

---

## 4. Gesture Mappings

### Horizontal Swipe (Bottom Nav Screens)
```
Dashboard ⟷ Scan ⟷ Terminal ⟷ Tools ⟷ Settings
```
- **Left Swipe:** Next screen
- **Right Swipe:** Previous screen
- **Wrap:** Settings → Dashboard (circular)

### Long Press Actions

| Element | Long Press Action |
|---------|------------------|
| **Tag Card** | Show quick actions sheet (Copy UID, Clone, Emulate, Delete) |
| **Feature Card** | Add/remove from favorites + show details |
| **Terminal Line** | Copy line to clipboard |
| **Command History Chip** | Edit before sending |
| **Scan Button (FAB)** | Open scan options (frequency, timeout) |

### Pull Gestures

| Screen | Pull Down | Pull Up |
|--------|-----------|---------|
| **Scan** | Refresh scan / Clear session | - |
| **Terminal** | Scroll to latest output | - |
| **Tools** | Refresh feature catalog | - |
| **Dashboard** | Force sync tags | Show extended history |

### Pinch/Zoom
- **Terminal:** Adjust font size (persist to settings)
- **Tag Details:** Zoom into hex dump view

---

## 5. Navigation Flows for Common Tasks

### Flow 1: Scan Tag
```
Entry → Scan Screen
   ↓
[FAB: Scan] pressed
   ↓
Scanning state (animated feedback)
   ↓
Tag detected → Auto-show Tag Details Sheet
   ↓
Options: View Details | Clone | Emulate | Dismiss
   ↓
[Clone] → Navigate to Tools > Mifare > Clone Card
         → Pre-fill UID from scan
```

### Flow 2: Clone Tag
```
Entry → Dashboard
   ↓
Recent Scans card → Tap tag
   ↓
Tag Details overlay opens
   ↓
[Clone] button
   ↓
Bottom sheet: "Clone Options"
   ├─ Quick Clone (use last scanned)
   ├─ Manual Entry
   └─ Load from Saved
   ↓
Confirmation dialog (DESTRUCTIVE safety)
   ↓
Execute → Terminal screen with live output
   ↓
Result banner → Option to verify
```

### Flow 3: Send Custom Command
```
Entry → Terminal Screen
   ↓
Type command or select from history chips
   ↓
[FAB: Send] or Enter key
   ↓
Command sent → Live output streaming
   ↓
Long-press output line → Copy to clipboard
   ↓
[Export] button → Share log via Android intent
```

### Flow 4: Browse & Execute Feature
```
Entry → Tools Screen
   ↓
Select category tab (e.g., "Attacks")
   ↓
Scroll/search features
   ↓
Tap feature card
   ↓
IF requires parameters:
   └─ Parameter input sheet → [Run]
ELSE IF ADVANCED/DESTRUCTIVE:
   └─ Confirmation dialog → [Confirm]
ELSE:
   └─ Execute immediately
   ↓
Navigate to Terminal (auto) with live output
   ↓
Result notification → Return to previous screen
```

### Flow 5: Manage Favorites
```
Entry → Any screen
   ↓
Long-press feature card → Add to favorites
   ↓
Navigate to Dashboard → See in Quick Actions
   ↓
Tap favorite → Immediate execution (if SAFE)
   ↓
OR: Tools > [FAB: Favorites] → Filter view
```

---

## 6. Entry Points & Deep Link Structure

### Deep Link URI Scheme
```
proxmark3://screen/action/parameters
```

### Link Patterns

| URI | Destination | Parameters | Example |
|-----|-------------|------------|---------|
| `proxmark3://dashboard` | Dashboard screen | - | Open app to dashboard |
| `proxmark3://scan` | Scan screen | `?auto=true` | Start scan immediately |
| `proxmark3://scan/{uid}` | Tag details | UID string | View specific saved tag |
| `proxmark3://terminal` | Terminal screen | `?cmd={encoded}` | Pre-fill command |
| `proxmark3://tools/{category}` | Tools with category | Category enum | Open Mifare tools |
| `proxmark3://tools/feature/{id}` | Feature detail | Feature ID | Open specific feature |
| `proxmark3://settings/{section}` | Settings section | Section name | USB settings |

### Intent Filters (AndroidManifest.xml)
```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="proxmark3" />
</intent-filter>
```

### Shortcut Actions (Android 7.1+)
```xml
<!-- res/xml/shortcuts.xml -->
<shortcuts>
    <shortcut android:shortcutId="quick_scan" android:icon="@drawable/ic_scan">
        <intent android:action="android.intent.action.VIEW" android:targetClass=".MainActivity"
                android:data="proxmark3://scan?auto=true" />
        <categories android:name="android.shortcut.conversation" />
    </shortcut>
    <shortcut android:shortcutId="terminal" android:icon="@drawable/ic_terminal">
        <intent android:data="proxmark3://terminal" />
    </shortcut>
    <shortcut android:shortcutId="last_tag" android:icon="@drawable/ic_tag">
        <intent android:data="proxmark3://dashboard#recent" />
    </shortcut>
</shortcuts>
```

### Widget Entry Points
- **1x1 Quick Scan Widget:** Launches `proxmark3://scan?auto=true`
- **2x2 Tag History Widget:** Shows last 4 tags, tap opens detail
- **4x2 Feature Launcher:** Grid of favorite features

---

## 7. Compose Navigation Graph Outline

### Dependencies
```kotlin
// build.gradle.kts
implementation("androidx.navigation:navigation-compose:2.7.7")
```

### Navigation Setup

```kotlin
// navigation/NavGraph.kt
sealed class Screen(val route: String) {
    object Dashboard : Screen("dashboard")
    object Scan : Screen("scan")
    object Terminal : Screen("terminal")
    object Tools : Screen("tools/{category?}") {
        fun createRoute(category: String? = null) = 
            if (category != null) "tools/$category" else "tools"
    }
    object Settings : Screen("settings/{section?}") {
        fun createRoute(section: String? = null) = 
            if (section != null) "settings/$section" else "settings"
    }
    object TagDetail : Screen("tag/{uid}") {
        fun createRoute(uid: String) = "tag/$uid"
    }
    object FeatureDetail : Screen("feature/{id}") {
        fun createRoute(id: String) = "feature/$id"
    }
}

@Composable
fun ProxmarkNavGraph(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Screen.Dashboard.route,
    uiState: MainUiState,
    viewModel: MainViewModel
) {
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        // Bottom nav screens
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                uiState = uiState,
                onNavigateToScan = { navController.navigate(Screen.Scan.route) },
                onNavigateToTag = { uid -> navController.navigate(Screen.TagDetail.createRoute(uid)) },
                onRunFeature = viewModel::runFeature
                // ... other callbacks
            )
        }
        
        composable(Screen.Scan.route) {
            ScanScreen(
                uiState = uiState,
                onNavigateToTag = { uid -> navController.navigate(Screen.TagDetail.createRoute(uid)) },
                onStartScan = viewModel::startAutoScan,
                onStopScan = viewModel::stopAutoScan
            )
        }
        
        composable(Screen.Terminal.route) {
            TerminalScreen(
                uiState = uiState,
                onSendCommand = viewModel::sendRawCommand,
                onExportLog = { /* share intent */ }
            )
        }
        
        composable(
            route = Screen.Tools.route,
            arguments = listOf(navArgument("category") { 
                type = NavType.StringType
                nullable = true 
            })
        ) { backStackEntry ->
            val category = backStackEntry.arguments?.getString("category")
            ToolsScreen(
                uiState = uiState,
                initialCategory = category,
                onNavigateToFeature = { id -> navController.navigate(Screen.FeatureDetail.createRoute(id)) },
                onRunFeature = viewModel::runFeature
            )
        }
        
        composable(
            route = Screen.Settings.route,
            arguments = listOf(navArgument("section") { 
                type = NavType.StringType
                nullable = true 
            })
        ) { backStackEntry ->
            val section = backStackEntry.arguments?.getString("section")
            SettingsScreen(
                uiState = uiState,
                initialSection = section,
                onUpdateTheme = viewModel::updateThemeMode,
                onUpdateTimeout = viewModel::updateTimeout
            )
        }
        
        // Detail screens (not in bottom nav)
        composable(
            route = Screen.TagDetail.route,
            arguments = listOf(navArgument("uid") { type = NavType.StringType })
        ) { backStackEntry ->
            val uid = backStackEntry.arguments?.getString("uid") ?: return@composable
            TagDetailScreen(
                uid = uid,
                uiState = uiState,
                onNavigateBack = { navController.popBackStack() },
                onClone = { /* clone workflow */ },
                onEmulate = { /* emulation workflow */ }
            )
        }
        
        composable(
            route = Screen.FeatureDetail.route,
            arguments = listOf(navArgument("id") { type = NavType.StringType })
        ) { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id") ?: return@composable
            FeatureDetailScreen(
                featureId = id,
                uiState = uiState,
                onNavigateBack = { navController.popBackStack() },
                onRun = viewModel::runFeature
            )
        }
        
        // Deep link handling
        composable(
            route = "scan?auto={auto}",
            deepLinks = listOf(navDeepLink { 
                uriPattern = "proxmark3://scan?auto={auto}"
            }),
            arguments = listOf(navArgument("auto") { 
                type = NavType.BoolType
                defaultValue = false
            })
        ) { backStackEntry ->
            val autoStart = backStackEntry.arguments?.getBoolean("auto") ?: false
            LaunchedEffect(autoStart) {
                if (autoStart) viewModel.startAutoScan()
            }
            ScanScreen(uiState = uiState, onStartScan = viewModel::startAutoScan)
        }
    }
}
```

### Back Stack Management

```kotlin
// navigation/BackStackManager.kt
class BackStackManager(private val navController: NavHostController) {
    
    fun navigateWithSingleTop(route: String) {
        navController.navigate(route) {
            launchSingleTop = true
            restoreState = true
        }
    }
    
    fun navigateBottomNav(route: String) {
        navController.navigate(route) {
            // Pop everything up to start destination
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }
    
    fun handleDeepLink(uri: Uri): Boolean {
        return when {
            uri.scheme != "proxmark3" -> false
            uri.host == "scan" -> {
                val autoStart = uri.getQueryParameter("auto")?.toBoolean() ?: false
                navigateWithSingleTop("scan?auto=$autoStart")
                true
            }
            uri.host == "tools" && uri.pathSegments.size == 2 -> {
                val category = uri.pathSegments[1]
                navigateWithSingleTop("tools/$category")
                true
            }
            // ... other deep links
            else -> false
        }
    }
}
```

---

## 8. Modal Bottom Sheets Structure

### Sheet Hierarchy
```
Bottom Sheets (Persistent across config changes)
│
├─ ScanOptionsSheet
│   ├─ Frequency selection (HF/LF)
│   ├─ Timeout slider
│   ├─ Auto-scan toggle
│   └─ [Apply] [Cancel]
│
├─ TagActionsSheet
│   ├─ Copy UID
│   ├─ Clone to new tag
│   ├─ Emulate on PM3
│   ├─ View full dump
│   ├─ Add to group
│   └─ Delete
│
├─ FeatureParameterSheet
│   ├─ Dynamic parameter fields
│   ├─ Parameter validation
│   ├─ Help text
│   └─ [Run] [Cancel]
│
├─ CommandTemplatesSheet
│   ├─ Searchable template list
│   ├─ Category filter
│   └─ Tap to insert into terminal
│
└─ ExportOptionsSheet
    ├─ Format selection (TXT/JSON/CSV)
    ├─ Range selection (All/Visible/Selection)
    └─ Share via Android intent
```

### Sheet State Management
```kotlin
// ui/ProxmarkApp.kt
@Composable
fun ProxmarkApp(viewModel: MainViewModel) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    var currentSheet by remember { mutableStateOf<SheetContent?>(null) }
    
    Scaffold(
        bottomBar = { 
            BottomNavigationBar(
                currentRoute = currentRoute,
                onNavigate = { route -> backStackManager.navigateBottomNav(route) }
            )
        },
        floatingActionButton = {
            AdaptiveFab(
                currentRoute = currentRoute,
                uiState = uiState,
                extended = fabExtended,
                onAction = { /* context action */ }
            )
        }
    ) { padding ->
        ProxmarkNavGraph(
            navController = navController,
            uiState = uiState,
            viewModel = viewModel,
            onShowSheet = { sheet -> currentSheet = sheet }
        )
        
        // Sheet overlay
        currentSheet?.let { sheet ->
            ModalBottomSheet(
                onDismissRequest = { currentSheet = null },
                sheetState = sheetState
            ) {
                SheetContentRouter(
                    content = sheet,
                    uiState = uiState,
                    onAction = { action -> /* handle */ },
                    onDismiss = { currentSheet = null }
                )
            }
        }
    }
}
```

---

## 9. Gesture Implementation

### Swipe Navigation (Pager)
```kotlin
// ui/components/SwipeableBottomNav.kt
@Composable
fun SwipeableBottomNav(
    navController: NavHostController,
    screens: List<Screen> = listOf(
        Screen.Dashboard, Screen.Scan, Screen.Terminal, Screen.Tools, Screen.Settings
    )
) {
    val pagerState = rememberPagerState(pageCount = { screens.size })
    val currentRoute by navController.currentBackStackEntryAsState()
    val currentIndex = screens.indexOfFirst { it.route == currentRoute?.destination?.route }
    
    LaunchedEffect(pagerState.currentPage) {
        if (pagerState.currentPage != currentIndex) {
            navController.navigate(screens[pagerState.currentPage].route) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
                launchSingleTop = true
            }
        }
    }
    
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize()
    ) { page ->
        // Screen content
    }
}
```

### Long Press Detection
```kotlin
// ui/components/LongPressCard.kt
@Composable
fun LongPressFeatureCard(
    feature: ProxmarkFeature,
    onClick: () -> Unit,
    onLongPress: () -> Unit
) {
    Card(
        modifier = Modifier
            .pointerInput(Unit) {
                detectTapGestures(
                    onTap = { onClick() },
                    onLongPress = { onLongPress() }
                )
            }
    ) {
        // Card content
    }
}
```

---

## 10. Accessibility Considerations

- **TalkBack:** All gestures have button alternatives
- **Color Blind:** Icons + labels, not color alone
- **Large Text:** Scales with system font size
- **Screen Readers:** Semantic descriptions on all interactive elements
- **Haptic Feedback:** Confirm long-press, scan start/stop, errors

---

## 11. Navigation Analytics Events

Track key navigation patterns for UX optimization:

```kotlin
sealed class NavEvent {
    data class ScreenView(val route: String, val source: String) : NavEvent()
    data class DeepLinkOpen(val uri: String) : NavEvent()
    data class GestureUsed(val type: String, val screen: String) : NavEvent()
    data class FABPress(val action: String, val screen: String) : NavEvent()
    data class SheetOpen(val sheetType: String, val trigger: String) : NavEvent()
}
```

---

## 12. Migration Plan from Current Architecture

### Current State
- Single `DashboardScreen.kt` with all UI
- No navigation component
- Manual state hoisting
- All features in one screen

### Migration Steps

**Phase 1: Add Navigation Foundation**
1. Add navigation-compose dependency
2. Create `NavGraph.kt` and `Screen` sealed class
3. Wrap existing `DashboardScreen` in NavHost
4. Add bottom navigation bar (no-op initially)

**Phase 2: Extract Screens**
1. Extract terminal UI → `TerminalScreen.kt`
2. Extract scan controls → `ScanScreen.kt`
3. Extract feature catalog → `ToolsScreen.kt`
4. Create `SettingsScreen.kt` (new)
5. Refactor `DashboardScreen` to show overview only

**Phase 3: Add Navigation Logic**
1. Implement bottom nav routing
2. Add deep link handlers
3. Implement FAB per-screen behavior
4. Add back stack management

**Phase 4: Enhance UX**
1. Add bottom sheets
2. Implement gesture support (optional)
3. Add entry point shortcuts
4. Create widgets (optional)

**Phase 5: Test & Polish**
1. Test all navigation flows
2. Verify back stack behavior
3. Test deep links
4. Accessibility audit

---

## 13. ASCII Navigation Flow Diagram

```
┌─────────────────────────────────────────────────────────────────┐
│                         APP LAUNCH                              │
│                              │                                   │
│                        MainActivity                              │
│                              │                                   │
│                      ┌───────┴───────┐                          │
│                      │  NavHost      │                          │
│                      │  (startDest)  │                          │
│                      └───────┬───────┘                          │
│                              │                                   │
│                              ▼                                   │
│                    ┌─────────────────┐                          │
│                    │   Dashboard     │◄──────────┐              │
│                    │   (Default)     │           │              │
│                    └────┬───┬────┬──┘           │              │
│                         │   │    │               │              │
│        ┌────────────────┘   │    └──────────┐   │              │
│        │                    │               │   │              │
│        ▼                    ▼               ▼   │              │
│   ┌─────────┐        ┌──────────┐    ┌─────────┐              │
│   │  Scan   │───────▶│ Terminal │◄───│  Tools  │              │
│   │ Screen  │        │  Screen  │    │ Screen  │              │
│   └────┬────┘        └─────┬────┘    └────┬────┘              │
│        │                   │              │                    │
│        │                   │              │                    │
│        │     ┌─────────────┴──────────────┘                    │
│        │     │                                                 │
│        │     ▼                                                 │
│        │  ┌──────────┐                                         │
│        │  │ Settings │                                         │
│        │  │  Screen  │                                         │
│        │  └──────────┘                                         │
│        │                                                       │
│        │  [DETAIL SCREENS - Overlay Navigation]               │
│        │                                                       │
│        ├──────────────┬──────────────┬──────────────┐         │
│        │              │              │              │         │
│        ▼              ▼              ▼              ▼         │
│   ┌────────┐    ┌─────────┐   ┌─────────┐   ┌──────────┐    │
│   │  Tag   │    │ Feature │   │  Dump   │   │ Workflow │    │
│   │ Detail │    │ Detail  │   │ Viewer  │   │  Viewer  │    │
│   └────────┘    └─────────┘   └─────────┘   └──────────┘    │
│        │              │              │              │         │
│        └──────────────┴──────────────┴──────────────┘         │
│                       │                                        │
│                       ▼                                        │
│              [Back to previous screen]                         │
│                                                                │
│  [MODAL SHEETS - Non-navigation overlays]                      │
│                                                                │
│  ScanOptions │ TagActions │ FeatureParams │ CommandTemplates  │
│                                                                │
└─────────────────────────────────────────────────────────────────┘

BOTTOM NAV FLOW (Left-Right Swipe):
Dashboard ⇄ Scan ⇄ Terminal ⇄ Tools ⇄ Settings ⇄ (wrap to Dashboard)

DEEP LINK ENTRY:
proxmark3://scan?auto=true
    └─▶ MainActivity catches intent
         └─▶ NavController.handleDeepLink()
              └─▶ Navigate to Scan + start auto-scan

BACK STACK EXAMPLE (Feature execution):
Tools > Mifare > Clone Card > [Execute]
  └─▶ Navigate to Terminal (addToBackStack)
       └─▶ Show live output
            └─▶ [Back] returns to Tools/Mifare
```

---

## 14. Implementation Checklist

- [ ] Add navigation-compose dependency
- [ ] Create `Screen` sealed class
- [ ] Create `NavGraph.kt`
- [ ] Extract `ScanScreen.kt` from Dashboard
- [ ] Extract `TerminalScreen.kt` from Dashboard
- [ ] Extract `ToolsScreen.kt` from Dashboard
- [ ] Create `SettingsScreen.kt`
- [ ] Refactor `DashboardScreen.kt` to overview
- [ ] Implement `BottomNavigationBar.kt`
- [ ] Implement `AdaptiveFab.kt`
- [ ] Add deep link intent filters to manifest
- [ ] Create `BackStackManager.kt`
- [ ] Implement `TagDetailScreen.kt`
- [ ] Implement `FeatureDetailScreen.kt`
- [ ] Create modal bottom sheet infrastructure
- [ ] Implement `ScanOptionsSheet.kt`
- [ ] Implement `TagActionsSheet.kt`
- [ ] Implement `FeatureParameterSheet.kt`
- [ ] Add gesture support (HorizontalPager for swipe)
- [ ] Add long-press gesture detection
- [ ] Create app shortcuts (shortcuts.xml)
- [ ] Add TalkBack descriptions
- [ ] Test all navigation flows
- [ ] Test deep links
- [ ] Test back stack behavior
- [ ] Performance audit (navigation transitions)

---

## 15. Performance Considerations

- **Lazy Loading:** Tools screen uses LazyColumn for feature catalog
- **State Preservation:** Use `restoreState = true` in bottom nav
- **Avoid Recomposition:** Split screens into smaller composables
- **Predictive Back:** Support Android 14 gesture animations
- **Transition Animations:** Use Material Motion for screen transitions
- **Memory:** Limit terminal buffer to 1000 lines (configurable)

---

## Appendix A: Material 3 Navigation Patterns

This design follows Material 3 guidelines:
- Bottom navigation for 3-5 top-level destinations
- FAB for primary screen action
- Bottom sheets for secondary tasks
- Predictive back gesture support
- Color-coded navigation states

**Reference:** https://m3.material.io/components/navigation-bar

---

## Appendix B: Competitor Analysis

Analyzed UX patterns from:
- **nRF Connect:** Category tabs + FAB for scan
- **RFID Tools:** Bottom nav with 4 sections
- **TagInfo:** Scan-first UX with immediate detail view
- **PM3 Easy:** Terminal-centric with feature shortcuts

**Key Takeaway:** Users prefer immediate scan access + clear tool organization over raw terminal.

---

**End of Navigation Architecture Design**
