package com.loganmartlew.rangework.shared.repository

import com.loganmartlew.rangework.shared.model.ActiveRangeSessionSummary
import com.loganmartlew.rangework.shared.model.CompletedRangeSessionSummary
import com.loganmartlew.rangework.shared.model.RangeSession
import kotlinx.datetime.Instant

interface RangeSessionRepository {
    suspend fun start(sessionId: String): RangeSession
    suspend fun getSession(rangeSessionId: String): RangeSession?
    suspend fun listActiveSessions(): List<ActiveRangeSessionSummary>
    suspend fun listCompletedSessions(sessionId: String): List<CompletedRangeSessionSummary>
    /**
     * Marks the given steps complete or incomplete in one write. Steps
     * completed together share a single completion timestamp (a counter tap
     * sweeping Action Steps, or a finish-time batch completion, is
     * self-evidently a batch in the data).
     */
    suspend fun setStepsCompletion(rangeSessionId: String, stepIndices: List<Int>, completed: Boolean): RangeSession

    /** Records a per-step Club Override for each of the given steps. */
    suspend fun overrideStepClubs(rangeSessionId: String, stepIndices: List<Int>, clubCode: String): RangeSession
    suspend fun finishSession(rangeSessionId: String): RangeSession
    suspend fun abandonSession(rangeSessionId: String)
    suspend fun recordTimeEntry(rangeSessionId: String, enteredAt: Instant)
    suspend fun closeTimeEntry(rangeSessionId: String, enteredAt: Instant, exitedAt: Instant)
    suspend fun getElapsedSeconds(rangeSessionId: String): Long
    suspend fun hasActiveSessionsForTemplate(sessionId: String): Boolean
}
