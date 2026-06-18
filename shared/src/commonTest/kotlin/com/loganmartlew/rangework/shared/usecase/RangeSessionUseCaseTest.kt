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

private val BASE_INSTANT = Instant.parse("2026-06-18T10:00:00Z")
private val LATER_INSTANT = Instant.parse("2026-06-18T10:30:00Z")

class RangeSessionUseCaseTest {

    // ── StartRangeSessionUseCase ─────────────────────────────────────────────

    @Test
    fun startSessionCreatesNewRangeSession() = runTest {
        val repo = FakeRangeSessionRepository()
        val session = StartRangeSessionUseCase(repo).invoke("practice-session-1")

        assertNotNull(session)
        assertEquals("practice-session-1", repo.lastStartedSessionId)
        assertNotNull(repo.lastStartedRangeSessionId)
    }

    @Test
    fun startSessionGeneratesUniqueIds() = runTest {
        val repo = FakeRangeSessionRepository()
        val s1 = StartRangeSessionUseCase(repo).invoke("session-1")
        val s2 = StartRangeSessionUseCase(repo).invoke("session-1")

        assertFalse(s1.id == s2.id, "Each start should generate a distinct range session id")
    }

    // ── GetRangeSessionUseCase ───────────────────────────────────────────────

    @Test
    fun getRangeSessionReturnsExistingSession() = runTest {
        val repo = FakeRangeSessionRepository()
        val created = StartRangeSessionUseCase(repo).invoke("session-1")

        val fetched = GetRangeSessionUseCase(repo).invoke(created.id)

        assertEquals(created.id, fetched?.id)
    }

    @Test
    fun getRangeSessionReturnsNullForUnknownId() = runTest {
        val repo = FakeRangeSessionRepository()
        val result = GetRangeSessionUseCase(repo).invoke("does-not-exist")
        assertNull(result)
    }

    // ── ListActiveRangeSessionsUseCase ───────────────────────────────────────

    @Test
    fun listActiveSessionsReturnsOnlyActiveSessions() = runTest {
        val repo = FakeRangeSessionRepository()
        val s1 = StartRangeSessionUseCase(repo).invoke("session-1")
        val s2 = StartRangeSessionUseCase(repo).invoke("session-2")
        FinishRangeSessionUseCase(repo).invoke(s1.id)

        val active = ListActiveRangeSessionsUseCase(repo).invoke()

        assertEquals(1, active.size)
        assertEquals(s2.id, active.first().id)
    }

    @Test
    fun listActiveSessionsExcludesAbandonedSessions() = runTest {
        val repo = FakeRangeSessionRepository()
        val s1 = StartRangeSessionUseCase(repo).invoke("session-1")
        AbandonRangeSessionUseCase(repo).invoke(s1.id)

        val active = ListActiveRangeSessionsUseCase(repo).invoke()
        assertTrue(active.isEmpty())
    }

    // ── ToggleStepCompleteUseCase ────────────────────────────────────────────

    @Test
    fun toggleStepCompleteMarksStepAsCompleted() = runTest {
        val repo = FakeRangeSessionRepository()
        val session = StartRangeSessionUseCase(repo).invoke("session-1")

        val updated = ToggleStepCompleteUseCase(repo).invoke(session.id, 0, true)

        assertTrue(updated.completedSteps.any { it.stepIndex == 0 })
    }

    @Test
    fun toggleStepCompleteUncompleteRemovesStep() = runTest {
        val repo = FakeRangeSessionRepository()
        val session = StartRangeSessionUseCase(repo).invoke("session-1")
        ToggleStepCompleteUseCase(repo).invoke(session.id, 0, true)

        val uncompleted = ToggleStepCompleteUseCase(repo).invoke(session.id, 0, false)

        assertFalse(uncompleted.completedSteps.any { it.stepIndex == 0 })
    }

    @Test
    fun toggleStepCompleteIsIdempotentForDuplicateCompletion() = runTest {
        val repo = FakeRangeSessionRepository()
        val session = StartRangeSessionUseCase(repo).invoke("session-1")
        ToggleStepCompleteUseCase(repo).invoke(session.id, 0, true)

        val updated = ToggleStepCompleteUseCase(repo).invoke(session.id, 0, true)

        assertEquals(1, updated.completedSteps.count { it.stepIndex == 0 })
    }

    // ── OverrideStepClubUseCase ──────────────────────────────────────────────

    @Test
    fun overrideStepClubSetsClubForStep() = runTest {
        val repo = FakeRangeSessionRepository()
        val session = StartRangeSessionUseCase(repo).invoke("session-1")

        val updated = OverrideStepClubUseCase(repo).invoke(session.id, 2, "seven_iron")

        assertEquals("seven_iron", updated.clubOverrides["2"])
    }

    @Test
    fun overrideStepClubPreservesExistingOverrides() = runTest {
        val repo = FakeRangeSessionRepository()
        val session = StartRangeSessionUseCase(repo).invoke("session-1")
        OverrideStepClubUseCase(repo).invoke(session.id, 0, "driver")

        val updated = OverrideStepClubUseCase(repo).invoke(session.id, 1, "putter")

        assertEquals("driver", updated.clubOverrides["0"])
        assertEquals("putter", updated.clubOverrides["1"])
    }

    // ── UpdateLastViewedStepUseCase ──────────────────────────────────────────

    @Test
    fun updateLastViewedStepPersistsIndex() = runTest {
        val repo = FakeRangeSessionRepository()
        val session = StartRangeSessionUseCase(repo).invoke("session-1")

        UpdateLastViewedStepUseCase(repo).invoke(session.id, 3)

        val fetched = GetRangeSessionUseCase(repo).invoke(session.id)
        assertEquals(3, fetched?.lastViewedStepIndex)
    }

    // ── FinishRangeSessionUseCase ────────────────────────────────────────────

    @Test
    fun finishSessionSetsCompletedAt() = runTest {
        val repo = FakeRangeSessionRepository()
        val session = StartRangeSessionUseCase(repo).invoke("session-1")

        val finished = FinishRangeSessionUseCase(repo).invoke(session.id)

        assertNotNull(finished.completedAt)
        assertNull(finished.abandonedAt)
    }

    // ── AbandonRangeSessionUseCase ───────────────────────────────────────────

    @Test
    fun abandonSessionSetsAbandonedAt() = runTest {
        val repo = FakeRangeSessionRepository()
        val session = StartRangeSessionUseCase(repo).invoke("session-1")

        AbandonRangeSessionUseCase(repo).invoke(session.id)

        val fetched = GetRangeSessionUseCase(repo).invoke(session.id)
        assertNotNull(fetched?.abandonedAt)
        assertNull(fetched?.completedAt)
    }

    // ── RecordTimeEntryUseCase / CloseTimeEntryUseCase / GetElapsedSecondsUseCase ──

    @Test
    fun recordAndCloseTimeEntryContributesToElapsed() = runTest {
        val repo = FakeRangeSessionRepository()
        val session = StartRangeSessionUseCase(repo).invoke("session-1")

        RecordTimeEntryUseCase(repo).invoke(session.id, BASE_INSTANT)
        CloseTimeEntryUseCase(repo).invoke(session.id, BASE_INSTANT, LATER_INSTANT)

        val elapsed = GetElapsedSecondsUseCase(repo).invoke(session.id)
        assertEquals(30 * 60L, elapsed)
    }

    @Test
    fun openTimeEntryIsNotCountedInElapsed() = runTest {
        val repo = FakeRangeSessionRepository()
        val session = StartRangeSessionUseCase(repo).invoke("session-1")

        RecordTimeEntryUseCase(repo).invoke(session.id, BASE_INSTANT)
        // Not closed

        val elapsed = GetElapsedSecondsUseCase(repo).invoke(session.id)
        assertEquals(0L, elapsed)
    }

    @Test
    fun elapsedSecondsAccumulatesMultipleClosedEntries() = runTest {
        val repo = FakeRangeSessionRepository()
        val session = StartRangeSessionUseCase(repo).invoke("session-1")

        val t0 = Instant.parse("2026-06-18T10:00:00Z")
        val t1 = Instant.parse("2026-06-18T10:10:00Z")
        val t2 = Instant.parse("2026-06-18T10:20:00Z")
        val t3 = Instant.parse("2026-06-18T10:35:00Z")

        RecordTimeEntryUseCase(repo).invoke(session.id, t0)
        CloseTimeEntryUseCase(repo).invoke(session.id, t0, t1)
        RecordTimeEntryUseCase(repo).invoke(session.id, t2)
        CloseTimeEntryUseCase(repo).invoke(session.id, t2, t3)

        val elapsed = GetElapsedSecondsUseCase(repo).invoke(session.id)
        assertEquals((10 + 15) * 60L, elapsed)
    }

    // ── HasActiveRangeSessionsUseCase ────────────────────────────────────────

    @Test
    fun hasActiveSessionsReturnsTrueWhenActiveSessionExists() = runTest {
        val repo = FakeRangeSessionRepository()
        StartRangeSessionUseCase(repo).invoke("session-1")

        assertTrue(HasActiveRangeSessionsUseCase(repo).invoke("session-1"))
    }

    @Test
    fun hasActiveSessionsReturnsFalseAfterAllSessionsFinished() = runTest {
        val repo = FakeRangeSessionRepository()
        val s = StartRangeSessionUseCase(repo).invoke("session-1")
        FinishRangeSessionUseCase(repo).invoke(s.id)

        assertFalse(HasActiveRangeSessionsUseCase(repo).invoke("session-1"))
    }

    @Test
    fun hasActiveSessionsReturnsFalseForUnrelatedTemplate() = runTest {
        val repo = FakeRangeSessionRepository()
        StartRangeSessionUseCase(repo).invoke("session-1")

        assertFalse(HasActiveRangeSessionsUseCase(repo).invoke("session-2"))
    }
}

// ── FakeRangeSessionRepository ───────────────────────────────────────────────

private class FakeRangeSessionRepository : RangeSessionRepository {

    private val sessions = mutableMapOf<String, RangeSession>()
    private val timeEntries = mutableListOf<FakeTimeEntry>()

    var lastStartedRangeSessionId: String? = null
    var lastStartedSessionId: String? = null

    override suspend fun startSession(rangeSessionId: String, sessionId: String): RangeSession {
        lastStartedRangeSessionId = rangeSessionId
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

    override suspend fun toggleStepComplete(
        rangeSessionId: String,
        stepIndex: Int,
        completed: Boolean,
    ): RangeSession {
        val session = requireNotNull(sessions[rangeSessionId])
        val updatedSteps = if (completed) {
            val already = session.completedSteps.any { it.stepIndex == stepIndex }
            if (already) session.completedSteps
            else session.completedSteps + CompletedStep(stepIndex, BASE_INSTANT)
        } else {
            session.completedSteps.filter { it.stepIndex != stepIndex }
        }
        val updated = session.copy(completedSteps = updatedSteps)
        sessions[rangeSessionId] = updated
        return updated
    }

    override suspend fun overrideStepClub(
        rangeSessionId: String,
        stepIndex: Int,
        clubCode: String,
    ): RangeSession {
        val session = requireNotNull(sessions[rangeSessionId])
        val updated = session.copy(
            clubOverrides = session.clubOverrides + (stepIndex.toString() to clubCode),
        )
        sessions[rangeSessionId] = updated
        return updated
    }

    override suspend fun updateLastViewedStep(rangeSessionId: String, stepIndex: Int) {
        val session = requireNotNull(sessions[rangeSessionId])
        sessions[rangeSessionId] = session.copy(lastViewedStepIndex = stepIndex)
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
