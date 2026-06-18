package com.loganmartlew.rangework.shared.model

import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

private val BASE_INSTANT = Instant.parse("2026-06-18T10:00:00Z")

class RangeSessionProgressTest {

    // ── completedStepCount ───────────────────────────────────────────────────

    @Test
    fun completedStepCountReturnsZeroWhenNoneCompleted() {
        val session = makeSession(steps = twoSteps(), completedSteps = emptyList())
        assertEquals(0, session.completedStepCount())
    }

    @Test
    fun completedStepCountReturnsCorrectCount() {
        val session = makeSession(
            steps = twoSteps(),
            completedSteps = listOf(CompletedStep(0, BASE_INSTANT)),
        )
        assertEquals(1, session.completedStepCount())
    }

    // ── totalStepCount ───────────────────────────────────────────────────────

    @Test
    fun totalStepCountMatchesSnapshotStepsSize() {
        val session = makeSession(steps = twoSteps())
        assertEquals(2, session.totalStepCount())
    }

    @Test
    fun totalStepCountIsZeroForEmptySnapshot() {
        val session = makeSession(steps = emptyList())
        assertEquals(0, session.totalStepCount())
    }

    // ── completionPercentage ─────────────────────────────────────────────────

    @Test
    fun completionPercentageReturnsZeroWhenNoSteps() {
        val session = makeSession(steps = emptyList(), completedSteps = emptyList())
        assertEquals(0.0, session.completionPercentage())
    }

    @Test
    fun completionPercentageReturnsZeroWhenNoneCompleted() {
        val session = makeSession(steps = twoSteps(), completedSteps = emptyList())
        assertEquals(0.0, session.completionPercentage())
    }

    @Test
    fun completionPercentageReturnsOneWhenAllCompleted() {
        val session = makeSession(
            steps = twoSteps(),
            completedSteps = listOf(
                CompletedStep(0, BASE_INSTANT),
                CompletedStep(1, BASE_INSTANT),
            ),
        )
        assertEquals(1.0, session.completionPercentage())
    }

    @Test
    fun completionPercentageReturnsHalfForOneOfTwo() {
        val session = makeSession(
            steps = twoSteps(),
            completedSteps = listOf(CompletedStep(0, BASE_INSTANT)),
        )
        assertEquals(0.5, session.completionPercentage())
    }

    // ── completedBalls ───────────────────────────────────────────────────────

    @Test
    fun completedBallsReturnsZeroWhenNoneCompleted() {
        val session = makeSession(
            steps = listOf(makeStep(ballCount = 10), makeStep(ballCount = 20)),
            completedSteps = emptyList(),
        )
        assertEquals(0, session.completedBalls())
    }

    @Test
    fun completedBallsSumsOnlyCompletedSteps() {
        val session = makeSession(
            steps = listOf(makeStep(ballCount = 10), makeStep(ballCount = 20)),
            completedSteps = listOf(CompletedStep(0, BASE_INSTANT)),
        )
        assertEquals(10, session.completedBalls())
    }

    @Test
    fun completedBallsSkipsNullBallCount() {
        val session = makeSession(
            steps = listOf(makeStep(ballCount = null), makeStep(ballCount = 15)),
            completedSteps = listOf(
                CompletedStep(0, BASE_INSTANT),
                CompletedStep(1, BASE_INSTANT),
            ),
        )
        assertEquals(15, session.completedBalls())
    }

    // ── totalBalls ───────────────────────────────────────────────────────────

    @Test
    fun totalBallsSumsAllStepBallCounts() {
        val session = makeSession(
            steps = listOf(makeStep(ballCount = 10), makeStep(ballCount = 20)),
        )
        assertEquals(30, session.totalBalls())
    }

    @Test
    fun totalBallsSkipsNullBallCounts() {
        val session = makeSession(
            steps = listOf(makeStep(ballCount = 10), makeStep(ballCount = null), makeStep(ballCount = 5)),
        )
        assertEquals(15, session.totalBalls())
    }

    @Test
    fun totalBallsReturnsZeroWhenAllBallCountsNull() {
        val session = makeSession(
            steps = listOf(makeStep(ballCount = null), makeStep(ballCount = null)),
        )
        assertEquals(0, session.totalBalls())
    }

    @Test
    fun totalBallsReturnsZeroForEmptySession() {
        val session = makeSession(steps = emptyList())
        assertEquals(0, session.totalBalls())
    }

    // ── completedUnits ───────────────────────────────────────────────────────

    @Test
    fun completedUnitsReturnsZeroWhenNothingCompleted() {
        val session = makeSession(
            steps = listOf(makeStep(unitIndex = 0), makeStep(unitIndex = 1)),
            completedSteps = emptyList(),
        )
        assertEquals(0, session.completedUnits())
    }

    @Test
    fun completedUnitsCountsOnlyFullyCompletedUnits() {
        // Unit 0 has steps 0 and 1; unit 1 has step 2
        val session = makeSession(
            steps = listOf(
                makeStep(unitIndex = 0),
                makeStep(unitIndex = 0),
                makeStep(unitIndex = 1),
            ),
            completedSteps = listOf(
                CompletedStep(0, BASE_INSTANT),
                // step 1 not completed — unit 0 is NOT fully done
                CompletedStep(2, BASE_INSTANT),
            ),
        )
        assertEquals(1, session.completedUnits())
    }

    @Test
    fun completedUnitsCountsAllUnitsWhenFullyComplete() {
        val session = makeSession(
            steps = listOf(makeStep(unitIndex = 0), makeStep(unitIndex = 1)),
            completedSteps = listOf(
                CompletedStep(0, BASE_INSTANT),
                CompletedStep(1, BASE_INSTANT),
            ),
        )
        assertEquals(2, session.completedUnits())
    }

    // ── isFullyComplete ──────────────────────────────────────────────────────

    @Test
    fun isFullyCompleteReturnsFalseWhenPartiallyDone() {
        val session = makeSession(
            steps = twoSteps(),
            completedSteps = listOf(CompletedStep(0, BASE_INSTANT)),
        )
        assertFalse(session.isFullyComplete())
    }

    @Test
    fun isFullyCompleteReturnsTrueWhenAllStepsDone() {
        val session = makeSession(
            steps = twoSteps(),
            completedSteps = listOf(
                CompletedStep(0, BASE_INSTANT),
                CompletedStep(1, BASE_INSTANT),
            ),
        )
        assertTrue(session.isFullyComplete())
    }

    @Test
    fun isFullyCompleteReturnsTrueForEmptySession() {
        val session = makeSession(steps = emptyList(), completedSteps = emptyList())
        assertTrue(session.isFullyComplete())
    }

    // ── isActive ─────────────────────────────────────────────────────────────

    @Test
    fun isActiveReturnsTrueWhenBothTimestampsNull() {
        val session = makeSession(completedAt = null, abandonedAt = null)
        assertTrue(session.isActive())
    }

    @Test
    fun isActiveReturnsFalseWhenCompletedAtSet() {
        val session = makeSession(completedAt = BASE_INSTANT, abandonedAt = null)
        assertFalse(session.isActive())
    }

    @Test
    fun isActiveReturnsFalseWhenAbandonedAtSet() {
        val session = makeSession(completedAt = null, abandonedAt = BASE_INSTANT)
        assertFalse(session.isActive())
    }

    @Test
    fun isActiveReturnsFalseWhenBothTimestampsSet() {
        val session = makeSession(completedAt = BASE_INSTANT, abandonedAt = BASE_INSTANT)
        assertFalse(session.isActive())
    }
}

// ── Helpers ──────────────────────────────────────────────────────────────────

private fun makeSession(
    id: String = "range-session-1",
    steps: List<SnapshotStep> = emptyList(),
    completedSteps: List<CompletedStep> = emptyList(),
    completedAt: Instant? = null,
    abandonedAt: Instant? = null,
) = RangeSession(
    id = id,
    sourceSessionId = "session-1",
    sessionName = "Test session",
    snapshot = RangeSessionSnapshot(
        sessionNotes = null,
        units = emptyList(),
        steps = steps,
    ),
    snapshotVersion = 1,
    completedSteps = completedSteps,
    clubOverrides = emptyMap(),
    startedAt = BASE_INSTANT,
    completedAt = completedAt,
    abandonedAt = abandonedAt,
)

private fun makeStep(
    unitIndex: Int = 0,
    instructionIndex: Int = 0,
    ballCount: Int? = 10,
) = SnapshotStep(
    unitIndex = unitIndex,
    instructionIndex = instructionIndex,
    repNumber = 1,
    totalReps = 1,
    instructionText = "Hit balls",
    ballCount = ballCount,
    unitTitle = "Unit $unitIndex",
)

private fun twoSteps() = listOf(makeStep(unitIndex = 0), makeStep(unitIndex = 1))
