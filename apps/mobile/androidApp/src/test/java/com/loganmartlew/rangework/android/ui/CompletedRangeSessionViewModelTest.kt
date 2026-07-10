package com.loganmartlew.rangework.android.ui

import com.loganmartlew.rangework.shared.model.ActiveRangeSessionSummary
import com.loganmartlew.rangework.shared.model.BlockResult
import com.loganmartlew.rangework.shared.model.CompletedRangeSessionSummary
import com.loganmartlew.rangework.shared.model.Observation
import com.loganmartlew.rangework.shared.model.RangeSession
import com.loganmartlew.rangework.shared.model.RangeSessionSnapshot
import com.loganmartlew.rangework.shared.model.SnapshotInstruction
import com.loganmartlew.rangework.shared.model.SnapshotStep
import com.loganmartlew.rangework.shared.model.SnapshotUnit
import com.loganmartlew.rangework.shared.recording.DefaultRangeSessionRecorder
import com.loganmartlew.rangework.shared.repository.RangeSessionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class CompletedRangeSessionViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun loadsCompletedSessionAndStats() = runTest {
        val session = completedV3Session()
        val repo = FakeRepo(mutableListOf(session), elapsed = 125L)
        val viewModel = makeVm(repo, session.id)
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isLoading)
        assertEquals(session.id, state.rangeSession?.id)
        val stats = viewModel.stats()
        assertNotNull(stats)
        assertEquals(125L, stats?.elapsedSeconds)
    }

    @Test
    fun missingSessionSetsStatusMessage() = runTest {
        val repo = FakeRepo(mutableListOf())
        val viewModel = makeVm(repo, "missing")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.rangeSession)
        assertTrue(state.statusMessage != null)
    }

    @Test
    fun saveSessionNotePersistsOnCompletedSession() = runTest {
        val session = completedV3Session()
        val repo = FakeRepo(mutableListOf(session))
        val viewModel = makeVm(repo, session.id, DefaultRangeSessionRecorder(repo))
        advanceUntilIdle()

        viewModel.saveSessionNote("Reviewed after the fact")
        advanceUntilIdle()

        assertEquals("Reviewed after the fact", viewModel.uiState.value.rangeSession?.sessionNote)
        assertFalse(viewModel.uiState.value.isSavingSessionNote)
    }

    @Test
    fun saveBlockNotePersistsUnderUnitIndex() = runTest {
        val session = completedV3Session()
        val repo = FakeRepo(mutableListOf(session))
        val viewModel = makeVm(repo, session.id, DefaultRangeSessionRecorder(repo))
        advanceUntilIdle()

        viewModel.saveBlockNote(blockIndex = 0, note = "Solid contact")
        advanceUntilIdle()

        assertEquals("Solid contact", viewModel.uiState.value.rangeSession?.blockResults?.get("0")?.note)
        assertTrue(viewModel.uiState.value.savingBlockNoteIndices.isEmpty())
    }

    @Test
    fun saveBlockNoteFailureRevertsAndNotifies() = runTest {
        val session = completedV3Session()
        val repo = FakeRepo(mutableListOf(session), failOnBlockResult = true)
        val viewModel = makeVm(repo, session.id, DefaultRangeSessionRecorder(repo))
        advanceUntilIdle()

        viewModel.saveBlockNote(blockIndex = 0, note = "won't stick")
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNull(state.rangeSession?.blockResults?.get("0"))
        assertTrue(state.notification != null)
        assertTrue(state.savingBlockNoteIndices.isEmpty())
    }

    @Test
    fun frozenManualCountSurvivesButHasNoEditPath() = runTest {
        // A count recorded while Active is displayed frozen; the VM exposes no
        // count-edit method, so it can only be read here.
        val session = completedV3Session(
            blockResults = mapOf("0" to BlockResult(manualCount = 4)),
        )
        val repo = FakeRepo(mutableListOf(session))
        val viewModel = makeVm(repo, session.id, DefaultRangeSessionRecorder(repo))
        advanceUntilIdle()

        assertEquals(4, viewModel.uiState.value.rangeSession?.blockResults?.get("0")?.manualCount)
    }
}

private fun makeVm(
    repo: FakeRepo,
    rangeSessionId: String,
    recorder: DefaultRangeSessionRecorder? = null,
): CompletedRangeSessionViewModel = CompletedRangeSessionViewModel(
    rangeSessionId = rangeSessionId,
    rangeSessionRepository = repo,
    rangeSessionRecorder = recorder,
)

private class FakeRepo(
    val sessions: MutableList<RangeSession>,
    var elapsed: Long = 0L,
    var failOnBlockResult: Boolean = false,
) : RangeSessionRepository {
    override suspend fun start(sessionId: String): RangeSession = error("unused")
    override suspend fun getSession(rangeSessionId: String): RangeSession? =
        sessions.firstOrNull { it.id == rangeSessionId }
    override suspend fun listActiveSessions(): List<ActiveRangeSessionSummary> = emptyList()
    override suspend fun listCompletedSessions(sessionId: String): List<CompletedRangeSessionSummary> = emptyList()
    override suspend fun setStepsCompletion(
        rangeSessionId: String,
        stepIndices: List<Int>,
        completed: Boolean,
    ): RangeSession = error("unused")
    override suspend fun overrideStepClubs(
        rangeSessionId: String,
        stepIndices: List<Int>,
        clubCode: String,
    ): RangeSession = error("unused")
    override suspend fun finishSession(rangeSessionId: String): RangeSession = error("unused")
    override suspend fun abandonSession(rangeSessionId: String) = Unit
    override suspend fun saveSessionNote(rangeSessionId: String, note: String?): RangeSession {
        val session = sessions.first { it.id == rangeSessionId }
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
        if (failOnBlockResult) throw RuntimeException("Simulated network error")
        val session = sessions.first { it.id == rangeSessionId }
        val key = unitIndex.toString()
        val results = if (result.isEmpty) session.blockResults - key else session.blockResults + (key to result)
        val updated = session.copy(blockResults = results)
        sessions.removeAll { it.id == rangeSessionId }
        sessions.add(updated)
        return updated
    }
    override suspend fun listObservations(rangeSessionId: String): List<Observation> = emptyList()
    override suspend fun upsertObservation(
        rangeSessionId: String,
        stepIndex: Int,
        values: Map<String, String>,
    ): Observation = error("unused")
    override suspend fun deleteObservations(rangeSessionId: String, stepIndices: List<Int>) = Unit
    override suspend fun recordTimeEntry(rangeSessionId: String, enteredAt: Instant) = Unit
    override suspend fun closeTimeEntry(rangeSessionId: String, enteredAt: Instant, exitedAt: Instant) = Unit
    override suspend fun getElapsedSeconds(rangeSessionId: String): Long = elapsed
    override suspend fun hasActiveSessionsForTemplate(sessionId: String): Boolean = false
}

private fun completedV3Session(
    id: String = "completed-v3",
    blockResults: Map<String, BlockResult> = emptyMap(),
): RangeSession = RangeSession(
    id = id,
    sourceSessionId = "session-1",
    sessionName = "Finished block",
    snapshot = RangeSessionSnapshot(
        units = listOf(
            SnapshotUnit(
                unitTitle = "Wedge work",
                repeatCount = 1,
                instructions = listOf(SnapshotInstruction(text = "Hit", ballCount = 2)),
                successCriterion = "Within 10 feet",
            ),
        ),
        steps = listOf(
            SnapshotStep(
                unitIndex = 0,
                instructionIndex = 0,
                repNumber = 1,
                totalReps = 1,
                instructionText = "Hit",
                ballCount = 1,
                unitTitle = "Wedge work",
            ),
            SnapshotStep(
                unitIndex = 0,
                instructionIndex = 0,
                repNumber = 1,
                totalReps = 1,
                instructionText = "Hit",
                ballCount = 1,
                unitTitle = "Wedge work",
            ),
        ),
    ),
    snapshotVersion = 3,
    completedSteps = emptyList(),
    clubOverrides = emptyMap(),
    blockResults = blockResults,
    startedAt = Instant.parse("2026-06-18T08:00:00Z"),
    completedAt = Instant.parse("2026-06-18T09:00:00Z"),
)
