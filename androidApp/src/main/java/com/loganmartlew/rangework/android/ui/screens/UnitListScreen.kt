package com.loganmartlew.rangework.android.ui.screens

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.ui.PlannerStatus
import com.loganmartlew.rangework.android.ui.PracticePlannerUiState
import com.loganmartlew.rangework.android.ui.ballSummary
import com.loganmartlew.rangework.android.ui.components.ClubChip
import com.loganmartlew.rangework.android.ui.components.DeleteConfirmationDialog
import com.loganmartlew.rangework.android.ui.components.EmptyStateCard
import com.loganmartlew.rangework.android.ui.components.EntryHighlightCard
import com.loganmartlew.rangework.android.ui.components.ListEntryCard
import com.loganmartlew.rangework.android.ui.components.PlanningListContent
import com.loganmartlew.rangework.android.ui.components.RefreshableScrollableScreen
import com.loganmartlew.rangework.shared.model.PracticeUnit
import com.loganmartlew.rangework.shared.model.derivedBallCount

@Composable
internal fun UnitListScreen(
    plannerUiState: PracticePlannerUiState,
    onRefresh: () -> Unit,
    onCreateUnit: () -> Unit,
    onViewUnit: (String) -> Unit,
    onEditUnit: (String) -> Unit,
    onDeleteUnit: (String) -> Unit,
    onDuplicateUnit: (String) -> Unit,
) {
    var pendingDeleteUnit by remember { mutableStateOf<PracticeUnit?>(null) }

    pendingDeleteUnit?.let { unit ->
        DeleteConfirmationDialog(
            itemName = unit.title,
            onConfirm = {
                onDeleteUnit(unit.id)
                pendingDeleteUnit = null
            },
            onDismiss = { pendingDeleteUnit = null },
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
                        icon = Icons.Rounded.Widgets,
                        title = "No units yet",
                        body = "Build a reusable drill with ordered steps and a ball count, then combine units into sessions.",
                        actionLabel = "Create your first unit",
                        onAction = onCreateUnit,
                    )
                } else {
                    plannerUiState.units.forEach { unit ->
                        val clubName = unit.defaultClubReference
                            ?.takeIf(String::isNotBlank)
                            ?.let { code ->
                                plannerUiState.clubCatalog.firstOrNull { it.code == code }?.displayName ?: code
                            }
                        val instructionCount = unit.instructions.size
                        ListEntryCard(
                            title = unit.title,
                            subtitle = "$instructionCount instruction${if (instructionCount == 1) "" else "s"}  •  ${ballSummary(unit.derivedBallCount())}",
                            supportingText = unit.instructions.joinToString("  •  ") { it.text }.ifBlank { "No instructions." },
                            metadataRow = if (clubName != null) {
                                { ClubChip(name = clubName) }
                            } else null,
                            onClick = { onViewUnit(unit.id) },
                            onEdit = { onEditUnit(unit.id) },
                            onDelete = { pendingDeleteUnit = unit },
                            onDuplicate = { onDuplicateUnit(unit.id) },
                            overflowContentDescription = "More options for ${unit.title}",
                        )
                    }
                    Spacer(modifier = Modifier.height(96.dp))
                }
            },
        )
    }
}
