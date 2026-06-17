package com.loganmartlew.rangework.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Widgets
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSwipeToDismissBoxState
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
import com.loganmartlew.rangework.android.ui.ballSummary
import com.loganmartlew.rangework.android.ui.components.ClubChip
import com.loganmartlew.rangework.android.ui.components.DeleteConfirmationDialog
import com.loganmartlew.rangework.android.ui.components.EmptyStateCard
import com.loganmartlew.rangework.android.ui.components.EntryHighlightCard
import com.loganmartlew.rangework.android.ui.components.ListEntryCard
import com.loganmartlew.rangework.android.ui.components.PlanningListContent
import com.loganmartlew.rangework.android.ui.components.RefreshableScrollableScreen
import com.loganmartlew.rangework.android.ui.components.SwipeActionBackground
import com.loganmartlew.rangework.shared.model.PracticeUnit
import com.loganmartlew.rangework.shared.model.derivedBallCount
import com.loganmartlew.rangework.shared.model.sessionsUsingUnit

@OptIn(ExperimentalMaterial3Api::class)
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
        val usedCount = sessionsUsingUnit(unit.id, plannerUiState.sessions).size
        DeleteConfirmationDialog(
            itemName = unit.title,
            warning = if (usedCount > 0) {
                "This unit is used in $usedCount session${if (usedCount == 1) "" else "s"}. Those sessions will lose this unit."
            } else {
                null
            },
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
                        key(unit.id) {
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    when (value) {
                                        SwipeToDismissBoxValue.EndToStart -> {
                                            pendingDeleteUnit = unit
                                            false
                                        }
                                        SwipeToDismissBoxValue.StartToEnd -> {
                                            onEditUnit(unit.id)
                                            false
                                        }
                                        SwipeToDismissBoxValue.Settled -> false
                                    }
                                },
                            )
                            val clubName = unit.defaultClubReference
                                ?.takeIf(String::isNotBlank)
                                ?.let { code ->
                                    plannerUiState.clubCatalog.firstOrNull { it.code == code }?.displayName ?: code
                                }
                            val instructionCount = unit.instructions.size
                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    SwipeActionBackground(dismissState.dismissDirection)
                                },
                            ) {
                                ListEntryCard(
                                    title = unit.title,
                                    supportingText = unit.instructions.joinToString("  •  ") { it.text }.ifBlank { "No instructions." },
                                    metadataRow = {
                                        Row(
                                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            clubName?.let { ClubChip(name = it) }
                                            Text(
                                                text = "$instructionCount instruction${if (instructionCount == 1) "" else "s"}  •  ${ballSummary(unit.derivedBallCount())}",
                                                style = MaterialTheme.typography.bodyMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                            )
                                        }
                                    },
                                    onClick = { onViewUnit(unit.id) },
                                    onEdit = { onEditUnit(unit.id) },
                                    onDelete = { pendingDeleteUnit = unit },
                                    onDuplicate = { onDuplicateUnit(unit.id) },
                                    overflowContentDescription = "More options for ${unit.title}",
                                )
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(96.dp))
                }
            },
        )
    }
}

