package com.loganmartlew.rangework.shared.model

sealed interface NextMoveState {
    data object NoUnits : NextMoveState
    data object UnitsNoSessions : NextMoveState
    data object Both : NextMoveState
    data class ResumeEditing(val entityId: String, val isUnit: Boolean) : NextMoveState
}

fun resolveNextMoveState(
    units: List<PracticeUnit>,
    sessions: List<PracticeSession>,
    lastSavedUnitId: String? = null,
    lastSavedSessionId: String? = null,
): NextMoveState {
    if (units.isEmpty()) return NextMoveState.NoUnits
    if (sessions.isEmpty()) return NextMoveState.UnitsNoSessions
    if (lastSavedUnitId != null) return NextMoveState.ResumeEditing(lastSavedUnitId, isUnit = true)
    if (lastSavedSessionId != null) return NextMoveState.ResumeEditing(lastSavedSessionId, isUnit = false)
    return NextMoveState.Both
}
