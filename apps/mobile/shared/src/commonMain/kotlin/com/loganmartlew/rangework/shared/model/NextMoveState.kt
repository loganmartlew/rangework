package com.loganmartlew.rangework.shared.model

sealed interface NextMoveState {
    data object NoUnits : NextMoveState
    data object UnitsNoSessions : NextMoveState
    data object Both : NextMoveState
    data class ResumeEditing(
        val entityId: String,
        val isUnit: Boolean,
        val entityName: String? = null,
    ) : NextMoveState
}

fun resolveNextMoveState(
    units: List<PracticeUnit>,
    sessions: List<PracticeSession>,
    lastSavedUnitId: String? = null,
    lastSavedSessionId: String? = null,
): NextMoveState {
    if (units.isEmpty()) return NextMoveState.NoUnits
    if (sessions.isEmpty()) return NextMoveState.UnitsNoSessions
    val savedUnit = lastSavedUnitId?.let { savedId ->
        units.firstOrNull { unit -> unit.id == savedId }
    }
    if (savedUnit != null) {
        return NextMoveState.ResumeEditing(
            entityId = savedUnit.id,
            isUnit = true,
            entityName = savedUnit.title,
        )
    }
    val savedSession = lastSavedSessionId?.let { savedId ->
        sessions.firstOrNull { session -> session.id == savedId }
    }
    if (savedSession != null) {
        return NextMoveState.ResumeEditing(
            entityId = savedSession.id,
            isUnit = false,
            entityName = savedSession.name,
        )
    }
    return NextMoveState.Both
}
