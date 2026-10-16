package com.sushiomsky.proxmark.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.sushiomsky.proxmark.repository.TagGroup
import com.sushiomsky.proxmark.repository.TagInfo
import com.sushiomsky.proxmark.ui.components.CreateGroupDialog
import com.sushiomsky.proxmark.ui.components.TagDetailSheet
import com.sushiomsky.proxmark.ui.components.TagGroupsSection
import com.sushiomsky.proxmark.ui.components.TagInfoRow

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
    onExportTags: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    var selectedTag by remember { mutableStateOf<TagInfo?>(null) }
    var showGroupDialog by rememberSaveable { mutableStateOf(false) }
    var groupsExpanded by rememberSaveable { mutableStateOf(true) }
    // "all" | "selected" | null — destructive deletes need explicit confirmation.
    var confirmDeleteTarget by rememberSaveable { mutableStateOf<String?>(null) }

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
            .padding(horizontal = 12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Last scan - compact inline
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Last Scan", style = MaterialTheme.typography.labelMedium)
                if (lastScan != null) {
                    Text(
                        lastScan.uid,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.clickable { selectedTag = lastScan }
                    )
                    Text(
                        lastScan.type,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        "No tag detected yet",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Saved tags
        item {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Saved Tags (${savedTags.size})", style = MaterialTheme.typography.titleSmall)
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            if (tagSelectionMode && selectedTagIds.isNotEmpty()) {
                                Button(
                                    onClick = { confirmDeleteTarget = "selected" },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    )
                                ) { Text("Delete (${selectedTagIds.size})", style = MaterialTheme.typography.labelSmall) }
                            }
                            if (savedTags.isNotEmpty()) {
                                OutlinedButton(onClick = onToggleTagSelectionMode) {
                                    Text(if (tagSelectionMode) "Cancel" else "Select", style = MaterialTheme.typography.labelSmall)
                                }
                                if (!tagSelectionMode) {
                                    OutlinedButton(onClick = onExportTags) { Text("Export", style = MaterialTheme.typography.labelSmall) }
                                }
                            }
                            if (!tagSelectionMode) {
                                OutlinedButton(
                                    onClick = { confirmDeleteTarget = "all" },
                                    enabled = savedTags.isNotEmpty()
                                ) { Text("Clear", style = MaterialTheme.typography.labelSmall) }
                            }
                        }
                    }

                    if (savedTags.size > 3) {
                        OutlinedTextField(
                            value = tagSearchQuery,
                            onValueChange = onSetTagSearchQuery,
                            label = { Text("Search tags") },
                            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
                            trailingIcon = {
                                if (tagSearchQuery.isNotEmpty()) {
                                    IconButton(onClick = { onSetTagSearchQuery("") }) {
                                        Icon(Icons.Filled.Close, contentDescription = "Clear search")
                                    }
                                }
                            },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (filteredTags.isEmpty()) {
                        Text(
                            if (tagSearchQuery.isBlank()) "No tags saved" else "No matching tags",
                            style = MaterialTheme.typography.bodySmall
                        )
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

        // Tag groups - collapsible
        if (tagGroups.isNotEmpty() || savedTags.isNotEmpty()) {
            item {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { groupsExpanded = !groupsExpanded }
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Tag Groups", style = MaterialTheme.typography.labelMedium)
                        Text(
                            if (groupsExpanded) "\u25B2" else "\u25BC",
                            style = MaterialTheme.typography.labelSmall
                        )
                    }
                    AnimatedVisibility(visible = groupsExpanded) {
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
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }

    // Dialogs
    if (showGroupDialog) {
        CreateGroupDialog(
            onConfirm = { name -> onCreateTagGroup(name); showGroupDialog = false },
            onDismiss = { showGroupDialog = false }
        )
    }

    confirmDeleteTarget?.let { target ->
        val isAll = target == "all"
        AlertDialog(
            onDismissRequest = { confirmDeleteTarget = null },
            title = { Text(if (isAll) "Clear all saved tags?" else "Delete selected tags?") },
            text = {
                Text(
                    if (isAll) "This permanently removes all ${savedTags.size} saved tags. This cannot be undone."
                    else "This permanently removes ${selectedTagIds.size} selected tags. This cannot be undone.",
                    style = MaterialTheme.typography.bodySmall
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (isAll) onClearSavedTags() else onDeleteSelectedTags()
                        confirmDeleteTarget = null
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error
                    )
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteTarget = null }) { Text("Cancel") }
            }
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
