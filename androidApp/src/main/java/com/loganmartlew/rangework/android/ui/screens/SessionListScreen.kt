package com.loganmartlew.rangework.android.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.EventNote
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.ui.PlannerStatus
import com.loganmartlew.rangework.android.ui.PracticePlannerUiState
import com.loganmartlew.rangework.android.ui.components.BallCountPill
import com.loganmartlew.rangework.android.ui.components.DeleteConfirmationDialog
import com.loganmartlew.rangework.android.ui.components.EmptyStateCard
import com.loganmartlew.rangework.android.ui.components.EntryHighlightCard
import com.loganmartlew.rangework.android.ui.components.ListEntryCard
import com.loganmartlew.rangework.android.ui.components.PlanningListContent
import com.loganmartlew.rangework.android.ui.components.RefreshableScrollableScreen
import com.loganmartlew.rangework.shared.model.PracticeSession
import com.loganmartlew.rangework.shared.model.PracticeUnit
import com.loganmartlew.rangework.shared.model.derivedBallCount

@Composable
internal fun SessionListScreen(
    plannerUiState: PracticePlannerUiState,
    onRefresh: () -> Unit,
    onCreateSession: () -> Unit,
    onViewSession: (String) -> Unit,
    onEditSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onDuplicateSession: (String) -> Unit,
    onGoToUnits: () -> Unit,
) {
    val unitsById = remember(plannerUiState.units) {
        plannerUiState.units.associateBy(PracticeUnit::id)
    }
    var pendingDeleteSession by remember { mutableStateOf<PracticeSession?>(null) }

    pendingDeleteSession?.let { session ->
        DeleteConfirmationDialog(
            itemName = session.name,
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
                if (plannerUiState.units.isEmpty()) {
                    EmptyStateCard(
                        icon = Icons.AutoMirrored.Rounded.EventNote,
                        title = "Create a unit first",
                        body = "Sessions are assembled from units. Build at least one unit before creating a session.",
                        actionLabel = "Go to Units",
                        onAction = onGoToUnits,
                    )
                } else if (plannerUiState.sessions.isEmpty()) {
                    EmptyStateCard(
                        icon = Icons.AutoMirrored.Rounded.EventNote,
                        title = "No sessions yet",
                        body = "String your units together into a full practice session.",
                        actionLabel = "Create your first session",
                        onAction = onCreateSession,
                    )
                } else {
                    plannerUiState.sessions.forEach { session ->
                        val ballCount = session.derivedBallCount(unitsById)
                        val itemCount = session.items.size
                        val unitLineup = session.items.joinToString("  •  ") { item ->
                            unitsById[item.practiceUnitId]?.title ?: "Missing unit"
                        }.ifBlank { "No items yet." }
                        ListEntryCard(
                            title = session.name,
                            subtitle = "$itemCount item${if (itemCount == 1) "" else "s"}",
                            supportingText = unitLineup,
                            metadataRow = { BallCountPill(count = ballCount) },
                            onClick = { onViewSession(session.id) },
                            onEdit = { onEditSession(session.id) },
                            onDelete = { pendingDeleteSession = session },
                            onDuplicate = { onDuplicateSession(session.id) },
                            overflowContentDescription = "More options for ${session.name}",
                        )
                    }
                    Spacer(modifier = Modifier.height(96.dp))
                }
            },
        )
    }
}
