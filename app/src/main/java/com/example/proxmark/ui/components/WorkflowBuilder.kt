package com.example.proxmark.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.proxmark.plugins.CustomWorkflow
import com.example.proxmark.plugins.WorkflowStep
import java.util.UUID

private val AccentBlue = Color(0xFF2196F3)
private val AccentGreen = Color(0xFF4CAF50)
private val AccentAmber = Color(0xFFFFC107)
private val AccentRed = Color(0xFFF44336)
private val DarkSurface = Color(0xFF1A1A2E)
private val CardDark = Color(0xFF16213E)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkflowBuilderDialog(
    existingWorkflow: CustomWorkflow?,
    availableCommands: List<String>,
    onSaveWorkflow: (CustomWorkflow) -> Unit,
    onRunWorkflow: (CustomWorkflow) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf(existingWorkflow?.name ?: "") }
    var description by remember { mutableStateOf(existingWorkflow?.description ?: "") }
    var steps by remember {
        mutableStateOf(existingWorkflow?.steps ?: emptyList())
    }
    var editingStepIndex by remember { mutableStateOf<Int?>(null) }
    val clipboardManager = LocalClipboardManager.current

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
                // Header
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    AccentAmber.copy(alpha = 0.7f),
                                    Color(0xFFFF9800).copy(alpha = 0.5f),
                                    DarkSurface
                                )
                            )
                        )
                        .padding(16.dp)
                ) {
                    Column {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                if (existingWorkflow != null) "Edit Workflow" else "New Workflow",
                                style = MaterialTheme.typography.headlineMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            IconButton(onClick = onDismiss) {
                                Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White)
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        OutlinedTextField(
                            value = name,
                            onValueChange = { name = it },
                            label = { Text("Workflow Name", color = Color.White.copy(alpha = 0.7f)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.White.copy(alpha = 0.5f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                cursorColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )

                        Spacer(modifier = Modifier.height(6.dp))

                        OutlinedTextField(
                            value = description,
                            onValueChange = { description = it },
                            label = { Text("Description", color = Color.White.copy(alpha = 0.7f)) },
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = Color.White,
                                unfocusedTextColor = Color.White,
                                focusedBorderColor = Color.White.copy(alpha = 0.5f),
                                unfocusedBorderColor = Color.White.copy(alpha = 0.3f),
                                cursorColor = Color.White
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // Step list
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    if (steps.isEmpty()) {
                        item {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        Icons.Default.PlaylistAdd,
                                        contentDescription = null,
                                        modifier = Modifier.size(48.dp),
                                        tint = Color.White.copy(alpha = 0.3f)
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        "No steps yet — tap + to add one",
                                        color = Color.White.copy(alpha = 0.5f)
                                    )
                                }
                            }
                        }
                    }

                    itemsIndexed(steps, key = { _, step -> step.id }) { index, step ->
                        WorkflowStepCard(
                            index = index,
                            step = step,
                            isFirst = index == 0,
                            isLast = index == steps.lastIndex,
                            onEdit = { editingStepIndex = index },
                            onDelete = {
                                steps = steps.toMutableList().apply { removeAt(index) }
                            },
                            onMoveUp = {
                                if (index > 0) {
                                    steps = steps.toMutableList().apply {
                                        val item = removeAt(index)
                                        add(index - 1, item)
                                    }
                                }
                            },
                            onMoveDown = {
                                if (index < steps.lastIndex) {
                                    steps = steps.toMutableList().apply {
                                        val item = removeAt(index)
                                        add(index + 1, item)
                                    }
                                }
                            }
                        )
                    }

                    // Add step button
                    item {
                        OutlinedButton(
                            onClick = {
                                val newStep = WorkflowStep(
                                    id = UUID.randomUUID().toString(),
                                    title = "Step ${steps.size + 1}",
                                    command = ""
                                )
                                steps = steps + newStep
                                editingStepIndex = steps.size // will be the new last index after add
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = AccentBlue)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Add Step")
                        }
                    }
                }

                // Bottom controls
                Surface(
                    color = CardDark,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // Export
                        OutlinedButton(
                            onClick = {
                                val text = buildWorkflowExportText(name, description, steps)
                                clipboardManager.setText(AnnotatedString(text))
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Export", fontSize = 12.sp)
                        }

                        // Save
                        Button(
                            onClick = {
                                val workflow = CustomWorkflow(
                                    id = existingWorkflow?.id ?: UUID.randomUUID().toString(),
                                    name = name.ifBlank { "Untitled Workflow" },
                                    description = description,
                                    steps = steps,
                                    createdAt = existingWorkflow?.createdAt ?: System.currentTimeMillis()
                                )
                                onSaveWorkflow(workflow)
                            },
                            enabled = name.isNotBlank() && steps.isNotEmpty(),
                            colors = ButtonDefaults.buttonColors(containerColor = AccentGreen),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save", fontSize = 12.sp)
                        }

                        // Run
                        Button(
                            onClick = {
                                val workflow = CustomWorkflow(
                                    id = existingWorkflow?.id ?: UUID.randomUUID().toString(),
                                    name = name.ifBlank { "Untitled Workflow" },
                                    description = description,
                                    steps = steps,
                                    createdAt = existingWorkflow?.createdAt ?: System.currentTimeMillis()
                                )
                                onRunWorkflow(workflow)
                            },
                            enabled = steps.any { it.command.isNotBlank() },
                            colors = ButtonDefaults.buttonColors(containerColor = AccentBlue),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Run", fontSize = 12.sp)
                        }
                    }
                }
            }
        }
    }

    // Step editor dialog
    editingStepIndex?.let { index ->
        if (index in steps.indices) {
            StepEditorDialog(
                step = steps[index],
                availableCommands = availableCommands,
                onSave = { updatedStep ->
                    steps = steps.toMutableList().apply { set(index, updatedStep) }
                    editingStepIndex = null
                },
                onDismiss = { editingStepIndex = null }
            )
        } else {
            editingStepIndex = null
        }
    }
}

@Composable
private fun WorkflowStepCard(
    index: Int,
    step: WorkflowStep,
    isFirst: Boolean,
    isLast: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = CardDark),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Step number badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = AccentBlue.copy(alpha = 0.2f),
                modifier = Modifier.size(36.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.fillMaxSize()) {
                    Text(
                        "${index + 1}",
                        fontWeight = FontWeight.Bold,
                        color = AccentBlue,
                        fontSize = 16.sp
                    )
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    step.title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                if (step.command.isNotBlank()) {
                    Text(
                        step.command,
                        style = MaterialTheme.typography.bodySmall,
                        fontFamily = FontFamily.Monospace,
                        color = AccentGreen.copy(alpha = 0.8f)
                    )
                }
                if (step.successPattern != null || step.failurePattern != null) {
                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        step.successPattern?.let {
                            Text("✓ $it", fontSize = 9.sp, color = AccentGreen.copy(alpha = 0.6f))
                        }
                        step.failurePattern?.let {
                            Text("✗ $it", fontSize = 9.sp, color = AccentRed.copy(alpha = 0.6f))
                        }
                    }
                }
            }

            // Reorder & action buttons
            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                IconButton(onClick = onMoveUp, enabled = !isFirst, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = "Move up", tint = if (!isFirst) Color.White else Color.White.copy(alpha = 0.2f), modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onMoveDown, enabled = !isLast, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = "Move down", tint = if (!isLast) Color.White else Color.White.copy(alpha = 0.2f), modifier = Modifier.size(18.dp))
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(0.dp)) {
                IconButton(onClick = onEdit, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", tint = AccentAmber, modifier = Modifier.size(16.dp))
                }
                IconButton(onClick = onDelete, modifier = Modifier.size(28.dp)) {
                    Icon(Icons.Default.Close, contentDescription = "Delete", tint = AccentRed.copy(alpha = 0.7f), modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StepEditorDialog(
    step: WorkflowStep,
    availableCommands: List<String>,
    onSave: (WorkflowStep) -> Unit,
    onDismiss: () -> Unit
) {
    var title by remember { mutableStateOf(step.title) }
    var command by remember { mutableStateOf(step.command) }
    var desc by remember { mutableStateOf(step.description) }
    var waitForResult by remember { mutableStateOf(step.waitForResult) }
    var successPattern by remember { mutableStateOf(step.successPattern ?: "") }
    var failurePattern by remember { mutableStateOf(step.failurePattern ?: "") }
    var showSuggestions by remember { mutableStateOf(false) }

    val filteredSuggestions = if (command.isNotBlank()) {
        availableCommands.filter { it.contains(command, ignoreCase = true) }.take(5)
    } else emptyList()

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = CardDark,
        title = {
            Text("Edit Step", fontWeight = FontWeight.Bold, color = Color.White)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White
                    )
                )

                OutlinedTextField(
                    value = command,
                    onValueChange = {
                        command = it
                        showSuggestions = it.isNotBlank()
                    },
                    label = { Text("Command") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AccentGreen,
                        unfocusedTextColor = AccentGreen,
                        cursorColor = AccentGreen
                    ),
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace)
                )

                // Command autocomplete suggestions
                if (showSuggestions && filteredSuggestions.isNotEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = DarkSurface,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            filteredSuggestions.forEach { suggestion ->
                                TextButton(
                                    onClick = {
                                        command = suggestion
                                        showSuggestions = false
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        suggestion,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        color = Color.White.copy(alpha = 0.8f),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }
                    }
                }

                OutlinedTextField(
                    value = desc,
                    onValueChange = { desc = it },
                    label = { Text("Description (optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        cursorColor = Color.White
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Wait for result", color = Color.White.copy(alpha = 0.7f))
                    Switch(
                        checked = waitForResult,
                        onCheckedChange = { waitForResult = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = AccentBlue)
                    )
                }

                OutlinedTextField(
                    value = successPattern,
                    onValueChange = { successPattern = it },
                    label = { Text("Success pattern (regex, optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AccentGreen,
                        unfocusedTextColor = AccentGreen,
                        cursorColor = AccentGreen
                    ),
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                )

                OutlinedTextField(
                    value = failurePattern,
                    onValueChange = { failurePattern = it },
                    label = { Text("Failure pattern (regex, optional)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = AccentRed,
                        unfocusedTextColor = AccentRed,
                        cursorColor = AccentRed
                    ),
                    textStyle = LocalTextStyle.current.copy(fontFamily = FontFamily.Monospace, fontSize = 12.sp)
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onSave(
                        step.copy(
                            title = title.ifBlank { "Untitled Step" },
                            command = command,
                            description = desc,
                            waitForResult = waitForResult,
                            successPattern = successPattern.ifBlank { null },
                            failurePattern = failurePattern.ifBlank { null }
                        )
                    )
                },
                colors = ButtonDefaults.buttonColors(containerColor = AccentBlue)
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

private fun buildWorkflowExportText(
    name: String,
    description: String,
    steps: List<WorkflowStep>
): String = buildString {
    appendLine("# Workflow: $name")
    if (description.isNotBlank()) appendLine("# $description")
    appendLine()
    steps.forEachIndexed { i, step ->
        appendLine("# Step ${i + 1}: ${step.title}")
        if (step.description.isNotBlank()) appendLine("# ${step.description}")
        appendLine(step.command)
        appendLine()
    }
}
