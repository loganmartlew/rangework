package com.loganmartlew.rangework.android.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.loganmartlew.rangework.android.ui.PracticeSessionItemEditorState
import com.loganmartlew.rangework.android.ui.PracticePlannerUiState
import com.loganmartlew.rangework.android.ui.ballSummary
import com.loganmartlew.rangework.android.ui.sessionEditorTotalText
import com.loganmartlew.rangework.android.ui.components.ClubPickerField
import com.loganmartlew.rangework.android.ui.components.CountStepper
import com.loganmartlew.rangework.android.ui.components.DockedSaveBar
import com.loganmartlew.rangework.android.ui.components.MoreOptionsExpander
import com.loganmartlew.rangework.android.ui.components.NumberBadge
import com.loganmartlew.rangework.android.ui.components.StickyTotalBar
import com.loganmartlew.rangework.android.ui.theme.RangeworkMono
import com.loganmartlew.rangework.shared.model.Club
import com.loganmartlew.rangework.shared.model.PracticeUnit
import com.loganmartlew.rangework.shared.model.derivedBallCount

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
internal fun SessionEditorScreen(
    plannerUiState: PracticePlannerUiState,
    title: String,
    onSaveSession: () -> Unit,
    onUpdateSessionName: (String) -> Unit,
    onUpdateSessionNotes: (String) -> Unit,
    onAddSessionItem: () -> Unit,
    onUpdateSessionItemUnit: (Int, String) -> Unit,
    onUpdateSessionItemRepeatCount: (Int, Int) -> Unit,
    onUpdateSessionItemClubReference: (Int, String) -> Unit,
    onUpdateSessionItemNotes: (Int, String) -> Unit,
    onUpdateSessionItemFocusCue: (Int, String) -> Unit,
    onMoveSessionItem: (Int, Int) -> Unit,
    onRemoveSessionItem: (Int) -> Unit,
    onNavigateToCreateUnit: () -> Unit,
) {
    val editor = plannerUiState.sessionEditor
    val isWorking = plannerUiState.isWorking
    val isCreateMode = editor.sessionId == null
    val hasSessionNotes = editor.notes.isNotBlank()

    val unitsById = remember(plannerUiState.units) {
        plannerUiState.units.associateBy(PracticeUnit::id)
    }

    val totalBalls = editor.items.sumOf { item ->
        item.derivedBallCount(unitsById[item.practiceUnitId]) ?: 0
    }

    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            DockedSaveBar(
                label = "Save session",
                onClick = onSaveSession,
                enabled = !isWorking,
            )
        },
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            stickyHeader {
                StickyTotalBar(
                    label = "Total",
                    value = sessionEditorTotalText(totalBalls),
                )
            }

            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(top = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    OutlinedTextField(
                        value = editor.name,
                        onValueChange = onUpdateSessionName,
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Name") },
                        enabled = !isWorking,
                        singleLine = true,
                        isError = editor.nameError != null,
                        supportingText = editor.nameError?.let { { Text(it) } },
                    )

                    MoreOptionsExpander(
                        label = if (isCreateMode && !hasSessionNotes) "Add notes" else "Session notes",
                        hasContent = hasSessionNotes,
                    ) {
                        OutlinedTextField(
                            value = editor.notes,
                            onValueChange = onUpdateSessionNotes,
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text("Session notes") },
                            supportingText = { Text("Reminders for the whole session") },
                            enabled = !isWorking,
                            minLines = 3,
                        )
                    }

                    Text(
                        text = "SESSION ITEMS",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp),
                    )
                }
            }

            if (editor.items.isEmpty()) {
                item {
                    Text(
                        text = if (plannerUiState.units.isEmpty())
                            "Add a practice unit first, then come back to build your session."
                        else
                            "No items yet. Use 'Add item' below to get started.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp),
                    )
                }
            }

            itemsIndexed(
                items = editor.items,
                key = { _, item -> item.order },
            ) { index, item ->
                SessionItemEditorCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                    item = item,
                    number = index + 1,
                    availableUnits = plannerUiState.units,
                    selectedUnit = unitsById[item.practiceUnitId],
                    clubCatalog = plannerUiState.clubCatalog,
                    enabledClubCodes = plannerUiState.enabledClubCodes,
                    isWorking = isWorking,
                    onSelectUnit = { onUpdateSessionItemUnit(index, it) },
                    onUpdateRepeatCount = { onUpdateSessionItemRepeatCount(index, it) },
                    onSelectClub = { onUpdateSessionItemClubReference(index, it) },
                    onUpdateNotes = { onUpdateSessionItemNotes(index, it) },
                    onUpdateFocusCue = { onUpdateSessionItemFocusCue(index, it) },
                    onMoveUp = { onMoveSessionItem(index, index - 1) },
                    onMoveDown = { onMoveSessionItem(index, index + 1) },
                    onRemove = { onRemoveSessionItem(index) },
                    canMoveUp = index > 0,
                    canMoveDown = index < editor.items.lastIndex,
                )
            }

            item {
                TextButton(
                    onClick = {
                        if (plannerUiState.units.isEmpty()) onNavigateToCreateUnit()
                        else onAddSessionItem()
                    },
                    enabled = !isWorking,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp),
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        if (plannerUiState.units.isEmpty()) "Create a unit first" else "Add item",
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SessionItemEditorCard(
    modifier: Modifier = Modifier,
    item: PracticeSessionItemEditorState,
    number: Int,
    availableUnits: List<PracticeUnit>,
    selectedUnit: PracticeUnit?,
    clubCatalog: List<Club>,
    enabledClubCodes: Set<String>,
    isWorking: Boolean,
    onSelectUnit: (String) -> Unit,
    onUpdateRepeatCount: (Int) -> Unit,
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
    val haptic = LocalHapticFeedback.current
    val repeatCountValue = item.repeatCount.trim().toIntOrNull() ?: 1
    val subtotal = item.derivedBallCount(selectedUnit)
    val hasMoreOptions = item.clubReference.isNotBlank() ||
        item.notes.isNotBlank() ||
        item.focusCue.isNotBlank()

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
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            // Header row: drag handle + badge + subtotal + delete
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Default.DragHandle,
                        contentDescription = "Drag to reorder item $number",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(24.dp),
                    )
                    NumberBadge(number = number)
                }
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = ballSummary(subtotal),
                        style = RangeworkMono.medium,
                        color = if (subtotal != null)
                            MaterialTheme.colorScheme.secondary
                        else
                            MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    IconButton(
                        onClick = onRemove,
                        modifier = Modifier
                            .size(48.dp)
                            .semantics { contentDescription = "Delete item $number" },
                        colors = IconButtonDefaults.iconButtonColors(
                            contentColor = MaterialTheme.colorScheme.error,
                        ),
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null)
                    }
                }
            }

            // Practice unit dropdown
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
                    trailingIcon = {
                        ExposedDropdownMenuDefaults.TrailingIcon(expanded = unitMenuExpanded)
                    },
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

            // Repeat count stepper
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Text(
                    text = "Repeats",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                CountStepper(
                    value = repeatCountValue,
                    onValueChange = onUpdateRepeatCount,
                    min = 1,
                    max = 50,
                    label = "Repeat count for item $number",
                )
                if (item.repeatCountError != null) {
                    Text(
                        text = item.repeatCountError,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }

            // Accessibility chevrons for TalkBack reorder
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
            ) {
                IconButton(
                    enabled = canMoveUp,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onMoveUp()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .semantics { contentDescription = "Move item $number up" },
                ) {
                    Icon(Icons.Default.KeyboardArrowUp, contentDescription = null)
                }
                IconButton(
                    enabled = canMoveDown,
                    onClick = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        onMoveDown()
                    },
                    modifier = Modifier
                        .size(48.dp)
                        .semantics { contentDescription = "Move item $number down" },
                ) {
                    Icon(Icons.Default.KeyboardArrowDown, contentDescription = null)
                }
            }

            // Optional fields: club override, item notes, focus cue
            MoreOptionsExpander(
                label = "More options",
                hasContent = hasMoreOptions,
            ) {
                ClubPickerField(
                    label = "Session club override",
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
                    supportingText = { Text("Reminders specific to this item") },
                    enabled = !isWorking,
                    minLines = 2,
                )
                OutlinedTextField(
                    value = item.focusCue,
                    onValueChange = onUpdateFocusCue,
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Focus cue") },
                    supportingText = { Text("One mental cue to hold while practising") },
                    enabled = !isWorking,
                    singleLine = true,
                )
            }
        }
    }
}

private fun PracticeSessionItemEditorState.derivedBallCount(unit: PracticeUnit?): Int? {
    val repeats = repeatCount.trim().toIntOrNull() ?: return unit?.derivedBallCount()
    return unit?.derivedBallCount()?.times(repeats)
}
