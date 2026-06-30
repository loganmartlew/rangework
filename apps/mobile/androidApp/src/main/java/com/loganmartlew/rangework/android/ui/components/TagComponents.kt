package com.loganmartlew.rangework.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
 * The tag editor: selectable chips for every available tag plus an inline field
 * to create a new Custom Tag. Selecting a tag toggles it on the draft; typing a
 * name that resolves to an existing tag reuses it. Capped at [MAX_TAGS_PER_ITEM].
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
    var newTagName by remember { mutableStateOf("") }
    val selected = selectedTagIds.toSet()
    val atCap = selected.size >= MAX_TAGS_PER_ITEM

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

        if (availableTags.isNotEmpty()) {
            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                availableTags.forEach { tag ->
                    val isSelected = tag.id in selected
                    FilterChip(
                        selected = isSelected,
                        onClick = { onToggle(tag.id) },
                        label = { Text(tag.displayName) },
                        enabled = enabled && (isSelected || !atCap),
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

        OutlinedTextField(
            value = newTagName,
            onValueChange = { newTagName = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Add a tag") },
            supportingText = { Text("Type a name and add it — existing tags are reused") },
            enabled = enabled && !atCap,
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(
                onDone = {
                    val name = newTagName.trim()
                    if (name.isNotEmpty()) {
                        onCreate(name)
                        newTagName = ""
                    }
                },
            ),
            trailingIcon = {
                IconButton(
                    onClick = {
                        val name = newTagName.trim()
                        if (name.isNotEmpty()) {
                            onCreate(name)
                            newTagName = ""
                        }
                    },
                    enabled = enabled && !atCap && newTagName.isNotBlank(),
                ) {
                    Icon(imageVector = Icons.Default.Add, contentDescription = "Add tag")
                }
            },
        )
    }
}

/**
 * A wrapping multi-select tag filter (OR semantics). Renders nothing when there
 * are no tags to filter by. Shows a clear affordance when a selection is active.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun TagFilterBar(
    availableTags: List<Tag>,
    selectedTagIds: Set<String>,
    onToggle: (String) -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (availableTags.isEmpty()) return
    FlowRow(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        availableTags.forEach { tag ->
            FilterChip(
                selected = tag.id in selectedTagIds,
                onClick = { onToggle(tag.id) },
                label = { Text(tag.displayName) },
            )
        }
        if (selectedTagIds.isNotEmpty()) {
            AssistChip(
                onClick = onClear,
                label = { Text("Clear") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = null,
                        modifier = Modifier.size(AssistChipDefaults.IconSize),
                    )
                },
            )
        }
    }
}
