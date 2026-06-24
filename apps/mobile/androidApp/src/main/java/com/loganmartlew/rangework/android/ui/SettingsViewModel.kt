package com.loganmartlew.rangework.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.loganmartlew.rangework.android.ui.theme.ThemeMode
import com.loganmartlew.rangework.android.ui.theme.ThemePreferenceStore
import com.loganmartlew.rangework.shared.auth.AuthState
import com.loganmartlew.rangework.shared.model.Club
import com.loganmartlew.rangework.shared.model.EnabledClubCount
import com.loganmartlew.rangework.shared.model.DistanceUnit
import com.loganmartlew.rangework.shared.model.MeasurementPreferences
import com.loganmartlew.rangework.shared.model.SpeedUnit
import com.loganmartlew.rangework.shared.model.UnitSystem
import com.loganmartlew.rangework.shared.repository.ClubRepository
import com.loganmartlew.rangework.shared.repository.MeasurementPreferencesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

data class SettingsUiState(
    val dataConfigured: Boolean,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val measurementPreferences: MeasurementPreferences = MeasurementPreferences.Imperial,
    val clubCatalog: List<Club> = emptyList(),
    val enabledClubCodes: Set<String> = emptySet(),
    val isWorking: Boolean = false,
    val statusMessage: String? = null,
) {
    val enabledClubCount: EnabledClubCount
        get() = EnabledClubCount.from(clubCatalog, enabledClubCodes)
}

class SettingsViewModel(
    private val measurementPreferencesRepository: MeasurementPreferencesRepository?,
    private val clubRepository: ClubRepository?,
    private val themePreferenceStore: ThemePreferenceStore,
) : ViewModel() {
    private var activeUserId: String? = null
    private val saveMutex = Mutex()
    private var saveToken = 0
    private val clubMutex = Mutex()
    private var clubToken = 0

    private val _uiState = MutableStateFlow(
        SettingsUiState(
            dataConfigured = measurementPreferencesRepository != null && clubRepository != null,
        ),
    )
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

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
                loadClubs()
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
                    clubCatalog = emptyList(),
                    enabledClubCodes = emptySet(),
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

    fun setClubEnabled(code: String, enabled: Boolean) {
        val repo = clubRepository ?: return
        val previous = _uiState.value.enabledClubCodes
        val token = ++clubToken
        _uiState.value = _uiState.value.copy(
            enabledClubCodes = if (enabled) previous + code else previous - code,
        )
        viewModelScope.launch {
            try {
                clubMutex.withLock {
                    repo.setClubEnabled(code, enabled)
                }
                if (token == clubToken) {
                    val refreshed = repo.getEnabledClubCodes()
                    _uiState.value = _uiState.value.copy(enabledClubCodes = refreshed)
                }
            } catch (e: Exception) {
                if (token == clubToken) {
                    _uiState.value = _uiState.value.copy(
                        enabledClubCodes = previous,
                        statusMessage = "Could not save club preference.",
                    )
                }
            }
        }
    }

    fun enableCommonBag() {
        val repo = clubRepository ?: return
        val catalog = _uiState.value.clubCatalog
        val targetCodes = catalog.map { it.code }.filter { it in COMMON_BAG_CODES }.toSet()
        val token = ++clubToken
        _uiState.value = _uiState.value.copy(enabledClubCodes = targetCodes)
        viewModelScope.launch {
            try {
                clubMutex.withLock {
                    catalog.forEach { club ->
                        repo.setClubEnabled(club.code, club.code in COMMON_BAG_CODES)
                    }
                }
                if (token == clubToken) {
                    val refreshed = repo.getEnabledClubCodes()
                    _uiState.value = _uiState.value.copy(enabledClubCodes = refreshed)
                }
            } catch (e: Exception) {
                if (token == clubToken) {
                    _uiState.value = _uiState.value.copy(
                        statusMessage = "Could not update club bag.",
                    )
                }
            }
        }
    }

    fun disableAllClubs() {
        val repo = clubRepository ?: return
        val catalog = _uiState.value.clubCatalog
        val token = ++clubToken
        _uiState.value = _uiState.value.copy(enabledClubCodes = emptySet())
        viewModelScope.launch {
            try {
                clubMutex.withLock {
                    catalog.forEach { club ->
                        repo.setClubEnabled(club.code, false)
                    }
                }
                if (token == clubToken) {
                    val refreshed = repo.getEnabledClubCodes()
                    _uiState.value = _uiState.value.copy(enabledClubCodes = refreshed)
                }
            } catch (e: Exception) {
                if (token == clubToken) {
                    _uiState.value = _uiState.value.copy(
                        statusMessage = "Could not update club bag.",
                    )
                }
            }
        }
    }

    private fun loadClubs() {
        val repo = clubRepository ?: return
        viewModelScope.launch {
            try {
                val catalog = repo.listCatalog()
                val enabled = repo.getEnabledClubCodes()
                _uiState.value = _uiState.value.copy(
                    clubCatalog = catalog,
                    enabledClubCodes = enabled,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Could not load club preferences.",
                )
            }
        }
    }

    private fun loadMeasurementPreferences() {
        val repo = measurementPreferencesRepository ?: return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isWorking = true)
            try {
                val prefs = repo.get()
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
        val repo = measurementPreferencesRepository ?: return
        val previous = _uiState.value.measurementPreferences
        val token = ++saveToken
        _uiState.value = _uiState.value.copy(measurementPreferences = preferences)
        viewModelScope.launch {
            try {
                val saved = saveMutex.withLock {
                    repo.save(preferences)
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
        private val COMMON_BAG_CODES = setOf(
            "driver", "three_wood", "five_wood",
            "four_iron", "five_iron", "six_iron", "seven_iron", "eight_iron", "nine_iron",
            "pitching_wedge", "gap_wedge", "sand_wedge", "lob_wedge",
            "putter",
        )

        fun factory(
            measurementPreferencesRepository: MeasurementPreferencesRepository?,
            clubRepository: ClubRepository?,
            themePreferenceStore: ThemePreferenceStore,
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(SettingsViewModel::class.java)) {
                    "Unsupported ViewModel class: ${modelClass.name}"
                }
                return SettingsViewModel(
                    measurementPreferencesRepository = measurementPreferencesRepository,
                    clubRepository = clubRepository,
                    themePreferenceStore = themePreferenceStore,
                ) as T
            }
        }
    }
}
