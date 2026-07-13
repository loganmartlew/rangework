package com.loganmartlew.rangework.shared.repository

import com.loganmartlew.rangework.shared.model.PracticeSession
import com.loganmartlew.rangework.shared.model.PracticeSessionDraft
import kotlinx.datetime.Instant

abstract class PracticeSessionRepository {
    abstract suspend fun persist(validated: PracticeSessionDraft, sessionId: String?): PracticeSession
    abstract suspend fun get(id: String): PracticeSession?
    abstract suspend fun list(): List<PracticeSession>
    abstract suspend fun listArchived(): List<PracticeSession>
    abstract suspend fun setArchived(id: String, archivedAt: Instant?): PracticeSession

    /**
     * Deep-copy a session into a new one: inline units are copied and owned by
     * the copy, library references are shared, and the copy is always
     * unarchived. Returns the new session.
     */
    abstract suspend fun duplicate(id: String): PracticeSession
    abstract suspend fun delete(id: String)
}