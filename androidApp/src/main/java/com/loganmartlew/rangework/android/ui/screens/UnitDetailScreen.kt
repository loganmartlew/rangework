package com.loganmartlew.rangework.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Card
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.ui.PracticePlannerUiState
import com.loganmartlew.rangework.android.ui.components.BallCountPill
import com.loganmartlew.rangework.android.ui.components.BriefingRow
import com.loganmartlew.rangework.android.ui.components.EmptyStateCard
import com.loganmartlew.rangework.android.ui.components.EntryHighlightCard
import com.loganmartlew.rangework.android.ui.components.FocusCard
import com.loganmartlew.rangework.android.ui.components.NumberBadge
import com.loganmartlew.rangework.android.ui.components.ScrollableScreen
import com.loganmartlew.rangework.shared.model.PracticeInstruction
import com.loganmartlew.rangework.shared.model.derivedBallCount

@Composable
internal fun UnitDetailScreen(
    plannerUiState: PracticePlannerUiState,
    unitId: String,
    onCreateUnit: () -> Unit,
    onEditUnit: () -> Unit,
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

        // Stat strip: total balls (primary) + instruction count
        BriefingRow(
            stats = listOf(
                unit.derivedBallCount().toString() to "Balls",
                unit.instructions.size.toString() to "Instructions",
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

        // Default club (if set)
        unit.defaultClubReference?.takeIf(String::isNotBlank)?.let { code ->
            val name = plannerUiState.clubCatalog.firstOrNull { it.code == code }?.displayName ?: code
            EntryHighlightCard(title = "Default club", body = name)
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
