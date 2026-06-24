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
import com.loganmartlew.rangework.shared.repository.ClubRepository
import com.loganmartlew.rangework.shared.repository.MeasurementPreferencesRepository
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
        measurementPreferencesRepository = repo,
        clubRepository = clubRepo,
        themePreferenceStore = themePreferenceStore,
    )

    private fun signedIn() = AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com")
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
) : MeasurementPreferencesRepository() {
    override suspend fun get(): MeasurementPreferences = storedPreferences

    override suspend fun persist(validated: MeasurementPreferences): MeasurementPreferences {
        storedPreferences = validated
        return validated
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
