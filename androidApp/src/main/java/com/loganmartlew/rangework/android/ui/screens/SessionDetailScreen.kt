package com.loganmartlew.rangework.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.ui.PracticePlannerUiState
import com.loganmartlew.rangework.android.ui.ballSummary
import com.loganmartlew.rangework.android.ui.components.ActionRow
import com.loganmartlew.rangework.android.ui.components.DetailListCard
import com.loganmartlew.rangework.android.ui.components.EntryHighlightCard
import com.loganmartlew.rangework.android.ui.components.ScrollableScreen
import com.loganmartlew.rangework.shared.model.Club
import com.loganmartlew.rangework.shared.model.PracticeSessionItem
import com.loganmartlew.rangework.shared.model.PracticeUnit
import com.loganmartlew.rangework.shared.model.derivedBallCount

@Composable
internal fun SessionDetailScreen(
    plannerUiState: PracticePlannerUiState,
    sessionId: String,
    onCreateSession: () -> Unit,
    onEditSession: () -> Unit,
    onDeleteSession: () -> Unit,
) {
    val session = plannerUiState.sessions.firstOrNull { it.id == sessionId }
    val unitsById = remember(plannerUiState.units) {
        plannerUiState.units.associateBy(PracticeUnit::id)
    }
    ScrollableScreen {
        if (session == null) {
            EntryHighlightCard(
                title = "Session not found",
                body = "This session no longer exists in the current planner data.",
            )
            FilledTonalButton(onClick = onCreateSession) {
                Text("New session")
            }
            return@ScrollableScreen
        }

        Text(
            text = session.name,
            style = MaterialTheme.typography.headlineMedium,
        )
        ActionRow(
            primaryLabel = "Edit session",
            onPrimary = onEditSession,
            secondaryLabel = "Delete session",
            onSecondary = onDeleteSession,
            primaryEnabled = !plannerUiState.isWorking,
            secondaryEnabled = !plannerUiState.isWorking,
        )
        EntryHighlightCard(
            title = "Summary",
            body = "${session.items.size} item${if (session.items.size == 1) "" else "s"}  •  ${ballSummary(session.derivedBallCount(unitsById))}",
        )
        session.notes?.takeIf(String::isNotBlank)?.let { notes ->
            EntryHighlightCard(title = "Session notes", body = notes)
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Session items",
                    style = MaterialTheme.typography.titleMedium,
                )
                session.items.forEachIndexed { index, item ->
                    SessionItemDetailCard(
                        item = item,
                        unit = unitsById[item.practiceUnitId],
                        clubCatalog = plannerUiState.clubCatalog,
                    )
                    if (index != session.items.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionItemDetailCard(
    item: PracticeSessionItem,
    unit: PracticeUnit?,
    clubCatalog: List<Club>,
) {
    fun resolveClubName(code: String?): String? = code?.takeIf(String::isNotBlank)?.let { c ->
        clubCatalog.firstOrNull { it.code == c }?.displayName ?: c
    }
    DetailListCard(
        title = unit?.title ?: "Missing unit",
        subtitle = buildString {
            append("Repeat ${item.repeatCount}x")
            val effectiveClubName = resolveClubName(item.clubReference)
                ?: resolveClubName(unit?.defaultClubReference)
            effectiveClubName?.let { append("  •  Club: $it") }
        },
        supportingText = buildString {
            append(ballSummary(item.derivedBallCount(unit)))
            item.focusCue?.takeIf(String::isNotBlank)?.let { append("  •  Focus cue: $it") }
            item.notes?.takeIf(String::isNotBlank)?.let { append("  •  $it") }
        },
    )
}
