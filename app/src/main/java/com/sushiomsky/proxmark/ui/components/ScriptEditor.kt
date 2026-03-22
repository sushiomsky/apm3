package com.sushiomsky.proxmark.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.sushiomsky.proxmark.plugins.ScriptFile
import com.sushiomsky.proxmark.plugins.ScriptLanguage
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

private val EditorBackground = Color(0xFF0D1117)
private val LineNumberColor = Color(0xFF484F58)
private val CommentColor = Color(0xFF6A737D)
private val CommandColor = Color(0xFF79C0FF)
private val StringColor = Color(0xFF7EE787)
private val KeywordColor = Color(0xFFFF7B72)
private val AccentBlue = Color(0xFF2196F3)
private val AccentGreen = Color(0xFF4CAF50)
private val AccentRed = Color(0xFFF44336)
private val DarkSurface = Color(0xFF1A1A2E)
private val CardDark = Color(0xFF16213E)

private val pm3Commands = setOf(
    "hf", "lf", "data", "hw", "mem", "analyse", "script", "trace",
    "search", "dump", "sim", "clone", "read", "write", "info", "chk",
    "nested", "hardnested", "autopwn", "bruteforce", "sniff", "list"
)

private val luaKeywords = setOf(
    "local", "function", "end", "if", "then", "else", "elseif",
    "for", "while", "do", "return", "nil", "true", "false",
    "and", "or", "not", "in", "repeat", "until", "break"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScriptEditorDialog(
    existingScript: ScriptFile?,
    savedScripts: List<ScriptFile>,
    onSaveScript: (ScriptFile) -> Unit,
    onDeleteScript: (String) -> Unit,
    onRunScript: (ScriptFile) -> Unit,
    onDismiss: () -> Unit
) {
    var scriptName by remember { mutableStateOf(existingScript?.name ?: "untitled.cmd") }
    var content by remember { mutableStateOf(existingScript?.content ?: "") }
    var language by remember { mutableStateOf(existingScript?.language ?: ScriptLanguage.PM3_SCRIPT) }
    var currentScriptId by remember { mutableStateOf(existingScript?.id) }
    var showScriptList by remember { mutableStateOf(false) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            shape = RoundedCornerShape(0.dp),
            color = DarkSurface
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Toolbar
                Surface(color = CardDark, modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Script name
                            OutlinedTextField(
                                value = scriptName,
                                onValueChange = { scriptName = it },
                                singleLine = true,
                                textStyle = TextStyle(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.sp,
                                    color = Color.White
                                ),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = AccentBlue.copy(alpha = 0.5f),
                                    unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                                    cursorColor = Color.White
                                ),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp)
                            )

                            Spacer(modifier = Modifier.width(8.dp))

                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Action bar
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState()),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            // New
                            AssistChip(
                                onClick = {
                                    scriptName = "untitled${language.extension}"
                                    content = ""
                                    currentScriptId = null
                                },
                                label = { Text("New", fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                colors = AssistChipDefaults.assistChipColors(
                                    labelColor = Color.White,
                                    leadingIconContentColor = Color.White
                                ),
                                modifier = Modifier.height(28.dp)
                            )

                            // Open
                            AssistChip(
                                onClick = { showScriptList = true },
                                label = { Text("Open", fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Default.FolderOpen, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                colors = AssistChipDefaults.assistChipColors(
                                    labelColor = Color.White,
                                    leadingIconContentColor = Color.White
                                ),
                                modifier = Modifier.height(28.dp)
                            )

                            // Save
                            AssistChip(
                                onClick = {
                                    val script = ScriptFile(
                                        id = currentScriptId ?: UUID.randomUUID().toString(),
                                        name = scriptName.ifBlank { "untitled${language.extension}" },
                                        content = content,
                                        language = language,
                                        lastModified = System.currentTimeMillis()
                                    )
                                    currentScriptId = script.id
                                    onSaveScript(script)
                                },
                                label = { Text("Save", fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                colors = AssistChipDefaults.assistChipColors(
                                    labelColor = AccentGreen,
                                    leadingIconContentColor = AccentGreen
                                ),
                                modifier = Modifier.height(28.dp)
                            )

                            // Run
                            AssistChip(
                                onClick = {
                                    val script = ScriptFile(
                                        id = currentScriptId ?: UUID.randomUUID().toString(),
                                        name = scriptName,
                                        content = content,
                                        language = language
                                    )
                                    onRunScript(script)
                                },
                                label = { Text("Run", fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                colors = AssistChipDefaults.assistChipColors(
                                    labelColor = AccentBlue,
                                    leadingIconContentColor = AccentBlue
                                ),
                                enabled = content.isNotBlank(),
                                modifier = Modifier.height(28.dp)
                            )

                            // Language toggle
                            AssistChip(
                                onClick = {
                                    language = when (language) {
                                        ScriptLanguage.PM3_SCRIPT -> ScriptLanguage.LUA
                                        ScriptLanguage.LUA -> ScriptLanguage.PM3_SCRIPT
                                    }
                                },
                                label = { Text(language.label, fontSize = 11.sp) },
                                leadingIcon = { Icon(Icons.Default.Code, contentDescription = null, modifier = Modifier.size(16.dp)) },
                                colors = AssistChipDefaults.assistChipColors(
                                    labelColor = Color(0xFFFFC107),
                                    leadingIconContentColor = Color(0xFFFFC107)
                                ),
                                modifier = Modifier.height(28.dp)
                            )
                        }
                    }
                }

                // Editor area
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(EditorBackground)
                ) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        // Line numbers
                        val lines = content.split("\n")
                        Column(
                            modifier = Modifier
                                .width(40.dp)
                                .fillMaxHeight()
                                .background(EditorBackground)
                                .padding(top = 8.dp),
                            horizontalAlignment = Alignment.End
                        ) {
                            lines.forEachIndexed { index, _ ->
                                Text(
                                    "${index + 1}",
                                    style = TextStyle(
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 13.sp,
                                        color = LineNumberColor,
                                        lineHeight = 20.sp
                                    ),
                                    modifier = Modifier.padding(end = 8.dp)
                                )
                            }
                        }

                        // Divider
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(LineNumberColor.copy(alpha = 0.3f))
                        )

                        // Code editor
                        BasicTextField(
                            value = content,
                            onValueChange = { content = it },
                            textStyle = TextStyle(
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = Color.White,
                                lineHeight = 20.sp
                            ),
                            cursorBrush = SolidColor(Color.White),
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(start = 8.dp, top = 8.dp, end = 8.dp),
                            decorationBox = { innerTextField ->
                                Box {
                                    if (content.isEmpty()) {
                                        Text(
                                            "Enter your script here…",
                                            style = TextStyle(
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 13.sp,
                                                color = Color.White.copy(alpha = 0.3f)
                                            )
                                        )
                                    }
                                    innerTextField()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    // Script list dialog
    if (showScriptList) {
        ScriptListDialog(
            scripts = savedScripts,
            onSelect = { script ->
                scriptName = script.name
                content = script.content
                language = script.language
                currentScriptId = script.id
                showScriptList = false
            },
            onDelete = { scriptId ->
                onDeleteScript(scriptId)
                if (currentScriptId == scriptId) {
                    currentScriptId = null
                    scriptName = "untitled${language.extension}"
                    content = ""
                }
            },
            onDismiss = { showScriptList = false }
        )
    }
}

@Composable
private fun ScriptListDialog(
    scripts: List<ScriptFile>,
    onSelect: (ScriptFile) -> Unit,
    onDelete: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardDark,
        title = {
            Text("Saved Scripts", fontWeight = FontWeight.Bold, color = Color.White)
        },
        text = {
            if (scripts.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No saved scripts", color = Color.White.copy(alpha = 0.5f))
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    items(scripts, key = { it.id }) { script ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelect(script) },
                            colors = CardDefaults.cardColors(containerColor = DarkSurface),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = when (script.language) {
                                        ScriptLanguage.PM3_SCRIPT -> Icons.Default.Terminal
                                        ScriptLanguage.LUA -> Icons.Default.Code
                                    },
                                    contentDescription = null,
                                    tint = when (script.language) {
                                        ScriptLanguage.PM3_SCRIPT -> AccentGreen
                                        ScriptLanguage.LUA -> AccentBlue
                                    },
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        script.name,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.SemiBold,
                                        color = Color.White
                                    )
                                    Text(
                                        "${script.language.label} • ${dateFormat.format(Date(script.lastModified))}",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White.copy(alpha = 0.4f)
                                    )
                                }
                                IconButton(
                                    onClick = { onDelete(script.id) },
                                    modifier = Modifier.size(28.dp)
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = AccentRed.copy(alpha = 0.6f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
private fun highlightedCode(text: String, language: ScriptLanguage): androidx.compose.ui.text.AnnotatedString {
    return buildAnnotatedString {
        val lines = text.split("\n")
        lines.forEachIndexed { lineIndex, line ->
            if (lineIndex > 0) append("\n")
            val trimmedLine = line.trimStart()

            when {
                // Comments
                (language == ScriptLanguage.PM3_SCRIPT && trimmedLine.startsWith("#")) ||
                (language == ScriptLanguage.LUA && trimmedLine.startsWith("--")) -> {
                    withStyle(SpanStyle(color = CommentColor)) { append(line) }
                }
                else -> {
                    val words = line.split(Regex("(\\s+)"))
                    var pos = 0
                    words.forEach { word ->
                        val startIdx = line.indexOf(word, pos)
                        if (startIdx > pos) {
                            append(line.substring(pos, startIdx))
                        }
                        val lowerWord = word.lowercase()
                        when {
                            language == ScriptLanguage.PM3_SCRIPT && lowerWord in pm3Commands -> {
                                withStyle(SpanStyle(color = CommandColor)) { append(word) }
                            }
                            language == ScriptLanguage.LUA && lowerWord in luaKeywords -> {
                                withStyle(SpanStyle(color = KeywordColor)) { append(word) }
                            }
                            word.startsWith("\"") || word.startsWith("'") -> {
                                withStyle(SpanStyle(color = StringColor)) { append(word) }
                            }
                            else -> {
                                withStyle(SpanStyle(color = Color.White)) { append(word) }
                            }
                        }
                        pos = startIdx + word.length
                    }
                    if (pos < line.length) {
                        append(line.substring(pos))
                    }
                }
            }
        }
    }
}
