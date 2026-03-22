# Live Terminal View - UX/UI Design Document

## Executive Summary

This document defines a powerful terminal interface for the Proxmark3 Android control app that combines the flexibility of a full CLI with modern UX enhancements. The terminal provides real-time command execution, syntax highlighting, intelligent autocomplete, command history management, and efficient rendering for high-frequency output.

---

## 1. Terminal Layout & Component Structure

### 1.1 Overall Layout Hierarchy

```
┌─────────────────────────────────────────────────────────┐
│  TopBar                                                  │
│  [Terminal] [View Toggle] [Export] [Clear]             │
├─────────────────────────────────────────────────────────┤
│  PinnedCommandsBar (collapsible)                        │
│  [hf search] [lf search] [hw status] [+]               │
├─────────────────────────────────────────────────────────┤
│                                                         │
│  TerminalOutputArea (LazyColumn)                       │
│  > hf search                                           │
│  [#] Searching for known HF tags...                    │
│  [#] ISO14443-A tag found                              │
│  [#] UID: 04:9A:3C:2A                                  │
│  │                                                      │
│  │  [Auto-scroll locked] 🔒                            │
│  │  Tap to unlock                                      │
│  │                                                      │
│  ▼ (2500 more lines...)                                │
│                                                         │
├─────────────────────────────────────────────────────────┤
│  InputField & Autocomplete                              │
│  ┌──────────────────────────────────────────┐          │
│  │ hf mf rdbl                              │ [Send]   │
│  └──────────────────────────────────────────┘          │
│  ┌──────────────────────────────────────────┐          │
│  │ ✓ hf mf rdbl                             │ (fuzzy) │
│  │   hf mf rdsc                             │          │
│  │   hf mf restore                          │          │
│  └──────────────────────────────────────────┘          │
└─────────────────────────────────────────────────────────┘
```

### 1.2 Component Breakdown

#### TopBar
- **Terminal tab indicator** (always visible when in terminal mode)
- **View toggle button**: Switch between Raw CLI / Structured output
- **Export button**: Export session log (TXT, JSON formats)
- **Clear button**: Clear terminal output (with confirmation)

#### PinnedCommandsBar
- **Horizontal scrollable row** of chips representing pinned commands
- **Add button (+)**: Pin current command or select from favorites
- **Long-press to unpin** or reorder
- **Collapsible**: Swipe down to hide, swipe up to show
- **Persisted** across sessions via SettingsStore

#### TerminalOutputArea
- **LazyColumn** for efficient rendering of 10,000+ lines
- **Auto-scroll behavior**: Automatically scrolls to bottom when new output arrives
- **Manual scroll lock**: When user scrolls up, auto-scroll is disabled; floating "unlock" button appears
- **Line numbering** (optional, toggle in settings)
- **Timestamp prefix** (optional, toggle in settings)
- **Context menu**: Long-press any line to copy, search, or filter

#### InputField
- **Multi-line TextField** with monospace font
- **Up/Down arrow buttons** for command history navigation
- **Autocomplete dropdown** appears below input (fuzzy match)
- **Send button** or Enter key submits command
- **Shift+Enter** for multi-line input (advanced users)

---

## 2. Syntax Highlighting Color Scheme

### 2.1 Color Palette

#### Light Theme
```kotlin
object TerminalColors {
    // Sent commands
    val commandKeyword = Color(0xFF0066CC)      // Blue
    val commandArgument = Color(0xFF009900)     // Green
    
    // Received output
    val receivedText = Color(0xFF333333)        // Dark gray
    val receivedHighlight = Color(0xFFCC6600)   // Orange (UID, keys)
    
    // System messages
    val systemInfo = Color(0xFF666666)          // Medium gray
    
    // Errors
    val errorText = Color(0xFFCC0000)           // Red
    
    // Success indicators
    val successText = Color(0xFF009900)         // Green
    
    // Background
    val background = Color(0xFFF5F5F5)          // Light gray
    val inputBackground = Color(0xFFFFFFFF)     // White
}
```

#### Dark Theme
```kotlin
object TerminalColorsDark {
    // Sent commands
    val commandKeyword = Color(0xFF5599FF)      // Bright blue
    val commandArgument = Color(0xFF66CC66)     // Bright green
    
    // Received output
    val receivedText = Color(0xFFCCCCCC)        // Light gray
    val receivedHighlight = Color(0xFFFF9933)   // Bright orange
    
    // System messages
    val systemInfo = Color(0xFF999999)          // Medium gray
    
    // Errors
    val errorText = Color(0xFFFF4444)           // Bright red
    
    // Success indicators
    val successText = Color(0xFF66CC66)         // Bright green
    
    // Background
    val background = Color(0xFF1E1E1E)          // Dark background
    val inputBackground = Color(0xFF2D2D2D)     // Slightly lighter
}
```

### 2.2 Syntax Highlighting Rules

```kotlin
sealed class TerminalSegment {
    data class Command(val text: String) : TerminalSegment()
    data class Argument(val text: String) : TerminalSegment()
    data class Output(val text: String) : TerminalSegment()
    data class Highlight(val text: String) : TerminalSegment() // UID, keys
    data class Error(val text: String) : TerminalSegment()
    data class Success(val text: String) : TerminalSegment()
}

fun parseTerminalLine(line: TerminalLine): List<TerminalSegment> {
    return when (line.type) {
        TerminalLineType.SENT -> parseSentCommand(line.text)
        TerminalLineType.RECEIVED -> parseReceivedOutput(line.text)
        TerminalLineType.ERROR -> listOf(TerminalSegment.Error(line.text))
        TerminalLineType.SYSTEM -> listOf(TerminalSegment.Output(line.text))
    }
}

private fun parseSentCommand(text: String): List<TerminalSegment> {
    val parts = text.split(" ", limit = 2)
    return if (parts.size == 2) {
        listOf(
            TerminalSegment.Command(parts[0]),
            TerminalSegment.Argument(" " + parts[1])
        )
    } else {
        listOf(TerminalSegment.Command(text))
    }
}

private fun parseReceivedOutput(text: String): List<TerminalSegment> {
    val segments = mutableListOf<TerminalSegment>()
    
    // Highlight patterns: UID, ATQA, SAK, keys
    val uidPattern = Regex("""UID\s*:\s*([0-9A-Fa-f:]+)""")
    val keyPattern = Regex("""[0-9A-Fa-f]{12}""") // 12 hex chars = key
    
    var lastIndex = 0
    
    // Find and highlight UIDs
    uidPattern.findAll(text).forEach { match ->
        if (match.range.first > lastIndex) {
            segments.add(TerminalSegment.Output(text.substring(lastIndex, match.range.first)))
        }
        segments.add(TerminalSegment.Highlight(match.value))
        lastIndex = match.range.last + 1
    }
    
    // Add remaining text
    if (lastIndex < text.length) {
        segments.add(TerminalSegment.Output(text.substring(lastIndex)))
    }
    
    // Check for success/error indicators
    if (text.contains("[+]") || text.contains("✓") || text.contains("SUCCESS", ignoreCase = true)) {
        return listOf(TerminalSegment.Success(text))
    }
    if (text.contains("[-]") || text.contains("✗") || text.contains("FAILED", ignoreCase = true)) {
        return listOf(TerminalSegment.Error(text))
    }
    
    return segments.ifEmpty { listOf(TerminalSegment.Output(text)) }
}
```

---

## 3. Autocomplete Dropdown Design

### 3.1 Visual Design

```
┌────────────────────────────────────────────────────┐
│ hf mf rd▮                                         │
└────────────────────────────────────────────────────┘
┌────────────────────────────────────────────────────┐
│ ✓ hf mf rdbl                               [SAFE] │ ← Best match
│   Read MIFARE Classic block                       │
│                                                    │
│ • hf mf rdsc                           [ADVANCED] │
│   Read MIFARE Classic sector                      │
│                                                    │
│ • hf mf restore                      [DESTRUCTIVE] │
│   Restore MIFARE Classic dump to tag              │
│                                                    │
│ 📚 Recent: hf mf rdbl 0 A FFFFFFFFFFFF           │
└────────────────────────────────────────────────────┘
```

### 3.2 Autocomplete Features

- **Fuzzy matching**: Match anywhere in command string (e.g., "hfrd" → "hf mf rdbl")
- **Score ranking**: Best matches first, based on:
  - Prefix match (highest priority)
  - Word boundary match
  - Character sequence match
  - Recent usage frequency
- **Feature metadata display**: Title, description, safety level
- **Recent commands section**: Last 5 matching commands from history
- **Keyboard navigation**: Up/Down arrows to select, Enter to complete
- **Touch interaction**: Tap to complete
- **Max 10 results** to prevent dropdown overflow

### 3.3 Fuzzy Matching Algorithm

```kotlin
data class AutocompleteMatch(
    val command: String,
    val feature: ProxmarkFeature?,
    val score: Int,
    val matchRanges: List<IntRange>
)

fun fuzzyMatch(query: String, candidate: String): Int? {
    if (query.isEmpty()) return null
    
    val q = query.lowercase()
    val c = candidate.lowercase()
    
    // Perfect prefix match
    if (c.startsWith(q)) return 1000 + query.length * 10
    
    // Word boundary match
    val words = c.split(" ")
    if (words.any { it.startsWith(q) }) return 500 + query.length * 10
    
    // Consecutive character match
    var qIndex = 0
    var consecutiveMatches = 0
    var lastMatchIndex = -1
    
    for ((cIndex, char) in c.withIndex()) {
        if (qIndex < q.length && char == q[qIndex]) {
            if (cIndex == lastMatchIndex + 1) {
                consecutiveMatches++
            }
            lastMatchIndex = cIndex
            qIndex++
        }
    }
    
    if (qIndex == q.length) {
        return 100 + consecutiveMatches * 20
    }
    
    return null
}

fun getAutocompleteMatches(
    query: String,
    features: List<ProxmarkFeature>,
    commandHistory: List<String>
): List<AutocompleteMatch> {
    val matches = mutableListOf<AutocompleteMatch>()
    
    // Match features
    features.forEach { feature ->
        fuzzyMatch(query, feature.command)?.let { score ->
            matches.add(AutocompleteMatch(feature.command, feature, score, emptyList()))
        }
    }
    
    // Match history (with recency boost)
    commandHistory.forEachIndexed { index, cmd ->
        fuzzyMatch(query, cmd)?.let { score ->
            val recencyBoost = (commandHistory.size - index) * 5
            matches.add(AutocompleteMatch(cmd, null, score + recencyBoost, emptyList()))
        }
    }
    
    return matches.sortedByDescending { it.score }.take(10)
}
```

---

## 4. Command History UI

### 4.1 History Drawer Design (Swipe from right edge)

```
┌─────────────────────────────────────────────────────┐
│  Command History                          [Search] │
│  ─────────────────────────────────────────────────  │
│                                                     │
│  Today                                              │
│  ● hf search                              14:32    │
│  ● lf search                              14:28    │
│  ● hw tune                                14:15    │
│                                                     │
│  Yesterday                                          │
│  ● hf mf rdbl 0 A FFFFFFFFFFFF           19:45    │
│  ● hf 14a read                            19:42    │
│                                                     │
│  [Clear History]                                    │
└─────────────────────────────────────────────────────┘
```

### 4.2 Inline History Navigation

- **Up arrow button** (top-right of input field): Navigate backwards through history
- **Down arrow button**: Navigate forwards through history
- **Swipe up on input field**: Previous command
- **Swipe down on input field**: Next command
- **State management**: Current history index resets when user types

### 4.3 History Search

```
┌─────────────────────────────────────────────────────┐
│  Search History               [🔍 "mf rdbl"]    [X]│
│  ─────────────────────────────────────────────────  │
│                                                     │
│  5 results                                          │
│  ● hf mf rdbl 4 A FFFFFFFFFFFF           Today     │
│  ● hf mf rdbl 0 A FFFFFFFFFFFF           Today     │
│  ● hf mf rdbl 1 B A0A1A2A3A4A5           Yesterday │
│  ● hf mf rdbl 5 A FFFFFFFFFFFF           Jan 15    │
│  ● hf mf rdbl 10 A FFFFFFFFFFFF          Jan 12    │
│                                                     │
└─────────────────────────────────────────────────────┘
```

### 4.4 History Data Model

```kotlin
data class CommandHistoryEntry(
    val command: String,
    val timestamp: Long,
    val executionTimeMs: Long? = null,
    val wasSuccessful: Boolean? = null
)

class CommandHistoryManager(
    private val maxSize: Int = 1000
) {
    private val history = ArrayDeque<CommandHistoryEntry>(maxSize)
    private var currentIndex = -1
    
    fun add(entry: CommandHistoryEntry) {
        // Remove duplicates (keep most recent)
        history.removeAll { it.command == entry.command }
        history.addFirst(entry)
        
        // Enforce max size
        while (history.size > maxSize) {
            history.removeLast()
        }
        
        currentIndex = -1
    }
    
    fun navigateBack(): String? {
        if (history.isEmpty()) return null
        currentIndex = (currentIndex + 1).coerceAtMost(history.size - 1)
        return history[currentIndex].command
    }
    
    fun navigateForward(): String? {
        if (currentIndex <= 0) {
            currentIndex = -1
            return ""
        }
        currentIndex--
        return history[currentIndex].command
    }
    
    fun search(query: String): List<CommandHistoryEntry> {
        return history.filter { it.command.contains(query, ignoreCase = true) }
    }
    
    fun groupByDate(): Map<String, List<CommandHistoryEntry>> {
        // Group by "Today", "Yesterday", "This Week", "Earlier"
        // Implementation omitted for brevity
    }
}
```

---

## 5. Pinned Commands Section Design

### 5.1 Visual Design

```
┌─────────────────────────────────────────────────────┐
│  📌 Pinned Commands                    [Collapse ▲] │
│  ─────────────────────────────────────────────────  │
│  [hf search]  [lf search]  [hw tune]  [hw status]  │
│  [version]    [hf 14a read]           [+ Add]      │
└─────────────────────────────────────────────────────┘
```

### 5.2 Interaction Model

- **Tap chip**: Execute command immediately (if SAFE) or show confirmation (if ADVANCED/DESTRUCTIVE)
- **Long-press chip**: Options menu
  - Unpin
  - Edit (for commands with parameters)
  - Move to beginning/end
- **Drag & drop**: Reorder chips
- **Add button**: Shows sheet with:
  - Current command in input field (if any)
  - Recent commands
  - Favorite features
  - All available features (searchable)

### 5.3 Data Model

```kotlin
data class PinnedCommand(
    val id: String,
    val command: String,
    val displayName: String,
    val feature: ProxmarkFeature?,
    val order: Int
)

// Stored in SettingsStore
data class AppSettings(
    // ... existing fields ...
    val pinnedCommands: List<PinnedCommand> = emptyList()
)
```

---

## 6. Raw vs Structured View Toggle Design

### 6.1 View Modes

#### Raw CLI View (Default)
- Displays all terminal output line-by-line
- No parsing or interpretation
- Identical to desktop CLI experience
- Monospace font, syntax highlighting

#### Structured View
- Parses output into semantic blocks
- Groups related output (e.g., tag scan results)
- Collapsible sections
- Better for reviewing large outputs

### 6.2 Toggle Button Design

```
┌─────────────────────────────────┐
│  [Raw] | Structured              │ ← Segmented button
└─────────────────────────────────┘
```

### 6.3 Structured View Example

```
┌─────────────────────────────────────────────────────┐
│  Raw | [Structured]                                 │
├─────────────────────────────────────────────────────┤
│                                                     │
│  ▼ Command: hf search                     14:32:15 │
│     ├─ ISO14443-A tag found                        │
│     ├─ UID: 04:9A:3C:2A:89:67:80                   │
│     ├─ ATQA: 00:04                                 │
│     ├─ SAK: 08                                     │
│     └─ Card type: MIFARE Classic 1K                │
│                                                     │
│  ▶ Command: hw tune                       14:28:03 │
│                                                     │
│  ▼ Command: lf search                     14:15:47 │
│     ├─ EM410x tag found                            │
│     ├─ EM TAG ID: 1234567890                       │
│     └─ Clock rate: 64                              │
│                                                     │
└─────────────────────────────────────────────────────┘
```

### 6.4 Implementation Strategy

```kotlin
sealed class TerminalViewMode {
    object Raw : TerminalViewMode()
    object Structured : TerminalViewMode()
}

data class StructuredBlock(
    val command: String,
    val timestamp: Long,
    val lines: List<TerminalLine>,
    val parsedData: Map<String, String>? = null,
    val isExpanded: Boolean = true
)

fun groupTerminalLinesIntoBlocks(
    lines: List<TerminalLine>
): List<StructuredBlock> {
    val blocks = mutableListOf<StructuredBlock>()
    var currentBlock: MutableList<TerminalLine>? = null
    var currentCommand: String? = null
    var commandTimestamp: Long? = null
    
    lines.forEach { line ->
        when (line.type) {
            TerminalLineType.SENT -> {
                // Finish previous block
                if (currentBlock != null && currentCommand != null && commandTimestamp != null) {
                    blocks.add(StructuredBlock(
                        command = currentCommand,
                        timestamp = commandTimestamp,
                        lines = currentBlock.toList()
                    ))
                }
                // Start new block
                currentCommand = line.text
                commandTimestamp = line.timestamp
                currentBlock = mutableListOf(line)
            }
            else -> {
                currentBlock?.add(line)
            }
        }
    }
    
    // Add final block
    if (currentBlock != null && currentCommand != null && commandTimestamp != null) {
        blocks.add(StructuredBlock(
            command = currentCommand,
            timestamp = commandTimestamp,
            lines = currentBlock.toList()
        ))
    }
    
    return blocks
}
```

---

## 7. Input Field Design

### 7.1 Multi-line Support

```
┌─────────────────────────────────────────────────────┐
│  hf mf rdbl 0 A FFFFFFFFFFFF▮                      │ ← Single-line mode
│  [↑] [↓]                                    [Send] │
└─────────────────────────────────────────────────────┘

// User taps expand button or presses Shift+Enter

┌─────────────────────────────────────────────────────┐
│  hf mf rdbl 0 A FFFFFFFFFFFF                       │ ← Multi-line mode
│  hf mf rdbl 1 A FFFFFFFFFFFF                       │
│  hf mf rdbl 2 A FFFFFFFFFFFF▮                      │
│  [↑] [↓] [▼Collapse]                        [Send] │
└─────────────────────────────────────────────────────┘
```

### 7.2 Input Features

- **Auto-expand**: Input field grows as user types (max 5 lines visible)
- **Scroll**: If content exceeds 5 lines, becomes scrollable
- **Line numbers**: Show line numbers in multi-line mode
- **Syntax highlighting**: Real-time as user types
- **Paste support**: Handle multi-line paste
- **Enter behavior**:
  - Single-line mode: Enter = Send
  - Multi-line mode: Enter = New line, Ctrl+Enter = Send

### 7.3 Implementation

```kotlin
@Composable
fun TerminalInputField(
    value: String,
    onValueChange: (String) -> Unit,
    onSend: () -> Unit,
    onHistoryUp: () -> Unit,
    onHistoryDown: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isMultiLine by remember { mutableStateOf(false) }
    val lineCount = value.count { it == '\n' } + 1
    
    // Auto-expand to multi-line if user types newline
    LaunchedEffect(lineCount) {
        if (lineCount > 1) {
            isMultiLine = true
        }
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        // History navigation buttons
        Column(
            modifier = Modifier.padding(end = 4.dp)
        ) {
            IconButton(onClick = onHistoryUp) {
                Icon(Icons.Default.KeyboardArrowUp, "Previous command")
            }
            IconButton(onClick = onHistoryDown) {
                Icon(Icons.Default.KeyboardArrowDown, "Next command")
            }
        }
        
        // Input field
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier
                .weight(1f)
                .heightIn(min = 56.dp, max = if (isMultiLine) 200.dp else 56.dp),
            textStyle = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 14.sp
            ),
            maxLines = if (isMultiLine) 10 else 1,
            singleLine = !isMultiLine,
            keyboardOptions = KeyboardOptions(
                imeAction = if (isMultiLine) ImeAction.None else ImeAction.Send
            ),
            keyboardActions = KeyboardActions(
                onSend = { if (!isMultiLine) onSend() }
            ),
            placeholder = { Text("Enter command...") }
        )
        
        // Send button
        Button(
            onClick = onSend,
            modifier = Modifier
                .padding(start = 4.dp)
                .height(56.dp),
            enabled = value.isNotBlank()
        ) {
            Text("Send")
        }
    }
}
```

---

## 8. Output Rendering Approach

### 8.1 Efficient LazyColumn Rendering

**Challenge**: Rendering 10,000+ lines without UI lag

**Solution**: Virtualized scrolling with windowed rendering

```kotlin
@Composable
fun TerminalOutputArea(
    lines: List<TerminalLine>,
    autoScroll: Boolean,
    onAutoScrollChange: (Boolean) -> Unit,
    viewMode: TerminalViewMode,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    
    // Auto-scroll to bottom when new lines arrive
    LaunchedEffect(lines.size) {
        if (autoScroll && lines.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(lines.size - 1)
            }
        }
    }
    
    // Detect manual scroll and disable auto-scroll
    LaunchedEffect(listState.isScrollInProgress) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index ->
                if (listState.isScrollInProgress && index < lines.size - 5) {
                    onAutoScrollChange(false)
                }
            }
    }
    
    Box(modifier = modifier.fillMaxSize()) {
        when (viewMode) {
            TerminalViewMode.Raw -> {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = lines,
                        key = { "${it.timestamp}-${it.text}" }
                    ) { line ->
                        TerminalLineView(line)
                    }
                }
            }
            TerminalViewMode.Structured -> {
                val blocks = remember(lines) { 
                    groupTerminalLinesIntoBlocks(lines) 
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(
                        items = blocks,
                        key = { "${it.timestamp}-${it.command}" }
                    ) { block ->
                        StructuredBlockView(block)
                    }
                }
            }
        }
        
        // Auto-scroll lock indicator
        if (!autoScroll) {
            FloatingActionButton(
                onClick = { 
                    onAutoScrollChange(true)
                    coroutineScope.launch {
                        listState.animateScrollToItem(lines.size - 1)
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                Icon(Icons.Default.ArrowDownward, "Resume auto-scroll")
            }
        }
    }
}
```

### 8.2 Line Rendering Performance

```kotlin
@Composable
fun TerminalLineView(line: TerminalLine) {
    val segments = remember(line) { parseTerminalLine(line) }
    val colors = if (isSystemInDarkTheme()) TerminalColorsDark else TerminalColors
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        // Icon prefix (optional)
        when (line.type) {
            TerminalLineType.SENT -> Text(
                "> ",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    color = colors.commandKeyword,
                    fontWeight = FontWeight.Bold
                )
            )
            TerminalLineType.ERROR -> Text(
                "✗ ",
                style = TextStyle(color = colors.errorText)
            )
            TerminalLineType.SYSTEM -> Text(
                "• ",
                style = TextStyle(color = colors.systemInfo)
            )
            else -> {}
        }
        
        // Render segments with syntax highlighting
        Text(
            text = buildAnnotatedString {
                segments.forEach { segment ->
                    when (segment) {
                        is TerminalSegment.Command -> {
                            withStyle(
                                SpanStyle(
                                    color = colors.commandKeyword,
                                    fontWeight = FontWeight.Bold
                                )
                            ) {
                                append(segment.text)
                            }
                        }
                        is TerminalSegment.Argument -> {
                            withStyle(SpanStyle(color = colors.commandArgument)) {
                                append(segment.text)
                            }
                        }
                        is TerminalSegment.Highlight -> {
                            withStyle(
                                SpanStyle(
                                    color = colors.receivedHighlight,
                                    fontWeight = FontWeight.Bold
                                )
                            ) {
                                append(segment.text)
                            }
                        }
                        is TerminalSegment.Error -> {
                            withStyle(SpanStyle(color = colors.errorText)) {
                                append(segment.text)
                            }
                        }
                        is TerminalSegment.Success -> {
                            withStyle(SpanStyle(color = colors.successText)) {
                                append(segment.text)
                            }
                        }
                        is TerminalSegment.Output -> {
                            withStyle(SpanStyle(color = colors.receivedText)) {
                                append(segment.text)
                            }
                        }
                    }
                }
            },
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            ),
            modifier = Modifier.fillMaxWidth()
        )
    }
}
```

### 8.3 Memory Management

**Buffer Strategy**: Keep last 10,000 lines in memory

```kotlin
class TerminalBufferManager(
    private val maxLines: Int = 10_000
) {
    private val buffer = ArrayDeque<TerminalLine>(maxLines)
    
    fun add(line: TerminalLine) {
        if (buffer.size >= maxLines) {
            buffer.removeFirst()
        }
        buffer.addLast(line)
    }
    
    fun getLines(): List<TerminalLine> = buffer.toList()
    
    fun clear() {
        buffer.clear()
    }
}
```

---

## 9. Compose Implementation Strategy

### 9.1 State Management

```kotlin
// Add to MainUiState
data class MainUiState(
    // ... existing fields ...
    val terminalLines: List<TerminalLine> = emptyList(),
    val terminalAutoScroll: Boolean = true,
    val terminalViewMode: TerminalViewMode = TerminalViewMode.Raw,
    val terminalInputText: String = "",
    val commandHistory: List<CommandHistoryEntry> = emptyList(),
    val historyDrawerOpen: Boolean = false,
    val showAutocomplete: Boolean = false,
    val autocompleteMatches: List<AutocompleteMatch> = emptyList()
)

// Add to MainViewModel
class MainViewModel(
    // ... existing params ...
) : ViewModel() {
    
    private val terminalBuffer = TerminalBufferManager()
    private val historyManager = CommandHistoryManager()
    
    init {
        // Collect terminal lines from repository
        viewModelScope.launch {
            repository.terminalLines.collect { line ->
                terminalBuffer.add(line)
                _uiState.update { 
                    it.copy(terminalLines = terminalBuffer.getLines())
                }
            }
        }
    }
    
    fun sendTerminalCommand(command: String) {
        if (command.isBlank()) return
        
        repository.sendRawCommand(command)
        
        historyManager.add(CommandHistoryEntry(
            command = command,
            timestamp = System.currentTimeMillis()
        ))
        
        _uiState.update { 
            it.copy(
                terminalInputText = "",
                commandHistory = historyManager.history.toList()
            )
        }
    }
    
    fun onTerminalInputChanged(text: String) {
        _uiState.update { 
            it.copy(
                terminalInputText = text,
                showAutocomplete = text.isNotEmpty(),
                autocompleteMatches = if (text.isNotEmpty()) {
                    getAutocompleteMatches(
                        text, 
                        it.availableFeatures,
                        it.commandHistory.map { h -> h.command }
                    )
                } else {
                    emptyList()
                }
            )
        }
    }
    
    fun navigateHistoryUp() {
        historyManager.navigateBack()?.let { command ->
            _uiState.update { it.copy(terminalInputText = command) }
        }
    }
    
    fun navigateHistoryDown() {
        historyManager.navigateForward()?.let { command ->
            _uiState.update { it.copy(terminalInputText = command) }
        }
    }
    
    fun toggleTerminalViewMode() {
        _uiState.update {
            it.copy(
                terminalViewMode = when (it.terminalViewMode) {
                    TerminalViewMode.Raw -> TerminalViewMode.Structured
                    TerminalViewMode.Structured -> TerminalViewMode.Raw
                }
            )
        }
    }
    
    fun setTerminalAutoScroll(enabled: Boolean) {
        _uiState.update { it.copy(terminalAutoScroll = enabled) }
    }
    
    fun clearTerminal() {
        terminalBuffer.clear()
        _uiState.update { it.copy(terminalLines = emptyList()) }
    }
    
    fun exportTerminalLog(): String {
        return terminalBuffer.getLines().joinToString("\n") { line ->
            val timestamp = SimpleDateFormat("HH:mm:ss", Locale.US)
                .format(Date(line.timestamp))
            "[$timestamp] ${line.type.name}: ${line.text}"
        }
    }
}
```

### 9.2 Repository Integration

```kotlin
// Add to ProxmarkRepository
class ProxmarkRepository(
    // ... existing params ...
) {
    
    val terminalLines: SharedFlow<TerminalLine> = proxmarkClient.terminalLines
    
    fun sendRawCommand(command: String) {
        proxmarkClient.sendCommand(command)
    }
}
```

### 9.3 Screen Integration

```kotlin
@Composable
fun TerminalScreen(
    uiState: MainUiState,
    onSendCommand: (String) -> Unit,
    onInputChanged: (String) -> Unit,
    onHistoryUp: () -> Unit,
    onHistoryDown: () -> Unit,
    onToggleViewMode: () -> Unit,
    onClearTerminal: () -> Unit,
    onExportLog: () -> String,
    onSetAutoScroll: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // Top bar
        TerminalTopBar(
            viewMode = uiState.terminalViewMode,
            onToggleViewMode = onToggleViewMode,
            onClearTerminal = onClearTerminal,
            onExportLog = onExportLog
        )
        
        // Pinned commands (collapsible)
        if (uiState.settings.pinnedCommands.isNotEmpty()) {
            PinnedCommandsBar(
                commands = uiState.settings.pinnedCommands,
                onCommandClick = { onSendCommand(it.command) }
            )
        }
        
        // Output area
        Box(modifier = Modifier.weight(1f)) {
            TerminalOutputArea(
                lines = uiState.terminalLines,
                autoScroll = uiState.terminalAutoScroll,
                onAutoScrollChange = onSetAutoScroll,
                viewMode = uiState.terminalViewMode
            )
        }
        
        // Input field with autocomplete
        Box {
            if (uiState.showAutocomplete && uiState.autocompleteMatches.isNotEmpty()) {
                AutocompleteDropdown(
                    matches = uiState.autocompleteMatches,
                    onSelect = { match ->
                        onInputChanged(match.command)
                    },
                    modifier = Modifier.align(Alignment.BottomStart)
                )
            }
            
            TerminalInputField(
                value = uiState.terminalInputText,
                onValueChange = onInputChanged,
                onSend = { onSendCommand(uiState.terminalInputText) },
                onHistoryUp = onHistoryUp,
                onHistoryDown = onHistoryDown
            )
        }
    }
}
```

---

## 10. Example Compose Code

### Complete Terminal Component

```kotlin
package com.example.proxmark.ui.terminal

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.proxmark.protocol.TerminalLine
import com.example.proxmark.protocol.TerminalLineType
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TerminalView(
    lines: List<TerminalLine>,
    inputText: String,
    onInputChange: (String) -> Unit,
    onSendCommand: () -> Unit,
    onHistoryUp: () -> Unit,
    onHistoryDown: () -> Unit,
    autoScroll: Boolean,
    onAutoScrollChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var viewMode by remember { mutableStateOf(TerminalViewMode.Raw) }
    val clipboardManager = LocalClipboardManager.current
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()
    
    // Auto-scroll effect
    LaunchedEffect(lines.size, autoScroll) {
        if (autoScroll && lines.isNotEmpty()) {
            listState.animateScrollToItem(lines.size - 1)
        }
    }
    
    // Detect manual scroll
    LaunchedEffect(listState.isScrollInProgress) {
        snapshotFlow { listState.firstVisibleItemIndex }
            .collect { index ->
                if (listState.isScrollInProgress && index < lines.size - 5) {
                    onAutoScrollChange(false)
                }
            }
    }
    
    Column(modifier = modifier.fillMaxSize()) {
        // Top bar
        Surface(
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "Terminal",
                    style = MaterialTheme.typography.titleMedium
                )
                
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    // View mode toggle
                    SegmentedButton(
                        selected = viewMode == TerminalViewMode.Raw,
                        onClick = { viewMode = TerminalViewMode.Raw }
                    ) {
                        Text("Raw", fontSize = 12.sp)
                    }
                    SegmentedButton(
                        selected = viewMode == TerminalViewMode.Structured,
                        onClick = { viewMode = TerminalViewMode.Structured }
                    ) {
                        Text("Structured", fontSize = 12.sp)
                    }
                    
                    // Export
                    IconButton(onClick = {
                        val log = lines.joinToString("\n") { it.text }
                        clipboardManager.setText(AnnotatedString(log))
                    }) {
                        Icon(Icons.Default.Share, "Export log")
                    }
                    
                    // Clear
                    IconButton(onClick = { /* Clear terminal */ }) {
                        Icon(Icons.Default.Delete, "Clear terminal")
                    }
                }
            }
        }
        
        // Output area
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(8.dp)
            ) {
                items(
                    items = lines,
                    key = { "${it.timestamp}-${it.text.hashCode()}" }
                ) { line ->
                    TerminalLineItem(line)
                }
            }
            
            // Auto-scroll FAB
            AnimatedVisibility(
                visible = !autoScroll,
                enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { it }),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
            ) {
                SmallFloatingActionButton(
                    onClick = {
                        onAutoScrollChange(true)
                        scope.launch {
                            listState.animateScrollToItem(lines.size - 1)
                        }
                    }
                ) {
                    Icon(Icons.Default.ArrowDownward, "Resume auto-scroll")
                }
            }
        }
        
        // Input area
        Surface(
            tonalElevation = 3.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                // History buttons
                Column(modifier = Modifier.padding(end = 4.dp)) {
                    IconButton(
                        onClick = onHistoryUp,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowUp,
                            "Previous",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(
                        onClick = onHistoryDown,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            Icons.Default.KeyboardArrowDown,
                            "Next",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                
                // Input field
                OutlinedTextField(
                    value = inputText,
                    onValueChange = onInputChange,
                    modifier = Modifier.weight(1f),
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp
                    ),
                    placeholder = {
                        Text(
                            "Enter command...",
                            style = TextStyle(fontFamily = FontFamily.Monospace)
                        )
                    },
                    maxLines = 5
                )
                
                // Send button
                Button(
                    onClick = onSendCommand,
                    modifier = Modifier
                        .padding(start = 4.dp)
                        .height(56.dp),
                    enabled = inputText.isNotBlank()
                ) {
                    Text("Send")
                }
            }
        }
    }
}

@Composable
private fun TerminalLineItem(line: TerminalLine) {
    val isDark = MaterialTheme.colorScheme.background.luminance() < 0.5f
    val colors = if (isDark) TerminalColorsDark else TerminalColors
    
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 1.dp),
        verticalAlignment = Alignment.Top
    ) {
        // Prefix icon
        val (icon, color) = when (line.type) {
            TerminalLineType.SENT -> ">" to colors.commandKeyword
            TerminalLineType.ERROR -> "✗" to colors.errorText
            TerminalLineType.SYSTEM -> "•" to colors.systemInfo
            TerminalLineType.RECEIVED -> " " to colors.receivedText
        }
        
        Text(
            text = icon,
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp,
                color = color,
                fontWeight = if (line.type == TerminalLineType.SENT) FontWeight.Bold else FontWeight.Normal
            ),
            modifier = Modifier.width(20.dp)
        )
        
        // Line text with syntax highlighting
        Text(
            text = buildAnnotatedString {
                when (line.type) {
                    TerminalLineType.SENT -> {
                        val parts = line.text.split(" ", limit = 2)
                        withStyle(SpanStyle(
                            color = colors.commandKeyword,
                            fontWeight = FontWeight.Bold
                        )) {
                            append(parts[0])
                        }
                        if (parts.size > 1) {
                            withStyle(SpanStyle(color = colors.commandArgument)) {
                                append(" " + parts[1])
                            }
                        }
                    }
                    TerminalLineType.ERROR -> {
                        withStyle(SpanStyle(color = colors.errorText)) {
                            append(line.text)
                        }
                    }
                    else -> {
                        withStyle(SpanStyle(color = colors.receivedText)) {
                            append(line.text)
                        }
                    }
                }
            },
            style = TextStyle(
                fontFamily = FontFamily.Monospace,
                fontSize = 13.sp
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun SegmentedButton(
    selected: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Button(
        onClick = onClick,
        colors = ButtonDefaults.buttonColors(
            containerColor = if (selected) 
                MaterialTheme.colorScheme.primary 
            else 
                MaterialTheme.colorScheme.surfaceVariant,
            contentColor = if (selected) 
                MaterialTheme.colorScheme.onPrimary 
            else 
                MaterialTheme.colorScheme.onSurfaceVariant
        ),
        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
        modifier = Modifier.height(36.dp)
    ) {
        content()
    }
}

// Color definitions
object TerminalColors {
    val commandKeyword = Color(0xFF0066CC)
    val commandArgument = Color(0xFF009900)
    val receivedText = Color(0xFF333333)
    val receivedHighlight = Color(0xFFCC6600)
    val systemInfo = Color(0xFF666666)
    val errorText = Color(0xFFCC0000)
    val successText = Color(0xFF009900)
}

object TerminalColorsDark {
    val commandKeyword = Color(0xFF5599FF)
    val commandArgument = Color(0xFF66CC66)
    val receivedText = Color(0xFFCCCCCC)
    val receivedHighlight = Color(0xFFFF9933)
    val systemInfo = Color(0xFF999999)
    val errorText = Color(0xFFFF4444)
    val successText = Color(0xFF66CC66)
}

sealed class TerminalViewMode {
    object Raw : TerminalViewMode()
    object Structured : TerminalViewMode()
}

// Helper extension
private fun Color.luminance(): Float {
    return (0.299f * red + 0.587f * green + 0.114f * blue)
}
```

---

## Performance Optimization Summary

### Key Strategies

1. **Virtualized Scrolling**: LazyColumn only renders visible items
2. **Efficient Keys**: Use stable keys (`timestamp-hash`) for item recomposition
3. **Buffer Management**: Cap at 10,000 lines to prevent memory issues
4. **Syntax Parsing**: Cache parsed segments with `remember()`
5. **Auto-scroll Debouncing**: Only scroll when at bottom
6. **Lazy Evaluation**: Parse structured blocks only when in Structured view mode

### Expected Performance

- **10,000 lines**: Smooth scrolling, instant search
- **High-frequency output** (100+ lines/sec): No dropped frames with auto-scroll
- **Memory footprint**: ~5-10 MB for 10,000 lines
- **Startup time**: <100ms to render initial state

---

## Future Enhancements

1. **Filter/Search**: Regex search across all lines
2. **Bookmarks**: Mark specific lines for quick navigation
3. **Session Management**: Save/load terminal sessions
4. **Themes**: Multiple syntax highlighting themes
5. **Split View**: Show terminal + dashboard simultaneously
6. **Command Templates**: Parametrized command templates
7. **Macro Recording**: Record and replay command sequences
8. **Voice Input**: Speak commands (experimental)

---

## Integration Checklist

- [ ] Add `terminalLines` state to `MainUiState`
- [ ] Implement `TerminalBufferManager` in `MainViewModel`
- [ ] Create `CommandHistoryManager` utility class
- [ ] Implement syntax highlighting parsers
- [ ] Build `TerminalView` composable
- [ ] Add autocomplete logic with fuzzy matching
- [ ] Implement pinned commands persistence
- [ ] Add export functionality (TXT, JSON)
- [ ] Create structured view parser
- [ ] Add command history drawer
- [ ] Implement auto-scroll behavior
- [ ] Test with high-frequency output (stress test)
- [ ] Add accessibility labels for screen readers
- [ ] Document public APIs

---

**Document Version**: 1.0  
**Last Updated**: 2025-01-27  
**Author**: Senior Android UX/UI Designer
