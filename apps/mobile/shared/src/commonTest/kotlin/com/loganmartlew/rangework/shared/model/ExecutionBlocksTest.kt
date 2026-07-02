package com.loganmartlew.rangework.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ExecutionBlocksTest {

    // ── step classification ──────────────────────────────────────────────────

    @Test
    fun positiveBallCountIsBallStep() {
        assertTrue(ballStep().isBallStep)
        assertTrue(step(ballCount = 20).isBallStep) // v1 coarse step: still a ball tick
    }

    @Test
    fun zeroAndUncountedAreActionSteps() {
        assertFalse(step(ballCount = 0).isBallStep)
        assertFalse(step(ballCount = null).isBallStep)
    }

    // ── executionBlocks ──────────────────────────────────────────────────────

    @Test
    fun executionBlocksGroupStepsByUnitInSnapshotOrder() {
        val snapshot = RangeSessionSnapshot(
            units = listOf(unit("Warm-up"), unit("Driver work")),
            steps = listOf(
                ballStep(unitIndex = 0),
                ballStep(unitIndex = 0),
                ballStep(unitIndex = 1),
            ),
        )
        val blocks = snapshot.executionBlocks()

        assertEquals(2, blocks.size)
        assertEquals(listOf(0, 1), blocks[0].stepIndices)
        assertEquals(listOf(2), blocks[1].stepIndices)
        assertEquals("Warm-up", blocks[0].unit.unitTitle)
    }

    @Test
    fun executionBlocksToleratesUnitWithNoSteps() {
        val snapshot = RangeSessionSnapshot(
            units = listOf(unit("Empty"), unit("Real")),
            steps = listOf(ballStep(unitIndex = 1)),
        )
        val blocks = snapshot.executionBlocks()

        assertEquals(emptyList(), blocks[0].stepIndices)
        assertEquals(listOf(0), blocks[1].stepIndices)
    }

    // ── focus cue and notes precedence ───────────────────────────────────────

    @Test
    fun itemFocusCueOverridesUnitFocus() {
        val block = ExecutionBlock(
            unitIndex = 0,
            unit = unit("U", unitFocus = "long focus", itemFocusCue = "short cue"),
            stepIndices = emptyList(),
        )
        assertEquals("short cue", block.focusCue)
    }

    @Test
    fun blankItemFocusCueFallsBackToUnitFocus() {
        val block = ExecutionBlock(
            unitIndex = 0,
            unit = unit("U", unitFocus = "unit focus", itemFocusCue = " "),
            stepIndices = emptyList(),
        )
        assertEquals("unit focus", block.focusCue)
    }

    @Test
    fun itemNotesOverrideUnitNotes() {
        val block = ExecutionBlock(
            unitIndex = 0,
            unit = unit("U", unitNotes = "unit notes", itemNotes = "item notes"),
            stepIndices = emptyList(),
        )
        assertEquals("item notes", block.notes)
    }

    // ── progress ─────────────────────────────────────────────────────────────

    @Test
    fun progressCountsStepsAndBalls() {
        val steps = listOf(
            step(ballCount = 0),
            ballStep(),
            ballStep(),
            ballStep(),
        )
        val block = blockOf(steps)

        val progress = block.progress(steps, completedStepIndices = setOf(0, 1))

        assertEquals(2, progress.completedSteps)
        assertEquals(4, progress.totalSteps)
        assertEquals(1, progress.completedBalls)
        assertEquals(3, progress.totalBalls)
        assertFalse(progress.isComplete)
        assertFalse(progress.isUntouched)
    }

    @Test
    fun progressDerivesPassFromFirstIncompleteStep() {
        val steps = listOf(
            ballStep(repNumber = 1, totalReps = 3),
            ballStep(repNumber = 2, totalReps = 3),
            ballStep(repNumber = 3, totalReps = 3),
        )
        val block = blockOf(steps, repeatCount = 3)

        assertEquals(1, block.progress(steps, emptySet()).currentPass)
        assertEquals(2, block.progress(steps, setOf(0)).currentPass)
        assertEquals(3, block.progress(steps, setOf(0, 1, 2)).currentPass)
        assertEquals(3, block.progress(steps, setOf(0, 1, 2)).totalPasses)
    }

    @Test
    fun progressIsCompleteOnlyWhenAllStepsComplete() {
        val steps = listOf(ballStep(), ballStep())
        val block = blockOf(steps)

        assertFalse(block.progress(steps, setOf(0)).isComplete)
        assertTrue(block.progress(steps, setOf(0, 1)).isComplete)
        assertTrue(block.progress(steps, emptySet()).isUntouched)
    }

    // ── incrementTargets ─────────────────────────────────────────────────────

    @Test
    fun incrementCompletesNextBallStep() {
        val steps = listOf(ballStep(), ballStep(), ballStep())
        val block = blockOf(steps)

        assertEquals(listOf(0), block.incrementTargets(steps, emptySet()))
        assertEquals(listOf(1), block.incrementTargets(steps, setOf(0)))
    }

    @Test
    fun incrementSweepsPrecedingActionSteps() {
        // Rehearse (0 balls), then hit — the classic per-ball drill pass.
        val steps = listOf(
            step(ballCount = 0),
            ballStep(),
            step(ballCount = 0),
            ballStep(),
        )
        val block = blockOf(steps)

        assertEquals(listOf(0, 1), block.incrementTargets(steps, emptySet()))
        assertEquals(listOf(2, 3), block.incrementTargets(steps, setOf(0, 1)))
    }

    @Test
    fun incrementReturnsRemainingActionStepsWhenNoBallStepsLeft() {
        val steps = listOf(ballStep(), step(ballCount = 0), step(ballCount = null))
        val block = blockOf(steps)

        // Only trailing Action Steps remain → the "Done" affordance.
        assertEquals(listOf(1, 2), block.incrementTargets(steps, setOf(0)))
    }

    @Test
    fun incrementReturnsEmptyWhenBlockComplete() {
        val steps = listOf(ballStep(), ballStep())
        val block = blockOf(steps)

        assertEquals(emptyList(), block.incrementTargets(steps, setOf(0, 1)))
    }

    @Test
    fun incrementIgnoresOtherBlocksSteps() {
        val steps = listOf(
            ballStep(unitIndex = 0),
            ballStep(unitIndex = 1),
            ballStep(unitIndex = 1),
        )
        val block = ExecutionBlock(unitIndex = 1, unit = unit("U"), stepIndices = listOf(1, 2))

        assertEquals(listOf(1), block.incrementTargets(steps, emptySet()))
    }

    // ── decrementTargets ─────────────────────────────────────────────────────

    @Test
    fun decrementUncompletesLastCompletedBallStep() {
        val steps = listOf(ballStep(), ballStep(), ballStep())
        val block = blockOf(steps)

        assertEquals(listOf(1), block.decrementTargets(steps, setOf(0, 1)))
    }

    @Test
    fun decrementIsExactInverseOfSweepingIncrement() {
        val steps = listOf(
            step(ballCount = 0),
            ballStep(),
            step(ballCount = 0),
            ballStep(),
        )
        val block = blockOf(steps)

        val afterTwoTaps = setOf(0, 1, 2, 3)
        assertEquals(listOf(2, 3), block.decrementTargets(steps, afterTwoTaps))
        assertEquals(listOf(0, 1), block.decrementTargets(steps, setOf(0, 1)))
    }

    @Test
    fun decrementUncompletesTrailingActionSegmentFirst() {
        // Ball, ball, then trailing action steps completed by "Done".
        val steps = listOf(ballStep(), ballStep(), step(ballCount = 0))
        val block = blockOf(steps)

        // Inverse of the "Done" tap: reopen the trailing Action Step only.
        assertEquals(listOf(2), block.decrementTargets(steps, setOf(0, 1, 2)))
        // Then the inverse of the last "+1".
        assertEquals(listOf(1), block.decrementTargets(steps, setOf(0, 1)))
    }

    @Test
    fun decrementReturnsEmptyWhenNothingCompleted() {
        val steps = listOf(ballStep(), ballStep())
        val block = blockOf(steps)

        assertEquals(emptyList(), block.decrementTargets(steps, emptySet()))
    }

    // ── hasIncompleteBallSteps ───────────────────────────────────────────────

    @Test
    fun hasIncompleteBallStepsDrivesButtonMorph() {
        val steps = listOf(ballStep(), step(ballCount = 0))
        val block = blockOf(steps)

        assertTrue(block.hasIncompleteBallSteps(steps, emptySet()))
        assertFalse(block.hasIncompleteBallSteps(steps, setOf(0)))
    }

    // ── clubSwapTargets ──────────────────────────────────────────────────────

    @Test
    fun clubSwapTargetsRemainingIncompleteStepsOfInstruction() {
        val steps = listOf(
            ballStep(instructionIndex = 0),
            ballStep(instructionIndex = 0),
            ballStep(instructionIndex = 1),
            ballStep(instructionIndex = 0, repNumber = 2, totalReps = 2),
        )
        val block = blockOf(steps, repeatCount = 2)

        // Step 0 already hit — it keeps its club; only 1 and 3 are swapped.
        assertEquals(
            listOf(1, 3),
            block.clubSwapTargets(steps, setOf(0), instructionIndex = 0),
        )
    }

    // ── firstIncompleteBlockIndex ────────────────────────────────────────────

    @Test
    fun landsOnFirstIncompleteBlock() {
        val snapshot = RangeSessionSnapshot(
            units = listOf(unit("A"), unit("B")),
            steps = listOf(ballStep(unitIndex = 0), ballStep(unitIndex = 1)),
        )
        val blocks = snapshot.executionBlocks()

        assertEquals(0, firstIncompleteBlockIndex(blocks, emptySet()))
        assertEquals(1, firstIncompleteBlockIndex(blocks, setOf(0)))
    }

    @Test
    fun landsOnLastBlockWhenAllComplete() {
        val snapshot = RangeSessionSnapshot(
            units = listOf(unit("A"), unit("B")),
            steps = listOf(ballStep(unitIndex = 0), ballStep(unitIndex = 1)),
        )
        val blocks = snapshot.executionBlocks()

        assertEquals(1, firstIncompleteBlockIndex(blocks, setOf(0, 1)))
    }
}

// ── fixtures ─────────────────────────────────────────────────────────────────

private fun unit(
    title: String,
    unitNotes: String? = null,
    unitFocus: String? = null,
    itemNotes: String? = null,
    itemFocusCue: String? = null,
    repeatCount: Int = 1,
): SnapshotUnit = SnapshotUnit(
    unitTitle = title,
    unitNotes = unitNotes,
    unitFocus = unitFocus,
    itemNotes = itemNotes,
    itemFocusCue = itemFocusCue,
    repeatCount = repeatCount,
    instructions = emptyList(),
)

private fun step(
    ballCount: Int?,
    unitIndex: Int = 0,
    instructionIndex: Int = 0,
    repNumber: Int = 1,
    totalReps: Int = 1,
): SnapshotStep = SnapshotStep(
    unitIndex = unitIndex,
    instructionIndex = instructionIndex,
    repNumber = repNumber,
    totalReps = totalReps,
    instructionText = "Instruction",
    ballCount = ballCount,
    unitTitle = "Unit",
)

private fun ballStep(
    unitIndex: Int = 0,
    instructionIndex: Int = 0,
    repNumber: Int = 1,
    totalReps: Int = 1,
): SnapshotStep = step(
    ballCount = 1,
    unitIndex = unitIndex,
    instructionIndex = instructionIndex,
    repNumber = repNumber,
    totalReps = totalReps,
)

private fun blockOf(steps: List<SnapshotStep>, repeatCount: Int = 1): ExecutionBlock =
    ExecutionBlock(
        unitIndex = 0,
        unit = unit("Unit", repeatCount = repeatCount),
        stepIndices = steps.indices.toList(),
    )
