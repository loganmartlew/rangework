package com.loganmartlew.rangework.shared.usecase

import com.loganmartlew.rangework.shared.model.Club
import com.loganmartlew.rangework.shared.model.ClubCategory
import com.loganmartlew.rangework.shared.model.DistanceUnit
import com.loganmartlew.rangework.shared.model.MeasurementPreferences
import com.loganmartlew.rangework.shared.model.SpeedUnit
import com.loganmartlew.rangework.shared.model.UnitSystem
import com.loganmartlew.rangework.shared.repository.ClubRepository
import com.loganmartlew.rangework.shared.repository.MeasurementPreferencesRepository
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DataFoundationUseCaseTest {

    // ── ClubRepository conformance ───────────────────────────────────────────

    @Test
    fun clubRepositoryReturnsCatalog() = kotlinx.coroutines.test.runTest {
        val repository = RecordingClubRepository(
            catalog = listOf(
                Club(code = "driver", displayName = "Driver", category = ClubCategory.WOOD, sortOrder = 1),
                Club(code = "putter", displayName = "Putter", category = ClubCategory.PUTTER, sortOrder = 30),
            ),
        )

        val result = repository.listCatalog()

        assertEquals(2, result.size)
        assertEquals("driver", result.first().code)
    }

    @Test
    fun clubRepositoryPersistsEnableAction() = kotlinx.coroutines.test.runTest {
        val repository = RecordingClubRepository(enabledCodes = mutableSetOf("driver"))

        repository.setClubEnabled("putter", true)

        assertTrue("putter" in repository.enabledCodes)
    }

    @Test
    fun clubRepositoryPersistsDisableAction() = kotlinx.coroutines.test.runTest {
        val repository = RecordingClubRepository(enabledCodes = mutableSetOf("driver", "putter"))

        repository.setClubEnabled("driver", false)

        assertFalse("driver" in repository.enabledCodes)
        assertTrue("putter" in repository.enabledCodes)
    }

    @Test
    fun clubRepositoryReturnsEnabledCodes() = kotlinx.coroutines.test.runTest {
        val repository = RecordingClubRepository(enabledCodes = mutableSetOf("driver", "seven_iron"))

        val result = repository.getEnabledClubCodes()

        assertEquals(setOf("driver", "seven_iron"), result)
    }

    // ── MeasurementPreferencesRepository seam ───────────────────────────────

    @Test
    fun measurementPreferencesSeamNormalizesPresetsOnSave() = kotlinx.coroutines.test.runTest {
        val repository = RecordingMeasurementPreferencesRepository()

        repository.save(
            MeasurementPreferences(
                unitSystem = UnitSystem.METRIC,
                distanceUnit = DistanceUnit.YARDS,
                speedUnit = SpeedUnit.MILES_PER_HOUR,
            ),
        )

        assertEquals(MeasurementPreferences.Metric, repository.lastPersistedPreferences)
    }
}
    var lastPersistedPreferences: MeasurementPreferences? = null

    override suspend fun get(): MeasurementPreferences = MeasurementPreferences.Imperial

    override suspend fun persist(validated: MeasurementPreferences): MeasurementPreferences {
        lastPersistedPreferences = validated
        return validated
    }
}

private class RecordingClubRepository(
    val catalog: List<Club> = emptyList(),
    val enabledCodes: MutableSet<String> = mutableSetOf(),
) : ClubRepository {
    override suspend fun listCatalog(): List<Club> = catalog
    override suspend fun getEnabledClubCodes(): Set<String> = enabledCodes.toSet()
    override suspend fun setClubEnabled(code: String, enabled: Boolean) {
        if (enabled) enabledCodes.add(code) else enabledCodes.remove(code)
    }
}
