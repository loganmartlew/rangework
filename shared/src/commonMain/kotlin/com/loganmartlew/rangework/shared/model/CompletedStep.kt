package com.loganmartlew.rangework.shared.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class CompletedStep(
    val stepIndex: Int,
    val completedAt: Instant,
)
