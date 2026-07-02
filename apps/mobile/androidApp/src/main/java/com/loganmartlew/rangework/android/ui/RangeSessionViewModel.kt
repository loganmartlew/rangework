package com.loganmartlew.rangework.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.loganmartlew.rangework.shared.model.RangeSession
import com.loganmartlew.rangework.shared.model.clubSwapTargets
import com.loganmartlew.rangework.shared.model.completedBalls
import com.loganmartlew.rangework.shared.model.completedStepCount
import com.loganmartlew.rangework.shared.model.completionPercentage
import com.loganmartlew.rangework.shared.model.decrementTargets
import com.loganmartlew.rangework.shared.model.executionBlocks
import com.loganmartlew.rangework.shared.model.firstIncompleteBlockIndex
import com.loganmartlew.rangework.shared.model.incrementTargets
import com.loganmartlew.rangework.shared.model.totalBalls
import com.loganmartlew.rangework.shared.model.totalStepCount
import com.loganmartlew.rangework.shared.repository.RangeSessionRepository
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
)

class RangeSessionViewModel(
    private val rangeSessionId: String,
    private val rangeSessionRepository: RangeSessionRepository?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RangeSessionUiState())
    val uiState: StateFlow<RangeSessionUiState> = _uiState.asStateFlow()

    private var tickJob: Job? = null
    private var currentEnteredAt: Instant? = null

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
                _uiState.value = RangeSessionUiState(
                    rangeSession = session,
                    currentBlockIndex = landingBlock,
                    isLoading = false,
                    statusMessage = if (session == null) "Session not found." else null,
                    completedStepIndices = completedIndices,
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

    /** One "+1" (or "Done") tap on the given block's counter. */
    fun incrementBlock(blockIndex: Int) {
        val state = _uiState.value
        val session = state.rangeSession ?: return
        val block = session.snapshot.executionBlocks().getOrNull(blockIndex) ?: return
        val targets = block.incrementTargets(session.snapshot.steps, state.completedStepIndices)
        setStepsCompletion(targets, completed = true)
    }

    /** One "−1" tap: the exact inverse of the block's most recent counter tap. */
    fun decrementBlock(blockIndex: Int) {
        val state = _uiState.value
        val session = state.rangeSession ?: return
        val block = session.snapshot.executionBlocks().getOrNull(blockIndex) ?: return
        val targets = block.decrementTargets(session.snapshot.steps, state.completedStepIndices)
        setStepsCompletion(targets, completed = false)
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
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(RangeSessionViewModel::class.java)) {
                    "Unsupported ViewModel class: ${modelClass.name}"
                }
                return RangeSessionViewModel(
                    rangeSessionId = rangeSessionId,
                    rangeSessionRepository = rangeSessionRepository,
                ) as T
            }
        }
    }
}
