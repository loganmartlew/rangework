package com.loganmartlew.rangework.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import com.loganmartlew.rangework.android.ui.FinishSummaryData
import com.loganmartlew.rangework.android.ui.theme.RangeworkMono
import kotlin.math.roundToInt

@Composable
internal fun FinishSummaryContent(
    summary: FinishSummaryData,
    onDone: () -> Unit,
    modifier: Modifier = Modifier,
    showSessionNote: Boolean = false,
    savedSessionNote: String? = null,
    isSavingSessionNote: Boolean = false,
    onSaveSessionNote: (note: String?, onComplete: () -> Unit) -> Unit = { _, done -> done() },
    onArchiveSession: (() -> Unit)? = null,
) {
    // The note draft lives here (not inside SessionNoteCard) so the Done button
    // can flush a dirty unsaved note before navigating (P2). The session is
    // already Completed when this shows; the freeze matrix permits session-note
    // writes when Completed, so this ordering is by design.
    var noteDraft by rememberSaveable { mutableStateOf(savedSessionNote ?: "") }
    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = "Session Complete",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.secondary,
            )
            Text(
                text = summary.sessionName,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onBackground,
            )
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                SummaryStat(
                    value = "${summary.completedBalls}",
                    unit = "of ${summary.totalBalls} balls",
                    label = "Balls hit",
                    accessibleDescription = "${summary.completedBalls} of ${summary.totalBalls} balls hit",
                    highlighted = true,
                )
                HorizontalDivider()
                SummaryStat(
                    value = "${summary.completedStepCount}/${summary.totalStepCount}",
                    label = "Steps completed",
                    accessibleDescription = "${summary.completedStepCount} of ${summary.totalStepCount} steps completed",
                )
                HorizontalDivider()
                val pctDisplay = "${(summary.completionPercentage * 100).roundToInt()}%"
                SummaryStat(
                    value = pctDisplay,
                    label = "Completion",
                    accessibleDescription = "${(summary.completionPercentage * 100).roundToInt()} percent complete",
                    highlighted = true,
                )
                HorizontalDivider()
                val timeDisplay = summary.elapsedSeconds?.let { formatElapsedTime(it) } ?: "—"
                val timeDescription = summary.elapsedSeconds?.let {
                    val mins = it / 60
                    val secs = it % 60
                    "$mins minutes $secs seconds"
                } ?: "Time not tracked"
                SummaryStat(
                    value = timeDisplay,
                    label = "Time",
                    accessibleDescription = timeDescription,
                )
            }
        }

        if (showSessionNote) {
            SessionNoteCard(
                label = "Session note",
                draft = noteDraft,
                onDraftChange = { noteDraft = it },
                savedNote = savedSessionNote,
                isSaving = isSavingSessionNote,
                onSave = { note -> onSaveSessionNote(note) {} },
                placeholder = "How did the session feel?",
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (showSessionNote && noteIsDirty(noteDraft, savedSessionNote)) {
                    // Flush the dirty note, then navigate; a failed flush stays put
                    // and surfaces the snackbar (the note is never silently lost).
                    onSaveSessionNote(normalizedNote(noteDraft), onDone)
                } else {
                    onDone()
                }
            },
            enabled = !isSavingSessionNote,
            modifier = Modifier
                .fillMaxWidth()
                .semantics { contentDescription = "Done, return to previous screen" },
        ) {
            Text("Done", style = MaterialTheme.typography.labelLarge)
        }

        if (onArchiveSession != null) {
            TextButton(onClick = onArchiveSession) {
                Text(
                    text = "Archive this session",
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun SummaryStat(
    value: String,
    label: String,
    accessibleDescription: String,
    unit: String? = null,
    highlighted: Boolean = false,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) { contentDescription = accessibleDescription },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = value,
                style = RangeworkMono.large,
                color = if (highlighted) {
                    MaterialTheme.colorScheme.secondary
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
            )
            if (unit != null) {
                Text(
                    text = unit,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

private fun formatElapsedTime(seconds: Long): String {
    val h = seconds / 3600
    val m = (seconds % 3600) / 60
    val s = seconds % 60
    return if (h > 0) {
        "%d:%02d:%02d".format(h, m, s)
    } else {
        "%d:%02d".format(m, s)
    }
}
