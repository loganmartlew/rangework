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
    fun practiceUnitRepositorySaveValidatesAndTrimsId() = kotlinx.coroutines.test.runTest {
        val repository = RecordingPracticeUnitRepository()

        repository.save(
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
    fun practiceSessionRepositorySaveTrimsIdAndNormalizesDraft() = kotlinx.coroutines.test.runTest {
        val repository = RecordingPracticeSessionRepository()

        repository.save(
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

private class RecordingPracticeUnitRepository : PracticeUnitRepository() {
    var lastDraft: PracticeUnitDraft? = null
    var lastSavedUnitId: String? = null

    override suspend fun list(): List<PracticeUnit> = emptyList()

    override suspend fun get(id: String): PracticeUnit? = null

    override suspend fun persist(validated: PracticeUnitDraft, unitId: String?): PracticeUnit {
        lastDraft = validated
        lastSavedUnitId = unitId
        return PracticeUnit(
            id = unitId ?: "generated-unit",
            title = validated.title,
            instructions = emptyList(),
            notes = validated.notes,
            focus = validated.focus,
            defaultClubCode = validated.defaultClubCode,
            createdAt = Instant.parse("2026-06-15T00:00:00Z"),
            updatedAt = Instant.parse("2026-06-15T00:00:00Z"),
        )
    }

    override suspend fun delete(id: String) = Unit
}

private class RecordingPracticeSessionRepository : PracticeSessionRepository() {
    var lastDraft: PracticeSessionDraft? = null
    var lastSavedSessionId: String? = null

    override suspend fun list(): List<PracticeSession> = emptyList()

    override suspend fun get(id: String): PracticeSession? = null

    override suspend fun persist(validated: PracticeSessionDraft, sessionId: String?): PracticeSession {
        lastDraft = validated
        lastSavedSessionId = sessionId
        return PracticeSession(
            id = sessionId ?: "generated-session",
            name = validated.name,
            items = emptyList(),
            notes = validated.notes,
            createdAt = Instant.parse("2026-06-15T00:00:00Z"),
            updatedAt = Instant.parse("2026-06-15T00:00:00Z"),
        )
    }

    override suspend fun delete(id: String) = Unit
}

private class RecordingMeasurementPreferencesRepository : MeasurementPreferencesRepository() {
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
