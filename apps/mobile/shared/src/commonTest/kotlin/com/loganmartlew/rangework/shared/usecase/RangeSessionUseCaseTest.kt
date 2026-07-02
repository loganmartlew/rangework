package com.loganmartlew.rangework.shared.usecase

import com.loganmartlew.rangework.shared.model.ActiveRangeSessionSummary
import com.loganmartlew.rangework.shared.model.CompletedRangeSessionSummary
import com.loganmartlew.rangework.shared.model.CompletedStep
import com.loganmartlew.rangework.shared.model.RangeSession
import com.loganmartlew.rangework.shared.model.RangeSessionSnapshot
import com.loganmartlew.rangework.shared.model.SnapshotStep
import com.loganmartlew.rangework.shared.model.SnapshotUnit
import com.loganmartlew.rangework.shared.repository.RangeSessionRepository
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private val BASE_INSTANT = Instant.parse("2026-06-18T10:00:00Z")
private val LATER_INSTANT = Instant.parse("2026-06-18T10:30:00Z")

class RangeSessionRepositoryTest {

    // ── start ────────────────────────────────────────────────────────────────

    @Test
    fun startCreatesNewRangeSession() = runTest {
        val repo = FakeRangeSessionRepository()
        val session = repo.start("practice-session-1")

        assertNotNull(session)
        assertEquals("practice-session-1", repo.lastStartedSessionId)
        assertNotNull(session.id)
    }

    @Test
    fun startGeneratesUniqueIds() = runTest {
        val repo = FakeRangeSessionRepository()
        val s1 = repo.start("session-1")
        val s2 = repo.start("session-1")

        assertFalse(s1.id == s2.id, "Each start should generate a distinct range session id")
    }

    // ── getSession ───────────────────────────────────────────────────────────

    @Test
    fun getSessionReturnsExistingSession() = runTest {
        val repo = FakeRangeSessionRepository()
        val created = repo.start("session-1")

        val fetched = repo.getSession(created.id)

        assertEquals(created.id, fetched?.id)
    }

    @Test
    fun getSessionReturnsNullForUnknownId() = runTest {
        val repo = FakeRangeSessionRepository()
        val result = repo.getSession("does-not-exist")
        assertNull(result)
    }

    // ── listActiveSessions ───────────────────────────────────────────────────

    @Test
    fun listActiveSessionsReturnsOnlyActiveSessions() = runTest {
        val repo = FakeRangeSessionRepository()
        val s1 = repo.start("session-1")
        val s2 = repo.start("session-2")
        repo.finishSession(s1.id)

        val active = repo.listActiveSessions()

        assertEquals(1, active.size)
        assertEquals(s2.id, active.first().id)
    }

    @Test
    fun listActiveSessionsExcludesAbandonedSessions() = runTest {
        val repo = FakeRangeSessionRepository()
        val s1 = repo.start("session-1")
        repo.abandonSession(s1.id)

        val active = repo.listActiveSessions()
        assertTrue(active.isEmpty())
    }

    // ── setStepsCompletion ───────────────────────────────────────────────────

    @Test
    fun setStepsCompletionMarksStepsAsCompleted() = runTest {
        val repo = FakeRangeSessionRepository()
        val session = repo.start("session-1")

        val updated = repo.setStepsCompletion(session.id, listOf(0, 1), true)

        assertTrue(updated.completedSteps.any { it.stepIndex == 0 })
        assertTrue(updated.completedSteps.any { it.stepIndex == 1 })
    }

    @Test
    fun setStepsCompletionUncompleteRemovesSteps() = runTest {
        val repo = FakeRangeSessionRepository()
        val session = repo.start("session-1")
        repo.setStepsCompletion(session.id, listOf(0, 1), true)

        val uncompleted = repo.setStepsCompletion(session.id, listOf(0), false)

        assertFalse(uncompleted.completedSteps.any { it.stepIndex == 0 })
        assertTrue(uncompleted.completedSteps.any { it.stepIndex == 1 })
    }

    @Test
    fun setStepsCompletionIsIdempotentForDuplicateCompletion() = runTest {
        val repo = FakeRangeSessionRepository()
        val session = repo.start("session-1")
        repo.setStepsCompletion(session.id, listOf(0), true)

        val updated = repo.setStepsCompletion(session.id, listOf(0), true)

        assertEquals(1, updated.completedSteps.count { it.stepIndex == 0 })
    }

    // ── overrideStepClubs ────────────────────────────────────────────────────

    @Test
    fun overrideStepClubsSetsClubForEachStep() = runTest {
        val repo = FakeRangeSessionRepository()
        val session = repo.start("session-1")

        val updated = repo.overrideStepClubs(session.id, listOf(1, 2), "seven_iron")

        assertEquals("seven_iron", updated.clubOverrides["1"])
        assertEquals("seven_iron", updated.clubOverrides["2"])
    }

    @Test
    fun overrideStepClubsPreservesExistingOverrides() = runTest {
        val repo = FakeRangeSessionRepository()
        val session = repo.start("session-1")
        repo.overrideStepClubs(session.id, listOf(0), "driver")

        val updated = repo.overrideStepClubs(session.id, listOf(1), "putter")

        assertEquals("driver", updated.clubOverrides["0"])
        assertEquals("putter", updated.clubOverrides["1"])
    }

    // ── finishSession ────────────────────────────────────────────────────────

    @Test
    fun finishSessionSetsCompletedAt() = runTest {
        val repo = FakeRangeSessionRepository()
        val session = repo.start("session-1")

        val finished = repo.finishSession(session.id)

        assertNotNull(finished.completedAt)
        assertNull(finished.abandonedAt)
    }

    // ── abandonSession ───────────────────────────────────────────────────────

    @Test
    fun abandonSessionSetsAbandonedAt() = runTest {
        val repo = FakeRangeSessionRepository()
        val session = repo.start("session-1")

        repo.abandonSession(session.id)

        val fetched = repo.getSession(session.id)
        assertNotNull(fetched?.abandonedAt)
        assertNull(fetched?.completedAt)
    }

    // ── recordTimeEntry / closeTimeEntry / getElapsedSeconds ─────────────────

    @Test
    fun recordAndCloseTimeEntryContributesToElapsed() = runTest {
        val repo = FakeRangeSessionRepository()
        val session = repo.start("session-1")

        repo.recordTimeEntry(session.id, BASE_INSTANT)
        repo.closeTimeEntry(session.id, BASE_INSTANT, LATER_INSTANT)

        val elapsed = repo.getElapsedSeconds(session.id)
        assertEquals(30 * 60L, elapsed)
    }

    @Test
    fun openTimeEntryIsNotCountedInElapsed() = runTest {
        val repo = FakeRangeSessionRepository()
        val session = repo.start("session-1")

        repo.recordTimeEntry(session.id, BASE_INSTANT)
        // Not closed

        val elapsed = repo.getElapsedSeconds(session.id)
        assertEquals(0L, elapsed)
    }

    @Test
    fun elapsedSecondsAccumulatesMultipleClosedEntries() = runTest {
        val repo = FakeRangeSessionRepository()
        val session = repo.start("session-1")

        val t0 = Instant.parse("2026-06-18T10:00:00Z")
        val t1 = Instant.parse("2026-06-18T10:10:00Z")
        val t2 = Instant.parse("2026-06-18T10:20:00Z")
        val t3 = Instant.parse("2026-06-18T10:35:00Z")

        repo.recordTimeEntry(session.id, t0)
        repo.closeTimeEntry(session.id, t0, t1)
        repo.recordTimeEntry(session.id, t2)
        repo.closeTimeEntry(session.id, t2, t3)

        val elapsed = repo.getElapsedSeconds(session.id)
        assertEquals((10 + 15) * 60L, elapsed)
    }

    // ── hasActiveSessionsForTemplate ─────────────────────────────────────────

    @Test
    fun hasActiveSessionsReturnsTrueWhenActiveSessionExists() = runTest {
        val repo = FakeRangeSessionRepository()
        repo.start("session-1")

        assertTrue(repo.hasActiveSessionsForTemplate("session-1"))
    }

    @Test
    fun hasActiveSessionsReturnsFalseAfterAllSessionsFinished() = runTest {
        val repo = FakeRangeSessionRepository()
        val s = repo.start("session-1")
        repo.finishSession(s.id)

        assertFalse(repo.hasActiveSessionsForTemplate("session-1"))
    }

    @Test
    fun hasActiveSessionsReturnsFalseForUnrelatedTemplate() = runTest {
        val repo = FakeRangeSessionRepository()
        repo.start("session-1")

        assertFalse(repo.hasActiveSessionsForTemplate("session-2"))
    }
}

// ── FakeRangeSessionRepository ───────────────────────────────────────────────

private class FakeRangeSessionRepository : RangeSessionRepository {

    private val sessions = mutableMapOf<String, RangeSession>()
    private val timeEntries = mutableListOf<FakeTimeEntry>()

    var lastStartedSessionId: String? = null

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun start(sessionId: String): RangeSession {
        val rangeSessionId = Uuid.random().toString()
        lastStartedSessionId = sessionId
        val session = RangeSession(
            id = rangeSessionId,
            sourceSessionId = sessionId,
            sessionName = "Fake session",
            snapshot = RangeSessionSnapshot(
                sessionNotes = null,
                units = listOf(
                    SnapshotUnit(
                        unitTitle = "Unit 1",
                        repeatCount = 1,
                        instructions = emptyList(),
                    ),
                ),
                steps = listOf(
                    makeStep(unitIndex = 0, instructionIndex = 0, ballCount = 10),
                    makeStep(unitIndex = 0, instructionIndex = 1, ballCount = 20),
                    makeStep(unitIndex = 0, instructionIndex = 2, ballCount = null),
                ),
            ),
            snapshotVersion = 1,
            completedSteps = emptyList(),
            clubOverrides = emptyMap(),
            startedAt = BASE_INSTANT,
        )
        sessions[rangeSessionId] = session
        return session
    }

    override suspend fun getSession(rangeSessionId: String): RangeSession? =
        sessions[rangeSessionId]

    override suspend fun listActiveSessions(): List<ActiveRangeSessionSummary> =
        sessions.values
            .filter { it.completedAt == null && it.abandonedAt == null }
            .map { session ->
                ActiveRangeSessionSummary(
                    id = session.id,
                    sessionName = session.sessionName,
                    totalSteps = session.snapshot.steps.size,
                    completedStepCount = session.completedSteps.size,
                    startedAt = session.startedAt,
                )
            }

    override suspend fun listCompletedSessions(sessionId: String): List<CompletedRangeSessionSummary> =
        sessions.values
            .filter { it.sourceSessionId == sessionId && it.completedAt != null && it.abandonedAt == null }
            .map { session ->
                CompletedRangeSessionSummary(
                    id = session.id,
                    sessionName = session.sessionName,
                    totalSteps = session.snapshot.steps.size,
                    completedStepCount = session.completedSteps.size,
                    totalBalls = session.snapshot.steps.sumOf { it.ballCount ?: 0 },
                    completedBalls = session.completedSteps.sumOf { cs ->
                        session.snapshot.steps.getOrNull(cs.stepIndex)?.ballCount ?: 0
                    },
                    startedAt = session.startedAt,
                    completedAt = session.completedAt!!,
                    elapsedSeconds = 0L,
                )
            }

    override suspend fun setStepsCompletion(
        rangeSessionId: String,
        stepIndices: List<Int>,
        completed: Boolean,
    ): RangeSession {
        val session = requireNotNull(sessions[rangeSessionId])
        val updatedSteps = if (completed) {
            val already = session.completedSteps.map { it.stepIndex }.toSet()
            session.completedSteps + stepIndices
                .filter { it !in already }
                .map { CompletedStep(it, BASE_INSTANT) }
        } else {
            val toRemove = stepIndices.toSet()
            session.completedSteps.filter { it.stepIndex !in toRemove }
        }
        val updated = session.copy(completedSteps = updatedSteps)
        sessions[rangeSessionId] = updated
        return updated
    }

    override suspend fun overrideStepClubs(
        rangeSessionId: String,
        stepIndices: List<Int>,
        clubCode: String,
    ): RangeSession {
        val session = requireNotNull(sessions[rangeSessionId])
        val updated = session.copy(
            clubOverrides = session.clubOverrides + stepIndices.map { it.toString() to clubCode },
        )
        sessions[rangeSessionId] = updated
        return updated
    }

    override suspend fun finishSession(rangeSessionId: String): RangeSession {
        val session = requireNotNull(sessions[rangeSessionId])
        val updated = session.copy(completedAt = BASE_INSTANT)
        sessions[rangeSessionId] = updated
        return updated
    }

    override suspend fun abandonSession(rangeSessionId: String) {
        val session = requireNotNull(sessions[rangeSessionId])
        sessions[rangeSessionId] = session.copy(abandonedAt = BASE_INSTANT)
    }

    override suspend fun recordTimeEntry(rangeSessionId: String, enteredAt: Instant) {
        timeEntries.add(FakeTimeEntry(rangeSessionId = rangeSessionId, enteredAt = enteredAt))
    }

    override suspend fun closeTimeEntry(
        rangeSessionId: String,
        enteredAt: Instant,
        exitedAt: Instant,
    ) {
        val index = timeEntries.indexOfFirst { it.rangeSessionId == rangeSessionId && it.enteredAt == enteredAt }
        if (index >= 0) {
            timeEntries[index] = timeEntries[index].copy(exitedAt = exitedAt)
        }
    }

    override suspend fun getElapsedSeconds(rangeSessionId: String): Long =
        timeEntries
            .filter { it.rangeSessionId == rangeSessionId && it.exitedAt != null }
            .sumOf { it.exitedAt!!.epochSeconds - it.enteredAt.epochSeconds }

    override suspend fun hasActiveSessionsForTemplate(sessionId: String): Boolean =
        sessions.values.any {
            it.sourceSessionId == sessionId && it.completedAt == null && it.abandonedAt == null
        }
}

private data class FakeTimeEntry(
    val rangeSessionId: String,
    val enteredAt: Instant,
    val exitedAt: Instant? = null,
)

private fun makeStep(
    unitIndex: Int = 0,
    instructionIndex: Int = 0,
    repNumber: Int = 1,
    totalReps: Int = 1,
    instructionText: String = "Hit balls",
    ballCount: Int? = 10,
    club: String? = null,
) = SnapshotStep(
    unitIndex = unitIndex,
    instructionIndex = instructionIndex,
    repNumber = repNumber,
    totalReps = totalReps,
    instructionText = instructionText,
    ballCount = ballCount,
    club = club,
    unitTitle = "Unit $unitIndex",
)
