package com.loganmartlew.rangework.shared.data

import com.loganmartlew.rangework.shared.model.PracticeInstruction
import com.loganmartlew.rangework.shared.model.PracticeUnit
import com.loganmartlew.rangework.shared.model.PracticeUnitDraft
import com.loganmartlew.rangework.shared.repository.PracticeUnitRepository
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class InMemoryPracticeUnitRepository : PracticeUnitRepository() {
    private val store = mutableMapOf<String, PracticeUnit>()
    val drafts = mutableListOf<PracticeUnitDraft>()

    override suspend fun list(): List<PracticeUnit> =
        // Inline Units (scoped to a session) never surface in the library.
        store.values.filter { it.scopedToSessionId == null }.sortedByDescending { it.updatedAt }

    // `get` is deliberately unfiltered so a session can load its inline units by id.
    override suspend fun get(id: String): PracticeUnit? = store[id]

    @OptIn(ExperimentalUuidApi::class)
    override suspend fun persist(validated: PracticeUnitDraft, unitId: String?): PracticeUnit {
        val resolvedId = unitId ?: Uuid.random().toString()
        val now = Clock.System.now()
        val unit = PracticeUnit(
            id = resolvedId,
            title = validated.title,
            instructions = validated.instructions.map { instruction ->
                PracticeInstruction(
                    id = Uuid.random().toString(),
                    order = instruction.order,
                    text = instruction.text,
                    ballCount = instruction.ballCount,
                    clubCode = instruction.clubCode,
                )
            },
            notes = validated.notes,
            focus = validated.focus,
            defaultClubCode = validated.defaultClubCode,
            successCriterion = validated.successCriterion,
            createdAt = store[resolvedId]?.createdAt ?: now,
            updatedAt = now,
            // Editing an Inline Unit preserves its scope (D5): the editor saves
            // it under its own id and must not detach ownership.
            scopedToSessionId = store[resolvedId]?.scopedToSessionId,
        )
        store[resolvedId] = unit
        drafts += validated
        return unit
    }

    override suspend fun setScopedSession(id: String, sessionId: String?): PracticeUnit {
        val existing = requireNotNull(store[id]) { "Unit $id not found" }
        val updated = existing.copy(scopedToSessionId = sessionId)
        store[id] = updated
        return updated
    }

    /**
     * Deep-copy an existing unit as a fresh Inline Unit scoped to [sessionId],
     * mirroring the server-side copy in `duplicate_practice_session`. New unit
     * and instruction ids; used by the in-memory session repo's `duplicate`.
     */
    @OptIn(ExperimentalUuidApi::class)
    internal fun cloneScoped(source: PracticeUnit, sessionId: String): PracticeUnit {
        val now = Clock.System.now()
        val copy = source.copy(
            id = Uuid.random().toString(),
            instructions = source.instructions.map { it.copy(id = Uuid.random().toString()) },
            scopedToSessionId = sessionId,
            createdAt = now,
            updatedAt = now,
        )
        store[copy.id] = copy
        return copy
    }

    /**
     * Cascade helper mirroring the schema's `on delete cascade` on
     * `scoped_to_session_id`: remove every Inline Unit owned by [sessionId].
     * Used by the in-memory session repo's `delete`.
     */
    internal fun deleteScopedTo(sessionId: String) {
        store.values
            .filter { it.scopedToSessionId == sessionId }
            .map { it.id }
            .forEach(store::remove)
    }

    /**
     * Orphan GC mirroring `save_practice_session` step 6: remove Inline Units
     * owned by [sessionId] whose id is not in [keep]. Promoted units (null
     * scope) are outside the predicate and always survive.
     */
    internal fun reapOrphansScopedTo(sessionId: String, keep: Set<String>) {
        store.values
            .filter { it.scopedToSessionId == sessionId && it.id !in keep }
            .map { it.id }
            .forEach(store::remove)
    }

    override suspend fun delete(id: String) {
        store.remove(id)
    }
}