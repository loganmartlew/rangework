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
    val clubReference: String = "",
    val repCount: String = "",
    val ballCount: String = "",
)

data class PracticeUnitEditorState(
    val unitId: String? = null,
    val title: String = "",
    val notes: String = "",
    val focus: String = "",
    val defaultClubReference: String = "",
    val tags: String = "",
    val defaultBallCount: String = "",
    val instructions: List<PracticeInstructionEditorState> = listOf(
        PracticeInstructionEditorState(order = 1),
    ),
)

data class PracticeSessionItemEditorState(
    val order: Int,
    val practiceUnitId: String = "",
    val notes: String = "",
    val focusCue: String = "",
    val restSeconds: String = "",
    val overrideBallCount: String = "",
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
    val selectedUnitId: String? = null,
    val selectedSessionId: String? = null,
    val unitEditor: PracticeUnitEditorState = PracticeUnitEditorState(),
    val sessionEditor: PracticeSessionEditorState = PracticeSessionEditorState(),
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
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                )
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
                    selectedUnitId = null,
                    selectedSessionId = null,
                    unitEditor = PracticeUnitEditorState(),
                    sessionEditor = PracticeSessionEditorState(),
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
            selectedUnitId = null,
            unitEditor = PracticeUnitEditorState(),
            statusMessage = null,
        )
    }

    fun editUnit(unitId: String) {
        val unit = _uiState.value.units.firstOrNull { item -> item.id == unitId } ?: return
        _uiState.value = _uiState.value.copy(
            selectedUnitId = unit.id,
            unitEditor = unit.toEditorState(),
            statusMessage = "Editing ${unit.title}.",
        )
    }

    fun updateUnitTitle(value: String) = updateUnitEditor { copy(title = value) }

    fun updateUnitNotes(value: String) = updateUnitEditor { copy(notes = value) }

    fun updateUnitFocus(value: String) = updateUnitEditor { copy(focus = value) }

    fun updateUnitDefaultClubReference(value: String) = updateUnitEditor { copy(defaultClubReference = value) }

    fun updateUnitTags(value: String) = updateUnitEditor { copy(tags = value) }

    fun updateUnitDefaultBallCount(value: String) = updateUnitEditor { copy(defaultBallCount = value) }

    fun addInstruction() = updateUnitEditor {
        copy(
            instructions = reindexedInstructions(
                instructions + PracticeInstructionEditorState(order = instructions.size + 1),
            ),
        )
    }

    fun updateInstructionText(index: Int, value: String) = updateInstruction(index) { copy(text = value) }

    fun updateInstructionClubReference(index: Int, value: String) = updateInstruction(index) {
        copy(clubReference = value)
    }

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
                statusMessage = null,
            )

            try {
                val savedUnit = foundation.savePracticeUnitUseCase(
                    draft = _uiState.value.unitEditor.toDraft(),
                    unitId = _uiState.value.selectedUnitId,
                )
                refreshPlanning(
                    statusMessage = "Saved ${savedUnit.title}.",
                    preferredUnitId = savedUnit.id,
                    preferredSessionId = _uiState.value.selectedSessionId,
                )
            } catch (exception: IllegalArgumentException) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    statusMessage = plannerStatusMessage(
                        exception = exception,
                        fallback = "Unit save failed.",
                    ),
                )
            } catch (exception: IllegalStateException) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    statusMessage = plannerStatusMessage(
                        exception = exception,
                        fallback = "Unit save failed.",
                    ),
                )
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    statusMessage = plannerStatusMessage(
                        exception = exception,
                        fallback = "Unit save failed.",
                    ),
                )
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
            _uiState.value = _uiState.value.copy(isSaving = true)
            try {
                val title = _uiState.value.units.firstOrNull { unit -> unit.id == unitId }?.title ?: "unit"
                foundation.deletePracticeUnitUseCase(unitId)
                refreshPlanning(
                    statusMessage = "Deleted $title.",
                    preferredUnitId = if (_uiState.value.selectedUnitId == unitId) null else _uiState.value.selectedUnitId,
                    preferredSessionId = _uiState.value.selectedSessionId,
                )
            } catch (exception: IllegalArgumentException) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    statusMessage = plannerStatusMessage(
                        exception = exception,
                        fallback = "Unit delete failed.",
                    ),
                )
            } catch (exception: IllegalStateException) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    statusMessage = plannerStatusMessage(
                        exception = exception,
                        fallback = "Unit delete failed.",
                    ),
                )
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    statusMessage = plannerStatusMessage(
                        exception = exception,
                        fallback = "Unit delete failed.",
                    ),
                )
            }
        }
    }

    fun beginNewSession() {
        _uiState.value = _uiState.value.copy(
            selectedSessionId = null,
            sessionEditor = PracticeSessionEditorState(),
            statusMessage = null,
        )
    }

    fun editSession(sessionId: String) {
        val session = _uiState.value.sessions.firstOrNull { item -> item.id == sessionId } ?: return
        _uiState.value = _uiState.value.copy(
            selectedSessionId = session.id,
            sessionEditor = session.toEditorState(),
            statusMessage = "Editing ${session.name}.",
        )
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
                ),
            ),
        )
    }

    fun updateSessionItemUnit(index: Int, practiceUnitId: String) = updateSessionItem(index) {
        copy(practiceUnitId = practiceUnitId)
    }

    fun updateSessionItemNotes(index: Int, value: String) = updateSessionItem(index) { copy(notes = value) }

    fun updateSessionItemFocusCue(index: Int, value: String) = updateSessionItem(index) { copy(focusCue = value) }

    fun updateSessionItemRestSeconds(index: Int, value: String) = updateSessionItem(index) {
        copy(restSeconds = value)
    }

    fun updateSessionItemOverrideBallCount(index: Int, value: String) = updateSessionItem(index) {
        copy(overrideBallCount = value)
    }

    fun moveSessionItemUp(index: Int) = moveSessionItem(index, index - 1)

    fun moveSessionItemDown(index: Int) = moveSessionItem(index, index + 1)

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
                statusMessage = null,
            )

            try {
                val savedSession = foundation.savePracticeSessionUseCase(
                    draft = _uiState.value.sessionEditor.toDraft(),
                    sessionId = _uiState.value.selectedSessionId,
                )
                refreshPlanning(
                    statusMessage = "Saved ${savedSession.name}.",
                    preferredUnitId = _uiState.value.selectedUnitId,
                    preferredSessionId = savedSession.id,
                )
            } catch (exception: IllegalArgumentException) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    statusMessage = plannerStatusMessage(
                        exception = exception,
                        fallback = "Session save failed.",
                    ),
                )
            } catch (exception: IllegalStateException) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    statusMessage = plannerStatusMessage(
                        exception = exception,
                        fallback = "Session save failed.",
                    ),
                )
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    statusMessage = plannerStatusMessage(
                        exception = exception,
                        fallback = "Session save failed.",
                    ),
                )
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
            _uiState.value = _uiState.value.copy(isSaving = true)
            try {
                val name = _uiState.value.sessions.firstOrNull { session -> session.id == sessionId }?.name ?: "session"
                foundation.deletePracticeSessionUseCase(sessionId)
                refreshPlanning(
                    statusMessage = "Deleted $name.",
                    preferredUnitId = _uiState.value.selectedUnitId,
                    preferredSessionId = if (_uiState.value.selectedSessionId == sessionId) null else _uiState.value.selectedSessionId,
                )
            } catch (exception: IllegalArgumentException) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    statusMessage = plannerStatusMessage(
                        exception = exception,
                        fallback = "Session delete failed.",
                    ),
                )
            } catch (exception: IllegalStateException) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    statusMessage = plannerStatusMessage(
                        exception = exception,
                        fallback = "Session delete failed.",
                    ),
                )
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    statusMessage = plannerStatusMessage(
                        exception = exception,
                        fallback = "Session delete failed.",
                    ),
                )
            }
        }
    }

    private fun refreshPlanning(
        statusMessage: String?,
        preferredUnitId: String? = _uiState.value.selectedUnitId,
        preferredSessionId: String? = _uiState.value.selectedSessionId,
    ) {
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
                val resolvedUnit = preferredUnitId?.let { selectedId ->
                    units.firstOrNull { unit -> unit.id == selectedId }
                }
                val resolvedSession = preferredSessionId?.let { selectedId ->
                    sessions.firstOrNull { session -> session.id == selectedId }
                }

                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSaving = false,
                    units = units,
                    sessions = sessions,
                    selectedUnitId = resolvedUnit?.id,
                    selectedSessionId = resolvedSession?.id,
                    unitEditor = resolvedUnit?.toEditorState() ?: _uiState.value.unitEditor.takeIf {
                        preferredUnitId == null
                    } ?: PracticeUnitEditorState(),
                    sessionEditor = resolvedSession?.toEditorState() ?: _uiState.value.sessionEditor.takeIf {
                        preferredSessionId == null
                    } ?: PracticeSessionEditorState(),
                    statusMessage = statusMessage,
                )
            } catch (exception: IllegalArgumentException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSaving = false,
                    statusMessage = plannerStatusMessage(
                        exception = exception,
                        fallback = "Planning refresh failed.",
                    ),
                )
            } catch (exception: IllegalStateException) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSaving = false,
                    statusMessage = plannerStatusMessage(
                        exception = exception,
                        fallback = "Planning refresh failed.",
                    ),
                )
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSaving = false,
                    statusMessage = plannerStatusMessage(
                        exception = exception,
                        fallback = "Planning refresh failed.",
                    ),
                )
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

    private fun updateUnitEditor(
        transform: PracticeUnitEditorState.() -> PracticeUnitEditorState,
    ) {
        _uiState.value = _uiState.value.copy(
            unitEditor = _uiState.value.unitEditor.transform(),
        )
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

    private fun moveInstruction(
        fromIndex: Int,
        toIndex: Int,
    ) {
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
        _uiState.value = _uiState.value.copy(
            sessionEditor = _uiState.value.sessionEditor.transform(),
        )
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

    private fun moveSessionItem(
        fromIndex: Int,
        toIndex: Int,
    ) {
        updateSessionEditor {
            copy(
                items = reindexedSessionItems(
                    items.moveItem(fromIndex, toIndex),
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
    "Planning data needs Supabase configuration. ${missingConfigMessage(environment)}"

internal fun planningSchemaUnavailableMessage(): String =
    "Practice planning tables are not available in this Supabase project yet. Apply the Phase 3 planning-data migration, then refresh the app."

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

private fun PracticeUnit.toEditorState(): PracticeUnitEditorState = PracticeUnitEditorState(
    unitId = id,
    title = title,
    notes = notes.orEmpty(),
    focus = focus.orEmpty(),
    defaultClubReference = defaultClubReference.orEmpty(),
    tags = tags.joinToString(", "),
    defaultBallCount = defaultBallCount?.toString().orEmpty(),
    instructions = if (instructions.isEmpty()) {
        listOf(
            PracticeInstructionEditorState(order = 1),
        )
    } else {
        instructions.map { instruction ->
            instruction.toEditorState()
        }
    },
)

private fun PracticeInstruction.toEditorState(): PracticeInstructionEditorState = PracticeInstructionEditorState(
    order = order,
    text = text,
    clubReference = clubReference.orEmpty(),
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
    notes = notes.orEmpty(),
    focusCue = focusCue.orEmpty(),
    restSeconds = restSeconds?.toString().orEmpty(),
    overrideBallCount = overrideBallCount?.toString().orEmpty(),
)

private fun PracticeUnitEditorState.toDraft(): PracticeUnitDraft = PracticeUnitDraft(
    title = title,
    instructions = instructions.map { instruction ->
        PracticeInstructionDraft(
            order = instruction.order,
            text = instruction.text,
            clubReference = instruction.clubReference,
            repCount = instruction.repCount.parseOptionalInt("Instruction reps"),
            ballCount = instruction.ballCount.parseOptionalInt("Instruction ball count"),
        )
    },
    notes = notes,
    focus = focus,
    defaultClubReference = defaultClubReference,
    tags = tags.split(",").map(String::trim).filter(String::isNotEmpty),
    defaultBallCount = defaultBallCount.parseOptionalInt("Default ball count"),
)

private fun PracticeSessionEditorState.toDraft(): PracticeSessionDraft = PracticeSessionDraft(
    name = name,
    notes = notes,
    items = items.map { item ->
        PracticeSessionItemDraft(
            practiceUnitId = item.practiceUnitId,
            order = item.order,
            notes = item.notes,
            focusCue = item.focusCue,
            restSeconds = item.restSeconds.parseOptionalInt("Rest seconds"),
            overrideBallCount = item.overrideBallCount.parseOptionalInt("Override ball count"),
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
