package com.loganmartlew.rangework.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.shared.model.MAX_TAGS_PER_ITEM
import com.loganmartlew.rangework.shared.model.Tag

/** A non-interactive pill showing a tag name, for list/detail surfaces. */
@Composable
internal fun TagChip(
    name: String,
    modifier: Modifier = Modifier,
) {
    AssistChip(
        onClick = {},
        enabled = false,
        label = { Text(name) },
        modifier = modifier,
        colors = AssistChipDefaults.assistChipColors(
            disabledLabelColor = MaterialTheme.colorScheme.onSurfaceVariant,
        ),
    )
}

/** A wrapping row of read-only tag chips. Renders nothing when [tags] is empty. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TagChipRow(
    tags: List<Tag>,
    modifier: Modifier = Modifier,
) {
    if (tags.isEmpty()) return
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        tags.forEach { tag ->
            TagChip(name = tag.displayName)
        }
    }
}

/**
 * The tag editor: shows only the tags attached to the draft as removable chips,
 * plus an "Add tags" chip that opens a bottom-sheet picker with search and
 * inline creation. Capped at [MAX_TAGS_PER_ITEM].
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TagPicker(
    availableTags: List<Tag>,
    selectedTagIds: List<String>,
    enabled: Boolean,
    onToggle: (String) -> Unit,
    onCreate: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var showSheet by remember { mutableStateOf(false) }
    val selected = selectedTagIds.toSet()
    val atCap = selected.size >= MAX_TAGS_PER_ITEM
    val selectedTags = selectedTagIds.mapNotNull { id -> availableTags.firstOrNull { it.id == id } }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                text = "TAGS",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = "${selected.size}/$MAX_TAGS_PER_ITEM",
                style = MaterialTheme.typography.labelMedium,
                color = if (atCap) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            selectedTags.forEach { tag ->
                InputChip(
                    selected = true,
                    onClick = { onToggle(tag.id) },
                    label = { Text(tag.displayName) },
                    enabled = enabled,
                    trailingIcon = {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Remove ${tag.displayName}",
                            modifier = Modifier.size(InputChipDefaults.IconSize),
                        )
                    },
                )
            }
            AssistChip(
                onClick = { showSheet = true },
                label = { Text(if (selectedTags.isEmpty()) "Add tags" else "Add") },
                enabled = enabled,
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                    )
                },
            )
        }
    }

    if (showSheet) {
        TagPickerSheet(
            availableTags = availableTags,
            selectedTagIds = selected,
            atCap = atCap,
            onToggle = onToggle,
            onCreate = onCreate,
            onDismiss = { showSheet = false },
        )
    }
}

/**
 * Bottom-sheet body for [TagPicker]: search across all available tags, toggle
 * chips, and a "Create" affordance when the query matches no existing name.
 * Stays open across toggles so several tags can be added in one visit.
 */
@OptIn(ExperimentalLayoutApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun TagPickerSheet(
    availableTags: List<Tag>,
    selectedTagIds: Set<String>,
    atCap: Boolean,
    onToggle: (String) -> Unit,
    onCreate: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var query by remember { mutableStateOf("") }
    val trimmed = query.trim()
    val visibleTags = if (trimmed.isEmpty()) {
        availableTags
    } else {
        availableTags.filter { it.displayName.contains(trimmed, ignoreCase = true) }
    }
    val canCreate = trimmed.isNotEmpty() &&
        availableTags.none { it.displayName.equals(trimmed, ignoreCase = true) }
    val create = {
        if (canCreate && !atCap) {
            onCreate(trimmed)
            query = ""
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .imePadding()
                .padding(horizontal = 24.dp)
                .padding(bottom = 40.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("Tags", style = MaterialTheme.typography.titleLarge)
                Text(
                    text = "${selectedTagIds.size}/$MAX_TAGS_PER_ITEM",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (atCap) MaterialTheme.colorScheme.error
                    else MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            OutlinedTextField(
                value = query,
                onValueChange = { query = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Search or create a tag") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Search, contentDescription = null)
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { create() }),
            )

            if (canCreate) {
                AssistChip(
                    onClick = create,
                    label = { Text("Create \"$trimmed\"") },
                    enabled = !atCap,
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            modifier = Modifier.size(AssistChipDefaults.IconSize),
                        )
                    },
                )
            }

            if (visibleTags.isEmpty() && !canCreate) {
                Text(
                    text = "No tags yet — type a name to create one.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    visibleTags.forEach { tag ->
                        val isSelected = tag.id in selectedTagIds
                        FilterChip(
                            selected = isSelected,
                            onClick = { onToggle(tag.id) },
                            label = { Text(tag.displayName) },
                            enabled = isSelected || !atCap,
                            leadingIcon = if (isSelected) {
                                {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(FilterChipDefaults.IconSize),
                                    )
                                }
                            } else null,
                        )
                    }
                }
            }
        }
    }
}

/**
 * A compact multi-select tag filter (OR semantics): a single "Tags" chip that
 * opens a dropdown of all tags with checkmarks. The chip shows the active
 * count and a clear entry lives at the bottom of the menu. Renders nothing
 * when there are no tags to filter by.
 */
@Composable
internal fun TagFilterBar(
    availableTags: List<Tag>,
    selectedTagIds: Set<String>,
    onToggle: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (availableTags.isEmpty()) return
    var expanded by remember { mutableStateOf(false) }
    val count = selectedTagIds.size

    Box(modifier = modifier) {
        FilterChip(
            selected = count > 0,
            onClick = { expanded = true },
            label = { Text(if (count > 0) "Tags · $count" else "Tags") },
            trailingIcon = {
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    modifier = Modifier.size(FilterChipDefaults.IconSize),
                )
            },
        )
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            availableTags.forEach { tag ->
                val isSelected = tag.id in selectedTagIds
                DropdownMenuItem(
                    text = { Text(tag.displayName) },
                    onClick = { onToggle(tag.id) },
                    leadingIcon = {
                        if (isSelected) {
                            Icon(imageVector = Icons.Default.Check, contentDescription = null)
                        } else {
                            Spacer(modifier = Modifier.size(24.dp))
                        }
                    },
                )
            }
            if (count > 0) {
                HorizontalDivider()
                DropdownMenuItem(
                    text = { Text("Clear filters") },
                    onClick = {
                        onClear()
                        expanded = false
                    },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Close, contentDescription = null)
                    },
                )
            }
        }
    }
}
