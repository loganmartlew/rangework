package com.loganmartlew.rangework.shared.model

/**
 * Pure derived-count computation for a Block. The tally surface is the input
 * surface (design §6.1); these functions feed both the live capture chips/grids
 * and the history summaries.
 *
 * Tally hygiene rule (Stage 2 plan): counts include only observations whose
 * `stepIndex` is a **completed Ball Step of the block**, and only values inside
 * the type's vocabulary. A stray observation row — an orphan from a −1 sweep
 * whose delete half-failed, or an out-of-vocabulary value from version skew — is
 * therefore harmless to every consumer. Deletion is hygiene, not correctness.
 *
 * Empty-record equivalence (Stage 1 D2): a ball with no row and a ball with an
 * `{}` row both read as *unobserved* for every type — they carry no value, so
 * they never enter a denominator.
 */

/** A single Observation Type's distribution across a block's observed balls. */
data class TypeTally(
    /** Balls carrying an in-vocabulary value for this type (the per-type denominator). */
    val observedCount: Int,
    /** Value string → count, over the observed balls. */
    val valueCounts: Map<String, Int>,
)

/**
 * A block's success count, with its provenance made explicit so no consumer ever
 * mixes manual and derived numbers.
 */
sealed interface BlockSuccessCount {
    /** Success Observation Type enabled: hits over *observed* balls (design §4). */
    data class Derived(val hits: Int, val observed: Int) : BlockSuccessCount

    /** No Success type, criterion present, a manual count entered: over the block's total balls. */
    data class Manual(val count: Int, val totalBalls: Int) : BlockSuccessCount

    /** No legitimate success count for this block. */
    data object None : BlockSuccessCount
}

/** The Ball Steps of this block that have been completed, in snapshot order. */
private fun ExecutionBlock.completedBallStepIndices(
    steps: List<SnapshotStep>,
    completedStepIndices: Set<Int>,
): List<Int> = stepIndices.filter { it in completedStepIndices && steps[it].isBallStep }

/**
 * The per-type tally for [type] over this block's completed Ball Steps.
 * Observations are looked up by step index; missing rows, absent keys, and
 * out-of-vocabulary values are all excluded (they carry no value for the type).
 */
fun ExecutionBlock.typeTally(
    steps: List<SnapshotStep>,
    completedStepIndices: Set<Int>,
    observationsByStep: Map<Int, Observation>,
    type: ObservationType,
): TypeTally {
    val valueCounts = mutableMapOf<String, Int>()
    var observed = 0
    for (stepIndex in completedBallStepIndices(steps, completedStepIndices)) {
        val value = observationsByStep[stepIndex]?.value(type) ?: continue
        observed++
        valueCounts[value] = (valueCounts[value] ?: 0) + 1
    }
    return TypeTally(observedCount = observed, valueCounts = valueCounts)
}

/** Tallies for every Observation Type enabled on this block's unit entry. */
fun ExecutionBlock.typeTallies(
    steps: List<SnapshotStep>,
    completedStepIndices: Set<Int>,
    observationsByStep: Map<Int, Observation>,
): Map<ObservationType, TypeTally> =
    unit.enabledObservationTypes.associateWith { type ->
        typeTally(steps, completedStepIndices, observationsByStep, type)
    }

/**
 * The block's success count with provenance, per the design's "one source, never
 * two" rule:
 *
 * - Success enabled → [BlockSuccessCount.Derived] (hits over observed balls),
 *   even at 0 observed; a stray [BlockResult.manualCount] is ignored — derived wins.
 * - Else criterion present and a manual count stored → [BlockSuccessCount.Manual]
 *   (over the block's total balls).
 * - Else [BlockSuccessCount.None].
 */
fun ExecutionBlock.successCount(
    steps: List<SnapshotStep>,
    completedStepIndices: Set<Int>,
    observationsByStep: Map<Int, Observation>,
    blockResult: BlockResult?,
): BlockSuccessCount {
    if (ObservationType.SUCCESS in unit.enabledObservationTypes) {
        val tally = typeTally(steps, completedStepIndices, observationsByStep, ObservationType.SUCCESS)
        val hits = tally.valueCounts[SuccessValue.HIT.id] ?: 0
        return BlockSuccessCount.Derived(hits = hits, observed = tally.observedCount)
    }
    val manualCount = blockResult?.manualCount
    if (unit.successCriterion != null && manualCount != null) {
        return BlockSuccessCount.Manual(count = manualCount, totalBalls = totalBalls(steps))
    }
    return BlockSuccessCount.None
}
