package com.loganmartlew.rangework.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/** Auto-save fires this long after the last keystroke. */
private const val NOTE_AUTOSAVE_DEBOUNCE_MS = 800L

/** True when [draft], normalized, differs from the saved value — i.e. a save would change something. */
internal fun noteIsDirty(draft: String, savedNote: String?): Boolean =
    draft.trim().takeIf(String::isNotEmpty) != savedNote

/** [draft] normalized to the value a save would persist (null clears). */
internal fun normalizedNote(draft: String): String? = draft.trim().takeIf(String::isNotEmpty)

/**
 * A self-saving prose-note field — no Save button. Edits debounce-save
 * ([NOTE_AUTOSAVE_DEBOUNCE_MS] after typing settles), and any still-pending edit
 * flushes when the field leaves composition (swiping to another block, collapsing
 * the section, leaving the screen), so a quick type-then-navigate isn't dropped.
 * The one lossy edge is tearing the screen down (Android back) mid-write before
 * the debounce fires — accepted for simplicity; the status line keeps in-flight
 * and settled saves visible.
 *
 * The draft is hoisted so callers can flush it on their own actions (the finish
 * summary's Done flushes a dirty session note before navigating) and hold it in
 * `rememberSaveable` so rotation/process death keep unsaved text; the saved value
 * re-derives from the model.
 */
@Composable
internal fun NoteAutoSaveField(
    draft: String,
    onDraftChange: (String) -> Unit,
    savedNote: String?,
    isSaving: Boolean,
    onSave: (String?) -> Unit,
    fieldContentDescription: String,
    savingContentDescription: String,
    savedContentDescription: String,
    placeholder: String,
    modifier: Modifier = Modifier,
) {
    // Debounced auto-save: restarts on each keystroke, writes once typing settles.
    LaunchedEffect(draft, savedNote) {
        if (noteIsDirty(draft, savedNote)) {
            delay(NOTE_AUTOSAVE_DEBOUNCE_MS)
            onSave(normalizedNote(draft))
        }
    }

    // Flush a still-pending edit on dispose. rememberUpdatedState so onDispose
    // reads the latest values without re-registering the effect.
    val latestDraft = rememberUpdatedState(draft)
    val latestSaved = rememberUpdatedState(savedNote)
    val latestOnSave = rememberUpdatedState(onSave)
    DisposableEffect(Unit) {
        onDispose {
            if (noteIsDirty(latestDraft.value, latestSaved.value)) {
                latestOnSave.value(normalizedNote(latestDraft.value))
            }
        }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedTextField(
            value = draft,
            onValueChange = onDraftChange,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = fieldContentDescription },
            placeholder = { Text(placeholder) },
            minLines = 2,
        )
        NoteSaveStatus(
            isSaving = isSaving,
            showSaved = !noteIsDirty(draft, savedNote) && savedNote != null,
            savingContentDescription = savingContentDescription,
            savedContentDescription = savedContentDescription,
        )
    }
}

/**
 * The passive save indicator that replaces the Save button: a spinner while a
 * write is in flight, a check once the saved value matches the draft, nothing
 * otherwise. Fixed height so the field doesn't jump as the state changes.
 */
@Composable
private fun NoteSaveStatus(
    isSaving: Boolean,
    showSaved: Boolean,
    savingContentDescription: String,
    savedContentDescription: String,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(20.dp),
        contentAlignment = Alignment.CenterEnd,
    ) {
        when {
            isSaving -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.semantics { contentDescription = savingContentDescription },
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(14.dp),
                    strokeWidth = 2.dp,
                )
                Text(
                    text = "Saving…",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }

            showSaved -> Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.semantics { contentDescription = savedContentDescription },
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
    }
}

/**
 * A labelled auto-saving prose-note card (session-level). Wraps [NoteAutoSaveField]
 * in a titled Card; the draft is hoisted so the finish summary's Done can flush a
 * dirty note before navigating.
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
            NoteAutoSaveField(
                draft = draft,
                onDraftChange = onDraftChange,
                savedNote = savedNote,
                isSaving = isSaving,
                onSave = onSave,
                fieldContentDescription = "$label text field",
                savingContentDescription = "Saving $label",
                savedContentDescription = "$label saved",
                placeholder = placeholder,
            )
        }
    }
}
