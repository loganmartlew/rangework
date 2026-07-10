package com.loganmartlew.rangework.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.shared.model.BlockResult

/**
 * The passive per-Block capture affordance on the execution block screen
 * (snapshot v3 only). Collapsed by default — a single "Block result" header —
 * so it never prompts or badges; expansion is deliberate.
 *
 * Expanded, every block gets a free-text note with an explicit Save (P2). Blocks
 * whose unit has a Success Criterion and did *not* enable the Success Observation
 * Type ([manualCountEligible]) also get a manual X-of-Y count row: the criterion
 * rubric verbatim, then either "Not counted"/Add count, or a stepper bounded to
 * `0..totalBalls` with Remove count. Count edits write immediately (optimistic);
 * note edits wait for Save. Derived (Success-enabled) counts render nothing here —
 * the tally surface is Stage 5's.
 */
@Composable
internal fun BlockResultSection(
    blockResult: BlockResult?,
    isSavingNote: Boolean,
    onSaveNote: (String?) -> Unit,
    manualCountEligible: Boolean,
    successCriterion: String?,
    totalBalls: Int,
    onSetManualCount: (Int?) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Card(modifier = modifier.fillMaxWidth()) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(horizontal = 16.dp, vertical = 12.dp)
                    .semantics {
                        contentDescription =
                            if (expanded) "Collapse block result" else "Expand block result"
                    },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Block result".uppercase(),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Icon(
                    imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, bottom = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    BlockNoteEditor(
                        savedNote = blockResult?.note,
                        isSaving = isSavingNote,
                        onSave = onSaveNote,
                    )

                    if (manualCountEligible) {
                        HorizontalDivider()
                        ManualCountRow(
                            criterion = successCriterion,
                            count = blockResult?.manualCount,
                            totalBalls = totalBalls,
                            onSetCount = onSetManualCount,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BlockNoteEditor(
    savedNote: String?,
    isSaving: Boolean,
    onSave: (String?) -> Unit,
) {
    var draft by rememberSaveable { mutableStateOf(savedNote ?: "") }
    val normalized = draft.trim().takeIf(String::isNotEmpty)
    val isDirty = normalized != savedNote

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = draft,
            onValueChange = { draft = it },
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Block note text field" },
            placeholder = { Text("Note on this block") },
            minLines = 2,
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.End,
        ) {
            when {
                isSaving -> CircularProgressIndicator(
                    modifier = Modifier
                        .size(18.dp)
                        .semantics { contentDescription = "Saving block note" },
                    strokeWidth = 2.dp,
                )

                !isDirty && savedNote != null -> Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.semantics { contentDescription = "Block note saved" },
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text = "Saved",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }

                else -> TextButton(
                    onClick = { onSave(normalized) },
                    enabled = isDirty,
                    modifier = Modifier.semantics { contentDescription = "Save block note" },
                ) {
                    Text("Save")
                }
            }
        }
    }
}

@Composable
private fun ManualCountRow(
    criterion: String?,
    count: Int?,
    totalBalls: Int,
    onSetCount: (Int?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        // The rubric where the number is entered — without it "X of Y" is the
        // bare number the design disallows.
        criterion?.let {
            Text(
                text = it,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        if (count == null) {
            // Unset ≠ 0: idle as "Not counted" with an Add count affordance.
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = "Not counted",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                TextButton(
                    onClick = { onSetCount(0) },
                    modifier = Modifier.semantics { contentDescription = "Add success count" },
                ) {
                    Text("Add count")
                }
            }
        } else {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                CountStepper(
                    value = count,
                    onValueChange = { onSetCount(it) },
                    min = 0,
                    max = totalBalls,
                    label = "Success count",
                )
                Text(
                    text = "of $totalBalls balls",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f),
                )
                TextButton(
                    onClick = { onSetCount(null) },
                    modifier = Modifier.semantics { contentDescription = "Remove success count" },
                ) {
                    Text("Remove")
                }
            }
        }
    }
}
