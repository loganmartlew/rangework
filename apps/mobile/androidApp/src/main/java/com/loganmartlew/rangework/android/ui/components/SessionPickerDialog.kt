package com.loganmartlew.rangework.android.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.BasicAlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.shared.model.PracticeSession
import com.loganmartlew.rangework.shared.model.PracticeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SessionPickerDialog(
    sessions: List<PracticeSession>,
    unitsByIdMap: Map<String, PracticeUnit>,
    isLoading: Boolean = false,
    onSessionSelected: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    val runnableSessions = sessions.filter { session ->
        session.items.isNotEmpty() &&
            session.items.any { item ->
                (unitsByIdMap[item.practiceUnitId]?.instructions?.size ?: 0) > 0
            }
    }

    BasicAlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier
            .fillMaxWidth(0.9f)
            .semantics(mergeDescendants = true) {
                contentDescription = "Start Session dialog"
            },
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                Text(
                    text = "Start Session",
                    style = MaterialTheme.typography.headlineSmall,
                )

                if (isLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                } else if (runnableSessions.isEmpty()) {
                    Text(
                        text = "No sessions available. Create a practice session first.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(runnableSessions, key = { it.id }) { session ->
                            SessionPickerItem(
                                session = session,
                                unitsByIdMap = unitsByIdMap,
                                onClick = {
                                    onSessionSelected(session.id)
                                },
                            )
                        }
                    }
                }

                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.align(Alignment.End),
                ) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
private fun SessionPickerItem(
    session: PracticeSession,
    unitsByIdMap: Map<String, PracticeUnit>,
    onClick: () -> Unit,
) {
    val uniqueUnits = session.items.map { it.practiceUnitId }.distinct().size
    val totalInstructions = session.items.sumOf { item ->
        unitsByIdMap[item.practiceUnitId]?.instructions?.size ?: 0
    }

    val summaryText = buildString {
        append("$uniqueUnits ${if (uniqueUnits == 1) "unit" else "units"}")
        append(" · $totalInstructions ${if (totalInstructions == 1) "instruction" else "instructions"}")
    }

    val accessibleDescription = "${session.name}, $summaryText. Tap to start."

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = accessibleDescription
            },
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(
                text = session.name,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = summaryText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
