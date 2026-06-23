package com.loganmartlew.rangework.android.ui

import com.loganmartlew.rangework.shared.data.DataFoundation
import com.loganmartlew.rangework.shared.model.ActiveRangeSessionSummary
import com.loganmartlew.rangework.shared.model.Club
import com.loganmartlew.rangework.shared.model.CompletedRangeSessionSummary
import com.loganmartlew.rangework.shared.model.MeasurementPreferences
import com.loganmartlew.rangework.shared.model.PracticeSession
import com.loganmartlew.rangework.shared.model.PracticeSessionDraft
import com.loganmartlew.rangework.shared.model.PracticeUnit
import com.loganmartlew.rangework.shared.model.PracticeUnitDraft
import com.loganmartlew.rangework.shared.model.RangeSession
import com.loganmartlew.rangework.shared.model.RangeSessionSnapshot
import com.loganmartlew.rangework.shared.model.SnapshotInstruction
import com.loganmartlew.rangework.shared.model.SnapshotStep
import com.loganmartlew.rangework.shared.model.SnapshotUnit
import com.loganmartlew.rangework.shared.repository.ClubRepository
import com.loganmartlew.rangework.shared.repository.MeasurementPreferencesRepository
import com.loganmartlew.rangework.shared.repository.PracticeSessionRepository
import com.loganmartlew.rangework.shared.repository.PracticeUnitRepository
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RangeSessionViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun loadsSessionAndStartsAtFirstStep() = runTest {
        val session = sampleRangeSession(steps = 3)
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(session.id, state.rangeSession?.id)
        assertEquals(0, state.currentStepIndex)
        assertNull(state.statusMessage)
    }

    @Test
    fun restoresLastViewedStepIndexOnLoad() = runTest {
        val session = sampleRangeSession(steps = 5, lastViewedStepIndex = 3)
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.currentStepIndex)
    }

    @Test
    fun ignoresOutOfBoundsLastViewedStepIndex() = runTest {
        val session = sampleRangeSession(steps = 3, lastViewedStepIndex = 99)
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.currentStepIndex)
    }

    @Test
    fun nextStepIncrementsIndex() = runTest {
        val session = sampleRangeSession(steps = 3)
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        viewModel.nextStep()

        assertEquals(1, viewModel.uiState.value.currentStepIndex)
    }

    @Test
    fun previousStepDecrementsIndex() = runTest {
        val session = sampleRangeSession(steps = 3, lastViewedStepIndex = 2)
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        viewModel.previousStep()

        assertEquals(1, viewModel.uiState.value.currentStepIndex)
    }

    @Test
    fun nextStepClampsAtLastStep() = runTest {
        val session = sampleRangeSession(steps = 2)
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        viewModel.nextStep() // index = 1
        viewModel.nextStep() // should clamp at 1

        assertEquals(1, viewModel.uiState.value.currentStepIndex)
    }

    @Test
    fun previousStepClampsAtFirstStep() = runTest {
        val session = sampleRangeSession(steps = 3)
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        viewModel.previousStep() // already at 0, should not move

        assertEquals(0, viewModel.uiState.value.currentStepIndex)
    }

    @Test
    fun navigateToStepSetsIndexDirectly() = runTest {
        val session = sampleRangeSession(steps = 5)
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        viewModel.navigateToStep(4)

        assertEquals(4, viewModel.uiState.value.currentStepIndex)
    }

    @Test
    fun navigateToStepClampsOutOfBoundsIndex() = runTest {
        val session = sampleRangeSession(steps = 3)
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        viewModel.navigateToStep(100)

        assertEquals(2, viewModel.uiState.value.currentStepIndex)
    }

    @Test
    fun nextStepPersistsLastViewedStep() = runTest {
        val session = sampleRangeSession(steps = 3)
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        viewModel.nextStep()
        advanceUntilIdle()

        assertEquals(1, repo.lastViewedStepUpdates[session.id])
    }

    @Test
    fun sessionNotFoundSetsErrorMessage() = runTest {
        val repo = FakeRangeSessionRepo(sessions = mutableListOf())
        val viewModel = makeViewModel(repo = repo, rangeSessionId = "missing-id")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.rangeSession)
        assertTrue("Expected error message", state.statusMessage != null)
    }

    @Test
    fun nullDataFoundationSetsErrorMessage() = runTest {
        val viewModel = RangeSessionViewModel(
            rangeSessionId = "any-id",
            dataFoundation = null,
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.rangeSession)
        assertTrue("Expected error message for null foundation", state.statusMessage != null)
    }

    @Test
    fun loadSessionPopulatesCompletedStepIndices() = runTest {
        val session = sampleRangeSession(steps = 3, completedStepIndices = setOf(0, 2))
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        assertEquals(setOf(0, 2), viewModel.uiState.value.completedStepIndices)
    }

    @Test
    fun toggleStepCompleteAddsIndexOptimistically() = runTest {
        val session = sampleRangeSession(steps = 3)
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        viewModel.toggleStepComplete(0)

        // Optimistic update — visible before coroutine completes
        assertTrue(0 in viewModel.uiState.value.completedStepIndices)
    }

    @Test
    fun toggleStepCompleteServerResponseUpdatesSession() = runTest {
        val session = sampleRangeSession(steps = 3)
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        viewModel.toggleStepComplete(0)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(0 in state.completedStepIndices)
        assertEquals(1, state.rangeSession?.completedSteps?.size)
        assertEquals(0, state.rangeSession?.completedSteps?.first()?.stepIndex)
    }

    @Test
    fun toggleStepCompleteRemovesIndexWhenAlreadyComplete() = runTest {
        val session = sampleRangeSession(steps = 3, completedStepIndices = setOf(1))
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        viewModel.toggleStepComplete(1)
        advanceUntilIdle()

        assertFalse(1 in viewModel.uiState.value.completedStepIndices)
        assertTrue(viewModel.uiState.value.rangeSession?.completedSteps?.isEmpty() == true)
    }

    @Test
    fun toggleStepCompleteAutoAdvancesFromCurrentStep() = runTest {
        val session = sampleRangeSession(steps = 3)
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.currentStepIndex)
        viewModel.toggleStepComplete(0) // completing current step 0

        assertEquals(1, viewModel.uiState.value.currentStepIndex)
    }

    @Test
    fun toggleStepCompleteDoesNotAdvanceWhenCompletingNonCurrentStep() = runTest {
        val session = sampleRangeSession(steps = 3)
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.currentStepIndex)
        viewModel.toggleStepComplete(2) // completing a step that isn't current

        assertEquals(0, viewModel.uiState.value.currentStepIndex)
    }

    @Test
    fun toggleStepCompleteDoesNotAdvancePastLastStep() = runTest {
        val session = sampleRangeSession(steps = 2, lastViewedStepIndex = 1)
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.currentStepIndex)
        viewModel.toggleStepComplete(1) // completing last step

        assertEquals(1, viewModel.uiState.value.currentStepIndex)
    }

    @Test
    fun toggleStepCompleteDoesNotAutoAdvanceOnUncomplete() = runTest {
        val session = sampleRangeSession(steps = 3, lastViewedStepIndex = 1, completedStepIndices = setOf(1))
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.currentStepIndex)
        viewModel.toggleStepComplete(1) // uncompleting current step

        assertEquals(1, viewModel.uiState.value.currentStepIndex)
    }

    @Test
    fun toggleStepCompleteRevertsOnNetworkFailure() = runTest {
        val session = sampleRangeSession(steps = 3)
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session), shouldFailOnToggle = true)
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        viewModel.toggleStepComplete(0) // optimistic update applies
        assertTrue(0 in viewModel.uiState.value.completedStepIndices)

        advanceUntilIdle() // network fails → revert

        val state = viewModel.uiState.value
        assertFalse(0 in state.completedStepIndices)
        assertEquals(0, state.currentStepIndex)
        assertTrue("Expected error notification after failure", state.notification != null)
    }

    @Test
    fun rapidToggleDoesNotJitterCompletedSteps() = runTest {
        val session = sampleRangeSession(steps = 4)
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        // Rapidly complete steps 0, 1, 2 before any network response
        viewModel.toggleStepComplete(0)
        viewModel.toggleStepComplete(1)
        viewModel.toggleStepComplete(2)

        // All three optimistic updates should be visible
        assertEquals(setOf(0, 1, 2), viewModel.uiState.value.completedStepIndices)
        assertEquals(3, viewModel.uiState.value.currentStepIndex)

        // Let all network requests resolve
        advanceUntilIdle()

        // State should remain stable — no jitter from stale server snapshots
        assertEquals(setOf(0, 1, 2), viewModel.uiState.value.completedStepIndices)
        assertEquals(3, viewModel.uiState.value.currentStepIndex)
    }

    @Test
    fun failedToggleDoesNotRevertOtherSteps() = runTest {
        val session = sampleRangeSession(steps = 4)
        var failStep = 1
        val repo = object : FakeRangeSessionRepo(sessions = mutableListOf(session)) {
            override suspend fun toggleStepComplete(
                rangeSessionId: String,
                stepIndex: Int,
                completed: Boolean,
            ): RangeSession {
                if (stepIndex == failStep) throw RuntimeException("Simulated network error")
                return super.toggleStepComplete(rangeSessionId, stepIndex, completed)
            }
        }
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        // Complete steps 0 and 1 rapidly (step 1 will fail on the server)
        viewModel.toggleStepComplete(0)
        viewModel.toggleStepComplete(1)

        assertEquals(setOf(0, 1), viewModel.uiState.value.completedStepIndices)
        assertEquals(2, viewModel.uiState.value.currentStepIndex)

        advanceUntilIdle()

        // Step 1 failed — only step 1 should revert; step 0 stays complete
        val state = viewModel.uiState.value
        assertTrue("Step 0 should remain complete", 0 in state.completedStepIndices)
        assertFalse("Step 1 should revert", 1 in state.completedStepIndices)
        assertTrue("Expected error notification", state.notification != null)
    }

    @Test
    fun consumeNotificationClearsNotification() = runTest {
        val session = sampleRangeSession(steps = 3)
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session), shouldFailOnToggle = true)
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        viewModel.toggleStepComplete(0)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.notification != null)

        viewModel.consumeNotification()
        assertNull(viewModel.uiState.value.notification)
    }

    @Test
    fun timerIsNotRunningInitially() = runTest {
        val session = sampleRangeSession(steps = 3)
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isTimerRunning)
        assertEquals(0L, viewModel.uiState.value.elapsedSeconds)
    }

    @Test
    fun timerStartsOnScreenEnter() = runTest {
        val session = sampleRangeSession(steps = 3)
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session), getElapsedSecondsResult = 45L)
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        viewModel.onScreenEnter()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.isTimerRunning)
        assertEquals(45L, viewModel.uiState.value.elapsedSeconds)

        viewModel.onScreenExit()
        advanceUntilIdle()
    }

    @Test
    fun recordTimeEntryCalledOnScreenEnter() = runTest {
        val session = sampleRangeSession(steps = 3)
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        viewModel.onScreenEnter()
        advanceUntilIdle()

        assertEquals(1, repo.recordedTimeEntries.size)
        assertEquals(session.id, repo.recordedTimeEntries.first().first)

        viewModel.onScreenExit()
        advanceUntilIdle()
    }

    @Test
    fun timerStopsOnScreenExit() = runTest {
        val session = sampleRangeSession(steps = 3)
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        viewModel.onScreenEnter()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isTimerRunning)

        viewModel.onScreenExit()
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isTimerRunning)
    }

    @Test
    fun closeTimeEntryCalledOnScreenExit() = runTest {
        val session = sampleRangeSession(steps = 3)
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        viewModel.onScreenEnter()
        advanceUntilIdle()
        viewModel.onScreenExit()
        advanceUntilIdle()

        assertEquals(1, repo.closedTimeEntries.size)
        assertEquals(session.id, repo.closedTimeEntries.first().first)
    }

    @Test
    fun overrideStepClubUpdatesOptimistically() = runTest {
        val session = sampleRangeSession(steps = 3)
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        viewModel.overrideStepClub(0, "seven_iron")

        val state = viewModel.uiState.value
        assertEquals("seven_iron", state.rangeSession?.clubOverrides?.get("0"))
    }

    @Test
    fun overrideStepClubReconcileAfterSuccess() = runTest {
        val session = sampleRangeSession(steps = 3)
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        viewModel.overrideStepClub(0, "seven_iron")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("seven_iron", state.rangeSession?.clubOverrides?.get("0"))
        assertEquals(1, repo.overrideInvocations.size)
    }

    @Test
    fun overrideStepClubRevertsOnFailure() = runTest {
        val session = sampleRangeSession(steps = 3)
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session), shouldFailOnOverride = true)
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        viewModel.overrideStepClub(0, "seven_iron")
        assertTrue("Optimistic: override should appear", viewModel.uiState.value.rangeSession?.clubOverrides?.containsKey("0") == true)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Club overrides should revert to empty", state.rangeSession?.clubOverrides?.isEmpty() == true)
        assertTrue("Expected error notification", state.notification != null)
    }

    @Test
    fun doubleEnterGuardPreventsMultipleTimeEntries() = runTest {
        val session = sampleRangeSession(steps = 3)
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        viewModel.onScreenEnter()
        viewModel.onScreenEnter()
        advanceUntilIdle()

        assertEquals(1, repo.recordedTimeEntries.size)

        viewModel.onScreenExit()
        advanceUntilIdle()
    }

    @Test
    fun closeTimeEntryEnteredAtMatchesRecordedEnteredAt() = runTest {
        val session = sampleRangeSession(steps = 3)
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        viewModel.onScreenEnter()
        advanceUntilIdle()
        viewModel.onScreenExit()
        advanceUntilIdle()

        val recordedEnteredAt = repo.recordedTimeEntries.first().second
        val closedEnteredAt = repo.closedTimeEntries.first().second
        assertEquals(recordedEnteredAt, closedEnteredAt)
    }
}

private fun makeViewModel(
    repo: FakeRangeSessionRepo,
    rangeSessionId: String,
): RangeSessionViewModel = RangeSessionViewModel(
    rangeSessionId = rangeSessionId,
    dataFoundation = buildDataFoundation(rangeRepo = repo),
)

private fun buildDataFoundation(rangeRepo: RangeSessionRepository): DataFoundation {
    val nopUnit = NopPracticeUnitRepository()
    val nopSession = NopPracticeSessionRepository()
    val nopMeasurement = NopMeasurementPreferencesRepository()
    val nopClub = NopClubRepository()
    val nopRange = NopRangeSessionRepository()
    return DataFoundation(
        listPracticeUnitsUseCase = ListPracticeUnitsUseCase(nopUnit),
        getPracticeUnitUseCase = GetPracticeUnitUseCase(nopUnit),
        savePracticeUnitUseCase = SavePracticeUnitUseCase(nopUnit),
        deletePracticeUnitUseCase = DeletePracticeUnitUseCase(nopUnit),
        duplicatePracticeUnitUseCase = DuplicateUnitUseCase(
            getPracticeUnitUseCase = GetPracticeUnitUseCase(nopUnit),
            savePracticeUnitUseCase = SavePracticeUnitUseCase(nopUnit),
        ),
        listPracticeSessionsUseCase = ListPracticeSessionsUseCase(nopSession),
        getPracticeSessionUseCase = GetPracticeSessionUseCase(nopSession),
        savePracticeSessionUseCase = SavePracticeSessionUseCase(nopSession),
        deletePracticeSessionUseCase = DeletePracticeSessionUseCase(nopSession),
        duplicatePracticeSessionUseCase = DuplicatePracticeSessionUseCase(
            getPracticeSessionUseCase = GetPracticeSessionUseCase(nopSession),
            savePracticeSessionUseCase = SavePracticeSessionUseCase(nopSession),
        ),
        getMeasurementPreferencesUseCase = GetMeasurementPreferencesUseCase(nopMeasurement),
        saveMeasurementPreferencesUseCase = SaveMeasurementPreferencesUseCase(nopMeasurement),
        getClubCatalogUseCase = GetClubCatalogUseCase(nopClub),
        getEnabledClubsUseCase = GetEnabledClubsUseCase(nopClub),
        setClubEnabledUseCase = SetClubEnabledUseCase(nopClub),
        startRangeSessionUseCase = StartRangeSessionUseCase(nopRange),
        getRangeSessionUseCase = GetRangeSessionUseCase(rangeRepo),
        listActiveRangeSessionsUseCase = ListActiveRangeSessionsUseCase(rangeRepo),
        listCompletedRangeSessionsUseCase = ListCompletedRangeSessionsUseCase(rangeRepo),
        toggleStepCompleteUseCase = ToggleStepCompleteUseCase(rangeRepo),
        overrideStepClubUseCase = OverrideStepClubUseCase(rangeRepo),
        updateLastViewedStepUseCase = UpdateLastViewedStepUseCase(rangeRepo),
        finishRangeSessionUseCase = FinishRangeSessionUseCase(rangeRepo),
        abandonRangeSessionUseCase = AbandonRangeSessionUseCase(rangeRepo),
        recordTimeEntryUseCase = RecordTimeEntryUseCase(rangeRepo),
        closeTimeEntryUseCase = CloseTimeEntryUseCase(rangeRepo),
        getElapsedSecondsUseCase = GetElapsedSecondsUseCase(rangeRepo),
        hasActiveRangeSessionsUseCase = HasActiveRangeSessionsUseCase(rangeRepo),
        deleteAccountUseCase = com.loganmartlew.rangework.shared.usecase.DeleteAccountUseCase(
            object : com.loganmartlew.rangework.shared.repository.AccountDeletionRepository {
                override suspend fun deleteAccount() = Unit
            },
        ),
    )
}

private open class FakeRangeSessionRepo(
    val sessions: MutableList<RangeSession> = mutableListOf(),
    var shouldFailOnToggle: Boolean = false,
    var shouldFailOnOverride: Boolean = false,
    var getElapsedSecondsResult: Long = 0L,
) : RangeSessionRepository {
    val lastViewedStepUpdates = mutableMapOf<String, Int>()
    val toggleInvocations = mutableListOf<Triple<String, Int, Boolean>>()
    val overrideInvocations = mutableListOf<Triple<String, Int, String>>()
    val recordedTimeEntries = mutableListOf<Pair<String, Instant>>()
    val closedTimeEntries = mutableListOf<Triple<String, Instant, Instant>>()

    override suspend fun startSession(rangeSessionId: String, sessionId: String): RangeSession =
        error("Not called in these tests")
    override suspend fun getSession(rangeSessionId: String): RangeSession? =
        sessions.firstOrNull { it.id == rangeSessionId }
    override suspend fun listActiveSessions(): List<ActiveRangeSessionSummary> = emptyList()
    override suspend fun listCompletedSessions(sessionId: String): List<CompletedRangeSessionSummary> = emptyList()
    open override suspend fun toggleStepComplete(rangeSessionId: String, stepIndex: Int, completed: Boolean): RangeSession {
        if (shouldFailOnToggle) throw RuntimeException("Simulated network error")
        toggleInvocations.add(Triple(rangeSessionId, stepIndex, completed))
        val session = sessions.firstOrNull { it.id == rangeSessionId } ?: error("Session not found")
        val updatedCompletedSteps = if (completed) {
            session.completedSteps + com.loganmartlew.rangework.shared.model.CompletedStep(
                stepIndex = stepIndex,
                completedAt = Instant.parse("2026-06-19T10:00:00Z"),
            )
        } else {
            session.completedSteps.filter { it.stepIndex != stepIndex }
        }
        val updated = session.copy(completedSteps = updatedCompletedSteps)
        sessions.removeAll { it.id == rangeSessionId }
        sessions.add(updated)
        return updated
    }
    override suspend fun overrideStepClub(rangeSessionId: String, stepIndex: Int, clubCode: String): RangeSession {
        if (shouldFailOnOverride) throw RuntimeException("Simulated network error")
        overrideInvocations.add(Triple(rangeSessionId, stepIndex, clubCode))
        val session = sessions.firstOrNull { it.id == rangeSessionId } ?: error("Session not found")
        val updated = session.copy(clubOverrides = session.clubOverrides + (stepIndex.toString() to clubCode))
        sessions.removeAll { it.id == rangeSessionId }
        sessions.add(updated)
        return updated
    }
    override suspend fun updateLastViewedStep(rangeSessionId: String, stepIndex: Int) {
        lastViewedStepUpdates[rangeSessionId] = stepIndex
    }
    override suspend fun finishSession(rangeSessionId: String): RangeSession =
        error("Not called in these tests")
    override suspend fun abandonSession(rangeSessionId: String) = Unit
    override suspend fun recordTimeEntry(rangeSessionId: String, enteredAt: Instant) {
        recordedTimeEntries.add(Pair(rangeSessionId, enteredAt))
    }
    override suspend fun closeTimeEntry(rangeSessionId: String, enteredAt: Instant, exitedAt: Instant) {
        closedTimeEntries.add(Triple(rangeSessionId, enteredAt, exitedAt))
    }
    override suspend fun getElapsedSeconds(rangeSessionId: String): Long = getElapsedSecondsResult
    override suspend fun hasActiveSessionsForTemplate(sessionId: String): Boolean = false
}

private class NopRangeSessionRepository : RangeSessionRepository {
    override suspend fun startSession(rangeSessionId: String, sessionId: String): RangeSession = error("nop")
    override suspend fun getSession(rangeSessionId: String): RangeSession? = null
    override suspend fun listActiveSessions(): List<ActiveRangeSessionSummary> = emptyList()
    override suspend fun listCompletedSessions(sessionId: String): List<CompletedRangeSessionSummary> = emptyList()
    override suspend fun toggleStepComplete(rangeSessionId: String, stepIndex: Int, completed: Boolean): RangeSession = error("nop")
    override suspend fun overrideStepClub(rangeSessionId: String, stepIndex: Int, clubCode: String): RangeSession = error("nop")
    override suspend fun updateLastViewedStep(rangeSessionId: String, stepIndex: Int) = Unit
    override suspend fun finishSession(rangeSessionId: String): RangeSession = error("nop")
    override suspend fun abandonSession(rangeSessionId: String) = Unit
    override suspend fun recordTimeEntry(rangeSessionId: String, enteredAt: Instant) = Unit
    override suspend fun closeTimeEntry(rangeSessionId: String, enteredAt: Instant, exitedAt: Instant) = Unit
    override suspend fun getElapsedSeconds(rangeSessionId: String): Long = 0L
    override suspend fun hasActiveSessionsForTemplate(sessionId: String): Boolean = false
}

private class NopPracticeUnitRepository : PracticeUnitRepository {
    override suspend fun listPracticeUnits(): List<PracticeUnit> = emptyList()
    override suspend fun getPracticeUnit(unitId: String): PracticeUnit? = null
    override suspend fun savePracticeUnit(draft: PracticeUnitDraft, unitId: String?): PracticeUnit = error("nop")
    override suspend fun deletePracticeUnit(unitId: String) = Unit
}

private class NopPracticeSessionRepository : PracticeSessionRepository {
    override suspend fun listPracticeSessions(): List<PracticeSession> = emptyList()
    override suspend fun getPracticeSession(sessionId: String): PracticeSession? = null
    override suspend fun savePracticeSession(draft: PracticeSessionDraft, sessionId: String?): PracticeSession = error("nop")
    override suspend fun deletePracticeSession(sessionId: String) = Unit
}

private class NopMeasurementPreferencesRepository : MeasurementPreferencesRepository {
    override suspend fun getMeasurementPreferences(): MeasurementPreferences = MeasurementPreferences.Imperial
    override suspend fun saveMeasurementPreferences(preferences: MeasurementPreferences): MeasurementPreferences = preferences
}

private class NopClubRepository : ClubRepository {
    override suspend fun listCatalog(): List<Club> = emptyList()
    override suspend fun getEnabledClubCodes(): Set<String> = emptySet()
    override suspend fun setClubEnabled(code: String, enabled: Boolean) = Unit
}

private fun sampleRangeSession(
    id: String = "range-session-1",
    steps: Int = 3,
    lastViewedStepIndex: Int? = null,
    completedStepIndices: Set<Int> = emptySet(),
): RangeSession = RangeSession(
    id = id,
    sourceSessionId = "session-1",
    sessionName = "Morning block",
    snapshot = RangeSessionSnapshot(
        units = listOf(
            SnapshotUnit(
                unitTitle = "Wedge work",
                repeatCount = 1,
                instructions = List(steps) { i ->
                    SnapshotInstruction(text = "Instruction ${i + 1}", ballCount = 10)
                },
            ),
        ),
        steps = List(steps) { i ->
            SnapshotStep(
                unitIndex = 0,
                instructionIndex = i,
                repNumber = 1,
                totalReps = 1,
                instructionText = "Instruction ${i + 1}",
                ballCount = 10,
                unitTitle = "Wedge work",
            )
        },
    ),
    snapshotVersion = 1,
    completedSteps = completedStepIndices.map {
        com.loganmartlew.rangework.shared.model.CompletedStep(
            stepIndex = it,
            completedAt = Instant.parse("2026-06-19T09:00:00Z"),
        )
    },
    clubOverrides = emptyMap(),
    lastViewedStepIndex = lastViewedStepIndex,
    startedAt = Instant.parse("2026-06-18T08:00:00Z"),
)
