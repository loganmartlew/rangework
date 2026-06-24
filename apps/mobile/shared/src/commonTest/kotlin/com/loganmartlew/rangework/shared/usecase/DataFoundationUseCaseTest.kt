package com.loganmartlew.rangework.shared.usecase

import com.loganmartlew.rangework.shared.model.Club
import com.loganmartlew.rangework.shared.model.ClubCategory
import com.loganmartlew.rangework.shared.model.DistanceUnit
import com.loganmartlew.rangework.shared.model.MeasurementPreferences
import com.loganmartlew.rangework.shared.model.PracticeInstructionDraft
import com.loganmartlew.rangework.shared.model.PracticeSession
import com.loganmartlew.rangework.shared.model.PracticeSessionDraft
import com.loganmartlew.rangework.shared.model.PracticeSessionItemDraft
import com.loganmartlew.rangework.shared.model.PracticeUnit
import com.loganmartlew.rangework.shared.model.PracticeUnitDraft
import com.loganmartlew.rangework.shared.model.SpeedUnit
import com.loganmartlew.rangework.shared.model.UnitSystem
import com.loganmartlew.rangework.shared.repository.ClubRepository
import com.loganmartlew.rangework.shared.repository.MeasurementPreferencesRepository
import com.loganmartlew.rangework.shared.repository.PracticeSessionRepository
import com.loganmartlew.rangework.shared.repository.PracticeUnitRepository
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class DataFoundationUseCaseTest {
    @Test
    fun savePracticeUnitUseCaseValidatesBeforePersisting() = kotlinx.coroutines.test.runTest {
        val repository = RecordingPracticeUnitRepository()

        SavePracticeUnitUseCase(repository).invoke(
            draft = PracticeUnitDraft(
                title = "  Distance wedges ",
                instructions = listOf(
                    PracticeInstructionDraft(
                        order = 4,
                        text = "  Hit 10 balls  ",
                    ),
                ),
            ),
            unitId = "  unit-1  ",
        )

        assertEquals("Distance wedges", repository.lastDraft?.title)
        assertEquals("unit-1", repository.lastSavedUnitId)
        assertEquals(listOf(1), repository.lastDraft?.instructions?.map(PracticeInstructionDraft::order))
    }

    @Test
    fun savePracticeSessionUseCaseTrimsSessionIdAndDraft() = kotlinx.coroutines.test.runTest {
        val repository = RecordingPracticeSessionRepository()

        SavePracticeSessionUseCase(repository).invoke(
            draft = PracticeSessionDraft(
                name = "  Short game block ",
                items = listOf(
                    PracticeSessionItemDraft(
                        practiceUnitId = "  unit-4 ",
                        order = 9,
                        repeatCount = 3,
                        clubCode = "  PW ",
                    ),
                ),
                notes = "  Repeat twice ",
            ),
            sessionId = "  session-7  ",
        )

        assertEquals("Short game block", repository.lastDraft?.name)
        assertEquals("Repeat twice", repository.lastDraft?.notes)
        assertEquals(listOf(1), repository.lastDraft?.items?.map { it.order })
        assertEquals("unit-4", repository.lastDraft?.items?.single()?.practiceUnitId)
        assertEquals(3, repository.lastDraft?.items?.single()?.repeatCount)
        assertEquals("PW", repository.lastDraft?.items?.single()?.clubCode)
        assertEquals("session-7", repository.lastSavedSessionId)
    }

    @Test
    fun getClubCatalogUseCaseReturnsCatalogFromRepository() = kotlinx.coroutines.test.runTest {
        val repository = RecordingClubRepository(
            catalog = listOf(
                Club(code = "driver", displayName = "Driver", category = ClubCategory.WOOD, sortOrder = 1),
                Club(code = "putter", displayName = "Putter", category = ClubCategory.PUTTER, sortOrder = 30),
            ),
        )

        val result = GetClubCatalogUseCase(repository).invoke()

        assertEquals(2, result.size)
        assertEquals("driver", result.first().code)
    }

    @Test
    fun setClubEnabledUseCasePersistsEnableAction() = kotlinx.coroutines.test.runTest {
        val repository = RecordingClubRepository(enabledCodes = mutableSetOf("driver"))

        SetClubEnabledUseCase(repository).invoke("putter", true)

        assertTrue("putter" in repository.enabledCodes)
    }

    @Test
    fun setClubDisabledUseCasePersistsDisableAction() = kotlinx.coroutines.test.runTest {
        val repository = RecordingClubRepository(enabledCodes = mutableSetOf("driver", "putter"))

        SetClubEnabledUseCase(repository).invoke("driver", false)

        assertFalse("driver" in repository.enabledCodes)
        assertTrue("putter" in repository.enabledCodes)
    }

    @Test
    fun getEnabledClubsUseCaseReturnsEnabledCodes() = kotlinx.coroutines.test.runTest {
        val repository = RecordingClubRepository(enabledCodes = mutableSetOf("driver", "seven_iron"))

        val result = GetEnabledClubsUseCase(repository).invoke()

        assertEquals(setOf("driver", "seven_iron"), result)
    }

    @Test
    fun saveMeasurementPreferencesUseCaseNormalizesPresets() = kotlinx.coroutines.test.runTest {
        val repository = RecordingMeasurementPreferencesRepository()

        SaveMeasurementPreferencesUseCase(repository).invoke(
            MeasurementPreferences(
                unitSystem = UnitSystem.METRIC,
                distanceUnit = DistanceUnit.YARDS,
                speedUnit = SpeedUnit.MILES_PER_HOUR,
            ),
        )

        assertEquals(MeasurementPreferences.Metric, repository.lastSavedPreferences)
    }
}

private class RecordingPracticeUnitRepository : PracticeUnitRepository {
    var lastDraft: PracticeUnitDraft? = null
    var lastSavedUnitId: String? = null

    override suspend fun listPracticeUnits(): List<PracticeUnit> = emptyList()

    override suspend fun getPracticeUnit(unitId: String): PracticeUnit? = null

    override suspend fun savePracticeUnit(
        draft: PracticeUnitDraft,
        unitId: String?,
    ): PracticeUnit {
        lastDraft = draft
        lastSavedUnitId = unitId
        return PracticeUnit(
            id = unitId ?: "generated-unit",
            title = draft.title,
            instructions = emptyList(),
            notes = draft.notes,
            focus = draft.focus,
            defaultClubCode = draft.defaultClubCode,
            createdAt = Instant.parse("2026-06-15T00:00:00Z"),
            updatedAt = Instant.parse("2026-06-15T00:00:00Z"),
        )
    }

    override suspend fun deletePracticeUnit(unitId: String) = Unit
}

private class RecordingPracticeSessionRepository : PracticeSessionRepository {
    var lastDraft: PracticeSessionDraft? = null
    var lastSavedSessionId: String? = null

    override suspend fun listPracticeSessions(): List<PracticeSession> = emptyList()

    override suspend fun getPracticeSession(sessionId: String): PracticeSession? = null

    override suspend fun savePracticeSession(
        draft: PracticeSessionDraft,
        sessionId: String?,
    ): PracticeSession {
        lastDraft = draft
        lastSavedSessionId = sessionId
        return PracticeSession(
            id = sessionId ?: "generated-session",
            name = draft.name,
            items = emptyList(),
            notes = draft.notes,
            createdAt = Instant.parse("2026-06-15T00:00:00Z"),
            updatedAt = Instant.parse("2026-06-15T00:00:00Z"),
        )
    }

    override suspend fun deletePracticeSession(sessionId: String) = Unit
}

private class RecordingMeasurementPreferencesRepository : MeasurementPreferencesRepository {
    var lastSavedPreferences: MeasurementPreferences? = null

    override suspend fun getMeasurementPreferences(): MeasurementPreferences = MeasurementPreferences.Imperial

    override suspend fun saveMeasurementPreferences(
        preferences: MeasurementPreferences,
    ): MeasurementPreferences {
        lastSavedPreferences = preferences
        return preferences
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
