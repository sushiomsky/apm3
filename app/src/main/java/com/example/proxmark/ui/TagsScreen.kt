package com.example.proxmark.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
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
import com.example.proxmark.ui.components.CreateGroupDialog
import com.example.proxmark.ui.components.TagDetailSheet
import com.example.proxmark.ui.components.TagGroupsSection
import com.example.proxmark.ui.components.TagInfoRow
import com.example.proxmark.ui.components.exportTagsAsJson

@Composable
fun TagsScreen(
    savedTags: List<TagInfo>,
    tagGroups: List<TagGroup>,
    lastScan: TagInfo?,
    tagSearchQuery: String,
    tagSelectionMode: Boolean,
    selectedTagIds: Set<String>,
    onSetTagSearchQuery: (String) -> Unit,
    onClearSavedTags: () -> Unit,
    onToggleTagSelectionMode: () -> Unit,
    onToggleTagSelected: (String) -> Unit,
    onDeleteSelectedTags: () -> Unit,
    onCreateTagGroup: (String) -> Unit,
    onDeleteTagGroup: (String) -> Unit,
    onAddTagToGroup: (String, String) -> Unit,
    onRemoveTagFromGroup: (String, String) -> Unit,
    onUpdateTagNote: (String, String, String?) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedTag by remember { mutableStateOf<TagInfo?>(null) }
    var showGroupDialog by remember { mutableStateOf(false) }
    val clipboardManager = LocalClipboardManager.current

    val filteredTags = if (tagSearchQuery.isBlank()) {
        savedTags
    } else {
        savedTags.filter { tag ->
            tag.uid.contains(tagSearchQuery, ignoreCase = true) ||
                tag.type.contains(tagSearchQuery, ignoreCase = true)
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Last scan
        item {
            Card(modifier = Modifier.fillMaxWidth().padding(top = 12.dp)) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Last Scan", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(4.dp))
                    if (lastScan == null) {
                        Text("No tag detected yet", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    } else {
                        TagInfoRow(tag = lastScan, onClick = { selectedTag = lastScan })
                    }
                }
            }
        }

        // Saved tags
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Saved Tags (${savedTags.size})", style = MaterialTheme.typography.titleMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (tagSelectionMode && selectedTagIds.isNotEmpty()) {
                                Button(
                                    onClick = onDeleteSelectedTags,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) { Text("Delete (${selectedTagIds.size})") }
                            }
                            if (savedTags.isNotEmpty()) {
                                OutlinedButton(onClick = onToggleTagSelectionMode) {
                                    Text(if (tagSelectionMode) "Cancel" else "Select")
                                }
                                if (!tagSelectionMode) {
                                    OutlinedButton(onClick = {
                                        val json = exportTagsAsJson(savedTags)
                                        clipboardManager.setText(AnnotatedString(json))
                                    }) { Text("Export") }
                                }
                            }
                            if (!tagSelectionMode) {
                                OutlinedButton(
                                    onClick = onClearSavedTags,
                                    enabled = savedTags.isNotEmpty()
                                ) { Text("Clear") }
                            }
                        }
                    }

                    if (savedTags.size > 3) {
                        OutlinedTextField(
                            value = tagSearchQuery,
                            onValueChange = onSetTagSearchQuery,
                            label = { Text("Search tags") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (filteredTags.isEmpty()) {
                        Text(if (tagSearchQuery.isBlank()) "No tags saved" else "No matching tags")
                    } else {
                        filteredTags.forEach { tag ->
                            val tagKey = "${tag.uid}|${tag.type}"
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                if (tagSelectionMode) {
                                    Checkbox(
                                        checked = tagKey in selectedTagIds,
                                        onCheckedChange = { onToggleTagSelected(tagKey) }
                                    )
                                }
                                Box(modifier = Modifier.weight(1f)) {
                                    TagInfoRow(tag = tag, onClick = {
                                        if (tagSelectionMode) onToggleTagSelected(tagKey)
                                        else selectedTag = tag
                                    })
                                }
                            }
                        }
                    }
                }
            }
        }

        // Tag groups
        if (tagGroups.isNotEmpty() || savedTags.isNotEmpty()) {
            item {
                TagGroupsSection(
                    groups = tagGroups,
                    savedTags = savedTags,
                    onCreateGroup = { showGroupDialog = true },
                    onDeleteGroup = onDeleteTagGroup,
                    onRemoveTagFromGroup = onRemoveTagFromGroup,
                    onTagClick = { tag -> selectedTag = tag }
                )
            }
        }
    }

    // Dialogs
    if (showGroupDialog) {
        CreateGroupDialog(
            onConfirm = { name -> onCreateTagGroup(name); showGroupDialog = false },
            onDismiss = { showGroupDialog = false }
        )
    }

    selectedTag?.let { tag ->
        TagDetailSheet(
            tag = tag,
            groups = tagGroups,
            onDismiss = { selectedTag = null },
            onUpdateNote = { note -> onUpdateTagNote(tag.uid, tag.type, note) },
            onAddToGroup = { groupId -> onAddTagToGroup(groupId, "${tag.uid}|${tag.type}") },
            onRemoveFromGroup = { groupId -> onRemoveTagFromGroup(groupId, "${tag.uid}|${tag.type}") }
        )
    }
}
