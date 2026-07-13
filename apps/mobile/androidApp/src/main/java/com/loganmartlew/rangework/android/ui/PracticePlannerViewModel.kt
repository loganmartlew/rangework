package com.loganmartlew.rangework.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.loganmartlew.rangework.shared.auth.AuthState
import com.loganmartlew.rangework.shared.config.AppEnvironment
import com.loganmartlew.rangework.shared.data.DataFoundation
import com.loganmartlew.rangework.shared.library.PracticeLibrary
import com.loganmartlew.rangework.shared.library.PracticeLibraryResult
import com.loganmartlew.rangework.shared.library.editor.DraftReview
import com.loganmartlew.rangework.shared.library.editor.PracticeDraftEditor
import com.loganmartlew.rangework.shared.library.editor.PracticeInstructionDraftInput
import com.loganmartlew.rangework.shared.library.editor.PracticeSessionDraftInput
import com.loganmartlew.rangework.shared.library.editor.PracticeSessionItemDraftInput
import com.loganmartlew.rangework.shared.library.editor.PracticeUnitDraftInput
import com.loganmartlew.rangework.shared.model.ActiveRangeSessionSummary
import com.loganmartlew.rangework.shared.model.Club
import com.loganmartlew.rangework.shared.model.CompletedRangeSessionSummary
import com.loganmartlew.rangework.shared.model.PracticeInstruction
import com.loganmartlew.rangework.shared.model.PracticeSession
import com.loganmartlew.rangework.shared.model.PracticeSessionDraft
import com.loganmartlew.rangework.shared.model.PracticeSessionItem
import com.loganmartlew.rangework.shared.model.PracticeUnit
import com.loganmartlew.rangework.shared.model.PracticeUnitDraft
import com.loganmartlew.rangework.shared.model.EnabledClubCount
import com.loganmartlew.rangework.shared.model.NextMoveState
import com.loganmartlew.rangework.shared.model.ObservationType
import com.loganmartlew.rangework.shared.model.derivedBallCount
import com.loganmartlew.rangework.shared.model.RecentItem
import com.loganmartlew.rangework.shared.model.Tag
import com.loganmartlew.rangework.shared.model.MAX_TAGS_PER_ITEM
import com.loganmartlew.rangework.shared.model.ValidationIssue
import com.loganmartlew.rangework.shared.model.filteredByAnyTag
import com.loganmartlew.rangework.shared.model.recentItems
import com.loganmartlew.rangework.shared.model.resolveNextMoveState
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

/**
 * Typealiases to match the original Android editor state names.
 * Compose screens and ViewModel-internal operations (add/remove/reorder/reindex)
 * are unchanged — they resolve through the alias.
 */
typealias PracticeInstructionEditorState = PracticeInstructionDraftInput
typealias PracticeUnitEditorState = PracticeUnitDraftInput
typealias PracticeSessionItemEditorState = PracticeSessionItemDraftInput
typealias PracticeSessionEditorState = PracticeSessionDraftInput

data class PracticePlannerUiState(
    val environment: AppEnvironment,
    val dataConfigured: Boolean,
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val hasLoaded: Boolean = false,
    val units: List<PracticeUnit> = emptyList(),
    val inlineUnits: List<PracticeUnit> = emptyList(),
    val sessions: List<PracticeSession> = emptyList(),
    val archivedSessions: List<PracticeSession> = emptyList(),
    val clubCatalog: List<Club> = emptyList(),
    val enabledClubCodes: Set<String> = emptySet(),
    val availableTags: List<Tag> = emptyList(),
    val unitTagFilter: Set<String> = emptySet(),
    val sessionTagFilter: Set<String> = emptySet(),
    val unitEditor: PracticeUnitEditorState = PracticeUnitEditorState(),
    val sessionEditor: PracticeSessionEditorState = PracticeSessionEditorState(),
    val unitEditorBaseline: PracticeUnitEditorState? = null,
    val sessionEditorBaseline: PracticeSessionEditorState? = null,
    val savedUnitId: String? = null,
    val savedSessionId: String? = null,
    val duplicatedUnitId: String? = null,
    val duplicatedSessionId: String? = null,
    val startedRangeSessionId: String? = null,
    val activeRangeSessions: List<ActiveRangeSessionSummary> = emptyList(),
    val completedRangeSessionHistory: Map<String, List<CompletedRangeSessionSummary>> = emptyMap(),
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

    val filteredUnits: List<PracticeUnit>
        get() = units.filteredByAnyTag(unitTagFilter) { unit -> unit.tags.map(Tag::id) }

    val filteredSessions: List<PracticeSession>
        get() = sessions.filteredByAnyTag(sessionTagFilter) { session -> session.tags.map(Tag::id) }

    val recentItems: List<RecentItem>
        get() = recentItems(units, sessions)

    val nextMoveState: NextMoveState
        get() = resolveNextMoveState(units, sessions, savedUnitId, savedSessionId)
}

val PracticePlannerUiState.allSessions: List<PracticeSession>
    get() = sessions + archivedSessions

fun PracticePlannerUiState.findSession(id: String): PracticeSession? =
    allSessions.firstOrNull { session -> session.id == id }

val PracticePlannerUiState.allUnits: List<PracticeUnit>
    get() = units + inlineUnits

fun PracticePlannerUiState.findUnit(id: String): PracticeUnit? =
    allUnits.firstOrNull { unit -> unit.id == id }

class PracticePlannerViewModel(
    private val environment: AppEnvironment,
    // The planner spans four of the six aggregate seams (Practice Unit, Practice Session,
    // Club, Range Session) behind a single all-or-nothing availability gate: DataFoundation
    // is either fully wired or null as a whole. It deliberately takes the whole holder rather
    // than four lockstep-nullable seams, which would encode states (e.g. Units present, Clubs
    // absent) that can never occur. The other ViewModels narrow to their seam(s) because they
    // touch only one or two; see issue #16.
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
        val previousUserId = activeUserId
        val previousState = _uiState.value
        when (authState) {
            is AuthState.SignedIn -> {
                activeUserId = authState.userId
                val needsFreshLoad = previousUserId != authState.userId || !previousState.hasLoaded
                val refreshStatus = if (previousState.units.isEmpty() && previousState.sessions.isEmpty()) {
                    PlannerStatus.Info("Planning workspace ready.")
                } else {
                    previousState.status
                }
                if (needsFreshLoad) {
                    operationToken += 1
                    _uiState.value = previousState.copy(
                        isLoading = true,
                        isSaving = false,
                        hasLoaded = false,
                        units = emptyList(),
                        inlineUnits = emptyList(),
                        sessions = emptyList(),
                        archivedSessions = emptyList(),
                        clubCatalog = emptyList(),
                        enabledClubCodes = emptySet(),
                        availableTags = emptyList(),
                        unitTagFilter = emptySet(),
                        sessionTagFilter = emptySet(),
                        unitEditor = PracticeUnitEditorState(),
                        sessionEditor = PracticeSessionEditorState(),
                        unitEditorBaseline = null,
                        sessionEditorBaseline = null,
                        savedUnitId = null,
                        savedSessionId = null,
                        duplicatedUnitId = null,
                        duplicatedSessionId = null,
                        startedRangeSessionId = null,
                        activeRangeSessions = emptyList(),
                        completedRangeSessionHistory = emptyMap(),
                        status = null,
                    )
                }
                if (dataFoundation == null) {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        isSaving = false,
                        hasLoaded = true,
                        status = PlannerStatus.Unavailable,
                    )
                    return
                }
                refreshPlanning(status = refreshStatus)
                loadClubs()
                loadTags()
            }

            AuthState.Restoring -> {
                operationToken += 1
                _uiState.value = _uiState.value.copy(
                    isLoading = true,
                    isSaving = false,
                    hasLoaded = false,
                )
            }

            AuthState.SignedOut,
            is AuthState.Error,
            -> {
                operationToken += 1
                activeUserId = null
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSaving = false,
                    hasLoaded = false,
                    units = emptyList(),
                    inlineUnits = emptyList(),
                    sessions = emptyList(),
                    archivedSessions = emptyList(),
                    clubCatalog = emptyList(),
                    enabledClubCodes = emptySet(),
                    availableTags = emptyList(),
                    unitTagFilter = emptySet(),
                    sessionTagFilter = emptySet(),
                    unitEditor = PracticeUnitEditorState(),
                    sessionEditor = PracticeSessionEditorState(),
                    unitEditorBaseline = null,
                    sessionEditorBaseline = null,
                    savedUnitId = null,
                    savedSessionId = null,
                    duplicatedUnitId = null,
                    duplicatedSessionId = null,
                    startedRangeSessionId = null,
                    activeRangeSessions = emptyList(),
                    completedRangeSessionHistory = emptyMap(),
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
        val unit = _uiState.value.findUnit(unitId) ?: return
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

    fun updateUnitDefaultClubCode(value: String) = updateUnitEditor { copy(defaultClubCode = value) }

    fun updateUnitSuccessCriterion(value: String) = updateUnitEditor { copy(successCriterion = value) }

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

    fun updateInstructionBallCount(index: Int, value: Int) = updateInstructionBallCount(index, value.toString())

    fun updateInstructionClubCode(index: Int, value: String) = updateInstruction(index) { copy(clubCode = value) }

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

    @OptIn(ExperimentalUuidApi::class)
    fun saveUnit() {
        val foundation = dataFoundation ?: return markPlannerUnavailable()
        if (activeUserId == null) {
            markSignedOut()
            return
        }

        val editor = _uiState.value.unitEditor
        when (val review = PracticeDraftEditor.reviewUnit(editor)) {
            is DraftReview.Invalid -> {
                _uiState.value = _uiState.value.copy(
                    unitEditor = review.input,
                    status = PlannerStatus.Notification(review.issues.joinToString(" ") { it.message }),
                )
                return
            }
            is DraftReview.Valid -> {
                val draft = review.draft
                val resolvedUnitId = editor.unitId ?: Uuid.random().toString()
                // Editing an existing Inline Unit must keep it out of `units` and stay
                // inline through save (D4) — the save reconcile branches on this.
                val previousScopedSessionId = editor.unitId?.let { _uiState.value.findUnit(it)?.scopedToSessionId }
                val isInlineEdit = previousScopedSessionId != null
                val previousUnits = _uiState.value.units
                val previousInlineUnits = _uiState.value.inlineUnits
                val previousSessions = _uiState.value.sessions
                val now = Clock.System.now()
                val optimisticUnit = buildOptimisticUnit(resolvedUnitId, draft, now)
                    .copy(scopedToSessionId = previousScopedSessionId)
                val optimisticUnits: List<PracticeUnit>
                val optimisticInlineUnits: List<PracticeUnit>
                if (isInlineEdit) {
                    optimisticUnits = previousUnits
                    optimisticInlineUnits = previousInlineUnits.map { if (it.id == resolvedUnitId) optimisticUnit else it }
                } else {
                    optimisticUnits = if (editor.unitId != null) {
                        previousUnits.map { if (it.id == resolvedUnitId) optimisticUnit else it }
                    } else {
                        previousUnits + optimisticUnit
                    }
                    optimisticInlineUnits = previousInlineUnits
                }

                _uiState.value = _uiState.value.copy(
                    units = optimisticUnits,
                    inlineUnits = optimisticInlineUnits,
                    unitEditor = optimisticUnit.toEditorState(),
                    unitEditorBaseline = null,
                    savedUnitId = resolvedUnitId,
                    status = PlannerStatus.Notification("Saved ${draft.title}."),
                )

                val library = foundation.practiceLibrary
                val token = ++operationToken
                viewModelScope.launch {
                    operationMutex.withLock {
                        try {
                            when (val result = library.saveUnit(draft = draft, unitId = resolvedUnitId)) {
                                is PracticeLibraryResult.Saved -> {
                                    val units = library.listUnits()
                                    val sessions = library.listSessions()
                                    val archivedSessions = library.listArchivedSessions()
                                    val inlineUnits = hydrateInlineUnits(library, units, sessions + archivedSessions)
                                    if (token == operationToken) {
                                        _uiState.value = _uiState.value.copy(
                                            isLoading = false,
                                            units = units,
                                            sessions = sessions,
                                            archivedSessions = archivedSessions,
                                            inlineUnits = inlineUnits,
                                            unitEditor = _uiState.value.unitEditor.resolveWith(units + inlineUnits),
                                        )
                                    }
                                }
                                is PracticeLibraryResult.Invalid -> {
                                    if (token == operationToken) {
                                        _uiState.value = _uiState.value.copy(
                                            units = previousUnits,
                                            inlineUnits = previousInlineUnits,
                                            sessions = previousSessions,
                                            unitEditor = PracticeDraftEditor.placeUnitErrors(
                                                _uiState.value.unitEditor, result.issues,
                                            ),
                                            status = PlannerStatus.Notification(
                                                result.issues.joinToString(" ") { it.message },
                                            ),
                                        )
                                    }
                                }
                            }
                        } catch (exception: Exception) {
                            if (token == operationToken) {
                                _uiState.value = _uiState.value.copy(
                                    units = previousUnits,
                                    inlineUnits = previousInlineUnits,
                                    sessions = previousSessions,
                                    unitEditor = _uiState.value.unitEditor.resolveWith(previousUnits + previousInlineUnits),
                                    status = plannerStatus(exception = exception, fallback = "Unit save failed."),
                                )
                            }
                        }
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

        val previousUnits = _uiState.value.units
        val previousSessions = _uiState.value.sessions
        val title = previousUnits.firstOrNull { it.id == unitId }?.title ?: "unit"
        val optimisticUnits = previousUnits.filter { it.id != unitId }

        _uiState.value = _uiState.value.copy(
            units = optimisticUnits,
            savedUnitId = null,
            unitEditor = if (_uiState.value.unitEditor.unitId == unitId) {
                PracticeUnitEditorState()
            } else {
                _uiState.value.unitEditor.resolveWith(optimisticUnits + _uiState.value.inlineUnits)
            },
            sessionEditor = _uiState.value.sessionEditor.resolveWith(previousSessions),
            status = PlannerStatus.Notification("Deleted $title."),
        )

        val token = ++operationToken
        val library = foundation.practiceLibrary
        viewModelScope.launch {
            operationMutex.withLock {
                try {
                    library.deleteUnit(unitId)
                    val units = library.listUnits()
                    val sessions = library.listSessions()
                    val inlineUnits = hydrateInlineUnits(library, units, sessions + _uiState.value.archivedSessions)
                    if (token == operationToken) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            units = units,
                            sessions = sessions,
                            inlineUnits = inlineUnits,
                            unitEditor = _uiState.value.unitEditor.resolveWith(units + inlineUnits),
                            sessionEditor = _uiState.value.sessionEditor.resolveWith(sessions),
                        )
                    }
                } catch (exception: Exception) {
                    if (token == operationToken) {
                        _uiState.value = _uiState.value.copy(
                            units = previousUnits,
                            sessions = previousSessions,
                            unitEditor = _uiState.value.unitEditor.resolveWith(previousUnits + _uiState.value.inlineUnits),
                            sessionEditor = _uiState.value.sessionEditor.resolveWith(previousSessions),
                            status = if (exception.isForeignKeyViolation()) {
                                PlannerStatus.Notification(
                                    "This unit is used by one or more sessions. Remove it from those sessions first.",
                                )
                            } else {
                                plannerStatus(exception = exception, fallback = "Unit delete failed.")
                            },
                        )
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
        copy(
            items = reindexedSessionItems(
                items + PracticeSessionItemEditorState(
                    order = items.size + 1,
                    repeatCount = "1",
                ),
            ),
        )
    }

    fun updateSessionItemUnit(index: Int, practiceUnitId: String) {
        // Eligibility reads from the saved units list (blank-normalized), not from
        // any editor draft. Action-only units (0 balls) never offer observations,
        // so switching to one clears all types — otherwise they'd persist invisibly
        // (the picker is hidden for action-only units). A ball-bearing unit without
        // a criterion keeps its other types but drops SUCCESS.
        val newUnit = _uiState.value.units.firstOrNull { it.id == practiceUnitId }
        val newUnitHasBalls = (newUnit?.derivedBallCount() ?: 0) > 0
        val newUnitHasCriterion = newUnit?.successCriterion?.isNotBlank() == true
        updateSessionItem(index) {
            val nextTypes = when {
                !newUnitHasBalls -> emptyList()
                newUnitHasCriterion -> observationTypes
                else -> observationTypes - ObservationType.SUCCESS
            }
            copy(
                practiceUnitId = practiceUnitId,
                unitError = null,
                observationTypes = nextTypes,
                observationTypesError = null,
            )
        }
    }

    fun toggleSessionItemObservationType(index: Int, type: ObservationType) = updateSessionItem(index) {
        val nextTypes = if (type in observationTypes) {
            observationTypes - type
        } else {
            observationTypes + type
        }
        copy(observationTypes = nextTypes, observationTypesError = null)
    }

    fun updateSessionItemRepeatCount(index: Int, value: String) = updateSessionItem(index) {
        copy(repeatCount = value, repeatCountError = null)
    }

    fun updateSessionItemRepeatCount(index: Int, value: Int) = updateSessionItemRepeatCount(index, value.toString())

    fun updateSessionItemClubCode(index: Int, value: String) = updateSessionItem(index) {
        copy(clubCode = value)
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

    @OptIn(ExperimentalUuidApi::class)
    fun saveSession() {
        val foundation = dataFoundation ?: return markPlannerUnavailable()
        if (activeUserId == null) {
            markSignedOut()
            return
        }

        val editor = _uiState.value.sessionEditor
        when (val review = PracticeDraftEditor.reviewSession(editor)) {
            is DraftReview.Invalid -> {
                _uiState.value = _uiState.value.copy(
                    sessionEditor = review.input,
                    status = PlannerStatus.Notification(review.issues.joinToString(" ") { it.message }),
                )
                return
            }
            is DraftReview.Valid -> {
                val draft = review.draft
                val resolvedSessionId = editor.sessionId ?: Uuid.random().toString()
                val previousUnits = _uiState.value.units
                val previousSessions = _uiState.value.sessions
                val now = Clock.System.now()
                val optimisticSession = buildOptimisticSession(resolvedSessionId, draft, now)
                val optimisticSessions = if (editor.sessionId != null) {
                    previousSessions.map { if (it.id == resolvedSessionId) optimisticSession else it }
                } else {
                    previousSessions + optimisticSession
                }

                _uiState.value = _uiState.value.copy(
                    sessions = optimisticSessions,
                    unitEditor = _uiState.value.unitEditor.resolveWith(previousUnits + _uiState.value.inlineUnits),
                    sessionEditor = optimisticSession.toEditorState(),
                    sessionEditorBaseline = null,
                    savedSessionId = resolvedSessionId,
                    status = PlannerStatus.Notification("Saved ${draft.name}."),
                )

                val library = foundation.practiceLibrary
                val token = ++operationToken
                viewModelScope.launch {
                    operationMutex.withLock {
                        try {
                            when (val result = library.saveSession(draft = draft, sessionId = resolvedSessionId)) {
                                is PracticeLibraryResult.Saved -> {
                                    val units = library.listUnits()
                                    val sessions = library.listSessions()
                                    val archivedSessions = library.listArchivedSessions()
                                    val inlineUnits = hydrateInlineUnits(library, units, sessions + archivedSessions)
                                    if (token == operationToken) {
                                        _uiState.value = _uiState.value.copy(
                                            isLoading = false,
                                            units = units,
                                            sessions = sessions,
                                            archivedSessions = archivedSessions,
                                            inlineUnits = inlineUnits,
                                            unitEditor = _uiState.value.unitEditor.resolveWith(units + inlineUnits),
                                            sessionEditor = _uiState.value.sessionEditor.resolveWith(sessions),
                                        )
                                    }
                                }
                                is PracticeLibraryResult.Invalid -> {
                                    if (token == operationToken) {
                                        _uiState.value = _uiState.value.copy(
                                            units = previousUnits,
                                            sessions = previousSessions,
                                            unitEditor = _uiState.value.unitEditor.resolveWith(previousUnits + _uiState.value.inlineUnits),
                                            sessionEditor = PracticeDraftEditor.placeSessionErrors(
                                                _uiState.value.sessionEditor, result.issues,
                                            ),
                                            status = PlannerStatus.Notification(
                                                result.issues.joinToString(" ") { it.message },
                                            ),
                                        )
                                    }
                                }
                            }
                        } catch (exception: Exception) {
                            if (token == operationToken) {
                                _uiState.value = _uiState.value.copy(
                                    units = previousUnits,
                                    sessions = previousSessions,
                                    unitEditor = _uiState.value.unitEditor.resolveWith(previousUnits + _uiState.value.inlineUnits),
                                    sessionEditor = _uiState.value.sessionEditor.resolveWith(previousSessions),
                                    status = plannerStatus(exception = exception, fallback = "Session save failed."),
                                )
                            }
                        }
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

        val previousUnits = _uiState.value.units
        val previousSessions = _uiState.value.sessions
        val previousArchived = _uiState.value.archivedSessions
        val name = (previousSessions + previousArchived).firstOrNull { it.id == sessionId }?.name ?: "session"
        val optimisticSessions = previousSessions.filter { it.id != sessionId }
        val optimisticArchived = previousArchived.filter { it.id != sessionId }

        _uiState.value = _uiState.value.copy(
            sessions = optimisticSessions,
            archivedSessions = optimisticArchived,
            savedSessionId = null,
            unitEditor = _uiState.value.unitEditor.resolveWith(previousUnits + _uiState.value.inlineUnits),
            sessionEditor = if (_uiState.value.sessionEditor.sessionId == sessionId) {
                PracticeSessionEditorState()
            } else {
                _uiState.value.sessionEditor.resolveWith(optimisticSessions)
            },
            status = PlannerStatus.Notification("Deleted $name."),
        )

        val token = ++operationToken
        val library = foundation.practiceLibrary
        viewModelScope.launch {
            operationMutex.withLock {
                try {
                    library.deleteSession(sessionId)
                    val units = library.listUnits()
                    val sessions = library.listSessions()
                    val archivedSessions = library.listArchivedSessions()
                    val inlineUnits = hydrateInlineUnits(library, units, sessions + archivedSessions)
                    if (token == operationToken) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            units = units,
                            sessions = sessions,
                            archivedSessions = archivedSessions,
                            inlineUnits = inlineUnits,
                            unitEditor = _uiState.value.unitEditor.resolveWith(units + inlineUnits),
                            sessionEditor = _uiState.value.sessionEditor.resolveWith(sessions),
                        )
                    }
                } catch (exception: Exception) {
                    if (token == operationToken) {
                        _uiState.value = _uiState.value.copy(
                            units = previousUnits,
                            sessions = previousSessions,
                            archivedSessions = previousArchived,
                            unitEditor = _uiState.value.unitEditor.resolveWith(previousUnits + _uiState.value.inlineUnits),
                            sessionEditor = _uiState.value.sessionEditor.resolveWith(previousSessions),
                            status = plannerStatus(exception = exception, fallback = "Session delete failed."),
                        )
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

        val library = foundation.practiceLibrary
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, duplicatedUnitId = null)
            try {
                val duplicated = library.duplicateUnit(unitId)
                val units = library.listUnits()
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

        val library = foundation.practiceLibrary
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSaving = true, duplicatedSessionId = null)
            try {
                val duplicated = library.duplicateSession(sessionId)
                val sessions = library.listSessions()
                val inlineUnits = hydrateInlineUnits(
                    library,
                    _uiState.value.units,
                    sessions + _uiState.value.archivedSessions,
                )
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    isSaving = false,
                    sessions = sessions,
                    inlineUnits = inlineUnits,
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

    /**
     * Promote an Inline Unit to the library (design §7): user-initiated only,
     * one-way, session content unchanged. Mirrors the optimistic
     * duplicate/delete/archive shape — move the unit between the two lists
     * immediately, then reconcile against the source of truth.
     */
    fun promoteUnit(unitId: String) {
        val foundation = dataFoundation ?: return markPlannerUnavailable()
        if (activeUserId == null) {
            markSignedOut()
            return
        }

        val target = _uiState.value.findUnit(unitId)
        if (target == null || !target.isInline) {
            _uiState.value = _uiState.value.copy(
                status = PlannerStatus.Notification("Unit is not available to promote."),
            )
            return
        }

        val previousUnits = _uiState.value.units
        val previousInlineUnits = _uiState.value.inlineUnits

        _uiState.value = _uiState.value.copy(
            units = previousUnits + target.copy(scopedToSessionId = null),
            inlineUnits = previousInlineUnits.filter { it.id != unitId },
            status = PlannerStatus.Notification("Promoted \"${target.title}\" to your library."),
        )

        val token = ++operationToken
        val library = foundation.practiceLibrary
        viewModelScope.launch {
            operationMutex.withLock {
                try {
                    library.promoteUnit(unitId)
                    val units = library.listUnits()
                    val sessions = library.listSessions()
                    val archivedSessions = library.listArchivedSessions()
                    val inlineUnits = hydrateInlineUnits(library, units, sessions + archivedSessions)
                    if (token == operationToken) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            units = units,
                            sessions = sessions,
                            archivedSessions = archivedSessions,
                            inlineUnits = inlineUnits,
                            unitEditor = _uiState.value.unitEditor.resolveWith(units + inlineUnits),
                            sessionEditor = _uiState.value.sessionEditor.resolveWith(sessions),
                        )
                    }
                } catch (exception: Exception) {
                    if (token == operationToken) {
                        _uiState.value = _uiState.value.copy(
                            units = previousUnits,
                            inlineUnits = previousInlineUnits,
                            status = plannerStatus(exception = exception, fallback = "Promote failed."),
                        )
                    }
                }
            }
        }
    }

    fun restoreUnit(unit: PracticeUnit) {
        val foundation = dataFoundation ?: return markPlannerUnavailable()
        if (activeUserId == null) { markSignedOut(); return }
        val library = foundation.practiceLibrary
        viewModelScope.launch {
            try {
                library.restoreUnit(unit)
                val units = library.listUnits()
                val sessions = library.listSessions()
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
        val library = foundation.practiceLibrary
        viewModelScope.launch {
            try {
                library.restoreSession(session)
                val units = library.listUnits()
                val sessions = library.listSessions()
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

    fun loadArchivedSessions() {
        val foundation = dataFoundation ?: return
        val userId = activeUserId ?: return
        val token = operationToken
        viewModelScope.launch {
            operationMutex.withLock {
                try {
                    val archivedSessions = foundation.practiceLibrary.listArchivedSessions()
                    if (userId == activeUserId && token == operationToken) {
                        val currentState = _uiState.value
                        val inlineUnits = hydrateInlineUnits(
                            library = foundation.practiceLibrary,
                            units = currentState.units,
                            sessions = currentState.sessions + archivedSessions,
                        )
                        _uiState.value = currentState.copy(
                            archivedSessions = archivedSessions,
                            inlineUnits = inlineUnits,
                        )
                    }
                } catch (e: Exception) {
                    // Non-fatal: the archived list/footer count simply won't refresh.
                }
            }
        }
    }

    fun archiveSession(sessionId: String) {
        val foundation = dataFoundation ?: return markPlannerUnavailable()
        if (activeUserId == null) {
            markSignedOut()
            return
        }

        val previousSessions = _uiState.value.sessions
        val previousArchived = _uiState.value.archivedSessions
        val target = previousSessions.firstOrNull { it.id == sessionId } ?: return

        _uiState.value = _uiState.value.copy(
            sessions = previousSessions.filter { it.id != sessionId },
            archivedSessions = previousArchived + target.copy(archivedAt = Clock.System.now()),
            status = PlannerStatus.Notification("Archived \"${target.name}\"."),
        )

        val token = ++operationToken
        val library = foundation.practiceLibrary
        viewModelScope.launch {
            operationMutex.withLock {
                try {
                    library.archiveSession(sessionId)
                    val sessions = library.listSessions()
                    val archivedSessions = library.listArchivedSessions()
                    if (token == operationToken) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            sessions = sessions,
                            archivedSessions = archivedSessions,
                        )
                    }
                } catch (exception: Exception) {
                    if (token == operationToken) {
                        _uiState.value = _uiState.value.copy(
                            sessions = previousSessions,
                            archivedSessions = previousArchived,
                            status = plannerStatus(exception = exception, fallback = "Archive failed."),
                        )
                    }
                }
            }
        }
    }

    fun unarchiveSession(sessionId: String) {
        val foundation = dataFoundation ?: return markPlannerUnavailable()
        if (activeUserId == null) {
            markSignedOut()
            return
        }

        val previousSessions = _uiState.value.sessions
        val previousArchived = _uiState.value.archivedSessions
        val target = previousArchived.firstOrNull { it.id == sessionId } ?: return

        _uiState.value = _uiState.value.copy(
            sessions = previousSessions + target.copy(archivedAt = null),
            archivedSessions = previousArchived.filter { it.id != sessionId },
            status = PlannerStatus.Notification("Unarchived \"${target.name}\"."),
        )

        val token = ++operationToken
        val library = foundation.practiceLibrary
        viewModelScope.launch {
            operationMutex.withLock {
                try {
                    library.unarchiveSession(sessionId)
                    val sessions = library.listSessions()
                    val archivedSessions = library.listArchivedSessions()
                    if (token == operationToken) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            sessions = sessions,
                            archivedSessions = archivedSessions,
                        )
                    }
                } catch (exception: Exception) {
                    if (token == operationToken) {
                        _uiState.value = _uiState.value.copy(
                            sessions = previousSessions,
                            archivedSessions = previousArchived,
                            status = plannerStatus(exception = exception, fallback = "Unarchive failed."),
                        )
                    }
                }
            }
        }
    }

    fun startRangeSession(sessionId: String) {
        val foundation = dataFoundation ?: run {
            _uiState.value = _uiState.value.copy(
                status = PlannerStatus.Notification("Cannot start session: data not configured."),
            )
            return
        }
        viewModelScope.launch {
            try {
                val rangeSession = foundation.rangeSessionRepository.start(sessionId)
                _uiState.value = _uiState.value.copy(
                    startedRangeSessionId = rangeSession.id,
                )
            } catch (exception: Exception) {
                _uiState.value = _uiState.value.copy(
                    status = PlannerStatus.Notification(
                        "Failed to start session: ${exception.message ?: "unknown error"}",
                    ),
                )
            }
        }
    }

    fun consumeStartedRangeSessionId() {
        _uiState.value = _uiState.value.copy(startedRangeSessionId = null)
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
        val library = foundation.practiceLibrary
        viewModelScope.launch {
            operationMutex.withLock {
                try {
                    val units = library.listUnits()
                    val sessions = library.listSessions()
                    val archivedSessions = library.listArchivedSessions()
                    val inlineUnits = hydrateInlineUnits(library, units, sessions + archivedSessions)
                    if (token == operationToken) {
                        _uiState.value = _uiState.value.copy(
                            isLoading = false,
                            isSaving = false,
                            hasLoaded = true,
                            units = units,
                            sessions = sessions,
                            archivedSessions = archivedSessions,
                            inlineUnits = inlineUnits,
                            unitEditor = _uiState.value.unitEditor.resolveWith(units + inlineUnits),
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

    /**
     * Inline units have no "list" surface (Stage 4 declined one; they're reached
     * by id through their owning session), so gather them by id: anything a
     * session references that isn't already in `units` is either inline or gone.
     * A failed `getUnit` is non-fatal — the id simply falls back to "Missing unit".
     */
    private suspend fun hydrateInlineUnits(
        library: PracticeLibrary,
        units: List<PracticeUnit>,
        sessions: List<PracticeSession>,
    ): List<PracticeUnit> {
        val known = units.mapTo(HashSet()) { it.id }
        val referenced = sessions.flatMap { it.items }.map { it.practiceUnitId }.toSet()
        return (referenced - known).mapNotNull { id ->
            try {
                library.getUnit(id)
            } catch (e: Exception) {
                null
            }
        }.filter { it.isInline }
    }

    @OptIn(ExperimentalUuidApi::class)
    private fun buildOptimisticUnit(
        id: String,
        draft: PracticeUnitDraft,
        now: Instant,
    ): PracticeUnit = PracticeUnit(
        id = id,
        title = draft.title,
        instructions = draft.instructions.map { instr ->
            PracticeInstruction(
                id = Uuid.random().toString(),
                order = instr.order,
                text = instr.text,
                ballCount = instr.ballCount,
                clubCode = instr.clubCode,
            )
        },
        notes = draft.notes,
        focus = draft.focus,
        defaultClubCode = draft.defaultClubCode,
        successCriterion = draft.successCriterion,
        tags = resolveTagsForDraft(draft.tagIds),
        createdAt = now,
        updatedAt = now,
    )

    @OptIn(ExperimentalUuidApi::class)
    private fun buildOptimisticSession(
        id: String,
        draft: PracticeSessionDraft,
        now: Instant,
    ): PracticeSession = PracticeSession(
        id = id,
        name = draft.name,
        items = draft.items.map { item ->
            PracticeSessionItem(
                id = Uuid.random().toString(),
                practiceUnitId = item.practiceUnitId,
                order = item.order,
                repeatCount = item.repeatCount,
                clubCode = item.clubCode,
                notes = item.notes,
                focusCue = item.focusCue,
                observationTypes = item.observationTypes,
            )
        },
        notes = draft.notes,
        tags = resolveTagsForDraft(draft.tagIds),
        createdAt = now,
        updatedAt = now,
    )

    private fun resolveTagsForDraft(tagIds: List<String>): List<Tag> {
        val byId = _uiState.value.availableTags.associateBy(Tag::id)
        return tagIds.mapNotNull(byId::get)
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
            hasLoaded = true,
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

    fun moveInstruction(fromIndex: Int, toIndex: Int) {
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

    fun loadTags() {
        val foundation = dataFoundation ?: return
        viewModelScope.launch {
            try {
                val tags = foundation.tagRepository.list()
                _uiState.value = _uiState.value.copy(availableTags = tags)
            } catch (e: Exception) {
                // Non-fatal: tag picker/filter simply show no options.
            }
        }
    }

    fun toggleUnitTag(tagId: String) = updateUnitEditor {
        if (tagId in tagIds) {
            copy(tagIds = tagIds - tagId)
        } else if (tagIds.size < MAX_TAGS_PER_ITEM) {
            copy(tagIds = tagIds + tagId)
        } else {
            this
        }
    }

    fun createUnitTag(name: String) = createTagThen(name) { tag ->
        updateUnitEditor {
            if (tag.id in tagIds || tagIds.size >= MAX_TAGS_PER_ITEM) this
            else copy(tagIds = tagIds + tag.id)
        }
    }

    fun toggleSessionTag(tagId: String) = updateSessionEditor {
        if (tagId in tagIds) {
            copy(tagIds = tagIds - tagId)
        } else if (tagIds.size < MAX_TAGS_PER_ITEM) {
            copy(tagIds = tagIds + tagId)
        } else {
            this
        }
    }

    fun createSessionTag(name: String) = createTagThen(name) { tag ->
        updateSessionEditor {
            if (tag.id in tagIds || tagIds.size >= MAX_TAGS_PER_ITEM) this
            else copy(tagIds = tagIds + tag.id)
        }
    }

    fun toggleUnitTagFilter(tagId: String) {
        val current = _uiState.value.unitTagFilter
        _uiState.value = _uiState.value.copy(
            unitTagFilter = if (tagId in current) current - tagId else current + tagId,
        )
    }

    fun clearUnitTagFilter() {
        _uiState.value = _uiState.value.copy(unitTagFilter = emptySet())
    }

    fun toggleSessionTagFilter(tagId: String) {
        val current = _uiState.value.sessionTagFilter
        _uiState.value = _uiState.value.copy(
            sessionTagFilter = if (tagId in current) current - tagId else current + tagId,
        )
    }

    fun clearSessionTagFilter() {
        _uiState.value = _uiState.value.copy(sessionTagFilter = emptySet())
    }

    private fun createTagThen(name: String, attach: (Tag) -> Unit) {
        val foundation = dataFoundation ?: return
        if (activeUserId == null) {
            markSignedOut()
            return
        }
        viewModelScope.launch {
            try {
                val tag = foundation.tagRepository.createOrGet(name = name)
                val tags = _uiState.value.availableTags
                if (tags.none { it.id == tag.id }) {
                    _uiState.value = _uiState.value.copy(availableTags = tags + tag)
                }
                attach(tag)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    status = plannerStatus(exception = e, fallback = "Could not add tag."),
                )
            }
        }
    }

    private fun loadClubs() {
        val foundation = dataFoundation ?: return
        viewModelScope.launch {
            try {
                val catalog = foundation.clubRepository.listCatalog()
                val enabled = foundation.clubRepository.getEnabledClubCodes()
                _uiState.value = _uiState.value.copy(
                    clubCatalog = catalog,
                    enabledClubCodes = enabled,
                )
            } catch (e: Exception) {
                // Club catalog failures are non-fatal; picker will show empty options
            }
        }
    }

    fun loadActiveRangeSessions() {
        val foundation = dataFoundation ?: return
        viewModelScope.launch {
            try {
                val activeSessions = foundation.rangeSessionRepository.listActiveSessions()
                _uiState.value = _uiState.value.copy(activeRangeSessions = activeSessions)
            } catch (e: Exception) {
                // Non-fatal: active sessions simply won't be shown
            }
        }
    }

    fun loadRangeSessionHistory(sessionId: String) {
        val foundation = dataFoundation ?: return
        viewModelScope.launch {
            try {
                val history = foundation.rangeSessionRepository.listCompletedSessions(sessionId)
                _uiState.value = _uiState.value.copy(
                    completedRangeSessionHistory = _uiState.value.completedRangeSessionHistory.toMutableMap()
                        .apply { put(sessionId, history) }
                )
            } catch (e: Exception) {
                // Non-fatal: history simply won't be shown
            }
        }
    }

    fun startRangeSessionFromPicker(sessionId: String) {
        val foundation = dataFoundation ?: return
        viewModelScope.launch {
            try {
                val newSession = foundation.rangeSessionRepository.start(sessionId)
                _uiState.value = _uiState.value.copy(startedRangeSessionId = newSession.id)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    status = PlannerStatus.Notification("Failed to start session: ${e.message ?: "unknown error"}")
                )
            }
        }
    }

    fun checkActiveSessionsForTemplate(sessionId: String, onResult: (Boolean) -> Unit) {
        val foundation = dataFoundation ?: return
        viewModelScope.launch {
            try {
                val hasActive = foundation.rangeSessionRepository.hasActiveSessionsForTemplate(sessionId)
                onResult(hasActive)
            } catch (e: Exception) {
                // Default to no active sessions on error
                onResult(false)
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
    defaultClubCode = defaultClubCode.orEmpty(),
    instructions = if (instructions.isEmpty()) {
        listOf(PracticeInstructionEditorState(order = 1))
    } else {
        instructions.map { instruction -> instruction.toEditorState() }
    },
    successCriterion = successCriterion.orEmpty(),
    tagIds = tags.map(Tag::id),
)

private fun PracticeInstruction.toEditorState(): PracticeInstructionEditorState = PracticeInstructionEditorState(
    order = order,
    text = text,
    ballCount = ballCount?.toString().orEmpty(),
    clubCode = clubCode.orEmpty(),
)

private fun PracticeSession.toEditorState(): PracticeSessionEditorState = PracticeSessionEditorState(
    sessionId = id,
    name = name,
    notes = notes.orEmpty(),
    items = items.map { item -> item.toEditorState() },
    tagIds = tags.map(Tag::id),
)

private fun PracticeSessionItem.toEditorState(): PracticeSessionItemEditorState = PracticeSessionItemEditorState(
    order = order,
    practiceUnitId = practiceUnitId,
    repeatCount = repeatCount.toString(),
    clubCode = clubCode.orEmpty(),
    notes = notes.orEmpty(),
    focusCue = focusCue.orEmpty(),
    observationTypes = observationTypes,
)

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

// reindexedInstructions, reindexedSessionItems, and moveItem are unchanged
