package com.example.proxmark.ui.components

import android.text.format.DateUtils
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.example.proxmark.repository.TagGroup
import com.example.proxmark.repository.TagInfo

@Composable
internal fun TagInfoRow(tag: TagInfo, onClick: () -> Unit) {
    val relativeTime = DateUtils.getRelativeTimeSpanString(
        tag.timestamp,
        System.currentTimeMillis(),
        DateUtils.SECOND_IN_MILLIS
    )
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = tag.uid,
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "tap for details",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
        Text(
            text = buildString {
                append(tag.type)
                tag.atqa?.let { append("  ATQA: $it") }
                tag.sak?.let { append("  SAK: $it") }
            },
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = relativeTime.toString(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        tag.note?.let { note ->
            Text(
                text = "📝 $note",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
                maxLines = 1
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun TagDetailSheet(
    tag: TagInfo,
    groups: List<TagGroup>,
    onDismiss: () -> Unit,
    onUpdateNote: (String?) -> Unit,
    onAddToGroup: (String) -> Unit,
    onRemoveFromGroup: (String) -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val sheetState = rememberModalBottomSheetState()
    val relativeTime = DateUtils.getRelativeTimeSpanString(
        tag.timestamp,
        System.currentTimeMillis(),
        DateUtils.SECOND_IN_MILLIS
    )
    var editingNote by remember(tag.uid, tag.type) { mutableStateOf(tag.note ?: "") }
    val tagKey = "${tag.uid}|${tag.type}"
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Tag Details", style = MaterialTheme.typography.headlineSmall)
            HorizontalDivider()
            TagDetailField(label = "UID", value = tag.uid) {
                clipboardManager.setText(AnnotatedString(tag.uid))
            }
            TagDetailField(label = "Type", value = tag.type) {
                clipboardManager.setText(AnnotatedString(tag.type))
            }
            tag.atqa?.let { atqa ->
                TagDetailField(label = "ATQA", value = atqa) {
                    clipboardManager.setText(AnnotatedString(atqa))
                }
            }
            tag.sak?.let { sak ->
                TagDetailField(label = "SAK", value = sak) {
                    clipboardManager.setText(AnnotatedString(sak))
                }
            }
            TagDetailField(label = "Discovered", value = relativeTime.toString(), onCopy = null)

            HorizontalDivider()
            Text("Note", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            OutlinedTextField(
                value = editingNote,
                onValueChange = { editingNote = it },
                placeholder = { Text("Add a note for this tag…") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = false,
                maxLines = 3
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { onUpdateNote(editingNote.ifBlank { null }) }
                ) {
                    Text("Save note")
                }
                if (tag.note != null) {
                    OutlinedButton(onClick = {
                        editingNote = ""
                        onUpdateNote(null)
                    }) {
                        Text("Clear note")
                    }
                }
            }

            // Group membership
            if (groups.isNotEmpty()) {
                HorizontalDivider()
                Text("Groups", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                groups.forEach { group ->
                    val inGroup = tagKey in group.tagKeys
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                if (inGroup) onRemoveFromGroup(group.id)
                                else onAddToGroup(group.id)
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Checkbox(
                            checked = inGroup,
                            onCheckedChange = {
                                if (inGroup) onRemoveFromGroup(group.id)
                                else onAddToGroup(group.id)
                            }
                        )
                        Text(group.name, style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
internal fun TagDetailField(label: String, value: String, onCopy: (() -> Unit)?) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.bodyLarge)
        }
        if (onCopy != null) {
            TextButton(onClick = onCopy) { Text("Copy") }
        }
    }
}

internal fun exportTagsAsJson(tags: List<TagInfo>): String {
    val entries = tags.joinToString(",\n  ") { tag ->
        buildString {
            append("{\"uid\":\"${tag.uid}\",\"type\":\"${tag.type}\"")
            tag.atqa?.let { append(",\"atqa\":\"$it\"") }
            tag.sak?.let { append(",\"sak\":\"$it\"") }
            append(",\"timestamp\":${tag.timestamp}}")
        }
    }
    return "[\n  $entries\n]"
}

@Composable
internal fun CreateGroupDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Tag Group") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Group name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name.trim()) },
                enabled = name.isNotBlank()
            ) { Text("Create") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
internal fun TagGroupsSection(
    groups: List<TagGroup>,
    savedTags: List<TagInfo>,
    onCreateGroup: () -> Unit,
    onDeleteGroup: (String) -> Unit,
    onRemoveTagFromGroup: (String, String) -> Unit,
    onTagClick: (TagInfo) -> Unit
) {
    val tagsByKey = remember(savedTags) {
        savedTags.associateBy { "${it.uid}|${it.type}" }
    }
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Tag Groups", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = onCreateGroup) { Text("+ New") }
            }
            if (groups.isEmpty()) {
                Text(
                    "No groups yet. Create one to organize your tags.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                groups.forEach { group ->
                    var expanded by remember { mutableStateOf(false) }
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { expanded = !expanded },
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    "${group.name} (${group.tagKeys.size})",
                                    style = MaterialTheme.typography.bodyMedium
                                )
                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(if (expanded) "▲" else "▼", style = MaterialTheme.typography.bodySmall)
                                    TextButton(onClick = { onDeleteGroup(group.id) }) {
                                        Text("✕", color = MaterialTheme.colorScheme.error)
                                    }
                                }
                            }
                            if (expanded) {
                                if (group.tagKeys.isEmpty()) {
                                    Text(
                                        "No tags. Tap a tag → Details → Groups to add.",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                } else {
                                    group.tagKeys.forEach { key ->
                                        val tag = tagsByKey[key]
                                        if (tag != null) {
                                            Row(
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .clickable { onTagClick(tag) }
                                                    .padding(vertical = 2.dp),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Column(modifier = Modifier.weight(1f)) {
                                                    Text(tag.uid, style = MaterialTheme.typography.bodySmall)
                                                    Text(tag.type, style = MaterialTheme.typography.labelSmall,
                                                        color = MaterialTheme.colorScheme.onSurfaceVariant)
                                                }
                                                TextButton(onClick = { onRemoveTagFromGroup(group.id, key) }) {
                                                    Text("Remove", style = MaterialTheme.typography.labelSmall)
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
