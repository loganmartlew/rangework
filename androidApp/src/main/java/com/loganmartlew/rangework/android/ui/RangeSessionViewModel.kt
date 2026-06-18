package com.loganmartlew.rangework.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.loganmartlew.rangework.shared.data.DataFoundation
import com.loganmartlew.rangework.shared.model.RangeSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class RangeSessionUiState(
    val rangeSession: RangeSession? = null,
    val currentStepIndex: Int = 0,
    val isLoading: Boolean = true,
    val statusMessage: String? = null,
)

class RangeSessionViewModel(
    private val rangeSessionId: String,
    private val dataFoundation: DataFoundation?,
) : ViewModel() {

    private val _uiState = MutableStateFlow(RangeSessionUiState())
    val uiState: StateFlow<RangeSessionUiState> = _uiState.asStateFlow()

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
