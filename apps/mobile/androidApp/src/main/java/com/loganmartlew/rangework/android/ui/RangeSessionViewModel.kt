package com.loganmartlew.rangework.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.loganmartlew.rangework.shared.model.BlockResult
import com.loganmartlew.rangework.shared.model.Handedness
import com.loganmartlew.rangework.shared.model.Observation
import com.loganmartlew.rangework.shared.model.RangeSession
import com.loganmartlew.rangework.shared.model.clubSwapTargets
import com.loganmartlew.rangework.shared.model.completedBalls
import com.loganmartlew.rangework.shared.model.completedStepCount
import com.loganmartlew.rangework.shared.model.completionPercentage
import com.loganmartlew.rangework.shared.model.decrementTargets
import com.loganmartlew.rangework.shared.model.enabledObservationTypes
import com.loganmartlew.rangework.shared.model.executionBlocks
import com.loganmartlew.rangework.shared.model.firstIncompleteBlockIndex
import com.loganmartlew.rangework.shared.model.incrementTargets
import com.loganmartlew.rangework.shared.model.isBallStep
import com.loganmartlew.rangework.shared.model.ObservationType
import com.loganmartlew.rangework.shared.model.totalBalls
import com.loganmartlew.rangework.shared.model.totalStepCount
import com.loganmartlew.rangework.shared.recording.RangeSessionRecorder
import com.loganmartlew.rangework.shared.recording.RecordingResult
import com.loganmartlew.rangework.shared.repository.MeasurementPreferencesRepository
import com.loganmartlew.rangework.shared.repository.RangeSessionRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant

data class FinishSummaryData(
    val sessionName: String,
    val totalBalls: Int,
    val completedBalls: Int,
    val completionPercentage: Double,
    val completedStepCount: Int,
    val totalStepCount: Int,
    val elapsedSeconds: Long?,
)

data class RangeSessionUiState(
    val rangeSession: RangeSession? = null,
    val currentBlockIndex: Int = 0,
    val isLoading: Boolean = true,
    val statusMessage: String? = null,
    val completedStepIndices: Set<Int> = emptySet(),
    val notification: String? = null,
    val elapsedSeconds: Long = 0,
    val isTimerRunning: Boolean = false,
    val showAbandonDialog: Boolean = false,
    val showFinishDialog: Boolean = false,
    val finishSummary: FinishSummaryData? = null,
    val isFinishing: Boolean = false,
    val isAbandoning: Boolean = false,
    val isSavingSessionNote: Boolean = false,
    /** unitIndex set of blocks whose note is mid-save (for the per-block saving spinner). */
    val savingBlockNoteIndices: Set<Int> = emptySet(),
    // ── Per-ball observation capture (snapshot v3) ────────────────────────────
    /** Committed observations keyed by global step index (the Ball Step's identity). */
    val observationsByStep: Map<Int, Observation> = emptyMap(),
    /** The pending (uncommitted) ball's staged values per block index: type id → value. */
    val stagingByBlock: Map<Int, Map<String, String>> = emptyMap(),
    /** The block whose staged ball is in its auto-commit arm window, or null. */
    val armingBlockIndex: Int? = null,
    /** Orients the perspective-dependent capture surfaces; RIGHT until loaded. */
    val handedness: Handedness = Handedness.RIGHT,
    /** Bumps once per successful commit; the committed block's counter pulses on change. */
    val commitSignal: Int = 0,
    /** The block index of the most recent commit (which page should pulse). */
    val committedBlockIndex: Int? = null,
)

class RangeSessionViewModel(
    private val rangeSessionId: String,
    private val rangeSessionRepository: RangeSessionRepository?,
    private val rangeSessionRecorder: RangeSessionRecorder? = null,
    private val measurementPreferencesRepository: MeasurementPreferencesRepository? = null,
    private val autoCommitDelayMillis: Long = 300,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RangeSessionUiState())
    val uiState: StateFlow<RangeSessionUiState> = _uiState.asStateFlow()

    private var tickJob: Job? = null
    private var currentEnteredAt: Instant? = null

    /** The pending auto-commit; cancelled if the ball is re-edited before it fires. */
    private var armJob: Job? = null

    // Latest-wins guards for auto-saving notes: a new debounced/flush write
    // cancels the in-flight one for the same target so rapid edits can't land out
    // of order. Keyed by block unit index; the session note is a single slot.
    private var sessionNoteSaveJob: Job? = null
    private val blockNoteSaveJobs = mutableMapOf<Int, Job>()

    init {
        loadSession()
    }

    private fun loadSession() {
        val repository = rangeSessionRepository ?: run {
            _uiState.value = RangeSessionUiState(
                isLoading = false,
                statusMessage = "Session data not available.",
            )
            return
        }
        viewModelScope.launch {
            try {
                val session = repository.getSession(rangeSessionId)
                val completedIndices =
                    session?.completedSteps?.map { it.stepIndex }?.toSet() ?: emptySet()
                val landingBlock = session?.let {
                    firstIncompleteBlockIndex(it.snapshot.executionBlocks(), completedIndices)
                } ?: 0

                // v3 capture: load committed observations (failure degrades to empty
                // + a snackbar — the counter floor still works) and the handedness
                // preference (any failure → RIGHT, the transform identity).
                var observationsByStep: Map<Int, Observation> = emptyMap()
                var observationLoadFailed = false
                var handedness = Handedness.RIGHT
                if (session?.supportsDataCapture == true) {
                    val recorder = rangeSessionRecorder
                    if (recorder != null) {
                        try {
                            observationsByStep = recorder.observations(rangeSessionId)
                                .associateBy(Observation::stepIndex)
                        } catch (_: Exception) {
                            observationLoadFailed = true
                        }
                    }
                    handedness = try {
                        measurementPreferencesRepository?.get()?.handedness ?: Handedness.RIGHT
                    } catch (_: Exception) {
                        Handedness.RIGHT
                    }
                }

                _uiState.value = RangeSessionUiState(
                    rangeSession = session,
                    currentBlockIndex = landingBlock,
                    isLoading = false,
                    statusMessage = if (session == null) "Session not found." else null,
                    completedStepIndices = completedIndices,
                    observationsByStep = observationsByStep,
                    handedness = handedness,
                    notification = if (observationLoadFailed) "Couldn't load observations." else null,
                )
            } catch (exception: Exception) {
                _uiState.value = RangeSessionUiState(
                    isLoading = false,
                    statusMessage = "Failed to load session: ${exception.message ?: "unknown error"}",
                )
            }
        }
    }

    fun navigateToBlock(index: Int) {
        val state = _uiState.value
        val blockCount = state.rangeSession?.snapshot?.units?.size ?: return
        if (blockCount == 0) return
        val clamped = index.coerceIn(0, blockCount - 1)
        if (clamped != state.currentBlockIndex) {
            _uiState.value = state.copy(currentBlockIndex = clamped)
        }
    }

    /**
     * One "+1" (or "Done") tap on the given block's counter. v3 sessions route it
     * through the observation-committing path (whatever is staged is written with
     * the ball); v1/v2 keep the plain repository completion.
     */
    fun incrementBlock(blockIndex: Int) {
        val state = _uiState.value
        val session = state.rangeSession ?: return
        if (session.supportsDataCapture) {
            commitBall(blockIndex)
            return
        }
        val block = session.snapshot.executionBlocks().getOrNull(blockIndex) ?: return
        val targets = block.incrementTargets(session.snapshot.steps, state.completedStepIndices)
        setStepsCompletion(targets, completed = true)
    }

    /**
     * One "−1" tap: the exact inverse of the block's most recent counter tap. On v3
     * it also voids the undone ball's observations (Stage 2 rule); v1/v2 keep the
     * plain repository path.
     */
    fun decrementBlock(blockIndex: Int) {
        val state = _uiState.value
        val session = state.rangeSession ?: return
        val block = session.snapshot.executionBlocks().getOrNull(blockIndex) ?: return
        val targets = block.decrementTargets(session.snapshot.steps, state.completedStepIndices)
        if (targets.isEmpty()) return

        if (!session.supportsDataCapture) {
            setStepsCompletion(targets, completed = false)
            return
        }
        // Arm window locks −/+1/stage for the arming block.
        if (state.armingBlockIndex != null) return
        val recorder = rangeSessionRecorder ?: run {
            // Misconfigured foundation: keep the counter working, no observation void.
            setStepsCompletion(targets, completed = false)
            return
        }

        val targetSet = targets.toSet()
        val removedObservations = state.observationsByStep.filterKeys { it in targetSet }
        _uiState.value = state.copy(
            completedStepIndices = state.completedStepIndices - targetSet,
            observationsByStep = state.observationsByStep - targetSet,
        )
        viewModelScope.launch {
            val result = runRecording { recorder.uncompleteStepsVoidingObservations(rangeSessionId, targets) }
            if (result is RecordingResult.Success) {
                _uiState.value = _uiState.value.copy(rangeSession = result.value)
            } else {
                val current = _uiState.value
                _uiState.value = current.copy(
                    completedStepIndices = current.completedStepIndices + targetSet,
                    observationsByStep = current.observationsByStep + removedObservations,
                    notification = "Couldn't undo ball. Please try again.",
                )
            }
        }
    }

    // ── Observation staging & commit (v3) ─────────────────────────────────────

    /**
     * Toggles a staged value for the pending ball on [blockIndex]: tap stages,
     * re-tap the same value un-stages, tapping another value in the type moves the
     * selection. When every enabled type is staged, arms the auto-commit. No-op
     * while that block is arming (input locked) or on non-v3 sessions.
     */
    fun stageObservation(blockIndex: Int, typeId: String, value: String) {
        val state = _uiState.value
        val session = state.rangeSession ?: return
        if (!session.supportsDataCapture) return
        if (state.armingBlockIndex != null) return
        val block = session.snapshot.executionBlocks().getOrNull(blockIndex) ?: return
        val enabled = block.unit.enabledObservationTypes
        if (enabled.isEmpty()) return

        val current = state.stagingByBlock[blockIndex].orEmpty()
        val updatedForBlock = if (current[typeId] == value) current - typeId else current + (typeId to value)
        val updatedStaging = if (updatedForBlock.isEmpty()) {
            state.stagingByBlock - blockIndex
        } else {
            state.stagingByBlock + (blockIndex to updatedForBlock)
        }
        _uiState.value = state.copy(stagingByBlock = updatedStaging)

        val enabledIds = enabled.map(ObservationType::id).toSet()
        if (enabledIds.isNotEmpty() && updatedForBlock.keys.containsAll(enabledIds)) {
            scheduleAutoCommit(blockIndex)
        }
    }

    /** Sets the arm flash and schedules the (uncancellable) auto-commit. */
    private fun scheduleAutoCommit(blockIndex: Int) {
        armJob?.cancel()
        _uiState.value = _uiState.value.copy(armingBlockIndex = blockIndex)
        armJob = viewModelScope.launch {
            delay(autoCommitDelayMillis)
            doCommit(blockIndex)
        }
    }

    /** The user +1 tap on a v3 block: ignored while arming, else commits now. */
    fun commitBall(blockIndex: Int) {
        if (_uiState.value.armingBlockIndex != null) return
        doCommit(blockIndex)
    }

    /**
     * The single commit path (both +1 and auto-commit). Optimistically completes
     * the ball's steps, attaches the staged Observation, clears staging/arm, and
     * pulses the counter; persists via [RangeSessionRecorder.completeStepsRecordingObservation].
     * A failure reverts every optimistic piece (staging is *not* restored — the
     * tap failed wholesale) and surfaces the snackbar.
     */
    private fun doCommit(blockIndex: Int) {
        armJob?.cancel()
        armJob = null
        val state = _uiState.value
        val session = state.rangeSession ?: return
        val block = session.snapshot.executionBlocks().getOrNull(blockIndex) ?: return
        val targets = block.incrementTargets(session.snapshot.steps, state.completedStepIndices)
        if (targets.isEmpty()) {
            _uiState.value = state.copy(armingBlockIndex = null)
            return
        }
        val recorder = rangeSessionRecorder ?: run {
            // Misconfigured foundation: keep the counter working, drop observations.
            _uiState.value = state.copy(armingBlockIndex = null)
            setStepsCompletion(targets, completed = true)
            return
        }

        val ballStep = targets.firstOrNull { session.snapshot.steps[it].isBallStep }
        val staging = state.stagingByBlock[blockIndex].orEmpty()
        val writesObservation = ballStep != null && staging.isNotEmpty()

        val optimisticObservations = if (writesObservation) {
            state.observationsByStep + (ballStep!! to Observation(ballStep, staging))
        } else {
            state.observationsByStep
        }
        _uiState.value = state.copy(
            completedStepIndices = state.completedStepIndices + targets,
            observationsByStep = optimisticObservations,
            stagingByBlock = state.stagingByBlock - blockIndex,
            armingBlockIndex = null,
            commitSignal = state.commitSignal + 1,
            committedBlockIndex = blockIndex,
        )

        viewModelScope.launch {
            val result = runRecording {
                recorder.completeStepsRecordingObservation(rangeSessionId, targets, staging)
            }
            if (result is RecordingResult.Success) {
                _uiState.value = _uiState.value.copy(rangeSession = result.value)
            } else {
                val current = _uiState.value
                _uiState.value = current.copy(
                    completedStepIndices = current.completedStepIndices - targets.toSet(),
                    observationsByStep = if (writesObservation) {
                        current.observationsByStep - ballStep!!
                    } else {
                        current.observationsByStep
                    },
                    notification = "Couldn't record ball. Please try again.",
                )
            }
        }
    }

    /**
     * Edit-sheet write-through: toggles one type's value on an already-committed
     * ball (re-selecting the same value, or a null [value], clears it), then upserts
     * the ball's *full* value map. Optimistic; reverts that ball's record on failure.
     */
    fun updateBallObservation(stepIndex: Int, typeId: String, value: String?) {
        val state = _uiState.value
        val session = state.rangeSession ?: return
        if (!session.supportsDataCapture) return
        val recorder = rangeSessionRecorder ?: return

        val previous = state.observationsByStep[stepIndex]
        val currentValues = previous?.values.orEmpty()
        val updatedValues = if (value == null || currentValues[typeId] == value) {
            currentValues - typeId
        } else {
            currentValues + (typeId to value)
        }
        _uiState.value = state.copy(
            observationsByStep = state.observationsByStep + (stepIndex to Observation(stepIndex, updatedValues)),
        )
        viewModelScope.launch {
            val result = runRecording { recorder.recordObservation(rangeSessionId, stepIndex, updatedValues) }
            if (result !is RecordingResult.Success) {
                val current = _uiState.value
                _uiState.value = current.copy(
                    observationsByStep = if (previous != null) {
                        current.observationsByStep + (stepIndex to previous)
                    } else {
                        current.observationsByStep - stepIndex
                    },
                    notification = "Couldn't update ball. Please try again.",
                )
            }
        }
    }

    /**
     * Check-off tap on an Action instruction row: completes its next
     * incomplete step, or reopens its last completed step when all are done.
     */
    fun toggleActionInstruction(blockIndex: Int, instructionIndex: Int) {
        val state = _uiState.value
        val session = state.rangeSession ?: return
        val block = session.snapshot.executionBlocks().getOrNull(blockIndex) ?: return
        val indices = block.stepIndices.filter {
            session.snapshot.steps[it].instructionIndex == instructionIndex
        }
        if (indices.isEmpty()) return
        val firstIncomplete = indices.firstOrNull { it !in state.completedStepIndices }
        if (firstIncomplete != null) {
            setStepsCompletion(listOf(firstIncomplete), completed = true)
        } else {
            setStepsCompletion(listOf(indices.last()), completed = false)
        }
    }

    /**
     * Swaps the club for one instruction within a block: overrides fan out to
     * that instruction's remaining incomplete steps, so completed steps keep
     * the club they were actually hit with.
     */
    fun swapClub(blockIndex: Int, instructionIndex: Int, clubCode: String) {
        val state = _uiState.value
        val session = state.rangeSession ?: return
        val repository = rangeSessionRepository ?: return
        val block = session.snapshot.executionBlocks().getOrNull(blockIndex) ?: return
        val targets = block.clubSwapTargets(
            steps = session.snapshot.steps,
            completedStepIndices = state.completedStepIndices,
            instructionIndex = instructionIndex,
        )
        if (targets.isEmpty()) return

        val optimisticSession = session.copy(
            clubOverrides = session.clubOverrides + targets.map { it.toString() to clubCode },
        )
        _uiState.value = state.copy(rangeSession = optimisticSession)

        viewModelScope.launch {
            try {
                val updatedSession = repository.overrideStepClubs(
                    rangeSessionId = rangeSessionId,
                    stepIndices = targets,
                    clubCode = clubCode,
                )
                _uiState.value = _uiState.value.copy(rangeSession = updatedSession)
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    rangeSession = session,
                    notification = "Failed to change club. Please try again.",
                )
            }
        }
    }

    private fun setStepsCompletion(stepIndices: List<Int>, completed: Boolean) {
        if (stepIndices.isEmpty()) return
        val state = _uiState.value
        if (state.rangeSession == null) return
        val repository = rangeSessionRepository ?: return

        val optimisticIndices = if (completed) {
            state.completedStepIndices + stepIndices
        } else {
            state.completedStepIndices - stepIndices.toSet()
        }
        _uiState.value = state.copy(completedStepIndices = optimisticIndices)

        viewModelScope.launch {
            try {
                val updatedSession = repository.setStepsCompletion(
                    rangeSessionId = rangeSessionId,
                    stepIndices = stepIndices,
                    completed = completed,
                )
                _uiState.value = _uiState.value.copy(rangeSession = updatedSession)
            } catch (_: Exception) {
                val current = _uiState.value
                val revertedIndices = if (completed) {
                    current.completedStepIndices - stepIndices.toSet()
                } else {
                    current.completedStepIndices + stepIndices
                }
                _uiState.value = current.copy(
                    completedStepIndices = revertedIndices,
                    notification = "Failed to update progress. Please try again.",
                )
            }
        }
    }

    fun consumeNotification() {
        _uiState.value = _uiState.value.copy(notification = null)
    }

    // ── Data capture (snapshot v3) ───────────────────────────────────────────
    //
    // Notes auto-save: the editor debounces edits and flushes any pending edit on
    // dispose (block swipe, screen change), so there's no Save button. Each write
    // cancels the in-flight one for the same target (latest wins) and no-ops when
    // the normalized value already matches the saved model, so a debounce racing a
    // dispose-flush can't double-write. The manual count writes on each stepper
    // commit, optimistically, reverting on failure (same pattern as step
    // completion). All no-op when the recorder is absent (misconfigured
    // foundation) — the affordances still render.

    /**
     * Saves the session-level note. [onComplete] runs after a successful (or
     * no-op) write — the finish-summary Done button passes its navigation here so
     * a dirty note flushes before leaving; a failed flush stays put and surfaces
     * the snackbar.
     */
    fun saveSessionNote(note: String?, onComplete: () -> Unit = {}) {
        val recorder = rangeSessionRecorder
        val session = _uiState.value.rangeSession
        if (session == null || recorder == null) {
            onComplete()
            return
        }
        // Nothing changed (e.g. a dispose-flush after the debounce already saved):
        // skip the write but still let Done proceed.
        if (note.normalizedNoteValue() == session.sessionNote) {
            onComplete()
            return
        }
        sessionNoteSaveJob?.cancel()
        _uiState.value = _uiState.value.copy(isSavingSessionNote = true)
        sessionNoteSaveJob = viewModelScope.launch {
            val result = runRecording { recorder.saveSessionNote(rangeSessionId, note) }
            if (result is RecordingResult.Success) {
                _uiState.value = _uiState.value.copy(
                    rangeSession = result.value,
                    isSavingSessionNote = false,
                )
                onComplete()
            } else {
                _uiState.value = _uiState.value.copy(
                    isSavingSessionNote = false,
                    notification = "Couldn't save note. Please try again.",
                )
            }
        }
    }

    /** Saves one block's free-text note, keyed by the block's snapshot unit index. */
    fun saveBlockNote(blockIndex: Int, note: String?) {
        val session = _uiState.value.rangeSession ?: return
        val recorder = rangeSessionRecorder ?: return
        val block = session.snapshot.executionBlocks().getOrNull(blockIndex) ?: return
        val unitIndex = block.unitIndex
        // No-op when the normalized value already matches (debounce/dispose race).
        if (note.normalizedNoteValue() == session.blockResults[unitIndex.toString()]?.note) {
            return
        }
        blockNoteSaveJobs.remove(unitIndex)?.cancel()
        _uiState.value = _uiState.value.copy(
            savingBlockNoteIndices = _uiState.value.savingBlockNoteIndices + unitIndex,
        )
        blockNoteSaveJobs[unitIndex] = viewModelScope.launch {
            val result = runRecording { recorder.saveBlockNote(rangeSessionId, unitIndex, note) }
            val remaining = _uiState.value.savingBlockNoteIndices - unitIndex
            if (result is RecordingResult.Success) {
                _uiState.value = _uiState.value.copy(
                    rangeSession = result.value,
                    savingBlockNoteIndices = remaining,
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    savingBlockNoteIndices = remaining,
                    notification = "Couldn't save note. Please try again.",
                )
            }
        }
    }

    /**
     * Sets (or clears, with [count] `null`) one block's manual success count.
     * Writes immediately, optimistically; a failure reverts to the pre-tap
     * session and surfaces the snackbar.
     */
    fun saveManualCount(blockIndex: Int, count: Int?) {
        val session = _uiState.value.rangeSession ?: return
        val recorder = rangeSessionRecorder ?: return
        val block = session.snapshot.executionBlocks().getOrNull(blockIndex) ?: return
        val unitIndex = block.unitIndex
        val key = unitIndex.toString()
        val merged = (session.blockResults[key] ?: BlockResult()).copy(manualCount = count)
        val optimisticResults = if (merged.isEmpty) {
            session.blockResults - key
        } else {
            session.blockResults + (key to merged)
        }
        _uiState.value = _uiState.value.copy(
            rangeSession = session.copy(blockResults = optimisticResults),
        )
        viewModelScope.launch {
            val result = runRecording { recorder.saveManualCount(rangeSessionId, unitIndex, count) }
            if (result is RecordingResult.Success) {
                _uiState.value = _uiState.value.copy(rangeSession = result.value)
            } else {
                _uiState.value = _uiState.value.copy(
                    rangeSession = session,
                    notification = "Couldn't save count. Please try again.",
                )
            }
        }
    }

    /**
     * Runs a recorder call, folding a thrown exception into a null (treated as
     * failure). CancellationException is rethrown so a latest-wins cancel of an
     * in-flight note save doesn't get mis-reported as a save failure.
     */
    private suspend fun <T> runRecording(block: suspend () -> RecordingResult<T>): RecordingResult<T>? =
        try {
            block()
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        }

    /** Normalizes a raw note to the value the model stores (null clears). */
    private fun String?.normalizedNoteValue(): String? = this?.trim()?.takeIf(String::isNotEmpty)

    /**
     * Finish entry point. Finishing never requires 100% of steps: with
     * incomplete steps a three-way dialog asks whether to batch-complete the
     * remainder, finish as-is, or cancel.
     */
    fun requestFinish() {
        val state = _uiState.value
        val session = state.rangeSession ?: return
        if (state.isFinishing) return
        val incomplete = session.snapshot.steps.indices.any { it !in state.completedStepIndices }
        if (incomplete) {
            _uiState.value = state.copy(showFinishDialog = true)
        } else {
            finishSession(completeRemaining = false)
        }
    }

    fun dismissFinishDialog() {
        _uiState.value = _uiState.value.copy(showFinishDialog = false)
    }

    fun completeRemainingAndFinish() {
        finishSession(completeRemaining = true)
    }

    fun finishAsIs() {
        finishSession(completeRemaining = false)
    }

    private fun finishSession(completeRemaining: Boolean) {
        val state = _uiState.value
        val session = state.rangeSession ?: return
        if (state.isFinishing) return

        _uiState.value = state.copy(isFinishing = true, showFinishDialog = false)

        val enteredAt = currentEnteredAt
        currentEnteredAt = null
        stopTick()
        val finalElapsed = state.elapsedSeconds

        val repository = rangeSessionRepository ?: run {
            _uiState.value = _uiState.value.copy(isFinishing = false)
            return
        }

        viewModelScope.launch {
            if (enteredAt != null) {
                try {
                    repository.closeTimeEntry(rangeSessionId, enteredAt, Clock.System.now())
                } catch (_: Exception) { }
            }
            try {
                var latestSession = session
                if (completeRemaining) {
                    val remaining = session.snapshot.steps.indices
                        .filter { it !in _uiState.value.completedStepIndices }
                    if (remaining.isNotEmpty()) {
                        // One write, one shared timestamp — self-evidently a
                        // finish-time batch in the data.
                        latestSession = repository.setStepsCompletion(
                            rangeSessionId = rangeSessionId,
                            stepIndices = remaining,
                            completed = true,
                        )
                        _uiState.value = _uiState.value.copy(
                            completedStepIndices = latestSession.completedSteps
                                .map { it.stepIndex }.toSet(),
                        )
                    }
                }
                repository.finishSession(rangeSessionId)
                val summary = FinishSummaryData(
                    sessionName = latestSession.sessionName,
                    totalBalls = latestSession.totalBalls(),
                    completedBalls = latestSession.completedBalls(),
                    completionPercentage = latestSession.completionPercentage(),
                    completedStepCount = latestSession.completedStepCount(),
                    totalStepCount = latestSession.totalStepCount(),
                    elapsedSeconds = finalElapsed,
                )
                _uiState.value = _uiState.value.copy(
                    isFinishing = false,
                    finishSummary = summary,
                    isTimerRunning = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isFinishing = false,
                    notification = "Failed to finish session: ${e.message ?: "unknown error"}",
                )
            }
        }
    }

    fun requestAbandon() {
        _uiState.value = _uiState.value.copy(showAbandonDialog = true)
    }

    fun dismissAbandon() {
        _uiState.value = _uiState.value.copy(showAbandonDialog = false)
    }

    fun confirmAbandon(onNavigateBack: () -> Unit) {
        val state = _uiState.value
        if (state.isAbandoning) return

        _uiState.value = state.copy(isAbandoning = true, showAbandonDialog = false)

        val enteredAt = currentEnteredAt
        currentEnteredAt = null
        stopTick()

        val repository = rangeSessionRepository ?: run {
            _uiState.value = _uiState.value.copy(isAbandoning = false)
            onNavigateBack()
            return
        }

        viewModelScope.launch {
            if (enteredAt != null) {
                try {
                    repository.closeTimeEntry(rangeSessionId, enteredAt, Clock.System.now())
                } catch (_: Exception) { }
            }
            try {
                repository.abandonSession(rangeSessionId)
                onNavigateBack()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isAbandoning = false,
                    notification = "Failed to abandon session: ${e.message ?: "unknown error"}",
                )
            }
        }
    }

    fun onScreenEnter() {
        val repository = rangeSessionRepository ?: return
        if (currentEnteredAt != null) return
        val enteredAt = Clock.System.now()
        currentEnteredAt = enteredAt
        viewModelScope.launch {
            val elapsed = try {
                repository.getElapsedSeconds(rangeSessionId)
            } catch (_: Exception) {
                0L
            }
            _uiState.value = _uiState.value.copy(
                elapsedSeconds = elapsed,
                isTimerRunning = true,
            )
            try {
                repository.recordTimeEntry(rangeSessionId, enteredAt)
            } catch (_: Exception) {
                // fire-and-forget: network failures don't affect local timer
            }
            startTick()
        }
    }

    fun onScreenExit() {
        val enteredAt = currentEnteredAt ?: return
        currentEnteredAt = null
        stopTick()
        _uiState.value = _uiState.value.copy(isTimerRunning = false)
        val repository = rangeSessionRepository ?: return
        val exitedAt = Clock.System.now()
        viewModelScope.launch {
            try {
                repository.closeTimeEntry(rangeSessionId, enteredAt, exitedAt)
            } catch (_: Exception) {
                // fire-and-forget: network failures don't affect local timer
            }
        }
    }

    private fun startTick() {
        tickJob?.cancel()
        tickJob = viewModelScope.launch(Dispatchers.Default) {
            while (true) {
                delay(1000)
                _uiState.value = _uiState.value.copy(
                    elapsedSeconds = _uiState.value.elapsedSeconds + 1,
                )
            }
        }
    }

    private fun stopTick() {
        tickJob?.cancel()
        tickJob = null
    }

    override fun onCleared() {
        super.onCleared()
        onScreenExit()
    }

    companion object {
        fun factory(
            rangeSessionId: String,
            rangeSessionRepository: RangeSessionRepository?,
            rangeSessionRecorder: RangeSessionRecorder? = null,
            measurementPreferencesRepository: MeasurementPreferencesRepository? = null,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(RangeSessionViewModel::class.java)) {
                    "Unsupported ViewModel class: ${modelClass.name}"
                }
                return RangeSessionViewModel(
                    rangeSessionId = rangeSessionId,
                    rangeSessionRepository = rangeSessionRepository,
                    rangeSessionRecorder = rangeSessionRecorder,
                    measurementPreferencesRepository = measurementPreferencesRepository,
                ) as T
            }
        }
    }
}
