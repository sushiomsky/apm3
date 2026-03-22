package com.sushiomsky.proxmark.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sushiomsky.proxmark.features.FeatureExecutionState
import com.sushiomsky.proxmark.mifare.*
import com.sushiomsky.proxmark.protocol.TerminalLine
import com.sushiomsky.proxmark.protocol.TerminalLineType

// Domain-specific colors
private val AccentGreen = Color(0xFF4CAF50)
private val AccentBlue = Color(0xFF2196F3)
private val AccentAmber = Color(0xFFFFC107)
private val AccentRed = Color(0xFFF44336)
private val DarkSurface = Color(0xFF1A1A2E)
private val CardDark = Color(0xFF16213E)

/**
 * Full-screen interactive gym locker analysis workflow.
 * Guides user through tag identification → key recovery → dump analysis → clone/emulate.
 * Uses real-world McFit/Gantner domain knowledge.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GymLockerWorkflowDialog(
    keyState: MifareKeyState,
    executionState: FeatureExecutionState,
    terminalLines: List<TerminalLine>,
    connected: Boolean,
    tagUid: String?,
    tagType: String?,
    onSectorClick: (Int) -> Unit,
    onRunCommand: (String, String) -> Unit,
    onReset: () -> Unit,
    onDismiss: () -> Unit
) {
    @Suppress("UNUSED_VARIABLE")
    val resetAction = onReset // held for future use
    var currentPhase by remember { mutableIntStateOf(0) }
    var completedPhases by remember { mutableStateOf(setOf<Int>()) }
    var selectedSector by remember { mutableStateOf<Int?>(null) }
    var showTerminal by remember { mutableStateOf(false) }

    val isRunning = executionState is FeatureExecutionState.Running
    val isFudan = tagType?.contains("FM11RF08", ignoreCase = true) == true ||
                  tagType?.contains("Fudan", ignoreCase = true) == true

    // Auto-detect interesting sectors (non-default keys)
    val interestingSectors = keyState.sectors.filter { s ->
        s.keyAStatus == KeyStatus.FOUND && s.keyA != null && s.keyA != "FFFFFFFFFFFF" ||
        s.keyBStatus == KeyStatus.FOUND && s.keyB != null && s.keyB != "FFFFFFFFFFFF"
    }

    val phases = listOf(
        "Identify Tag", "Recover Keys", "Read Dump", "Analyze Data", "Test & Clone"
    )

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = DarkSurface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // ── Top bar with gradient ──
                Surface(
                    color = Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF0F3460), Color(0xFF533483))
                            )
                        )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, "Close", tint = Color.White)
                            }
                            Text(
                                "🏋️ Gym Locker Analysis",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            IconButton(onClick = { showTerminal = !showTerminal }) {
                                Icon(
                                    Icons.Default.Terminal,
                                    "Terminal",
                                    tint = if (showTerminal) AccentGreen else Color.White.copy(alpha = 0.6f)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Phase stepper
                        PhaseStepperRow(
                            phases = phases,
                            currentPhase = currentPhase,
                            completedPhases = completedPhases
                        )
                    }
                }

                // ── Main content ──
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item { Spacer(modifier = Modifier.height(8.dp)) }

                    // Connection status
                    if (!connected) {
                        item {
                            StatusBanner(
                                text = "⚠️ Proxmark3 not connected. Connect device to begin.",
                                color = AccentRed
                            )
                        }
                    }

                    // Tag info card (always visible once scanned)
                    if (tagUid != null) {
                        item {
                            TagInfoCard(
                                uid = tagUid,
                                type = tagType,
                                isFudan = isFudan,
                                weakPrng = isFudan // FM11RF08S always has weak PRNG
                            )
                        }
                    }

                    // ── Phase-specific content ──
                    when (currentPhase) {
                        0 -> { // Identify Tag
                            item {
                                PhaseCard(
                                    title = "Phase 1: Identify Tag",
                                    description = "Place the wristband/fob on the PM3 antenna and scan.",
                                    icon = "🔍"
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        Text(
                                            "Most gym wristbands use cheap Chinese clones (Fudan FM11RF08S) " +
                                            "with weak PRNG and hardware backdoors. These crack in seconds.",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = Color.White.copy(alpha = 0.7f)
                                        )
                                        ActionButton(
                                            label = "Scan HF Tag",
                                            command = "hf 14a reader",
                                            enabled = connected && !isRunning,
                                            onClick = { onRunCommand("hf 14a reader", "Scan Tag") }
                                        )
                                        ActionButton(
                                            label = "Get MIFARE Info",
                                            command = "hf mf info",
                                            enabled = connected && !isRunning,
                                            onClick = { onRunCommand("hf mf info", "MIFARE Info") }
                                        )
                                        if (tagUid != null) {
                                            SuccessChip("Tag found: $tagUid")
                                        }
                                    }
                                }
                            }
                        }
                        1 -> { // Recover Keys
                            // Smart recommendation
                            item {
                                AttackRecommendationCard(
                                    keyState = keyState,
                                    tagType = tagType,
                                    executionState = executionState,
                                    connected = connected,
                                    onRunCommand = onRunCommand
                                )
                            }

                            // Full attack pipeline
                            item {
                                AttackPipelineCard(
                                    keyState = keyState,
                                    tagType = tagType,
                                    executionState = executionState,
                                    connected = connected,
                                    onRunCommand = onRunCommand
                                )
                            }

                            // Recovery stats
                            item {
                                KeyRecoveryStatsCard(keyState = keyState)
                            }

                            // Sector map
                            item {
                                SectorMapCard(
                                    keyState = keyState,
                                    selectedSector = selectedSector,
                                    onSectorClick = { selectedSector = it; onSectorClick(it) }
                                )
                            }
                        }
                        2 -> { // Read Dump
                            item {
                                PhaseCard(
                                    title = "Phase 3: Read Full Card",
                                    description = "Dump all 16 sectors using recovered keys.",
                                    icon = "📋"
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        ActionButton(
                                            label = "Dump All Sectors",
                                            command = "hf mf dump",
                                            enabled = connected && !isRunning,
                                            onClick = { onRunCommand("hf mf dump", "Full Dump") }
                                        )
                                        if (interestingSectors.isNotEmpty()) {
                                            Text(
                                                "Or read just the interesting sector(s):",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = Color.White.copy(alpha = 0.7f)
                                            )
                                            interestingSectors.forEach { sector ->
                                                val keyHex = sector.keyA ?: sector.keyB ?: "FFFFFFFFFFFF"
                                                val blockNo = sector.sectorNumber * 4
                                                ActionButton(
                                                    label = "Read Sector ${sector.sectorNumber} (blocks $blockNo-${blockNo + 3})",
                                                    command = "hf mf rdbl --blk $blockNo -k $keyHex",
                                                    enabled = connected && !isRunning,
                                                    onClick = {
                                                        onRunCommand(
                                                            "hf mf rdbl --blk $blockNo -k $keyHex",
                                                            "Read Sector ${sector.sectorNumber}"
                                                        )
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                            item {
                                SectorMapCard(
                                    keyState = keyState,
                                    selectedSector = selectedSector,
                                    onSectorClick = { selectedSector = it; onSectorClick(it) }
                                )
                            }
                        }
                        3 -> { // Analyze
                            item {
                                AccessPatternCard(keyState = keyState)
                            }
                            item {
                                KeyRecoveryStatsCard(keyState = keyState)
                            }
                            item {
                                PhaseCard(
                                    title = "Phase 4: Analyze Dump",
                                    description = "Identify the access control sector and understand the data.",
                                    icon = "📊"
                                ) {
                                    AnalysisContent(
                                        keyState = keyState,
                                        interestingSectors = interestingSectors,
                                        isFudan = isFudan
                                    )
                                }
                            }
                            item {
                                SectorMapCard(
                                    keyState = keyState,
                                    selectedSector = selectedSector,
                                    onSectorClick = { selectedSector = it; onSectorClick(it) }
                                )
                            }
                        }
                        4 -> { // Clone
                            item {
                                PhaseCard(
                                    title = "Phase 5: Test & Clone",
                                    description = "Emulate the dump or write to a blank card.",
                                    icon = "📡"
                                ) {
                                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                        WarningBanner(
                                            "⚖️ Only test your own assigned locker. " +
                                            "Unauthorized access is illegal (§242/§202a StGB in Germany)."
                                        )
                                        Text(
                                            "Step 1: Test with emulation first (no card needed)",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = AccentBlue,
                                            fontWeight = FontWeight.Bold
                                        )
                                        ActionButton(
                                            label = "Load dump to emulator",
                                            command = "hf mf eload -f dump.bin",
                                            enabled = connected && !isRunning,
                                            onClick = { onRunCommand("hf mf eload -f hf-mf-dump", "Load Dump") }
                                        )
                                        ActionButton(
                                            label = "Start Emulation",
                                            command = "hf mf sim",
                                            enabled = connected && !isRunning,
                                            primary = true,
                                            onClick = { onRunCommand("hf mf sim", "Emulate") }
                                        )
                                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                        Text(
                                            "Step 2: Clone to physical card (if emulation works)",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = AccentAmber,
                                            fontWeight = FontWeight.Bold
                                        )
                                        ActionButton(
                                            label = "Restore dump to card",
                                            command = "hf mf restore",
                                            enabled = connected && !isRunning,
                                            onClick = { onRunCommand("hf mf restore", "Clone Card") }
                                        )
                                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))
                                        Text(
                                            "Step 3: Sniff real reader (if clone fails)",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = AccentAmber,
                                            fontWeight = FontWeight.Bold
                                        )
                                        ActionButton(
                                            label = "Sniff reader ↔ tag comms",
                                            command = "hf 14a snoop",
                                            enabled = connected && !isRunning,
                                            onClick = { onRunCommand("hf 14a snoop", "Sniff Comms") }
                                        )
                                        TipBox(listOf(
                                            "Hold PM3 steady near reader during emulation",
                                            "If fails: reader may check UID → use Gen2/CUID magic card",
                                            "Gantner GL7p (common in McFit) usually accepts clones",
                                            "If no reaction: try sniffing to see what sector/block the reader checks"
                                        ))
                                    }
                                }
                            }
                        }
                    }

                    // Execution state indicator
                    if (isRunning) {
                        item {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = CardDark)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth().padding(12.dp),
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(20.dp),
                                        strokeWidth = 2.dp,
                                        color = AccentBlue
                                    )
                                    val title = (executionState as FeatureExecutionState.Running).title
                                    Text(
                                        "$title...",
                                        color = Color.White,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                        }
                    }

                    if (executionState is FeatureExecutionState.Success && executionState.output != null) {
                        item {
                            ResultCard(output = executionState.output)
                        }
                    }

                    // Terminal output
                    if (showTerminal && terminalLines.isNotEmpty()) {
                        item {
                            TerminalOutputCard(lines = terminalLines.takeLast(30))
                        }
                    }

                    item { Spacer(modifier = Modifier.height(80.dp)) }
                }

                // ── Bottom navigation ──
                Surface(
                    color = CardDark,
                    shadowElevation = 8.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = { if (currentPhase > 0) currentPhase-- },
                            enabled = currentPhase > 0,
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Back")
                        }

                        // Phase dots
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            phases.forEachIndexed { i, _ ->
                                val dotColor = when {
                                    i in completedPhases -> AccentGreen
                                    i == currentPhase -> AccentBlue
                                    else -> Color.White.copy(alpha = 0.2f)
                                }
                                Box(
                                    modifier = Modifier
                                        .size(8.dp)
                                        .background(dotColor, CircleShape)
                                )
                            }
                        }

                        Button(
                            onClick = {
                                completedPhases = completedPhases + currentPhase
                                if (currentPhase < phases.size - 1) currentPhase++
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = AccentBlue
                            )
                        ) {
                            Text(if (currentPhase < phases.size - 1) "Next" else "Done")
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                if (currentPhase < phases.size - 1) Icons.AutoMirrored.Filled.ArrowForward else Icons.Default.Check,
                                null, modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ── Subcomponents ──────────────────────────────────────────────

@Composable
private fun PhaseStepperRow(
    phases: List<String>,
    currentPhase: Int,
    completedPhases: Set<Int>
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        phases.forEachIndexed { i, name ->
            val isComplete = i in completedPhases
            val isCurrent = i == currentPhase
            val color = when {
                isComplete -> AccentGreen
                isCurrent -> AccentBlue
                else -> Color.White.copy(alpha = 0.3f)
            }
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = color.copy(alpha = if (isCurrent) 0.3f else 0.1f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isComplete) {
                        Icon(Icons.Default.Check, null, modifier = Modifier.size(12.dp), tint = color)
                    } else {
                        Text("${i + 1}", fontSize = 10.sp, color = color, fontWeight = FontWeight.Bold)
                    }
                    Text(name, fontSize = 10.sp, color = color, maxLines = 1)
                }
            }
        }
    }
}

@Composable
private fun TagInfoCard(uid: String, type: String?, isFudan: Boolean, weakPrng: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("📶 Tag Detected", color = AccentGreen, fontWeight = FontWeight.Bold)
                if (isFudan) {
                    Surface(shape = RoundedCornerShape(4.dp), color = AccentRed.copy(alpha = 0.2f)) {
                        Text(
                            "VULNERABLE",
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                            fontSize = 10.sp, color = AccentRed, fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            InfoRow("UID", uid)
            type?.let { InfoRow("Type", it) }
            if (weakPrng) InfoRow("PRNG", "Weak (exploitable)")
            if (isFudan) InfoRow("Backdoor", "A396EFA4E24F (known)")
        }
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = Color.White.copy(alpha = 0.5f))
        Text(value, fontSize = 12.sp, color = Color.White, fontFamily = FontFamily.Monospace)
    }
}

@Composable
private fun PhaseCard(
    title: String,
    description: String,
    icon: String,
    content: @Composable () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "$icon $title",
                style = MaterialTheme.typography.titleMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.7f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            content()
        }
    }
}

@Composable
private fun ActionButton(
    label: String,
    command: String,
    enabled: Boolean,
    primary: Boolean = false,
    onClick: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onClick,
            enabled = enabled,
            modifier = Modifier.weight(1f),
            colors = if (primary) ButtonDefaults.buttonColors(containerColor = AccentBlue)
                     else ButtonDefaults.buttonColors(containerColor = Color(0xFF2D3748)),
            shape = RoundedCornerShape(8.dp)
        ) {
            Column(horizontalAlignment = Alignment.Start, modifier = Modifier.weight(1f)) {
                Text(label, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
                Text(
                    command,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = Color.White.copy(alpha = 0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
        }
        IconButton(
            onClick = { clipboardManager.setText(AnnotatedString(command)) },
            modifier = Modifier.size(40.dp)
        ) {
            Icon(Icons.Default.ContentCopy, "Copy", tint = Color.White.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun SectorMapCard(
    keyState: MifareKeyState,
    selectedSector: Int?,
    onSectorClick: (Int) -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = CardDark),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("🗺️ Sector Map", color = Color.White, fontWeight = FontWeight.Bold)
                Text(
                    "${keyState.foundKeysCount}/${keyState.totalKeysNeeded} keys",
                    fontSize = 11.sp, color = AccentGreen
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            KeyRecoveryProgress(keyState = keyState)
            Spacer(modifier = Modifier.height(8.dp))
            SectorMapGrid(keyState = keyState, onSectorClick = onSectorClick)
            if (selectedSector != null && selectedSector in keyState.sectors.indices) {
                Spacer(modifier = Modifier.height(8.dp))
                SectorDetailPanel(sector = keyState.sectors[selectedSector])
            }
        }
    }
}

@Composable
private fun AnalysisContent(
    keyState: MifareKeyState,
    interestingSectors: List<SectorKeyState>,
    isFudan: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (interestingSectors.isEmpty() && keyState.foundKeysCount == 0) {
            Text(
                "Run key recovery first (Phase 2) to identify which sectors have custom keys.",
                style = MaterialTheme.typography.bodySmall,
                color = Color.White.copy(alpha = 0.6f)
            )
        } else if (interestingSectors.isEmpty()) {
            Text(
                "All sectors use default keys (FFFFFFFFFFFF). The reader may rely on " +
                "auth-only verification or UID matching.",
                style = MaterialTheme.typography.bodySmall,
                color = AccentAmber
            )
        } else {
            Text("🎯 Access Control Sectors:", color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            interestingSectors.forEach { sector ->
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = AccentGreen.copy(alpha = 0.1f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            "Sector ${sector.sectorNumber} — likely access sector",
                            color = AccentGreen, fontWeight = FontWeight.Bold, fontSize = 12.sp
                        )
                        sector.keyA?.let {
                            Text("Key A: $it", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White)
                        }
                        sector.keyB?.let {
                            Text("Key B: $it", fontFamily = FontFamily.Monospace, fontSize = 11.sp, color = Color.White)
                        }
                        val blockStart = sector.sectorNumber * 4
                        Text(
                            "Blocks $blockStart–${blockStart + 2} contain data, block ${blockStart + 3} = trailer",
                            fontSize = 10.sp, color = Color.White.copy(alpha = 0.5f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(4.dp))
            Text("📖 Typical Gym Patterns:", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 12.sp)
            TipBox(listOf(
                "Sector 7 is most common for gym access data (McFit/Gantner)",
                "Key A0A1A2A3A4A5 = factory default (very common in cheap installs)",
                "Block 28: often contains user token/membership ID",
                "Block 29: flags, expiration, or status bytes (80 FF FF D2 pattern)",
                "Reader authenticates → reads 1-2 blocks → checks pattern → unlocks",
                "Most readers DON'T write back to the tag (static token system)"
            ))
        }

        if (isFudan) {
            Spacer(modifier = Modifier.height(4.dp))
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AccentRed.copy(alpha = 0.1f)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("⚡ Fudan FM11RF08S Analysis", color = AccentRed, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    Text(
                        "• Hardware backdoor (A396EFA4E24F) enables instant key recovery\n" +
                        "• Weak/static PRNG makes nested attacks trivial\n" +
                        "• Very common in budget European gym wristbands\n" +
                        "• Security grade: F (broken by design)",
                        fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}

@Composable
private fun FudanBadge() {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = AccentAmber.copy(alpha = 0.15f)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("⚡", fontSize = 18.sp)
            Column {
                Text("Fudan FM11RF08S Detected", color = AccentAmber, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(
                    "Known hardware backdoor • Recovery takes <30 seconds",
                    fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

@Composable
private fun TipBox(tips: List<String>) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = AccentBlue.copy(alpha = 0.08f)
    ) {
        Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            tips.forEach { tip ->
                Text("💡 $tip", fontSize = 11.sp, color = Color.White.copy(alpha = 0.7f))
            }
        }
    }
}

@Composable
private fun WarningBanner(text: String) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = AccentRed.copy(alpha = 0.1f)
    ) {
        Text(
            text,
            modifier = Modifier.padding(12.dp),
            fontSize = 11.sp,
            color = AccentAmber,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun StatusBanner(text: String, color: Color) {
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = color.copy(alpha = 0.15f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text,
            modifier = Modifier.padding(12.dp),
            fontSize = 12.sp,
            color = color
        )
    }
}

@Composable
private fun SuccessChip(text: String) {
    Surface(shape = RoundedCornerShape(8.dp), color = AccentGreen.copy(alpha = 0.15f)) {
        Text(
            "✅ $text",
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
            fontSize = 12.sp, color = AccentGreen, fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ResultCard(output: String) {
    Card(
        colors = CardDefaults.cardColors(containerColor = AccentGreen.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text("✅ Result", color = AccentGreen, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                output,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.9f),
                maxLines = 10,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TerminalOutputCard(lines: List<TerminalLine>) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF0D0D0D)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Text("Terminal Output", fontSize = 10.sp, color = Color.White.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(4.dp))
            lines.forEach { line ->
                val color = when (line.type) {
                    TerminalLineType.SENT -> Color(0xFF00BCD4)
                    TerminalLineType.RECEIVED -> AccentGreen
                    TerminalLineType.ERROR -> AccentRed
                    TerminalLineType.SYSTEM -> Color(0xFF666666)
                }
                Text(
                    line.text,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 9.sp,
                    color = color,
                    maxLines = 2
                )
            }
        }
    }
}
