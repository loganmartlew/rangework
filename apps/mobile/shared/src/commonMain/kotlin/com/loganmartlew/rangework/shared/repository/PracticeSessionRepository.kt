package com.loganmartlew.rangework.shared.repository

import com.loganmartlew.rangework.shared.model.PracticeSession
import com.loganmartlew.rangework.shared.model.PracticeSessionDraft
import com.loganmartlew.rangework.shared.model.PracticeSessionItemDraft
import com.loganmartlew.rangework.shared.model.validated

abstract class PracticeSessionRepository {
    suspend fun save(draft: PracticeSessionDraft, sessionId: String? = null): PracticeSession =
        persist(draft.validated(), sessionId?.trim()?.takeIf(String::isNotEmpty))

    suspend fun duplicate(id: String): PracticeSession {
        val session = get(id) ?: error("Session $id not found")
        return persist(
            PracticeSessionDraft(
                name = session.name,
                notes = session.notes,
                items = session.items.map { item ->
                    PracticeSessionItemDraft(
                        practiceUnitId = item.practiceUnitId,
                        order = item.order,
                        repeatCount = item.repeatCount,
                        clubCode = item.clubCode,
                        notes = item.notes,
                        focusCue = item.focusCue,
                    )
                },
            ),
            null,
        )
    }

    protected abstract suspend fun persist(validated: PracticeSessionDraft, sessionId: String?): PracticeSession
    abstract suspend fun get(id: String): PracticeSession?
    abstract suspend fun list(): List<PracticeSession>
    abstract suspend fun delete(id: String)
}
