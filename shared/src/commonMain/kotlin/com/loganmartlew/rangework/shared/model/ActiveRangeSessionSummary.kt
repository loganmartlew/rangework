package com.loganmartlew.rangework.shared.model

import kotlinx.datetime.Instant

data class ActiveRangeSessionSummary(
    val id: String,
    val sessionName: String,
    val totalSteps: Int,
    val completedStepCount: Int,
    val startedAt: Instant,
)
