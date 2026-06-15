package com.loganmartlew.rangework.android.ui

import com.loganmartlew.rangework.android.ui.theme.ThemeMode
import com.loganmartlew.rangework.android.ui.theme.ThemePreferenceStore
import com.loganmartlew.rangework.shared.auth.AuthState
import com.loganmartlew.rangework.shared.model.DistanceUnit
import com.loganmartlew.rangework.shared.model.MeasurementPreferences
import com.loganmartlew.rangework.shared.model.SpeedUnit
import com.loganmartlew.rangework.shared.model.UnitSystem
import com.loganmartlew.rangework.shared.repository.MeasurementPreferencesRepository
import com.loganmartlew.rangework.shared.usecase.GetMeasurementPreferencesUseCase
import com.loganmartlew.rangework.shared.usecase.SaveMeasurementPreferencesUseCase
import com.loganmartlew.rangework.shared.data.DataFoundation
import com.loganmartlew.rangework.shared.usecase.DeletePracticeSessionUseCase
import com.loganmartlew.rangework.shared.usecase.DeletePracticeUnitUseCase
import com.loganmartlew.rangework.shared.usecase.GetPracticeSessionUseCase
import com.loganmartlew.rangework.shared.usecase.GetPracticeUnitUseCase
import com.loganmartlew.rangework.shared.usecase.ListPracticeSessionsUseCase
import com.loganmartlew.rangework.shared.usecase.ListPracticeUnitsUseCase
import com.loganmartlew.rangework.shared.usecase.SavePracticeSessionUseCase
import com.loganmartlew.rangework.shared.usecase.SavePracticeUnitUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun signedInLoadsPreferencesFromRepository() = runTest {
        val repo = FakeMeasurementPreferencesRepository(
            storedPreferences = MeasurementPreferences.Metric,
        )
        val viewModel = createViewModel(repo)

        viewModel.onAuthStateChanged(signedIn())
        advanceUntilIdle()

        assertEquals(UnitSystem.METRIC, viewModel.uiState.value.measurementPreferences.unitSystem)
        assertEquals(DistanceUnit.METERS, viewModel.uiState.value.measurementPreferences.distanceUnit)
    }

    @Test
    fun selectDistanceUnitSetsCustomSystem() = runTest {
        val repo = FakeMeasurementPreferencesRepository()
        val viewModel = createViewModel(repo)

        viewModel.onAuthStateChanged(signedIn())
        advanceUntilIdle()

        viewModel.selectDistanceUnit(DistanceUnit.METERS)
        advanceUntilIdle()

        assertEquals(UnitSystem.CUSTOM, viewModel.uiState.value.measurementPreferences.unitSystem)
        assertEquals(DistanceUnit.METERS, viewModel.uiState.value.measurementPreferences.distanceUnit)
    }

    @Test
    fun selectSpeedUnitSetsCustomSystem() = runTest {
        val repo = FakeMeasurementPreferencesRepository()
        val viewModel = createViewModel(repo)

        viewModel.onAuthStateChanged(signedIn())
        advanceUntilIdle()

        viewModel.selectSpeedUnit(SpeedUnit.KILOMETRES_PER_HOUR)
        advanceUntilIdle()

        assertEquals(UnitSystem.CUSTOM, viewModel.uiState.value.measurementPreferences.unitSystem)
        assertEquals(SpeedUnit.KILOMETRES_PER_HOUR, viewModel.uiState.value.measurementPreferences.speedUnit)
    }

    @Test
    fun setThemeModeDarkUpdatesState() = runTest {
        val themeStore = FakeThemePreferenceStore()
        val viewModel = createViewModel(themePreferenceStore = themeStore)

        viewModel.setThemeMode(ThemeMode.DARK)
        advanceUntilIdle()

        assertEquals(ThemeMode.DARK, viewModel.uiState.value.themeMode)
        assertEquals(ThemeMode.DARK, themeStore.lastSet)
    }

    @Test
    fun signedOutResetsPreferencesToDefaults() = runTest {
        val repo = FakeMeasurementPreferencesRepository(
            storedPreferences = MeasurementPreferences.Metric,
        )
        val viewModel = createViewModel(repo)

        viewModel.onAuthStateChanged(signedIn())
        advanceUntilIdle()

        viewModel.onAuthStateChanged(AuthState.SignedOut)
        advanceUntilIdle()

        assertEquals(MeasurementPreferences.Imperial, viewModel.uiState.value.measurementPreferences)
    }

    private fun createViewModel(
        repo: FakeMeasurementPreferencesRepository = FakeMeasurementPreferencesRepository(),
        themePreferenceStore: FakeThemePreferenceStore = FakeThemePreferenceStore(),
    ): SettingsViewModel = SettingsViewModel(
        dataFoundation = fakeDataFoundation(repo),
        themePreferenceStore = themePreferenceStore,
    )

    private fun signedIn() = AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com")
}

private fun fakeDataFoundation(repo: MeasurementPreferencesRepository): DataFoundation {
    val emptyUnitRepo = object : com.loganmartlew.rangework.shared.repository.PracticeUnitRepository {
        override suspend fun listPracticeUnits() = emptyList<com.loganmartlew.rangework.shared.model.PracticeUnit>()
        override suspend fun getPracticeUnit(unitId: String) = null
        override suspend fun savePracticeUnit(draft: com.loganmartlew.rangework.shared.model.PracticeUnitDraft, unitId: String?) = throw NotImplementedError()
        override suspend fun deletePracticeUnit(unitId: String) = Unit
    }
    val emptySessionRepo = object : com.loganmartlew.rangework.shared.repository.PracticeSessionRepository {
        override suspend fun listPracticeSessions() = emptyList<com.loganmartlew.rangework.shared.model.PracticeSession>()
        override suspend fun getPracticeSession(sessionId: String) = null
        override suspend fun savePracticeSession(draft: com.loganmartlew.rangework.shared.model.PracticeSessionDraft, sessionId: String?) = throw NotImplementedError()
        override suspend fun deletePracticeSession(sessionId: String) = Unit
    }
    return DataFoundation(
        listPracticeUnitsUseCase = ListPracticeUnitsUseCase(emptyUnitRepo),
        getPracticeUnitUseCase = GetPracticeUnitUseCase(emptyUnitRepo),
        savePracticeUnitUseCase = SavePracticeUnitUseCase(emptyUnitRepo),
        deletePracticeUnitUseCase = DeletePracticeUnitUseCase(emptyUnitRepo),
        listPracticeSessionsUseCase = ListPracticeSessionsUseCase(emptySessionRepo),
        getPracticeSessionUseCase = GetPracticeSessionUseCase(emptySessionRepo),
        savePracticeSessionUseCase = SavePracticeSessionUseCase(emptySessionRepo),
        deletePracticeSessionUseCase = DeletePracticeSessionUseCase(emptySessionRepo),
        getMeasurementPreferencesUseCase = GetMeasurementPreferencesUseCase(repo),
        saveMeasurementPreferencesUseCase = SaveMeasurementPreferencesUseCase(repo),
    )
}

private class FakeMeasurementPreferencesRepository(
    var storedPreferences: MeasurementPreferences = MeasurementPreferences.Imperial,
) : MeasurementPreferencesRepository {
    override suspend fun getMeasurementPreferences(): MeasurementPreferences = storedPreferences

    override suspend fun saveMeasurementPreferences(preferences: MeasurementPreferences): MeasurementPreferences {
        storedPreferences = preferences
        return preferences
    }
}

private class FakeThemePreferenceStore : ThemePreferenceStore {
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    override val themeMode: Flow<ThemeMode> = _themeMode

    var lastSet: ThemeMode? = null

    override suspend fun setThemeMode(mode: ThemeMode) {
        lastSet = mode
        _themeMode.value = mode
    }
}
