package com.loganmartlew.rangework.shared.recording

import com.loganmartlew.rangework.shared.model.ActiveRangeSessionSummary
import com.loganmartlew.rangework.shared.model.BlockResult
import com.loganmartlew.rangework.shared.model.CompletedRangeSessionSummary
import com.loganmartlew.rangework.shared.model.CompletedStep
import com.loganmartlew.rangework.shared.model.Observation
import com.loganmartlew.rangework.shared.model.RangeSession
import com.loganmartlew.rangework.shared.model.RangeSessionSnapshot
import com.loganmartlew.rangework.shared.model.RecordingRejection
import com.loganmartlew.rangework.shared.model.SnapshotStep
import com.loganmartlew.rangework.shared.model.SnapshotUnit
import com.loganmartlew.rangework.shared.repository.RangeSessionRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RangeSessionRecorderTest {

    private val started = Instant.parse("2026-07-09T10:00:00Z")

    // ── Session Note ────────────────────────────────────────────────────────────

    @Test
    fun saveSessionNoteNormalizesAndPersists() = runTest {
        val repo = FakeRepo(session())
        val recorder = DefaultRangeSessionRecorder(repo)

        val result = recorder.saveSessionNote("rs-1", "  went well  ")

        assertTrue(result is RecordingResult.Success)
        assertEquals("went well", repo.session.sessionNote)
    }

    @Test
    fun saveSessionNoteRejectedOnUnsupportedSnapshot() = runTest {
        val repo = FakeRepo(session(version = 2))
        val recorder = DefaultRangeSessionRecorder(repo)

        val result = recorder.saveSessionNote("rs-1", "note")

        assertEquals(RecordingRejection.UnsupportedSnapshot, (result as RecordingResult.Rejected).reason)
        assertNull(repo.session.sessionNote)
    }

    // ── Block Result merge ──────────────────────────────────────────────────────

    @Test
    fun blockNoteAndManualCountMergeIntoOneResult() = runTest {
        val repo = FakeRepo(session(observationTypes = emptyList()))
        val recorder = DefaultRangeSessionRecorder(repo)

        recorder.saveBlockNote("rs-1", 0, "left misses")
        recorder.saveManualCount("rs-1", 0, 1)

        assertEquals(BlockResult(note = "left misses", manualCount = 1), repo.session.blockResults["0"])
    }

    @Test
    fun clearingBothFieldsRemovesTheKey() = runTest {
        val repo = FakeRepo(session(observationTypes = emptyList()))
        val recorder = DefaultRangeSessionRecorder(repo)

        recorder.saveBlockNote("rs-1", 0, "note")
        recorder.saveBlockNote("rs-1", 0, "   ") // whitespace clears the note
        recorder.saveManualCount("rs-1", 0, null)

        assertTrue(repo.session.blockResults.isEmpty())
    }

    @Test
    fun manualCountRejectionSurfacesReason() = runTest {
        val repo = FakeRepo(session(observationTypes = listOf("success")))
        val recorder = DefaultRangeSessionRecorder(repo)

        val result = recorder.saveManualCount("rs-1", 0, 1)

        assertEquals(RecordingRejection.SuccessTypeEnabled, (result as RecordingResult.Rejected).reason)
    }

    // ── Observations ────────────────────────────────────────────────────────────

    @Test
    fun recordObservationUpsertsGuarded() = runTest {
        val repo = FakeRepo(session(observationTypes = listOf("shape")))
        val recorder = DefaultRangeSessionRecorder(repo)

        val ok = recorder.recordObservation("rs-1", 1, mapOf("shape" to "straight_left"))
        assertTrue(ok is RecordingResult.Success)
        assertEquals(mapOf("shape" to "straight_left"), repo.observations[1]?.values)

        val bad = recorder.recordObservation("rs-1", 1, mapOf("shape" to "banana"))
        assertEquals(RecordingRejection.ValueOutOfVocabulary, (bad as RecordingResult.Rejected).reason)
    }

    @Test
    fun minusOneSweepVoidsObservationsThenUncompletes() = runTest {
        val repo = FakeRepo(
            session(observationTypes = listOf("shape")).copy(
                completedSteps = listOf(
                    CompletedStep(1, started),
                    CompletedStep(2, started),
                ),
            ),
        )
        repo.observations[1] = Observation(1, mapOf("shape" to "straight_left"))
        repo.observations[2] = Observation(2, mapOf("shape" to "straight_right"))
        val recorder = DefaultRangeSessionRecorder(repo)

        val result = recorder.uncompleteStepsVoidingObservations("rs-1", listOf(2))

        assertTrue(result is RecordingResult.Success)
        // Swept ball's observation deleted; untouched ball survives.
        assertNull(repo.observations[2])
        assertEquals(mapOf("shape" to "straight_left"), repo.observations[1]?.values)
        // Swept ball un-completed; untouched ball still complete.
        assertTrue(repo.session.completedSteps.none { it.stepIndex == 2 })
        assertTrue(repo.session.completedSteps.any { it.stepIndex == 1 })
    }

    @Test
    fun correctingObservationNeverTouchesCompletion() = runTest {
        val repo = FakeRepo(
            session(observationTypes = listOf("shape")).copy(
                completedSteps = listOf(CompletedStep(1, started)),
            ),
        )
        repo.observations[1] = Observation(1, mapOf("shape" to "straight_left"))
        val recorder = DefaultRangeSessionRecorder(repo)

        recorder.recordObservation("rs-1", 1, mapOf("shape" to "straight_right"))

        assertEquals(mapOf("shape" to "straight_right"), repo.observations[1]?.values)
        assertTrue(repo.session.completedSteps.any { it.stepIndex == 1 })
    }

    @Test
    fun voidRejectedWhenFrozen() = runTest {
        val repo = FakeRepo(session(observationTypes = listOf("shape")).copy(completedAt = started))
        val recorder = DefaultRangeSessionRecorder(repo)

        val result = recorder.voidObservations("rs-1", listOf(1))

        assertEquals(RecordingRejection.SessionFrozen, (result as RecordingResult.Rejected).reason)
    }

    // ── Fixture ─────────────────────────────────────────────────────────────────

    private fun ballStep() = SnapshotStep(
        unitIndex = 0,
        instructionIndex = 0,
        repNumber = 1,
        totalReps = 1,
        instructionText = "Hit",
        ballCount = 1,
        unitTitle = "Unit",
    )

    private fun actionStep() = SnapshotStep(
        unitIndex = 0,
        instructionIndex = 0,
        repNumber = 1,
        totalReps = 1,
        instructionText = "Rehearse",
        ballCount = null,
        unitTitle = "Unit",
    )

    private fun session(
        version: Int = 3,
        successCriterion: String? = "inside 5m",
        observationTypes: List<String> = listOf("shape"),
    ) = RangeSession(
        id = "rs-1",
        sessionName = "Session",
        snapshot = RangeSessionSnapshot(
            units = listOf(
                SnapshotUnit(
                    unitTitle = "Unit",
                    repeatCount = 1,
                    instructions = emptyList(),
                    successCriterion = successCriterion,
                    observationTypes = observationTypes,
                ),
            ),
            steps = listOf(actionStep(), ballStep(), ballStep()),
        ),
        snapshotVersion = version,
        completedSteps = emptyList(),
        clubOverrides = emptyMap(),
        startedAt = started,
    )
}

/** In-memory repository exercising the recorder's guarded writes. */
private class FakeRepo(initial: RangeSession) : RangeSessionRepository {
    var session: RangeSession = initial
    val observations = mutableMapOf<Int, Observation>()

    override suspend fun getSession(rangeSessionId: String): RangeSession = session

    override suspend fun saveSessionNote(rangeSessionId: String, note: String?): RangeSession {
        session = session.copy(sessionNote = note)
        return session
    }

    override suspend fun saveBlockResult(rangeSessionId: String, unitIndex: Int, result: BlockResult): RangeSession {
        val key = unitIndex.toString()
        val updated = if (result.isEmpty) session.blockResults - key else session.blockResults + (key to result)
        session = session.copy(blockResults = updated)
        return session
    }

    override suspend fun listObservations(rangeSessionId: String): List<Observation> =
        observations.values.sortedBy(Observation::stepIndex)

    override suspend fun upsertObservation(rangeSessionId: String, stepIndex: Int, values: Map<String, String>): Observation {
        val observation = Observation(stepIndex, values)
        observations[stepIndex] = observation
        return observation
    }

    override suspend fun deleteObservations(rangeSessionId: String, stepIndices: List<Int>) {
        stepIndices.forEach(observations::remove)
    }

    override suspend fun setStepsCompletion(rangeSessionId: String, stepIndices: List<Int>, completed: Boolean): RangeSession {
        session = if (completed) {
            val already = session.completedSteps.map { it.stepIndex }.toSet()
            session.copy(completedSteps = session.completedSteps + stepIndices.filter { it !in already }.map { CompletedStep(it, Instant.parse("2026-07-09T10:00:00Z")) })
        } else {
            val remove = stepIndices.toSet()
            session.copy(completedSteps = session.completedSteps.filter { it.stepIndex !in remove })
        }
        return session
    }

    // ── Unused by the recorder ──────────────────────────────────────────────────
    override suspend fun start(sessionId: String): RangeSession = error("unused")
    override suspend fun listActiveSessions(): List<ActiveRangeSessionSummary> = emptyList()
    override suspend fun listCompletedSessions(sessionId: String): List<CompletedRangeSessionSummary> = emptyList()
    override suspend fun overrideStepClubs(rangeSessionId: String, stepIndices: List<Int>, clubCode: String): RangeSession = error("unused")
    override suspend fun finishSession(rangeSessionId: String): RangeSession = error("unused")
    override suspend fun abandonSession(rangeSessionId: String) = Unit
    override suspend fun recordTimeEntry(rangeSessionId: String, enteredAt: Instant) = Unit
    override suspend fun closeTimeEntry(rangeSessionId: String, enteredAt: Instant, exitedAt: Instant) = Unit
    override suspend fun getElapsedSeconds(rangeSessionId: String): Long = 0L
    override suspend fun hasActiveSessionsForTemplate(sessionId: String): Boolean = false
}
