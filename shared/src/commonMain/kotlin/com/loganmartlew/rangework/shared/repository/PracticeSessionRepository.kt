package com.loganmartlew.rangework.shared.repository

import com.loganmartlew.rangework.shared.model.PracticeSession
import com.loganmartlew.rangework.shared.model.PracticeSessionDraft

interface PracticeSessionRepository {
    suspend fun listPracticeSessions(): List<PracticeSession>

    suspend fun getPracticeSession(sessionId: String): PracticeSession?

    suspend fun savePracticeSession(
        draft: PracticeSessionDraft,
        sessionId: String? = null,
    ): PracticeSession

    suspend fun deletePracticeSession(sessionId: String)
}
