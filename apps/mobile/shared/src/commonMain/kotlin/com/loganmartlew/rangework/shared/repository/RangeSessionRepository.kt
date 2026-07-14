package com.loganmartlew.rangework.shared.repository

import com.loganmartlew.rangework.shared.model.ActiveRangeSessionSummary
import com.loganmartlew.rangework.shared.model.BlockResult
import com.loganmartlew.rangework.shared.model.CompletedRangeSessionSummary
import com.loganmartlew.rangework.shared.model.Observation
import com.loganmartlew.rangework.shared.model.RangeSession
import kotlinx.datetime.Instant

interface RangeSessionRepository {
    suspend fun start(sessionId: String): RangeSession
    suspend fun getSession(rangeSessionId: String): RangeSession?
    suspend fun listActiveSessions(): List<ActiveRangeSessionSummary>
    suspend fun listCompletedSessions(sessionId: String): List<CompletedRangeSessionSummary>
    /**
     * Atomically marks the given steps complete or incomplete in one write. Steps
     * completed together share a single completion timestamp (a counter tap
     * sweeping Action Steps, or a finish-time batch completion, is
     * self-evidently a batch in the data). Concurrent calls must merge against
     * the latest stored value rather than replacing another call's progress.
     */
    suspend fun setStepsCompletion(rangeSessionId: String, stepIndices: List<Int>, completed: Boolean): RangeSession

    /** Records a per-step Club Override for each of the given steps. */
    suspend fun overrideStepClubs(rangeSessionId: String, stepIndices: List<Int>, clubCode: String): RangeSession
    suspend fun finishSession(rangeSessionId: String): RangeSession
    suspend fun abandonSession(rangeSessionId: String)

    // ── Data recording (snapshot v3) ─────────────────────────────────────────

    /** Writes the session-level free-text note (null clears it). */
    suspend fun saveSessionNote(rangeSessionId: String, note: String?): RangeSession

    /**
     * Merges a Block Result into `block_results` under [unitIndex]. Merge
     * semantics: the single key is replaced; an [BlockResult.isEmpty] result
     * removes the key. Sibling keys are always preserved.
     */
    suspend fun saveBlockResult(rangeSessionId: String, unitIndex: Int, result: BlockResult): RangeSession

    /** All Observations for the session, ascending by step index. */
    suspend fun listObservations(rangeSessionId: String): List<Observation>

    /**
     * Upserts one ball's Observation row, keyed by (range_session_id, step_index).
     * [values] is the complete record for the ball — the write replaces the whole
     * stored value map, it does not merge per type. Callers must pass every value
     * the ball still carries, not just a changed one.
     */
    suspend fun upsertObservation(rangeSessionId: String, stepIndex: Int, values: Map<String, String>): Observation

    /** Deletes the Observation rows for the given step indices (no-op for absent rows). */
    suspend fun deleteObservations(rangeSessionId: String, stepIndices: List<Int>)
    suspend fun recordTimeEntry(rangeSessionId: String, enteredAt: Instant)
    suspend fun closeTimeEntry(rangeSessionId: String, enteredAt: Instant, exitedAt: Instant)
    suspend fun getElapsedSeconds(rangeSessionId: String): Long
    suspend fun hasActiveSessionsForTemplate(sessionId: String): Boolean
}
