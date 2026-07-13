package com.loganmartlew.rangework.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.ui.PlannerStatus
import com.loganmartlew.rangework.android.ui.PracticePlannerUiState
import com.loganmartlew.rangework.android.ui.allUnits
import com.loganmartlew.rangework.android.ui.components.BallCountPill
import com.loganmartlew.rangework.android.ui.components.DeleteConfirmationDialog
import com.loganmartlew.rangework.android.ui.components.EntryHighlightCard
import com.loganmartlew.rangework.android.ui.components.ListEntryCard
import com.loganmartlew.rangework.android.ui.components.PlanningListContent
import com.loganmartlew.rangework.android.ui.components.RefreshableScrollableScreen
import com.loganmartlew.rangework.android.ui.components.SESSION_INLINE_UNITS_DELETE_WARNING
import com.loganmartlew.rangework.android.ui.components.TagChipRow
import com.loganmartlew.rangework.shared.model.PracticeSession
import com.loganmartlew.rangework.shared.model.PracticeUnit
import com.loganmartlew.rangework.shared.model.derivedBallCount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ArchivedSessionsScreen(
    plannerUiState: PracticePlannerUiState,
    onRefresh: () -> Unit,
    onViewSession: (String) -> Unit,
    onUnarchiveSession: (String) -> Unit,
    onDuplicateSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
) {
    val unitsById = remember(plannerUiState.allUnits) {
        plannerUiState.allUnits.associateBy(PracticeUnit::id)
    }
    var pendingDeleteSession by remember { mutableStateOf<PracticeSession?>(null) }

    pendingDeleteSession?.let { session ->
        DeleteConfirmationDialog(
            itemName = session.name,
            warning = SESSION_INLINE_UNITS_DELETE_WARNING,
            onConfirm = {
                onDeleteSession(session.id)
                pendingDeleteSession = null
            },
            onDismiss = { pendingDeleteSession = null },
        )
    }

    RefreshableScrollableScreen(
        isRefreshing = plannerUiState.isLoading,
        onRefresh = {
            if (!plannerUiState.isWorking) {
                onRefresh()
            }
        },
    ) {
        PlanningListContent(
            dataConfigured = plannerUiState.dataConfigured,
            status = plannerUiState.status,
            hasLoaded = plannerUiState.hasLoaded,
            isLoading = plannerUiState.isLoading,
            unavailableContent = {
                EntryHighlightCard(
                    title = "Planning unavailable",
                    body = PlannerStatus.Unavailable.text,
                )
            },
            listContent = {
                if (plannerUiState.archivedSessions.isEmpty()) {
                    Text(
                        text = "No archived sessions.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                } else {
                    plannerUiState.archivedSessions.forEach { session ->
                        key(session.id) {
                            val ballCount = session.derivedBallCount(unitsById)
                            val itemCount = session.items.size
                            val unitLineup = session.items.joinToString("  •  ") { item ->
                                unitsById[item.practiceUnitId]?.title ?: "Missing unit"
                            }.ifBlank { "No items yet." }
                            ListEntryCard(
                                title = session.name,
                                supportingText = unitLineup,
                                metadataRow = {
                                    Column(
                                        verticalArrangement = Arrangement.spacedBy(8.dp),
                                    ) {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            BallCountPill(count = ballCount)
                                            Text(
                                                text = "$itemCount item${if (itemCount == 1) "" else "s"}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                        TagChipRow(tags = session.tags)
                                    }
                                },
                                onClick = { onViewSession(session.id) },
                                onDelete = { pendingDeleteSession = session },
                                onDuplicate = { onDuplicateSession(session.id) },
                                onUnarchive = { onUnarchiveSession(session.id) },
                                overflowContentDescription = "More options for ${session.name}",
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(96.dp))
                }
            },
        )
    }
}
