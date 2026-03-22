package com.example.proxmark.ui

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.proxmark.features.FeatureRunOutcome
import com.example.proxmark.features.ProxmarkFeature
import com.example.proxmark.features.ProxmarkFeatureCategory
import com.example.proxmark.mifare.MifareWorkflow
import com.example.proxmark.persistence.ThemeMode
import com.example.proxmark.reader.ReaderConnectionState
import com.example.proxmark.reader.ReaderType
import com.example.proxmark.repository.TagInfo
import com.example.proxmark.ui.components.*
import com.example.proxmark.usb.UsbConnectionState
import com.example.proxmark.viewmodel.MainUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    modifier: Modifier = Modifier,
    uiState: MainUiState,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onReconnect: () -> Unit,
    onRunFeature: (ProxmarkFeature, Map<String, String>) -> Unit,
    onClearError: () -> Unit,
    onClearSavedTags: () -> Unit,
    onClearFeatureHistory: () -> Unit,
    onClearFeatureExecutionState: () -> Unit,
    onSetSkipConfirmations: (Boolean) -> Unit = {},
    onSetTagSearchQuery: (String) -> Unit = {},
    onUpdateTimeout: (Int) -> Unit = {},
    onToggleFavorite: (String) -> Unit = {},
    onUpdateThemeMode: (ThemeMode) -> Unit = {},
    onSetAutoScanEnabled: (Boolean) -> Unit = {},
    onUpdateAutoScanInterval: (Int) -> Unit = {},
    onUpdateTagNote: (String, String, String?) -> Unit = { _, _, _ -> },
    onSetActiveReader: (ReaderType) -> Unit = {},
    onReadAcrTag: () -> Unit = {},
    onSetFeatureSearchQuery: (String) -> Unit = {},
    onToggleRawCommandMode: () -> Unit = {},
    onSendRawCommand: (String) -> Unit = {},
    onToggleTagSelectionMode: () -> Unit = {},
    onToggleTagSelected: (String) -> Unit = {},
    onDeleteSelectedTags: () -> Unit = {},
    onCreateTagGroup: (String) -> Unit = {},
    onDeleteTagGroup: (String) -> Unit = {},
    onAddTagToGroup: (String, String) -> Unit = { _, _ -> },
    onRemoveTagFromGroup: (String, String) -> Unit = { _, _ -> },
    onParseDump: (String) -> Unit = {},
    onCloseDumpViewer: () -> Unit = {},
    onSetDump1: (String) -> Unit = {},
    onSetDump2: (String) -> Unit = {},
    onClearDumpComparison: () -> Unit = {},
    onCloseDumpComparison: () -> Unit = {},
    onShowWorkflow: (MifareWorkflow) -> Unit = {},
    onCloseWorkflowViewer: () -> Unit = {},
    onGetUsbDebugInfo: () -> String = { "" }
) {
    val connected = uiState.connectionState is UsbConnectionState.Connected
    val nfcConnected = uiState.readerStates[ReaderType.BUILTIN_NFC] is ReaderConnectionState.Connected
    val groupedFeatures = uiState.availableFeatures.groupBy { it.category }
    val categories = ProxmarkFeatureCategory.entries.filter { groupedFeatures[it].orEmpty().isNotEmpty() }
    val quickFeatures = groupedFeatures[ProxmarkFeatureCategory.QUICK_START].orEmpty()
    val favoriteIds = uiState.settings.favoriteFeatureIds
    val favoriteFeatures = uiState.availableFeatures.filter { it.id in favoriteIds }
    var pendingConfirmationFeature by remember { mutableStateOf<ProxmarkFeature?>(null) }
    var pendingParameterFeature by remember { mutableStateOf<ProxmarkFeature?>(null) }
    var selectedTag by remember { mutableStateOf<TagInfo?>(null) }
    var showGroupDialog by remember { mutableStateOf(false) }

    val maybeConfirmThenRun: (ProxmarkFeature) -> Unit = { feature ->
        if (feature.parameters.isNotEmpty()) {
            // Features with parameters always show parameter dialog first
            pendingParameterFeature = feature
        } else if (feature.safetyLevel.requiresConfirmation && !uiState.skipConfirmations) {
            pendingConfirmationFeature = feature
        } else {
            onRunFeature(feature, emptyMap())
        }
    }

    val isFeatureEnabled: (ProxmarkFeature) -> Boolean = { feature ->
        when (feature.targetReader) {
            ReaderType.BUILTIN_NFC -> nfcConnected
            ReaderType.ACR_USB -> uiState.readerStates[ReaderType.ACR_USB] is ReaderConnectionState.Connected
            else -> connected
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "PM3 Companion",
                style = MaterialTheme.typography.headlineSmall
            )
        }

        // Reader selector
        item {
            ReaderSelectorCard(
                activeReader = uiState.activeReaderType,
                readerStates = uiState.readerStates,
                onSelectReader = onSetActiveReader
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    val activeState = uiState.readerStates[uiState.activeReaderType]
                    Text(text = "${uiState.activeReaderType.label} status")
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = readerStateText(activeState),
                        color = readerStateColor(activeState)
                    )
                }
            }
        }

        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onConnect,
                    modifier = Modifier.weight(1f),
                    enabled = !connected && uiState.connectionState !is UsbConnectionState.Connecting
                ) {
                    Text("Connect")
                }
                OutlinedButton(
                    onClick = onDisconnect,
                    modifier = Modifier.weight(1f),
                    enabled = connected
                ) {
                    Text("Disconnect")
                }
                OutlinedButton(
                    onClick = onReconnect,
                    modifier = Modifier.weight(1f),
                    enabled = uiState.connectionState !is UsbConnectionState.Connecting
                ) {
                    Text("Reconnect")
                }
            }
        }

        // ACR manual read button
        if (uiState.activeReaderType == ReaderType.ACR_USB) {
            item {
                val acrConnected = uiState.readerStates[ReaderType.ACR_USB] is ReaderConnectionState.Connected
                Button(
                    onClick = onReadAcrTag,
                    enabled = acrConnected,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Read card on ACR reader")
                }
            }
        }

        if (uiState.skipConfirmations) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF3E0))
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = androidx.compose.ui.Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⚠ Confirmations disabled for this session",
                            style = MaterialTheme.typography.bodySmall,
                            color = Color(0xFFE65100)
                        )
                        OutlinedButton(onClick = { onSetSkipConfirmations(false) }) {
                            Text("Re-enable", style = MaterialTheme.typography.bodySmall)
                        }
                    }
                }
            }
        }

        if (favoriteFeatures.isNotEmpty()) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "★ Favorites", style = MaterialTheme.typography.titleMedium)
                        favoriteFeatures.forEach { feature ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Button(
                                    onClick = { maybeConfirmThenRun(feature) },
                                    enabled = connected,
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Text(feature.title)
                                }
                                TextButton(onClick = { onToggleFavorite(feature.id) }) {
                                    Text("★")
                                }
                            }
                        }
                    }
                }
            }
        }

        item {
            if (quickFeatures.isNotEmpty()) {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(text = "Quick actions", style = MaterialTheme.typography.titleMedium)
                        quickFeatures.forEach { feature ->
                            Button(
                                onClick = { maybeConfirmThenRun(feature) },
                                enabled = connected,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(feature.title)
                            }
                        }
                    }
                }
            }
        }

        item {
            FeatureExecutionCard(
                state = uiState.featureExecutionState,
                onDismiss = onClearFeatureExecutionState
            )
        }

        // Raw command mode (hidden by default, toggled via settings long-press)
        if (uiState.rawCommandMode) {
            item {
                RawCommandCard(
                    enabled = connected && uiState.activeReaderType == ReaderType.PROXMARK3,
                    onSend = onSendRawCommand,
                    onClose = onToggleRawCommandMode
                )
            }
        }

        // MIFARE Classic Analysis
        item {
            var dumpInput by remember { mutableStateOf("") }
            var showInput by remember { mutableStateOf(false) }
            var showCompareMode by remember { mutableStateOf(false) }
            
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("MIFARE Classic Analysis", style = MaterialTheme.typography.titleMedium)
                        TextButton(onClick = { showCompareMode = !showCompareMode }) {
                            Text(if (showCompareMode) "View Mode" else "Compare Mode")
                        }
                    }
                    
                    if (!showCompareMode) {
                        // Single dump analysis mode
                        Text(
                            "Analyze PM3 dump files. Paste hex output from 'hf mf dump'.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        if (showInput) {
                            OutlinedTextField(
                                value = dumpInput,
                                onValueChange = { dumpInput = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Paste hex dump (2048 chars for 1K card)...") },
                                minLines = 3,
                                maxLines = 6
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = {
                                        if (dumpInput.isNotBlank()) {
                                            onParseDump(dumpInput)
                                            dumpInput = ""
                                            showInput = false
                                        }
                                    },
                                    enabled = dumpInput.length >= 2048
                                ) {
                                    Text("Analyze")
                                }
                                OutlinedButton(onClick = { showInput = false }) {
                                    Text("Cancel")
                                }
                            }
                        } else {
                            Button(
                                onClick = { showInput = true },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Paste & Analyze Dump")
                            }
                        }
                    } else {
                        // Compare mode
                        Text(
                            "Compare two dumps to detect changes (e.g., before/after locker use).",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        
                        var dump1Input by remember { mutableStateOf("") }
                        var dump2Input by remember { mutableStateOf("") }
                        var showDump1Input by remember { mutableStateOf(false) }
                        var showDump2Input by remember { mutableStateOf(false) }
                        
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Dump 1
                            Column(modifier = Modifier.weight(1f)) {
                                if (showDump1Input) {
                                    OutlinedTextField(
                                        value = dump1Input,
                                        onValueChange = { dump1Input = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("Dump 1...") },
                                        minLines = 2,
                                        maxLines = 4
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Button(
                                            onClick = {
                                                onSetDump1(dump1Input)
                                                showDump1Input = false
                                            },
                                            enabled = dump1Input.length >= 2048,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Set", style = MaterialTheme.typography.labelSmall)
                                        }
                                        OutlinedButton(
                                            onClick = { showDump1Input = false },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Cancel", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { showDump1Input = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            if (uiState.mifareDump1 != null) "Dump 1 ✓" else "Paste Dump 1",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                            
                            // Dump 2
                            Column(modifier = Modifier.weight(1f)) {
                                if (showDump2Input) {
                                    OutlinedTextField(
                                        value = dump2Input,
                                        onValueChange = { dump2Input = it },
                                        modifier = Modifier.fillMaxWidth(),
                                        placeholder = { Text("Dump 2...") },
                                        minLines = 2,
                                        maxLines = 4
                                    )
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Button(
                                            onClick = {
                                                onSetDump2(dump2Input)
                                                showDump2Input = false
                                            },
                                            enabled = dump2Input.length >= 2048,
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Set", style = MaterialTheme.typography.labelSmall)
                                        }
                                        OutlinedButton(
                                            onClick = { showDump2Input = false },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text("Cancel", style = MaterialTheme.typography.labelSmall)
                                        }
                                    }
                                } else {
                                    OutlinedButton(
                                        onClick = { showDump2Input = true },
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            if (uiState.mifareDump2 != null) "Dump 2 ✓" else "Paste Dump 2",
                                            style = MaterialTheme.typography.labelSmall
                                        )
                                    }
                                }
                            }
                        }
                        
                        if (uiState.mifareDump1 != null || uiState.mifareDump2 != null) {
                            OutlinedButton(
                                onClick = onClearDumpComparison,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Clear Both Dumps")
                            }
                        }
                    }
                }
            }
        }

        // MIFARE Workflows
        item {
            MifareWorkflowsCard(
                onSelectWorkflow = onShowWorkflow
            )
        }

        item {
            uiState.errorMessage?.let { error ->
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "Error", color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = error)
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedButton(onClick = onClearError) {
                            Text("Dismiss")
                        }
                    }
                }
            }
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(text = "Last scan", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    val scan = uiState.lastScan
                    if (scan == null) {
                        Text("No tag detected yet")
                    } else {
                        TagInfoRow(tag = scan, onClick = { selectedTag = scan })
                    }
                }
            }
        }

        item {
            val clipboardManager = LocalClipboardManager.current
            val query = uiState.tagSearchQuery
            val filteredTags = if (query.isBlank()) {
                uiState.savedTags
            } else {
                uiState.savedTags.filter { tag ->
                    tag.uid.contains(query, ignoreCase = true) ||
                        tag.type.contains(query, ignoreCase = true)
                }
            }
            val selectionMode = uiState.tagSelectionMode
            val selectedIds = uiState.selectedTagIds
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "Recent tags (${uiState.savedTags.size})", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (selectionMode && selectedIds.isNotEmpty()) {
                                Button(
                                    onClick = onDeleteSelectedTags,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) {
                                    Text("Delete (${selectedIds.size})")
                                }
                            }
                            if (uiState.savedTags.isNotEmpty()) {
                                OutlinedButton(onClick = onToggleTagSelectionMode) {
                                    Text(if (selectionMode) "Cancel" else "Select")
                                }
                                if (!selectionMode) {
                                    OutlinedButton(onClick = {
                                        val json = exportTagsAsJson(uiState.savedTags)
                                        clipboardManager.setText(AnnotatedString(json))
                                    }) {
                                        Text("Export")
                                    }
                                }
                            }
                            if (!selectionMode) {
                                OutlinedButton(
                                    onClick = onClearSavedTags,
                                    enabled = uiState.savedTags.isNotEmpty()
                                ) {
                                    Text("Clear")
                                }
                            }
                        }
                    }
                    if (uiState.savedTags.size > 3) {
                        OutlinedTextField(
                            value = query,
                            onValueChange = onSetTagSearchQuery,
                            label = { Text("Search tags") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                    if (filteredTags.isEmpty()) {
                        Text(if (query.isBlank()) "No tags saved" else "No matching tags")
                    } else {
                        filteredTags.forEach { tag ->
                            val tagKey = "${tag.uid}|${tag.type}"
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (selectionMode) {
                                    Checkbox(
                                        checked = tagKey in selectedIds,
                                        onCheckedChange = { onToggleTagSelected(tagKey) }
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    TagInfoRow(tag = tag, onClick = {
                                        if (selectionMode) onToggleTagSelected(tagKey)
                                        else selectedTag = tag
                                    })
                                }
                            }
                        }
                    }
                }
            }
        }

        // Tag groups section
        if (uiState.tagGroups.isNotEmpty() || uiState.savedTags.isNotEmpty()) {
            item {
                TagGroupsSection(
                    groups = uiState.tagGroups,
                    savedTags = uiState.savedTags,
                    onCreateGroup = { showGroupDialog = true },
                    onDeleteGroup = onDeleteTagGroup,
                    onRemoveTagFromGroup = onRemoveTagFromGroup,
                    onTagClick = { tag -> selectedTag = tag }
                )
            }
        }

        // Feature search
        item {
            val featureQuery = uiState.featureSearchQuery
            OutlinedTextField(
                value = featureQuery,
                onValueChange = onSetFeatureSearchQuery,
                label = { Text("Search features…") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        }

        val featureQuery = uiState.featureSearchQuery
        val filteredCategories = if (featureQuery.isBlank()) {
            categories.filter { it != ProxmarkFeatureCategory.QUICK_START }
        } else {
            categories.filter { cat ->
                cat != ProxmarkFeatureCategory.QUICK_START &&
                    groupedFeatures[cat].orEmpty().any { f ->
                        f.title.contains(featureQuery, ignoreCase = true) ||
                            f.description.contains(featureQuery, ignoreCase = true)
                    }
            }
        }

        items(filteredCategories, key = { it.name }) { category ->
            val features = if (featureQuery.isBlank()) {
                groupedFeatures[category].orEmpty()
            } else {
                groupedFeatures[category].orEmpty().filter { f ->
                    f.title.contains(featureQuery, ignoreCase = true) ||
                        f.description.contains(featureQuery, ignoreCase = true)
                }
            }
            FeatureCategoryCard(
                category = category,
                features = features,
                isFeatureEnabled = isFeatureEnabled,
                onRunFeature = maybeConfirmThenRun,
                favoriteIds = favoriteIds,
                onToggleFavorite = onToggleFavorite
            )
        }

        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Recent operations", style = MaterialTheme.typography.titleMedium)
                        OutlinedButton(
                            onClick = onClearFeatureHistory,
                            enabled = uiState.featureRunHistory.isNotEmpty()
                        ) {
                            Text("Clear")
                        }
                    }
                    if (uiState.featureRunHistory.isEmpty()) {
                        Text("No operations run yet")
                    } else {
                        uiState.featureRunHistory.take(10).forEach { run ->
                            val color = if (run.outcome == FeatureRunOutcome.SUCCESS) {
                                Color(0xFF2E7D32)
                            } else {
                                MaterialTheme.colorScheme.error
                            }
                            val relativeTime = DateUtils.getRelativeTimeSpanString(
                                run.timestamp,
                                System.currentTimeMillis(),
                                DateUtils.SECOND_IN_MILLIS
                            )
                            Column {
                                Text(
                                    text = "${run.title}: ${run.detail}",
                                    color = color,
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = relativeTime.toString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            SettingsSection(
                settings = uiState.settings,
                connected = connected,
                autoScanEnabled = uiState.autoScanEnabled,
                rawCommandMode = uiState.rawCommandMode,
                onUpdateTimeout = onUpdateTimeout,
                onUpdateThemeMode = onUpdateThemeMode,
                onSetAutoScanEnabled = onSetAutoScanEnabled,
                onUpdateAutoScanInterval = onUpdateAutoScanInterval,
                onToggleRawCommandMode = onToggleRawCommandMode,
                onShowUsbDebugInfo = onGetUsbDebugInfo
            )
        }
    }

    pendingConfirmationFeature?.let { feature ->
        FeatureConfirmationDialog(
            feature = feature,
            onConfirm = {
                onRunFeature(feature, emptyMap())
                pendingConfirmationFeature = null
            },
            onDismiss = { pendingConfirmationFeature = null },
            onSkipFutureConfirmations = {
                onSetSkipConfirmations(true)
                onRunFeature(feature, emptyMap())
                pendingConfirmationFeature = null
            }
        )
    }

    pendingParameterFeature?.let { feature ->
        FeatureParameterDialog(
            feature = feature,
            onRun = { paramValues ->
                if (feature.safetyLevel.requiresConfirmation && !uiState.skipConfirmations) {
                    // Show confirmation after collecting params — stash values
                    pendingParameterFeature = null
                    pendingConfirmationFeature = feature
                    // Run with params directly (safety already reviewed in parameter dialog)
                    onRunFeature(feature, paramValues)
                } else {
                    onRunFeature(feature, paramValues)
                }
                pendingParameterFeature = null
            },
            onDismiss = { pendingParameterFeature = null }
        )
    }

    if (showGroupDialog) {
        CreateGroupDialog(
            onConfirm = { name ->
                onCreateTagGroup(name)
                showGroupDialog = false
            },
            onDismiss = { showGroupDialog = false }
        )
    }

    selectedTag?.let { tag ->
        TagDetailSheet(
            tag = tag,
            groups = uiState.tagGroups,
            onDismiss = { selectedTag = null },
            onUpdateNote = { note -> onUpdateTagNote(tag.uid, tag.type, note) },
            onAddToGroup = { groupId -> onAddTagToGroup(groupId, "${tag.uid}|${tag.type}") },
            onRemoveFromGroup = { groupId -> onRemoveTagFromGroup(groupId, "${tag.uid}|${tag.type}") }
        )
    }

    if (uiState.showDumpViewer && uiState.mifareDumpToView != null) {
        DumpViewerDialog(
            dump = uiState.mifareDumpToView,
            onDismiss = onCloseDumpViewer
        )
    }

    if (uiState.showDumpComparison && uiState.dumpComparison != null) {
        DumpComparisonDialog(
            comparison = uiState.dumpComparison,
            onDismiss = onCloseDumpComparison
        )
    }

    if (uiState.showWorkflowViewer && uiState.workflowToView != null) {
        WorkflowViewerDialog(
            workflow = uiState.workflowToView,
            onDismiss = onCloseWorkflowViewer,
            onRunFeature = { featureId ->
                // TODO: Look up feature by ID and run it
            }
        )
    }
}


