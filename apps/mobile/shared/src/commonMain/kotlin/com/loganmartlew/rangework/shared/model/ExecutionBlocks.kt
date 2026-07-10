package com.loganmartlew.rangework.shared.model

/**
 * Execution-layer view of a Range Session: Blocks, Ball/Action Step
 * classification, and the ball-counter increment/decrement rules.
 * See apps/mobile/CONTEXT.md (Block, Ball Step, Action Step) and ADR 0004.
 */

/**
 * A Step that consumes at least one ball. Version-2 snapshots emit Ball Steps
 * with ballCount 1; version-1 snapshots may carry a larger count on a single
 * step, which renders as one tick worth that many balls.
 */
val SnapshotStep.isBallStep: Boolean
    get() = (ballCount ?: 0) > 0

/**
 * The live view of one Session Item within a Range Session: the grouping of
 * that item's Steps, addressed by their global snapshot indices.
 */
data class ExecutionBlock(
    val unitIndex: Int,
    val unit: SnapshotUnit,
    /** Global snapshot step indices belonging to this block, in snapshot order. */
    val stepIndices: List<Int>,
) {
    /** The Focus Cue shown on the block screen (Session Item override wins). */
    val focusCue: String?
        get() = unit.itemFocusCue?.takeIf(String::isNotBlank)
            ?: unit.unitFocus?.takeIf(String::isNotBlank)

    /** The notes shown on the block screen (Session Item override wins). */
    val notes: String?
        get() = unit.itemNotes?.takeIf(String::isNotBlank)
            ?: unit.unitNotes?.takeIf(String::isNotBlank)
}

fun RangeSessionSnapshot.executionBlocks(): List<ExecutionBlock> {
    val indicesByUnit = steps.withIndex().groupBy({ it.value.unitIndex }, { it.index })
    return units.mapIndexed { unitIndex, unit ->
        ExecutionBlock(
            unitIndex = unitIndex,
            unit = unit,
            stepIndices = indicesByUnit[unitIndex].orEmpty(),
        )
    }
}

/** Total balls addressable by this block: every Ball Step's count, completed or not. */
fun ExecutionBlock.totalBalls(steps: List<SnapshotStep>): Int =
    stepIndices.sumOf { steps[it].ballCount ?: 0 }

data class BlockProgress(
    val completedSteps: Int,
    val totalSteps: Int,
    val completedBalls: Int,
    val totalBalls: Int,
    /** Derived pass position: repNumber of the first incomplete step. */
    val currentPass: Int,
    val totalPasses: Int,
) {
    val isComplete: Boolean get() = totalSteps > 0 && completedSteps == totalSteps
    val isUntouched: Boolean get() = completedSteps == 0
}

fun ExecutionBlock.progress(
    steps: List<SnapshotStep>,
    completedStepIndices: Set<Int>,
): BlockProgress {
    val completed = stepIndices.filter { it in completedStepIndices }
    val firstIncomplete = stepIndices.firstOrNull { it !in completedStepIndices }
    return BlockProgress(
        completedSteps = completed.size,
        totalSteps = stepIndices.size,
        completedBalls = completed.sumOf { steps[it].ballCount ?: 0 },
        totalBalls = totalBalls(steps),
        currentPass = firstIncomplete?.let { steps[it].repNumber } ?: unit.repeatCount,
        totalPasses = unit.repeatCount,
    )
}

/**
 * Step indices a single "+1" tap completes: the next incomplete Ball Step in
 * snapshot order, sweeping along the incomplete Action Steps before it. When
 * no incomplete Ball Steps remain, all remaining (Action) steps — the "Done"
 * affordance. Empty when the block is already complete.
 */
fun ExecutionBlock.incrementTargets(
    steps: List<SnapshotStep>,
    completedStepIndices: Set<Int>,
): List<Int> {
    val incomplete = stepIndices.filter { it !in completedStepIndices }
    if (incomplete.isEmpty()) return emptyList()
    val nextBall = incomplete.firstOrNull { steps[it].isBallStep }
        ?: return incomplete
    return incomplete.filter { it <= nextBall }
}

/**
 * Step indices a single "−1" tap un-completes: the exact inverse of the tap
 * that completed the block's most recent segment. A segment is the span
 * ending at a Ball Step (Action Steps travel with the Ball Step that follows
 * them); Action Steps after the last Ball Step form a trailing segment (the
 * "Done" tap). Un-completes the completed members of the segment holding the
 * last completed step. Empty when nothing in the block is completed.
 */
fun ExecutionBlock.decrementTargets(
    steps: List<SnapshotStep>,
    completedStepIndices: Set<Int>,
): List<Int> {
    val lastCompleted = stepIndices.lastOrNull { it in completedStepIndices }
        ?: return emptyList()
    val segment = mutableListOf<Int>()
    for (index in stepIndices) {
        segment += index
        if (steps[index].isBallStep) {
            if (lastCompleted in segment) break
            segment.clear()
        }
    }
    return segment.filter { it in completedStepIndices }
}

/** True when the block still has incomplete Ball Steps (counter shows "+1"). */
fun ExecutionBlock.hasIncompleteBallSteps(
    steps: List<SnapshotStep>,
    completedStepIndices: Set<Int>,
): Boolean = stepIndices.any { it !in completedStepIndices && steps[it].isBallStep }

/**
 * Steps a club swap gesture applies to: the given instruction's remaining
 * incomplete steps in this block. Completed steps keep the club they were
 * actually hit with (see CONTEXT.md, Club Override).
 */
fun ExecutionBlock.clubSwapTargets(
    steps: List<SnapshotStep>,
    completedStepIndices: Set<Int>,
    instructionIndex: Int,
): List<Int> = stepIndices.filter {
    steps[it].instructionIndex == instructionIndex && it !in completedStepIndices
}

/**
 * The block a freshly opened session lands on: the first with an incomplete
 * step, or the last block when everything is complete.
 */
fun firstIncompleteBlockIndex(
    blocks: List<ExecutionBlock>,
    completedStepIndices: Set<Int>,
): Int {
    if (blocks.isEmpty()) return 0
    val first = blocks.indexOfFirst { block ->
        block.stepIndices.any { it !in completedStepIndices }
    }
    return if (first >= 0) first else blocks.lastIndex
}
