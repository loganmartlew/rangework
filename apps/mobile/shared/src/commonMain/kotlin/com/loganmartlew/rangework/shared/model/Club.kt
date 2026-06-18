package com.loganmartlew.rangework.shared.model

import kotlinx.serialization.Serializable

@Serializable
enum class ClubCategory {
    WOOD,
    HYBRID,
    IRON,
    WEDGE,
    PUTTER,
}

@Serializable
data class Club(
    val code: String,
    val displayName: String,
    val category: ClubCategory,
    val sortOrder: Int,
)
