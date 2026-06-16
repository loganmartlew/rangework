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
import com.loganmartlew.rangework.shared.repository.ClubRepository
import com.loganmartlew.rangework.shared.usecase.DeletePracticeSessionUseCase
import com.loganmartlew.rangework.shared.usecase.DuplicatePracticeSessionUseCase
import com.loganmartlew.rangework.shared.usecase.DeletePracticeUnitUseCase
import com.loganmartlew.rangework.shared.usecase.GetClubCatalogUseCase
import com.loganmartlew.rangework.shared.usecase.GetEnabledClubsUseCase
import com.loganmartlew.rangework.shared.usecase.GetMeasurementPreferencesUseCase
import com.loganmartlew.rangework.shared.usecase.GetPracticeSessionUseCase
import com.loganmartlew.rangework.shared.usecase.GetPracticeUnitUseCase
import com.loganmartlew.rangework.shared.usecase.ListPracticeSessionsUseCase
import com.loganmartlew.rangework.shared.usecase.ListPracticeUnitsUseCase
import com.loganmartlew.rangework.shared.usecase.SaveMeasurementPreferencesUseCase
import com.loganmartlew.rangework.shared.usecase.SavePracticeSessionUseCase
import com.loganmartlew.rangework.shared.usecase.SavePracticeUnitUseCase
import com.loganmartlew.rangework.shared.usecase.SetClubEnabledUseCase
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

    @Test
    fun signOutClearsLoadedPlanningState() = runTest {
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
        viewModel.editUnit("unit-1")

        viewModel.onAuthStateChanged(AuthState.SignedOut)

        assertTrue(viewModel.uiState.value.units.isEmpty())
        assertTrue(viewModel.uiState.value.sessions.isEmpty())
        assertEquals(PracticeUnitEditorState(), viewModel.uiState.value.unitEditor)
        assertEquals(PracticeSessionEditorState(), viewModel.uiState.value.sessionEditor)
        assertEquals(null, viewModel.uiState.value.statusMessage)
    }

    @Test
    fun signedOutSaveAttemptPromptsForSignIn() = runTest {
        val repositories = FakePlannerRepositories()
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )

        viewModel.updateUnitTitle("Distance wedges")
        viewModel.saveUnit()
        advanceUntilIdle()

        assertTrue(repositories.savedUnitDrafts.isEmpty())
        assertEquals("Sign in before changing practice plans.", viewModel.uiState.value.statusMessage)
    }

    @Test
    fun saveUnitWithBlankTitlePopulatesErrorAndAborts() = runTest {
        val repositories = FakePlannerRepositories()
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        advanceUntilIdle()

        viewModel.updateInstructionText(0, "Hit some balls")
        viewModel.saveUnit()
        advanceUntilIdle()

        assertTrue(repositories.savedUnitDrafts.isEmpty())
        val titleError = viewModel.uiState.value.unitEditor.titleError
        assertTrue("Expected titleError to be non-null", titleError != null)
    }

    @Test
    fun editingUnitTitleClearsItsError() = runTest {
        val repositories = FakePlannerRepositories()
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        advanceUntilIdle()

        viewModel.updateInstructionText(0, "Do something")
        viewModel.saveUnit()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.unitEditor.titleError != null)

        viewModel.updateUnitTitle("Wedge work")
        assertEquals(null, viewModel.uiState.value.unitEditor.titleError)
    }

    @Test
    fun isUnitEditorDirtyTogglesCorrectly() = runTest {
        val repositories = FakePlannerRepositories()
        repositories.units += sampleUnit()
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isUnitEditorDirty)

        viewModel.editUnit("unit-1")
        assertEquals(false, viewModel.uiState.value.isUnitEditorDirty)

        viewModel.updateUnitTitle("Changed title")
        assertEquals(true, viewModel.uiState.value.isUnitEditorDirty)
    }

    @Test
    fun hasLoadedFlipsAfterFirstSuccessfulRefresh() = runTest {
        val repositories = FakePlannerRepositories()
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )

        assertEquals(false, viewModel.uiState.value.hasLoaded)

        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.hasLoaded)
    }
}

private class FakePlannerRepositories :
    PracticeUnitRepository,
    PracticeSessionRepository,
    MeasurementPreferencesRepository,
    ClubRepository {
    constructor(
        listUnitsException: Throwable? = null,
    ) {
        this.listUnitsException = listUnitsException
    }

    private var listUnitsException: Throwable? = null
    val units = mutableListOf<PracticeUnit>()
    val sessions = mutableListOf<PracticeSession>()
    val savedUnitDrafts = mutableListOf<PracticeUnitDraft>()
    var listUnitsCallCount = 0
    var listSessionsCallCount = 0

    override suspend fun listPracticeUnits(): List<PracticeUnit> {
        listUnitsCallCount += 1
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

    override suspend fun listPracticeSessions(): List<PracticeSession> {
        listSessionsCallCount += 1
        return sessions.toList()
    }

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

    override suspend fun listCatalog() = emptyList<com.loganmartlew.rangework.shared.model.Club>()

    override suspend fun getEnabledClubCodes() = emptySet<String>()

    override suspend fun setClubEnabled(code: String, enabled: Boolean) = Unit

    fun toDataFoundation(): DataFoundation = DataFoundation(
        listPracticeUnitsUseCase = ListPracticeUnitsUseCase(this),
        getPracticeUnitUseCase = GetPracticeUnitUseCase(this),
        savePracticeUnitUseCase = SavePracticeUnitUseCase(this),
        deletePracticeUnitUseCase = DeletePracticeUnitUseCase(this),
        listPracticeSessionsUseCase = ListPracticeSessionsUseCase(this),
        getPracticeSessionUseCase = GetPracticeSessionUseCase(this),
        savePracticeSessionUseCase = SavePracticeSessionUseCase(this),
        deletePracticeSessionUseCase = DeletePracticeSessionUseCase(this),
        duplicatePracticeSessionUseCase = DuplicatePracticeSessionUseCase(
            getPracticeSessionUseCase = GetPracticeSessionUseCase(this),
            savePracticeSessionUseCase = SavePracticeSessionUseCase(this),
        ),
        getMeasurementPreferencesUseCase = GetMeasurementPreferencesUseCase(this),
        saveMeasurementPreferencesUseCase = SaveMeasurementPreferencesUseCase(this),
        getClubCatalogUseCase = GetClubCatalogUseCase(this),
        getEnabledClubsUseCase = GetEnabledClubsUseCase(this),
        setClubEnabledUseCase = SetClubEnabledUseCase(this),
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
            clubReference = "lob_wedge",
            notes = "Start here",
        ),
    ),
    createdAt = Instant.parse("2026-06-15T00:00:00Z"),
    updatedAt = Instant.parse("2026-06-15T00:00:00Z"),
)
