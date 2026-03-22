package com.example.proxmark.plugins

object PluginCatalog {
    val builtInPlugins: List<Plugin> = listOf(
        Plugin(
            id = "gym-locker-analyzer",
            name = "Gym Locker Analyzer",
            description = "Interactive guided workflow for gym locker MIFARE analysis. Scans tag, recovers keys, dumps data, and identifies access patterns used by common gym lock systems.",
            author = "APM3 Team",
            version = "1.2.0",
            category = PluginCategory.ANALYSIS,
            icon = "FitnessCenter",
            installed = true,
            enabled = true,
            permissions = listOf(PluginPermission.READ_TAG, PluginPermission.EXECUTE_COMMANDS, PluginPermission.ACCESS_KEYS),
            commands = listOf("hf search", "hf mf chk", "hf mf dump", "hf mf rdbl")
        ),
        Plugin(
            id = "mifare-key-dumper",
            name = "MIFARE Key Dumper",
            description = "Full MIFARE Classic key recovery automation. Runs dictionary attacks, nested attacks, and hardnested attacks to recover all sector keys from a MIFARE Classic card.",
            author = "APM3 Team",
            version = "2.0.1",
            category = PluginCategory.ATTACK,
            icon = "VpnKey",
            permissions = listOf(PluginPermission.READ_TAG, PluginPermission.EXECUTE_COMMANDS, PluginPermission.ACCESS_KEYS),
            commands = listOf("hf mf chk", "hf mf nested", "hf mf hardnested", "hf mf autopwn")
        ),
        Plugin(
            id = "tag-cloner-pro",
            name = "Tag Cloner Pro",
            description = "One-tap tag cloning workflow. Reads source tag, stores data, and writes to a blank target tag. Supports MIFARE Classic 1K/4K and various LF tags.",
            author = "APM3 Team",
            version = "1.5.0",
            category = PluginCategory.CLONING,
            icon = "ContentCopy",
            permissions = listOf(PluginPermission.READ_TAG, PluginPermission.WRITE_TAG, PluginPermission.EXECUTE_COMMANDS, PluginPermission.ACCESS_KEYS),
            commands = listOf("hf mf dump", "hf mf restore", "hf mf wrbl")
        ),
        Plugin(
            id = "wiegand-decoder",
            name = "Wiegand Decoder",
            description = "Decode Wiegand data from HID, Indala, and other access control cards. Supports 26-bit, 34-bit, 37-bit, and custom Wiegand formats.",
            author = "APM3 Team",
            version = "1.1.0",
            category = PluginCategory.UTILITY,
            icon = "QrCode",
            permissions = listOf(PluginPermission.READ_TAG, PluginPermission.EXECUTE_COMMANDS),
            commands = listOf("lf hid read", "lf hid decode", "lf indala read")
        ),
        Plugin(
            id = "brute-force-suite",
            name = "Brute Force Suite",
            description = "Automated brute force attack chains for various tag types. Includes dictionary attacks, key space enumeration, and default key checking.",
            author = "APM3 Team",
            version = "1.3.0",
            category = PluginCategory.ATTACK,
            icon = "Security",
            permissions = listOf(PluginPermission.READ_TAG, PluginPermission.EXECUTE_COMMANDS, PluginPermission.ACCESS_KEYS),
            commands = listOf("hf mf chk", "hf mf fchk", "lf t55xx bruteforce")
        ),
        Plugin(
            id = "data-export",
            name = "Data Export",
            description = "Export scanned tags, recovered keys, and dump files. Supports JSON, CSV, and binary formats for sharing and archival.",
            author = "APM3 Team",
            version = "1.0.2",
            category = PluginCategory.UTILITY,
            icon = "FileDownload",
            permissions = listOf(PluginPermission.READ_TAG, PluginPermission.ACCESS_KEYS, PluginPermission.EXPORT_DATA),
            commands = emptyList()
        ),
        Plugin(
            id = "access-pattern-analyzer",
            name = "Access Pattern Analyzer",
            description = "Analyze access control patterns from MIFARE sector trailers. Detects read/write permissions, key usage, and potential security weaknesses.",
            author = "APM3 Team",
            version = "1.0.0",
            category = PluginCategory.ANALYSIS,
            icon = "Analytics",
            permissions = listOf(PluginPermission.READ_TAG, PluginPermission.EXECUTE_COMMANDS, PluginPermission.ACCESS_KEYS),
            commands = listOf("hf mf rdbl", "hf mf rdsc")
        ),
        Plugin(
            id = "multi-tag-scanner",
            name = "Multi-Tag Scanner",
            description = "Batch scan multiple tags in sequence. Automatically detects tag type, reads UID, and catalogs each tag with timestamps and metadata.",
            author = "APM3 Team",
            version = "1.4.0",
            category = PluginCategory.AUTOMATION,
            icon = "DynamicFeed",
            permissions = listOf(PluginPermission.READ_TAG, PluginPermission.EXECUTE_COMMANDS),
            commands = listOf("hf search", "lf search", "hf 14a info")
        ),
        Plugin(
            id = "em4100-toolkit",
            name = "EM4100 Toolkit",
            description = "Complete toolkit for EM4100/EM4102 low-frequency tags. Read, clone, simulate, and manage EM4x series tags with one-tap actions.",
            author = "APM3 Team",
            version = "1.2.1",
            category = PluginCategory.CLONING,
            icon = "Nfc",
            permissions = listOf(PluginPermission.READ_TAG, PluginPermission.WRITE_TAG, PluginPermission.EXECUTE_COMMANDS),
            commands = listOf("lf em 410x read", "lf em 410x clone", "lf em 410x sim")
        ),
        Plugin(
            id = "iclass-tools",
            name = "iClass Tools",
            description = "HID iClass / Picopass operations. Read, analyze, and manage iClass credentials with support for legacy and SE key diversification.",
            author = "Security Labs",
            version = "1.1.0",
            category = PluginCategory.ANALYSIS,
            icon = "Badge",
            permissions = listOf(PluginPermission.READ_TAG, PluginPermission.EXECUTE_COMMANDS, PluginPermission.ACCESS_KEYS),
            commands = listOf("hf iclass info", "hf iclass dump", "hf iclass rdbl")
        ),
        Plugin(
            id = "uid-changer",
            name = "UID Changer",
            description = "Change the UID of compatible Chinese magic cards (Gen1a, Gen2, CUID). Essential for cloning tags that check UID during authentication.",
            author = "Security Labs",
            version = "1.0.0",
            category = PluginCategory.CLONING,
            icon = "Edit",
            permissions = listOf(PluginPermission.READ_TAG, PluginPermission.WRITE_TAG, PluginPermission.EXECUTE_COMMANDS),
            commands = listOf("hf mf csetuid", "hf mf gen3uid", "hf 14a info")
        ),
        Plugin(
            id = "auto-recon",
            name = "Auto Reconnaissance",
            description = "Automated tag reconnaissance. Scans all frequencies, identifies tag type, runs appropriate info commands, and produces a comprehensive report.",
            author = "APM3 Team",
            version = "2.0.0",
            category = PluginCategory.AUTOMATION,
            icon = "Radar",
            permissions = listOf(PluginPermission.READ_TAG, PluginPermission.EXECUTE_COMMANDS, PluginPermission.EXPORT_DATA),
            commands = listOf("hf search", "lf search", "hf 14a info", "hf mfp info", "hf mfu info")
        )
    )

    val sampleWorkflows: List<CustomWorkflow> = listOf(
        CustomWorkflow(
            id = "quick-clone",
            name = "Quick Clone",
            description = "Scan a tag, read its data, and emulate it — three steps to a working clone.",
            steps = listOf(
                WorkflowStep(
                    id = "qc-1",
                    title = "Scan Tag",
                    command = "hf search",
                    description = "Identify the tag type and UID",
                    waitForResult = true,
                    successPattern = "found|Valid|UID"
                ),
                WorkflowStep(
                    id = "qc-2",
                    title = "Read Data",
                    command = "hf mf dump",
                    description = "Dump all sectors to memory",
                    waitForResult = true,
                    successPattern = "saved|dumped|bytes"
                ),
                WorkflowStep(
                    id = "qc-3",
                    title = "Emulate Tag",
                    command = "hf mf sim",
                    description = "Start tag emulation with dumped data",
                    waitForResult = false
                )
            )
        ),
        CustomWorkflow(
            id = "full-audit",
            name = "Full Security Audit",
            description = "Comprehensive tag audit: scan, recover keys, dump data, and analyze access patterns.",
            steps = listOf(
                WorkflowStep(
                    id = "fa-1",
                    title = "Identify Tag",
                    command = "hf search",
                    description = "Detect tag type and technology",
                    waitForResult = true,
                    successPattern = "found|Valid|UID"
                ),
                WorkflowStep(
                    id = "fa-2",
                    title = "Check Default Keys",
                    command = "hf mf chk --1k",
                    description = "Try default/known keys on all sectors",
                    waitForResult = true,
                    successPattern = "found keys"
                ),
                WorkflowStep(
                    id = "fa-3",
                    title = "Recover Remaining Keys",
                    command = "hf mf autopwn",
                    description = "Automated key recovery using nested/hardnested",
                    waitForResult = true,
                    successPattern = "found keys|all keys"
                ),
                WorkflowStep(
                    id = "fa-4",
                    title = "Full Dump",
                    command = "hf mf dump",
                    description = "Dump all sectors using recovered keys",
                    waitForResult = true,
                    successPattern = "saved|dumped"
                ),
                WorkflowStep(
                    id = "fa-5",
                    title = "Analyze Access Bits",
                    command = "hf mf acl",
                    description = "Decode access control bits for all sectors",
                    waitForResult = true
                )
            )
        ),
        CustomWorkflow(
            id = "lf-clone",
            name = "LF Tag Clone",
            description = "Read and clone low-frequency access cards (EM4100, HID).",
            steps = listOf(
                WorkflowStep(
                    id = "lf-1",
                    title = "Detect LF Tag",
                    command = "lf search",
                    description = "Search for low-frequency tags",
                    waitForResult = true,
                    successPattern = "found|Valid"
                ),
                WorkflowStep(
                    id = "lf-2",
                    title = "Read Tag Data",
                    command = "lf em 410x read",
                    description = "Read EM4100 tag ID",
                    waitForResult = true,
                    successPattern = "EM 410x ID"
                ),
                WorkflowStep(
                    id = "lf-3",
                    title = "Clone to T55xx",
                    command = "lf em 410x clone",
                    description = "Write tag data to a T55xx card",
                    waitForResult = true,
                    successPattern = "Written|clone"
                )
            )
        )
    )

    val sampleScripts: List<ScriptFile> = listOf(
        ScriptFile(
            id = "sample-hf-audit",
            name = "hf_audit.cmd",
            content = """# HF Tag Audit Script
# Scans and identifies high-frequency tags

hf search
hf 14a info
hf mf chk --1k
hf mf dump
""".trimIndent(),
            language = ScriptLanguage.PM3_SCRIPT
        ),
        ScriptFile(
            id = "sample-lf-enum",
            name = "lf_enum.cmd",
            content = """# LF Enumeration Script
# Searches for all low-frequency tag types

lf search
lf em 410x read
lf hid read
lf indala read
lf t55xx detect
""".trimIndent(),
            language = ScriptLanguage.PM3_SCRIPT
        )
    )
}
