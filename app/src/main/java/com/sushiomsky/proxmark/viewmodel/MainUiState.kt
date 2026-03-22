package com.sushiomsky.proxmark.viewmodel

import com.sushiomsky.proxmark.features.FeatureExecutionState
import com.sushiomsky.proxmark.features.FeatureRunRecord
import com.sushiomsky.proxmark.features.ProxmarkFeature
import com.sushiomsky.proxmark.mifare.DumpComparison
import com.sushiomsky.proxmark.mifare.MifareClassicDump
import com.sushiomsky.proxmark.mifare.MifareKeyState
import com.sushiomsky.proxmark.mifare.MifareWorkflow
import com.sushiomsky.proxmark.persistence.AppSettings
import com.sushiomsky.proxmark.plugins.CustomWorkflow
import com.sushiomsky.proxmark.plugins.Plugin
import com.sushiomsky.proxmark.plugins.ScriptFile
import com.sushiomsky.proxmark.protocol.TerminalLine
import com.sushiomsky.proxmark.reader.ReaderConnectionState
import com.sushiomsky.proxmark.reader.ReaderType
import com.sushiomsky.proxmark.repository.TagGroup
import com.sushiomsky.proxmark.repository.TagInfo
import com.sushiomsky.proxmark.usb.UsbConnectionState

data class CommandMetrics(
    val commandsSent: Int = 0,
    val lastCommandTimeMs: Long = 0,
    val totalResponseTimeMs: Long = 0,
    val sessionStartTime: Long = System.currentTimeMillis()
) {
    val averageResponseTimeMs: Long
        get() = if (commandsSent > 0) totalResponseTimeMs / commandsSent else 0
    val uptimeMs: Long
        get() = System.currentTimeMillis() - sessionStartTime
}

data class MainUiState(
    val connectionState: UsbConnectionState = UsbConnectionState.Disconnected,
    val availableFeatures: List<ProxmarkFeature> = emptyList(),
    val featureExecutionState: FeatureExecutionState = FeatureExecutionState.Idle,
    val featureRunHistory: List<FeatureRunRecord> = emptyList(),
    val lastScan: TagInfo? = null,
    val savedTags: List<TagInfo> = emptyList(),
    val tagGroups: List<TagGroup> = emptyList(),
    val errorMessage: String? = null,
    val skipConfirmations: Boolean = false,
    val tagSearchQuery: String = "",
    val featureSearchQuery: String = "",
    val settings: AppSettings = AppSettings(),
    val autoScanEnabled: Boolean = false,
    val activeReaderType: ReaderType = ReaderType.PROXMARK3,
    val readerStates: Map<ReaderType, ReaderConnectionState> = emptyMap(),
    val rawCommandMode: Boolean = false,
    val tagSelectionMode: Boolean = false,
    val selectedTagIds: Set<String> = emptySet(),
    val mifareDumpToView: MifareClassicDump? = null,
    val showDumpViewer: Boolean = false,
    val mifareDump1: MifareClassicDump? = null,
    val mifareDump2: MifareClassicDump? = null,
    val dumpComparison: DumpComparison? = null,
    val showDumpComparison: Boolean = false,
    val workflowToView: MifareWorkflow? = null,
    val showWorkflowViewer: Boolean = false,
    val terminalLines: List<TerminalLine> = emptyList(),
    val mifareKeyState: MifareKeyState = MifareKeyState(),
    val showKeyRecovery: Boolean = false,
    val selectedSector: Int? = null,
    val showGymLockerWorkflow: Boolean = false,
    val advancedMode: Boolean = false,
    val commandHistory: List<String> = emptyList(),
    val commandMetrics: CommandMetrics = CommandMetrics(),
    val showAdvancedPanel: Boolean = false,
    val showProtocolInspector: Boolean = false,
    // Plugin system
    val showPluginBrowser: Boolean = false,
    val installedPlugins: List<Plugin> = emptyList(),
    val customWorkflows: List<CustomWorkflow> = emptyList(),
    val showWorkflowBuilder: Boolean = false,
    val editingWorkflow: CustomWorkflow? = null,
    val savedScripts: List<ScriptFile> = emptyList(),
    val showScriptEditor: Boolean = false,
    val editingScript: ScriptFile? = null
)
