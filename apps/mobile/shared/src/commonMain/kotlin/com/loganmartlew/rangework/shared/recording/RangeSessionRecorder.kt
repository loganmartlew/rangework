package com.loganmartlew.rangework.shared.recording

import com.loganmartlew.rangework.shared.model.Observation
import com.loganmartlew.rangework.shared.model.RangeSession
import com.loganmartlew.rangework.shared.model.RecordingRejection

/**
 * The outcome of a guarded recording write: either the value, or a structured
 * rejection carrying the invariant that refused it. Mirrors
 * `PracticeLibraryResult` — rejections are data, not exceptions.
 */
sealed interface RecordingResult<out T> {
    data class Success<T>(val value: T) : RecordingResult<T>
    data class Rejected(val reason: RecordingRejection) : RecordingResult<Nothing>
}

/**
 * The guarded domain layer over [com.loganmartlew.rangework.shared.repository.RangeSessionRepository]
 * for data capture. It owns the mutability and count-provenance invariants
 * ([com.loganmartlew.rangework.shared.model.RangeSessionRecordingRules]); the raw
 * repository stays available for execution stepping/finish. Mirrors the
 * `PracticeLibrary` / `DefaultPracticeLibrary` split.
 */
interface RangeSessionRecorder {
    /** Session-level reflection note. Whitespace-only normalizes to cleared. */
    suspend fun saveSessionNote(rangeSessionId: String, note: String?): RecordingResult<RangeSession>

    /** Free-text note on one Block. Whitespace-only clears just the note. */
    suspend fun saveBlockNote(rangeSessionId: String, unitIndex: Int, note: String?): RecordingResult<RangeSession>

    /** Manual X-of-Y success count on one Block. `null` clears the count. */
    suspend fun saveManualCount(rangeSessionId: String, unitIndex: Int, count: Int?): RecordingResult<RangeSession>

    /**
     * Records or corrects one ball's Observation (upsert by step index). [values]
     * is the *complete* record for the ball: the upsert replaces the whole stored
     * value map rather than merging per type, so callers must pass every value the
     * ball still carries, not just the changed one.
     */
    suspend fun recordObservation(
        rangeSessionId: String,
        stepIndex: Int,
        values: Map<String, String>,
    ): RecordingResult<Observation>

    /** Voids (deletes) the Observations for the given steps. */
    suspend fun voidObservations(rangeSessionId: String, stepIndices: List<Int>): RecordingResult<Unit>

    /**
     * The −1 sweep: voids the swept balls' Observations, then un-completes them.
     * Observations are deleted first so a partial failure leaves a legal state
     * (completed-but-unobserved), never a half-observed ghost.
     */
    suspend fun uncompleteStepsVoidingObservations(
        rangeSessionId: String,
        stepIndices: List<Int>,
    ): RecordingResult<RangeSession>

    /**
     * The +1 / auto-commit — the mirror image of [uncompleteStepsVoidingObservations].
     * Validates, completes [stepIndices], then upserts the ball's Observation.
     *
     * [stepIndices] is the `incrementTargets` output: the next incomplete Ball
     * Step plus any Action Steps swept along before it. The Observation attaches
     * to the single Ball Step among them. When [values] is non-empty it is
     * validated (via `validateObservationWrite`) *before* any write, so a rejected
     * value leaves nothing written. Completion runs before the observation so a
     * partial failure leaves a legal completed-but-unobserved state, never an
     * observation on an uncompleted step.
     *
     * An empty [values] map, or targets with no Ball Step (the "Done" tap arriving
     * here defensively), writes no Observation row at all — byte-identical to never
     * observing (Stage 1 D2).
     */
    suspend fun completeStepsRecordingObservation(
        rangeSessionId: String,
        stepIndices: List<Int>,
        values: Map<String, String>,
    ): RecordingResult<RangeSession>

    /** All Observations for the session, ascending by step index. */
    suspend fun observations(rangeSessionId: String): List<Observation>
}
