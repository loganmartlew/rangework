package com.loganmartlew.rangework.android.ui

import com.loganmartlew.rangework.android.ui.theme.ThemeMode
import com.loganmartlew.rangework.android.ui.theme.ThemePreferenceStore
import com.loganmartlew.rangework.shared.auth.AuthState
import com.loganmartlew.rangework.shared.model.Club
import com.loganmartlew.rangework.shared.model.ClubCategory
import com.loganmartlew.rangework.shared.model.DistanceUnit
import com.loganmartlew.rangework.shared.model.MeasurementPreferences
import com.loganmartlew.rangework.shared.model.SpeedUnit
import com.loganmartlew.rangework.shared.model.UnitSystem
import com.loganmartlew.rangework.shared.model.ActiveRangeSessionSummary
import com.loganmartlew.rangework.shared.model.CompletedRangeSessionSummary
import com.loganmartlew.rangework.shared.model.RangeSession
import com.loganmartlew.rangework.shared.repository.ClubRepository
import com.loganmartlew.rangework.shared.repository.MeasurementPreferencesRepository
import com.loganmartlew.rangework.shared.repository.RangeSessionRepository
import com.loganmartlew.rangework.shared.usecase.AbandonRangeSessionUseCase
import com.loganmartlew.rangework.shared.usecase.CloseTimeEntryUseCase
import com.loganmartlew.rangework.shared.usecase.DeletePracticeSessionUseCase
import com.loganmartlew.rangework.shared.usecase.DeletePracticeUnitUseCase
import com.loganmartlew.rangework.shared.usecase.DuplicatePracticeSessionUseCase
import com.loganmartlew.rangework.shared.usecase.DuplicateUnitUseCase
import com.loganmartlew.rangework.shared.usecase.FinishRangeSessionUseCase
import com.loganmartlew.rangework.shared.usecase.GetClubCatalogUseCase
import com.loganmartlew.rangework.shared.usecase.GetElapsedSecondsUseCase
import com.loganmartlew.rangework.shared.usecase.GetEnabledClubsUseCase
import com.loganmartlew.rangework.shared.usecase.GetMeasurementPreferencesUseCase
import com.loganmartlew.rangework.shared.usecase.GetPracticeSessionUseCase
import com.loganmartlew.rangework.shared.usecase.GetPracticeUnitUseCase
import com.loganmartlew.rangework.shared.usecase.GetRangeSessionUseCase
import com.loganmartlew.rangework.shared.usecase.HasActiveRangeSessionsUseCase
import com.loganmartlew.rangework.shared.usecase.ListActiveRangeSessionsUseCase
import com.loganmartlew.rangework.shared.usecase.ListCompletedRangeSessionsUseCase
import com.loganmartlew.rangework.shared.usecase.ListPracticeSessionsUseCase
import com.loganmartlew.rangework.shared.usecase.ListPracticeUnitsUseCase
import com.loganmartlew.rangework.shared.usecase.OverrideStepClubUseCase
import com.loganmartlew.rangework.shared.usecase.RecordTimeEntryUseCase
import com.loganmartlew.rangework.shared.usecase.SaveMeasurementPreferencesUseCase
import com.loganmartlew.rangework.shared.usecase.SavePracticeSessionUseCase
import com.loganmartlew.rangework.shared.usecase.SavePracticeUnitUseCase
import com.loganmartlew.rangework.shared.usecase.SetClubEnabledUseCase
import com.loganmartlew.rangework.shared.usecase.StartRangeSessionUseCase
import com.loganmartlew.rangework.shared.usecase.ToggleStepCompleteUseCase
import com.loganmartlew.rangework.shared.usecase.UpdateLastViewedStepUseCase
import com.loganmartlew.rangework.shared.data.DataFoundation
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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
    fun rapidDistanceTogglesEndOnLastSelection() = runTest {
        val repo = FakeMeasurementPreferencesRepository()
        val viewModel = createViewModel(repo)

        viewModel.onAuthStateChanged(signedIn())
        advanceUntilIdle()

        // Fire several toggles back-to-back without letting saves settle in between.
        viewModel.selectDistanceUnit(DistanceUnit.METERS)
        viewModel.selectDistanceUnit(DistanceUnit.YARDS)
        viewModel.selectDistanceUnit(DistanceUnit.METERS)
        advanceUntilIdle()

        assertEquals(DistanceUnit.METERS, viewModel.uiState.value.measurementPreferences.distanceUnit)
        assertEquals(DistanceUnit.METERS, repo.storedPreferences.distanceUnit)
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

    @Test
    fun signedInLoadsClubCatalogAndEnabledCodes() = runTest {
        val clubRepo = FakeClubRepository(
            catalog = listOf(sampleClub("driver"), sampleClub("putter")),
            enabledCodes = mutableSetOf("driver"),
        )
        val viewModel = createViewModel(clubRepo = clubRepo)

        viewModel.onAuthStateChanged(signedIn())
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.clubCatalog.size)
        assertTrue("driver" in viewModel.uiState.value.enabledClubCodes)
        assertFalse("putter" in viewModel.uiState.value.enabledClubCodes)
    }

    @Test
    fun setClubEnabledOptimisticallyUpdatesAndPersists() = runTest {
        val clubRepo = FakeClubRepository(
            catalog = listOf(sampleClub("driver"), sampleClub("putter")),
            enabledCodes = mutableSetOf("driver"),
        )
        val viewModel = createViewModel(clubRepo = clubRepo)

        viewModel.onAuthStateChanged(signedIn())
        advanceUntilIdle()

        viewModel.setClubEnabled("putter", true)
        advanceUntilIdle()

        assertTrue("putter" in viewModel.uiState.value.enabledClubCodes)
        assertTrue("putter" in clubRepo.enabledCodes)
    }

    @Test
    fun setClubDisabledRemovesFromEnabledSet() = runTest {
        val clubRepo = FakeClubRepository(
            catalog = listOf(sampleClub("driver"), sampleClub("putter")),
            enabledCodes = mutableSetOf("driver", "putter"),
        )
        val viewModel = createViewModel(clubRepo = clubRepo)

        viewModel.onAuthStateChanged(signedIn())
        advanceUntilIdle()

        viewModel.setClubEnabled("driver", false)
        advanceUntilIdle()

        assertFalse("driver" in viewModel.uiState.value.enabledClubCodes)
        assertFalse("driver" in clubRepo.enabledCodes)
    }

    @Test
    fun signedOutClearsClubs() = runTest {
        val clubRepo = FakeClubRepository(
            catalog = listOf(sampleClub("driver")),
            enabledCodes = mutableSetOf("driver"),
        )
        val viewModel = createViewModel(clubRepo = clubRepo)

        viewModel.onAuthStateChanged(signedIn())
        advanceUntilIdle()
        viewModel.onAuthStateChanged(AuthState.SignedOut)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.clubCatalog.isEmpty())
        assertTrue(viewModel.uiState.value.enabledClubCodes.isEmpty())
    }

    @Test
    fun disableAllClubsClearsEnabledSet() = runTest {
        val clubRepo = FakeClubRepository(
            catalog = listOf(sampleClub("driver"), sampleClub("putter"), sampleClub("seven_iron")),
            enabledCodes = mutableSetOf("driver", "putter", "seven_iron"),
        )
        val viewModel = createViewModel(clubRepo = clubRepo)

        viewModel.onAuthStateChanged(signedIn())
        advanceUntilIdle()

        viewModel.disableAllClubs()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.enabledClubCodes.isEmpty())
        assertTrue(clubRepo.enabledCodes.isEmpty())
    }

    @Test
    fun enableCommonBagEnablesOnlyCommonCodes() = runTest {
        val clubRepo = FakeClubRepository(
            catalog = listOf(
                sampleClub("driver"),
                sampleClub("two_wood"),
                sampleClub("putter"),
            ),
            enabledCodes = mutableSetOf(),
        )
        val viewModel = createViewModel(clubRepo = clubRepo)

        viewModel.onAuthStateChanged(signedIn())
        advanceUntilIdle()

        viewModel.enableCommonBag()
        advanceUntilIdle()

        assertTrue("driver" in viewModel.uiState.value.enabledClubCodes)
        assertFalse("two_wood" in viewModel.uiState.value.enabledClubCodes)
        assertTrue("putter" in viewModel.uiState.value.enabledClubCodes)
    }

    @Test
    fun enabledClubCountComputedFromState() = runTest {
        val clubRepo = FakeClubRepository(
            catalog = listOf(sampleClub("driver"), sampleClub("putter")),
            enabledCodes = mutableSetOf("driver"),
        )
        val viewModel = createViewModel(clubRepo = clubRepo)

        viewModel.onAuthStateChanged(signedIn())
        advanceUntilIdle()

        val count = viewModel.uiState.value.enabledClubCount
        assertEquals(1, count.enabled)
        assertEquals(2, count.total)
    }

    private fun createViewModel(
        repo: FakeMeasurementPreferencesRepository = FakeMeasurementPreferencesRepository(),
        clubRepo: FakeClubRepository = FakeClubRepository(),
        themePreferenceStore: FakeThemePreferenceStore = FakeThemePreferenceStore(),
    ): SettingsViewModel = SettingsViewModel(
        dataFoundation = fakeDataFoundation(repo, clubRepo),
        themePreferenceStore = themePreferenceStore,
    )

    private fun signedIn() = AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com")
}

private fun fakeDataFoundation(
    repo: MeasurementPreferencesRepository,
    clubRepo: ClubRepository = FakeClubRepository(),
): DataFoundation {
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
    val nopRangeRepo = object : RangeSessionRepository {
        override suspend fun startSession(rangeSessionId: String, sessionId: String): RangeSession = error("nop")
        override suspend fun getSession(rangeSessionId: String): RangeSession? = null
        override suspend fun listActiveSessions(): List<ActiveRangeSessionSummary> = emptyList()
        override suspend fun listCompletedSessions(sessionId: String): List<CompletedRangeSessionSummary> = emptyList()
        override suspend fun toggleStepComplete(rangeSessionId: String, stepIndex: Int, completed: Boolean): RangeSession = error("nop")
        override suspend fun overrideStepClub(rangeSessionId: String, stepIndex: Int, clubCode: String): RangeSession = error("nop")
        override suspend fun updateLastViewedStep(rangeSessionId: String, stepIndex: Int) = Unit
        override suspend fun finishSession(rangeSessionId: String): RangeSession = error("nop")
        override suspend fun abandonSession(rangeSessionId: String) = Unit
        override suspend fun recordTimeEntry(rangeSessionId: String, enteredAt: kotlinx.datetime.Instant) = Unit
        override suspend fun closeTimeEntry(rangeSessionId: String, enteredAt: kotlinx.datetime.Instant, exitedAt: kotlinx.datetime.Instant) = Unit
        override suspend fun getElapsedSeconds(rangeSessionId: String): Long = 0L
        override suspend fun hasActiveSessionsForTemplate(sessionId: String): Boolean = false
    }
    return DataFoundation(
        listPracticeUnitsUseCase = ListPracticeUnitsUseCase(emptyUnitRepo),
        getPracticeUnitUseCase = GetPracticeUnitUseCase(emptyUnitRepo),
        savePracticeUnitUseCase = SavePracticeUnitUseCase(emptyUnitRepo),
        deletePracticeUnitUseCase = DeletePracticeUnitUseCase(emptyUnitRepo),
        duplicatePracticeUnitUseCase = DuplicateUnitUseCase(
            getPracticeUnitUseCase = GetPracticeUnitUseCase(emptyUnitRepo),
            savePracticeUnitUseCase = SavePracticeUnitUseCase(emptyUnitRepo),
        ),
        listPracticeSessionsUseCase = ListPracticeSessionsUseCase(emptySessionRepo),
        getPracticeSessionUseCase = GetPracticeSessionUseCase(emptySessionRepo),
        savePracticeSessionUseCase = SavePracticeSessionUseCase(emptySessionRepo),
        deletePracticeSessionUseCase = DeletePracticeSessionUseCase(emptySessionRepo),
        duplicatePracticeSessionUseCase = DuplicatePracticeSessionUseCase(
            getPracticeSessionUseCase = GetPracticeSessionUseCase(emptySessionRepo),
            savePracticeSessionUseCase = SavePracticeSessionUseCase(emptySessionRepo),
        ),
        getMeasurementPreferencesUseCase = GetMeasurementPreferencesUseCase(repo),
        saveMeasurementPreferencesUseCase = SaveMeasurementPreferencesUseCase(repo),
        getClubCatalogUseCase = GetClubCatalogUseCase(clubRepo),
        getEnabledClubsUseCase = GetEnabledClubsUseCase(clubRepo),
        setClubEnabledUseCase = SetClubEnabledUseCase(clubRepo),
        startRangeSessionUseCase = StartRangeSessionUseCase(nopRangeRepo),
        getRangeSessionUseCase = GetRangeSessionUseCase(nopRangeRepo),
        listActiveRangeSessionsUseCase = ListActiveRangeSessionsUseCase(nopRangeRepo),
        listCompletedRangeSessionsUseCase = ListCompletedRangeSessionsUseCase(nopRangeRepo),
        toggleStepCompleteUseCase = ToggleStepCompleteUseCase(nopRangeRepo),
        overrideStepClubUseCase = OverrideStepClubUseCase(nopRangeRepo),
        updateLastViewedStepUseCase = UpdateLastViewedStepUseCase(nopRangeRepo),
        finishRangeSessionUseCase = FinishRangeSessionUseCase(nopRangeRepo),
        abandonRangeSessionUseCase = AbandonRangeSessionUseCase(nopRangeRepo),
        recordTimeEntryUseCase = RecordTimeEntryUseCase(nopRangeRepo),
        closeTimeEntryUseCase = CloseTimeEntryUseCase(nopRangeRepo),
        getElapsedSecondsUseCase = GetElapsedSecondsUseCase(nopRangeRepo),
        hasActiveRangeSessionsUseCase = HasActiveRangeSessionsUseCase(nopRangeRepo),
    )
}

private class FakeClubRepository(
    val catalog: List<Club> = emptyList(),
    val enabledCodes: MutableSet<String> = mutableSetOf(),
) : ClubRepository {
    override suspend fun listCatalog(): List<Club> = catalog
    override suspend fun getEnabledClubCodes(): Set<String> = enabledCodes.toSet()
    override suspend fun setClubEnabled(code: String, enabled: Boolean) {
        if (enabled) enabledCodes.add(code) else enabledCodes.remove(code)
    }
}

private fun sampleClub(code: String) = Club(
    code = code,
    displayName = code.replaceFirstChar { it.uppercase() },
    category = ClubCategory.IRON,
    sortOrder = 1,
)

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
