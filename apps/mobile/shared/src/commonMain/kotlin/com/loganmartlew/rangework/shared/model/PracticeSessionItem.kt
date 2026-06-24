package com.loganmartlew.rangework.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class PracticeSessionItem(
    val id: String,
    val practiceUnitId: String,
    val order: Int,
    val repeatCount: Int,
    val clubCode: String? = null,
    val notes: String? = null,
    val focusCue: String? = null,
)
