package com.loganmartlew.rangework.shared.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * A label from the single shared tagging vocabulary, attached to Practice Units
 * and Practice Sessions. A Default Tag is owned by no user (`isDefault`); a
 * Custom Tag is owned by the user who created it.
 */
@Serializable
data class Tag(
    val id: String,
    val code: String,
    val displayName: String,
    val isDefault: Boolean,
)

/** How many Units and Sessions a Tag is currently attached to. */
@Serializable
data class TagAttachmentCounts(
    @SerialName("unit_count")
    val unitCount: Int,
    @SerialName("session_count")
    val sessionCount: Int,
)

/** The maximum number of Tags that may be attached to a single Unit or Session. */
const val MAX_TAGS_PER_ITEM: Int = 8
