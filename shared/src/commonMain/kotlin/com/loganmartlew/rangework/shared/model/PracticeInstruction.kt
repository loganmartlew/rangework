package com.loganmartlew.rangework.shared.model

import kotlinx.serialization.Serializable

@Serializable
data class PracticeInstruction(
    val id: String,
    val order: Int,
    val text: String,
    val ballCount: Int? = null,
)
