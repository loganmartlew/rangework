package com.loganmartlew.rangework.shared.model

fun PracticeUnit.derivedBallCount(): Int = instructions.sumOf { instruction ->
    instruction.ballCount ?: 0
}

fun PracticeSessionItem.derivedBallCount(unit: PracticeUnit?): Int? = unit?.derivedBallCount()?.let { baseBallCount ->
    baseBallCount * repeatCount
}

fun PracticeSession.derivedBallCount(unitsById: Map<String, PracticeUnit>): Int = items.sumOf { item ->
    item.derivedBallCount(unitsById[item.practiceUnitId]) ?: 0
}

// Product assumption: average seconds per ball including setup, alignment, and
// reflection between shots. Tune SECONDS_PER_BALL if timing data suggests a
// different value.
const val SECONDS_PER_BALL = 40

/**
 * Single source of truth for turning a ball count into an estimated practice
 * duration in minutes. Returns 0 for an empty count; otherwise rounds to the
 * nearest minute. Callers with a ball total should use this rather than
 * re-deriving the arithmetic.
 */
fun estimateDurationMinutes(totalBalls: Int): Int {
    if (totalBalls <= 0) return 0
    return (totalBalls * SECONDS_PER_BALL + 30) / 60
}

fun sessionsUsingUnit(
    unitId: String,
    sessions: List<PracticeSession>,
): List<PracticeSession> = sessions.filter { session ->
    session.items.any { it.practiceUnitId == unitId }
}

fun estimateSessionDurationMinutes(session: PracticeSession, unitsById: Map<String, PracticeUnit>): Int =
    estimateDurationMinutes(session.derivedBallCount(unitsById))
