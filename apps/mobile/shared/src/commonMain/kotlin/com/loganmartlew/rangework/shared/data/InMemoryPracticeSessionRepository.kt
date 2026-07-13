package com.loganmartlew.rangework.shared.data

import com.loganmartlew.rangework.shared.model.PracticeSession
import com.loganmartlew.rangework.shared.model.PracticeSessionDraft
import com.loganmartlew.rangework.shared.model.PracticeSessionItem
import com.loganmartlew.rangework.shared.repository.PracticeSessionRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class InMemoryPracticeSessionRepository(
    // Optional handle to the unit repo so the deep-copy and cascade-delete of
    // Inline Units are observable in the shared in-memory world, matching the
    // server-side `duplicate_practice_session` and the scope cascade FK. When
    // null, sessions carry no inline units (library-only test doubles).
    private val unitRepository: InMemoryPracticeUnitRepository? = null,
) : PracticeSessionRepository() {
    private val store = mutableMapOf<String, PracticeSession>()
    val drafts = mutableListOf<PracticeSessionDraft>()

    override suspend fun list(): List<PracticeSession> =
        store.values.filter { !it.isArchived }.sortedByDescending { it.updatedAt }

    override suspend fun listArchived(): List<PracticeSession> =
        store.values.filter { it.isArchived }.sortedByDescending { it.updatedAt }

    override suspend fun get(id: String): PracticeSession? = store[id]

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun persist(validated: PracticeSessionDraft, sessionId: String?): PracticeSession {
        val resolvedId = sessionId ?: Uuid.random().toString()
        val now = Clock.System.now()
        val session = PracticeSession(
            id = resolvedId,
            name = validated.name,
            items = validated.items.map { item ->
                PracticeSessionItem(
                    id = Uuid.random().toString(),
                    practiceUnitId = item.practiceUnitId,
                    order = item.order,
                    repeatCount = item.repeatCount,
                    clubCode = item.clubCode,
                    notes = item.notes,
                    focusCue = item.focusCue,
                    observationTypes = item.observationTypes,
                )
            },
            notes = validated.notes,
            createdAt = store[resolvedId]?.createdAt ?: now,
            updatedAt = now,
            archivedAt = store[resolvedId]?.archivedAt,
        )
        store[resolvedId] = session
        drafts += validated
        // Orphan GC (mirrors save_practice_session step 6): reap Inline Units
        // this session owns that the save dropped or replaced.
        unitRepository?.reapOrphansScopedTo(
            resolvedId,
            keep = session.items.map { it.practiceUnitId }.toSet(),
        )
        return session
    }

    override suspend fun setArchived(id: String, archivedAt: Instant?): PracticeSession {
        val existing = requireNotNull(store[id]) { "Session $id not found" }
        val updated = existing.copy(archivedAt = archivedAt)
        store[id] = updated
        return updated
    }

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun duplicate(id: String): PracticeSession {
        val source = requireNotNull(store[id]) { "Session $id not found" }
        val newId = Uuid.random().toString()
        val now = Clock.System.now()

        val copiedItems = source.items.map { item ->
            // An item whose unit is an Inline Unit of the source session gets a
            // deep copy scoped to the new session; library references are shared.
            val unit = unitRepository?.get(item.practiceUnitId)
            if (unit != null && unit.scopedToSessionId == id) {
                val clone = unitRepository.cloneScoped(unit, newId)
                item.copy(id = Uuid.random().toString(), practiceUnitId = clone.id)
            } else {
                item.copy(id = Uuid.random().toString())
            }
        }

        val copy = source.copy(
            id = newId,
            items = copiedItems,
            createdAt = now,
            updatedAt = now,
            // A duplicate is always unarchived.
            archivedAt = null,
        )
        store[newId] = copy
        return copy
    }

    override suspend fun delete(id: String) {
        // Mirror the schema's `on delete cascade` on scoped_to_session_id.
        unitRepository?.deleteScopedTo(id)
        store.remove(id)
    }
}