package com.example.proxmark.features

import com.example.proxmark.reader.ReaderType

object ProxmarkFeatureCatalog {
    val allFeatures: List<ProxmarkFeature> = listOf(
        // ── Quick Start ──
        ProxmarkFeature(
            id = "auto-hf-search",
            title = "Scan HF tag",
            description = "Run automatic high-frequency tag search.",
            category = ProxmarkFeatureCategory.QUICK_START,
            command = "hf search",
            prerequisites = listOf("Connect PM3 over USB", "Place HF tag near antenna")
        ),
        ProxmarkFeature(
            id = "auto-lf-search",
            title = "Scan LF tag",
            description = "Run automatic low-frequency tag search.",
            category = ProxmarkFeatureCategory.QUICK_START,
            command = "lf search",
            prerequisites = listOf("Connect PM3 over USB", "Place LF tag near antenna")
        ),

        // ── Device ──
        ProxmarkFeature(
            id = "device-info",
            title = "Device info",
            description = "Read PM3 hardware, firmware, and memory information.",
            category = ProxmarkFeatureCategory.DEVICE,
            command = "hw status",
            prerequisites = listOf("Connect PM3 over USB")
        ),
        ProxmarkFeature(
            id = "device-tune",
            title = "Antenna tune",
            description = "Measure antenna tuning and coupling status.",
            category = ProxmarkFeatureCategory.DEVICE,
            command = "hw tune",
            prerequisites = listOf("Connect PM3 over USB", "Keep target tag near antenna for meaningful coupling data")
        ),
        ProxmarkFeature(
            id = "device-version",
            title = "Firmware version",
            description = "Display PM3 firmware version and capabilities.",
            category = ProxmarkFeatureCategory.DEVICE,
            command = "version",
            prerequisites = listOf("Connect PM3 over USB")
        ),
        ProxmarkFeature(
            id = "mem-info",
            title = "Memory info",
            description = "Show PM3 memory usage and segment details.",
            category = ProxmarkFeatureCategory.DEVICE,
            command = "mem info",
            prerequisites = listOf("Connect PM3 over USB")
        ),

        // ── Firmware & Bootloader ──
        ProxmarkFeature(
            id = "flash-bootrom",
            title = "Flash bootloader",
            description = "Update PM3 bootloader from file. DANGEROUS - can brick device if interrupted!",
            category = ProxmarkFeatureCategory.FIRMWARE,
            command = "flasher bootrom {file}",
            safetyLevel = FeatureSafetyLevel.DESTRUCTIVE,
            parameters = listOf(
                FeatureParameter(
                    id = "file",
                    label = "Bootloader file path",
                    hint = "Path to bootrom .elf file, e.g., /sdcard/Download/bootrom.elf"
                )
            ),
            prerequisites = listOf(
                "Connect PM3 over USB",
                "Have correct bootrom .elf file ready",
                "Ensure stable power supply",
                "BACKUP important data first"
            ),
            warningMessage = "CRITICAL: Flashing bootloader can BRICK your device if:\n" +
                "• Wrong file used\n" +
                "• Power lost during flash\n" +
                "• USB disconnected during process\n" +
                "Only proceed if you have recovery method available!"
        ),
        ProxmarkFeature(
            id = "flash-fullimage",
            title = "Flash full image",
            description = "Flash complete firmware image (OS + FPGA). Takes several minutes.",
            category = ProxmarkFeatureCategory.FIRMWARE,
            command = "flasher fullimage {file}",
            safetyLevel = FeatureSafetyLevel.DESTRUCTIVE,
            parameters = listOf(
                FeatureParameter(
                    id = "file",
                    label = "Full image file path",
                    hint = "Path to fullimage .elf file"
                )
            ),
            prerequisites = listOf(
                "Connect PM3 over USB",
                "Have fullimage.elf file ready",
                "Stable power and USB connection",
                "At least 5 minutes uninterrupted time"
            ),
            warningMessage = "Flashing takes 3-5 minutes. Do NOT:\n" +
                "• Disconnect USB\n" +
                "• Power off device\n" +
                "• Close app\n" +
                "Wait for completion message before using PM3."
        ),
        ProxmarkFeature(
            id = "flash-os",
            title = "Flash OS only",
            description = "Update PM3 operating system firmware only (faster than full image).",
            category = ProxmarkFeatureCategory.FIRMWARE,
            command = "flasher osimage {file}",
            safetyLevel = FeatureSafetyLevel.DESTRUCTIVE,
            parameters = listOf(
                FeatureParameter(
                    id = "file",
                    label = "OS image file path",
                    hint = "Path to osimage .elf or .bin file"
                )
            ),
            prerequisites = listOf(
                "Connect PM3 over USB",
                "Have OS image file ready",
                "Stable connection"
            ),
            warningMessage = "Flashing OS will restart the PM3. Ensure stable power and USB connection throughout the process."
        ),
        ProxmarkFeature(
            id = "flash-fpga",
            title = "Flash FPGA bitstream",
            description = "Update FPGA configuration for HF/LF operations.",
            category = ProxmarkFeatureCategory.FIRMWARE,
            command = "flasher fpgaimage {file}",
            safetyLevel = FeatureSafetyLevel.DESTRUCTIVE,
            parameters = listOf(
                FeatureParameter(
                    id = "file",
                    label = "FPGA file path",
                    hint = "Path to FPGA bitstream file (.bit)"
                )
            ),
            prerequisites = listOf(
                "Connect PM3 over USB",
                "Have FPGA bitstream file ready"
            ),
            warningMessage = "Wrong FPGA image will prevent HF/LF operations until re-flashed with correct version."
        ),
        ProxmarkFeature(
            id = "hw-bootloader",
            title = "Enter bootloader mode",
            description = "Reboot PM3 into bootloader/flasher mode for firmware updates.",
            category = ProxmarkFeatureCategory.FIRMWARE,
            command = "hw bootloader",
            safetyLevel = FeatureSafetyLevel.ADVANCED,
            prerequisites = listOf("Connect PM3 over USB"),
            warningMessage = "PM3 will disconnect and enter bootloader mode. You'll need to reconnect to flash firmware or power cycle to return to normal mode."
        ),
        ProxmarkFeature(
            id = "hw-reboot",
            title = "Reboot device",
            description = "Restart PM3 (useful after configuration changes).",
            category = ProxmarkFeatureCategory.FIRMWARE,
            command = "hw reboot",
            safetyLevel = FeatureSafetyLevel.ADVANCED,
            prerequisites = listOf("Connect PM3 over USB"),
            warningMessage = "PM3 will disconnect briefly during reboot."
        ),

        // ── High Frequency ──
        ProxmarkFeature(
            id = "hf-info",
            title = "HF tag info",
            description = "Read details from the currently presented HF tag.",
            category = ProxmarkFeatureCategory.HIGH_FREQUENCY,
            command = "hf 14a info",
            prerequisites = listOf("Connect PM3 over USB", "Place ISO14443A tag on antenna")
        ),
        ProxmarkFeature(
            id = "hf-reader",
            title = "HF reader",
            description = "Continuously read ISO14443A tags in reader mode.",
            category = ProxmarkFeatureCategory.HIGH_FREQUENCY,
            command = "hf 14a reader",
            safetyLevel = FeatureSafetyLevel.ADVANCED,
            prerequisites = listOf("Connect PM3 over USB", "Place ISO14443A tag near antenna"),
            warningMessage = "Continuous reader mode keeps the RF field active. Remove tag when done."
        ),
        ProxmarkFeature(
            id = "hf-list",
            title = "HF command help",
            description = "Show available high-frequency command set.",
            category = ProxmarkFeatureCategory.HIGH_FREQUENCY,
            command = "hf help",
            prerequisites = listOf("Connect PM3 over USB")
        ),
        ProxmarkFeature(
            id = "hf-15-info",
            title = "ISO15693 tag info",
            description = "Read info from an ISO15693 (iCODE/NFC-V) tag.",
            category = ProxmarkFeatureCategory.HIGH_FREQUENCY,
            command = "hf 15 info",
            prerequisites = listOf("Connect PM3 over USB", "Place ISO15693 tag on antenna")
        ),
        ProxmarkFeature(
            id = "hf-felica-info",
            title = "FeliCa tag info",
            description = "Read FeliCa tag system information.",
            category = ProxmarkFeatureCategory.HIGH_FREQUENCY,
            command = "hf felica info",
            prerequisites = listOf("Connect PM3 over USB", "Place FeliCa tag on antenna")
        ),

        // ── Low Frequency ──
        ProxmarkFeature(
            id = "lf-read",
            title = "LF read",
            description = "Read low-frequency waveform data.",
            category = ProxmarkFeatureCategory.LOW_FREQUENCY,
            command = "lf read",
            safetyLevel = FeatureSafetyLevel.ADVANCED,
            prerequisites = listOf("Connect PM3 over USB", "Place LF tag near antenna"),
            warningMessage = "Raw LF read captures waveform data and overwrites the current graph buffer."
        ),
        ProxmarkFeature(
            id = "lf-search",
            title = "LF search",
            description = "Detect common low-frequency card formats.",
            category = ProxmarkFeatureCategory.LOW_FREQUENCY,
            command = "lf search",
            prerequisites = listOf("Connect PM3 over USB", "Place LF tag near antenna")
        ),
        ProxmarkFeature(
            id = "lf-help",
            title = "LF command help",
            description = "Show available low-frequency command set.",
            category = ProxmarkFeatureCategory.LOW_FREQUENCY,
            command = "lf help",
            prerequisites = listOf("Connect PM3 over USB")
        ),

        // ── Mifare Classic ──
        ProxmarkFeature(
            id = "mifare-info",
            title = "Mifare Classic info",
            description = "Read Mifare Classic tag information and sector structure.",
            category = ProxmarkFeatureCategory.MIFARE,
            command = "hf mf info",
            prerequisites = listOf("Connect PM3 over USB", "Place Mifare Classic tag on antenna")
        ),
        ProxmarkFeature(
            id = "mifare-reader",
            title = "Mifare autopwn",
            description = "Automatic key recovery and full dump of Mifare Classic.",
            category = ProxmarkFeatureCategory.MIFARE,
            command = "hf mf autopwn",
            safetyLevel = FeatureSafetyLevel.ADVANCED,
            prerequisites = listOf("Connect PM3 over USB", "Use only on cards you are authorized to test"),
            warningMessage = "Autopwn attempts multiple key recovery attacks. Only use on tags you own or have authorization to test."
        ),
        ProxmarkFeature(
            id = "mifare-chk",
            title = "Mifare check keys",
            description = "Check Mifare Classic sectors against known default keys.",
            category = ProxmarkFeatureCategory.MIFARE,
            command = "hf mf chk --1k",
            safetyLevel = FeatureSafetyLevel.ADVANCED,
            prerequisites = listOf("Connect PM3 over USB", "Place Mifare Classic tag on antenna"),
            warningMessage = "Key checking sends authentication attempts to the card. This is non-destructive but generates RF activity."
        ),
        ProxmarkFeature(
            id = "mifare-dump",
            title = "Mifare dump",
            description = "Dump all sectors from Mifare Classic to device memory.",
            category = ProxmarkFeatureCategory.MIFARE,
            command = "hf mf dump",
            safetyLevel = FeatureSafetyLevel.ADVANCED,
            prerequisites = listOf("Connect PM3 over USB", "Run key recovery first (autopwn or check keys)"),
            warningMessage = "Requires known keys. Reads all accessible sectors. Use only on authorized cards."
        ),
        ProxmarkFeature(
            id = "mifare-restore",
            title = "Mifare restore",
            description = "Write a previously dumped image back to a Mifare Classic tag.",
            category = ProxmarkFeatureCategory.MIFARE,
            command = "hf mf restore",
            safetyLevel = FeatureSafetyLevel.DESTRUCTIVE,
            prerequisites = listOf("Connect PM3 over USB", "Have a valid dump file", "Place writable tag on antenna"),
            warningMessage = "This OVERWRITES all sectors on the target tag with dump data. The original card content will be permanently lost."
        ),
        ProxmarkFeature(
            id = "mifare-rdbl",
            title = "Mifare read block",
            description = "Read a single block from Mifare Classic.",
            category = ProxmarkFeatureCategory.MIFARE,
            command = "hf mf rdbl --blk 0 -k FFFFFFFFFFFF",
            safetyLevel = FeatureSafetyLevel.ADVANCED,
            prerequisites = listOf("Connect PM3 over USB", "Place Mifare Classic tag on antenna"),
            warningMessage = "Reads block 0 with default key. Modify the command parameters for other blocks/keys."
        ),

        // ── Mifare Ultralight / NTAG ──
        ProxmarkFeature(
            id = "mfu-info",
            title = "Ultralight info",
            description = "Read Mifare Ultralight / NTAG tag information.",
            category = ProxmarkFeatureCategory.MIFARE_ULTRA,
            command = "hf mfu info",
            prerequisites = listOf("Connect PM3 over USB", "Place Ultralight/NTAG tag on antenna")
        ),
        ProxmarkFeature(
            id = "mfu-dump",
            title = "Ultralight dump",
            description = "Dump all pages from Mifare Ultralight / NTAG.",
            category = ProxmarkFeatureCategory.MIFARE_ULTRA,
            command = "hf mfu dump",
            safetyLevel = FeatureSafetyLevel.ADVANCED,
            prerequisites = listOf("Connect PM3 over USB", "Place Ultralight/NTAG tag on antenna"),
            warningMessage = "Reads all accessible pages. Non-destructive but generates significant RF activity."
        ),
        ProxmarkFeature(
            id = "mfu-rdpg",
            title = "Ultralight read page",
            description = "Read a single page from Mifare Ultralight / NTAG.",
            category = ProxmarkFeatureCategory.MIFARE_ULTRA,
            command = "hf mfu rdpg --page 0",
            prerequisites = listOf("Connect PM3 over USB", "Place Ultralight/NTAG tag on antenna")
        ),
        ProxmarkFeature(
            id = "hf-ndef-read",
            title = "Read NDEF",
            description = "Read NDEF message from NFC Forum tag (Type 2).",
            category = ProxmarkFeatureCategory.MIFARE_ULTRA,
            command = "hf mfu ndef",
            prerequisites = listOf("Connect PM3 over USB", "Place NDEF-formatted tag on antenna")
        ),

        // ── DESFire ──
        ProxmarkFeature(
            id = "desfire-info",
            title = "DESFire info",
            description = "Read DESFire tag manufacturing and application info.",
            category = ProxmarkFeatureCategory.DESFIRE,
            command = "hf mfdes info",
            prerequisites = listOf("Connect PM3 over USB", "Place DESFire tag on antenna")
        ),
        ProxmarkFeature(
            id = "desfire-apps",
            title = "DESFire list apps",
            description = "List applications on a DESFire tag.",
            category = ProxmarkFeatureCategory.DESFIRE,
            command = "hf mfdes lsapp",
            prerequisites = listOf("Connect PM3 over USB", "Place DESFire tag on antenna")
        ),
        ProxmarkFeature(
            id = "desfire-files",
            title = "DESFire list files",
            description = "List files within the selected DESFire application.",
            category = ProxmarkFeatureCategory.DESFIRE,
            command = "hf mfdes lsfiles",
            safetyLevel = FeatureSafetyLevel.ADVANCED,
            prerequisites = listOf("Connect PM3 over USB", "Place DESFire tag on antenna"),
            warningMessage = "Requires application selection and may need authentication keys."
        ),

        // ── iCLASS / Picopass ──
        ProxmarkFeature(
            id = "iclass-info",
            title = "iCLASS info",
            description = "Read iCLASS / Picopass tag information.",
            category = ProxmarkFeatureCategory.ICLASS,
            command = "hf iclass info",
            prerequisites = listOf("Connect PM3 over USB", "Place iCLASS tag on antenna")
        ),
        ProxmarkFeature(
            id = "iclass-dump",
            title = "iCLASS dump",
            description = "Dump iCLASS / Picopass card contents.",
            category = ProxmarkFeatureCategory.ICLASS,
            command = "hf iclass dump",
            safetyLevel = FeatureSafetyLevel.ADVANCED,
            prerequisites = listOf("Connect PM3 over USB", "Place iCLASS tag on antenna"),
            warningMessage = "Reads accessible blocks using default or provided keys. Use only on authorized cards."
        ),

        // ── EM4x / HID / Indala ──
        ProxmarkFeature(
            id = "em-read",
            title = "EM4100 read",
            description = "Read EM4100 / EM410x tag ID.",
            category = ProxmarkFeatureCategory.EM_HID,
            command = "lf em 410x reader",
            prerequisites = listOf("Connect PM3 over USB", "Place EM4100 tag near antenna")
        ),
        ProxmarkFeature(
            id = "em-clone",
            title = "EM4100 clone",
            description = "Clone an EM4100 ID onto a T55xx card.",
            category = ProxmarkFeatureCategory.EM_HID,
            command = "lf em 410x clone --id 0102030405",
            safetyLevel = FeatureSafetyLevel.DESTRUCTIVE,
            prerequisites = listOf("Connect PM3 over USB", "Place writable T55xx tag near antenna", "Know the target EM ID"),
            warningMessage = "This OVERWRITES the T55xx tag with a new EM4100 ID. Original data will be permanently lost. Only clone IDs you own."
        ),
        ProxmarkFeature(
            id = "hid-read",
            title = "HID Prox read",
            description = "Read HID Prox card credential.",
            category = ProxmarkFeatureCategory.EM_HID,
            command = "lf hid reader",
            prerequisites = listOf("Connect PM3 over USB", "Place HID card near antenna")
        ),
        ProxmarkFeature(
            id = "hid-clone",
            title = "HID clone",
            description = "Clone HID credential onto a T55xx card.",
            category = ProxmarkFeatureCategory.EM_HID,
            command = "lf hid clone -r 2006ec0c86",
            safetyLevel = FeatureSafetyLevel.DESTRUCTIVE,
            prerequisites = listOf("Connect PM3 over USB", "Place writable T55xx tag near antenna", "Know the HID raw value"),
            warningMessage = "This OVERWRITES the T55xx tag with HID credential data. Cloning access credentials without authorization may be illegal."
        ),
        ProxmarkFeature(
            id = "indala-read",
            title = "Indala read",
            description = "Read Indala card credential.",
            category = ProxmarkFeatureCategory.EM_HID,
            command = "lf indala reader",
            prerequisites = listOf("Connect PM3 over USB", "Place Indala card near antenna")
        ),

        // ── T55xx ──
        ProxmarkFeature(
            id = "t55xx-detect",
            title = "T55xx detect",
            description = "Detect and identify T55xx tag configuration.",
            category = ProxmarkFeatureCategory.T55XX,
            command = "lf t55xx detect",
            prerequisites = listOf("Connect PM3 over USB", "Place T55xx tag near antenna")
        ),
        ProxmarkFeature(
            id = "t55xx-dump",
            title = "T55xx dump",
            description = "Dump all blocks from a T55xx tag.",
            category = ProxmarkFeatureCategory.T55XX,
            command = "lf t55xx dump",
            safetyLevel = FeatureSafetyLevel.ADVANCED,
            prerequisites = listOf("Connect PM3 over USB", "Place T55xx tag near antenna"),
            warningMessage = "Reads all T55xx blocks. Non-destructive but may need correct password for protected tags."
        ),
        ProxmarkFeature(
            id = "t55xx-info",
            title = "T55xx info",
            description = "Show T55xx configuration block details.",
            category = ProxmarkFeatureCategory.T55XX,
            command = "lf t55xx info",
            prerequisites = listOf("Connect PM3 over USB", "Place T55xx tag near antenna")
        ),
        ProxmarkFeature(
            id = "t55xx-wipe",
            title = "T55xx wipe",
            description = "Reset T55xx tag to factory defaults.",
            category = ProxmarkFeatureCategory.T55XX,
            command = "lf t55xx wipe",
            safetyLevel = FeatureSafetyLevel.DESTRUCTIVE,
            prerequisites = listOf("Connect PM3 over USB", "Place T55xx tag near antenna"),
            warningMessage = "This ERASES all data on the T55xx tag and resets to factory configuration. All stored credentials will be permanently lost."
        ),

        // ── Emulation ──
        ProxmarkFeature(
            id = "hf-simulate",
            title = "HF simulate tag",
            description = "Simulate a high-frequency tag using PM3.",
            category = ProxmarkFeatureCategory.EMULATION,
            command = "hf 14a sim",
            safetyLevel = FeatureSafetyLevel.ADVANCED,
            prerequisites = listOf("Connect PM3 over USB", "Ensure simulation is legal in your environment"),
            warningMessage = "Tag simulation makes the PM3 act as a card. This may trigger access-control systems — use only in authorized environments."
        ),
        ProxmarkFeature(
            id = "lf-simulate",
            title = "LF simulate tag",
            description = "Simulate a low-frequency tag with PM3.",
            category = ProxmarkFeatureCategory.EMULATION,
            command = "lf sim",
            safetyLevel = FeatureSafetyLevel.ADVANCED,
            prerequisites = listOf("Connect PM3 over USB", "Ensure simulation is legal in your environment"),
            warningMessage = "Tag simulation makes the PM3 act as a card. This may trigger access-control systems — use only in authorized environments."
        ),
        ProxmarkFeature(
            id = "hf-mf-sim",
            title = "Mifare Classic simulate",
            description = "Simulate a Mifare Classic tag from a dump file.",
            category = ProxmarkFeatureCategory.EMULATION,
            command = "hf mf sim",
            safetyLevel = FeatureSafetyLevel.ADVANCED,
            prerequisites = listOf("Connect PM3 over USB", "Have a valid Mifare Classic dump loaded"),
            warningMessage = "Simulates a full Mifare Classic card. May trigger access-control readers — use only in authorized environments."
        ),

        // ── Attacks & key recovery ──
        ProxmarkFeature(
            id = "mifare-nested",
            title = "Mifare nested attack",
            description = "Run nested key recovery against Mifare Classic.",
            category = ProxmarkFeatureCategory.ATTACKS,
            command = "hf mf nested",
            safetyLevel = FeatureSafetyLevel.DESTRUCTIVE,
            prerequisites = listOf("Connect PM3 over USB", "Use only on cards you are authorized to audit"),
            warningMessage = "Nested attack actively probes the card to recover encryption keys. This is a security audit tool — unauthorized use may be illegal."
        ),
        ProxmarkFeature(
            id = "mifare-hardnested",
            title = "Mifare hardnested",
            description = "Run hardnested key recovery mode.",
            category = ProxmarkFeatureCategory.ATTACKS,
            command = "hf mf hardnested",
            safetyLevel = FeatureSafetyLevel.DESTRUCTIVE,
            prerequisites = listOf("Connect PM3 over USB", "Use only on cards you are authorized to audit"),
            warningMessage = "Hardnested attack is a powerful key recovery method. It runs many card interactions and is intended for authorized security audits only."
        ),
        ProxmarkFeature(
            id = "mifare-darkside",
            title = "Mifare darkside attack",
            description = "Recover a key from a Mifare Classic tag with no known keys.",
            category = ProxmarkFeatureCategory.ATTACKS,
            command = "hf mf darkside",
            safetyLevel = FeatureSafetyLevel.DESTRUCTIVE,
            prerequisites = listOf("Connect PM3 over USB", "Place Mifare Classic tag on antenna"),
            warningMessage = "Darkside attack exploits PRNG weakness to recover keys without any known key. Only use on authorized cards."
        ),

        // ── Sniffing ──
        ProxmarkFeature(
            id = "hf-snoop",
            title = "HF snoop",
            description = "Capture HF traffic between card and reader.",
            category = ProxmarkFeatureCategory.SNIFFING,
            command = "hf 14a snoop",
            safetyLevel = FeatureSafetyLevel.ADVANCED,
            prerequisites = listOf("Connect PM3 over USB", "Position PM3 between reader and card"),
            warningMessage = "Snoop captures live RF traffic. Large captures may fill device memory."
        ),
        ProxmarkFeature(
            id = "lf-snoop",
            title = "LF snoop",
            description = "Capture low-frequency communication activity.",
            category = ProxmarkFeatureCategory.SNIFFING,
            command = "lf snoop",
            safetyLevel = FeatureSafetyLevel.ADVANCED,
            prerequisites = listOf("Connect PM3 over USB", "Position PM3 close to LF interaction"),
            warningMessage = "LF snoop captures live traffic and may fill device memory with large traces."
        ),
        ProxmarkFeature(
            id = "hf-14a-list",
            title = "HF trace list",
            description = "Show captured ISO14443A trace data.",
            category = ProxmarkFeatureCategory.SNIFFING,
            command = "hf 14a list",
            prerequisites = listOf("Connect PM3 over USB", "Run HF snoop first to capture data")
        ),

        // ── Utilities ──
        ProxmarkFeature(
            id = "plot-clear",
            title = "Clear graph buffer",
            description = "Clear PM3 graph buffer before new captures.",
            category = ProxmarkFeatureCategory.UTILITIES,
            command = "data clear",
            safetyLevel = FeatureSafetyLevel.DESTRUCTIVE,
            prerequisites = listOf("Connect PM3 over USB", "This removes current graph buffer data"),
            warningMessage = "This permanently erases the current graph buffer. Any unsaved capture data will be lost."
        ),
        ProxmarkFeature(
            id = "plot-print",
            title = "Show graph data",
            description = "Print current graph data values and metadata.",
            category = ProxmarkFeatureCategory.UTILITIES,
            command = "data print",
            prerequisites = listOf("Connect PM3 over USB", "Capture or load graph data first")
        ),
        ProxmarkFeature(
            id = "data-hex",
            title = "Data hex dump",
            description = "Show captured data as hex dump.",
            category = ProxmarkFeatureCategory.UTILITIES,
            command = "data hexsamples",
            prerequisites = listOf("Connect PM3 over USB", "Capture or load data first")
        ),

        // ── Scripts ──
        ProxmarkFeature(
            id = "script-list",
            title = "List scripts",
            description = "Show available PM3 scripts on device.",
            category = ProxmarkFeatureCategory.SCRIPTS,
            command = "script list",
            prerequisites = listOf("Connect PM3 over USB")
        ),
        ProxmarkFeature(
            id = "script-help",
            title = "Script help",
            description = "Display help for PM3 script subsystem.",
            category = ProxmarkFeatureCategory.SCRIPTS,
            command = "script help",
            prerequisites = listOf("Connect PM3 over USB")
        ),

        // --- Mifare Classic extended ---
        ProxmarkFeature(
            id = "mifare-autopwn",
            title = "Autopwn",
            description = "Automatically recover all keys and dump a Mifare Classic card.",
            category = ProxmarkFeatureCategory.MIFARE,
            command = "hf mf autopwn",
            safetyLevel = FeatureSafetyLevel.ADVANCED,
            prerequisites = listOf("Place Mifare Classic tag on reader"),
            warningMessage = "Runs multiple key attacks. May take several minutes."
        ),
        ProxmarkFeature(
            id = "mifare-wrbl",
            title = "Write block",
            description = "Write data to a specific Mifare Classic block.",
            category = ProxmarkFeatureCategory.MIFARE,
            command = "hf mf wrbl --blk 0 -k FFFFFFFFFFFF -d 00000000000000000000000000000000",
            safetyLevel = FeatureSafetyLevel.DESTRUCTIVE,
            prerequisites = listOf("Place Mifare Classic tag on reader", "Know the key for the target sector"),
            warningMessage = "Writing wrong data to block 0 can permanently brick the tag."
        ),
        ProxmarkFeature(
            id = "mifare-eload",
            title = "Load dump to emulator",
            description = "Load a card dump file into the PM3 emulator memory.",
            category = ProxmarkFeatureCategory.MIFARE,
            command = "hf mf eload -f hf-mf-dump",
            safetyLevel = FeatureSafetyLevel.SAFE,
            prerequisites = listOf("Dump file on PM3 flash")
        ),
        ProxmarkFeature(
            id = "mifare-esave",
            title = "Save emulator to file",
            description = "Save the PM3 emulator memory to a dump file.",
            category = ProxmarkFeatureCategory.MIFARE,
            command = "hf mf esave -f hf-mf-dump",
            safetyLevel = FeatureSafetyLevel.SAFE,
            prerequisites = listOf("Load card data into emulator first")
        ),
        ProxmarkFeature(
            id = "mifare-csetuid",
            title = "Set UID (magic card)",
            description = "Write a new UID to a Gen1a/Gen2 magic Mifare Classic card.",
            category = ProxmarkFeatureCategory.MIFARE,
            command = "hf mf csetuid --uid 01020304",
            safetyLevel = FeatureSafetyLevel.DESTRUCTIVE,
            prerequisites = listOf("Place Gen1a/Gen2 magic card on reader"),
            warningMessage = "Only works on magic/Chinese clone cards. Writing to a non-magic card will fail."
        ),

        // --- Mifare Ultralight extended ---
        ProxmarkFeature(
            id = "mfu-wrpg",
            title = "Write page",
            description = "Write 4 bytes to a Mifare Ultralight / NTAG page.",
            category = ProxmarkFeatureCategory.MIFARE_ULTRA,
            command = "hf mfu wrbl --blk 4 -d 01020304",
            safetyLevel = FeatureSafetyLevel.DESTRUCTIVE,
            prerequisites = listOf("Place Ultralight/NTAG tag on reader"),
            warningMessage = "Writing to OTP or lock pages is irreversible."
        ),

        // --- HF 14B / Topaz / LEGIC ---
        ProxmarkFeature(
            id = "hf-14b-info",
            title = "ISO 14443-B info",
            description = "Read info from ISO 14443-B tags (e.g., SRI, SRIX, CT).",
            category = ProxmarkFeatureCategory.HIGH_FREQUENCY,
            command = "hf 14b info",
            prerequisites = listOf("Place ISO 14443-B tag on reader")
        ),
        ProxmarkFeature(
            id = "hf-topaz-info",
            title = "Topaz / NFC Type 1 info",
            description = "Read info from Topaz (NFC Type 1) tags.",
            category = ProxmarkFeatureCategory.HIGH_FREQUENCY,
            command = "hf topaz info",
            prerequisites = listOf("Place Topaz tag on reader")
        ),
        ProxmarkFeature(
            id = "hf-legic-info",
            title = "LEGIC info",
            description = "Read info from LEGIC Prime or LEGIC Advant tags.",
            category = ProxmarkFeatureCategory.HIGH_FREQUENCY,
            command = "hf legic info",
            prerequisites = listOf("Place LEGIC tag on reader")
        ),

        // --- LF extended ---
        ProxmarkFeature(
            id = "lf-awid-read",
            title = "AWID read",
            description = "Read an AWID-format credential from an LF tag.",
            category = ProxmarkFeatureCategory.EM_HID,
            command = "lf awid reader",
            prerequisites = listOf("Place AWID tag on reader")
        ),
        ProxmarkFeature(
            id = "lf-io-read",
            title = "ioProx read",
            description = "Read an ioProx (Kantech) credential from an LF tag.",
            category = ProxmarkFeatureCategory.EM_HID,
            command = "lf io reader",
            prerequisites = listOf("Place ioProx tag on reader")
        ),
        ProxmarkFeature(
            id = "lf-pyramid-read",
            title = "Pyramid read",
            description = "Read a Farpointe Pyramid credential from an LF tag.",
            category = ProxmarkFeatureCategory.EM_HID,
            command = "lf pyramid reader",
            prerequisites = listOf("Place Pyramid tag on reader")
        ),
        ProxmarkFeature(
            id = "lf-em410x-clone",
            title = "EM410x clone",
            description = "Clone an EM410x ID to a T55xx card.",
            category = ProxmarkFeatureCategory.EM_HID,
            command = "lf em 410x clone --id 0102030405",
            safetyLevel = FeatureSafetyLevel.DESTRUCTIVE,
            prerequisites = listOf("Place T55xx card on reader", "Know the source EM410x ID"),
            warningMessage = "Overwrites the target T55xx card data permanently."
        ),

        // --- Device extended ---
        ProxmarkFeature(
            id = "hw-status",
            title = "Hardware status",
            description = "Show PM3 hardware status including firmware and FPGA info.",
            category = ProxmarkFeatureCategory.DEVICE,
            command = "hw status",
            prerequisites = listOf("Connect PM3 over USB")
        ),
        ProxmarkFeature(
            id = "hw-ping",
            title = "Ping device",
            description = "Ping the PM3 to check it is responding.",
            category = ProxmarkFeatureCategory.DEVICE,
            command = "hw ping",
            prerequisites = listOf("Connect PM3 over USB")
        ),

        // --- Utilities extended ---
        ProxmarkFeature(
            id = "data-samples",
            title = "Get samples",
            description = "Download ADC samples from the PM3 buffer.",
            category = ProxmarkFeatureCategory.UTILITIES,
            command = "data samples",
            prerequisites = listOf("Run a read or snoop command first")
        ),
        ProxmarkFeature(
            id = "data-asc",
            title = "ASCII view",
            description = "Display buffer data as ASCII characters.",
            category = ProxmarkFeatureCategory.UTILITIES,
            command = "data asn1dump",
            prerequisites = listOf("Data in PM3 buffer")
        ),

        // --- Sniffing extended ---
        ProxmarkFeature(
            id = "hf-14a-sniff",
            title = "ISO 14443-A sniff",
            description = "Sniff communication between a reader and an ISO 14443-A tag.",
            category = ProxmarkFeatureCategory.SNIFFING,
            command = "hf 14a sniff",
            safetyLevel = FeatureSafetyLevel.ADVANCED,
            prerequisites = listOf("Place PM3 between reader and tag"),
            warningMessage = "Captures live RF data. May require specific antenna positioning."
        ),
        ProxmarkFeature(
            id = "lf-sniff",
            title = "LF sniff",
            description = "Sniff low-frequency communication.",
            category = ProxmarkFeatureCategory.SNIFFING,
            command = "lf sniff",
            safetyLevel = FeatureSafetyLevel.ADVANCED,
            prerequisites = listOf("Place PM3 near LF reader/tag"),
            warningMessage = "Captures live LF data. May require specific antenna positioning."
        ),

        // ── Parameterized Mifare Classic commands ──
        ProxmarkFeature(
            id = "mfc-read-block",
            title = "Read block",
            description = "Read a single Mifare Classic block by number and key.",
            category = ProxmarkFeatureCategory.MIFARE,
            command = "hf mf rdbl --blk {block} -k {key} --key{keyType}",
            prerequisites = listOf("Connect PM3", "Place Mifare Classic tag"),
            parameters = listOf(
                FeatureParameter(id = "block", label = "Block number", type = ParameterType.NUMBER, hint = "0-63"),
                FeatureParameter(id = "key", label = "Key (hex)", type = ParameterType.HEX, defaultValue = "FFFFFFFFFFFF", hint = "6-byte key"),
                FeatureParameter(id = "keyType", label = "Key type", type = ParameterType.CHOICE, hint = "a|b", defaultValue = "a")
            )
        ),
        ProxmarkFeature(
            id = "mfc-write-block",
            title = "Write block",
            description = "Write 16 hex bytes to a Mifare Classic block.",
            category = ProxmarkFeatureCategory.MIFARE,
            command = "hf mf wrbl --blk {block} -k {key} --key{keyType} -d {data}",
            safetyLevel = FeatureSafetyLevel.DESTRUCTIVE,
            prerequisites = listOf("Connect PM3", "Place Mifare Classic tag"),
            warningMessage = "Writing to wrong block (especially block 0 or sector trailers) can permanently brick the tag.",
            parameters = listOf(
                FeatureParameter(id = "block", label = "Block number", type = ParameterType.NUMBER, hint = "0-63"),
                FeatureParameter(id = "key", label = "Key (hex)", type = ParameterType.HEX, defaultValue = "FFFFFFFFFFFF"),
                FeatureParameter(id = "keyType", label = "Key type", type = ParameterType.CHOICE, hint = "a|b", defaultValue = "a"),
                FeatureParameter(id = "data", label = "Data (32 hex chars)", type = ParameterType.HEX, hint = "16 bytes = 32 hex chars")
            )
        ),
        ProxmarkFeature(
            id = "mfc-change-key",
            title = "Change key",
            description = "Write a new key to a sector trailer on Mifare Classic.",
            category = ProxmarkFeatureCategory.MIFARE,
            command = "hf mf wrbl --blk {trailer} -k {oldKey} --keya -d {newKeyA}FF078069{newKeyB}",
            safetyLevel = FeatureSafetyLevel.DESTRUCTIVE,
            prerequisites = listOf("Connect PM3", "Place Mifare Classic tag"),
            warningMessage = "Incorrect sector trailer write can permanently lock sectors. Triple-check all values.",
            parameters = listOf(
                FeatureParameter(id = "trailer", label = "Sector trailer block", type = ParameterType.NUMBER, hint = "3, 7, 11, 15, …"),
                FeatureParameter(id = "oldKey", label = "Current key A (hex)", type = ParameterType.HEX, defaultValue = "FFFFFFFFFFFF"),
                FeatureParameter(id = "newKeyA", label = "New key A (hex)", type = ParameterType.HEX, defaultValue = "FFFFFFFFFFFF"),
                FeatureParameter(id = "newKeyB", label = "New key B (hex)", type = ParameterType.HEX, defaultValue = "FFFFFFFFFFFF")
            )
        ),

        // ── Parameterized Ultralight commands ──
        ProxmarkFeature(
            id = "mfu-read-page",
            title = "Read page",
            description = "Read a single page (4 bytes) from Mifare Ultralight / NTAG.",
            category = ProxmarkFeatureCategory.MIFARE_ULTRA,
            command = "hf mfu rdbl -b {page}",
            prerequisites = listOf("Connect PM3", "Place Ultralight/NTAG tag"),
            parameters = listOf(
                FeatureParameter(id = "page", label = "Page number", type = ParameterType.NUMBER, hint = "0-44 for NTAG213")
            )
        ),
        ProxmarkFeature(
            id = "mfu-write-page-param",
            title = "Write page (custom)",
            description = "Write 4 hex bytes to a Mifare Ultralight / NTAG page.",
            category = ProxmarkFeatureCategory.MIFARE_ULTRA,
            command = "hf mfu wrbl -b {page} -d {data}",
            safetyLevel = FeatureSafetyLevel.DESTRUCTIVE,
            prerequisites = listOf("Connect PM3", "Place Ultralight/NTAG tag"),
            warningMessage = "Writing OTP or configuration pages may be irreversible.",
            parameters = listOf(
                FeatureParameter(id = "page", label = "Page number", type = ParameterType.NUMBER, hint = "4+"),
                FeatureParameter(id = "data", label = "Data (8 hex chars)", type = ParameterType.HEX, hint = "4 bytes = 8 hex chars")
            )
        ),

        // ── Parameterized T55xx commands ──
        ProxmarkFeature(
            id = "t55xx-read-block",
            title = "Read block",
            description = "Read a T55xx block by number.",
            category = ProxmarkFeatureCategory.T55XX,
            command = "lf t55xx read -b {block}",
            prerequisites = listOf("Connect PM3", "Place T55xx tag"),
            parameters = listOf(
                FeatureParameter(id = "block", label = "Block number", type = ParameterType.NUMBER, hint = "0-7", defaultValue = "0")
            )
        ),
        ProxmarkFeature(
            id = "t55xx-write-block",
            title = "Write block",
            description = "Write data to a T55xx block.",
            category = ProxmarkFeatureCategory.T55XX,
            command = "lf t55xx write -b {block} -d {data}",
            safetyLevel = FeatureSafetyLevel.DESTRUCTIVE,
            prerequisites = listOf("Connect PM3", "Place T55xx tag"),
            warningMessage = "Writing block 0 reconfigures the entire tag. Incorrect config can brick it.",
            parameters = listOf(
                FeatureParameter(id = "block", label = "Block number", type = ParameterType.NUMBER, hint = "0-7"),
                FeatureParameter(id = "data", label = "Data (8 hex chars)", type = ParameterType.HEX, hint = "32-bit word = 8 hex chars")
            )
        ),

        // ── Parameterized EM4x / HID commands ──
        ProxmarkFeature(
            id = "em410x-write",
            title = "Write EM410x ID",
            description = "Write a 5-byte EM410x ID to a T55xx tag.",
            category = ProxmarkFeatureCategory.EM_HID,
            command = "lf em 410x clone --id {id}",
            safetyLevel = FeatureSafetyLevel.DESTRUCTIVE,
            prerequisites = listOf("Connect PM3", "Place T55xx tag"),
            warningMessage = "Overwrites the T55xx tag with EM410x modulation. Previous data will be lost.",
            parameters = listOf(
                FeatureParameter(id = "id", label = "EM410x ID (10 hex chars)", type = ParameterType.HEX, hint = "5-byte ID, e.g. 0102030405")
            )
        ),
        ProxmarkFeature(
            id = "hid-clone-param",
            title = "Clone HID card (custom)",
            description = "Clone an HID Prox card to a T55xx tag with custom raw value.",
            category = ProxmarkFeatureCategory.EM_HID,
            command = "lf hid clone -r {raw}",
            safetyLevel = FeatureSafetyLevel.DESTRUCTIVE,
            prerequisites = listOf("Connect PM3", "Place T55xx tag"),
            warningMessage = "Overwrites the T55xx tag with HID modulation.",
            parameters = listOf(
                FeatureParameter(id = "raw", label = "HID raw (hex)", type = ParameterType.HEX, hint = "Raw hex from 'lf hid read'")
            )
        ),

        // ── Parameterized emulation commands ──
        ProxmarkFeature(
            id = "hf-sim-uid",
            title = "Simulate UID",
            description = "Simulate a custom 14443-A UID on PM3.",
            category = ProxmarkFeatureCategory.EMULATION,
            command = "hf 14a sim -t 1 -u {uid}",
            safetyLevel = FeatureSafetyLevel.ADVANCED,
            prerequisites = listOf("Connect PM3"),
            warningMessage = "Simulates a tag UID. Use responsibly and legally.",
            parameters = listOf(
                FeatureParameter(id = "uid", label = "UID (hex)", type = ParameterType.HEX, hint = "4, 7, or 10 bytes")
            )
        ),

        // ── Built-in NFC features ──
        ProxmarkFeature(
            id = "nfc-read-tag",
            title = "Read NFC tag",
            description = "Read tag UID, type, ATQA, SAK using phone NFC.",
            category = ProxmarkFeatureCategory.NFC_OPERATIONS,
            command = "nfc:read",
            targetReader = ReaderType.BUILTIN_NFC,
            prerequisites = listOf("Enable NFC in phone settings", "Hold tag near phone")
        ),
        ProxmarkFeature(
            id = "nfc-write-ndef-text",
            title = "Write NDEF text",
            description = "Write a plain text NDEF record to an NFC tag.",
            category = ProxmarkFeatureCategory.NFC_OPERATIONS,
            command = "nfc:ndef-text:{text}",
            targetReader = ReaderType.BUILTIN_NFC,
            safetyLevel = FeatureSafetyLevel.ADVANCED,
            prerequisites = listOf("Enable NFC", "Hold NDEF-compatible tag near phone"),
            warningMessage = "Overwrites existing NDEF data on the tag.",
            parameters = listOf(
                FeatureParameter(id = "text", label = "Text to write", hint = "e.g. Hello World")
            )
        ),
        ProxmarkFeature(
            id = "nfc-write-ndef-url",
            title = "Write NDEF URL",
            description = "Write a URL NDEF record to an NFC tag.",
            category = ProxmarkFeatureCategory.NFC_OPERATIONS,
            command = "nfc:ndef-url:{url}",
            targetReader = ReaderType.BUILTIN_NFC,
            safetyLevel = FeatureSafetyLevel.ADVANCED,
            prerequisites = listOf("Enable NFC", "Hold NDEF-compatible tag near phone"),
            warningMessage = "Overwrites existing NDEF data on the tag.",
            parameters = listOf(
                FeatureParameter(id = "url", label = "URL", hint = "https://example.com", defaultValue = "https://")
            )
        ),
        ProxmarkFeature(
            id = "nfc-read-ndef",
            title = "Read NDEF data",
            description = "Read all NDEF records from an NFC tag.",
            category = ProxmarkFeatureCategory.NFC_OPERATIONS,
            command = "nfc:read-ndef",
            targetReader = ReaderType.BUILTIN_NFC,
            prerequisites = listOf("Enable NFC", "Hold NDEF-capable tag near phone")
        ),
        ProxmarkFeature(
            id = "nfc-send-apdu",
            title = "Send raw APDU",
            description = "Send a raw ISO-DEP APDU command to an NFC tag.",
            category = ProxmarkFeatureCategory.NFC_OPERATIONS,
            command = "nfc:apdu:{apdu}",
            targetReader = ReaderType.BUILTIN_NFC,
            safetyLevel = FeatureSafetyLevel.ADVANCED,
            prerequisites = listOf("Enable NFC", "Hold ISO-DEP tag near phone"),
            warningMessage = "Raw APDU commands can modify tag data. Know what you are sending.",
            parameters = listOf(
                FeatureParameter(id = "apdu", label = "APDU (hex)", type = ParameterType.HEX, hint = "e.g. 00A4040007D276000085010100")
            )
        ),
        ProxmarkFeature(
            id = "nfc-format-ndef",
            title = "Format as NDEF",
            description = "Format an NFC tag for NDEF use (erases existing data).",
            category = ProxmarkFeatureCategory.NFC_OPERATIONS,
            command = "nfc:format-ndef",
            targetReader = ReaderType.BUILTIN_NFC,
            safetyLevel = FeatureSafetyLevel.DESTRUCTIVE,
            prerequisites = listOf("Enable NFC", "Hold tag near phone"),
            warningMessage = "Erases all data and formats the tag for NDEF. This cannot be undone."
        ),
        ProxmarkFeature(
            id = "nfc-read-mifare-block",
            title = "Read Mifare block (NFC)",
            description = "Read a Mifare Classic block using phone NFC with key authentication.",
            category = ProxmarkFeatureCategory.NFC_OPERATIONS,
            command = "nfc:mfc-read:{block}:{keyType}:{key}",
            targetReader = ReaderType.BUILTIN_NFC,
            prerequisites = listOf("Enable NFC", "Hold Mifare Classic tag near phone"),
            parameters = listOf(
                FeatureParameter(id = "block", label = "Block number", type = ParameterType.NUMBER, hint = "0-63"),
                FeatureParameter(id = "keyType", label = "Key type", type = ParameterType.CHOICE, hint = "A|B", defaultValue = "A"),
                FeatureParameter(id = "key", label = "Key (hex)", type = ParameterType.HEX, defaultValue = "FFFFFFFFFFFF", hint = "6-byte key")
            )
        )
    )

    val groupedByCategory: Map<ProxmarkFeatureCategory, List<ProxmarkFeature>> =
        allFeatures.groupBy { it.category }
}
