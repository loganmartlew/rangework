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
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.ui.PracticePlannerUiState
import com.loganmartlew.rangework.android.ui.ballSummary
import com.loganmartlew.rangework.android.ui.components.ActionRow
import com.loganmartlew.rangework.android.ui.components.DetailListCard
import com.loganmartlew.rangework.android.ui.components.EntryHighlightCard
import com.loganmartlew.rangework.android.ui.components.ScrollableScreen
import com.loganmartlew.rangework.shared.model.derivedBallCount

@Composable
internal fun UnitDetailScreen(
    plannerUiState: PracticePlannerUiState,
    unitId: String,
    onCreateUnit: () -> Unit,
    onEditUnit: () -> Unit,
    onDeleteUnit: () -> Unit,
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

        Text(
            text = unit.title,
            style = MaterialTheme.typography.headlineMedium,
        )
        ActionRow(
            primaryLabel = "Edit unit",
            onPrimary = onEditUnit,
            secondaryLabel = "Delete unit",
            onSecondary = onDeleteUnit,
            primaryEnabled = !plannerUiState.isWorking,
            secondaryEnabled = !plannerUiState.isWorking,
        )
        EntryHighlightCard(
            title = "Summary",
            body = buildString {
                append("${unit.instructions.size} instruction")
                if (unit.instructions.size != 1) append("s")
                append("  •  ${ballSummary(unit.derivedBallCount())}")
                unit.defaultClubReference?.takeIf(String::isNotBlank)?.let { code ->
                    val name = plannerUiState.clubCatalog.firstOrNull { it.code == code }?.displayName ?: code
                    append("  •  Default club: $name")
                }
            },
        )
        unit.notes?.takeIf(String::isNotBlank)?.let { notes ->
            EntryHighlightCard(title = "Notes", body = notes)
        }
        unit.focus?.takeIf(String::isNotBlank)?.let { focus ->
            EntryHighlightCard(title = "Focus", body = focus)
        }
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
                unit.instructions.forEachIndexed { index, instruction ->
                    DetailListCard(
                        title = "Instruction ${instruction.order}",
                        subtitle = instruction.text,
                        supportingText = buildString {
                            instruction.ballCount?.let {
                                append("Balls: $it")
                            }
                            if (isEmpty()) {
                                append("No ball count set")
                            }
                        },
                    )
                    if (index != unit.instructions.lastIndex) {
                        HorizontalDivider()
                    }
                }
            }
        }
    }
}
