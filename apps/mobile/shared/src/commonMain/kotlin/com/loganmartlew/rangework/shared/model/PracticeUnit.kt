package com.loganmartlew.rangework.shared.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class PracticeUnit(
    val id: String,
    val title: String,
    val instructions: List<PracticeInstruction>,
    val notes: String? = null,
    val focus: String? = null,
    val defaultClubCode: String? = null,
    /** Optional rubric read by the player and coaching model; never machine-parsed. */
    val successCriterion: String? = null,
    val tags: List<Tag> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant,
    /**
     * The session that owns this unit as an Inline Unit, or null for an ordinary
     * library unit. Non-null units are excluded from the library and die with
     * their session (cascade). Promotion detaches ownership by setting this null.
     */
    val scopedToSessionId: String? = null,
) {
    /** True when this unit is session-owned content, not a library citizen. */
    val isInline: Boolean get() = scopedToSessionId != null
}
