package com.loganmartlew.rangework.shared.model

import kotlinx.datetime.Instant
import kotlinx.serialization.Serializable

@Serializable
data class PracticeSession(
    val id: String,
    val name: String,
    val items: List<PracticeSessionItem>,
    val notes: String? = null,
    val tags: List<Tag> = emptyList(),
    val createdAt: Instant,
    val updatedAt: Instant,
    val archivedAt: Instant? = null,
) {
    val isArchived: Boolean get() = archivedAt != null
}
