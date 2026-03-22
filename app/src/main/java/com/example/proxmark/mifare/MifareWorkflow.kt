package com.example.proxmark.mifare

data class MifareWorkflow(
    val id: String,
    val title: String,
    val description: String,
    val category: WorkflowCategory,
    val difficulty: WorkflowDifficulty,
    val estimatedTime: String,
    val prerequisites: List<String>,
    val steps: List<WorkflowStep>,
    val warningMessage: String? = null,
    val legalNotice: String? = null
)

enum class WorkflowCategory {
    ANALYSIS,
    CLONING,
    SECURITY_TESTING,
    RECOVERY,
    ADVANCED
}

enum class WorkflowDifficulty {
    BEGINNER,
    INTERMEDIATE,
    ADVANCED,
    EXPERT
}

data class WorkflowStep(
    val number: Int,
    val title: String,
    val description: String,
    val command: String? = null,
    val expectedOutput: String? = null,
    val tips: List<String> = emptyList(),
    val warnings: List<String> = emptyList(),
    val isOptional: Boolean = false,
    val relatedFeatureId: String? = null
)

object MifareWorkflowCatalog {
    val allWorkflows = listOf(
        // Gym Locker Analysis (from user's research)
        MifareWorkflow(
            id = "gym-locker-analysis",
            title = "Gym Locker Analysis",
            description = "Analyze a gym/spa locker wristband to understand the security implementation and identify the access control sector.",
            category = WorkflowCategory.ANALYSIS,
            difficulty = WorkflowDifficulty.BEGINNER,
            estimatedTime = "5-10 minutes",
            prerequisites = listOf("PM3 connected", "Target wristband/fob"),
            legalNotice = "Only test your own assigned locker. Unauthorized access is illegal.",
            steps = listOf(
                WorkflowStep(
                    number = 1,
                    title = "Identify the card type",
                    description = "Place the wristband on the PM3 antenna and scan to identify chip type.",
                    command = "hf search",
                    expectedOutput = "MIFARE Classic 1K or FM11RF08S (Fudan clone)",
                    tips = listOf(
                        "Most gym wristbands use cheap Chinese clones (FM11RF08S)",
                        "These clones have weak PRNG and hardware backdoors"
                    )
                ),
                WorkflowStep(
                    number = 2,
                    title = "Check for known vulnerabilities",
                    description = "Run hardnested or darkside attack to detect weak crypto.",
                    command = "hf mf autopwn",
                    expectedOutput = "If FM11RF08S: instant key recovery via backdoor",
                    tips = listOf(
                        "autopwn tries multiple attacks automatically",
                        "Fudan clones typically crack in <10 seconds"
                    ),
                    relatedFeatureId = "mifare-autopwn"
                ),
                WorkflowStep(
                    number = 3,
                    title = "Dump all sectors",
                    description = "Extract full card data including all keys and blocks.",
                    command = "hf mf dump",
                    expectedOutput = "Creates .bin and .key files in PM3 client directory",
                    tips = listOf(
                        "Files saved as hf-mf-<UID>-dump.bin and hf-mf-<UID>-key.bin",
                        "Copy hex output and paste into app's dump analyzer"
                    )
                ),
                WorkflowStep(
                    number = 4,
                    title = "Analyze the dump",
                    description = "Identify which sectors contain data and which use custom keys.",
                    tips = listOf(
                        "Look for sectors with non-default keys (not FFFFFFFFFFFF)",
                        "Common pattern: most sectors empty, one 'protected' sector with data",
                        "For gym lockers, typically sector 4-8 contains access data",
                        "Sector 7 is very common (blocks 28-31)"
                    ),
                    warnings = listOf(
                        "Block 0 contains UID - usually read-only on genuine cards"
                    )
                ),
                WorkflowStep(
                    number = 5,
                    title = "Identify the access control sector",
                    description = "Find which sector the reader actually checks.",
                    tips = listOf(
                        "Sector with custom keys + non-zero data is likely the one",
                        "Common gym keys: A0A1A2A3A4A5 (factory default, often unchanged)",
                        "If unsure, proceed to sniffing step"
                    )
                ),
                WorkflowStep(
                    number = 6,
                    title = "Sniff real usage (optional)",
                    description = "Capture actual reader-card communication to confirm which sector is used.",
                    command = "hf 14a snoop",
                    expectedOutput = "Auth command showing sector number + read commands",
                    tips = listOf(
                        "Start snoop, then tap wristband on locker reader",
                        "Look for AUTH command - shows which sector reader authenticates to",
                        "READ commands after AUTH show which blocks are checked",
                        "Most readers only read 1-2 blocks for simple ID verification"
                    ),
                    isOptional = true
                ),
                WorkflowStep(
                    number = 7,
                    title = "Test emulation",
                    description = "Emulate the dump with PM3 to verify it works.",
                    command = "hf mf eload -f <dump.bin>\nhf mf sim",
                    expectedOutput = "Locker opens when PM3 held near reader",
                    tips = listOf(
                        "Hold PM3 steady near reader during emulation",
                        "If fails: reader may check UID or detect magic cards",
                        "Try 'hf mf sim --uid <UID>' to match UID exactly"
                    ),
                    warnings = listOf(
                        "Don't emulate other people's cards - illegal!"
                    )
                ),
                WorkflowStep(
                    number = 8,
                    title = "Clone to physical card (optional)",
                    description = "Write dump to a writable MIFARE card for permanent clone.",
                    command = "hf mf restore -f <dump.bin> -k <key.bin>",
                    tips = listOf(
                        "Use magic Gen2/CUID card if UID must match",
                        "Regular writable MIFARE 1K works if reader doesn't check UID",
                        "Test with emulation first before wasting a card"
                    ),
                    warnings = listOf(
                        "This overwrites the target card completely"
                    ),
                    isOptional = true
                )
            )
        ),

        // Key Recovery
        MifareWorkflow(
            id = "key-recovery-hard",
            title = "Key Recovery (Hardnested)",
            description = "Recover unknown MIFARE Classic keys using the hardnested attack (works on cards with weak randomness).",
            category = WorkflowCategory.RECOVERY,
            difficulty = WorkflowDifficulty.INTERMEDIATE,
            estimatedTime = "2-30 minutes",
            prerequisites = listOf("PM3 with hardnested support", "One known key (or default key works)"),
            steps = listOf(
                WorkflowStep(
                    number = 1,
                    title = "Try default keys first",
                    description = "Check if any sectors use common default keys.",
                    command = "hf mf chk --1k",
                    expectedOutput = "List of sectors with found keys",
                    tips = listOf(
                        "Most common: FFFFFFFFFFFF (factory default)",
                        "Also try: A0A1A2A3A4A5, B0B1B2B3B4B5, 000000000000"
                    )
                ),
                WorkflowStep(
                    number = 2,
                    title = "Run hardnested attack",
                    description = "Use one known key to recover all other keys.",
                    command = "hf mf hardnested --blk 0 -a -k FFFFFFFFFFFF --tblk 4 --ta",
                    expectedOutput = "Keys recovered after nonce collection and computation",
                    tips = listOf(
                        "Requires PM3 client with hardnested binary",
                        "Can take 2-30 minutes depending on card randomness",
                        "Fudan clones crack much faster due to weak PRNG"
                    )
                ),
                WorkflowStep(
                    number = 3,
                    title = "Verify recovered keys",
                    description = "Test that all keys work.",
                    command = "hf mf dump",
                    expectedOutput = "Full dump succeeds with all sectors",
                    tips = listOf("If dump fails, some keys may be wrong - rerun attack")
                )
            )
        ),

        // Door Access Clone
        MifareWorkflow(
            id = "door-access-clone",
            title = "Door Access Card Clone",
            description = "Clone a door access card to a backup card or wearable.",
            category = WorkflowCategory.CLONING,
            difficulty = WorkflowDifficulty.INTERMEDIATE,
            estimatedTime = "10-15 minutes",
            prerequisites = listOf("PM3 connected", "Original access card", "Blank writable MIFARE card"),
            warningMessage = "Only clone your own authorized access cards. Cloning others' cards is illegal.",
            legalNotice = "Ensure you have permission to duplicate the card. Many jurisdictions prohibit unauthorized duplication of access credentials.",
            steps = listOf(
                WorkflowStep(
                    number = 1,
                    title = "Read original card",
                    description = "Dump the original access card.",
                    command = "hf mf autopwn",
                    tips = listOf(
                        "autopwn tries multiple attack methods automatically",
                        "If fails, card may use upgraded crypto (DESFire, etc.) - not clonable with Classic methods"
                    )
                ),
                WorkflowStep(
                    number = 2,
                    title = "Check UID dependency",
                    description = "Determine if the reader verifies the UID.",
                    tips = listOf(
                        "Sniff the reader: 'hf 14a snoop' while tapping card",
                        "If reader sends UID in communication, it's checked",
                        "Some readers only check data in sectors, not UID"
                    ),
                    warnings = listOf(
                        "If UID is checked, you need a magic card (Gen1/Gen2/CUID) to change UID"
                    )
                ),
                WorkflowStep(
                    number = 3,
                    title = "Test with emulation first",
                    description = "Emulate before writing to verify compatibility.",
                    command = "hf mf eload -f <dump.bin>\nhf mf sim",
                    tips = listOf(
                        "Hold PM3 near reader - if it grants access, clone will work",
                        "If rejected: reader may have anti-magic detection or require specific UID"
                    )
                ),
                WorkflowStep(
                    number = 4,
                    title = "Write to blank card",
                    description = "Clone the dump to your backup card.",
                    command = "hf mf restore -f <dump.bin> -k <key.bin>",
                    tips = listOf(
                        "If UID must match: use magic card and write UID first",
                        "Command for magic Gen2: hf mf csetuid -u <UID>",
                        "Then restore data with normal restore command"
                    ),
                    warnings = listOf(
                        "Double-check you're writing to the correct card!",
                        "This will erase all data on the target card"
                    )
                ),
                WorkflowStep(
                    number = 5,
                    title = "Verify clone",
                    description = "Test the cloned card on the actual reader.",
                    tips = listOf(
                        "Test in a non-critical scenario first if possible",
                        "Keep original card as backup",
                        "If fails: check UID match and sector data with dump comparison"
                    )
                )
            )
        ),

        // Sniff and Analyze
        MifareWorkflow(
            id = "sniff-analysis",
            title = "Sniff & Analyze Communication",
            description = "Capture and analyze the communication between a card and reader to understand the authentication and data exchange.",
            category = WorkflowCategory.SECURITY_TESTING,
            difficulty = WorkflowDifficulty.ADVANCED,
            estimatedTime = "15-30 minutes",
            prerequisites = listOf("PM3 with sniff support", "Access to reader and working card"),
            steps = listOf(
                WorkflowStep(
                    number = 1,
                    title = "Position PM3 for sniffing",
                    description = "Place PM3 antenna between card and reader.",
                    tips = listOf(
                        "PM3 must be very close to both card and reader",
                        "Best position: PM3 sandwiched between card and reader",
                        "Some readers need card held longer - be patient"
                    )
                ),
                WorkflowStep(
                    number = 2,
                    title = "Start sniffing",
                    description = "Begin capture mode.",
                    command = "hf 14a snoop",
                    tips = listOf(
                        "Press button once to start",
                        "Alternative: 'hf mf sniff' for MIFARE-specific sniff"
                    )
                ),
                WorkflowStep(
                    number = 3,
                    title = "Trigger reader-card interaction",
                    description = "Tap the card on reader while PM3 is sniffing.",
                    tips = listOf(
                        "Hold card steady for full transaction",
                        "If reader beeps/lights up, transaction was captured"
                    )
                ),
                WorkflowStep(
                    number = 4,
                    title = "Stop and analyze trace",
                    description = "Review captured data to understand protocol.",
                    command = "hf 14a list",
                    expectedOutput = "Shows REQA, AUTH, READ/WRITE commands",
                    tips = listOf(
                        "Look for AUTH commands - reveal which sector is accessed",
                        "READ commands after AUTH show which blocks are read",
                        "WRITE commands indicate reader updates the card (counters, logs, etc.)",
                        "Note the nonces and encrypted responses for offline analysis"
                    )
                ),
                WorkflowStep(
                    number = 5,
                    title = "Extract actionable info",
                    description = "Identify key details from the trace.",
                    tips = listOf(
                        "Which sector is authenticated? (shown in AUTH command)",
                        "Which blocks are read/written?",
                        "Is UID sent/checked by reader?",
                        "Are there multiple transactions (read then write)?",
                        "Timing: how long between commands? (anti-relay check)"
                    )
                ),
                WorkflowStep(
                    number = 6,
                    title = "Use info for cloning/emulation",
                    description = "Apply findings to clone strategy.",
                    tips = listOf(
                        "If only specific sector read: clone just that sector",
                        "If UID checked: need magic card",
                        "If writes occur: emulation may fail (card state changes)",
                        "If timing-sensitive: PM3 emulation may be too slow"
                    )
                )
            )
        ),

        // Quick Dump & Backup
        MifareWorkflow(
            id = "quick-backup",
            title = "Quick Backup",
            description = "Quickly backup a MIFARE Classic card for safekeeping.",
            category = WorkflowCategory.ANALYSIS,
            difficulty = WorkflowDifficulty.BEGINNER,
            estimatedTime = "2-5 minutes",
            prerequisites = listOf("PM3 connected", "Card to backup"),
            steps = listOf(
                WorkflowStep(
                    number = 1,
                    title = "Run autopwn",
                    description = "Automatic attack and dump.",
                    command = "hf mf autopwn",
                    tips = listOf("Tries multiple methods, picks fastest")
                ),
                WorkflowStep(
                    number = 2,
                    title = "Save dump files",
                    description = "Store .bin and .key files safely.",
                    tips = listOf(
                        "Files saved as hf-mf-<UID>-dump.bin",
                        "Transfer to computer for archival",
                        "Paste hex into app for visual analysis"
                    )
                ),
                WorkflowStep(
                    number = 3,
                    title = "Verify backup",
                    description = "Test emulation to confirm dump is valid.",
                    command = "hf mf eload -f <dump.bin>\nhf mf sim",
                    tips = listOf("Quick emulation test ensures dump is complete")
                )
            )
        )
    )

    fun getWorkflowById(id: String): MifareWorkflow? = allWorkflows.find { it.id == id }

    fun getWorkflowsByCategory(category: WorkflowCategory): List<MifareWorkflow> =
        allWorkflows.filter { it.category == category }

    fun getWorkflowsByDifficulty(difficulty: WorkflowDifficulty): List<MifareWorkflow> =
        allWorkflows.filter { it.difficulty == difficulty }
}
