package com.loganmartlew.rangework.shared.repository

import com.loganmartlew.rangework.shared.model.ActiveRangeSessionSummary
import com.loganmartlew.rangework.shared.model.CompletedRangeSessionSummary
import com.loganmartlew.rangework.shared.model.RangeSession
import kotlinx.datetime.Instant

interface RangeSessionRepository {
    suspend fun startSession(rangeSessionId: String, sessionId: String): RangeSession
    suspend fun getSession(rangeSessionId: String): RangeSession?
    suspend fun listActiveSessions(): List<ActiveRangeSessionSummary>
    suspend fun listCompletedSessions(sessionId: String): List<CompletedRangeSessionSummary>
    suspend fun toggleStepComplete(rangeSessionId: String, stepIndex: Int, completed: Boolean): RangeSession
    suspend fun overrideStepClub(rangeSessionId: String, stepIndex: Int, clubCode: String): RangeSession
    suspend fun updateLastViewedStep(rangeSessionId: String, stepIndex: Int)
    suspend fun finishSession(rangeSessionId: String): RangeSession
    suspend fun abandonSession(rangeSessionId: String)
    suspend fun recordTimeEntry(rangeSessionId: String, enteredAt: Instant)
    suspend fun closeTimeEntry(rangeSessionId: String, enteredAt: Instant, exitedAt: Instant)
    suspend fun getElapsedSeconds(rangeSessionId: String): Long
    suspend fun hasActiveSessionsForTemplate(sessionId: String): Boolean
}
