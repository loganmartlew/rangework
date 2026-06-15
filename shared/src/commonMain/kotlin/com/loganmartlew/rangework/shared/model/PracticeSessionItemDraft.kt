package com.loganmartlew.rangework.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class PracticeSessionItemDraft(
    val practiceUnitId: String,
    val order: Int,
    val notes: String? = null,
    val focusCue: String? = null,
    val restSeconds: Int? = null,
    val overrideBallCount: Int? = null,
)
