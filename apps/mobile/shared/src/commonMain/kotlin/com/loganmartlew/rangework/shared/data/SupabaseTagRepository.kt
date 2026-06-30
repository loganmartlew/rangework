package com.loganmartlew.rangework.shared.data

import com.loganmartlew.rangework.shared.model.SharedValidationException
import com.loganmartlew.rangework.shared.model.Tag
import com.loganmartlew.rangework.shared.model.TagAttachmentCounts
import com.loganmartlew.rangework.shared.model.ValidationIssue
import com.loganmartlew.rangework.shared.model.ValidationTarget
import com.loganmartlew.rangework.shared.model.slugifyTag
import com.loganmartlew.rangework.shared.repository.TagRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import io.github.jan.supabase.postgrest.rpc
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject

internal const val TAGS_TABLE = "tags"
internal const val PRACTICE_UNIT_TAGS_TABLE = "practice_unit_tags"
internal const val PRACTICE_SESSION_TAGS_TABLE = "practice_session_tags"

class SupabaseTagRepository(
    private val client: SupabaseClient,
) : TagRepository {

    override suspend fun list(): List<Tag> =
        client.postgrest[TAGS_TABLE]
            .select {
                order("display_name", Order.ASCENDING)
            }
            .decodeList<TagRow>()
            .map(TagRow::toModel)

    override suspend fun createOrGet(name: String): Tag {
        val code = slugifyTag(name)
            ?: throw SharedValidationException(
                listOf(
                    ValidationIssue(
                        target = ValidationTarget.Tags,
                        message = "Tag name must contain at least one letter or number.",
                    ),
                ),
            )
        val params = CreateOrGetTagParams(code = code, name = name.trim())
        val tagId = client.postgrest
            .rpc(
                "create_or_get_tag",
                Json.encodeToJsonElement(CreateOrGetTagParams.serializer(), params).jsonObject,
            )
            .decodeAs<String>()
        return getById(tagId)
    }

    override suspend fun rename(tagId: String, newName: String): Tag {
        val trimmed = newName.trim()
        if (trimmed.isEmpty()) {
            throw SharedValidationException(
                listOf(
                    ValidationIssue(target = ValidationTarget.Tags, message = "Tag name cannot be blank."),
                ),
            )
        }
        client.postgrest[TAGS_TABLE].update(
            TagDisplayNameUpdate(displayName = trimmed),
        ) {
            filter { eq("id", tagId) }
        }
        return getById(tagId)
    }

    override suspend fun delete(tagId: String) {
        client.postgrest[TAGS_TABLE].delete {
            filter { eq("id", tagId) }
        }
    }

    override suspend fun attachmentCounts(tagId: String): TagAttachmentCounts {
        val params = CountTagAttachmentsParams(tagId = tagId)
        return client.postgrest
            .rpc(
                "count_tag_attachments",
                Json.encodeToJsonElement(CountTagAttachmentsParams.serializer(), params).jsonObject,
            )
            .decodeAs<TagAttachmentCounts>()
    }

    private suspend fun getById(tagId: String): Tag =
        requireNotNull(
            client.postgrest[TAGS_TABLE]
                .select {
                    filter { eq("id", tagId) }
                }
                .decodeList<TagRow>()
                .firstOrNull()
                ?.toModel(),
        ) { "Tag $tagId could not be loaded." }
}

/** Load every Tag visible to the user, keyed by id, for resolving attachments. */
internal suspend fun SupabaseClient.loadVisibleTagsById(): Map<String, Tag> =
    postgrest[TAGS_TABLE]
        .select()
        .decodeList<TagRow>()
        .associate { it.id to it.toModel() }

/** Sort attached tags for stable display: Default Tags first, then alphabetical. */
internal fun List<Tag>.sortedForDisplay(): List<Tag> =
    sortedWith(compareByDescending<Tag> { it.isDefault }.thenBy { it.displayName.lowercase() })

@Serializable
internal data class PracticeUnitTagRow(
    @SerialName("practice_unit_id")
    val practiceUnitId: String,
    @SerialName("tag_id")
    val tagId: String,
)

@Serializable
internal data class PracticeSessionTagRow(
    @SerialName("practice_session_id")
    val practiceSessionId: String,
    @SerialName("tag_id")
    val tagId: String,
)

@Serializable
internal data class TagRow(
    val id: String,
    val code: String,
    @SerialName("display_name")
    val displayName: String,
    @SerialName("owner_id")
    val ownerId: String? = null,
)

internal fun TagRow.toModel(): Tag = Tag(
    id = id,
    code = code,
    displayName = displayName,
    isDefault = ownerId == null,
)

@Serializable
private data class CreateOrGetTagParams(
    @SerialName("p_code") val code: String,
    @SerialName("p_name") val name: String,
)

@Serializable
private data class CountTagAttachmentsParams(
    @SerialName("p_tag_id") val tagId: String,
)

@Serializable
private data class TagDisplayNameUpdate(
    @SerialName("display_name") val displayName: String,
)
