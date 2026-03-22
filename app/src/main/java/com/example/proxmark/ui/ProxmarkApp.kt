package com.example.proxmark.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.proxmark.features.ProxmarkFeature
import com.example.proxmark.features.ProxmarkFeatureCategory
import com.example.proxmark.reader.ReaderConnectionState
import com.example.proxmark.reader.ReaderType
import com.example.proxmark.ui.components.DumpComparisonDialog
import com.example.proxmark.ui.components.DumpViewerDialog
import com.example.proxmark.ui.components.FeatureConfirmationDialog
import com.example.proxmark.ui.components.FeatureParameterDialog
import com.example.proxmark.ui.components.GymLockerWorkflowDialog
import com.example.proxmark.ui.components.KeyRecoveryDialog
import com.example.proxmark.ui.components.RawCommandCard
import com.example.proxmark.ui.components.WorkflowViewerDialog
import com.example.proxmark.ui.components.AdvancedModePanel
import com.example.proxmark.usb.UsbConnectionState
import com.example.proxmark.viewmodel.MainViewModel

private enum class AppTab(val label: String, val iconName: String) {
    HOME("Home", "home"),
    TOOLS("Tools", "build"),
    TAGS("Tags", "list"),
    TERMINAL("Terminal", "terminal")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProxmarkApp(
    viewModel: MainViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }
    var pendingConfirmationFeature by remember { mutableStateOf<ProxmarkFeature?>(null) }
    var pendingParameterFeature by remember { mutableStateOf<ProxmarkFeature?>(null) }

    val connected = uiState.connectionState is UsbConnectionState.Connected
    val nfcConnected = uiState.readerStates[ReaderType.BUILTIN_NFC] is ReaderConnectionState.Connected
    val favoriteIds = uiState.settings.favoriteFeatureIds
    val groupedFeatures = uiState.availableFeatures.groupBy { it.category }
    val quickFeatures = groupedFeatures[ProxmarkFeatureCategory.QUICK_START].orEmpty()
    val favoriteFeatures = uiState.availableFeatures.filter { it.id in favoriteIds }

    val maybeConfirmThenRun: (ProxmarkFeature) -> Unit = { feature ->
        if (feature.parameters.isNotEmpty()) {
            pendingParameterFeature = feature
        } else if (feature.safetyLevel.requiresConfirmation && !uiState.skipConfirmations) {
            pendingConfirmationFeature = feature
        } else {
            viewModel.runFeature(feature)
        }
    }

    val isFeatureEnabled: (ProxmarkFeature) -> Boolean = { feature ->
        when (feature.targetReader) {
            ReaderType.BUILTIN_NFC -> nfcConnected
            ReaderType.ACR_USB -> uiState.readerStates[ReaderType.ACR_USB] is ReaderConnectionState.Connected
            else -> connected
        }
    }

    val tabs = AppTab.entries

    Scaffold(
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        icon = {
                            when (tab) {
                                AppTab.HOME -> Icon(Icons.Default.Home, contentDescription = tab.label)
                                AppTab.TOOLS -> Icon(Icons.Default.Build, contentDescription = tab.label)
                                AppTab.TAGS -> Icon(Icons.Default.List, contentDescription = tab.label)
                                AppTab.TERMINAL -> Icon(Icons.Default.Menu, contentDescription = tab.label)
                            }
                        },
                        label = { Text(tab.label) },
                        selected = selectedTab == index,
                        onClick = { selectedTab = index }
                    )
                }
            }
        }
    ) { innerPadding ->
        val contentModifier = Modifier.padding(innerPadding)

        when (tabs[selectedTab]) {
            AppTab.HOME -> HomeScreen(
                connectionState = uiState.connectionState,
                activeReaderType = uiState.activeReaderType,
                readerStates = uiState.readerStates,
                featureExecutionState = uiState.featureExecutionState,
                featureRunHistory = uiState.featureRunHistory,
                quickFeatures = quickFeatures,
                favoriteFeatures = favoriteFeatures,
                lastScan = uiState.lastScan,
                skipConfirmations = uiState.skipConfirmations,
                settings = uiState.settings,
                autoScanEnabled = uiState.autoScanEnabled,
                rawCommandMode = uiState.rawCommandMode,
                onConnect = viewModel::connect,
                onDisconnect = viewModel::disconnect,
                onReconnect = viewModel::reconnect,
                onRunFeature = maybeConfirmThenRun,
                onSetActiveReader = viewModel::setActiveReader,
                onReadAcrTag = viewModel::readAcrTag,
                onClearFeatureExecutionState = viewModel::clearFeatureExecutionState,
                onClearFeatureHistory = viewModel::clearFeatureHistory,
                onSetSkipConfirmations = viewModel::setSkipConfirmations,
                onUpdateTimeout = viewModel::updateTimeout,
                onUpdateThemeMode = viewModel::updateThemeMode,
                onSetAutoScanEnabled = viewModel::setAutoScanEnabled,
                onUpdateAutoScanInterval = viewModel::updateAutoScanInterval,
                onToggleRawCommandMode = viewModel::toggleRawCommandMode,
                onGetUsbDebugInfo = viewModel::getUsbDeviceDebugInfo,
                modifier = contentModifier
            )

            AppTab.TOOLS -> ToolsScreen(
                features = uiState.availableFeatures,
                featureSearchQuery = uiState.featureSearchQuery,
                connected = connected,
                favoriteIds = favoriteIds,
                mifareDump1 = uiState.mifareDump1,
                mifareDump2 = uiState.mifareDump2,
                advancedMode = uiState.advancedMode,
                isFeatureEnabled = isFeatureEnabled,
                onRunFeature = maybeConfirmThenRun,
                onSetFeatureSearchQuery = viewModel::setFeatureSearchQuery,
                onToggleFavorite = viewModel::toggleFavorite,
                onParseDump = viewModel::parseDumpFromHex,
                onSetDump1 = viewModel::setDump1,
                onSetDump2 = viewModel::setDump2,
                onClearDumpComparison = viewModel::clearDumpComparison,
                onShowWorkflow = viewModel::showWorkflow,
                onShowKeyRecovery = viewModel::showKeyRecovery,
                onShowGymLockerWorkflow = viewModel::showGymLockerWorkflow,
                onToggleAdvancedMode = viewModel::toggleAdvancedMode,
                modifier = contentModifier
            )

            AppTab.TAGS -> TagsScreen(
                savedTags = uiState.savedTags,
                tagGroups = uiState.tagGroups,
                lastScan = uiState.lastScan,
                tagSearchQuery = uiState.tagSearchQuery,
                tagSelectionMode = uiState.tagSelectionMode,
                selectedTagIds = uiState.selectedTagIds,
                onSetTagSearchQuery = viewModel::setTagSearchQuery,
                onClearSavedTags = viewModel::clearSavedTags,
                onToggleTagSelectionMode = viewModel::toggleTagSelectionMode,
                onToggleTagSelected = viewModel::toggleTagSelected,
                onDeleteSelectedTags = viewModel::deleteSelectedTags,
                onCreateTagGroup = viewModel::createTagGroup,
                onDeleteTagGroup = viewModel::deleteTagGroup,
                onAddTagToGroup = viewModel::addTagToGroup,
                onRemoveTagFromGroup = viewModel::removeTagFromGroup,
                onUpdateTagNote = viewModel::updateTagNote,
                modifier = contentModifier
            )

            AppTab.TERMINAL -> TerminalScreen(
                terminalLines = uiState.terminalLines,
                connected = connected,
                advancedMode = uiState.advancedMode,
                onSendCommand = viewModel::sendRawCommand,
                onClear = viewModel::clearTerminal,
                onShowAdvancedPanel = viewModel::showAdvancedPanel,
                modifier = contentModifier
            )
        }
    }

    // ── Global dialogs ──────────────────────────────────────────────

    pendingConfirmationFeature?.let { feature ->
        FeatureConfirmationDialog(
            feature = feature,
            onConfirm = {
                viewModel.runFeature(feature)
                pendingConfirmationFeature = null
            },
            onDismiss = { pendingConfirmationFeature = null },
            onSkipFutureConfirmations = {
                viewModel.setSkipConfirmations(true)
                viewModel.runFeature(feature)
                pendingConfirmationFeature = null
            }
        )
    }

    pendingParameterFeature?.let { feature ->
        FeatureParameterDialog(
            feature = feature,
            onRun = { paramValues ->
                viewModel.runFeature(feature, paramValues)
                pendingParameterFeature = null
            },
            onDismiss = { pendingParameterFeature = null }
        )
    }

    if (uiState.showDumpViewer && uiState.mifareDumpToView != null) {
        DumpViewerDialog(dump = uiState.mifareDumpToView!!, onDismiss = viewModel::closeDumpViewer)
    }

    if (uiState.showDumpComparison && uiState.dumpComparison != null) {
        DumpComparisonDialog(comparison = uiState.dumpComparison!!, onDismiss = viewModel::closeDumpComparison)
    }

    if (uiState.showWorkflowViewer && uiState.workflowToView != null) {
        WorkflowViewerDialog(
            workflow = uiState.workflowToView!!,
            onDismiss = viewModel::closeWorkflowViewer,
            onRunFeature = { featureId ->
                val feature = uiState.availableFeatures.find { it.id == featureId }
                if (feature != null) maybeConfirmThenRun(feature)
            }
        )
    }

    if (uiState.showKeyRecovery) {
        KeyRecoveryDialog(
            keyState = uiState.mifareKeyState,
            selectedSector = uiState.selectedSector,
            terminalLines = uiState.terminalLines,
            connected = connected,
            executionState = uiState.featureExecutionState,
            tagType = uiState.lastScan?.type,
            onSectorClick = viewModel::selectSector,
            onStartDictionary = viewModel::startDictionaryCheck,
            onStartScan = {
                val scanFeature = uiState.availableFeatures.find { it.id == "auto-hf-search" }
                if (scanFeature != null) viewModel.runFeature(scanFeature)
            },
            onRunCommand = viewModel::runGymWorkflowCommand,
            onReset = viewModel::resetKeyRecovery,
            onDismiss = viewModel::hideKeyRecovery
        )
    }

    if (uiState.showGymLockerWorkflow) {
        GymLockerWorkflowDialog(
            keyState = uiState.mifareKeyState,
            executionState = uiState.featureExecutionState,
            terminalLines = uiState.terminalLines,
            connected = connected,
            tagUid = uiState.lastScan?.uid,
            tagType = uiState.lastScan?.type,
            onSectorClick = viewModel::selectSector,
            onRunCommand = viewModel::runGymWorkflowCommand,
            onReset = viewModel::resetKeyRecovery,
            onDismiss = viewModel::hideGymLockerWorkflow
        )
    }
}
