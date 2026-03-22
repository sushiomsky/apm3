package com.sushiomsky.proxmark.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sushiomsky.proxmark.features.FeatureExecutionState
import com.sushiomsky.proxmark.mifare.*

// Professional color palette
private val GreenSuccess = Color(0xFF4CAF50)
private val BlueActive = Color(0xFF2196F3)
private val AmberWarn = Color(0xFFFFC107)
private val RedDanger = Color(0xFFF44336)
private val PurpleAdvanced = Color(0xFF9C27B0)
private val CyanInfo = Color(0xFF00BCD4)

/**
 * Attack strategy recommendation card.
 * Analyzes current key state and tag type to suggest the optimal next attack.
 */
@Composable
fun AttackRecommendationCard(
    keyState: MifareKeyState,
    tagType: String?,
    executionState: FeatureExecutionState,
    connected: Boolean,
    onRunCommand: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isRunning = executionState is FeatureExecutionState.Running
    val isFudan = tagType?.contains("FM11RF08", ignoreCase = true) == true ||
                  tagType?.contains("Fudan", ignoreCase = true) == true

    // Determine recommended attack based on current state
    val recommendation = remember(keyState, isFudan) {
        getRecommendedAttack(keyState, isFudan)
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = recommendation.color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        recommendation.icon,
                        null,
                        tint = recommendation.color,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            "Recommended: ${recommendation.title}",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = recommendation.color
                        )
                        Text(
                            recommendation.reason,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = { onRunCommand(recommendation.command, recommendation.title) },
                enabled = connected && !isRunning,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = recommendation.color),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (isRunning) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Running...")
                } else {
                    Icon(Icons.Default.PlayArrow, null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Run ${recommendation.title}")
                }
            }

            // Show command for transparency
            Text(
                recommendation.command,
                fontFamily = FontFamily.Monospace,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f),
                modifier = Modifier.padding(top = 4.dp)
            )
        }
    }
}

/**
 * Full attack pipeline card showing all available attack methods with their status.
 */
@Composable
fun AttackPipelineCard(
    keyState: MifareKeyState,
    tagType: String?,
    executionState: FeatureExecutionState,
    connected: Boolean,
    onRunCommand: (String, String) -> Unit,
    modifier: Modifier = Modifier
) {
    val isRunning = executionState is FeatureExecutionState.Running
    val isFudan = tagType?.contains("FM11RF08", ignoreCase = true) == true
    val hasAnyKey = keyState.foundKeysCount > 0
    val allFound = keyState.isComplete

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                "⚔️ Attack Pipeline",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Execute attacks in order. Each step unlocks the next.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Phase 1: Dictionary
            AttackStepRow(
                stepNumber = 1,
                title = "Dictionary Attack",
                description = "Try known default keys against all sectors",
                command = "hf mf chk --1k",
                icon = Icons.AutoMirrored.Filled.MenuBook,
                status = when {
                    keyState.attackMethod == AttackMethod.DICTIONARY && isRunning -> StepStatus.RUNNING
                    hasAnyKey -> StepStatus.COMPLETE
                    else -> StepStatus.READY
                },
                enabled = connected && !isRunning && !allFound,
                onRun = { onRunCommand("hf mf chk --1k", "Dictionary Attack") }
            )

            // Phase 2: Fudan Recovery (conditional)
            if (isFudan) {
                AttackStepRow(
                    stepNumber = 2,
                    title = "Fudan FM11RF08S Recovery",
                    description = "Exploit hardware backdoor for instant key extraction",
                    command = "script run fm11rf08s_recovery",
                    icon = Icons.Default.FlashOn,
                    status = when {
                        allFound -> StepStatus.COMPLETE
                        !hasAnyKey -> StepStatus.LOCKED
                        else -> StepStatus.READY
                    },
                    enabled = connected && !isRunning && !allFound,
                    highlight = true,
                    onRun = { onRunCommand("hf mf chk --1k", "Fudan Recovery") }
                )
            }

            // Phase 3: Nested Attack
            AttackStepRow(
                stepNumber = if (isFudan) 3 else 2,
                title = "Nested Attack",
                description = "Use one known key to derive others (requires ≥1 key)",
                command = "hf mf nested --1k --blk 0 -a -k FFFFFFFFFFFF",
                icon = Icons.Default.AccountTree,
                status = when {
                    keyState.attackMethod == AttackMethod.NESTED && isRunning -> StepStatus.RUNNING
                    allFound -> StepStatus.COMPLETE
                    !hasAnyKey -> StepStatus.LOCKED
                    else -> StepStatus.READY
                },
                enabled = connected && !isRunning && hasAnyKey && !allFound,
                onRun = {
                    // Use first found key for nested attack
                    val knownSector = keyState.sectors.firstOrNull {
                        it.keyAStatus == KeyStatus.FOUND && it.keyA != null
                    }
                    val key = knownSector?.keyA ?: "FFFFFFFFFFFF"
                    val blk = (knownSector?.sectorNumber ?: 0) * 4
                    onRunCommand("hf mf nested --1k --blk $blk -a -k $key", "Nested Attack")
                }
            )

            // Phase 4: Hardnested (for stubborn keys)
            AttackStepRow(
                stepNumber = if (isFudan) 4 else 3,
                title = "Hardnested Attack",
                description = "Advanced attack for non-weak PRNG cards (slower)",
                command = "hf mf hardnested --blk 0 -a -k FFFFFFFFFFFF --tblk 28 --tb",
                icon = Icons.Default.Security,
                status = when {
                    keyState.attackMethod == AttackMethod.HARDNESTED && isRunning -> StepStatus.RUNNING
                    allFound -> StepStatus.COMPLETE
                    !hasAnyKey -> StepStatus.LOCKED
                    else -> StepStatus.READY
                },
                enabled = connected && !isRunning && hasAnyKey && !allFound,
                onRun = {
                    val knownSector = keyState.sectors.firstOrNull {
                        it.keyAStatus == KeyStatus.FOUND && it.keyA != null
                    }
                    val key = knownSector?.keyA ?: "FFFFFFFFFFFF"
                    val blk = (knownSector?.sectorNumber ?: 0) * 4
                    // Target first unknown sector
                    val targetSector = keyState.sectors.firstOrNull {
                        it.keyAStatus != KeyStatus.FOUND || it.keyBStatus != KeyStatus.FOUND
                    }
                    val tblk = ((targetSector?.sectorNumber ?: 7) * 4) + 3
                    onRunCommand("hf mf hardnested --blk $blk -a -k $key --tblk $tblk --ta", "Hardnested")
                }
            )

            // Phase 5: Darkside (no keys needed)
            AttackStepRow(
                stepNumber = if (isFudan) 5 else 4,
                title = "Darkside Attack",
                description = "Zero-knowledge attack — no existing keys required",
                command = "hf mf darkside",
                icon = Icons.Default.Visibility,
                status = when {
                    keyState.attackMethod == AttackMethod.DARKSIDE && isRunning -> StepStatus.RUNNING
                    allFound -> StepStatus.COMPLETE
                    else -> StepStatus.READY
                },
                enabled = connected && !isRunning && !allFound,
                onRun = { onRunCommand("hf mf darkside", "Darkside Attack") }
            )

            // All keys found summary
            if (allFound) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = GreenSuccess.copy(alpha = 0.15f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CheckCircle, null, tint = GreenSuccess)
                        Column {
                            Text(
                                "All ${keyState.totalKeysNeeded} keys recovered!",
                                fontWeight = FontWeight.Bold,
                                color = GreenSuccess
                            )
                            Text(
                                "Ready to dump, clone, or emulate.",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                            )
                        }
                    }
                }
            }
        }
    }
}

private enum class StepStatus { READY, RUNNING, COMPLETE, LOCKED }

@Suppress("unused")
@Composable
private fun AttackStepRow(
    stepNumber: Int,
    title: String,
    description: String,
    command: String,
    icon: ImageVector,
    status: StepStatus,
    enabled: Boolean,
    highlight: Boolean = false,
    onRun: () -> Unit
) {
    val stepColor = when (status) {
        StepStatus.READY -> BlueActive
        StepStatus.RUNNING -> AmberWarn
        StepStatus.COMPLETE -> GreenSuccess
        StepStatus.LOCKED -> MaterialTheme.colorScheme.outline
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Step number badge
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(
                    if (highlight && status == StepStatus.READY)
                        Brush.linearGradient(listOf(AmberWarn, RedDanger))
                    else
                        Brush.linearGradient(listOf(stepColor, stepColor)),
                    CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            when (status) {
                StepStatus.COMPLETE -> Icon(
                    Icons.Default.Check, null,
                    tint = Color.White, modifier = Modifier.size(16.dp)
                )
                StepStatus.RUNNING -> {
                    val transition = rememberInfiniteTransition(label = "step$stepNumber")
                    val alpha by transition.animateFloat(
                        initialValue = 0.5f, targetValue = 1f,
                        animationSpec = infiniteRepeatable(tween(500), RepeatMode.Reverse),
                        label = "pulse"
                    )
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp).alpha(alpha),
                        strokeWidth = 2.dp, color = Color.White
                    )
                }
                StepStatus.LOCKED -> Icon(
                    Icons.Default.Lock, null,
                    tint = Color.White.copy(alpha = 0.6f), modifier = Modifier.size(14.dp)
                )
                StepStatus.READY -> Text(
                    "$stepNumber",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        // Info column
        Column(modifier = Modifier.weight(1f)) {
            Text(
                title,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = if (status == StepStatus.LOCKED)
                    MaterialTheme.colorScheme.outline
                else
                    MaterialTheme.colorScheme.onSurface
            )
            Text(
                description,
                style = MaterialTheme.typography.bodySmall,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(
                    alpha = if (status == StepStatus.LOCKED) 0.4f else 0.6f
                )
            )
        }

        // Action button
        when (status) {
            StepStatus.READY -> {
                FilledTonalButton(
                    onClick = onRun,
                    enabled = enabled,
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(icon, null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Run", fontSize = 12.sp)
                }
            }
            StepStatus.RUNNING -> {
                OutlinedButton(
                    onClick = { /* cancel not yet supported */ },
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text("⏳", fontSize = 12.sp)
                }
            }
            StepStatus.COMPLETE -> {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = GreenSuccess.copy(alpha = 0.15f)
                ) {
                    Text(
                        "✓",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        color = GreenSuccess,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            StepStatus.LOCKED -> {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)
                ) {
                    Text(
                        "🔒",
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

/**
 * Key recovery statistics card — summary of recovered keys with export option.
 */
@Composable
fun KeyRecoveryStatsCard(
    keyState: MifareKeyState,
    modifier: Modifier = Modifier
) {
    val defaultKeyCount = keyState.sectors.count { s ->
        (s.keyA == "FFFFFFFFFFFF") || (s.keyB == "FFFFFFFFFFFF")
    }
    val customKeyCount = keyState.sectors.count { s ->
        (s.keyAStatus == KeyStatus.FOUND && s.keyA != null && s.keyA != "FFFFFFFFFFFF") ||
        (s.keyBStatus == KeyStatus.FOUND && s.keyB != null && s.keyB != "FFFFFFFFFFFF")
    }
    val uniqueKeys = keyState.sectors.flatMap { s ->
        listOfNotNull(s.keyA, s.keyB)
    }.filter { it != "FFFFFFFFFFFF" }.toSet()

    AnimatedVisibility(
        visible = keyState.foundKeysCount > 0,
        enter = fadeIn() + expandVertically(),
        exit = fadeOut() + shrinkVertically()
    ) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "📊 Recovery Statistics",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    StatBubble("Keys Found", "${keyState.foundKeysCount}", GreenSuccess)
                    StatBubble("Default", "$defaultKeyCount", CyanInfo)
                    StatBubble("Custom", "$customKeyCount", AmberWarn)
                    StatBubble("Unique", "${uniqueKeys.size}", PurpleAdvanced)
                }

                if (uniqueKeys.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "Custom Keys:",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    uniqueKeys.forEach { key ->
                        Text(
                            key,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            color = AmberWarn
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StatBubble(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = CircleShape,
            color = color.copy(alpha = 0.15f),
            modifier = Modifier.size(40.dp)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    value,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = color
                )
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(label, fontSize = 9.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
    }
}

/**
 * Access pattern analysis card — identifies which sectors are likely access control sectors.
 */
@Composable
fun AccessPatternCard(
    keyState: MifareKeyState,
    modifier: Modifier = Modifier
) {
    val interestingSectors = keyState.sectors.filter { s ->
        s.keyAStatus == KeyStatus.FOUND && s.keyA != null && s.keyA != "FFFFFFFFFFFF" ||
        s.keyBStatus == KeyStatus.FOUND && s.keyB != null && s.keyB != "FFFFFFFFFFFF"
    }

    if (interestingSectors.isEmpty() && keyState.foundKeysCount > 0) {
        // All keys are defaults — special case
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = CyanInfo.copy(alpha = 0.1f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "🔓 All Default Keys",
                    fontWeight = FontWeight.Bold,
                    color = CyanInfo
                )
                Text(
                    "Every sector uses FFFFFFFFFFFF. The reader likely relies on " +
                    "successful authentication alone (no sector-specific data check), " +
                    "or checks the UID directly.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            }
        }
    } else if (interestingSectors.isNotEmpty()) {
        Card(
            modifier = modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = AmberWarn.copy(alpha = 0.1f)
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "🎯 Access Control Sectors",
                    fontWeight = FontWeight.Bold,
                    color = AmberWarn
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "These sectors have custom keys — likely where access data lives:",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
                Spacer(modifier = Modifier.height(8.dp))

                interestingSectors.forEach { sector ->
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = AmberWarn.copy(alpha = 0.08f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    "Sector ${sector.sectorNumber}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp
                                )
                                val blockRange = "${sector.sectorNumber * 4}–${sector.sectorNumber * 4 + 3}"
                                Text(
                                    "Blocks $blockRange",
                                    fontSize = 10.sp,
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                sector.keyA?.let {
                                    Text("A: $it", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                                }
                                sector.keyB?.let {
                                    Text("B: $it", fontFamily = FontFamily.Monospace, fontSize = 10.sp)
                                }
                            }
                        }
                    }
                }

                // Gym-specific tips
                val hasCommonGymSector = interestingSectors.any { it.sectorNumber in 4..8 }
                if (hasCommonGymSector) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        "💡 Sectors 4–8 with custom keys = typical gym/access pattern. " +
                        "Reader likely authenticates here and reads 1–2 data blocks.",
                        style = MaterialTheme.typography.bodySmall,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

// ── Attack recommendation logic ──────────────────────────────────────

private data class AttackRecommendation(
    val title: String,
    val command: String,
    val reason: String,
    val icon: ImageVector,
    val color: Color
)

private fun getRecommendedAttack(
    keyState: MifareKeyState,
    isFudan: Boolean
): AttackRecommendation {
    return when {
        keyState.isComplete -> AttackRecommendation(
            title = "Dump Card",
            command = "hf mf dump",
            reason = "All keys recovered — ready to read full card contents",
            icon = Icons.Default.Download,
            color = GreenSuccess
        )
        isFudan && keyState.foundKeysCount == 0 -> AttackRecommendation(
            title = "FM11RF08S Recovery",
            command = "hf mf chk --1k",
            reason = "Fudan chip detected — backdoor enables instant key extraction",
            icon = Icons.Default.FlashOn,
            color = RedDanger
        )
        keyState.foundKeysCount == 0 -> AttackRecommendation(
            title = "Dictionary Attack",
            command = "hf mf chk --1k",
            reason = "Start with default key dictionary (fastest first step)",
            icon = Icons.AutoMirrored.Filled.MenuBook,
            color = BlueActive
        )
        keyState.foundKeysCount < keyState.totalKeysNeeded -> AttackRecommendation(
            title = "Nested Attack",
            command = buildNestedCommand(keyState),
            reason = "${keyState.foundKeysCount} keys found — use them to derive remaining keys",
            icon = Icons.Default.AccountTree,
            color = PurpleAdvanced
        )
        else -> AttackRecommendation(
            title = "Dictionary Attack",
            command = "hf mf chk --1k",
            reason = "Try default keys first",
            icon = Icons.AutoMirrored.Filled.MenuBook,
            color = BlueActive
        )
    }
}

private fun buildNestedCommand(keyState: MifareKeyState): String {
    val knownSector = keyState.sectors.firstOrNull {
        it.keyAStatus == KeyStatus.FOUND && it.keyA != null
    }
    val key = knownSector?.keyA ?: "FFFFFFFFFFFF"
    val blk = (knownSector?.sectorNumber ?: 0) * 4
    return "hf mf nested --1k --blk $blk -a -k $key"
}
