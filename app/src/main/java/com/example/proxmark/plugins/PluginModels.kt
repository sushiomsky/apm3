package com.example.proxmark.plugins

data class Plugin(
    val id: String,
    val name: String,
    val description: String,
    val author: String,
    val version: String,
    val category: PluginCategory,
    val icon: String,
    val installed: Boolean = false,
    val enabled: Boolean = false,
    val permissions: List<PluginPermission> = emptyList(),
    val commands: List<String> = emptyList()
)

enum class PluginCategory(val label: String) {
    ATTACK("Attack"),
    ANALYSIS("Analysis"),
    CLONING("Cloning"),
    AUTOMATION("Automation"),
    UTILITY("Utility"),
    CUSTOM("Custom")
}

enum class PluginPermission(val label: String, val description: String) {
    READ_TAG("Read Tag", "Read data from NFC/RFID tags"),
    WRITE_TAG("Write Tag", "Write data to NFC/RFID tags"),
    EXECUTE_COMMANDS("Execute Commands", "Send raw commands to Proxmark3"),
    ACCESS_KEYS("Access Keys", "Access stored encryption keys"),
    EXPORT_DATA("Export Data", "Export tag data and logs"),
    MODIFY_SETTINGS("Modify Settings", "Change application settings")
}

data class CustomWorkflow(
    val id: String,
    val name: String,
    val description: String,
    val steps: List<WorkflowStep>,
    val createdAt: Long = System.currentTimeMillis()
)

data class WorkflowStep(
    val id: String,
    val title: String,
    val command: String,
    val description: String = "",
    val waitForResult: Boolean = true,
    val successPattern: String? = null,
    val failurePattern: String? = null
)

data class ScriptFile(
    val id: String,
    val name: String,
    val content: String,
    val language: ScriptLanguage = ScriptLanguage.PM3_SCRIPT,
    val lastModified: Long = System.currentTimeMillis()
)

enum class ScriptLanguage(val label: String, val extension: String) {
    PM3_SCRIPT("PM3 Script", ".cmd"),
    LUA("Lua", ".lua")
}
