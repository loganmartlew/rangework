package com.loganmartlew.rangework.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.loganmartlew.rangework.shared.model.RangeSession
import com.loganmartlew.rangework.shared.model.completedBalls
import com.loganmartlew.rangework.shared.model.completedStepCount
import com.loganmartlew.rangework.shared.model.completionPercentage
import com.loganmartlew.rangework.shared.model.executionBlocks
import com.loganmartlew.rangework.shared.model.totalBalls
import com.loganmartlew.rangework.shared.model.totalStepCount
import com.loganmartlew.rangework.shared.recording.RangeSessionRecorder
import com.loganmartlew.rangework.shared.recording.RecordingResult
import com.loganmartlew.rangework.shared.repository.RangeSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class CompletedRangeSessionUiState(
    val rangeSession: RangeSession? = null,
    val isLoading: Boolean = true,
    val statusMessage: String? = null,
    val notification: String? = null,
    val elapsedSeconds: Long = 0,
    val isSavingSessionNote: Boolean = false,
    val savingBlockNoteIndices: Set<Int> = emptySet(),
)

/**
 * The post-completion editing surface (P1): loads a completed session by id and
 * exposes the two still-editable prose fields — the session note and each block
 * note — through the recorder. Manual counts are frozen once the session leaves
 * Active (freeze matrix), so this VM offers no count edit.
 *
 * Deliberately *not* [RangeSessionViewModel]: that one records time entries on
 * screen enter, which would credit practice time to a finished session. This is a
 * small, timer-free VM of its own.
 */
class CompletedRangeSessionViewModel(
    private val rangeSessionId: String,
    private val rangeSessionRepository: RangeSessionRepository?,
    private val rangeSessionRecorder: RangeSessionRecorder?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CompletedRangeSessionUiState())
    val uiState: StateFlow<CompletedRangeSessionUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    private fun load() {
        val repository = rangeSessionRepository ?: run {
            _uiState.value = CompletedRangeSessionUiState(
                isLoading = false,
                statusMessage = "Session data not available.",
            )
            return
        }
        viewModelScope.launch {
            try {
                val session = repository.getSession(rangeSessionId)
                val elapsed = try {
                    repository.getElapsedSeconds(rangeSessionId)
                } catch (_: Exception) {
                    0L
                }
                _uiState.value = CompletedRangeSessionUiState(
                    rangeSession = session,
                    isLoading = false,
                    statusMessage = if (session == null) "Session not found." else null,
                    elapsedSeconds = elapsed,
                )
            } catch (exception: Exception) {
                _uiState.value = CompletedRangeSessionUiState(
                    isLoading = false,
                    statusMessage = "Failed to load session: ${exception.message ?: "unknown error"}",
                )
            }
        }
    }

    /** Total balls / completed balls / steps / completion — the finish-style header. */
    fun stats(): CompletedRangeSessionStats? {
        val session = _uiState.value.rangeSession ?: return null
        return CompletedRangeSessionStats(
            totalBalls = session.totalBalls(),
            completedBalls = session.completedBalls(),
            completedStepCount = session.completedStepCount(),
            totalStepCount = session.totalStepCount(),
            completionPercentage = session.completionPercentage(),
            elapsedSeconds = _uiState.value.elapsedSeconds,
        )
    }

    fun saveSessionNote(note: String?) {
        val recorder = rangeSessionRecorder ?: return
        if (_uiState.value.rangeSession == null) return
        _uiState.value = _uiState.value.copy(isSavingSessionNote = true)
        viewModelScope.launch {
            val result = runRecording { recorder.saveSessionNote(rangeSessionId, note) }
            if (result is RecordingResult.Success) {
                _uiState.value = _uiState.value.copy(
                    rangeSession = result.value,
                    isSavingSessionNote = false,
                )
            } else {
                _uiState.value = _uiState.value.copy(
                    isSavingSessionNote = false,
                    notification = "Couldn't save note. Please try again.",
                )
            }
        }
    }

    fun saveBlockNote(blockIndex: Int, note: String?) {
        val session = _uiState.value.rangeSession ?: return
        val recorder = rangeSessionRecorder ?: return
        val block = session.snapshot.executionBlocks().getOrNull(blockIndex) ?: return
        val unitIndex = block.unitIndex
        _uiState.value = _uiState.value.copy(
            savingBlockNoteIndices = _uiState.value.savingBlockNoteIndices + unitIndex,
        )
        viewModelScope.launch {
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

    fun consumeNotification() {
        _uiState.value = _uiState.value.copy(notification = null)
    }

    private suspend fun <T> runRecording(block: suspend () -> RecordingResult<T>): RecordingResult<T>? =
        try {
            block()
        } catch (_: Exception) {
            null
        }

    companion object {
        fun factory(
            rangeSessionId: String,
            rangeSessionRepository: RangeSessionRepository?,
            rangeSessionRecorder: RangeSessionRecorder?,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(CompletedRangeSessionViewModel::class.java)) {
                    "Unsupported ViewModel class: ${modelClass.name}"
                }
                return CompletedRangeSessionViewModel(
                    rangeSessionId = rangeSessionId,
                    rangeSessionRepository = rangeSessionRepository,
                    rangeSessionRecorder = rangeSessionRecorder,
                ) as T
            }
        }
    }
}

data class CompletedRangeSessionStats(
    val totalBalls: Int,
    val completedBalls: Int,
    val completedStepCount: Int,
    val totalStepCount: Int,
    val completionPercentage: Double,
    val elapsedSeconds: Long,
)
