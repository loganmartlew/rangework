package com.loganmartlew.rangework.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp

/** True when [draft], normalized, differs from the saved value — i.e. a Save would change something. */
internal fun noteIsDirty(draft: String, savedNote: String?): Boolean =
    draft.trim().takeIf(String::isNotEmpty) != savedNote

/** [draft] normalized to the value a save would persist (null clears). */
internal fun normalizedNote(draft: String): String? = draft.trim().takeIf(String::isNotEmpty)

/**
 * A reusable prose-note editor card: a labelled multiline field with an explicit
 * Save that enables only when the draft differs from the saved value (P2 — a
 * visible, verifiable save at the range beats save-on-keystroke or invisible
 * focus-loss writes). Blank/whitespace saves as a clear (null).
 *
 * The draft is hoisted so callers can flush it on their own actions (the finish
 * summary's Done button flushes a dirty session note before navigating). Callers
 * hold it in `rememberSaveable` so rotation/process death keep unsaved text; the
 * saved value re-derives from the model.
 */
@Composable
internal fun SessionNoteCard(
    label: String,
    draft: String,
    onDraftChange: (String) -> Unit,
    savedNote: String?,
    isSaving: Boolean,
    onSave: (String?) -> Unit,
    modifier: Modifier = Modifier,
    placeholder: String = "Add a note",
) {
    val isDirty = noteIsDirty(draft, savedNote)

    Card(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text(
                text = label.uppercase(),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = draft,
                onValueChange = onDraftChange,
                modifier = Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = "$label text field" },
                placeholder = { Text(placeholder) },
                minLines = 2,
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.End,
            ) {
                when {
                    isSaving -> {
                        CircularProgressIndicator(
                            modifier = Modifier
                                .size(18.dp)
                                .semantics { contentDescription = "Saving $label" },
                            strokeWidth = 2.dp,
                        )
                    }

                    !isDirty && savedNote != null -> {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.semantics { contentDescription = "$label saved" },
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
                    }

                    else -> {
                        TextButton(
                            onClick = { onSave(normalizedNote(draft)) },
                            enabled = isDirty,
                            modifier = Modifier.semantics {
                                contentDescription = "Save $label"
                            },
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}
