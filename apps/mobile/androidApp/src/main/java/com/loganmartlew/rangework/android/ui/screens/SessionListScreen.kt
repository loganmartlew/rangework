package com.loganmartlew.rangework.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.EventNote
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import com.loganmartlew.rangework.android.ui.allUnits
import com.loganmartlew.rangework.android.ui.components.BallCountPill
import com.loganmartlew.rangework.android.ui.components.DeleteConfirmationDialog
import com.loganmartlew.rangework.android.ui.components.EmptyStateCard
import com.loganmartlew.rangework.android.ui.components.EntryHighlightCard
import com.loganmartlew.rangework.android.ui.components.ListEntryCard
import com.loganmartlew.rangework.android.ui.components.PlanningListContent
import com.loganmartlew.rangework.android.ui.components.RefreshableScrollableScreen
import com.loganmartlew.rangework.android.ui.components.SwipeActionBackground
import com.loganmartlew.rangework.android.ui.components.TagChipRow
import com.loganmartlew.rangework.android.ui.components.TagFilterBar
import com.loganmartlew.rangework.shared.model.PracticeSession
import com.loganmartlew.rangework.shared.model.PracticeUnit
import com.loganmartlew.rangework.shared.model.derivedBallCount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SessionListScreen(
    plannerUiState: PracticePlannerUiState,
    onRefresh: () -> Unit,
    onCreateSession: () -> Unit,
    onViewSession: (String) -> Unit,
    onEditSession: (String) -> Unit,
    onDeleteSession: (String) -> Unit,
    onDuplicateSession: (String) -> Unit,
    onArchiveSession: (String) -> Unit,
    onNavigateToArchived: () -> Unit,
    onGoToUnits: () -> Unit,
    onToggleTagFilter: (String) -> Unit,
    onClearTagFilter: () -> Unit,
) {
    val unitsById = remember(plannerUiState.allUnits) {
        plannerUiState.allUnits.associateBy(PracticeUnit::id)
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
                if (plannerUiState.sessions.isEmpty() && plannerUiState.units.isEmpty()) {
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
                    TagFilterBar(
                        availableTags = plannerUiState.availableTags,
                        selectedTagIds = plannerUiState.sessionTagFilter,
                        onToggle = onToggleTagFilter,
                        onClear = onClearTagFilter,
                        modifier = Modifier.padding(bottom = 12.dp),
                    )
                    val visibleSessions = plannerUiState.filteredSessions
                    if (visibleSessions.isEmpty()) {
                        Text(
                            text = "No sessions match the selected tags.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    visibleSessions.forEach { session ->
                        key(session.id) {
                            val dismissState = rememberSwipeToDismissBoxState(
                                confirmValueChange = { value ->
                                    when (value) {
                                        SwipeToDismissBoxValue.EndToStart -> {
                                            pendingDeleteSession = session
                                            false
                                        }
                                        SwipeToDismissBoxValue.StartToEnd -> {
                                            onEditSession(session.id)
                                            false
                                        }
                                        SwipeToDismissBoxValue.Settled -> false
                                    }
                                },
                            )
                            val ballCount = session.derivedBallCount(unitsById)
                            val itemCount = session.items.size
                            val unitLineup = session.items.joinToString("  •  ") { item ->
                                unitsById[item.practiceUnitId]?.title ?: "Missing unit"
                            }.ifBlank { "No items yet." }
                            SwipeToDismissBox(
                                state = dismissState,
                                backgroundContent = {
                                    SwipeActionBackground(dismissState.dismissDirection)
                                },
                            ) {
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
                                    onEdit = { onEditSession(session.id) },
                                    onDelete = { pendingDeleteSession = session },
                                    onDuplicate = { onDuplicateSession(session.id) },
                                    onArchive = { onArchiveSession(session.id) },
                                    overflowContentDescription = "More options for ${session.name}",
                                )
                            }
                        }
                    }
                }
                if (plannerUiState.archivedSessions.isNotEmpty()) {
                    TextButton(onClick = onNavigateToArchived) {
                        Icon(
                            imageVector = Icons.Default.Archive,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Archived · ${plannerUiState.archivedSessions.size}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    Spacer(modifier = Modifier.height(96.dp))
                }
            },
        )
    }
}
