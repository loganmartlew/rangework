package com.loganmartlew.rangework.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.ui.PracticeInstructionEditorState
import com.loganmartlew.rangework.android.ui.PracticePlannerUiState
import com.loganmartlew.rangework.android.ui.components.ClubPickerField
import com.loganmartlew.rangework.android.ui.components.ReorderButtons
import com.loganmartlew.rangework.android.ui.components.ScrollableScreen
import com.loganmartlew.rangework.shared.model.Club

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UnitEditorScreen(
    plannerUiState: PracticePlannerUiState,
    title: String,
    onSaveUnit: () -> Unit,
    onUpdateTitle: (String) -> Unit,
    onUpdateNotes: (String) -> Unit,
    onUpdateFocus: (String) -> Unit,
    onUpdateDefaultClubReference: (String) -> Unit,
    onAddInstruction: () -> Unit,
    onUpdateInstructionText: (Int, String) -> Unit,
    onUpdateInstructionBallCount: (Int, String) -> Unit,
    onMoveInstructionUp: (Int) -> Unit,
    onMoveInstructionDown: (Int) -> Unit,
    onRemoveInstruction: (Int) -> Unit,
) {
    ScrollableScreen {
        Text(
            text = title,
            style = MaterialTheme.typography.headlineMedium,
        )
        UnitEditorCard(
            editorState = plannerUiState.unitEditor,
            clubCatalog = plannerUiState.clubCatalog,
            enabledClubCodes = plannerUiState.enabledClubCodes,
            isWorking = plannerUiState.isWorking,
            onUpdateTitle = onUpdateTitle,
            onUpdateNotes = onUpdateNotes,
            onUpdateFocus = onUpdateFocus,
            onSelectDefaultClub = onUpdateDefaultClubReference,
            onAddInstruction = onAddInstruction,
            onUpdateInstructionText = onUpdateInstructionText,
            onUpdateInstructionBallCount = onUpdateInstructionBallCount,
            onMoveInstructionUp = onMoveInstructionUp,
            onMoveInstructionDown = onMoveInstructionDown,
            onRemoveInstruction = onRemoveInstruction,
            onSaveUnit = onSaveUnit,
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnitEditorCard(
    editorState: com.loganmartlew.rangework.android.ui.PracticeUnitEditorState,
    clubCatalog: List<Club>,
    enabledClubCodes: Set<String>,
    isWorking: Boolean,
    onUpdateTitle: (String) -> Unit,
    onUpdateNotes: (String) -> Unit,
    onUpdateFocus: (String) -> Unit,
    onSelectDefaultClub: (String) -> Unit,
    onAddInstruction: () -> Unit,
    onUpdateInstructionText: (Int, String) -> Unit,
    onUpdateInstructionBallCount: (Int, String) -> Unit,
    onMoveInstructionUp: (Int) -> Unit,
    onMoveInstructionDown: (Int) -> Unit,
    onRemoveInstruction: (Int) -> Unit,
    onSaveUnit: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            OutlinedTextField(
                value = editorState.title,
                onValueChange = onUpdateTitle,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Title") },
                enabled = !isWorking,
                singleLine = true,
                isError = editorState.titleError != null,
                supportingText = editorState.titleError?.let { { Text(it) } },
            )
            OutlinedTextField(
                value = editorState.notes,
                onValueChange = onUpdateNotes,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Notes") },
                enabled = !isWorking,
                minLines = 3,
            )
            OutlinedTextField(
                value = editorState.focus,
                onValueChange = onUpdateFocus,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Focus") },
                enabled = !isWorking,
                singleLine = true,
            )
            ClubPickerField(
                label = "Default club",
                selectedCode = editorState.defaultClubReference.ifBlank { null },
                clubCatalog = clubCatalog,
                enabledClubCodes = enabledClubCodes,
                enabled = !isWorking,
                onSelect = onSelectDefaultClub,
            )
            Text(
                text = "Instructions",
                style = MaterialTheme.typography.titleMedium,
            )
            editorState.instructions.forEachIndexed { index, instruction ->
                InstructionEditorCard(
                    instruction = instruction,
                    isWorking = isWorking,
                    onUpdateText = { onUpdateInstructionText(index, it) },
                    onUpdateBallCount = { onUpdateInstructionBallCount(index, it) },
                    onMoveUp = { onMoveInstructionUp(index) },
                    onMoveDown = { onMoveInstructionDown(index) },
                    onRemove = { onRemoveInstruction(index) },
                    canMoveUp = index > 0,
                    canMoveDown = index < editorState.instructions.lastIndex,
                )
            }
            FilledTonalButton(
                enabled = !isWorking,
                onClick = onAddInstruction,
            ) {
                Text("Add instruction")
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = !isWorking,
                onClick = onSaveUnit,
            ) {
                Text("Save unit")
            }
        }
    }
}

@Composable
private fun InstructionEditorCard(
    instruction: PracticeInstructionEditorState,
    isWorking: Boolean,
    onUpdateText: (String) -> Unit,
    onUpdateBallCount: (String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                text = "Instruction ${instruction.order}",
                style = MaterialTheme.typography.titleSmall,
            )
            OutlinedTextField(
                value = instruction.text,
                onValueChange = onUpdateText,
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Instruction") },
                enabled = !isWorking,
                minLines = 2,
                isError = instruction.textError != null,
                supportingText = instruction.textError?.let { { Text(it) } },
            )
            OutlinedTextField(
                value = instruction.ballCount,
                onValueChange = { onUpdateBallCount(it.filter(Char::isDigit)) },
                modifier = Modifier.fillMaxWidth(),
                label = { Text("Ball count") },
                enabled = !isWorking,
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                isError = instruction.ballCountError != null,
                supportingText = instruction.ballCountError?.let { { Text(it) } },
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
