package com.loganmartlew.rangework.android.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import com.loganmartlew.rangework.android.ui.PlannerStatus
import com.loganmartlew.rangework.android.ui.PracticePlannerUiState
import com.loganmartlew.rangework.android.ui.ballSummary
import com.loganmartlew.rangework.android.ui.components.EmptyStateCard
import com.loganmartlew.rangework.android.ui.components.EntryHighlightCard
import com.loganmartlew.rangework.android.ui.components.PlanningListContent
import com.loganmartlew.rangework.android.ui.components.RefreshableScrollableScreen
import com.loganmartlew.rangework.android.ui.components.SummaryEntityCard
import com.loganmartlew.rangework.shared.model.derivedBallCount

@Composable
internal fun UnitListScreen(
    plannerUiState: PracticePlannerUiState,
    onRefresh: () -> Unit,
    onCreateUnit: () -> Unit,
    onViewUnit: (String) -> Unit,
    onEditUnit: (String) -> Unit,
    onDeleteUnit: (String) -> Unit,
) {
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
                        icon = Icons.Rounded.Widgets,
                        title = "No units yet",
                        body = "Build a reusable drill with ordered steps and a ball count, then combine units into sessions.",
                        actionLabel = "Create your first unit",
                        onAction = onCreateUnit,
                    )
                } else {
                    plannerUiState.units.forEachIndexed { index, unit ->
                        SummaryEntityCard(
                            title = unit.title,
                            subtitle = "${unit.instructions.size} instruction${if (unit.instructions.size == 1) "" else "s"}  •  ${ballSummary(unit.derivedBallCount())}",
                            supportingText = buildString {
                                unit.defaultClubReference?.takeIf(String::isNotBlank)?.let { code ->
                                    val name = plannerUiState.clubCatalog.firstOrNull { it.code == code }?.displayName ?: code
                                    append("$name  •  ")
                                }
                                append(unit.instructions.joinToString("  •  ") { instruction -> instruction.text })
                            },
                            onView = { onViewUnit(unit.id) },
                            onEdit = { onEditUnit(unit.id) },
                            onDelete = { onDeleteUnit(unit.id) },
                        )
                        if (index != plannerUiState.units.lastIndex) {
                            HorizontalDivider()
                        }
                    }
                }
            },
        )
    }
}
