package com.loganmartlew.rangework.shared.model

/**
 * The design's load-bearing capture invariants as pure, testable predicates: the
 * freeze matrix, manual-count validation, and observation validation. These live
 * here — not in UI conditionals — so the recorder (and any future surface) shares
 * one enforcement point. Everything additionally gates on `supportsDataCapture`
 * (snapshot v3): v1/v2 sessions take no notes, results, or observations.
 */

/** The mutability lifecycle of a Range Session. */
enum class RangeSessionState {
    ACTIVE,
    COMPLETED,
    ABANDONED,
}

val RangeSession.state: RangeSessionState
    get() = when {
        abandonedAt != null -> RangeSessionState.ABANDONED
        completedAt != null -> RangeSessionState.COMPLETED
        else -> RangeSessionState.ACTIVE
    }

// ── Freeze matrix (design "prose is reflection, counts are data") ─────────────
//
//                Session note | Block note | Manual count | Observations
//   Active            ✔       |     ✔      |      ✔       |      ✔
//   Completed         ✔       |     ✔      |      ✘       |      ✘
//   Abandoned         ✘       |     ✘      |      ✘       |      ✘
//
// All four additionally gated on snapshotVersion >= 3.

/** Prose stays editable after Completion; only Abandoned locks it. */
val RangeSession.canEditSessionNote: Boolean
    get() = supportsDataCapture && state != RangeSessionState.ABANDONED

val RangeSession.canEditBlockNote: Boolean
    get() = supportsDataCapture && state != RangeSessionState.ABANDONED

/** Counts are data: frozen the moment the session leaves Active. */
val RangeSession.canEditManualCount: Boolean
    get() = supportsDataCapture && state == RangeSessionState.ACTIVE

val RangeSession.canEditObservations: Boolean
    get() = supportsDataCapture && state == RangeSessionState.ACTIVE

// ── Rejection reasons ─────────────────────────────────────────────────────────

/** Why a recording write was refused. `null` from a validation function = accepted. */
enum class RecordingRejection {
    /** Snapshot predates v3 — the feature does not apply. */
    UnsupportedSnapshot,

    /** The session's state forbids this edit (Completed count/observation, or Abandoned anything). */
    SessionFrozen,

    /** unitIndex does not address a block in this snapshot. */
    UnknownBlock,

    /** stepIndex is out of range for this snapshot. */
    UnknownStep,

    /** A manual count was set on a block whose unit has no Success Criterion. */
    MissingSuccessCriterion,

    /** A manual count was set on a block whose Session Item enabled the Success type. */
    SuccessTypeEnabled,

    /** The block has no Ball Steps, so a manual count is meaningless. */
    NoBallSteps,

    /** The manual count is outside `0..totalBalls`. */
    CountOutOfRange,

    /** An observation targeted an Action Step (no ball, nothing to observe). */
    NotABallStep,

    /** An observation carried a key not enabled on the step's unit entry. */
    ObservationTypeNotEnabled,

    /** An observation carried a value outside its type's vocabulary. */
    ValueOutOfVocabulary,
}

private fun RangeSession.blockAt(unitIndex: Int): ExecutionBlock? =
    snapshot.executionBlocks().getOrNull(unitIndex)

// ── Validation ────────────────────────────────────────────────────────────────

/**
 * The support + freeze gate shared by every Observation edit — recording a value,
 * voiding, and the −1 sweep. One predicate so the write path and the void/sweep
 * path can never disagree about when observations are editable.
 */
fun RangeSession.validateObservationEdit(): RecordingRejection? = when {
    !supportsDataCapture -> RecordingRejection.UnsupportedSnapshot
    !canEditObservations -> RecordingRejection.SessionFrozen
    else -> null
}

/** Session Note edit: supported snapshot, and not Abandoned. */
fun RangeSession.validateSessionNoteEdit(): RecordingRejection? = when {
    !supportsDataCapture -> RecordingRejection.UnsupportedSnapshot
    !canEditSessionNote -> RecordingRejection.SessionFrozen
    else -> null
}

/** Block Note edit: supported snapshot, existing block, and not Abandoned. */
fun RangeSession.validateBlockNoteEdit(unitIndex: Int): RecordingRejection? = when {
    !supportsDataCapture -> RecordingRejection.UnsupportedSnapshot
    !canEditBlockNote -> RecordingRejection.SessionFrozen
    blockAt(unitIndex) == null -> RecordingRejection.UnknownBlock
    else -> null
}

/**
 * Manual count edit. [count] `null` clears the count (still a count edit, so it
 * is subject to the same freeze). A non-null count requires a criterion, no
 * Success type, a ball-bearing block, and `count in 0..totalBalls`.
 */
fun RangeSession.validateManualCountEdit(unitIndex: Int, count: Int?): RecordingRejection? {
    if (!supportsDataCapture) return RecordingRejection.UnsupportedSnapshot
    if (!canEditManualCount) return RecordingRejection.SessionFrozen
    val block = blockAt(unitIndex) ?: return RecordingRejection.UnknownBlock
    // Clearing the count (null) is a count edit subject only to the freeze matrix,
    // so it stays legal even where the structural preconditions no longer hold —
    // e.g. clearing a stray count left by bad data on a success-enabled block.
    if (count == null) return null
    if (block.unit.successCriterion == null) return RecordingRejection.MissingSuccessCriterion
    if (ObservationType.SUCCESS in block.unit.enabledObservationTypes) {
        return RecordingRejection.SuccessTypeEnabled
    }
    val totalBalls = block.totalBalls(snapshot.steps)
    if (totalBalls == 0) return RecordingRejection.NoBallSteps
    if (count !in 0..totalBalls) return RecordingRejection.CountOutOfRange
    return null
}

/**
 * Observation write. The step must be a Ball Step; every key must be an
 * Observation Type enabled on that step's unit entry; every value must be in its
 * type's vocabulary. An empty [values] map is legal (the bare +1 commit).
 */
fun RangeSession.validateObservationWrite(stepIndex: Int, values: Map<String, String>): RecordingRejection? {
    validateObservationEdit()?.let { return it }
    val step = snapshot.steps.getOrNull(stepIndex) ?: return RecordingRejection.UnknownStep
    if (!step.isBallStep) return RecordingRejection.NotABallStep
    val enabled = blockAt(step.unitIndex)?.unit?.enabledObservationTypes.orEmpty()
    val enabledIds = enabled.map(ObservationType::id).toSet()
    for ((key, value) in values) {
        if (key !in enabledIds) return RecordingRejection.ObservationTypeNotEnabled
        val type = ObservationType.fromId(key) ?: return RecordingRejection.ObservationTypeNotEnabled
        if (!type.accepts(value)) return RecordingRejection.ValueOutOfVocabulary
    }
    return null
}
