package com.loganmartlew.rangework.android.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.EventNote
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.loganmartlew.rangework.android.ui.PlannerStatus
import com.loganmartlew.rangework.android.ui.PracticePlannerUiState
import com.loganmartlew.rangework.android.ui.ballSummary
import com.loganmartlew.rangework.android.ui.components.EmptyStateCard
import com.loganmartlew.rangework.android.ui.components.EntryHighlightCard
import com.loganmartlew.rangework.android.ui.components.PlanningListContent
import com.loganmartlew.rangework.android.ui.components.RefreshableScrollableScreen
import com.loganmartlew.rangework.android.ui.components.SummaryEntityCard
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
) {
    val unitsById = remember(plannerUiState.units) {
        plannerUiState.units.associateBy(PracticeUnit::id)
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
                        onAction = { /* navigate handled by FAB/nav */ },
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
                    plannerUiState.sessions.forEachIndexed { index, session ->
                        SummaryEntityCard(
                            title = session.name,
                            subtitle = "${session.items.size} item${if (session.items.size == 1) "" else "s"}  •  ${ballSummary(session.derivedBallCount(unitsById))}",
                            supportingText = session.items.joinToString("  •  ") { item ->
                                unitsById[item.practiceUnitId]?.title ?: "Missing unit"
                            }.ifBlank { "No items yet." },
                            onView = { onViewSession(session.id) },
                            onEdit = { onEditSession(session.id) },
                            onDelete = { onDeleteSession(session.id) },
                            onDuplicate = { onDuplicateSession(session.id) },
                        )
                        if (index != plannerUiState.sessions.lastIndex) {
                            HorizontalDivider()
                        }
                    }
                }
            },
        )
    }
}
