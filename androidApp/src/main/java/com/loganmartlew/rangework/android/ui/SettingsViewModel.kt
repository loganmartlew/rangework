package com.loganmartlew.rangework.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.loganmartlew.rangework.android.ui.theme.ThemeMode
import com.loganmartlew.rangework.android.ui.theme.ThemePreferenceStore
import com.loganmartlew.rangework.shared.auth.AuthState
import com.loganmartlew.rangework.shared.data.DataFoundation
import com.loganmartlew.rangework.shared.model.DistanceUnit
import com.loganmartlew.rangework.shared.model.MeasurementPreferences
import com.loganmartlew.rangework.shared.model.SpeedUnit
import com.loganmartlew.rangework.shared.model.UnitSystem
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class SettingsUiState(
    val dataConfigured: Boolean,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val measurementPreferences: MeasurementPreferences = MeasurementPreferences.Imperial,
    val isWorking: Boolean = false,
    val statusMessage: String? = null,
)

class SettingsViewModel(
    private val dataFoundation: DataFoundation?,
    private val themePreferenceStore: ThemePreferenceStore,
) : ViewModel() {
    private var activeUserId: String? = null
    private val saveMutex = Mutex()
    private var saveToken = 0

    private val _uiState = androidx.compose.runtime.mutableStateOf(
        SettingsUiState(
            dataConfigured = dataFoundation != null,
        ),
    )
    val uiState: androidx.compose.runtime.State<SettingsUiState> = _uiState

    init {
        viewModelScope.launch {
            themePreferenceStore.themeMode.collect { mode ->
                _uiState.value = _uiState.value.copy(themeMode = mode)
            }
        }
    }

    fun onAuthStateChanged(authState: AuthState) {
        when (authState) {
            is AuthState.SignedIn -> {
                activeUserId = authState.userId
                loadMeasurementPreferences()
            }

            AuthState.Restoring -> {
                _uiState.value = _uiState.value.copy(isWorking = true)
            }

            AuthState.SignedOut,
            is AuthState.Error,
            -> {
                activeUserId = null
                _uiState.value = _uiState.value.copy(
                    isWorking = false,
                    measurementPreferences = MeasurementPreferences.Imperial,
                    statusMessage = null,
                )
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            themePreferenceStore.setThemeMode(mode)
        }
    }

    fun selectDistanceUnit(distanceUnit: DistanceUnit) {
        val current = _uiState.value.measurementPreferences
        saveMeasurementPreferences(
            current.copy(
                unitSystem = UnitSystem.CUSTOM,
                distanceUnit = distanceUnit,
            ),
        )
    }

    fun selectSpeedUnit(speedUnit: SpeedUnit) {
        val current = _uiState.value.measurementPreferences
        saveMeasurementPreferences(
            current.copy(
                unitSystem = UnitSystem.CUSTOM,
                speedUnit = speedUnit,
            ),
        )
    }

    private fun loadMeasurementPreferences() {
        val foundation = dataFoundation ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isWorking = true)
            try {
                val prefs = foundation.getMeasurementPreferencesUseCase()
                _uiState.value = _uiState.value.copy(
                    isWorking = false,
                    measurementPreferences = prefs,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isWorking = false,
                    statusMessage = "Could not load preferences.",
                )
            }
        }
    }

    private fun saveMeasurementPreferences(preferences: MeasurementPreferences) {
        val foundation = dataFoundation ?: return
        val previous = _uiState.value.measurementPreferences
        // Optimistically reflect the change immediately, and tag this request so a
        // slower in-flight save can't clobber the state with a stale result.
        val token = ++saveToken
        _uiState.value = _uiState.value.copy(measurementPreferences = preferences)
        viewModelScope.launch {
            try {
                // Serialize persistence so concurrent fast toggles apply in click order
                // instead of racing (which left the UI on an earlier selection).
                val saved = saveMutex.withLock {
                    foundation.saveMeasurementPreferencesUseCase(preferences)
                }
                if (token == saveToken) {
                    _uiState.value = _uiState.value.copy(measurementPreferences = saved)
                }
            } catch (e: Exception) {
                if (token == saveToken) {
                    _uiState.value = _uiState.value.copy(
                        measurementPreferences = previous,
                        statusMessage = "Could not save preferences.",
                    )
                }
            }
        }
    }

    companion object {
        fun factory(
            dataFoundation: DataFoundation?,
            themePreferenceStore: ThemePreferenceStore,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                    "Unsupported ViewModel class: ${modelClass.name}"
                }
                return SettingsViewModel(
                    dataFoundation = dataFoundation,
                    themePreferenceStore = themePreferenceStore,
                ) as T
            }
        }
    }
}
