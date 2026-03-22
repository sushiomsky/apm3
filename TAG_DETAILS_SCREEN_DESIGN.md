# Tag Details Screen - UX/UI Design Document

## Overview
A clean, card-based layout for viewing and manipulating RFID tag data with emphasis on visual hierarchy, data clarity, and actionable insights.

---

## 1. Screen Layout Wireframe

```
┌─────────────────────────────────────────┐
│  ← Tag Details              [•••] [✕]   │  ← Top bar with back, menu, close
├─────────────────────────────────────────┤
│                                          │
│  ┌────────────────────────────────────┐ │
│  │ 🏷️  UID CARD                       │ │  ← Hero card: UID + quick copy
│  │  F4A2C8B1                          │ │
│  │  [📋 Copy] [📤 Share] [⭐ Save]   │ │
│  └────────────────────────────────────┘ │
│                                          │
│  ┌────────────────────────────────────┐ │
│  │ 📊 TAG INFO                        │ │  ← Metadata card
│  │  Type: MIFARE Classic 1K           │ │
│  │  Protocol: ISO/IEC 14443A          │ │
│  │  ATQA: 0004  SAK: 08               │ │
│  │  Discovered: 2 hours ago           │ │
│  └────────────────────────────────────┘ │
│                                          │
│  ┌────────────────────────────────────┐ │
│  │ ⚡ QUICK ACTIONS                   │ │  ← Action button grid
│  │  [🔍 Read] [✏️ Write] [📥 Clone] │ │
│  │  [💾 Dump] [📤 Export] [🔄 Compare]│ │
│  └────────────────────────────────────┘ │
│                                          │
│  ┌────────────────────────────────────┐ │
│  │ 💾 MEMORY MAP    [Raw|Decoded] ▼  │ │  ← Toggle between views
│  │                                    │ │
│  │  ╔═ Sector 0 ═══════════════════╗ │ │  ← Expandable sectors
│  │  ║ 📍 Block 0 (Mfr)   [ACTIVE]  ║ │ │
│  │  ║ F4 A2 C8 B1 │ 44 08 04 00   ║ │ │
│  │  ║ ASCII: ô¢È±.D....           ║ │ │
│  │  ╚═══════════════════════════════╝ │ │
│  │                                    │ │
│  │  ╔═ Sector 1 ═══════════════════╗ │ │
│  │  ║ 📦 3 blocks · 48 bytes       ║ │ │
│  │  ║ [🔓 Default Keys] [Tap expand]║ │ │
│  │  ╚═══════════════════════════════╝ │ │
│  │                                    │ │
│  │  ╔═ Sector 3 (Trailer) ═════════╗ │ │
│  │  ║ 🔑 Key A: FFFFFFFFFFFF       ║ │ │
│  │  ║ 🔑 Key B: FFFFFFFFFFFF       ║ │ │
│  │  ║ 🔐 Access: FF0780            ║ │ │
│  │  ║ [R:✓ W:✗]                    ║ │ │  ← Permission badges
│  │  ╚═══════════════════════════════╝ │ │
│  └────────────────────────────────────┘ │
│                                          │
│  ┌────────────────────────────────────┐ │
│  │ 📝 NOTES                           │ │  ← User notes section
│  │  "Office access card - main door" │ │
│  │  [Edit]                            │ │
│  └────────────────────────────────────┘ │
│                                          │
└─────────────────────────────────────────┘
```

---

## 2. UID Display Design

### Visual Hierarchy
- **Size**: Typography.headlineLarge (32sp)
- **Font**: Monospace (JetBrains Mono / Roboto Mono)
- **Color**: Primary color for emphasis
- **Container**: Elevated card with gradient background

### Layout
```kotlin
┌───────────────────────────────────────────┐
│                                            │
│  🏷️  UNIQUE IDENTIFIER                    │  ← Label (caption)
│                                            │
│  F4 A2 C8 B1                               │  ← UID (headline, spaced)
│  ▔▔▔▔▔▔▔▔▔▔▔                              │  ← Subtle underline
│                                            │
│  [📋 Copy to Clipboard]  [📤 Share QR]    │  ← Action buttons
│                                            │
└───────────────────────────────────────────┘
```

### Features
- Auto-spacing every 2 or 4 hex chars for readability
- Haptic feedback on copy
- Toast confirmation: "UID copied: F4A2C8B1"
- Long-press to show QR code
- Swipe to reveal full hex dump

---

## 3. Memory Block Visualization Design

### Grid Layout (Compact View)
```
┌─────────────────────────────────────────┐
│ MEMORY MAP              [Grid | List]   │
│                                          │
│  Sector 0 ──────────────────────────┐   │
│  [00][01][02][03]                    │   │  ← Block chips
│  █████████░░░  75% used              │   │  ← Usage bar
│  🔑 Custom keys                      │   │  ← Key indicator
│  └─────────────────────────────────┘    │
│                                          │
│  Sector 1 ──────────────────────────┐   │
│  [04][05][06][07]                    │   │
│  ░░░░░░░░░░░░  0% used (empty)       │   │
│  🔓 Default keys                     │   │
│  └─────────────────────────────────┘    │
└─────────────────────────────────────────┘
```

### Hexdump View (Detail View)
```
┌─────────────────────────────────────────┐
│  Block 0 (Manufacturer)    [🔒 Read-Only]│
│  ┌───────────────────────────────────┐  │
│  │ 00: F4 A2 C8 B1 | 44 08 04 00  │  │  │  ← Hex bytes
│  │ 08: 00 00 00 00 | 00 00 00 00  │  │  │
│  │ ASCII: ô¢È±.D...........       │  │  │  ← ASCII preview
│  └───────────────────────────────────┘  │
│  [📋 Copy]  [🔍 Analyze]               │
└─────────────────────────────────────────┘
```

### Color Coding System
| State | Color | Icon | Description |
|-------|-------|------|-------------|
| **Active** | Primary/Blue | 🔵 | Contains non-zero data |
| **Empty** | Surface/Gray | ⚪ | All zeros |
| **Trailer** | Tertiary/Purple | 🟣 | Sector trailer block |
| **Read-Only** | Warning/Orange | 🔒 | Manufacturer block |
| **Modified** | Error/Red | 🔴 | Changed from default |
| **Default Keys** | Outline/Gray | 🔓 | Using FFFFFFFFFFFF |
| **Custom Keys** | Success/Green | 🔑 | Non-default keys |

---

## 4. Action Button Bar Design

### Grid Layout (2x3)
```
┌────────────────────────────────────────┐
│  ⚡ QUICK ACTIONS                      │
│                                         │
│  ┌────────────┐  ┌────────────┐       │
│  │ 🔍 READ    │  │ ✏️ WRITE   │       │  ← Primary actions
│  │ Full scan  │  │ Modify data │       │
│  └────────────┘  └────────────┘       │
│                                         │
│  ┌────────────┐  ┌────────────┐       │
│  │ 📥 CLONE   │  │ 💾 DUMP    │       │  ← Secondary actions
│  │ Copy to T55 │  │ Save .bin  │       │
│  └────────────┘  └────────────┘       │
│                                         │
│  ┌────────────┐  ┌────────────┐       │
│  │ 📤 EXPORT  │  │ 🔄 COMPARE │       │  ← Utility actions
│  │ File/QR    │  │ Diff tags  │       │
│  └────────────┘  └────────────┘       │
└────────────────────────────────────────┘
```

### Button States
- **Idle**: Surface color, full icon + label
- **Running**: Progress indicator, "Reading..."
- **Success**: Green checkmark pulse, then reset
- **Error**: Red X flash, tooltip with error
- **Disabled**: Grayed out, tooltip: "Connect device first"

### Safety Levels
```kotlin
// Inherited from feature system
SAFE → One-tap execution (green)
ADVANCED → Confirmation dialog (orange)
DESTRUCTIVE → Red warning + "I understand" checkbox
```

---

## 5. Sector/Block Detail View Design

### Expandable Accordion
```
┌──────────────────────────────────────────┐
│ ▶ Sector 0                    [Active]   │  ← Collapsed
│   4 blocks · 64 bytes · Custom keys      │
└──────────────────────────────────────────┘
       ↓ Tap to expand
┌──────────────────────────────────────────┐
│ ▼ Sector 0                    [Active]   │  ← Expanded
│                                           │
│   ┌─────────────────────────────────┐    │
│   │ Block 0 (Manufacturer)   [🔒]  │    │
│   │ F4 A2 C8 B1 44 08 04 00         │    │
│   │ 00 00 00 00 00 00 00 00         │    │
│   │ ASCII: ô¢È±.D...                │    │
│   └─────────────────────────────────┘    │
│                                           │
│   ┌─────────────────────────────────┐    │
│   │ Block 1 (Data)            [📝]  │    │
│   │ 00 00 00 00 00 00 00 00         │    │
│   │ 00 00 00 00 00 00 00 00         │    │
│   │ [Empty - tap to write]          │    │
│   └─────────────────────────────────┘    │
│                                           │
│   ┌─────────────────────────────────┐    │
│   │ Block 3 (Trailer)         [🔑]  │    │
│   │                                  │    │
│   │ 🔑 Key A: FFFFFFFFFFFF [📋]     │    │
│   │ 🔐 Access: FF0780       [?]     │    │
│   │ 🔑 Key B: FFFFFFFFFFFF [📋]     │    │
│   │                                  │    │
│   │ Permissions:                     │    │
│   │ • Read: ✓ Allowed                │    │
│   │ • Write: ✗ Protected             │    │
│   │ • Increment: ✓ Allowed           │    │
│   │ • Decrement: ✗ Protected         │    │
│   └─────────────────────────────────┘    │
└──────────────────────────────────────────┘
```

### Interactive Features
- **Tap block**: Expand hex view
- **Long-press block**: Copy hex data
- **Swipe block**: Quick actions (read/write/analyze)
- **Tap key**: Copy to clipboard
- **Tap access bits**: Show decoded permissions

---

## 6. Raw vs Decoded Toggle Design

### Segmented Control
```
┌─────────────────────────────────────┐
│  [  Raw Hex  |  Decoded  ]  ▼      │  ← Tab bar
└─────────────────────────────────────┘
```

### Raw View
- Hexdump with addresses: `00: F4 A2 C8 B1`
- ASCII column on right
- Byte-level selection
- Export as .bin / .hex

### Decoded View
- Sector/block structure
- Key visualization
- Access permissions in plain English
- Data interpretation hints:
  - "Possible UID: F4A2C8B1"
  - "Likely text data: HELLO"
  - "Empty block (all zeros)"
  - "Default transport keys"

### Toggle States
```kotlin
enum class ViewMode {
    RAW,        // Raw hex bytes
    DECODED,    // Structured view
    HYBRID      // Both side-by-side
}
```

---

## 7. Export Options Design

### Bottom Sheet Menu
```
┌─────────────────────────────────────────┐
│  📤 Export Tag Data                     │
│  ─────────────────────────────────────  │
│                                          │
│  [💾] Save as Binary (.bin)             │  ← Full memory dump
│  [📄] Save as Hex Text (.txt)           │  ← Human-readable
│  [📋] Copy to Clipboard                 │  ← Quick share
│  [📱] Share QR Code                     │  ← Scan on another device
│  [🔗] Generate Deep Link                │  ← URL with encoded data
│  [📧] Send via Email                    │  ← Attachment
│  [📂] Export to Proxmark3 format        │  ← .eml/.mfd
│  [🖨️] Print Hex Dump                   │  ← PDF/Print
│                                          │
│  Format options:                         │
│  ☑ Include UID                          │
│  ☑ Include keys (if available)          │
│  ☑ Include access bits                  │
│  ☐ Include ASCII representation         │
│  ☐ Include sector summaries             │
│                                          │
│  [Cancel]                 [Export]      │
└─────────────────────────────────────────┘
```

### QR Code Generation
```
┌───────────────────────────┐
│   📱 Scan to Load Tag     │
│                            │
│   ┌─────────────────────┐ │
│   │                     │ │
│   │  ██████  ██  ████  │ │  ← QR encoding UID + type
│   │  ██  ██  ██  ██    │ │
│   │  ██████  ██  ████  │ │
│   │                     │ │
│   └─────────────────────┘ │
│                            │
│  UID: F4A2C8B1             │
│  Type: MIFARE Classic 1K   │
│                            │
│  [📋 Copy Link] [💾 Save] │
└───────────────────────────┘
```

---

## 8. Empty/Loading/Error States

### Empty State (No Data)
```
┌─────────────────────────────────────┐
│                                      │
│          🏷️                         │
│                                      │
│     No Tag Data Available            │
│                                      │
│  Scan a tag or load a dump file     │
│  to view details                     │
│                                      │
│  [🔍 Start Scan] [📂 Load File]     │
│                                      │
└─────────────────────────────────────┘
```

### Loading State
```
┌─────────────────────────────────────┐
│                                      │
│          ⏳                          │
│     [Progress spinner]               │
│                                      │
│     Reading tag memory...            │
│     Block 12 of 64                   │
│                                      │
│     ████████░░░░  60%                │  ← Progress bar
│                                      │
│          [Cancel]                    │
│                                      │
└─────────────────────────────────────┘
```

### Error State
```
┌─────────────────────────────────────┐
│                                      │
│          ⚠️                          │
│                                      │
│     Failed to Read Tag               │
│                                      │
│  Error: Authentication failed on     │
│  sector 3. Default keys don't work.  │
│                                      │
│  [🔄 Retry] [🔑 Try Keys] [📋 Log]  │
│                                      │
└─────────────────────────────────────┘
```

### Partial Read State
```
┌─────────────────────────────────────┐
│  ⚠️ Partial Read (8 of 16 sectors)  │
│                                      │
│  Some sectors couldn't be read:      │
│  • Sector 3: Auth failed             │
│  • Sector 7: Auth failed             │
│  • Sector 15: CRC error              │
│                                      │
│  [🔑 Dictionary Attack] [Save Anyway]│
└─────────────────────────────────────┘
```

---

## 9. Compose Component Structure

### Component Hierarchy
```
TagDetailsScreen
├── TagDetailsScaffold
│   ├── TopBar
│   │   ├── BackButton
│   │   ├── Title
│   │   └── MenuButton (overflow)
│   └── Content (LazyColumn)
│       ├── UidCard
│       │   ├── UidDisplay (large text)
│       │   ├── CopyButton
│       │   └── ShareButton
│       ├── TagInfoCard
│       │   ├── TypeBadge
│       │   ├── ProtocolBadge
│       │   └── MetadataFields
│       ├── QuickActionsCard
│       │   └── ActionButtonGrid (2x3)
│       ├── MemoryMapCard
│       │   ├── ViewToggle (Raw/Decoded)
│       │   └── SectorList
│       │       └── SectorCard (expandable)
│       │           ├── SectorHeader
│       │           └── BlockList (when expanded)
│       │               ├── DataBlock
│       │               └── TrailerBlock
│       └── NotesCard
│           ├── NoteText
│           └── EditButton
└── ExportBottomSheet (modal)
```

### State Management
```kotlin
data class TagDetailsUiState(
    val tag: TagInfo,
    val memoryDump: MifareClassicDump? = null,
    val viewMode: ViewMode = ViewMode.DECODED,
    val expandedSectors: Set<Int> = emptySet(),
    val selectedBlocks: Set<Int> = emptySet(),
    val isLoading: Boolean = false,
    val loadingProgress: Float = 0f,
    val error: String? = null,
    val showExportSheet: Boolean = false,
    val showQrCode: Boolean = false,
    val clipboardMessage: String? = null
)

sealed class TagDetailsAction {
    data class ToggleSector(val sectorNum: Int) : TagDetailsAction()
    data class SelectBlock(val blockNum: Int) : TagDetailsAction()
    data class CopyData(val data: String) : TagDetailsAction()
    object ToggleViewMode : TagDetailsAction()
    object ShowExportSheet : TagDetailsAction()
    object DismissExportSheet : TagDetailsAction()
    data class ExportAs(val format: ExportFormat) : TagDetailsAction()
    data class ExecuteAction(val action: TagAction) : TagDetailsAction()
}

enum class TagAction {
    READ_FULL,
    WRITE_BLOCK,
    CLONE_TO_T55XX,
    DUMP_TO_FILE,
    COMPARE_WITH_OTHER
}
```

---

## 10. Example Compose Code for Memory Visualization

```kotlin
package com.example.proxmark.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import com.example.proxmark.mifare.MifareBlock
import com.example.proxmark.mifare.MifareClassicDump
import com.example.proxmark.mifare.MifareSector

@Composable
fun TagMemoryVisualization(
    dump: MifareClassicDump,
    viewMode: ViewMode,
    expandedSectors: Set<Int>,
    onToggleSector: (Int) -> Unit,
    onBlockClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val clipboardManager = LocalClipboardManager.current
    var showToast by remember { mutableStateOf<String?>(null) }

    Column(modifier = modifier.fillMaxWidth()) {
        // Header with stats
        MemoryMapHeader(dump = dump, viewMode = viewMode)

        Spacer(modifier = Modifier.height(12.dp))

        // Sector list
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(
                items = dump.sectors,
                key = { sector -> sector.number }
            ) { sector ->
                SectorVisualizationCard(
                    sector = sector,
                    isExpanded = sector.number in expandedSectors,
                    viewMode = viewMode,
                    onToggle = { onToggleSector(sector.number) },
                    onBlockClick = onBlockClick,
                    onCopyKey = { key ->
                        clipboardManager.setText(AnnotatedString(key))
                        showToast = "Key copied: $key"
                    }
                )
            }
        }
    }

    // Toast notification
    showToast?.let { message ->
        LaunchedEffect(message) {
            kotlinx.coroutines.delay(2000)
            showToast = null
        }
        Snackbar(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(message)
        }
    }
}

@Composable
private fun MemoryMapHeader(
    dump: MifareClassicDump,
    viewMode: ViewMode
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Memory Map",
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    buildString {
                        append("${dump.sectors.size} sectors · ")
                        append("${dump.getActiveSectors().size} active · ")
                        append("${dump.getCustomKeySectors().size} custom keys")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                )
            }
            
            // View mode badge
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.primary
            ) {
                Text(
                    text = viewMode.name,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SectorVisualizationCard(
    sector: MifareSector,
    isExpanded: Boolean,
    viewMode: ViewMode,
    onToggle: () -> Unit,
    onBlockClick: (Int) -> Unit,
    onCopyKey: (String) -> Unit
) {
    val isActive = sector.isActive()
    val hasCustomKeys = !sector.isDefaultKeyA || !sector.isDefaultKeyB

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = when {
                hasCustomKeys -> MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.3f)
                isActive -> MaterialTheme.colorScheme.secondaryContainer
                else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { onToggle() }
                .padding(12.dp)
        ) {
            // Sector header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Sector ${sector.number}",
                        style = MaterialTheme.typography.titleSmall,
                        fontFamily = FontFamily.Monospace
                    )
                    
                    // Status badges
                    if (hasCustomKeys) {
                        Badge(containerColor = MaterialTheme.colorScheme.error) {
                            Icon(
                                Icons.Default.Key,
                                contentDescription = "Custom keys",
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                    
                    if (isActive) {
                        Badge(containerColor = MaterialTheme.colorScheme.primary) {
                            Text("ACTIVE", style = MaterialTheme.typography.labelSmall)
                        }
                    }
                }

                Icon(
                    imageVector = if (isExpanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = if (isExpanded) "Collapse" else "Expand"
                )
            }

            // Sector summary (collapsed)
            if (!isExpanded) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    buildString {
                        append("${sector.blocks.size} blocks · ")
                        append("${sector.blocks.size * 16} bytes")
                        if (hasCustomKeys) append(" · 🔑 Custom keys")
                        else append(" · 🔓 Default keys")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            }

            // Expanded content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    sector.blocks.forEachIndexed { idx, block ->
                        if (idx == 3) {
                            // Trailer block (special rendering)
                            TrailerBlockCard(
                                sector = sector,
                                onCopyKey = onCopyKey
                            )
                        } else {
                            // Data block
                            DataBlockCard(
                                block = block,
                                isManufacturer = block.number == 0,
                                viewMode = viewMode,
                                onClick = { onBlockClick(block.number) }
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DataBlockCard(
    block: MifareBlock,
    isManufacturer: Boolean,
    viewMode: ViewMode,
    onClick: () -> Unit
) {
    val isEmpty = block.data.all { it == 0.toByte() }
    
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(8.dp),
        color = when {
            isManufacturer -> MaterialTheme.colorScheme.tertiaryContainer
            isEmpty -> MaterialTheme.colorScheme.surface
            else -> MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.3f)
        }
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Block ${block.number}",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace
                    )
                    
                    if (isManufacturer) {
                        Icon(
                            Icons.Default.Lock,
                            contentDescription = "Read-only",
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                        Text(
                            "Manufacturer",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }

                if (isEmpty) {
                    Text(
                        "Empty",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            when (viewMode) {
                ViewMode.RAW -> {
                    Text(
                        block.toHexString(),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = if (isEmpty) {
                            MaterialTheme.colorScheme.outline
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        }
                    )
                }
                ViewMode.DECODED -> {
                    Text(
                        block.toHexString(),
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace
                    )
                    Text(
                        "ASCII: ${block.toAsciiString()}",
                        style = MaterialTheme.typography.labelSmall,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.outline
                    )
                }
                ViewMode.HYBRID -> {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            block.toHexString(),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            block.toAsciiString(),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TrailerBlockCard(
    sector: MifareSector,
    onCopyKey: (String) -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.tertiaryContainer
    ) {
        Column(
            modifier = Modifier.padding(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "Block ${sector.blocks[3].number}",
                        style = MaterialTheme.typography.labelMedium,
                        fontFamily = FontFamily.Monospace
                    )
                    Icon(
                        Icons.Default.Key,
                        contentDescription = "Trailer",
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        "Trailer",
                        style = MaterialTheme.typography.labelSmall
                    )
                }

                if (!sector.isDefaultKeyA || !sector.isDefaultKeyB) {
                    Badge(containerColor = MaterialTheme.colorScheme.error) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = "Custom keys",
                            modifier = Modifier.size(12.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Key A
            KeyRow(
                label = "Key A",
                key = sector.keyA,
                isDefault = sector.isDefaultKeyA,
                onCopy = { onCopyKey(sector.keyA) }
            )

            Spacer(modifier = Modifier.height(4.dp))

            // Access bits
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Access",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
                )
                Text(
                    sector.accessBits,
                    style = MaterialTheme.typography.bodySmall,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Key B
            KeyRow(
                label = "Key B",
                key = sector.keyB,
                isDefault = sector.isDefaultKeyB,
                onCopy = { onCopyKey(sector.keyB) }
            )

            // Permission summary
            Spacer(modifier = Modifier.height(8.dp))
            PermissionChips()
        }
    }
}

@Composable
private fun KeyRow(
    label: String,
    key: String,
    isDefault: Boolean,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onTertiaryContainer.copy(alpha = 0.7f)
        )
        
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                key,
                style = MaterialTheme.typography.bodySmall,
                fontFamily = FontFamily.Monospace,
                color = if (isDefault) {
                    MaterialTheme.colorScheme.outline
                } else {
                    MaterialTheme.colorScheme.error
                }
            )
            
            IconButton(
                onClick = onCopy,
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    Icons.Default.ContentCopy,
                    contentDescription = "Copy key",
                    modifier = Modifier.size(14.dp)
                )
            }
        }
    }
}

@Composable
private fun PermissionChips() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        PermissionChip("R: ✓", true)
        PermissionChip("W: ✗", false)
        PermissionChip("Inc: ✓", true)
        PermissionChip("Dec: ✗", false)
    }
}

@Composable
private fun PermissionChip(text: String, allowed: Boolean) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = if (allowed) {
            MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
        } else {
            MaterialTheme.colorScheme.error.copy(alpha = 0.2f)
        }
    ) {
        Text(
            text,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            style = MaterialTheme.typography.labelSmall,
            color = if (allowed) {
                MaterialTheme.colorScheme.primary
            } else {
                MaterialTheme.colorScheme.error
            }
        )
    }
}

enum class ViewMode {
    RAW,      // Hex only
    DECODED,  // Hex + ASCII
    HYBRID    // Side-by-side
}
```

---

## Design Principles Summary

### 1. **Visual Hierarchy**
- UID is the hero element (largest, most prominent)
- Actions are secondary (medium emphasis)
- Technical details tertiary (collapsed by default)

### 2. **Progressive Disclosure**
- Start with summary view
- Expand sectors on demand
- Show tooltips for complex concepts (access bits)
- Hide empty/default sectors by default (with toggle to show all)

### 3. **Color Semantics**
- Blue/Primary → Active data
- Gray → Empty/default
- Red/Error → Custom keys (security attention)
- Green/Success → Successful operations
- Orange/Warning → Read-only/protected

### 4. **Interaction Patterns**
- Tap → Toggle expand
- Long-press → Copy
- Swipe → Quick actions
- Pull-to-refresh → Re-read tag

### 5. **Accessibility**
- Semantic content descriptions
- Sufficient color contrast
- Touch targets ≥ 48dp
- Screen reader friendly labels
- Haptic feedback for key actions

### 6. **Performance**
- Lazy loading for large dumps
- Virtualized lists for sectors
- Debounced search
- Cached hex rendering

---

## Implementation Roadmap

### Phase 1: Core Layout (Week 1)
- [ ] Create `TagDetailsScreen.kt`
- [ ] Implement `UidCard` component
- [ ] Implement `TagInfoCard` component
- [ ] Add basic navigation from tag list

### Phase 2: Memory Visualization (Week 2)
- [ ] Implement `MemoryMapCard`
- [ ] Create `SectorVisualizationCard`
- [ ] Add expand/collapse animations
- [ ] Implement view mode toggle

### Phase 3: Actions (Week 3)
- [ ] Implement `QuickActionsCard`
- [ ] Wire up read/write operations
- [ ] Add loading states
- [ ] Implement error handling

### Phase 4: Export & Share (Week 4)
- [ ] Create export bottom sheet
- [ ] Implement QR code generation
- [ ] Add file export (binary, hex, JSON)
- [ ] Implement share functionality

### Phase 5: Polish (Week 5)
- [ ] Add animations and transitions
- [ ] Implement haptic feedback
- [ ] Add accessibility features
- [ ] Write unit tests
- [ ] Conduct UX testing

---

## Files to Create

1. `ui/screens/TagDetailsScreen.kt` - Main screen composable
2. `ui/components/UidCard.kt` - UID display component
3. `ui/components/TagMemoryVisualization.kt` - Memory map component (included above)
4. `ui/components/QuickActionsGrid.kt` - Action button grid
5. `ui/components/ExportBottomSheet.kt` - Export options
6. `ui/components/QrCodeDialog.kt` - QR code display
7. `viewmodel/TagDetailsViewModel.kt` - Screen state management
8. `viewmodel/TagDetailsUiState.kt` - UI state data class

---

## Mockup Assets Needed

- Icon: 🏷️ Tag icon (Material Symbols)
- Icon: 🔑 Key icon (for custom keys)
- Icon: 🔓 Unlocked icon (for default keys)
- Icon: 🔒 Lock icon (for read-only)
- Icon: ⚡ Lightning icon (for actions)
- Color palette: Define semantic colors in Theme.kt
- Typography: Ensure monospace font is configured

---

**END OF DESIGN DOCUMENT**
