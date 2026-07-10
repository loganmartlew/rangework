package com.loganmartlew.rangework.shared.model

import kotlinx.datetime.Instant

data class CompletedRangeSessionSummary(
    val id: String,
    val sessionName: String,
    val totalSteps: Int,
    val completedStepCount: Int,
    val totalBalls: Int,
    val completedBalls: Int,
    val startedAt: Instant,
    val completedAt: Instant,
    val elapsedSeconds: Long,
    /**
     * Whether this completed session's snapshot supports data capture (v3+).
     * History rows key their tappability off this: v1/v2 sessions have no notes,
     * results, or observations to open. Defaults false so fixtures/fakes that do
     * not set it stay non-tappable.
     */
    val supportsDataCapture: Boolean = false,
)
