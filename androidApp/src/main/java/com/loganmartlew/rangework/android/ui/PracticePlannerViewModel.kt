package com.loganmartlew.rangework.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.loganmartlew.rangework.shared.auth.AuthState
import com.loganmartlew.rangework.shared.config.AppEnvironment
import com.loganmartlew.rangework.shared.data.DataFoundation
import com.loganmartlew.rangework.shared.model.PracticeInstruction
import com.loganmartlew.rangework.shared.model.PracticeInstructionDraft
import com.loganmartlew.rangework.shared.model.PracticeSession
import com.loganmartlew.rangework.shared.model.PracticeSessionDraft
import com.loganmartlew.rangework.shared.model.PracticeSessionItem
import com.loganmartlew.rangework.shared.model.PracticeSessionItemDraft
import com.loganmartlew.rangework.shared.model.PracticeUnit
import com.loganmartlew.rangework.shared.model.PracticeUnitDraft
import kotlinx.coroutines.launch

data class PracticeInstructionEditorState(
    val order: Int,
    val text: String = "",
    val repCount: String = "",
    val ballCount: String = "",
)

data class PracticeUnitEditorState(
    val unitId: String? = null,
    val title: String = "",
    val notes: String = "",
    val focus: String = "",
    val defaultClubReference: String = "",
    val instructions: List<PracticeInstructionEditorState> = listOf(
        PracticeInstructionEditorState(order = 1),
    ),
)

data class PracticeSessionItemEditorState(
    val order: Int,
    val practiceUnitId: String = "",
    val repeatCount: String = "1",
    val clubReference: String = "",
    val notes: String = "",
    val focusCue: String = "",
    val restSeconds: String = "",
)

data class PracticeSessionEditorState(
    val sessionId: String? = null,
    val name: String = "",
    val notes: String = "",
    val items: List<PracticeSessionItemEditorState> = emptyList(),
)

data class PracticePlannerUiState(
    val environment: AppEnvironment,
    val dataConfigured: Boolean,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val units: List<PracticeUnit> = emptyList(),
    val sessions: List<PracticeSession> = emptyList(),
    val unitEditor: PracticeUnitEditorState = PracticeUnitEditorState(),
    val sessionEditor: PracticeSessionEditorState = PracticeSessionEditorState(),
    val savedUnitId: String? = null,
    val savedSessionId: String? = null,
    val statusMessage: String? = if (dataConfigured) null else planningUnavailableMessage(environment),
) {
    val isWorking: Boolean
        get() = isLoading || isSaving
}

class PracticePlannerViewModel(
    private val environment: AppEnvironment,
    private val dataFoundation: DataFoundation?,
) : ViewModel() {
    private var activeUserId: String? = null

    private val _uiState = androidx.compose.runtime.mutableStateOf(
        PracticePlannerUiState(
            environment = environment,
            dataConfigured = dataFoundation != null,
        ),
    )
    val uiState: androidx.compose.runtime.State<PracticePlannerUiState> = _uiState

    fun onAuthStateChanged(authState: AuthState) {
        when (authState) {
            is AuthState.SignedIn -> {
                activeUserId = authState.userId
                if (dataFoundation == null) {
                    _uiState.value = _uiState.value.copy(
                        statusMessage = planningUnavailableMessage(environment),
                    )
                    return
                }
                refreshPlanning(
                    statusMessage = if (_uiState.value.units.isEmpty() && _uiState.value.sessions.isEmpty()) {
                        "Planning workspace ready."
                    } else {
                        _uiState.value.statusMessage
                    },
                )
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
                    units = emptyList(),
                    sessions = emptyList(),
                    unitEditor = PracticeUnitEditorState(),
                    sessionEditor = PracticeSessionEditorState(),
                    savedUnitId = null,
                    savedSessionId = null,
                    statusMessage = if (dataFoundation == null) {
                        planningUnavailableMessage(environment)
                    } else {
                        null
                    },
                )
            }
        }
    }

    fun refreshPlanning() {
        refreshPlanning(statusMessage = "Planning data refreshed.")
    }

    fun beginNewUnit() {
        _uiState.value = _uiState.value.copy(
            unitEditor = PracticeUnitEditorState(),
            savedUnitId = null,
            statusMessage = null,
        )
    }

    fun editUnit(unitId: String) {
        val unit = _uiState.value.units.firstOrNull { item -> item.id == unitId } ?: return
        _uiState.value = _uiState.value.copy(
            unitEditor = unit.toEditorState(),
            savedUnitId = null,
            statusMessage = "Editing ${unit.title}.",
        )
    }

    fun consumeSavedUnitId() {
        _uiState.value = _uiState.value.copy(savedUnitId = null)
    }

    fun updateUnitTitle(value: String) = updateUnitEditor { copy(title = value) }

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

    fun updateInstructionText(index: Int, value: String) = updateInstruction(index) { copy(text = value) }

    fun updateInstructionRepCount(index: Int, value: String) = updateInstruction(index) { copy(repCount = value) }

    fun updateInstructionBallCount(index: Int, value: String) = updateInstruction(index) {
        copy(ballCount = value)
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

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSaving = true,
                savedUnitId = null,
                statusMessage = null,
            )

            try {
                val draft = _uiState.value.unitEditor.toDraft()
                val savedUnit = foundation.savePracticeUnitUseCase(
                    draft = draft,
                    unitId = _uiState.value.unitEditor.unitId,
                )
                val units = foundation.listPracticeUnitsUseCase()
                val sessions = foundation.listPracticeSessionsUseCase()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSaving = false,
                    units = units,
                    sessions = sessions,
                    unitEditor = savedUnit.toEditorState(),
                    sessionEditor = _uiState.value.sessionEditor.resolveWith(sessions),
                    savedUnitId = savedUnit.id,
                    statusMessage = "Saved ${savedUnit.title}.",
                )
            } catch (exception: IllegalArgumentException) {
                markSaveFailure(exception, "Unit save failed.")
            } catch (exception: IllegalStateException) {
                markSaveFailure(exception, "Unit save failed.")
            } catch (exception: Exception) {
                markSaveFailure(exception, "Unit save failed.")
            }
        }
    }

    fun deleteUnit(unitId: String) {
        val foundation = dataFoundation ?: return markPlannerUnavailable()
        if (activeUserId == null) {
            markSignedOut()
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, savedUnitId = null)
            try {
                val title = _uiState.value.units.firstOrNull { unit -> unit.id == unitId }?.title ?: "unit"
                foundation.deletePracticeUnitUseCase(unitId)
                val units = foundation.listPracticeUnitsUseCase()
                val sessions = foundation.listPracticeSessionsUseCase()
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
                    statusMessage = "Deleted $title.",
                )
            } catch (exception: IllegalArgumentException) {
                markSaveFailure(exception, "Unit delete failed.")
            } catch (exception: IllegalStateException) {
                markSaveFailure(exception, "Unit delete failed.")
            } catch (exception: Exception) {
                markSaveFailure(exception, "Unit delete failed.")
            }
        }
    }

    fun beginNewSession() {
        _uiState.value = _uiState.value.copy(
            sessionEditor = PracticeSessionEditorState(),
            savedSessionId = null,
            statusMessage = null,
        )
    }

    fun editSession(sessionId: String) {
        val session = _uiState.value.sessions.firstOrNull { item -> item.id == sessionId } ?: return
        _uiState.value = _uiState.value.copy(
            sessionEditor = session.toEditorState(),
            savedSessionId = null,
            statusMessage = "Editing ${session.name}.",
        )
    }

    fun consumeSavedSessionId() {
        _uiState.value = _uiState.value.copy(savedSessionId = null)
    }

    fun updateSessionName(value: String) = updateSessionEditor { copy(name = value) }

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
        copy(practiceUnitId = practiceUnitId)
    }

    fun updateSessionItemRepeatCount(index: Int, value: String) = updateSessionItem(index) {
        copy(repeatCount = value)
    }

    fun updateSessionItemClubReference(index: Int, value: String) = updateSessionItem(index) {
        copy(clubReference = value)
    }

    fun updateSessionItemNotes(index: Int, value: String) = updateSessionItem(index) { copy(notes = value) }

    fun updateSessionItemFocusCue(index: Int, value: String) = updateSessionItem(index) { copy(focusCue = value) }

    fun updateSessionItemRestSeconds(index: Int, value: String) = updateSessionItem(index) {
        copy(restSeconds = value)
    }

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

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(
                isSaving = true,
                savedSessionId = null,
                statusMessage = null,
            )

            try {
                val draft = _uiState.value.sessionEditor.toDraft()
                val savedSession = foundation.savePracticeSessionUseCase(
                    draft = draft,
                    sessionId = _uiState.value.sessionEditor.sessionId,
                )
                val units = foundation.listPracticeUnitsUseCase()
                val sessions = foundation.listPracticeSessionsUseCase()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSaving = false,
                    units = units,
                    sessions = sessions,
                    unitEditor = _uiState.value.unitEditor.resolveWith(units),
                    sessionEditor = savedSession.toEditorState(),
                    savedSessionId = savedSession.id,
                    statusMessage = "Saved ${savedSession.name}.",
                )
            } catch (exception: IllegalArgumentException) {
                markSaveFailure(exception, "Session save failed.")
            } catch (exception: IllegalStateException) {
                markSaveFailure(exception, "Session save failed.")
            } catch (exception: Exception) {
                markSaveFailure(exception, "Session save failed.")
            }
        }
    }

    fun deleteSession(sessionId: String) {
        val foundation = dataFoundation ?: return markPlannerUnavailable()
        if (activeUserId == null) {
            markSignedOut()
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, savedSessionId = null)
            try {
                val name = _uiState.value.sessions.firstOrNull { session -> session.id == sessionId }?.name ?: "session"
                foundation.deletePracticeSessionUseCase(sessionId)
                val units = foundation.listPracticeUnitsUseCase()
                val sessions = foundation.listPracticeSessionsUseCase()
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
                    statusMessage = "Deleted $name.",
                )
            } catch (exception: IllegalArgumentException) {
                markSaveFailure(exception, "Session delete failed.")
            } catch (exception: IllegalStateException) {
                markSaveFailure(exception, "Session delete failed.")
            } catch (exception: Exception) {
                markSaveFailure(exception, "Session delete failed.")
            }
        }
    }

    private fun refreshPlanning(statusMessage: String?) {
        val foundation = dataFoundation ?: return markPlannerUnavailable()
        if (activeUserId == null) {
            markSignedOut()
            return
        }

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val units = foundation.listPracticeUnitsUseCase()
                val sessions = foundation.listPracticeSessionsUseCase()
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSaving = false,
                    units = units,
                    sessions = sessions,
                    unitEditor = _uiState.value.unitEditor.resolveWith(units),
                    sessionEditor = _uiState.value.sessionEditor.resolveWith(sessions),
                    statusMessage = statusMessage,
                )
            } catch (exception: IllegalArgumentException) {
                markRefreshFailure(exception, "Planning refresh failed.")
            } catch (exception: IllegalStateException) {
                markRefreshFailure(exception, "Planning refresh failed.")
            } catch (exception: Exception) {
                markRefreshFailure(exception, "Planning refresh failed.")
            }
        }
    }

    private fun markPlannerUnavailable() {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isSaving = false,
            statusMessage = planningUnavailableMessage(environment),
        )
    }

    private fun markSignedOut() {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isSaving = false,
            statusMessage = "Sign in before changing practice plans.",
        )
    }

    private fun markRefreshFailure(exception: Throwable, fallback: String) {
        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isSaving = false,
            statusMessage = plannerStatusMessage(
                exception = exception,
                fallback = fallback,
            ),
        )
    }

    private fun markSaveFailure(exception: Throwable, fallback: String) {
        _uiState.value = _uiState.value.copy(
            isSaving = false,
            statusMessage = plannerStatusMessage(
                exception = exception,
                fallback = fallback,
            ),
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

internal fun planningUnavailableMessage(environment: AppEnvironment): String =
    "Practice planning is not available in this build yet."

internal fun planningSchemaUnavailableMessage(): String =
    "Practice planning is still being prepared for this workspace. Refresh once setup is complete."

private fun plannerStatusMessage(
    exception: Throwable,
    fallback: String,
): String = if (exception.isPlanningAccessError()) {
    planningSchemaUnavailableMessage()
} else {
    exception.message ?: fallback
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
    repCount = repCount?.toString().orEmpty(),
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
    restSeconds = restSeconds?.toString().orEmpty(),
)

private fun PracticeUnitEditorState.toDraft(): PracticeUnitDraft = PracticeUnitDraft(
    title = title,
    instructions = instructions.map { instruction ->
        PracticeInstructionDraft(
            order = instruction.order,
            text = instruction.text,
            repCount = instruction.repCount.parseOptionalInt("Instruction reps"),
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
            restSeconds = item.restSeconds.parseOptionalInt("Rest seconds"),
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
