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
)
