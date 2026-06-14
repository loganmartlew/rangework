package com.loganmartlew.rangework.shared.model

data class PracticeInstructionDraft(
    val order: Int,
    val text: String,
    val clubReference: String? = null,
    val repCount: Int? = null,
    val ballCount: Int? = null,
)
