package com.loganmartlew.rangework.android.ui

import com.loganmartlew.rangework.android.ui.theme.ThemeMode
import com.loganmartlew.rangework.shared.model.DistanceUnit
import com.loganmartlew.rangework.shared.model.SpeedUnit

data class UnitEditorActions(
    val onBeginNew: () -> Unit,
    val onEdit: (String) -> Unit,
    val onDelete: (String) -> Unit,
    val onDuplicate: (String) -> Unit,
    val onClearDuplicatedId: () -> Unit,
    val onClearBaselines: () -> Unit,
    val onConsumeSavedId: () -> Unit,
    val onUpdateTitle: (String) -> Unit,
    val onUpdateNotes: (String) -> Unit,
    val onUpdateFocus: (String) -> Unit,
    val onUpdateDefaultClubReference: (String) -> Unit,
    val onAddInstruction: () -> Unit,
    val onUpdateInstructionText: (Int, String) -> Unit,
    val onUpdateInstructionBallCount: (Int, Int) -> Unit,
    val onMoveInstructionUp: (Int) -> Unit,
    val onMoveInstructionDown: (Int) -> Unit,
    val onMoveInstruction: (Int, Int) -> Unit,
    val onRemoveInstruction: (Int) -> Unit,
    val onSave: () -> Unit,
)

data class SessionEditorActions(
    val onBeginNew: () -> Unit,
    val onEdit: (String) -> Unit,
    val onDelete: (String) -> Unit,
    val onDuplicate: (String) -> Unit,
    val onConsumeSavedId: () -> Unit,
    val onClearDuplicatedId: () -> Unit,
    val onUpdateName: (String) -> Unit,
    val onUpdateNotes: (String) -> Unit,
    val onAddItem: () -> Unit,
    val onUpdateItemUnit: (Int, String) -> Unit,
    val onUpdateItemRepeatCount: (Int, Int) -> Unit,
    val onUpdateItemClubReference: (Int, String) -> Unit,
    val onUpdateItemNotes: (Int, String) -> Unit,
    val onUpdateItemFocusCue: (Int, String) -> Unit,
    val onMoveItem: (Int, Int) -> Unit,
    val onRemoveItem: (Int) -> Unit,
    val onSave: () -> Unit,
)

data class SettingsActions(
    val onSignOut: () -> Unit,
    val onSetThemeMode: (ThemeMode) -> Unit,
    val onToggleDynamicColor: () -> Unit,
    val onSelectDistanceUnit: (DistanceUnit) -> Unit,
    val onSelectSpeedUnit: (SpeedUnit) -> Unit,
    val onSetClubEnabled: (String, Boolean) -> Unit,
    val onEnableCommonBag: () -> Unit,
    val onDisableAllClubs: () -> Unit,
)
