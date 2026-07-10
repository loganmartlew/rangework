package com.loganmartlew.rangework.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ObservationTalliesTest {

    private fun ballStep(unitIndex: Int) = SnapshotStep(
        unitIndex = unitIndex,
        instructionIndex = 0,
        repNumber = 1,
        totalReps = 1,
        instructionText = "Hit",
        ballCount = 1,
        unitTitle = "Unit",
    )

    private fun actionStep(unitIndex: Int) = SnapshotStep(
        unitIndex = unitIndex,
        instructionIndex = 0,
        repNumber = 1,
        totalReps = 1,
        instructionText = "Rehearse",
        ballCount = null,
        unitTitle = "Unit",
    )

    /**
     * One block: an action step then five ball steps (indices 1..5), enabling
     * Shape + Success against a criterion.
     */
    private fun singleBlockSnapshot() = RangeSessionSnapshot(
        units = listOf(
            SnapshotUnit(
                unitTitle = "Wedge ladder",
                repeatCount = 1,
                instructions = emptyList(),
                successCriterion = "inside 5m of the flag",
                observationTypes = listOf("shape", "success"),
            ),
        ),
        steps = listOf(
            actionStep(0),
            ballStep(0),
            ballStep(0),
            ballStep(0),
            ballStep(0),
            ballStep(0),
        ),
    )

    @Test
    fun perTypeDenominatorsExcludeIncompleteEmptyAndOutOfVocabulary() {
        val snapshot = singleBlockSnapshot()
        val block = snapshot.executionBlocks().first()
        // Steps 1..4 completed; step 5 left incomplete.
        val completed = setOf(0, 1, 2, 3, 4)
        val observations = mapOf(
            1 to Observation(1, mapOf("shape" to "straight_left", "success" to "hit")),
            2 to Observation(2, mapOf("shape" to "straight_right", "success" to "miss")),
            3 to Observation(3, mapOf("shape" to "straight_left", "success" to "hit")),
            // Out-of-vocabulary shape (excluded); valid success (counted).
            4 to Observation(4, mapOf("shape" to "banana", "success" to "hit")),
            // Incomplete step — excluded entirely.
            5 to Observation(5, mapOf("shape" to "straight_left", "success" to "hit")),
            // Stray row outside the block — excluded.
            99 to Observation(99, mapOf("shape" to "straight_left")),
        )

        val shape = block.typeTally(snapshot.steps, completed, observations, ObservationType.SHAPE)
        assertEquals(3, shape.observedCount)
        assertEquals(mapOf("straight_left" to 2, "straight_right" to 1), shape.valueCounts)

        val success = block.typeTally(snapshot.steps, completed, observations, ObservationType.SUCCESS)
        assertEquals(4, success.observedCount)
        assertEquals(mapOf("hit" to 3, "miss" to 1), success.valueCounts)
    }

    @Test
    fun emptyRecordCountsAsUnobserved() {
        val snapshot = singleBlockSnapshot()
        val block = snapshot.executionBlocks().first()
        val completed = setOf(1, 2)
        val observations = mapOf(
            1 to Observation(1, emptyMap()),
            2 to Observation(2, mapOf("success" to "hit")),
        )
        val success = block.typeTally(snapshot.steps, completed, observations, ObservationType.SUCCESS)
        assertEquals(1, success.observedCount)
    }

    @Test
    fun successProvenanceIsDerivedWhenEnabled() {
        val snapshot = singleBlockSnapshot()
        val block = snapshot.executionBlocks().first()
        val completed = setOf(1, 2, 3, 4)
        val observations = mapOf(
            1 to Observation(1, mapOf("success" to "hit")),
            2 to Observation(2, mapOf("success" to "miss")),
            3 to Observation(3, mapOf("success" to "hit")),
            4 to Observation(4, mapOf("shape" to "banana", "success" to "hit")),
        )
        val count = block.successCount(snapshot.steps, completed, observations, blockResult = BlockResult(manualCount = 2))
        // Derived wins even though a stray manual count is present.
        assertEquals(BlockSuccessCount.Derived(hits = 3, observed = 4), count)
    }

    @Test
    fun derivedIsHonestAtZeroObserved() {
        val snapshot = singleBlockSnapshot()
        val block = snapshot.executionBlocks().first()
        val count = block.successCount(snapshot.steps, completedStepIndices = emptySet(), observationsByStep = emptyMap(), blockResult = null)
        assertEquals(BlockSuccessCount.Derived(hits = 0, observed = 0), count)
    }

    @Test
    fun manualProvenanceWhenCriterionPresentAndSuccessDisabled() {
        val snapshot = RangeSessionSnapshot(
            units = listOf(
                SnapshotUnit(
                    unitTitle = "Wedge ladder",
                    repeatCount = 1,
                    instructions = emptyList(),
                    successCriterion = "inside 5m",
                    observationTypes = listOf("shape"),
                ),
            ),
            steps = listOf(ballStep(0), ballStep(0), ballStep(0)),
        )
        val block = snapshot.executionBlocks().first()
        val count = block.successCount(snapshot.steps, setOf(0, 1), emptyMap(), BlockResult(manualCount = 2))
        assertEquals(BlockSuccessCount.Manual(count = 2, totalBalls = 3), count)
    }

    @Test
    fun noneWhenNeitherSuccessNorManualCount() {
        val snapshot = RangeSessionSnapshot(
            units = listOf(
                SnapshotUnit(
                    unitTitle = "Wedge ladder",
                    repeatCount = 1,
                    instructions = emptyList(),
                    successCriterion = "inside 5m",
                    observationTypes = emptyList(),
                ),
            ),
            steps = listOf(ballStep(0)),
        )
        val block = snapshot.executionBlocks().first()
        assertEquals(BlockSuccessCount.None, block.successCount(snapshot.steps, setOf(0), emptyMap(), null))
    }

    @Test
    fun sameUnitInTwoItemsTalliesPerBlock() {
        val unit = SnapshotUnit(
            unitTitle = "Wedge ladder",
            repeatCount = 1,
            instructions = emptyList(),
            successCriterion = "inside 5m",
            observationTypes = listOf("success"),
        )
        val snapshot = RangeSessionSnapshot(
            units = listOf(unit, unit),
            steps = listOf(ballStep(0), ballStep(1)),
        )
        val blocks = snapshot.executionBlocks()
        val completed = setOf(0, 1)
        val observations = mapOf(
            0 to Observation(0, mapOf("success" to "hit")),
            1 to Observation(1, mapOf("success" to "miss")),
        )
        assertEquals(
            BlockSuccessCount.Derived(hits = 1, observed = 1),
            blocks[0].successCount(snapshot.steps, completed, observations, null),
        )
        assertEquals(
            BlockSuccessCount.Derived(hits = 0, observed = 1),
            blocks[1].successCount(snapshot.steps, completed, observations, null),
        )
    }
}
