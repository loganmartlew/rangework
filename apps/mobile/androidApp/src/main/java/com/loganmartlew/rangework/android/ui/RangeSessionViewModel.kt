package com.loganmartlew.rangework.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.loganmartlew.rangework.shared.data.DataFoundation
import com.loganmartlew.rangework.shared.model.RangeSession
import com.loganmartlew.rangework.shared.model.completedBalls
import com.loganmartlew.rangework.shared.model.completedStepCount
import com.loganmartlew.rangework.shared.model.completionPercentage
import com.loganmartlew.rangework.shared.model.totalBalls
import com.loganmartlew.rangework.shared.model.totalStepCount
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
    val currentStepIndex: Int = 0,
    val isLoading: Boolean = true,
    val statusMessage: String? = null,
    val completedStepIndices: Set<Int> = emptySet(),
    val notification: String? = null,
    val elapsedSeconds: Long = 0,
    val isTimerRunning: Boolean = false,
    val showAbandonDialog: Boolean = false,
    val finishSummary: FinishSummaryData? = null,
    val isFinishing: Boolean = false,
    val isAbandoning: Boolean = false,
)

class RangeSessionViewModel(
    private val rangeSessionId: String,
    private val dataFoundation: DataFoundation?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RangeSessionUiState())
    val uiState: StateFlow<RangeSessionUiState> = _uiState.asStateFlow()

    private var tickJob: Job? = null
    private var currentEnteredAt: Instant? = null

    init {
        loadSession()
    }

    private fun loadSession() {
        val foundation = dataFoundation ?: run {
            _uiState.value = RangeSessionUiState(
                isLoading = false,
                statusMessage = "Session data not available.",
            )
            return
        }
        viewModelScope.launch {
            try {
                val session = foundation.getRangeSessionUseCase(rangeSessionId)
                val lastIndex = session?.lastViewedStepIndex
                val startIndex = if (session != null && lastIndex != null &&
                    lastIndex in 0 until session.snapshot.steps.size
                ) {
                    lastIndex
                } else {
                    0
                }
                _uiState.value = RangeSessionUiState(
                    rangeSession = session,
                    currentStepIndex = startIndex,
                    isLoading = false,
                    statusMessage = if (session == null) "Session not found." else null,
                    completedStepIndices = session?.completedSteps?.map { it.stepIndex }?.toSet() ?: emptySet(),
                )
            } catch (exception: Exception) {
                _uiState.value = RangeSessionUiState(
                    isLoading = false,
                    statusMessage = "Failed to load session: ${exception.message ?: "unknown error"}",
                )
            }
        }
    }

    fun nextStep() {
        val state = _uiState.value
        val totalSteps = state.rangeSession?.snapshot?.steps?.size ?: return
        val newIndex = (state.currentStepIndex + 1).coerceAtMost(totalSteps - 1)
        if (newIndex != state.currentStepIndex) {
            _uiState.value = state.copy(currentStepIndex = newIndex)
            persistLastViewedStep(newIndex)
        }
    }

    fun previousStep() {
        val state = _uiState.value
        val newIndex = (state.currentStepIndex - 1).coerceAtLeast(0)
        if (newIndex != state.currentStepIndex) {
            _uiState.value = state.copy(currentStepIndex = newIndex)
            persistLastViewedStep(newIndex)
        }
    }

    fun navigateToStep(index: Int) {
        val state = _uiState.value
        val totalSteps = state.rangeSession?.snapshot?.steps?.size ?: return
        val clamped = index.coerceIn(0, totalSteps - 1)
        _uiState.value = state.copy(currentStepIndex = clamped)
        persistLastViewedStep(clamped)
    }

    fun toggleStepComplete(stepIndex: Int) {
        val state = _uiState.value
        val session = state.rangeSession ?: return
        val foundation = dataFoundation ?: return
        val isCurrentlyComplete = stepIndex in state.completedStepIndices

        // Optimistic update — apply immediately for responsive UX
        val optimisticIndices = if (isCurrentlyComplete) {
            state.completedStepIndices - stepIndex
        } else {
            state.completedStepIndices + stepIndex
        }

        // Auto-advance: completing the current step advances to the next (if not at the end)
        val completing = !isCurrentlyComplete
        val isCurrentStep = stepIndex == state.currentStepIndex
        val totalSteps = session.snapshot.steps.size
        val autoAdvanced = completing && isCurrentStep && state.currentStepIndex < totalSteps - 1
        val newStepIndex = if (autoAdvanced) {
            state.currentStepIndex + 1
        } else {
            state.currentStepIndex
        }

        _uiState.value = state.copy(
            completedStepIndices = optimisticIndices,
            currentStepIndex = newStepIndex,
        )

        if (newStepIndex != state.currentStepIndex) {
            persistLastViewedStep(newStepIndex)
        }

        viewModelScope.launch {
            try {
                val updatedSession = foundation.toggleStepCompleteUseCase(
                    rangeSessionId = rangeSessionId,
                    stepIndex = stepIndex,
                    completed = !isCurrentlyComplete,
                )
                // Only refresh the session model; preserve completedStepIndices so
                // concurrent optimistic toggles aren't overwritten by a stale server snapshot.
                _uiState.value = _uiState.value.copy(
                    rangeSession = updatedSession,
                )
            } catch (_: Exception) {
                // Revert only this step's optimistic toggle; leave other steps and
                // navigation untouched since the user may have acted while in-flight.
                val current = _uiState.value
                val revertedIndices = if (isCurrentlyComplete) {
                    current.completedStepIndices + stepIndex
                } else {
                    current.completedStepIndices - stepIndex
                }
                val revertedStepIndex = if (autoAdvanced && current.currentStepIndex == newStepIndex) {
                    state.currentStepIndex
                } else {
                    current.currentStepIndex
                }
                _uiState.value = current.copy(
                    completedStepIndices = revertedIndices,
                    currentStepIndex = revertedStepIndex,
                    notification = "Failed to update step. Please try again.",
                )
            }
        }
    }

    fun consumeNotification() {
        _uiState.value = _uiState.value.copy(notification = null)
    }

    fun overrideStepClub(stepIndex: Int, clubCode: String) {
        val state = _uiState.value
        val session = state.rangeSession ?: return
        val foundation = dataFoundation ?: return

        val optimisticSession = session.copy(
            clubOverrides = session.clubOverrides + (stepIndex.toString() to clubCode),
        )
        _uiState.value = state.copy(rangeSession = optimisticSession)

        viewModelScope.launch {
            try {
                val updatedSession = foundation.overrideStepClubUseCase(
                    rangeSessionId = rangeSessionId,
                    stepIndex = stepIndex,
                    clubCode = clubCode,
                )
                _uiState.value = _uiState.value.copy(rangeSession = updatedSession)
            } catch (_: Exception) {
                _uiState.value = _uiState.value.copy(
                    rangeSession = session,
                    notification = "Failed to override club. Please try again.",
                )
            }
        }
    }

    fun finishSession() {
        val state = _uiState.value
        val session = state.rangeSession ?: return
        if (state.isFinishing) return

        _uiState.value = state.copy(isFinishing = true)

        val enteredAt = currentEnteredAt
        currentEnteredAt = null
        stopTick()
        val finalElapsed = state.elapsedSeconds

        val foundation = dataFoundation ?: run {
            _uiState.value = _uiState.value.copy(isFinishing = false)
            return
        }

        viewModelScope.launch {
            if (enteredAt != null) {
                try {
                    foundation.closeTimeEntryUseCase(rangeSessionId, enteredAt, Clock.System.now())
                } catch (_: Exception) { }
            }
            try {
                foundation.finishRangeSessionUseCase(rangeSessionId)
                val summary = FinishSummaryData(
                    sessionName = session.sessionName,
                    totalBalls = session.totalBalls(),
                    completedBalls = session.completedBalls(),
                    completionPercentage = session.completionPercentage(),
                    completedStepCount = session.completedStepCount(),
                    totalStepCount = session.totalStepCount(),
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

        val foundation = dataFoundation ?: run {
            _uiState.value = _uiState.value.copy(isAbandoning = false)
            onNavigateBack()
            return
        }

        viewModelScope.launch {
            if (enteredAt != null) {
                try {
                    foundation.closeTimeEntryUseCase(rangeSessionId, enteredAt, Clock.System.now())
                } catch (_: Exception) { }
            }
            try {
                foundation.abandonRangeSessionUseCase(rangeSessionId)
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
        val foundation = dataFoundation ?: return
        if (currentEnteredAt != null) return
        val enteredAt = Clock.System.now()
        currentEnteredAt = enteredAt
        viewModelScope.launch {
            val elapsed = try {
                foundation.getElapsedSecondsUseCase(rangeSessionId)
            } catch (_: Exception) {
                0L
            }
            _uiState.value = _uiState.value.copy(
                elapsedSeconds = elapsed,
                isTimerRunning = true,
            )
            try {
                foundation.recordTimeEntryUseCase(rangeSessionId, enteredAt)
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
        val foundation = dataFoundation ?: return
        val exitedAt = Clock.System.now()
        viewModelScope.launch {
            try {
                foundation.closeTimeEntryUseCase(rangeSessionId, enteredAt, exitedAt)
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

    private fun persistLastViewedStep(index: Int) {
        val foundation = dataFoundation ?: return
        viewModelScope.launch {
            try {
                foundation.updateLastViewedStepUseCase(rangeSessionId, index)
            } catch (_: Exception) {
                // fire-and-forget: persistence failures don't surface to the user
            }
        }
    }

    companion object {
        fun factory(
            rangeSessionId: String,
            dataFoundation: DataFoundation?,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(RangeSessionViewModel::class.java)) {
                    "Unsupported ViewModel class: ${modelClass.name}"
                }
                return RangeSessionViewModel(
                    rangeSessionId = rangeSessionId,
                    dataFoundation = dataFoundation,
                ) as T
            }
        }
    }
}
