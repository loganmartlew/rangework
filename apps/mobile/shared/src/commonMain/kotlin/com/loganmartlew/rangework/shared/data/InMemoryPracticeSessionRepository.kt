package com.loganmartlew.rangework.shared.data

import com.loganmartlew.rangework.shared.model.PracticeSession
import com.loganmartlew.rangework.shared.model.PracticeSessionDraft
import com.loganmartlew.rangework.shared.model.PracticeSessionItem
import com.loganmartlew.rangework.shared.repository.PracticeSessionRepository
import kotlinx.datetime.Clock
import kotlinx.datetime.Instant
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class InMemoryPracticeSessionRepository : PracticeSessionRepository() {
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
        return session
    }

    override suspend fun setArchived(id: String, archivedAt: Instant?): PracticeSession {
        val existing = requireNotNull(store[id]) { "Session $id not found" }
        val updated = existing.copy(archivedAt = archivedAt)
        store[id] = updated
        return updated
    }

    override suspend fun delete(id: String) {
        store.remove(id)
    }
}