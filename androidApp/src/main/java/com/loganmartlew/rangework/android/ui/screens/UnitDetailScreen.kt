package com.loganmartlew.rangework.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.ui.PracticePlannerUiState
import com.loganmartlew.rangework.android.ui.components.BallCountPill
import com.loganmartlew.rangework.android.ui.components.BriefingStat
import com.loganmartlew.rangework.android.ui.components.BriefingRow
import com.loganmartlew.rangework.android.ui.components.EmptyStateCard
import com.loganmartlew.rangework.android.ui.components.EntryHighlightCard
import com.loganmartlew.rangework.android.ui.components.FocusCard
import com.loganmartlew.rangework.android.ui.components.NumberBadge
import com.loganmartlew.rangework.android.ui.components.ScrollableScreen
import com.loganmartlew.rangework.android.ui.components.StatProminence
import com.loganmartlew.rangework.shared.model.PracticeInstruction
import com.loganmartlew.rangework.shared.model.PracticeSession
import com.loganmartlew.rangework.shared.model.derivedBallCount
import com.loganmartlew.rangework.shared.model.sessionsUsingUnit

@Composable
internal fun UnitDetailScreen(
    plannerUiState: PracticePlannerUiState,
    unitId: String,
    onCreateUnit: () -> Unit,
    onEditUnit: () -> Unit,
    onViewSession: (String) -> Unit,
) {
    val unit = plannerUiState.units.firstOrNull { it.id == unitId }
    ScrollableScreen {
        if (unit == null) {
            EntryHighlightCard(
                title = "Unit not found",
                body = "This unit no longer exists in the current planner data.",
            )
            FilledTonalButton(onClick = onCreateUnit) {
                Text("New unit")
            }
            return@ScrollableScreen
        }

        val defaultClubName = unit.defaultClubReference?.takeIf(String::isNotBlank)?.let { code ->
            plannerUiState.clubCatalog.firstOrNull { it.code == code }?.displayName ?: code
        } ?: "No club"

        // Stat strip: total balls (primary) + instruction count + default club
        BriefingRow(
            stats = listOf(
                BriefingStat(
                    value = unit.derivedBallCount().toString(),
                    label = "Balls",
                    prominence = StatProminence.Primary,
                ),
                BriefingStat(
                    value = unit.instructions.size.toString(),
                    label = "Instructions",
                ),
                BriefingStat(
                    value = defaultClubName,
                    label = "Default club",
                ),
            ),
        )

        // Notes
        unit.notes?.takeIf(String::isNotBlank)?.let { notes ->
            EntryHighlightCard(title = "Notes", body = notes)
        }

        // Focus cue — tinted FocusCard only when a cue exists
        unit.focus?.takeIf(String::isNotBlank)?.let { cue ->
            FocusCard(cue = cue)
        }

        // Instructions card
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Instructions",
                    style = MaterialTheme.typography.titleMedium,
                )
                if (unit.instructions.isEmpty()) {
                    EmptyStateCard(
                        icon = Icons.Default.Edit,
                        title = "No instructions yet",
                        body = "Add instructions to define how this unit should be practiced.",
                        actionLabel = "Edit unit",
                        onAction = onEditUnit,
                    )
                } else {
                    unit.instructions.forEachIndexed { index, instruction ->
                        InstructionDetailRow(
                            instruction = instruction,
                            position = instruction.order,
                        )
                        if (index != unit.instructions.lastIndex) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        }

        val usedInSessions = sessionsUsingUnit(unit.id, plannerUiState.sessions)
        if (usedInSessions.isNotEmpty()) {
            Card(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(
                        text = "Used in ${usedInSessions.size} session${if (usedInSessions.size == 1) "" else "s"}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    usedInSessions.forEachIndexed { index, session ->
                        SessionLinkRow(
                            session = session,
                            onClick = { onViewSession(session.id) },
                        )
                        if (index != usedInSessions.lastIndex) {
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionLinkRow(
    session: PracticeSession,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = session.name,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.weight(1f),
        )
        Icon(
            imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(24.dp),
        )
    }
}

@Composable
private fun InstructionDetailRow(
    instruction: PracticeInstruction,
    position: Int,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        NumberBadge(number = position)
        Text(
            text = instruction.text,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f),
        )
        instruction.ballCount?.let { count ->
            BallCountPill(count = count)
        }
    }
}
