package com.loganmartlew.rangework.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.ui.SettingsUiState
import com.loganmartlew.rangework.android.ui.components.DeleteConfirmationDialog
import com.loganmartlew.rangework.android.ui.components.ScrollableScreen
import com.loganmartlew.rangework.android.ui.components.SettingsSubheader
import com.loganmartlew.rangework.android.ui.components.TagChip
import com.loganmartlew.rangework.shared.model.Tag
import com.loganmartlew.rangework.shared.model.TagAttachmentCounts

@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ManageTagsScreen(
    settingsUiState: SettingsUiState,
    onRenameTag: (String, String) -> Unit,
    onDeleteTag: (String) -> Unit,
    onLoadAttachmentCounts: (String, (TagAttachmentCounts) -> Unit) -> Unit,
) {
    var tagBeingRenamed by remember { mutableStateOf<Tag?>(null) }
    var tagBeingDeleted by remember { mutableStateOf<Tag?>(null) }
    var deleteCounts by remember { mutableStateOf<TagAttachmentCounts?>(null) }

    tagBeingRenamed?.let { tag ->
        RenameTagDialog(
            tag = tag,
            onConfirm = { newName ->
                onRenameTag(tag.id, newName)
                tagBeingRenamed = null
            },
            onDismiss = { tagBeingRenamed = null },
        )
    }

    tagBeingDeleted?.let { tag ->
        val counts = deleteCounts
        val warning = counts?.let {
            val total = it.unitCount + it.sessionCount
            if (total == 0) {
                "This tag isn't attached to anything."
            } else {
                "Attached to ${it.unitCount} unit${if (it.unitCount == 1) "" else "s"} and " +
                    "${it.sessionCount} session${if (it.sessionCount == 1) "" else "s"}. " +
                    "It will be removed from all of them."
            }
        }
        DeleteConfirmationDialog(
            itemName = tag.displayName,
            warning = warning,
            onConfirm = {
                onDeleteTag(tag.id)
                tagBeingDeleted = null
                deleteCounts = null
            },
            onDismiss = {
                tagBeingDeleted = null
                deleteCounts = null
            },
        )
    }

    ScrollableScreen {
        SettingsSubheader("Your tags")
        if (settingsUiState.customTags.isEmpty()) {
            Text(
                text = "You haven't created any custom tags yet. Add tags while editing a unit or session.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(vertical = 8.dp),
            )
        } else {
            Column(modifier = Modifier.fillMaxWidth()) {
                settingsUiState.customTags.forEachIndexed { index, tag ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = tag.displayName,
                            style = MaterialTheme.typography.bodyLarge,
                            modifier = Modifier.weight(1f),
                        )
                        IconButton(
                            onClick = { tagBeingRenamed = tag },
                            enabled = !settingsUiState.isWorking,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Rename ${tag.displayName}",
                            )
                        }
                        IconButton(
                            onClick = {
                                deleteCounts = null
                                tagBeingDeleted = tag
                                onLoadAttachmentCounts(tag.id) { counts -> deleteCounts = counts }
                            },
                            enabled = !settingsUiState.isWorking,
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete ${tag.displayName}",
                                tint = MaterialTheme.colorScheme.error,
                            )
                        }
                    }
                    if (index != settingsUiState.customTags.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }

        SettingsSubheader("Default tags")
        Text(
            text = "Default tags are shared and can't be edited or deleted.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 8.dp),
        )
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            settingsUiState.defaultTags.forEach { tag ->
                TagChip(name = tag.displayName)
            }
        }
    }
}

@Composable
private fun RenameTagDialog(
    tag: Tag,
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var name by remember(tag.id) { mutableStateOf(tag.displayName) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Rename tag") },
        text = {
            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                label = { Text("Tag name") },
                singleLine = true,
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(name.trim()) },
                enabled = name.trim().isNotEmpty() && name.trim() != tag.displayName,
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}
