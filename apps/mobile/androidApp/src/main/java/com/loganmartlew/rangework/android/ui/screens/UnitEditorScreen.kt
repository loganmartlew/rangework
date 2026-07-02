package com.loganmartlew.rangework.android.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.ui.PracticeInstructionEditorState
import com.loganmartlew.rangework.android.ui.PracticePlannerUiState
import com.loganmartlew.rangework.android.ui.ballSummary
import com.loganmartlew.rangework.android.ui.components.ClubPickerField
import com.loganmartlew.rangework.android.ui.components.CountStepper
import com.loganmartlew.rangework.android.ui.components.DockedSaveBar
import com.loganmartlew.rangework.android.ui.components.MoreOptionsExpander
import com.loganmartlew.rangework.android.ui.components.NumberBadge
import com.loganmartlew.rangework.android.ui.components.ReorderableItemRow
import com.loganmartlew.rangework.android.ui.components.TagPicker
import com.loganmartlew.rangework.android.ui.theme.RangeworkMono
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun UnitEditorScreen(
    plannerUiState: PracticePlannerUiState,
    title: String,
    onSaveUnit: () -> Unit,
    onUpdateTitle: (String) -> Unit,
    onUpdateNotes: (String) -> Unit,
    onUpdateFocus: (String) -> Unit,
    onUpdateDefaultClubCode: (String) -> Unit,
    onAddInstruction: () -> Unit,
    onUpdateInstructionText: (Int, String) -> Unit,
    onUpdateInstructionBallCount: (Int, Int) -> Unit,
    onMoveInstructionUp: (Int) -> Unit,
    onMoveInstructionDown: (Int) -> Unit,
    onMoveInstruction: (Int, Int) -> Unit,
    onRemoveInstruction: (Int) -> Unit,
    onToggleTag: (String) -> Unit,
    onCreateTag: (String) -> Unit,
) {
    val editor = plannerUiState.unitEditor
    val isWorking = plannerUiState.isWorking
    val isCreateMode = editor.unitId == null
    val hasNotesOrFocus = editor.notes.isNotBlank() || editor.focus.isNotBlank()
    val totalBalls = editor.instructions.sumOf { it.ballCount.toIntOrNull() ?: 0 }

    val lazyListState = rememberLazyListState()
    val reorderableState = rememberReorderableLazyListState(lazyListState) { from, to ->
        // Header items before the instruction list: title, notes/focus, club, tags, INSTRUCTIONS label.
        val headerCount = 5
        onMoveInstruction(from.index - headerCount, to.index - headerCount)
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            DockedSaveBar(
                label = "Save unit",
                onClick = onSaveUnit,
                enabled = !isWorking,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            state = lazyListState,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                OutlinedTextField(
                    value = editor.title,
                    onValueChange = onUpdateTitle,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Title") },
                    enabled = !isWorking,
                    singleLine = true,
                    isError = editor.titleError != null,
                    supportingText = editor.titleError?.let { { Text(it) } },
                )
            }

            item {
                MoreOptionsExpander(
                    label = if (isCreateMode && !hasNotesOrFocus) "Add notes & focus" else "Notes & focus",
                    hasContent = hasNotesOrFocus,
                ) {
                    OutlinedTextField(
                        value = editor.notes,
                        onValueChange = onUpdateNotes,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Unit notes") },
                        supportingText = { Text("General reminders for this drill") },
                        enabled = !isWorking,
                        minLines = 3,
                    )
                    OutlinedTextField(
                        value = editor.focus,
                        onValueChange = onUpdateFocus,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Focus cue") },
                        supportingText = { Text("One mental cue to hold while practising") },
                        enabled = !isWorking,
                        singleLine = true,
                    )
                }
            }

            item {
                ClubPickerField(
                    label = "Default club",
                    selectedCode = editor.defaultClubCode.ifBlank { null },
                    clubCatalog = plannerUiState.clubCatalog,
                    enabledClubCodes = plannerUiState.enabledClubCodes,
                    enabled = !isWorking,
                    onSelect = onUpdateDefaultClubCode,
                )
            }

            item {
                TagPicker(
                    availableTags = plannerUiState.availableTags,
                    selectedTagIds = editor.tagIds,
                    enabled = !isWorking,
                    onToggle = onToggleTag,
                    onCreate = onCreateTag,
                )
            }

            item {
                Text(
                    text = "INSTRUCTIONS",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }

            itemsIndexed(
                items = editor.instructions,
                key = { _, instruction -> instruction.order },
            ) { index, instruction ->
                ReorderableItem(reorderableState, key = instruction.order) { _ ->
                    InstructionEditorRow(
                        instruction = instruction,
                        number = index + 1,
                        isWorking = isWorking,
                        dragHandleModifier = Modifier.draggableHandle(),
                        onUpdateText = { onUpdateInstructionText(index, it) },
                        onUpdateBallCount = { onUpdateInstructionBallCount(index, it) },
                        onMoveUp = { onMoveInstructionUp(index) },
                        onMoveDown = { onMoveInstructionDown(index) },
                        onRemove = { onRemoveInstruction(index) },
                        canMoveUp = index > 0,
                        canMoveDown = index < editor.instructions.lastIndex,
                    )
                }
            }

            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp, bottom = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = "Total",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = ballSummary(totalBalls),
                        style = RangeworkMono.medium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }

            item {
                TextButton(
                    onClick = onAddInstruction,
                    enabled = !isWorking,
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add instruction")
                }
            }
        }
    }
}

@Composable
private fun InstructionEditorRow(
    instruction: PracticeInstructionEditorState,
    number: Int,
    isWorking: Boolean,
    dragHandleModifier: Modifier = Modifier,
    onUpdateText: (String) -> Unit,
    onUpdateBallCount: (Int) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
) {
    val ballCountValue = instruction.ballCount.toIntOrNull() ?: 1

    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        tonalElevation = 0.dp,
    ) {
        ReorderableItemRow(
            canMoveUp = canMoveUp,
            canMoveDown = canMoveDown,
            onMoveUp = onMoveUp,
            onMoveDown = onMoveDown,
            onDelete = onRemove,
            moveUpContentDescription = "Move instruction $number up",
            moveDownContentDescription = "Move instruction $number down",
            deleteContentDescription = "Delete instruction $number",
            dragHandleModifier = dragHandleModifier,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
            leadingContent = {
                NumberBadge(
                    number = number,
                    modifier = Modifier.padding(end = 4.dp),
                )
            },
        ) {
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
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Balls",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                CountStepper(
                    value = ballCountValue,
                    onValueChange = onUpdateBallCount,
                    min = 0,
                    max = 100,
                    label = "Ball count for instruction $number",
                )
                val ballCountError = instruction.ballCountError
                if (ballCountError != null) {
                    Text(
                        text = ballCountError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}
