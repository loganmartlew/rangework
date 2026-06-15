package com.loganmartlew.rangework.shared.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class PracticeUnit(
    val id: String,
    val title: String,
    val instructions: List<PracticeInstruction>,
    val notes: String? = null,
    val focus: String? = null,
    val defaultClubReference: String? = null,
    val createdAt: Instant,
    val updatedAt: Instant,
)
