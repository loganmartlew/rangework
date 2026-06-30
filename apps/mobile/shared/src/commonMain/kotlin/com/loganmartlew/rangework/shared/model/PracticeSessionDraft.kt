package com.loganmartlew.rangework.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class PracticeSessionDraft(
    val name: String,
    val items: List<PracticeSessionItemDraft>,
    val notes: String? = null,
    val tagIds: List<String> = emptyList(),
)
