package com.loganmartlew.rangework.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.loganmartlew.rangework.shared.auth.AuthState
import com.loganmartlew.rangework.shared.config.AppEnvironment
import com.loganmartlew.rangework.shared.data.DataFoundation
import com.loganmartlew.rangework.shared.model.Club
import com.loganmartlew.rangework.shared.model.PracticeInstruction
import com.loganmartlew.rangework.shared.model.PracticeInstructionDraft
import com.loganmartlew.rangework.shared.model.PracticeSession
import com.loganmartlew.rangework.shared.model.PracticeSessionDraft
import com.loganmartlew.rangework.shared.model.PracticeSessionItem
import com.loganmartlew.rangework.shared.model.PracticeSessionItemDraft
import com.loganmartlew.rangework.shared.model.PracticeUnit
import com.loganmartlew.rangework.shared.model.PracticeUnitDraft
import com.loganmartlew.rangework.shared.model.EnabledClubCount
import com.loganmartlew.rangework.shared.model.NextMoveState
import com.loganmartlew.rangework.shared.model.RecentItem
import com.loganmartlew.rangework.shared.model.ValidationIssue
import com.loganmartlew.rangework.shared.model.recentItems
import com.loganmartlew.rangework.shared.model.resolveNextMoveState
import com.loganmartlew.rangework.shared.model.validationIssues
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class PracticeInstructionEditorState(
    val order: Int,
    val text: String = "",
    val ballCount: String = "",
    val textError: String? = null,
    val ballCountError: String? = null,
) {
    fun withoutErrors() = copy(textError = null, ballCountError = null)
}

data class PracticeUnitEditorState(
    val unitId: String? = null,
    val title: String = "",
    val notes: String = "",
    val focus: String = "",
    val defaultClubReference: String = "",
    val instructions: List<PracticeInstructionEditorState> = listOf(
        PracticeInstructionEditorState(order = 1),
    ),
    val titleError: String? = null,
) {
    fun withoutErrors() = copy(
        titleError = null,
        instructions = instructions.map { it.withoutErrors() },
    )
}

data class PracticeSessionItemEditorState(
    val order: Int,
    val practiceUnitId: String = "",
    val repeatCount: String = "1",
    val clubReference: String = "",
    val notes: String = "",
    val focusCue: String = "",
    val unitError: String? = null,
    val repeatCountError: String? = null,
) {
    fun withoutErrors() = copy(unitError = null, repeatCountError = null)
}

data class PracticeSessionEditorState(
    val sessionId: String? = null,
    val name: String = "",
    val notes: String = "",
    val items: List<PracticeSessionItemEditorState> = emptyList(),
    val nameError: String? = null,
) {
    fun withoutErrors() = copy(
        nameError = null,
        items = items.map { it.withoutErrors() },
    )
}

data class PracticePlannerUiState(
    val environment: AppEnvironment,
    val dataConfigured: Boolean,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val hasLoaded: Boolean = false,
    val units: List<PracticeUnit> = emptyList(),
    val sessions: List<PracticeSession> = emptyList(),
    val clubCatalog: List<Club> = emptyList(),
    val enabledClubCodes: Set<String> = emptySet(),
    val unitEditor: PracticeUnitEditorState = PracticeUnitEditorState(),
    val sessionEditor: PracticeSessionEditorState = PracticeSessionEditorState(),
    val unitEditorBaseline: PracticeUnitEditorState? = null,
    val sessionEditorBaseline: PracticeSessionEditorState? = null,
    val savedUnitId: String? = null,
    val savedSessionId: String? = null,
    val duplicatedUnitId: String? = null,
    val duplicatedSessionId: String? = null,
    val status: PlannerStatus? = if (dataConfigured) null else PlannerStatus.Unavailable,
) {
    val isWorking: Boolean
        get() = isLoading || isSaving

    val isUnitEditorDirty: Boolean
        get() = unitEditorBaseline != null && unitEditor.withoutErrors() != unitEditorBaseline

    val isSessionEditorDirty: Boolean
        get() = sessionEditorBaseline != null && sessionEditor.withoutErrors() != sessionEditorBaseline

    val enabledClubCount: EnabledClubCount
        get() = EnabledClubCount.from(clubCatalog, enabledClubCodes)

    val recentItems: List<RecentItem>
        get() = recentItems(units, sessions)

    val nextMoveState: NextMoveState
        get() = resolveNextMoveState(units, sessions, savedUnitId, savedSessionId)
}

class PracticePlannerViewModel(
    private val environment: AppEnvironment,
    private val dataFoundation: DataFoundation?,
) : ViewModel() {
    private var activeUserId: String? = null

    private val operationMutex = Mutex()
    private var operationToken = 0

    private val _uiState = MutableStateFlow(
        PracticePlannerUiState(
            environment = environment,
            dataConfigured = dataFoundation != null,
        ),
    )
    val uiState: StateFlow<PracticePlannerUiState> = _uiState.asStateFlow()

    fun onAuthStateChanged(authState: AuthState) {
        when (authState) {
            is AuthState.SignedIn -> {
                activeUserId = authState.userId
                if (dataFoundation == null) {
                    _uiState.value = _uiState.value.copy(
                        status = PlannerStatus.Unavailable,
                    )
                    return
                }
                refreshPlanning(
                    status = if (_uiState.value.units.isEmpty() && _uiState.value.sessions.isEmpty()) {
                        PlannerStatus.Info("Planning workspace ready.")
                    } else {
                        _uiState.value.status
                    },
                )
                loadClubs()
            }

            AuthState.Restoring -> {
                _uiState.value = _uiState.value.copy(isLoading = true)
            }

            AuthState.SignedOut,
            is AuthState.Error,
            -> {
                activeUserId = null
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSaving = false,
                    hasLoaded = false,
                    units = emptyList(),
                    sessions = emptyList(),
                    clubCatalog = emptyList(),
                    enabledClubCodes = emptySet(),
                    unitEditor = PracticeUnitEditorState(),
                    sessionEditor = PracticeSessionEditorState(),
                    unitEditorBaseline = null,
                    sessionEditorBaseline = null,
                    savedUnitId = null,
                    savedSessionId = null,
                    duplicatedUnitId = null,
                    duplicatedSessionId = null,
                    status = if (dataFoundation == null) {
                        PlannerStatus.Unavailable
                    } else {
                        null
                    },
                )
            }
        }
    }

    fun refreshPlanning() {
        refreshPlanning(status = PlannerStatus.Notification("Planning data refreshed."))
    }

    fun refreshPlanningOnNavigation() {
        refreshPlanning(
            status = _uiState.value.status,
            skipIfWorking = true,
        )
    }

    fun beginNewUnit() {
        val freshEditor = PracticeUnitEditorState()
        _uiState.value = _uiState.value.copy(
            unitEditor = freshEditor,
            unitEditorBaseline = freshEditor,
            savedUnitId = null,
            status = null,
        )
    }

    fun editUnit(unitId: String) {
        val unit = _uiState.value.units.firstOrNull { item -> item.id == unitId } ?: return
        val editorState = unit.toEditorState()
        _uiState.value = _uiState.value.copy(
            unitEditor = editorState,
            unitEditorBaseline = editorState,
            savedUnitId = null,
            status = PlannerStatus.Info("Editing ${unit.title}."),
        )
    }

    fun consumeSavedUnitId() {
        _uiState.value = _uiState.value.copy(savedUnitId = null)
    }

    fun updateUnitTitle(value: String) = updateUnitEditor { copy(title = value, titleError = null) }

    fun updateUnitNotes(value: String) = updateUnitEditor { copy(notes = value) }

    fun updateUnitFocus(value: String) = updateUnitEditor { copy(focus = value) }

    fun updateUnitDefaultClubReference(value: String) = updateUnitEditor { copy(defaultClubReference = value) }

    fun addInstruction() = updateUnitEditor {
        copy(
            instructions = reindexedInstructions(
                instructions + PracticeInstructionEditorState(order = instructions.size + 1),
            ),
        )
    }

    fun updateInstructionText(index: Int, value: String) = updateInstruction(index) { copy(text = value, textError = null) }

    fun updateInstructionBallCount(index: Int, value: String) = updateInstruction(index) {
        copy(ballCount = value, ballCountError = null)
    }

    fun moveInstructionUp(index: Int) = moveInstruction(index, index - 1)

    fun moveInstructionDown(index: Int) = moveInstruction(index, index + 1)

    fun removeInstruction(index: Int) = updateUnitEditor {
        val nextInstructions = if (instructions.size == 1) {
            listOf(PracticeInstructionEditorState(order = 1))
        } else {
            instructions.filterIndexed { currentIndex, _ -> currentIndex != index }
        }
        copy(instructions = reindexedInstructions(nextInstructions))
    }

    fun saveUnit() {
        val foundation = dataFoundation ?: return markPlannerUnavailable()
        if (activeUserId == null) {
            markSignedOut()
            return
        }

        val editor = _uiState.value.unitEditor
        val draft = editor.toDraft()
        val issues = draft.validationIssues()

        if (issues.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                unitEditor = editor.withErrors(issues),
                status = PlannerStatus.Notification(issues.joinToString(" ") { it.message }),
            )
            return
        }

        val token = ++operationToken
        viewModelScope.launch {
            operationMutex.withLock {
                _uiState.value = _uiState.value.copy(
                    isSaving = true,
                    savedUnitId = null,
                    status = null,
                )

                try {
                    val savedUnit = foundation.savePracticeUnitUseCase(
                        draft = draft,
                        unitId = editor.unitId,
                    )
                    val units = foundation.listPracticeUnitsUseCase()
                    val sessions = foundation.listPracticeSessionsUseCase()
                    if (token == operationToken) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isSaving = false,
                            units = units,
                            sessions = sessions,
                            unitEditor = savedUnit.toEditorState(),
                            unitEditorBaseline = null,
                            sessionEditor = _uiState.value.sessionEditor.resolveWith(sessions),
                            savedUnitId = savedUnit.id,
                            status = PlannerStatus.Notification("Saved ${savedUnit.title}."),
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(isSaving = false)
                    }
                } catch (exception: Exception) {
                    if (token == operationToken) {
                        markSaveFailure(exception, "Unit save failed.")
                    } else {
                        _uiState.value = _uiState.value.copy(isSaving = false)
                    }
                }
            }
        }
    }

    fun deleteUnit(unitId: String) {
        val foundation = dataFoundation ?: return markPlannerUnavailable()
        if (activeUserId == null) {
            markSignedOut()
            return
        }

        val token = ++operationToken
        viewModelScope.launch {
            operationMutex.withLock {
                _uiState.value = _uiState.value.copy(isSaving = true, savedUnitId = null)
                try {
                    val title = _uiState.value.units.firstOrNull { unit -> unit.id == unitId }?.title ?: "unit"
                    foundation.deletePracticeUnitUseCase(unitId)
                    val units = foundation.listPracticeUnitsUseCase()
                    val sessions = foundation.listPracticeSessionsUseCase()
                    if (token == operationToken) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isSaving = false,
                            units = units,
                            sessions = sessions,
                            unitEditor = if (_uiState.value.unitEditor.unitId == unitId) {
                                PracticeUnitEditorState()
                            } else {
                                _uiState.value.unitEditor.resolveWith(units)
                            },
                            sessionEditor = _uiState.value.sessionEditor.resolveWith(sessions),
                            status = PlannerStatus.Notification("Deleted $title."),
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(isSaving = false)
                    }
                } catch (exception: Exception) {
                    if (token == operationToken) {
                        if (exception.isForeignKeyViolation()) {
                            markSaveFailure(
                                exception,
                                "This unit is used by one or more sessions. Remove it from those sessions first.",
                            )
                        } else {
                            markSaveFailure(exception, "Unit delete failed.")
                        }
                    } else {
                        _uiState.value = _uiState.value.copy(isSaving = false)
                    }
                }
            }
        }
    }

    fun beginNewSession() {
        val freshEditor = PracticeSessionEditorState()
        _uiState.value = _uiState.value.copy(
            sessionEditor = freshEditor,
            sessionEditorBaseline = freshEditor,
            savedSessionId = null,
            status = null,
        )
    }

    fun editSession(sessionId: String) {
        val session = _uiState.value.sessions.firstOrNull { item -> item.id == sessionId } ?: return
        val editorState = session.toEditorState()
        _uiState.value = _uiState.value.copy(
            sessionEditor = editorState,
            sessionEditorBaseline = editorState,
            savedSessionId = null,
            status = PlannerStatus.Info("Editing ${session.name}."),
        )
    }

    fun consumeSavedSessionId() {
        _uiState.value = _uiState.value.copy(savedSessionId = null)
    }

    fun updateSessionName(value: String) = updateSessionEditor { copy(name = value, nameError = null) }

    fun updateSessionNotes(value: String) = updateSessionEditor { copy(notes = value) }

    fun addSessionItem() = updateSessionEditor {
        val defaultUnitId = _uiState.value.units.firstOrNull()?.id.orEmpty()
        copy(
            items = reindexedSessionItems(
                items + PracticeSessionItemEditorState(
                    order = items.size + 1,
                    practiceUnitId = defaultUnitId,
                    repeatCount = "1",
                ),
            ),
        )
    }

    fun updateSessionItemUnit(index: Int, practiceUnitId: String) = updateSessionItem(index) {
        copy(practiceUnitId = practiceUnitId, unitError = null)
    }

    fun updateSessionItemRepeatCount(index: Int, value: String) = updateSessionItem(index) {
        copy(repeatCount = value, repeatCountError = null)
    }

    fun updateSessionItemClubReference(index: Int, value: String) = updateSessionItem(index) {
        copy(clubReference = value)
    }

    fun updateSessionItemNotes(index: Int, value: String) = updateSessionItem(index) { copy(notes = value) }

    fun updateSessionItemFocusCue(index: Int, value: String) = updateSessionItem(index) { copy(focusCue = value) }


    fun moveSessionItem(fromIndex: Int, toIndex: Int) {
        updateSessionEditor {
            copy(
                items = reindexedSessionItems(
                    items.moveItem(fromIndex, toIndex),
                ),
            )
        }
    }

    fun removeSessionItem(index: Int) = updateSessionEditor {
        copy(
            items = reindexedSessionItems(
                items.filterIndexed { currentIndex, _ -> currentIndex != index },
            ),
        )
    }

    fun saveSession() {
        val foundation = dataFoundation ?: return markPlannerUnavailable()
        if (activeUserId == null) {
            markSignedOut()
            return
        }

        val editor = _uiState.value.sessionEditor

        // Check for empty required repeat counts before draft conversion
        val parseIssues = editor.items.mapIndexedNotNull { index, item ->
            if (item.repeatCount.trim().isEmpty()) {
                ValidationIssue("items[$index].repeatCount", "Repeat count is required.")
            } else {
                null
            }
        }

        if (parseIssues.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                sessionEditor = editor.withErrors(parseIssues),
                status = PlannerStatus.Notification(parseIssues.joinToString(" ") { it.message }),
            )
            return
        }

        val draft = editor.toDraft()
        val issues = draft.validationIssues()

        if (issues.isNotEmpty()) {
            _uiState.value = _uiState.value.copy(
                sessionEditor = editor.withErrors(issues),
                status = PlannerStatus.Notification(issues.joinToString(" ") { it.message }),
            )
            return
        }

        val token = ++operationToken
        viewModelScope.launch {
            operationMutex.withLock {
                _uiState.value = _uiState.value.copy(
                    isSaving = true,
                    savedSessionId = null,
                    status = null,
                )

                try {
                    val savedSession = foundation.savePracticeSessionUseCase(
                        draft = draft,
                        sessionId = editor.sessionId,
                    )
                    val units = foundation.listPracticeUnitsUseCase()
                    val sessions = foundation.listPracticeSessionsUseCase()
                    if (token == operationToken) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isSaving = false,
                            units = units,
                            sessions = sessions,
                            unitEditor = _uiState.value.unitEditor.resolveWith(units),
                            sessionEditor = savedSession.toEditorState(),
                            sessionEditorBaseline = null,
                            savedSessionId = savedSession.id,
                            status = PlannerStatus.Notification("Saved ${savedSession.name}."),
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(isSaving = false)
                    }
                } catch (exception: Exception) {
                    if (token == operationToken) {
                        markSaveFailure(exception, "Session save failed.")
                    } else {
                        _uiState.value = _uiState.value.copy(isSaving = false)
                    }
                }
            }
        }
    }

    fun deleteSession(sessionId: String) {
        val foundation = dataFoundation ?: return markPlannerUnavailable()
        if (activeUserId == null) {
            markSignedOut()
            return
        }

        val token = ++operationToken
        viewModelScope.launch {
            operationMutex.withLock {
                _uiState.value = _uiState.value.copy(isSaving = true, savedSessionId = null)
                try {
                    val name = _uiState.value.sessions.firstOrNull { session -> session.id == sessionId }?.name ?: "session"
                    foundation.deletePracticeSessionUseCase(sessionId)
                    val units = foundation.listPracticeUnitsUseCase()
                    val sessions = foundation.listPracticeSessionsUseCase()
                    if (token == operationToken) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isSaving = false,
                            units = units,
                            sessions = sessions,
                            unitEditor = _uiState.value.unitEditor.resolveWith(units),
                            sessionEditor = if (_uiState.value.sessionEditor.sessionId == sessionId) {
                                PracticeSessionEditorState()
                            } else {
                                _uiState.value.sessionEditor.resolveWith(sessions)
                            },
                            status = PlannerStatus.Notification("Deleted $name."),
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(isSaving = false)
                    }
                } catch (exception: Exception) {
                    if (token == operationToken) {
                        markSaveFailure(exception, "Session delete failed.")
                    } else {
                        _uiState.value = _uiState.value.copy(isSaving = false)
                    }
                }
            }
        }
    }

    fun duplicateUnit(unitId: String) {
        val foundation = dataFoundation ?: return markPlannerUnavailable()
        if (activeUserId == null) {
            markSignedOut()
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, duplicatedUnitId = null)
            try {
                val duplicated = foundation.duplicatePracticeUnitUseCase(unitId)
                val units = foundation.listPracticeUnitsUseCase()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSaving = false,
                    units = units,
                    duplicatedUnitId = duplicated.id,
                    status = PlannerStatus.Notification("Duplicated ${duplicated.title}."),
                )
            } catch (exception: Exception) {
                markSaveFailure(exception, "Unit duplicate failed.")
            }
        }
    }

    fun clearDuplicatedUnitId() {
        _uiState.value = _uiState.value.copy(duplicatedUnitId = null)
    }

    fun duplicateSession(sessionId: String) {
        val foundation = dataFoundation ?: return markPlannerUnavailable()
        if (activeUserId == null) {
            markSignedOut()
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, duplicatedSessionId = null)
            try {
                val duplicated = foundation.duplicatePracticeSessionUseCase(sessionId)
                val sessions = foundation.listPracticeSessionsUseCase()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSaving = false,
                    sessions = sessions,
                    duplicatedSessionId = duplicated.id,
                    status = PlannerStatus.Notification("Duplicated ${duplicated.name}."),
                )
            } catch (exception: Exception) {
                markSaveFailure(exception, "Session duplicate failed.")
            }
        }
    }

    fun clearDuplicatedSessionId() {
        _uiState.value = _uiState.value.copy(duplicatedSessionId = null)
    }

    fun restoreUnit(unit: PracticeUnit) {
        val foundation = dataFoundation ?: return markPlannerUnavailable()
        if (activeUserId == null) { markSignedOut(); return }
        viewModelScope.launch {
            try {
                val draft = PracticeUnitDraft(
                    title = unit.title,
                    notes = unit.notes,
                    focus = unit.focus,
                    defaultClubReference = unit.defaultClubReference,
                    instructions = unit.instructions.map { PracticeInstructionDraft(it.order, it.text, it.ballCount) },
                )
                foundation.savePracticeUnitUseCase(draft, unitId = unit.id)
                val units = foundation.listPracticeUnitsUseCase()
                val sessions = foundation.listPracticeSessionsUseCase()
                _uiState.value = _uiState.value.copy(
                    units = units,
                    sessions = sessions,
                    status = PlannerStatus.Notification("Restored \"${unit.title}\"."),
                )
            } catch (e: Exception) {
                markSaveFailure(e, "Restore failed.")
            }
        }
    }

    fun restoreSession(session: PracticeSession) {
        val foundation = dataFoundation ?: return markPlannerUnavailable()
        if (activeUserId == null) { markSignedOut(); return }
        viewModelScope.launch {
            try {
                val draft = PracticeSessionDraft(
                    name = session.name,
                    notes = session.notes,
                    items = session.items.map { item ->
                        PracticeSessionItemDraft(
                            practiceUnitId = item.practiceUnitId,
                            order = item.order,
                            repeatCount = item.repeatCount,
                            clubReference = item.clubReference,
                            notes = item.notes,
                            focusCue = item.focusCue,
                        )
                    },
                )
                foundation.savePracticeSessionUseCase(draft, sessionId = session.id)
                val units = foundation.listPracticeUnitsUseCase()
                val sessions = foundation.listPracticeSessionsUseCase()
                _uiState.value = _uiState.value.copy(
                    units = units,
                    sessions = sessions,
                    status = PlannerStatus.Notification("Restored \"${session.name}\"."),
                )
            } catch (e: Exception) {
                markSaveFailure(e, "Restore failed.")
            }
        }
    }

    fun clearEditorBaselines() {
        _uiState.value = _uiState.value.copy(
            unitEditorBaseline = null,
            sessionEditorBaseline = null,
        )
    }

    private fun refreshPlanning(
        status: PlannerStatus?,
        skipIfWorking: Boolean = false,
    ) {
        val foundation = dataFoundation ?: return markPlannerUnavailable()
        if (activeUserId == null) {
            markSignedOut()
            return
        }
        if (skipIfWorking && _uiState.value.isWorking) {
            return
        }

        _uiState.value = _uiState.value.copy(isLoading = true)

        val token = ++operationToken
        viewModelScope.launch {
            operationMutex.withLock {
                try {
                    val units = foundation.listPracticeUnitsUseCase()
                    val sessions = foundation.listPracticeSessionsUseCase()
                    if (token == operationToken) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isSaving = false,
                            hasLoaded = true,
                            units = units,
                            sessions = sessions,
                            unitEditor = _uiState.value.unitEditor.resolveWith(units),
                            sessionEditor = _uiState.value.sessionEditor.resolveWith(sessions),
                            status = status,
                        )
                    } else {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                } catch (exception: Exception) {
                    if (token == operationToken) {
                        markRefreshFailure(exception, "Planning refresh failed.")
                    } else {
                        _uiState.value = _uiState.value.copy(isLoading = false)
                    }
                }
            }
        }
    }

    private fun markPlannerUnavailable() {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isSaving = false,
            status = PlannerStatus.Unavailable,
        )
    }

    private fun markSignedOut() {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isSaving = false,
            status = PlannerStatus.Notification("Sign in before changing practice plans."),
        )
    }

    private fun markRefreshFailure(exception: Throwable, fallback: String) {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isSaving = false,
            status = plannerStatus(exception = exception, fallback = fallback),
        )
    }

    private fun markSaveFailure(exception: Throwable, fallback: String) {
        _uiState.value = _uiState.value.copy(
            isSaving = false,
            status = plannerStatus(exception = exception, fallback = fallback),
        )
    }

    private fun updateUnitEditor(
        transform: PracticeUnitEditorState.() -> PracticeUnitEditorState,
    ) {
        _uiState.value = _uiState.value.copy(unitEditor = _uiState.value.unitEditor.transform())
    }

    private fun updateInstruction(
        index: Int,
        transform: PracticeInstructionEditorState.() -> PracticeInstructionEditorState,
    ) {
        updateUnitEditor {
            copy(
                instructions = reindexedInstructions(
                    instructions.mapIndexed { currentIndex, instruction ->
                        if (currentIndex == index) {
                            instruction.transform()
                        } else {
                            instruction
                        }
                    },
                ),
            )
        }
    }

    private fun moveInstruction(fromIndex: Int, toIndex: Int) {
        updateUnitEditor {
            copy(
                instructions = reindexedInstructions(
                    instructions.moveItem(fromIndex, toIndex),
                ),
            )
        }
    }

    private fun updateSessionEditor(
        transform: PracticeSessionEditorState.() -> PracticeSessionEditorState,
    ) {
        _uiState.value = _uiState.value.copy(sessionEditor = _uiState.value.sessionEditor.transform())
    }

    private fun updateSessionItem(
        index: Int,
        transform: PracticeSessionItemEditorState.() -> PracticeSessionItemEditorState,
    ) {
        updateSessionEditor {
            copy(
                items = reindexedSessionItems(
                    items.mapIndexed { currentIndex, item ->
                        if (currentIndex == index) {
                            item.transform()
                        } else {
                            item
                        }
                    },
                ),
            )
        }
    }

    private fun loadClubs() {
        val foundation = dataFoundation ?: return
        viewModelScope.launch {
            try {
                val catalog = foundation.getClubCatalogUseCase()
                val enabled = foundation.getEnabledClubsUseCase()
                _uiState.value = _uiState.value.copy(
                    clubCatalog = catalog,
                    enabledClubCodes = enabled,
                )
            } catch (e: Exception) {
                // Club catalog failures are non-fatal; picker will show empty options
            }
        }
    }

    companion object {
        fun factory(
            environment: AppEnvironment,
            dataFoundation: DataFoundation?,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(PracticePlannerViewModel::class.java)) {
                    "Unsupported ViewModel class: ${modelClass.name}"
                }

                return PracticePlannerViewModel(
                    environment = environment,
                    dataFoundation = dataFoundation,
                ) as T
            }
        }
    }
}

private fun plannerStatus(
    exception: Throwable,
    fallback: String,
): PlannerStatus = if (exception.isPlanningAccessError()) {
    PlannerStatus.SchemaNotReady
} else {
    PlannerStatus.Notification(fallback)
}

private fun Throwable.isForeignKeyViolation(): Boolean {
    val msg = message?.lowercase() ?: return false
    return msg.contains("foreign key") || msg.contains("violates foreign key constraint")
}

private fun Throwable.isPlanningAccessError(): Boolean {
    val message = message?.lowercase() ?: return false
    return when {
        message.contains("could not find the table") -> {
            planningSchemaTables.any { tableName ->
                message.contains("public.$tableName")
            }
        }

        message.contains("permission denied for table") -> {
            planningSchemaTables.any { tableName ->
                message.contains("permission denied for table $tableName")
            }
        }

        else -> false
    }
}

private val planningSchemaTables = setOf(
    "practice_units",
    "practice_unit_instructions",
    "practice_sessions",
    "practice_session_items",
    "user_preferences",
    "clubs",
    "user_enabled_clubs",
)

private fun PracticeUnitEditorState.resolveWith(
    units: List<PracticeUnit>,
): PracticeUnitEditorState = unitId?.let { currentUnitId ->
    units.firstOrNull { unit -> unit.id == currentUnitId }?.toEditorState() ?: PracticeUnitEditorState()
} ?: this

private fun PracticeSessionEditorState.resolveWith(
    sessions: List<PracticeSession>,
): PracticeSessionEditorState = sessionId?.let { currentSessionId ->
    sessions.firstOrNull { session -> session.id == currentSessionId }?.toEditorState() ?: PracticeSessionEditorState()
} ?: this

private fun PracticeUnit.toEditorState(): PracticeUnitEditorState = PracticeUnitEditorState(
    unitId = id,
    title = title,
    notes = notes.orEmpty(),
    focus = focus.orEmpty(),
    defaultClubReference = defaultClubReference.orEmpty(),
    instructions = if (instructions.isEmpty()) {
        listOf(PracticeInstructionEditorState(order = 1))
    } else {
        instructions.map { instruction -> instruction.toEditorState() }
    },
)

private fun PracticeInstruction.toEditorState(): PracticeInstructionEditorState = PracticeInstructionEditorState(
    order = order,
    text = text,
    ballCount = ballCount?.toString().orEmpty(),
)

private fun PracticeSession.toEditorState(): PracticeSessionEditorState = PracticeSessionEditorState(
    sessionId = id,
    name = name,
    notes = notes.orEmpty(),
    items = items.map { item -> item.toEditorState() },
)

private fun PracticeSessionItem.toEditorState(): PracticeSessionItemEditorState = PracticeSessionItemEditorState(
    order = order,
    practiceUnitId = practiceUnitId,
    repeatCount = repeatCount.toString(),
    clubReference = clubReference.orEmpty(),
    notes = notes.orEmpty(),
    focusCue = focusCue.orEmpty(),
)

private fun PracticeUnitEditorState.toDraft(): PracticeUnitDraft = PracticeUnitDraft(
    title = title,
    instructions = instructions.map { instruction ->
        PracticeInstructionDraft(
            order = instruction.order,
            text = instruction.text,
            ballCount = instruction.ballCount.parseOptionalInt("Instruction ball count"),
        )
    },
    notes = notes,
    focus = focus,
    defaultClubReference = defaultClubReference,
)

private fun PracticeSessionEditorState.toDraft(): PracticeSessionDraft = PracticeSessionDraft(
    name = name,
    notes = notes,
    items = items.map { item ->
        PracticeSessionItemDraft(
            practiceUnitId = item.practiceUnitId,
            order = item.order,
            repeatCount = item.repeatCount.parseRequiredInt("Repeat count"),
            clubReference = item.clubReference,
            notes = item.notes,
            focusCue = item.focusCue,
        )
    },
)

private fun String.parseOptionalInt(fieldLabel: String): Int? {
    val normalized = trim()
    if (normalized.isEmpty()) {
        return null
    }

    return normalized.toIntOrNull() ?: throw IllegalArgumentException("$fieldLabel must be a whole number.")
}

private fun String.parseRequiredInt(fieldLabel: String): Int {
    val normalized = trim()
    if (normalized.isEmpty()) {
        throw IllegalArgumentException("$fieldLabel is required.")
    }

    return normalized.toIntOrNull() ?: throw IllegalArgumentException("$fieldLabel must be a whole number.")
}

private fun reindexedInstructions(
    items: List<PracticeInstructionEditorState>,
): List<PracticeInstructionEditorState> = items.mapIndexed { index, item ->
    item.copy(order = index + 1)
}

private fun reindexedSessionItems(
    items: List<PracticeSessionItemEditorState>,
): List<PracticeSessionItemEditorState> = items.mapIndexed { index, item ->
    item.copy(order = index + 1)
}

private fun <T> List<T>.moveItem(
    fromIndex: Int,
    toIndex: Int,
): List<T> {
    if (fromIndex !in indices || toIndex !in indices) {
        return this
    }

    val mutable = toMutableList()
    val item = mutable.removeAt(fromIndex)
    mutable.add(toIndex, item)
    return mutable.toList()
}

private val issueIndexRegex = Regex("\\[(\\d+)]")

private fun issueIndex(field: String): Int? =
    issueIndexRegex.find(field)?.groupValues?.get(1)?.toIntOrNull()

private fun PracticeUnitEditorState.withErrors(issues: List<ValidationIssue>): PracticeUnitEditorState {
    var updated = this
    for (issue in issues) {
        val idx = issueIndex(issue.field)
        when {
            issue.field == "title" -> updated = updated.copy(titleError = issue.message)
            idx != null && issue.field.endsWith("].text") ->
                updated = updated.copy(instructions = updated.instructions.mapIndexed { i, instr ->
                    if (i == idx) instr.copy(textError = issue.message) else instr
                })
            idx != null && issue.field.endsWith("].ballCount") ->
                updated = updated.copy(instructions = updated.instructions.mapIndexed { i, instr ->
                    if (i == idx) instr.copy(ballCountError = issue.message) else instr
                })
        }
    }
    return updated
}

private fun PracticeSessionEditorState.withErrors(issues: List<ValidationIssue>): PracticeSessionEditorState {
    var updated = this
    for (issue in issues) {
        val idx = issueIndex(issue.field)
        when {
            issue.field == "name" -> updated = updated.copy(nameError = issue.message)
            idx != null && issue.field.endsWith("].practiceUnitId") ->
                updated = updated.copy(items = updated.items.mapIndexed { i, item ->
                    if (i == idx) item.copy(unitError = issue.message) else item
                })
            idx != null && issue.field.endsWith("].repeatCount") ->
                updated = updated.copy(items = updated.items.mapIndexed { i, item ->
                    if (i == idx) item.copy(repeatCountError = issue.message) else item
                })
        }
    }
    return updated
}
