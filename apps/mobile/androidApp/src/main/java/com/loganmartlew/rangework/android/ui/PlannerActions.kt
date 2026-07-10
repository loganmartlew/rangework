package com.loganmartlew.rangework.android.ui

import com.loganmartlew.rangework.android.ui.theme.ThemeMode
import com.loganmartlew.rangework.shared.model.DistanceUnit
import com.loganmartlew.rangework.shared.model.Handedness
import com.loganmartlew.rangework.shared.model.ObservationType
import com.loganmartlew.rangework.shared.model.SpeedUnit
import com.loganmartlew.rangework.shared.model.TagAttachmentCounts

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
    val onUpdateSuccessCriterion: (String) -> Unit,
    val onUpdateDefaultClubCode: (String) -> Unit,
    val onAddInstruction: () -> Unit,
    val onUpdateInstructionText: (Int, String) -> Unit,
    val onUpdateInstructionBallCount: (Int, Int) -> Unit,
    val onUpdateInstructionClubCode: (Int, String) -> Unit,
    val onMoveInstructionUp: (Int) -> Unit,
    val onMoveInstructionDown: (Int) -> Unit,
    val onMoveInstruction: (Int, Int) -> Unit,
    val onRemoveInstruction: (Int) -> Unit,
    val onToggleTag: (String) -> Unit,
    val onCreateTag: (String) -> Unit,
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
    val onUpdateItemClubCode: (Int, String) -> Unit,
    val onUpdateItemNotes: (Int, String) -> Unit,
    val onUpdateItemFocusCue: (Int, String) -> Unit,
    val onToggleItemObservationType: (Int, ObservationType) -> Unit,
    val onMoveItem: (Int, Int) -> Unit,
    val onRemoveItem: (Int) -> Unit,
    val onToggleTag: (String) -> Unit,
    val onCreateTag: (String) -> Unit,
    val onSave: () -> Unit,
)

data class SettingsActions(
    val onSignOut: () -> Unit,
    val onSetThemeMode: (ThemeMode) -> Unit,
    val onSelectDistanceUnit: (DistanceUnit) -> Unit,
    val onSelectSpeedUnit: (SpeedUnit) -> Unit,
    val onSelectHandedness: (Handedness) -> Unit,
    val onSetClubEnabled: (String, Boolean) -> Unit,
    val onEnableCommonBag: () -> Unit,
    val onDisableAllClubs: () -> Unit,
    val onRenameTag: (String, String) -> Unit,
    val onDeleteTag: (String) -> Unit,
    val onLoadTagAttachmentCounts: (String, (TagAttachmentCounts) -> Unit) -> Unit,
    val onLoadTags: () -> Unit,
)
