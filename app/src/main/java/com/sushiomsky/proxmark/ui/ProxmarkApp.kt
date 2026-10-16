package com.sushiomsky.proxmark.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.sushiomsky.proxmark.features.ProxmarkFeature
import com.sushiomsky.proxmark.features.ProxmarkFeatureCategory
import com.sushiomsky.proxmark.reader.ReaderConnectionState
import com.sushiomsky.proxmark.reader.ReaderType
import com.sushiomsky.proxmark.ui.components.DumpComparisonDialog
import com.sushiomsky.proxmark.ui.components.DumpViewerDialog
import com.sushiomsky.proxmark.ui.components.FeatureConfirmationDialog
import com.sushiomsky.proxmark.ui.components.FeatureParameterDialog
import com.sushiomsky.proxmark.ui.components.GymLockerWorkflowDialog
import com.sushiomsky.proxmark.ui.components.KeyRecoveryDialog
import com.sushiomsky.proxmark.ui.components.PluginBrowserDialog
import com.sushiomsky.proxmark.ui.components.RawCommandCard
import com.sushiomsky.proxmark.ui.components.ScriptEditorDialog
import com.sushiomsky.proxmark.ui.components.WorkflowBuilderDialog
import com.sushiomsky.proxmark.ui.components.WorkflowViewerDialog
import com.sushiomsky.proxmark.ui.components.AdvancedModePanel
import com.sushiomsky.proxmark.usb.UsbConnectionState
import com.sushiomsky.proxmark.viewmodel.MainViewModel

private enum class AppTab(val label: String) {
    HOME("Home"),
    TOOLS("Tools"),
    TAGS("Tags"),
    TERMINAL("Terminal")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProxmarkApp(
    viewModel: MainViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by rememberSaveable { mutableIntStateOf(0) }

    // Rotation-safe: the pending feature id lives in uiState (ViewModel),
    // so an open confirmation/parameter dialog survives rotation.
    val pendingFeature = uiState.pendingFeatureId?.let { id ->
        uiState.availableFeatures.find { it.id == id }
    }

    // Errors were previously swallowed: surface them globally so a failed
    // action (e.g. "PM3 not connected") is never a silent no-op.
    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            viewModel.clearError()
        }
    }

    val connected = uiState.connectionState is UsbConnectionState.Connected
    val nfcConnected = uiState.readerStates[ReaderType.BUILTIN_NFC] is ReaderConnectionState.Connected
    val favoriteIds = uiState.settings.favoriteFeatureIds
    val groupedFeatures = uiState.availableFeatures.groupBy { it.category }
    val quickFeatures = groupedFeatures[ProxmarkFeatureCategory.QUICK_START].orEmpty()
    val favoriteFeatures = uiState.availableFeatures.filter { it.id in favoriteIds }

    val maybeConfirmThenRun: (ProxmarkFeature) -> Unit = { feature ->
        viewModel.requestRunFeature(feature)
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
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            NavigationBar {
                tabs.forEachIndexed { index, tab ->
                    NavigationBarItem(
                        icon = {
                            when (tab) {
                                AppTab.HOME -> Icon(Icons.Default.Home, contentDescription = tab.label)
                                AppTab.TOOLS -> Icon(Icons.Default.Build, contentDescription = tab.label)
                                AppTab.TAGS -> Icon(Icons.Default.List, contentDescription = tab.label)
                                AppTab.TERMINAL -> Icon(Icons.Default.Terminal, contentDescription = tab.label)
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
                isFeatureEnabled = isFeatureEnabled,
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
                onShowPluginBrowser = viewModel::showPluginBrowser,
                onShowWorkflowBuilder = { viewModel.showPluginWorkflowBuilder() },
                onShowScriptEditor = { viewModel.showPluginScriptEditor() },
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
                onExportLog = viewModel::exportTerminalLog,
                modifier = contentModifier
            )
        }
    }

    // ── Global dialogs ──────────────────────────────────────────────

    pendingFeature?.let { feature ->
        if (feature.parameters.isNotEmpty()) {
            FeatureParameterDialog(
                feature = feature,
                onRun = { paramValues -> viewModel.confirmPendingFeature(paramValues) },
                onDismiss = viewModel::dismissPendingFeature
            )
        } else {
            FeatureConfirmationDialog(
                feature = feature,
                onConfirm = { viewModel.confirmPendingFeature() },
                onDismiss = viewModel::dismissPendingFeature,
                onSkipFutureConfirmations = {
                    viewModel.setSkipConfirmations(true)
                    viewModel.confirmPendingFeature()
                }
            )
        }
    }

    if (uiState.showDumpViewer && uiState.mifareDumpToView != null) {
        DumpViewerDialog(
            dump = uiState.mifareDumpToView!!,
            onExportDump = viewModel::exportDump,
            onDismiss = viewModel::closeDumpViewer
        )
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
            onExportKeys = viewModel::exportKeys,
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

    if (uiState.showAdvancedPanel) {
        AdvancedModePanel(
            connected = connected,
            commandHistory = uiState.commandHistory,
            commandMetrics = uiState.commandMetrics,
            showProtocolInspector = uiState.showProtocolInspector,
            features = uiState.availableFeatures,
            onSendCommand = viewModel::sendRawCommand,
            onToggleProtocolInspector = viewModel::toggleProtocolInspector,
            onClearHistory = viewModel::clearCommandHistory,
            onDismiss = viewModel::hideAdvancedPanel
        )
    }

    // ── Plugin System Dialogs ───────────────────────────────────────

    if (uiState.showPluginBrowser) {
        PluginBrowserDialog(
            plugins = viewModel.getAllPluginsForBrowser(),
            onInstallPlugin = viewModel::installPlugin,
            onUninstallPlugin = viewModel::uninstallPlugin,
            onTogglePluginEnabled = viewModel::togglePluginEnabled,
            onDismiss = viewModel::hidePluginBrowser
        )
    }

    if (uiState.showWorkflowBuilder) {
        WorkflowBuilderDialog(
            existingWorkflow = uiState.editingWorkflow,
            availableCommands = uiState.availableFeatures.map { it.command }.distinct(),
            onSaveWorkflow = viewModel::saveWorkflow,
            onRunWorkflow = viewModel::runWorkflow,
            onDismiss = viewModel::hidePluginWorkflowBuilder
        )
    }

    if (uiState.showScriptEditor) {
        ScriptEditorDialog(
            existingScript = uiState.editingScript,
            savedScripts = uiState.savedScripts,
            onSaveScript = viewModel::saveScript,
            onDeleteScript = viewModel::deleteScript,
            onRunScript = viewModel::runScript,
            onDismiss = viewModel::hidePluginScriptEditor
        )
    }
}
