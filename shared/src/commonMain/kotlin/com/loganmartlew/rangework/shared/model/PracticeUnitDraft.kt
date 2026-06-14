package com.loganmartlew.rangework.shared.model

data class PracticeUnitDraft(
    val title: String,
    val instructions: List<PracticeInstructionDraft>,
    val notes: String? = null,
)
