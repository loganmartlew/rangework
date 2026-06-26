package com.loganmartlew.rangework.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class PracticeUnitDraft(
    val title: String,
    val instructions: List<PracticeInstructionDraft>,
    val notes: String? = null,
    val focus: String? = null,
    val defaultClubCode: String? = null,
    val tagIds: List<String> = emptyList(),
)
