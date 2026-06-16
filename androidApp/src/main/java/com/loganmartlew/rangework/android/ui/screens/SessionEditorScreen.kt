package com.loganmartlew.rangework.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.ui.PracticeSessionItemEditorState
import com.loganmartlew.rangework.android.ui.PracticePlannerUiState
import com.loganmartlew.rangework.android.ui.ballSummary
import com.loganmartlew.rangework.android.ui.components.ClubPickerField
import com.loganmartlew.rangework.android.ui.components.EntryHighlightCard
import com.loganmartlew.rangework.android.ui.components.ReorderButtons
import com.loganmartlew.rangework.android.ui.components.ScrollableScreen
import com.loganmartlew.rangework.shared.model.Club
import com.loganmartlew.rangework.shared.model.PracticeUnit
import com.loganmartlew.rangework.shared.model.derivedBallCount

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SessionEditorScreen(
    plannerUiState: PracticePlannerUiState,
    title: String,
    onSaveSession: () -> Unit,
    onUpdateSessionName: (String) -> Unit,
    onUpdateSessionNotes: (String) -> Unit,
    onAddSessionItem: () -> Unit,
    onUpdateSessionItemUnit: (Int, String) -> Unit,
    onUpdateSessionItemRepeatCount: (Int, String) -> Unit,
    onUpdateSessionItemClubReference: (Int, String) -> Unit,
    onUpdateSessionItemNotes: (Int, String) -> Unit,
    onUpdateSessionItemFocusCue: (Int, String) -> Unit,
    onMoveSessionItem: (Int, Int) -> Unit,
    onRemoveSessionItem: (Int) -> Unit,
) {
    val unitsById = remember(plannerUiState.units) {
        plannerUiState.units.associateBy(PracticeUnit::id)
    }

    ScrollableScreen(modifier = Modifier.fillMaxSize()) {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
        )
        if (plannerUiState.units.isEmpty()) {
            EntryHighlightCard(
                title = "Create a unit first",
                body = "Sessions need at least one unit before you can add items.",
            )
        }
        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedTextField(
                    value = plannerUiState.sessionEditor.name,
                    onValueChange = onUpdateSessionName,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Name") },
                    enabled = !plannerUiState.isWorking,
                    singleLine = true,
                    isError = plannerUiState.sessionEditor.nameError != null,
                    supportingText = plannerUiState.sessionEditor.nameError?.let { { Text(it) } },
                )
                OutlinedTextField(
                    value = plannerUiState.sessionEditor.notes,
                    onValueChange = onUpdateSessionNotes,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Session notes") },
                    enabled = !plannerUiState.isWorking,
                    minLines = 3,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(
                        text = "Session items",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    FilledTonalButton(
                        enabled = !plannerUiState.isWorking && plannerUiState.units.isNotEmpty(),
                        onClick = onAddSessionItem,
                    ) {
                        Text("Add item")
                    }
                }
                if (plannerUiState.sessionEditor.items.isEmpty()) {
                    Text(
                        text = "No session items yet.",
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
            }
        }
        plannerUiState.sessionEditor.items.forEachIndexed { index, item ->
            SessionItemEditorCard(
                modifier = Modifier.fillMaxWidth(),
                item = item,
                availableUnits = plannerUiState.units,
                selectedUnit = unitsById[item.practiceUnitId],
                clubCatalog = plannerUiState.clubCatalog,
                enabledClubCodes = plannerUiState.enabledClubCodes,
                isWorking = plannerUiState.isWorking,
                onSelectUnit = { onUpdateSessionItemUnit(index, it) },
                onUpdateRepeatCount = { onUpdateSessionItemRepeatCount(index, it) },
                onSelectClub = { onUpdateSessionItemClubReference(index, it) },
                onUpdateNotes = { onUpdateSessionItemNotes(index, it) },
                onUpdateFocusCue = { onUpdateSessionItemFocusCue(index, it) },
                onMoveUp = { onMoveSessionItem(index, index - 1) },
                onMoveDown = { onMoveSessionItem(index, index + 1) },
                onRemove = { onRemoveSessionItem(index) },
                canMoveUp = index > 0,
                canMoveDown = index < plannerUiState.sessionEditor.items.lastIndex,
            )
        }
        EntryHighlightCard(
            title = "Balls",
            body = ballSummary(plannerUiState.sessionEditor.items.sumOf { item ->
                item.derivedBallCount(unitsById[item.practiceUnitId]) ?: 0
            }),
        )
        Button(
            modifier = Modifier.fillMaxWidth(),
            enabled = !plannerUiState.isWorking,
            onClick = onSaveSession,
        ) {
            Text("Save session")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionItemEditorCard(
    modifier: Modifier = Modifier,
    item: PracticeSessionItemEditorState,
    availableUnits: List<PracticeUnit>,
    selectedUnit: PracticeUnit?,
    clubCatalog: List<Club>,
    enabledClubCodes: Set<String>,
    isWorking: Boolean,
    onSelectUnit: (String) -> Unit,
    onUpdateRepeatCount: (String) -> Unit,
    onSelectClub: (String) -> Unit,
    onUpdateNotes: (String) -> Unit,
    onUpdateFocusCue: (String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
) {
    var unitMenuExpanded by remember(item.order, item.practiceUnitId) { mutableStateOf(false) }

    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        ),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Session item ${item.order}",
                style = MaterialTheme.typography.titleSmall,
            )
            ExposedDropdownMenuBox(
                expanded = unitMenuExpanded,
                onExpandedChange = { if (!isWorking) unitMenuExpanded = it },
            ) {
                OutlinedTextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .menuAnchor(),
                    readOnly = true,
                    value = selectedUnit?.title ?: "",
                    onValueChange = {},
                    label = { Text("Practice unit") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitMenuExpanded) },
                    enabled = !isWorking,
                    isError = item.unitError != null,
                    supportingText = item.unitError?.let { { Text(it) } },
                    colors = ExposedDropdownMenuDefaults.outlinedTextFieldColors(),
                )
                ExposedDropdownMenu(
                    expanded = unitMenuExpanded,
                    onDismissRequest = { unitMenuExpanded = false },
                ) {
                    availableUnits.forEach { unit ->
                        DropdownMenuItem(
                            text = { Text(unit.title) },
                            onClick = {
                                onSelectUnit(unit.id)
                                unitMenuExpanded = false
                            },
                        )
                    }
                }
            }
            OutlinedTextField(
                value = item.repeatCount,
                onValueChange = { onUpdateRepeatCount(it.filter(Char::isDigit)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Repeat count") },
                enabled = !isWorking,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = item.repeatCountError != null,
                supportingText = item.repeatCountError?.let { { Text(it) } },
            )
            ClubPickerField(
                label = "Session club",
                selectedCode = item.clubReference.ifBlank { null },
                clubCatalog = clubCatalog,
                enabledClubCodes = enabledClubCodes,
                enabled = !isWorking,
                onSelect = onSelectClub,
            )
            OutlinedTextField(
                value = item.notes,
                onValueChange = onUpdateNotes,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Item notes") },
                enabled = !isWorking,
                minLines = 2,
            )
            OutlinedTextField(
                value = item.focusCue,
                onValueChange = onUpdateFocusCue,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Focus cue") },
                enabled = !isWorking,
                singleLine = true,
            )
            Text(
                text = buildString {
                    append(ballSummary(item.derivedBallCount(selectedUnit)))
                    val effectiveCode = item.clubReference.ifBlank {
                        selectedUnit?.defaultClubReference.orEmpty()
                    }
                    if (effectiveCode.isNotBlank()) {
                        val displayName =
                            clubCatalog.firstOrNull { it.code == effectiveCode }?.displayName
                                ?: effectiveCode
                        append("  •  Club: $displayName")
                    }
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ReorderButtons(
                isWorking = isWorking,
                canMoveUp = canMoveUp,
                canMoveDown = canMoveDown,
                onMoveUp = onMoveUp,
                onMoveDown = onMoveDown,
                onRemove = onRemove,
            )
        }
    }
}

private fun PracticeSessionItemEditorState.derivedBallCount(unit: PracticeUnit?): Int? {
    val repeats = repeatCount.trim().toIntOrNull() ?: return unit?.derivedBallCount()
    return unit?.derivedBallCount()?.times(repeats)
}
