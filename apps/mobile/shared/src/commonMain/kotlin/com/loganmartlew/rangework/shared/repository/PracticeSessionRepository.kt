package com.loganmartlew.rangework.shared.repository

import com.loganmartlew.rangework.shared.model.PracticeSession
import com.loganmartlew.rangework.shared.model.PracticeSessionDraft

abstract class PracticeSessionRepository {
    abstract suspend fun persist(validated: PracticeSessionDraft, sessionId: String?): PracticeSession
    abstract suspend fun get(id: String): PracticeSession?
    abstract suspend fun list(): List<PracticeSession>
    abstract suspend fun delete(id: String)
}
