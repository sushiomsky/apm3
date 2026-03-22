package com.sushiomsky.proxmark.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.sushiomsky.proxmark.mifare.*

// Sector status colors matching professional RFID tools
private val ColorComplete = Color(0xFF4CAF50)    // Green — both keys found
private val ColorPartial = Color(0xFFFFC107)      // Amber — one key found
private val ColorAttacking = Color(0xFF2196F3)    // Blue — in progress
private val ColorLocked = Color(0xFFF44336)       // Red — attack failed
private val ColorUnknown = Color(0xFF424242)       // Dark gray — not yet attempted

/**
 * MIFARE Classic sector map — visual grid showing key recovery status per sector.
 * 4 columns × N rows grid, each cell color-coded by key status.
 */
@Composable
fun SectorMapGrid(
    keyState: MifareKeyState,
    onSectorClick: ((Int) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val sectorCount = keyState.sectorCount()
    val columns = if (sectorCount <= 5) 5 else 4
    val rows = (sectorCount + columns - 1) / columns

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Legend
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            LegendItem(ColorComplete, "Found")
            LegendItem(ColorPartial, "Partial")
            LegendItem(ColorAttacking, "Attacking")
            LegendItem(ColorLocked, "Locked")
            LegendItem(ColorUnknown, "Unknown")
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Sector grid
        for (row in 0 until rows) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                for (col in 0 until columns) {
                    val sectorIdx = row * columns + col
                    if (sectorIdx < sectorCount) {
                        SectorCell(
                            sector = keyState.sectors[sectorIdx],
                            isCurrent = sectorIdx == keyState.currentSector,
                            onClick = if (onSectorClick != null) {
                                { onSectorClick(sectorIdx) }
                            } else null,
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SectorCell(
    sector: SectorKeyState,
    isCurrent: Boolean,
    onClick: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val baseColor = when (sector.overallStatus) {
        SectorStatus.COMPLETE -> ColorComplete
        SectorStatus.PARTIAL -> ColorPartial
        SectorStatus.ATTACKING -> ColorAttacking
        SectorStatus.LOCKED -> ColorLocked
        SectorStatus.UNKNOWN -> ColorUnknown
    }

    val animatedColor by animateColorAsState(
        targetValue = baseColor,
        animationSpec = tween(300),
        label = "sectorColor"
    )

    // Pulse animation for currently attacking sector
    val pulseAlpha = if (isCurrent && sector.overallStatus == SectorStatus.ATTACKING) {
        val transition = rememberInfiniteTransition(label = "pulse")
        transition.animateFloat(
            initialValue = 0.6f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(600), RepeatMode.Reverse),
            label = "pulseAlpha"
        ).value
    } else {
        1f
    }

    val borderMod = if (isCurrent) {
        Modifier.border(2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(6.dp))
    } else {
        Modifier
    }

    Surface(
        modifier = modifier
            .aspectRatio(1.2f)
            .then(borderMod)
            .alpha(pulseAlpha)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier),
        shape = RoundedCornerShape(6.dp),
        color = animatedColor
    ) {
        Column(
            modifier = Modifier.padding(2.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Sector number
            Text(
                text = "${sector.sectorNumber}",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            // Key indicators: A/B dots
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                KeyDot("A", sector.keyAStatus)
                KeyDot("B", sector.keyBStatus)
            }
        }
    }
}

@Composable
private fun KeyDot(label: String, status: KeyStatus) {
    val color = when (status) {
        KeyStatus.FOUND -> ColorComplete
        KeyStatus.IN_PROGRESS -> ColorAttacking
        KeyStatus.FAILED -> ColorLocked
        KeyStatus.UNKNOWN -> Color.White.copy(alpha = 0.3f)
    }
    Text(
        text = label,
        fontSize = 7.sp,
        fontWeight = FontWeight.Bold,
        color = color
    )
}

@Composable
private fun LegendItem(color: Color, label: String) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(color, RoundedCornerShape(2.dp))
        )
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontSize = 9.sp
        )
    }
}

/**
 * Detailed sector info panel — shown when a sector cell is tapped.
 */
@Composable
fun SectorDetailPanel(
    sector: SectorKeyState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                "Sector ${sector.sectorNumber}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                KeyInfoRow("Key A", sector.keyA, sector.keyAStatus)
                KeyInfoRow("Key B", sector.keyB, sector.keyBStatus)
            }
        }
    }
}

@Composable
private fun KeyInfoRow(label: String, key: String?, status: KeyStatus) {
    Column {
        Text(
            label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.SemiBold
        )
        val displayKey = when {
            key != null -> key
            status == KeyStatus.IN_PROGRESS -> "Attacking..."
            status == KeyStatus.FAILED -> "Failed"
            else -> "Unknown"
        }
        val color = when (status) {
            KeyStatus.FOUND -> ColorComplete
            KeyStatus.IN_PROGRESS -> ColorAttacking
            KeyStatus.FAILED -> ColorLocked
            KeyStatus.UNKNOWN -> MaterialTheme.colorScheme.outline
        }
        Text(
            displayKey,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            color = color
        )
    }
}

/**
 * Key recovery progress header — shows attack method, progress bar, and stats.
 */
@Composable
fun KeyRecoveryProgress(
    keyState: MifareKeyState,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (keyState.isComplete)
                MaterialTheme.colorScheme.tertiaryContainer
            else
                MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    if (keyState.isComplete) "✅ Recovery Complete"
                    else if (keyState.isRunning) "🔑 ${keyState.attackMethod.label}"
                    else "Key Recovery",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${keyState.foundKeysCount}/${keyState.totalKeysNeeded} keys",
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            LinearProgressIndicator(
                progress = { keyState.progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp),
                color = if (keyState.isComplete) ColorComplete else ColorAttacking,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )

            if (keyState.currentSector >= 0 && keyState.isRunning) {
                Text(
                    "Attacking sector ${keyState.currentSector}...",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.7f)
                )
            }
        }
    }
}

/**
 * Complete key recovery panel combining progress + sector map + detail.
 */
@Composable
fun KeyRecoveryPanel(
    keyState: MifareKeyState,
    selectedSector: Int?,
    onSectorClick: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        KeyRecoveryProgress(keyState = keyState)

        SectorMapGrid(
            keyState = keyState,
            onSectorClick = onSectorClick
        )

        if (selectedSector != null && selectedSector in keyState.sectors.indices) {
            SectorDetailPanel(sector = keyState.sectors[selectedSector])
        }
    }
}
