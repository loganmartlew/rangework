package com.loganmartlew.rangework.shared.data

import com.loganmartlew.rangework.shared.model.PracticeSession
import com.loganmartlew.rangework.shared.model.PracticeSessionDraft
import com.loganmartlew.rangework.shared.model.PracticeSessionItem
import com.loganmartlew.rangework.shared.model.PracticeSessionItemDraft
import com.loganmartlew.rangework.shared.repository.PracticeSessionRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.datetime.Instant
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

private const val PRACTICE_SESSIONS_TABLE = "practice_sessions"
private const val PRACTICE_SESSION_ITEMS_TABLE = "practice_session_items"

class SupabasePracticeSessionRepository(
    private val client: SupabaseClient,
) : PracticeSessionRepository {
    override suspend fun listPracticeSessions(): List<PracticeSession> {
        val sessionRows = client.postgrest[PRACTICE_SESSIONS_TABLE]
            .select()
            .decodeList<PracticeSessionRow>()

        if (sessionRows.isEmpty()) {
            return emptyList()
        }

        val itemRows = client.postgrest[PRACTICE_SESSION_ITEMS_TABLE]
            .select()
            .decodeList<PracticeSessionItemRow>()
            .groupBy(PracticeSessionItemRow::practiceSessionId)

        return sessionRows
            .map { row ->
                row.toModel(
                    items = itemRows[row.id].orEmpty(),
                )
            }
            .sortedByDescending(PracticeSession::updatedAt)
    }

    override suspend fun getPracticeSession(sessionId: String): PracticeSession? {
        val sessionRow = client.postgrest[PRACTICE_SESSIONS_TABLE]
            .select {
                filter {
                    eq("id", sessionId)
                }
            }
            .decodeList<PracticeSessionRow>()
            .firstOrNull()
            ?: return null

        val itemRows = client.postgrest[PRACTICE_SESSION_ITEMS_TABLE]
            .select {
                filter {
                    eq("practice_session_id", sessionId)
                }
            }
            .decodeList<PracticeSessionItemRow>()

        return sessionRow.toModel(itemRows)
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun savePracticeSession(
        draft: PracticeSessionDraft,
        sessionId: String?,
    ): PracticeSession {
        val resolvedSessionId = sessionId ?: Uuid.random().toString()

        if (sessionId == null) {
            client.postgrest[PRACTICE_SESSIONS_TABLE].insert(
                PracticeSessionInsertRow(
                    id = resolvedSessionId,
                    name = draft.name,
                    notes = draft.notes,
                ),
            )
        } else {
            client.postgrest[PRACTICE_SESSIONS_TABLE].update(
                PracticeSessionUpdateRow(
                    name = draft.name,
                    notes = draft.notes,
                ),
            ) {
                filter {
                    eq("id", resolvedSessionId)
                }
            }
        }

        replaceItems(
            practiceSessionId = resolvedSessionId,
            items = draft.items,
        )

        return requireNotNull(getPracticeSession(resolvedSessionId)) {
            "Practice session $resolvedSessionId could not be loaded after save."
        }
    }

    override suspend fun deletePracticeSession(sessionId: String) {
        val existing = getPracticeSession(sessionId)
            ?: throw NoSuchElementException("Practice session $sessionId does not exist.")

        client.postgrest[PRACTICE_SESSIONS_TABLE].delete {
            filter {
                eq("id", existing.id)
            }
        }
    }

    @OptIn(ExperimentalUuidApi::class)
    private suspend fun replaceItems(
        practiceSessionId: String,
        items: List<PracticeSessionItemDraft>,
    ) {
        client.postgrest[PRACTICE_SESSION_ITEMS_TABLE].delete {
            filter {
                eq("practice_session_id", practiceSessionId)
            }
        }

        if (items.isEmpty()) {
            return
        }

        client.postgrest[PRACTICE_SESSION_ITEMS_TABLE].insert(
            items.map { item ->
                PracticeSessionItemInsertRow(
                    id = Uuid.random().toString(),
                    practiceSessionId = practiceSessionId,
                    practiceUnitId = item.practiceUnitId,
                    sortOrder = item.order,
                    repeatCount = item.repeatCount,
                    clubReference = item.clubReference,
                    notes = item.notes,
                    focusCue = item.focusCue,
                )
            },
        )
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
private data class PracticeSessionInsertRow(
    val id: String,
    val name: String,
    val notes: String? = null,
)

@Serializable
private data class PracticeSessionUpdateRow(
    val name: String,
    val notes: String? = null,
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
    @SerialName("club_reference")
    val clubReference: String? = null,
    val notes: String? = null,
    @SerialName("focus_cue")
    val focusCue: String? = null,
)

@Serializable
private data class PracticeSessionItemInsertRow(
    val id: String,
    @SerialName("practice_session_id")
    val practiceSessionId: String,
    @SerialName("practice_unit_id")
    val practiceUnitId: String,
    @SerialName("sort_order")
    val sortOrder: Int,
    @SerialName("repeat_count")
    val repeatCount: Int,
    @SerialName("club_reference")
    val clubReference: String? = null,
    val notes: String? = null,
    @SerialName("focus_cue")
    val focusCue: String? = null,
)

private fun PracticeSessionRow.toModel(
    items: List<PracticeSessionItemRow>,
): PracticeSession = PracticeSession(
    id = id,
    name = name,
    items = items
        .sortedBy(PracticeSessionItemRow::sortOrder)
        .map { row ->
            PracticeSessionItem(
                id = row.id,
                practiceUnitId = row.practiceUnitId,
                order = row.sortOrder,
                repeatCount = row.repeatCount,
                clubReference = row.clubReference,
                notes = row.notes,
                focusCue = row.focusCue,
            )
        },
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
