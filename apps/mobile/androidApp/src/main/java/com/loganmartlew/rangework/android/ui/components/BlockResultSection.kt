package com.loganmartlew.rangework.android.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
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
 * The passive per-Block reflection affordance on the execution block screen
 * (snapshot v3 only). Collapsed by default — a single "Block result" header —
 * so it never prompts or badges; expansion is deliberate.
 *
 * Prose only: every block gets a free-text note, auto-saved. The success count
 * used to live here too, which is why nobody found it — it's data, not
 * reflection, so it now sits with the capture surface under the counter
 * ([BlockSuccessTallySection]).
 */
@Composable
internal fun BlockResultSection(
    blockResult: BlockResult?,
    isSavingNote: Boolean,
    onSaveNote: (String?) -> Unit,
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
                ) {
                    BlockNoteEditor(
                        savedNote = blockResult?.note,
                        isSaving = isSavingNote,
                        onSave = onSaveNote,
                    )
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
    // Draft held here (rememberSaveable) so it survives rotation/process death;
    // NoteAutoSaveField debounces the write and flushes on dispose (block swipe,
    // section collapse), so there's no Save button.
    var draft by rememberSaveable { mutableStateOf(savedNote ?: "") }

    NoteAutoSaveField(
        draft = draft,
        onDraftChange = { draft = it },
        savedNote = savedNote,
        isSaving = isSaving,
        onSave = onSave,
        fieldContentDescription = "Block note text field",
        savingContentDescription = "Saving block note",
        savedContentDescription = "Block note saved",
        placeholder = "Note on this block",
    )
}
