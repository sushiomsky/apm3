package com.example.proxmark.viewmodel

import com.example.proxmark.features.FeatureExecutionState
import com.example.proxmark.features.FeatureRunRecord
import com.example.proxmark.features.ProxmarkFeature
import com.example.proxmark.mifare.DumpComparison
import com.example.proxmark.mifare.MifareClassicDump
import com.example.proxmark.mifare.MifareKeyState
import com.example.proxmark.mifare.MifareWorkflow
import com.example.proxmark.persistence.AppSettings
import com.example.proxmark.plugins.CustomWorkflow
import com.example.proxmark.plugins.Plugin
import com.example.proxmark.plugins.ScriptFile
import com.example.proxmark.protocol.TerminalLine
import com.example.proxmark.reader.ReaderConnectionState
import com.example.proxmark.reader.ReaderType
import com.example.proxmark.repository.TagGroup
import com.example.proxmark.repository.TagInfo
import com.example.proxmark.usb.UsbConnectionState

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
