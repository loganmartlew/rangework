package com.loganmartlew.rangework.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class PracticeInstructionDraft(
    val order: Int,
    val text: String,
    val ballCount: Int? = null,
    val clubCode: String? = null,
)
