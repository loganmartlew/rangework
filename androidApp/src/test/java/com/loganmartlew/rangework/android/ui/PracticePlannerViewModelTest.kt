package com.loganmartlew.rangework.android.ui

import com.loganmartlew.rangework.shared.auth.AuthState
import com.loganmartlew.rangework.shared.config.baselineEnvironment
import com.loganmartlew.rangework.shared.data.DataFoundation
import com.loganmartlew.rangework.shared.model.PracticeInstruction
import com.loganmartlew.rangework.shared.model.PracticeSession
import com.loganmartlew.rangework.shared.model.PracticeSessionDraft
import com.loganmartlew.rangework.shared.model.PracticeSessionItem
import com.loganmartlew.rangework.shared.model.PracticeUnit
import com.loganmartlew.rangework.shared.model.PracticeUnitDraft
import com.loganmartlew.rangework.shared.repository.MeasurementPreferencesRepository
import com.loganmartlew.rangework.shared.repository.PracticeSessionRepository
import com.loganmartlew.rangework.shared.repository.PracticeUnitRepository
import com.loganmartlew.rangework.shared.usecase.DeletePracticeSessionUseCase
import com.loganmartlew.rangework.shared.usecase.DeletePracticeUnitUseCase
import com.loganmartlew.rangework.shared.usecase.GetMeasurementPreferencesUseCase
import com.loganmartlew.rangework.shared.usecase.GetPracticeSessionUseCase
import com.loganmartlew.rangework.shared.usecase.GetPracticeUnitUseCase
import com.loganmartlew.rangework.shared.usecase.ListPracticeSessionsUseCase
import com.loganmartlew.rangework.shared.usecase.ListPracticeUnitsUseCase
import com.loganmartlew.rangework.shared.usecase.SaveMeasurementPreferencesUseCase
import com.loganmartlew.rangework.shared.usecase.SavePracticeSessionUseCase
import com.loganmartlew.rangework.shared.usecase.SavePracticeUnitUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PracticePlannerViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun signedInRefreshLoadsUnitsAndSessions() = runTest {
        val repositories = FakePlannerRepositories()
        repositories.units += sampleUnit()
        repositories.sessions += sampleSession()

        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )

        viewModel.onAuthStateChanged(
            AuthState.SignedIn(
                userId = "user-1",
                userEmail = "logan@example.com",
            ),
        )
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.units.size)
        assertEquals(1, viewModel.uiState.value.sessions.size)
        assertEquals("Planning workspace ready.", viewModel.uiState.value.statusMessage)
    }

    @Test
    fun saveUnitPersistsNormalizedDraft() = runTest {
        val repositories = FakePlannerRepositories()
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )

        viewModel.onAuthStateChanged(
            AuthState.SignedIn(
                userId = "user-1",
                userEmail = "logan@example.com",
            ),
        )
        advanceUntilIdle()

        viewModel.updateUnitTitle("  Distance wedges ")
        viewModel.updateInstructionText(0, "  Hit 10 wedges ")
        viewModel.updateInstructionBallCount(0, "10")
        viewModel.saveUnit()
        advanceUntilIdle()

        assertEquals("Distance wedges", repositories.savedUnitDrafts.single().title)
        assertEquals(10, repositories.savedUnitDrafts.single().instructions.single().ballCount)
        assertTrue(viewModel.uiState.value.statusMessage.orEmpty().contains("Saved"))
        assertEquals(1, viewModel.uiState.value.units.size)
    }

    @Test
    fun missingPlanningTableShowsFriendlySetupMessage() = runTest {
        val repositories = FakePlannerRepositories(
            listUnitsException = IllegalStateException(
                "Could not find the table 'public.practice_units' in the schema cache (Perhaps you meant the table 'public.profiles').",
            ),
        )
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )

        viewModel.onAuthStateChanged(
            AuthState.SignedIn(
                userId = "user-1",
                userEmail = "logan@example.com",
            ),
        )
        advanceUntilIdle()

        assertEquals(
            planningSchemaUnavailableMessage(),
            viewModel.uiState.value.statusMessage,
        )
    }

    @Test
    fun planningPermissionDeniedShowsFriendlySetupMessage() = runTest {
        val repositories = FakePlannerRepositories(
            listUnitsException = IllegalStateException(
                "permission denied for table practice_units",
            ),
        )
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )

        viewModel.onAuthStateChanged(
            AuthState.SignedIn(
                userId = "user-1",
                userEmail = "logan@example.com",
            ),
        )
        advanceUntilIdle()

        assertEquals(
            planningSchemaUnavailableMessage(),
            viewModel.uiState.value.statusMessage,
        )
    }
}

private class FakePlannerRepositories :
    PracticeUnitRepository,
    PracticeSessionRepository,
    MeasurementPreferencesRepository {
    constructor(
        listUnitsException: Throwable? = null,
    ) {
        this.listUnitsException = listUnitsException
    }

    private var listUnitsException: Throwable? = null
    val units = mutableListOf<PracticeUnit>()
    val sessions = mutableListOf<PracticeSession>()
    val savedUnitDrafts = mutableListOf<PracticeUnitDraft>()

    override suspend fun listPracticeUnits(): List<PracticeUnit> {
        listUnitsException?.let { throw it }
        return units.toList()
    }

    override suspend fun getPracticeUnit(unitId: String): PracticeUnit? = units.firstOrNull { unit -> unit.id == unitId }

    override suspend fun savePracticeUnit(
        draft: PracticeUnitDraft,
        unitId: String?,
    ): PracticeUnit {
        savedUnitDrafts += draft
        val unit = PracticeUnit(
            id = unitId ?: "unit-${units.size + 1}",
            title = draft.title,
            instructions = draft.instructions.mapIndexed { index, instruction ->
                PracticeInstruction(
                    id = "instruction-$index",
                    order = index + 1,
                    text = instruction.text,
                    repCount = instruction.repCount,
                    ballCount = instruction.ballCount,
                )
            },
            notes = draft.notes,
            focus = draft.focus,
            defaultClubReference = draft.defaultClubReference,
            createdAt = Instant.parse("2026-06-15T00:00:00Z"),
            updatedAt = Instant.parse("2026-06-15T00:00:00Z"),
        )
        units.removeAll { existing -> existing.id == unit.id }
        units += unit
        return unit
    }

    override suspend fun deletePracticeUnit(unitId: String) {
        units.removeAll { unit -> unit.id == unitId }
    }

    override suspend fun listPracticeSessions(): List<PracticeSession> = sessions.toList()

    override suspend fun getPracticeSession(sessionId: String): PracticeSession? =
        sessions.firstOrNull { session -> session.id == sessionId }

    override suspend fun savePracticeSession(
        draft: PracticeSessionDraft,
        sessionId: String?,
    ): PracticeSession {
        val session = PracticeSession(
            id = sessionId ?: "session-${sessions.size + 1}",
            name = draft.name,
            items = draft.items.mapIndexed { index, item ->
                PracticeSessionItem(
                    id = "session-item-$index",
                    practiceUnitId = item.practiceUnitId,
                    order = index + 1,
                    repeatCount = item.repeatCount,
                    clubReference = item.clubReference,
                    notes = item.notes,
                    focusCue = item.focusCue,
                    restSeconds = item.restSeconds,
                )
            },
            notes = draft.notes,
            createdAt = Instant.parse("2026-06-15T00:00:00Z"),
            updatedAt = Instant.parse("2026-06-15T00:00:00Z"),
        )
        sessions.removeAll { existing -> existing.id == session.id }
        sessions += session
        return session
    }

    override suspend fun deletePracticeSession(sessionId: String) {
        sessions.removeAll { session -> session.id == sessionId }
    }

    override suspend fun getMeasurementPreferences() =
        com.loganmartlew.rangework.shared.model.MeasurementPreferences.Imperial

    override suspend fun saveMeasurementPreferences(
        preferences: com.loganmartlew.rangework.shared.model.MeasurementPreferences,
    ) = preferences

    fun toDataFoundation(): DataFoundation = DataFoundation(
        listPracticeUnitsUseCase = ListPracticeUnitsUseCase(this),
        getPracticeUnitUseCase = GetPracticeUnitUseCase(this),
        savePracticeUnitUseCase = SavePracticeUnitUseCase(this),
        deletePracticeUnitUseCase = DeletePracticeUnitUseCase(this),
        listPracticeSessionsUseCase = ListPracticeSessionsUseCase(this),
        getPracticeSessionUseCase = GetPracticeSessionUseCase(this),
        savePracticeSessionUseCase = SavePracticeSessionUseCase(this),
        deletePracticeSessionUseCase = DeletePracticeSessionUseCase(this),
        getMeasurementPreferencesUseCase = GetMeasurementPreferencesUseCase(this),
        saveMeasurementPreferencesUseCase = SaveMeasurementPreferencesUseCase(this),
    )
}

private fun sampleUnit(): PracticeUnit = PracticeUnit(
    id = "unit-1",
    title = "Wedge ladder",
    instructions = listOf(
        PracticeInstruction(
            id = "instruction-1",
            order = 1,
            text = "Hit 10 wedges",
            ballCount = 10,
        ),
    ),
    createdAt = Instant.parse("2026-06-15T00:00:00Z"),
    updatedAt = Instant.parse("2026-06-15T00:00:00Z"),
)

private fun sampleSession(): PracticeSession = PracticeSession(
    id = "session-1",
    name = "Scoring block",
    items = listOf(
        PracticeSessionItem(
            id = "session-item-1",
            practiceUnitId = "unit-1",
            order = 1,
            repeatCount = 2,
            clubReference = "58*",
            notes = "Start here",
        ),
    ),
    createdAt = Instant.parse("2026-06-15T00:00:00Z"),
    updatedAt = Instant.parse("2026-06-15T00:00:00Z"),
)
