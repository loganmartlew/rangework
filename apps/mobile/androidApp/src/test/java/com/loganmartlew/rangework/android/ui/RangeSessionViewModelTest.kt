package com.loganmartlew.rangework.android.ui

import com.loganmartlew.rangework.shared.model.ActiveRangeSessionSummary
import com.loganmartlew.rangework.shared.model.BlockResult
import com.loganmartlew.rangework.shared.model.CompletedRangeSessionSummary
import com.loganmartlew.rangework.shared.model.CompletedStep
import com.loganmartlew.rangework.shared.model.Handedness
import com.loganmartlew.rangework.shared.model.MeasurementPreferences
import com.loganmartlew.rangework.shared.model.Observation
import com.loganmartlew.rangework.shared.model.RangeSession
import com.loganmartlew.rangework.shared.model.RangeSessionSnapshot
import com.loganmartlew.rangework.shared.model.SnapshotInstruction
import com.loganmartlew.rangework.shared.model.SnapshotStep
import com.loganmartlew.rangework.shared.model.SnapshotUnit
import com.loganmartlew.rangework.shared.recording.DefaultRangeSessionRecorder
import com.loganmartlew.rangework.shared.recording.RangeSessionRecorder
import com.loganmartlew.rangework.shared.repository.MeasurementPreferencesRepository
import com.loganmartlew.rangework.shared.repository.RangeSessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RangeSessionViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    // ── loading and landing ──────────────────────────────────────────────────

    @Test
    fun loadsSessionAndLandsOnFirstBlock() = runTest {
        val session = twoBlockSession()
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(session.id, state.rangeSession?.id)
        assertEquals(0, state.currentBlockIndex)
        assertNull(state.statusMessage)
    }

    @Test
    fun landsOnFirstIncompleteBlock() = runTest {
        // Block 0 (steps 0-2) complete, block 1 (steps 3-4) untouched.
        val session = twoBlockSession(completedStepIndices = setOf(0, 1, 2))
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.currentBlockIndex)
    }

    @Test
    fun landsOnLastBlockWhenAllComplete() = runTest {
        val session = twoBlockSession(completedStepIndices = setOf(0, 1, 2, 3, 4))
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.currentBlockIndex)
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
    fun nullRepositorySetsErrorMessage() = runTest {
        val viewModel = RangeSessionViewModel(
            rangeSessionId = "any-id",
            rangeSessionRepository = null,
        )
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertNull(state.rangeSession)
        assertTrue("Expected error message for null repository", state.statusMessage != null)
    }

    @Test
    fun loadSessionPopulatesCompletedStepIndices() = runTest {
        val session = twoBlockSession(completedStepIndices = setOf(0, 2))
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        assertEquals(setOf(0, 2), viewModel.uiState.value.completedStepIndices)
    }

    // ── block navigation ─────────────────────────────────────────────────────

    @Test
    fun navigateToBlockSetsIndex() = runTest {
        val session = twoBlockSession()
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        viewModel.navigateToBlock(1)

        assertEquals(1, viewModel.uiState.value.currentBlockIndex)
    }

    @Test
    fun navigateToBlockClampsOutOfBounds() = runTest {
        val session = twoBlockSession()
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        viewModel.navigateToBlock(100)

        assertEquals(1, viewModel.uiState.value.currentBlockIndex)
    }

    // ── counter increment ────────────────────────────────────────────────────

    @Test
    fun incrementCompletesNextBallStepOptimistically() = runTest {
        // Block 0: action step 0, ball steps 1-2.
        val session = twoBlockSession()
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        viewModel.incrementBlock(0)

        // Sweeps the leading action step along with the first ball step.
        assertEquals(setOf(0, 1), viewModel.uiState.value.completedStepIndices)
    }

    @Test
    fun incrementPersistsBatchToRepository() = runTest {
        val session = twoBlockSession()
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        viewModel.incrementBlock(0)
        advanceUntilIdle()

        assertEquals(1, repo.completionInvocations.size)
        assertEquals(Triple(session.id, listOf(0, 1), true), repo.completionInvocations.first())
        assertEquals(2, viewModel.uiState.value.rangeSession?.completedSteps?.size)
    }

    @Test
    fun incrementOnCompleteBlockDoesNothing() = runTest {
        val session = twoBlockSession(completedStepIndices = setOf(0, 1, 2))
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        viewModel.incrementBlock(0)
        advanceUntilIdle()

        assertTrue(repo.completionInvocations.isEmpty())
    }

    @Test
    fun incrementRevertsOnNetworkFailure() = runTest {
        val session = twoBlockSession()
        val repo = FakeRangeSessionRepo(
            sessions = mutableListOf(session),
            shouldFailOnCompletion = true,
        )
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        viewModel.incrementBlock(0)
        assertEquals(setOf(0, 1), viewModel.uiState.value.completedStepIndices)

        advanceUntilIdle() // network fails → revert

        val state = viewModel.uiState.value
        assertTrue(state.completedStepIndices.isEmpty())
        assertTrue("Expected error notification after failure", state.notification != null)
    }

    @Test
    fun failedIncrementDoesNotRevertOtherBlocks() = runTest {
        val session = twoBlockSession()
        val repo = object : FakeRangeSessionRepo(sessions = mutableListOf(session)) {
            override suspend fun setStepsCompletion(
                rangeSessionId: String,
                stepIndices: List<Int>,
                completed: Boolean,
            ): RangeSession {
                if (3 in stepIndices) throw RuntimeException("Simulated network error")
                return super.setStepsCompletion(rangeSessionId, stepIndices, completed)
            }
        }
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        viewModel.incrementBlock(0) // steps 0, 1 — will succeed
        viewModel.incrementBlock(1) // step 3 — will fail

        assertEquals(setOf(0, 1, 3), viewModel.uiState.value.completedStepIndices)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Block 0 steps should remain complete", setOf(0, 1).all { it in state.completedStepIndices })
        assertFalse("Block 1 step should revert", 3 in state.completedStepIndices)
    }

    // ── counter decrement ────────────────────────────────────────────────────

    @Test
    fun decrementIsExactInverseOfIncrement() = runTest {
        val session = twoBlockSession()
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        viewModel.incrementBlock(0)
        advanceUntilIdle()
        assertEquals(setOf(0, 1), viewModel.uiState.value.completedStepIndices)

        viewModel.decrementBlock(0)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.completedStepIndices.isEmpty())
        assertEquals(
            Triple(session.id, listOf(0, 1), false),
            repo.completionInvocations.last(),
        )
    }

    @Test
    fun decrementOnUntouchedBlockDoesNothing() = runTest {
        val session = twoBlockSession()
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        viewModel.decrementBlock(0)
        advanceUntilIdle()

        assertTrue(repo.completionInvocations.isEmpty())
    }

    // ── action instruction check-off ─────────────────────────────────────────

    @Test
    fun toggleActionInstructionCompletesNextStep() = runTest {
        val session = twoBlockSession()
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        // Block 0 instruction 0 is the action step (index 0).
        viewModel.toggleActionInstruction(0, 0)
        advanceUntilIdle()

        assertTrue(0 in viewModel.uiState.value.completedStepIndices)
    }

    @Test
    fun toggleActionInstructionReopensWhenAllComplete() = runTest {
        val session = twoBlockSession(completedStepIndices = setOf(0))
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        viewModel.toggleActionInstruction(0, 0)
        advanceUntilIdle()

        assertFalse(0 in viewModel.uiState.value.completedStepIndices)
    }

    // ── club swap ────────────────────────────────────────────────────────────

    @Test
    fun swapClubFansOutToRemainingIncompleteSteps() = runTest {
        // Block 1 has ball steps 3, 4 on instruction 0; step 3 already hit.
        val session = twoBlockSession(completedStepIndices = setOf(3))
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        viewModel.swapClub(1, 0, "seven_iron")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull("Completed step keeps its club", state.rangeSession?.clubOverrides?.get("3"))
        assertEquals("seven_iron", state.rangeSession?.clubOverrides?.get("4"))
        assertEquals(1, repo.overrideInvocations.size)
        assertEquals(Triple(session.id, listOf(4), "seven_iron"), repo.overrideInvocations.first())
    }

    @Test
    fun swapClubUpdatesOptimistically() = runTest {
        val session = twoBlockSession()
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        viewModel.swapClub(1, 0, "seven_iron")

        assertEquals("seven_iron", viewModel.uiState.value.rangeSession?.clubOverrides?.get("3"))
    }

    @Test
    fun swapClubRevertsOnFailure() = runTest {
        val session = twoBlockSession()
        val repo = FakeRangeSessionRepo(
            sessions = mutableListOf(session),
            shouldFailOnOverride = true,
        )
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        viewModel.swapClub(1, 0, "seven_iron")
        assertTrue(viewModel.uiState.value.rangeSession?.clubOverrides?.isNotEmpty() == true)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Club overrides should revert to empty", state.rangeSession?.clubOverrides?.isEmpty() == true)
        assertTrue("Expected error notification", state.notification != null)
    }

    // ── finish flow ──────────────────────────────────────────────────────────

    @Test
    fun requestFinishWithIncompleteStepsShowsDialog() = runTest {
        val session = twoBlockSession(completedStepIndices = setOf(0, 1))
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        viewModel.requestFinish()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.showFinishDialog)
        assertNull(viewModel.uiState.value.finishSummary)
        assertNull(repo.finishedSessionId)
    }

    @Test
    fun requestFinishWhenAllCompleteFinishesDirectly() = runTest {
        val session = twoBlockSession(completedStepIndices = setOf(0, 1, 2, 3, 4))
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        viewModel.requestFinish()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.showFinishDialog)
        assertNotNull(state.finishSummary)
        assertEquals(session.id, repo.finishedSessionId)
    }

    @Test
    fun completeRemainingAndFinishBatchCompletesThenFinishes() = runTest {
        val session = twoBlockSession(completedStepIndices = setOf(0, 1))
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        viewModel.requestFinish()
        viewModel.completeRemainingAndFinish()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.showFinishDialog)
        assertNotNull(state.finishSummary)
        assertEquals(setOf(0, 1, 2, 3, 4), state.completedStepIndices)
        assertEquals(session.id, repo.finishedSessionId)
        // Remaining steps went through a single batch write.
        assertEquals(Triple(session.id, listOf(2, 3, 4), true), repo.completionInvocations.last())
    }

    @Test
    fun finishAsIsFinishesWithIncompleteSteps() = runTest {
        val session = twoBlockSession(completedStepIndices = setOf(0, 1))
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        viewModel.requestFinish()
        viewModel.finishAsIs()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.showFinishDialog)
        assertNotNull(state.finishSummary)
        assertEquals(setOf(0, 1), state.completedStepIndices)
        assertEquals(session.id, repo.finishedSessionId)
        assertTrue("No batch completion expected", repo.completionInvocations.isEmpty())
    }

    @Test
    fun dismissFinishDialogHidesDialog() = runTest {
        val session = twoBlockSession()
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        viewModel.requestFinish()
        assertTrue(viewModel.uiState.value.showFinishDialog)

        viewModel.dismissFinishDialog()

        assertFalse(viewModel.uiState.value.showFinishDialog)
        assertNull(repo.finishedSessionId)
    }

    // ── notifications ────────────────────────────────────────────────────────

    @Test
    fun consumeNotificationClearsNotification() = runTest {
        val session = twoBlockSession()
        val repo = FakeRangeSessionRepo(
            sessions = mutableListOf(session),
            shouldFailOnCompletion = true,
        )
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        viewModel.incrementBlock(0)
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.notification != null)

        viewModel.consumeNotification()
        assertNull(viewModel.uiState.value.notification)
    }

    // ── timer ────────────────────────────────────────────────────────────────

    @Test
    fun timerIsNotRunningInitially() = runTest {
        val session = twoBlockSession()
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo = repo, rangeSessionId = session.id)
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.isTimerRunning)
        assertEquals(0L, viewModel.uiState.value.elapsedSeconds)
    }

    @Test
    fun timerStartsOnScreenEnter() = runTest {
        val session = twoBlockSession()
        val repo = FakeRangeSessionRepo(
            sessions = mutableListOf(session),
            getElapsedSecondsResult = 45L,
        )
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
        val session = twoBlockSession()
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
        val session = twoBlockSession()
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
        val session = twoBlockSession()
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
    fun doubleEnterGuardPreventsMultipleTimeEntries() = runTest {
        val session = twoBlockSession()
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
        val session = twoBlockSession()
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

    // ── data capture: session note ───────────────────────────────────────────

    @Test
    fun saveSessionNotePersistsOnV3() = runTest {
        val session = v3CriterionSession()
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo, session.id, DefaultRangeSessionRecorder(repo))
        advanceUntilIdle()

        viewModel.saveSessionNote("Great tempo today")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals("Great tempo today", state.rangeSession?.sessionNote)
        assertFalse(state.isSavingSessionNote)
        assertNull(state.notification)
    }

    @Test
    fun saveSessionNoteRunsOnCompleteAfterSuccess() = runTest {
        val session = v3CriterionSession()
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo, session.id, DefaultRangeSessionRecorder(repo))
        advanceUntilIdle()

        var completed = false
        viewModel.saveSessionNote("Done note") { completed = true }
        advanceUntilIdle()

        assertTrue("onComplete should run after a successful flush", completed)
    }

    @Test
    fun saveSessionNoteRejectedOnV2ShowsNotificationAndDoesNotCompleteOrChangeModel() = runTest {
        val session = twoBlockSession() // v2
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo, session.id, DefaultRangeSessionRecorder(repo))
        advanceUntilIdle()

        var completed = false
        viewModel.saveSessionNote("Should be rejected") { completed = true }
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull("v2 session note stays unwritten", state.rangeSession?.sessionNote)
        assertFalse("Rejected flush must not navigate", completed)
        assertTrue("Expected rejection notification", state.notification != null)
        assertFalse(state.isSavingSessionNote)
    }

    @Test
    fun saveSessionNoteWithNullRecorderStillRunsOnComplete() = runTest {
        val session = v3CriterionSession()
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo, session.id, recorder = null)
        advanceUntilIdle()

        var completed = false
        viewModel.saveSessionNote("note") { completed = true }
        advanceUntilIdle()

        assertTrue("Null recorder is a no-op that still lets Done proceed", completed)
        assertNull(viewModel.uiState.value.rangeSession?.sessionNote)
    }

    // ── data capture: block note ─────────────────────────────────────────────

    @Test
    fun saveBlockNoteLandsUnderBlockUnitIndex() = runTest {
        val session = v3CriterionSession()
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo, session.id, DefaultRangeSessionRecorder(repo))
        advanceUntilIdle()

        viewModel.saveBlockNote(blockIndex = 1, note = "Pushed a few")
        advanceUntilIdle()

        val results = viewModel.uiState.value.rangeSession?.blockResults
        assertEquals("Pushed a few", results?.get("1")?.note)
        assertNull("Sibling block untouched", results?.get("0"))
        assertTrue(viewModel.uiState.value.savingBlockNoteIndices.isEmpty())
    }

    @Test
    fun saveBlockNoteFailureRevertsAndNotifies() = runTest {
        val session = v3CriterionSession()
        val repo = FakeRangeSessionRepo(
            sessions = mutableListOf(session),
            shouldFailOnBlockResult = true,
        )
        val viewModel = makeViewModel(repo, session.id, DefaultRangeSessionRecorder(repo))
        advanceUntilIdle()

        viewModel.saveBlockNote(blockIndex = 0, note = "won't stick")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.rangeSession?.blockResults?.get("0"))
        assertTrue("Expected error notification", state.notification != null)
        assertTrue(state.savingBlockNoteIndices.isEmpty())
    }

    // ── data capture: manual count ───────────────────────────────────────────

    @Test
    fun saveManualCountPersistsOptimisticallyAndConfirms() = runTest {
        val session = v3CriterionSession()
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo, session.id, DefaultRangeSessionRecorder(repo))
        advanceUntilIdle()

        viewModel.saveManualCount(blockIndex = 0, count = 2)
        // Optimistic: value visible before the write settles.
        assertEquals(2, viewModel.uiState.value.rangeSession?.blockResults?.get("0")?.manualCount)

        advanceUntilIdle()
        assertEquals(2, viewModel.uiState.value.rangeSession?.blockResults?.get("0")?.manualCount)
    }

    @Test
    fun saveManualCountDistinctUnitIndicesForSameUnitTwice() = runTest {
        val session = v3CriterionSession()
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo, session.id, DefaultRangeSessionRecorder(repo))
        advanceUntilIdle()

        viewModel.saveManualCount(blockIndex = 0, count = 1)
        advanceUntilIdle()
        viewModel.saveManualCount(blockIndex = 1, count = 2)
        advanceUntilIdle()

        val results = viewModel.uiState.value.rangeSession?.blockResults
        assertEquals(1, results?.get("0")?.manualCount)
        assertEquals(2, results?.get("1")?.manualCount)
    }

    @Test
    fun saveManualCountNullClearsTheCount() = runTest {
        val session = v3CriterionSession(
            blockResults = mapOf("0" to BlockResult(manualCount = 3)),
        )
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo, session.id, DefaultRangeSessionRecorder(repo))
        advanceUntilIdle()

        viewModel.saveManualCount(blockIndex = 0, count = null)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.rangeSession?.blockResults?.get("0"))
    }

    @Test
    fun saveManualCountRevertsOnFailure() = runTest {
        val session = v3CriterionSession()
        val repo = FakeRangeSessionRepo(
            sessions = mutableListOf(session),
            shouldFailOnBlockResult = true,
        )
        val viewModel = makeViewModel(repo, session.id, DefaultRangeSessionRecorder(repo))
        advanceUntilIdle()

        viewModel.saveManualCount(blockIndex = 0, count = 2)
        assertEquals(2, viewModel.uiState.value.rangeSession?.blockResults?.get("0")?.manualCount)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull("Count reverts on failure", state.rangeSession?.blockResults?.get("0"))
        assertTrue("Expected error notification", state.notification != null)
    }

    @Test
    fun saveManualCountRapidTapsKeepTheLatestValueWhenWritesLandOutOfOrder() = runTest {
        val session = v3CriterionSession()
        // The first write is slow, the second fast. The writer must send the
        // second only after the first settles: cancelling a client coroutine
        // cannot cancel an HTTP request already received by Supabase.
        val repo = FakeRangeSessionRepo(
            sessions = mutableListOf(session),
            blockResultDelaysMillis = mutableListOf(100L, 1L),
            nonCancellableBlockResultCalls = setOf(0),
        )
        val viewModel = makeViewModel(repo, session.id, DefaultRangeSessionRecorder(repo))
        advanceUntilIdle()

        viewModel.saveManualCount(blockIndex = 0, count = 1)
        // Let the first request reach the repository before tapping again.
        runCurrent()
        viewModel.saveManualCount(blockIndex = 0, count = 2)
        advanceUntilIdle()

        assertEquals(
            "Latest tap wins",
            2,
            viewModel.uiState.value.rangeSession?.blockResults?.get("0")?.manualCount,
        )
        // Both requests may reach the repository, but their order is stable.
        assertEquals(2, repo.getSession(session.id)?.blockResults?.get("0")?.manualCount)
        assertNull("No spurious failure snackbar", viewModel.uiState.value.notification)
    }

    @Test
    fun saveManualCountNoOpsWhenValueIsUnchanged() = runTest {
        val session = v3CriterionSession(
            blockResults = mapOf("0" to BlockResult(manualCount = 2)),
        )
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session), shouldFailOnBlockResult = true)
        val viewModel = makeViewModel(repo, session.id, DefaultRangeSessionRecorder(repo))
        advanceUntilIdle()

        // Re-tapping the value already shown must not write (a failing repo would
        // surface a snackbar and revert if it did).
        viewModel.saveManualCount(blockIndex = 0, count = 2)
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.rangeSession?.blockResults?.get("0")?.manualCount)
        assertNull(viewModel.uiState.value.notification)
    }

    @Test
    fun saveManualCountFailureRevertsOnlyTheCountNotTheWholeSession() = runTest {
        val session = v3CriterionSession(
            blockResults = mapOf("0" to BlockResult(note = "keep me", manualCount = 1)),
        )
        val repo = FakeRangeSessionRepo(
            sessions = mutableListOf(session),
            shouldFailOnBlockResult = true,
        )
        val viewModel = makeViewModel(repo, session.id, DefaultRangeSessionRecorder(repo))
        advanceUntilIdle()

        viewModel.saveManualCount(blockIndex = 0, count = 3)
        advanceUntilIdle()

        val result = viewModel.uiState.value.rangeSession?.blockResults?.get("0")
        assertEquals("Count reverts to the pre-tap value", 1, result?.manualCount)
        assertEquals("The block's note survives the revert", "keep me", result?.note)
        assertTrue("Expected error notification", viewModel.uiState.value.notification != null)
    }

    @Test
    fun saveManualCountFailurePreservesANoteSavedWhileTheCountWasInFlight() = runTest {
        val session = v3CriterionSession()
        val repo = FakeRangeSessionRepo(
            sessions = mutableListOf(session),
            // The count request is already in flight when the note save starts.
            // It fails after the note has completed successfully.
            blockResultDelaysMillis = mutableListOf(100L, 1L),
            failingBlockResultCalls = mutableSetOf(0),
        )
        val viewModel = makeViewModel(repo, session.id, DefaultRangeSessionRecorder(repo))
        advanceUntilIdle()

        viewModel.saveManualCount(blockIndex = 0, count = 2)
        runCurrent()
        viewModel.saveBlockNote(blockIndex = 0, note = "Kept after count failure")
        advanceUntilIdle()

        val result = viewModel.uiState.value.rangeSession?.blockResults?.get("0")
        assertNull("Failed count returns to its persisted value", result?.manualCount)
        assertEquals("Concurrent note survives count rollback", "Kept after count failure", result?.note)
        assertTrue("Expected error notification", viewModel.uiState.value.notification != null)
    }

    @Test
    fun saveManualCountWithNullRecorderIsNoOp() = runTest {
        val session = v3CriterionSession()
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo, session.id, recorder = null)
        advanceUntilIdle()

        viewModel.saveManualCount(blockIndex = 0, count = 2)
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.rangeSession?.blockResults?.get("0"))
        assertNull(viewModel.uiState.value.notification)
    }

    // ── data capture: observation staging & auto-commit ──────────────────────

    private fun captureViewModel(
        session: RangeSession,
        repo: FakeRangeSessionRepo,
        handedness: Handedness? = null,
    ) = makeViewModel(
        repo = repo,
        rangeSessionId = session.id,
        recorder = DefaultRangeSessionRecorder(repo),
        measurementPreferencesRepository = handedness?.let(::FakePrefsRepo),
    )

    @Test
    fun stagingSingleTypeAutoCommitsAfterDelay() = runTest {
        val session = v3CaptureSession(observationTypes = listOf("direction"))
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = captureViewModel(session, repo)
        advanceUntilIdle()

        viewModel.stageObservation(0, "direction", "left")
        // Arms synchronously; commit is deferred to the delay.
        assertEquals(0, viewModel.uiState.value.armingBlockIndex)

        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.armingBlockIndex)
        assertTrue("First ball step completed", 0 in state.completedStepIndices)
        assertEquals(mapOf("direction" to "left"), state.observationsByStep[0]?.values)
        assertTrue("Staging cleared after commit", state.stagingByBlock.isEmpty())
        assertEquals(mapOf("direction" to "left"), repo.observations[0]?.values)
    }

    @Test
    fun noAutoCommitWhileATypeIsMissing() = runTest {
        val session = v3CaptureSession(observationTypes = listOf("direction", "shape"))
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = captureViewModel(session, repo)
        advanceUntilIdle()

        viewModel.stageObservation(0, "direction", "left")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull("Partial coverage must not arm", state.armingBlockIndex)
        assertTrue("Nothing committed", state.completedStepIndices.isEmpty())
        assertEquals(mapOf("direction" to "left"), state.stagingByBlock[0])
    }

    @Test
    fun stagingNoOpWhenNoTypesEnabled() = runTest {
        val session = v3CaptureSession(observationTypes = emptyList())
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = captureViewModel(session, repo)
        advanceUntilIdle()

        viewModel.stageObservation(0, "direction", "left")

        assertTrue(viewModel.uiState.value.stagingByBlock.isEmpty())
        assertNull(viewModel.uiState.value.armingBlockIndex)
    }

    @Test
    fun plusOnePartialCommitPersistsStagedSubset() = runTest {
        val session = v3CaptureSession(observationTypes = listOf("direction", "shape"))
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = captureViewModel(session, repo)
        advanceUntilIdle()

        viewModel.stageObservation(0, "direction", "left") // partial — no arm
        viewModel.incrementBlock(0)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(0 in state.completedStepIndices)
        assertEquals(mapOf("direction" to "left"), state.observationsByStep[0]?.values)
        assertEquals(mapOf("direction" to "left"), repo.observations[0]?.values)
    }

    @Test
    fun plusOneEmptyCommitWritesNoObservation() = runTest {
        val session = v3CaptureSession(observationTypes = listOf("direction"))
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = captureViewModel(session, repo)
        advanceUntilIdle()

        viewModel.incrementBlock(0) // nothing staged
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue(0 in state.completedStepIndices)
        assertNull("Empty commit stores no observation", state.observationsByStep[0])
        assertTrue(repo.observations.isEmpty())
    }

    @Test
    fun inputIsIgnoredDuringArmWindow() = runTest {
        val session = v3CaptureSession(observationTypes = listOf("direction"))
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = captureViewModel(session, repo)
        advanceUntilIdle()

        viewModel.stageObservation(0, "direction", "left") // arms
        assertEquals(0, viewModel.uiState.value.armingBlockIndex)

        // Everything during the arm window is a no-op.
        viewModel.stageObservation(0, "direction", "right")
        viewModel.incrementBlock(0)
        assertEquals(mapOf("direction" to "left"), viewModel.uiState.value.stagingByBlock[0])
        assertTrue(viewModel.uiState.value.completedStepIndices.isEmpty())

        advanceUntilIdle()
        // The originally staged value is what commits.
        assertEquals(mapOf("direction" to "left"), viewModel.uiState.value.observationsByStep[0]?.values)
    }

    @Test
    fun commitFailureRevertsEverythingAndNotifies() = runTest {
        val session = v3CaptureSession(observationTypes = listOf("direction"))
        val repo = FakeRangeSessionRepo(
            sessions = mutableListOf(session),
            shouldFailOnCompletion = true,
        )
        val viewModel = captureViewModel(session, repo)
        advanceUntilIdle()

        viewModel.stageObservation(0, "direction", "left")
        advanceUntilIdle() // arm fires → commit → fails

        val state = viewModel.uiState.value
        assertTrue("Counter reverts", state.completedStepIndices.isEmpty())
        assertNull("Observation reverts", state.observationsByStep[0])
        assertTrue("Staging is not restored on wholesale failure", state.stagingByBlock.isEmpty())
        assertTrue("Expected error notification", state.notification != null)
    }

    @Test
    fun decrementVoidsObservationsAndKeepsStaging() = runTest {
        val session = v3CaptureSession(
            observationTypes = listOf("direction", "shape"),
            completedStepIndices = setOf(0),
        )
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        repo.observations[0] = Observation(0, mapOf("direction" to "left"))
        val viewModel = captureViewModel(session, repo)
        advanceUntilIdle()
        assertEquals(mapOf("direction" to "left"), viewModel.uiState.value.observationsByStep[0]?.values)

        viewModel.stageObservation(0, "direction", "right") // partial staging on the pending ball
        viewModel.decrementBlock(0)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse("Undone ball step", 0 in state.completedStepIndices)
        assertNull("Voided observation", state.observationsByStep[0])
        assertNull(repo.observations[0])
        assertEquals("Staging survives the undo", mapOf("direction" to "right"), state.stagingByBlock[0])
    }

    @Test
    fun decrementFailureRestoresCounterAndObservation() = runTest {
        val session = v3CaptureSession(
            observationTypes = listOf("direction"),
            completedStepIndices = setOf(0),
        )
        val repo = FakeRangeSessionRepo(
            sessions = mutableListOf(session),
            shouldFailOnCompletion = true,
        )
        repo.observations[0] = Observation(0, mapOf("direction" to "left"))
        val viewModel = captureViewModel(session, repo)
        advanceUntilIdle()

        viewModel.decrementBlock(0)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Counter restored", 0 in state.completedStepIndices)
        assertEquals(mapOf("direction" to "left"), state.observationsByStep[0]?.values)
        assertTrue("Expected error notification", state.notification != null)
    }

    @Test
    fun editWriteThroughUpsertsFullMapAndTogglesOff() = runTest {
        val session = v3CaptureSession(
            observationTypes = listOf("direction", "shape"),
            completedStepIndices = setOf(0),
        )
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        repo.observations[0] = Observation(0, mapOf("direction" to "left"))
        val viewModel = captureViewModel(session, repo)
        advanceUntilIdle()

        // Add a second type: the full map is upserted, not merged per key.
        viewModel.updateBallObservation(0, "shape", "straight_left")
        advanceUntilIdle()
        assertEquals(
            mapOf("direction" to "left", "shape" to "straight_left"),
            repo.observations[0]?.values,
        )

        // Re-selecting a set value toggles it off.
        viewModel.updateBallObservation(0, "direction", "left")
        advanceUntilIdle()
        assertEquals(mapOf("shape" to "straight_left"), viewModel.uiState.value.observationsByStep[0]?.values)

        // Emptying the ball upserts an empty map (D2-equivalent to no row).
        viewModel.updateBallObservation(0, "shape", "straight_left")
        advanceUntilIdle()
        assertEquals(emptyMap<String, String>(), viewModel.uiState.value.observationsByStep[0]?.values)
        assertEquals(emptyMap<String, String>(), repo.observations[0]?.values)
    }

    @Test
    fun editWriteThroughRevertsOnFailure() = runTest {
        val session = v3CaptureSession(
            observationTypes = listOf("direction"),
            completedStepIndices = setOf(0),
        )
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        repo.observations[0] = Observation(0, mapOf("direction" to "left"))
        val viewModel = captureViewModel(session, repo)
        advanceUntilIdle()
        repo.shouldFailOnObservation = true

        viewModel.updateBallObservation(0, "direction", "right")
        advanceUntilIdle()

        assertEquals(
            "Reverts to the pre-edit record",
            mapOf("direction" to "left"),
            viewModel.uiState.value.observationsByStep[0]?.values,
        )
        assertTrue(viewModel.uiState.value.notification != null)
    }

    @Test
    fun v2SessionIgnoresCaptureAndUsesPlainPaths() = runTest {
        val session = twoBlockSession() // v2
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = captureViewModel(session, repo)
        advanceUntilIdle()

        viewModel.stageObservation(0, "direction", "left")
        assertTrue("v2 rejects staging", viewModel.uiState.value.stagingByBlock.isEmpty())

        viewModel.incrementBlock(0)
        advanceUntilIdle()
        // Plain v2 increment sweeps the leading action step with the first ball step.
        assertEquals(setOf(0, 1), viewModel.uiState.value.completedStepIndices)
        assertTrue("No observations on v2", repo.observations.isEmpty())
    }

    @Test
    fun nullRecorderCaptureMethodsNoOpWithoutCrash() = runTest {
        val session = v3CaptureSession(observationTypes = listOf("direction"))
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = makeViewModel(repo, session.id, recorder = null)
        advanceUntilIdle()

        viewModel.updateBallObservation(0, "direction", "left")
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.observationsByStep[0])
        assertNull(viewModel.uiState.value.notification)

        // A commit still counts the ball and clears its staging (no leak onto the
        // next ball) even though the observation can't be written.
        viewModel.stageObservation(0, "direction", "left")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertTrue("Counter still advances", 0 in state.completedStepIndices)
        assertTrue("Staging cleared after degraded commit", state.stagingByBlock.isEmpty())
        assertNull(state.observationsByStep[0])
    }

    @Test
    fun handednessLoadsFromPreferences() = runTest {
        val session = v3CaptureSession()
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = captureViewModel(session, repo, handedness = Handedness.LEFT)
        advanceUntilIdle()

        assertEquals(Handedness.LEFT, viewModel.uiState.value.handedness)
    }

    @Test
    fun handednessFallsBackToRightWithoutPreferencesRepo() = runTest {
        val session = v3CaptureSession()
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        val viewModel = captureViewModel(session, repo, handedness = null)
        advanceUntilIdle()

        assertEquals(Handedness.RIGHT, viewModel.uiState.value.handedness)
    }

    @Test
    fun observationsLoadKeyedByGlobalStepIndex() = runTest {
        val session = v3CaptureSession(observationTypes = listOf("direction"))
        val repo = FakeRangeSessionRepo(sessions = mutableListOf(session))
        repo.observations[1] = Observation(1, mapOf("direction" to "right"))
        repo.observations[2] = Observation(2, mapOf("direction" to "on_line"))
        val viewModel = captureViewModel(session, repo)
        advanceUntilIdle()

        val loaded = viewModel.uiState.value.observationsByStep
        assertEquals(mapOf("direction" to "right"), loaded[1]?.values)
        assertEquals(mapOf("direction" to "on_line"), loaded[2]?.values)
        assertNull(loaded[0])
    }
}

/**
 * Two blocks, both with a Success Criterion and no Success Observation Type, on a
 * v3 snapshot — manual-count-eligible. Block 0 has ball steps 0-1, block 1 has
 * ball steps 2-3.
 */
private fun v3CriterionSession(
    id: String = "range-session-v3",
    blockResults: Map<String, BlockResult> = emptyMap(),
): RangeSession = RangeSession(
    id = id,
    sourceSessionId = "session-1",
    sessionName = "Criterion block",
    snapshot = RangeSessionSnapshot(
        units = listOf(
            SnapshotUnit(
                unitTitle = "Wedge A",
                repeatCount = 1,
                instructions = listOf(SnapshotInstruction(text = "Hit", ballCount = 2)),
                successCriterion = "Within 10 feet",
            ),
            SnapshotUnit(
                unitTitle = "Wedge B",
                repeatCount = 1,
                instructions = listOf(SnapshotInstruction(text = "Hit", ballCount = 2)),
                successCriterion = "Within 10 feet",
            ),
        ),
        steps = listOf(
            snapshotStep(unitIndex = 0, instructionIndex = 0, text = "Hit", ballCount = 1),
            snapshotStep(unitIndex = 0, instructionIndex = 0, text = "Hit", ballCount = 1),
            snapshotStep(unitIndex = 1, instructionIndex = 0, text = "Hit", ballCount = 1),
            snapshotStep(unitIndex = 1, instructionIndex = 0, text = "Hit", ballCount = 1),
        ),
    ),
    snapshotVersion = 3,
    completedSteps = emptyList(),
    clubOverrides = emptyMap(),
    blockResults = blockResults,
    startedAt = Instant.parse("2026-06-18T08:00:00Z"),
)

private fun makeViewModel(
    repo: FakeRangeSessionRepo,
    rangeSessionId: String,
    recorder: RangeSessionRecorder? = null,
    measurementPreferencesRepository: MeasurementPreferencesRepository? = null,
    autoCommitDelayMillis: Long = 300,
): RangeSessionViewModel = RangeSessionViewModel(
    rangeSessionId = rangeSessionId,
    rangeSessionRepository = repo,
    rangeSessionRecorder = recorder,
    measurementPreferencesRepository = measurementPreferencesRepository,
    autoCommitDelayMillis = autoCommitDelayMillis,
)

/** Fixed-handedness preferences fake for the capture load path. */
private class FakePrefsRepo(private val handedness: Handedness) : MeasurementPreferencesRepository() {
    override suspend fun persist(validated: MeasurementPreferences): MeasurementPreferences = validated
    override suspend fun get(): MeasurementPreferences = MeasurementPreferences(handedness = handedness)
}

/**
 * A single-block v3 session with three ball steps (indices 0-2) on one instruction,
 * [observationTypes] enabled — the substrate for the per-ball capture tests.
 */
private fun v3CaptureSession(
    id: String = "range-capture",
    observationTypes: List<String> = listOf("direction"),
    successCriterion: String? = null,
    completedStepIndices: Set<Int> = emptySet(),
): RangeSession = RangeSession(
    id = id,
    sourceSessionId = "session-1",
    sessionName = "Capture block",
    snapshot = RangeSessionSnapshot(
        units = listOf(
            SnapshotUnit(
                unitTitle = "Wedge work",
                repeatCount = 1,
                instructions = listOf(SnapshotInstruction(text = "Hit", ballCount = 3)),
                successCriterion = successCriterion,
                observationTypes = observationTypes,
            ),
        ),
        steps = listOf(
            snapshotStep(unitIndex = 0, instructionIndex = 0, text = "Hit", ballCount = 1),
            snapshotStep(unitIndex = 0, instructionIndex = 0, text = "Hit", ballCount = 1),
            snapshotStep(unitIndex = 0, instructionIndex = 0, text = "Hit", ballCount = 1),
        ),
    ),
    snapshotVersion = 3,
    completedSteps = completedStepIndices.map {
        CompletedStep(stepIndex = it, completedAt = Instant.parse("2026-06-19T09:00:00Z"))
    },
    clubOverrides = emptyMap(),
    startedAt = Instant.parse("2026-06-18T08:00:00Z"),
)

private open class FakeRangeSessionRepo(
    val sessions: MutableList<RangeSession> = mutableListOf(),
    var shouldFailOnCompletion: Boolean = false,
    var shouldFailOnOverride: Boolean = false,
    var getElapsedSecondsResult: Long = 0L,
    var shouldFailOnSessionNote: Boolean = false,
    var shouldFailOnBlockResult: Boolean = false,
    /**
     * Per-call latency for [saveBlockResult], consumed in invocation order
     * (missing/exhausted entries are instant). Lets a test land two writes out of
     * order, the way a real network does.
     */
    val blockResultDelaysMillis: MutableList<Long> = mutableListOf(),
    /** Zero-based saveBlockResult calls that fail after their configured delay. */
    val failingBlockResultCalls: MutableSet<Int> = mutableSetOf(),
    /** Calls whose simulated server work continues after client cancellation. */
    val nonCancellableBlockResultCalls: Set<Int> = emptySet(),
) : RangeSessionRepository {
    val completionInvocations = mutableListOf<Triple<String, List<Int>, Boolean>>()
    val overrideInvocations = mutableListOf<Triple<String, List<Int>, String>>()
    val recordedTimeEntries = mutableListOf<Pair<String, Instant>>()
    val closedTimeEntries = mutableListOf<Triple<String, Instant, Instant>>()
    var finishedSessionId: String? = null
    val observations = mutableMapOf<Int, Observation>()
    var shouldFailOnObservation: Boolean = false
    private var blockResultCallCount = 0

    override suspend fun start(sessionId: String): RangeSession =
        error("Not called in these tests")
    override suspend fun getSession(rangeSessionId: String): RangeSession? =
        sessions.firstOrNull { it.id == rangeSessionId }
    override suspend fun listActiveSessions(): List<ActiveRangeSessionSummary> = emptyList()
    override suspend fun listCompletedSessions(sessionId: String): List<CompletedRangeSessionSummary> = emptyList()
    override suspend fun setStepsCompletion(
        rangeSessionId: String,
        stepIndices: List<Int>,
        completed: Boolean,
    ): RangeSession {
        if (shouldFailOnCompletion) throw RuntimeException("Simulated network error")
        completionInvocations.add(Triple(rangeSessionId, stepIndices, completed))
        val session = sessions.firstOrNull { it.id == rangeSessionId } ?: error("Session not found")
        val updatedCompletedSteps = if (completed) {
            val already = session.completedSteps.map { it.stepIndex }.toSet()
            session.completedSteps + stepIndices
                .filter { it !in already }
                .map { CompletedStep(it, Instant.parse("2026-06-19T10:00:00Z")) }
        } else {
            val toRemove = stepIndices.toSet()
            session.completedSteps.filter { it.stepIndex !in toRemove }
        }
        val updated = session.copy(completedSteps = updatedCompletedSteps)
        sessions.removeAll { it.id == rangeSessionId }
        sessions.add(updated)
        return updated
    }
    override suspend fun overrideStepClubs(
        rangeSessionId: String,
        stepIndices: List<Int>,
        clubCode: String,
    ): RangeSession {
        if (shouldFailOnOverride) throw RuntimeException("Simulated network error")
        overrideInvocations.add(Triple(rangeSessionId, stepIndices, clubCode))
        val session = sessions.firstOrNull { it.id == rangeSessionId } ?: error("Session not found")
        val updated = session.copy(
            clubOverrides = session.clubOverrides + stepIndices.map { it.toString() to clubCode },
        )
        sessions.removeAll { it.id == rangeSessionId }
        sessions.add(updated)
        return updated
    }
    override suspend fun finishSession(rangeSessionId: String): RangeSession {
        finishedSessionId = rangeSessionId
        val session = sessions.firstOrNull { it.id == rangeSessionId } ?: error("Session not found")
        val updated = session.copy(completedAt = Instant.parse("2026-06-19T11:00:00Z"))
        sessions.removeAll { it.id == rangeSessionId }
        sessions.add(updated)
        return updated
    }
    override suspend fun abandonSession(rangeSessionId: String) = Unit
    override suspend fun saveSessionNote(rangeSessionId: String, note: String?): RangeSession {
        if (shouldFailOnSessionNote) throw RuntimeException("Simulated network error")
        val session = sessions.firstOrNull { it.id == rangeSessionId } ?: error("Session not found")
        val updated = session.copy(sessionNote = note)
        sessions.removeAll { it.id == rangeSessionId }
        sessions.add(updated)
        return updated
    }
    override suspend fun saveBlockResult(
        rangeSessionId: String,
        unitIndex: Int,
        result: BlockResult,
    ): RangeSession {
        val callIndex = blockResultCallCount++
        suspend fun persist(): RangeSession {
            blockResultDelaysMillis.removeFirstOrNull()?.let { delay(it) }
            if (shouldFailOnBlockResult || callIndex in failingBlockResultCalls) {
                throw RuntimeException("Simulated network error")
            }
            val session = sessions.firstOrNull { it.id == rangeSessionId } ?: error("Session not found")
            val key = unitIndex.toString()
            val updatedResults = if (result.isEmpty) {
                session.blockResults - key
            } else {
                session.blockResults + (key to result)
            }
            val updated = session.copy(blockResults = updatedResults)
            sessions.removeAll { it.id == rangeSessionId }
            sessions.add(updated)
            return updated
        }
        return if (callIndex in nonCancellableBlockResultCalls) {
            withContext(NonCancellable) { persist() }
        } else {
            persist()
        }
    }
    override suspend fun listObservations(rangeSessionId: String): List<Observation> =
        observations.values.sortedBy(Observation::stepIndex)
    override suspend fun upsertObservation(
        rangeSessionId: String,
        stepIndex: Int,
        values: Map<String, String>,
    ): Observation {
        if (shouldFailOnObservation) throw RuntimeException("Simulated network error")
        val observation = Observation(stepIndex, values)
        observations[stepIndex] = observation
        return observation
    }
    override suspend fun deleteObservations(rangeSessionId: String, stepIndices: List<Int>) {
        stepIndices.forEach(observations::remove)
    }
    override suspend fun recordTimeEntry(rangeSessionId: String, enteredAt: Instant) {
        recordedTimeEntries.add(Pair(rangeSessionId, enteredAt))
    }
    override suspend fun closeTimeEntry(rangeSessionId: String, enteredAt: Instant, exitedAt: Instant) {
        closedTimeEntries.add(Triple(rangeSessionId, enteredAt, exitedAt))
    }
    override suspend fun getElapsedSeconds(rangeSessionId: String): Long = getElapsedSecondsResult
    override suspend fun hasActiveSessionsForTemplate(sessionId: String): Boolean = false
}

/**
 * Two blocks, five steps (ball-granular, snapshot v2 shape):
 * - Block 0 "Wedge work": action step 0 (rehearse), ball steps 1, 2.
 * - Block 1 "Driver work": ball steps 3, 4 with a club.
 */
private fun twoBlockSession(
    id: String = "range-session-1",
    completedStepIndices: Set<Int> = emptySet(),
): RangeSession = RangeSession(
    id = id,
    sourceSessionId = "session-1",
    sessionName = "Morning block",
    snapshot = RangeSessionSnapshot(
        units = listOf(
            SnapshotUnit(
                unitTitle = "Wedge work",
                unitFocus = "Tempo",
                repeatCount = 1,
                instructions = listOf(
                    SnapshotInstruction(text = "Rehearse", ballCount = 0),
                    SnapshotInstruction(text = "Hit", ballCount = 2),
                ),
            ),
            SnapshotUnit(
                unitTitle = "Driver work",
                repeatCount = 1,
                instructions = listOf(
                    SnapshotInstruction(text = "Drive", ballCount = 2, club = "driver"),
                ),
            ),
        ),
        steps = listOf(
            snapshotStep(unitIndex = 0, instructionIndex = 0, text = "Rehearse", ballCount = 0),
            snapshotStep(unitIndex = 0, instructionIndex = 1, text = "Hit", ballCount = 1),
            snapshotStep(unitIndex = 0, instructionIndex = 1, text = "Hit", ballCount = 1),
            snapshotStep(unitIndex = 1, instructionIndex = 0, text = "Drive", ballCount = 1, club = "driver"),
            snapshotStep(unitIndex = 1, instructionIndex = 0, text = "Drive", ballCount = 1, club = "driver"),
        ),
    ),
    snapshotVersion = 2,
    completedSteps = completedStepIndices.map {
        CompletedStep(
            stepIndex = it,
            completedAt = Instant.parse("2026-06-19T09:00:00Z"),
        )
    },
    clubOverrides = emptyMap(),
    startedAt = Instant.parse("2026-06-18T08:00:00Z"),
)

private fun snapshotStep(
    unitIndex: Int,
    instructionIndex: Int,
    text: String,
    ballCount: Int?,
    club: String? = null,
): SnapshotStep = SnapshotStep(
    unitIndex = unitIndex,
    instructionIndex = instructionIndex,
    repNumber = 1,
    totalReps = 1,
    instructionText = text,
    ballCount = ballCount,
    club = club,
    clubDisplayName = club,
    unitTitle = if (unitIndex == 0) "Wedge work" else "Driver work",
)
