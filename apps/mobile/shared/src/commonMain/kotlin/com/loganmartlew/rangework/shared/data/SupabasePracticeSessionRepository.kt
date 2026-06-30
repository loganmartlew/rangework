package com.loganmartlew.rangework.shared.data

import com.loganmartlew.rangework.shared.model.PracticeSession
import com.loganmartlew.rangework.shared.model.PracticeSessionDraft
import com.loganmartlew.rangework.shared.model.PracticeSessionItem
import com.loganmartlew.rangework.shared.model.Tag
import com.loganmartlew.rangework.shared.repository.PracticeSessionRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.rpc
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val PRACTICE_SESSIONS_TABLE = "practice_sessions"
private const val PRACTICE_SESSION_ITEMS_TABLE = "practice_session_items"

class SupabasePracticeSessionRepository(
    private val client: SupabaseClient,
) : PracticeSessionRepository() {
    override suspend fun list(): List<PracticeSession> {
        val sessionRows = client.postgrest[PRACTICE_SESSIONS_TABLE]
            .select()
            .decodeList<PracticeSessionRow>()

        if (sessionRows.isEmpty()) {
            return emptyList()
        }

        val itemRows = client.postgrest[PRACTICE_SESSION_ITEMS_TABLE]
            .select()
            .decodeList<PracticeSessionItemRow>()

        val tagsById = client.loadVisibleTagsById()
        val tagsBySession = client.postgrest[PRACTICE_SESSION_TAGS_TABLE]
            .select()
            .decodeList<PracticeSessionTagRow>()
            .groupBy(PracticeSessionTagRow::practiceSessionId)

        return assembleParentsWithChildren(
            parents = sessionRows,
            children = itemRows,
            parentId = PracticeSessionRow::id,
            childParentId = PracticeSessionItemRow::practiceSessionId,
            childOrder = PracticeSessionItemRow::sortOrder,
            toModel = { row, items ->
                row.toModel(
                    items = items,
                    tags = resolveTags(
                        tagsBySession[row.id].orEmpty().map(PracticeSessionTagRow::tagId),
                        tagsById,
                    ),
                )
            },
            modelSort = PracticeSession::updatedAt,
        )
    }

    override suspend fun get(id: String): PracticeSession? {
        val sessionRow = client.postgrest[PRACTICE_SESSIONS_TABLE]
            .select {
                filter {
                    eq("id", id)
                }
            }
            .decodeList<PracticeSessionRow>()
            .firstOrNull()
            ?: return null

        val itemRows = client.postgrest[PRACTICE_SESSION_ITEMS_TABLE]
            .select {
                filter {
                    eq("practice_session_id", id)
                }
            }
            .decodeList<PracticeSessionItemRow>()

        val tagIds = client.postgrest[PRACTICE_SESSION_TAGS_TABLE]
            .select {
                filter {
                    eq("practice_session_id", id)
                }
            }
            .decodeList<PracticeSessionTagRow>()
            .map(PracticeSessionTagRow::tagId)
        val tags = resolveTags(tagIds, client.loadVisibleTagsById())

        return assembleParentWithChildren(
            parent = sessionRow,
            children = itemRows,
            childOrder = PracticeSessionItemRow::sortOrder,
            toModel = { row, items -> row.toModel(items, tags) },
        )
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun persist(validated: PracticeSessionDraft, sessionId: String?): PracticeSession {
        val resolvedSessionId = sessionId ?: Uuid.random().toString()
        val params = SavePracticeSessionParams(
            sessionId = resolvedSessionId,
            name = validated.name,
            notes = validated.notes,
            items = validated.items.map { item ->
                SessionItemParam(
                    practiceUnitId = item.practiceUnitId,
                    order = item.order,
                    repeatCount = item.repeatCount,
                    clubCode = item.clubCode,
                    notes = item.notes,
                    focusCue = item.focusCue,
                )
            },
            tagIds = validated.tagIds,
        )
        client.postgrest.rpc(
            "save_practice_session",
            Json.encodeToJsonElement(SavePracticeSessionParams.serializer(), params).jsonObject,
        )
        return requireNotNull(get(resolvedSessionId)) {
            "Practice session $resolvedSessionId could not be loaded after save."
        }
    }

    override suspend fun delete(id: String) {
        val existing = get(id)
            ?: throw NoSuchElementException("Practice session $id does not exist.")

        client.postgrest[PRACTICE_SESSIONS_TABLE].delete {
            filter {
                eq("id", existing.id)
            }
        }
    }
}

@Serializable
private data class PracticeSessionRow(
    val id: String,
    val name: String,
    val notes: String? = null,
    @SerialName("created_at")
    val createdAt: Instant,
    @SerialName("updated_at")
    val updatedAt: Instant,
)

@Serializable
private data class PracticeSessionItemRow(
    val id: String,
    @SerialName("practice_session_id")
    val practiceSessionId: String,
    @SerialName("practice_unit_id")
    val practiceUnitId: String,
    @SerialName("sort_order")
    val sortOrder: Int,
    @SerialName("repeat_count")
    val repeatCount: Int,
    @SerialName("club_code")
    val clubCode: String? = null,
    val notes: String? = null,
    @SerialName("focus_cue")
    val focusCue: String? = null,
)

@Serializable
private data class SavePracticeSessionParams(
    @SerialName("p_session_id") val sessionId: String,
    @SerialName("p_name") val name: String,
    @SerialName("p_notes") val notes: String?,
    @SerialName("p_items") val items: List<SessionItemParam>,
    @SerialName("p_tag_ids") val tagIds: List<String>,
)

@Serializable
private data class SessionItemParam(
    @SerialName("practice_unit_id") val practiceUnitId: String,
    @SerialName("order") val order: Int,
    @SerialName("repeat_count") val repeatCount: Int,
    @SerialName("club_code") val clubCode: String?,
    @SerialName("notes") val notes: String?,
    @SerialName("focus_cue") val focusCue: String?,
)

private fun PracticeSessionRow.toModel(
    items: List<PracticeSessionItemRow>,
    tags: List<Tag>,
): PracticeSession = PracticeSession(
    id = id,
    name = name,
    items = items.map { row ->
        PracticeSessionItem(
            id = row.id,
            practiceUnitId = row.practiceUnitId,
            order = row.sortOrder,
            repeatCount = row.repeatCount,
            clubCode = row.clubCode,
            notes = row.notes,
            focusCue = row.focusCue,
        )
    },
    notes = notes,
    tags = tags,
    createdAt = createdAt,
    updatedAt = updatedAt,
)