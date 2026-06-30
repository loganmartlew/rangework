package com.loganmartlew.rangework.shared.data

import com.loganmartlew.rangework.shared.model.PracticeSession
import com.loganmartlew.rangework.shared.model.PracticeSessionDraft
import com.loganmartlew.rangework.shared.model.PracticeSessionItem
import com.loganmartlew.rangework.shared.repository.PracticeSessionRepository
import kotlinx.datetime.Clock
import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

class InMemoryPracticeSessionRepository : PracticeSessionRepository() {
    private val store = mutableMapOf<String, PracticeSession>()
    val drafts = mutableListOf<PracticeSessionDraft>()

    override suspend fun list(): List<PracticeSession> =
        store.values.sortedByDescending { it.updatedAt }

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
                )
            },
            notes = validated.notes,
            createdAt = store[resolvedId]?.createdAt ?: now,
            updatedAt = now,
        )
        store[resolvedId] = session
        drafts += validated
        return session
    }

    override suspend fun delete(id: String) {
        store.remove(id)
    }
}