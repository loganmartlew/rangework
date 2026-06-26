package com.loganmartlew.rangework.shared.repository

import com.loganmartlew.rangework.shared.model.Tag
import com.loganmartlew.rangework.shared.model.TagAttachmentCounts

interface TagRepository {
    /** All Tags visible to the user: Default Tags plus the user's own Custom Tags. */
    suspend fun list(): List<Tag>

    /**
     * Resolve a typed name to a Tag, applying the shared dedup/collision rule
     * (slug → Default → own Custom → new Custom). May create a Custom Tag.
     */
    suspend fun createOrGet(name: String): Tag

    /** Rename a Custom Tag, preserving its identity and every attachment. */
    suspend fun rename(tagId: String, newName: String): Tag

    /** Hard-delete a Custom Tag, cascading to all its attachments. */
    suspend fun delete(tagId: String)

    /** How many Units and Sessions a Tag is attached to (for delete confirmation). */
    suspend fun attachmentCounts(tagId: String): TagAttachmentCounts
}
