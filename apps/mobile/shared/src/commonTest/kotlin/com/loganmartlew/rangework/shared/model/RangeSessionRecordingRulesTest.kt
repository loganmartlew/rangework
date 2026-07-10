package com.loganmartlew.rangework.shared.model

import kotlinx.datetime.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class RangeSessionRecordingRulesTest {

    private val started = Instant.parse("2026-07-09T10:00:00Z")
    private val later = Instant.parse("2026-07-09T11:00:00Z")

    private fun ballStep() = SnapshotStep(
        unitIndex = 0,
        instructionIndex = 0,
        repNumber = 1,
        totalReps = 1,
        instructionText = "Hit",
        ballCount = 1,
        unitTitle = "Unit",
    )

    private fun actionStep() = SnapshotStep(
        unitIndex = 0,
        instructionIndex = 0,
        repNumber = 1,
        totalReps = 1,
        instructionText = "Rehearse",
        ballCount = null,
        unitTitle = "Unit",
    )

    private fun session(
        state: RangeSessionState = RangeSessionState.ACTIVE,
        version: Int = 3,
        successCriterion: String? = "inside 5m",
        observationTypes: List<String> = listOf("shape"),
        steps: List<SnapshotStep> = listOf(actionStep(), ballStep(), ballStep()),
    ) = RangeSession(
        id = "rs-1",
        sessionName = "Session",
        snapshot = RangeSessionSnapshot(
            units = listOf(
                SnapshotUnit(
                    unitTitle = "Unit",
                    repeatCount = 1,
                    instructions = emptyList(),
                    successCriterion = successCriterion,
                    observationTypes = observationTypes,
                ),
            ),
            steps = steps,
        ),
        snapshotVersion = version,
        completedSteps = emptyList(),
        clubOverrides = emptyMap(),
        startedAt = started,
        completedAt = if (state == RangeSessionState.COMPLETED) later else null,
        abandonedAt = if (state == RangeSessionState.ABANDONED) later else null,
    )

    // ── Freeze matrix (3 states × 4 field kinds) ────────────────────────────────

    @Test
    fun freezeMatrixActive() {
        val s = session(RangeSessionState.ACTIVE)
        assertTrue(s.canEditSessionNote)
        assertTrue(s.canEditBlockNote)
        assertTrue(s.canEditManualCount)
        assertTrue(s.canEditObservations)
    }

    @Test
    fun freezeMatrixCompleted() {
        val s = session(RangeSessionState.COMPLETED)
        assertTrue(s.canEditSessionNote)
        assertTrue(s.canEditBlockNote)
        assertFalse(s.canEditManualCount)
        assertFalse(s.canEditObservations)
    }

    @Test
    fun freezeMatrixAbandoned() {
        val s = session(RangeSessionState.ABANDONED)
        assertFalse(s.canEditSessionNote)
        assertFalse(s.canEditBlockNote)
        assertFalse(s.canEditManualCount)
        assertFalse(s.canEditObservations)
    }

    @Test
    fun allFieldsGatedOnV3() {
        val s = session(RangeSessionState.ACTIVE, version = 2)
        assertFalse(s.supportsDataCapture)
        assertFalse(s.canEditSessionNote)
        assertFalse(s.canEditBlockNote)
        assertFalse(s.canEditManualCount)
        assertFalse(s.canEditObservations)
    }

    // ── Manual count validation ─────────────────────────────────────────────────

    @Test
    fun manualCountHappyPathAndNullClear() {
        val s = session(observationTypes = emptyList())
        assertNull(s.validateManualCountEdit(0, 2))
        assertNull(s.validateManualCountEdit(0, 0))
        assertNull(s.validateManualCountEdit(0, null))
    }

    @Test
    fun nullManualCountClearsRegardlessOfStructuralPreconditions() {
        // Clearing is a count edit subject only to the freeze matrix, so a null
        // count is accepted even where a non-null count would be rejected — e.g.
        // clearing a stray count on a success-enabled or criterion-less block.
        assertNull(session(observationTypes = listOf("success")).validateManualCountEdit(0, null))
        assertNull(session(successCriterion = null, observationTypes = emptyList()).validateManualCountEdit(0, null))
        assertNull(session(observationTypes = emptyList(), steps = listOf(actionStep())).validateManualCountEdit(0, null))
    }

    @Test
    fun nullManualCountStillRejectedWhenFrozenOrUnknownBlock() {
        assertEquals(
            RecordingRejection.SessionFrozen,
            session(RangeSessionState.COMPLETED, observationTypes = emptyList()).validateManualCountEdit(0, null),
        )
        assertEquals(
            RecordingRejection.UnknownBlock,
            session(observationTypes = emptyList()).validateManualCountEdit(5, null),
        )
    }

    @Test
    fun manualCountRejectedWithoutCriterion() {
        val s = session(successCriterion = null, observationTypes = emptyList())
        assertEquals(RecordingRejection.MissingSuccessCriterion, s.validateManualCountEdit(0, 1))
    }

    @Test
    fun manualCountRejectedWhenSuccessEnabled() {
        val s = session(observationTypes = listOf("success"))
        assertEquals(RecordingRejection.SuccessTypeEnabled, s.validateManualCountEdit(0, 1))
    }

    @Test
    fun manualCountRejectedOutOfRange() {
        val s = session(observationTypes = emptyList())
        // Two ball steps → totalBalls = 2.
        assertEquals(RecordingRejection.CountOutOfRange, s.validateManualCountEdit(0, 3))
        assertEquals(RecordingRejection.CountOutOfRange, s.validateManualCountEdit(0, -1))
    }

    @Test
    fun manualCountRejectedOnActionOnlyBlock() {
        val s = session(observationTypes = emptyList(), steps = listOf(actionStep()))
        assertEquals(RecordingRejection.NoBallSteps, s.validateManualCountEdit(0, 0))
    }

    @Test
    fun manualCountRejectedWhenFrozen() {
        val completed = session(RangeSessionState.COMPLETED, observationTypes = emptyList())
        assertEquals(RecordingRejection.SessionFrozen, completed.validateManualCountEdit(0, 1))
    }

    @Test
    fun manualCountRejectedOnUnknownBlock() {
        val s = session(observationTypes = emptyList())
        assertEquals(RecordingRejection.UnknownBlock, s.validateManualCountEdit(5, 1))
    }

    // ── Observation validation ──────────────────────────────────────────────────

    @Test
    fun observationHappyPathIncludingEmptyMap() {
        val s = session(observationTypes = listOf("shape"))
        assertNull(s.validateObservationWrite(1, mapOf("shape" to "straight_left")))
        assertNull(s.validateObservationWrite(1, emptyMap()))
    }

    @Test
    fun observationRejectedOnActionStep() {
        val s = session(observationTypes = listOf("shape"))
        // Step 0 is the action step.
        assertEquals(RecordingRejection.NotABallStep, s.validateObservationWrite(0, mapOf("shape" to "straight_left")))
    }

    @Test
    fun observationRejectedForTypeNotEnabled() {
        val s = session(observationTypes = listOf("shape"))
        assertEquals(RecordingRejection.ObservationTypeNotEnabled, s.validateObservationWrite(1, mapOf("contact" to "flush")))
    }

    @Test
    fun observationRejectedForOutOfVocabularyValue() {
        val s = session(observationTypes = listOf("shape"))
        assertEquals(RecordingRejection.ValueOutOfVocabulary, s.validateObservationWrite(1, mapOf("shape" to "banana")))
    }

    @Test
    fun observationRejectedWhenFrozen() {
        val completed = session(RangeSessionState.COMPLETED, observationTypes = listOf("shape"))
        assertEquals(RecordingRejection.SessionFrozen, completed.validateObservationWrite(1, mapOf("shape" to "straight_left")))
    }

    @Test
    fun observationRejectedOnUnknownStep() {
        val s = session(observationTypes = listOf("shape"))
        assertEquals(RecordingRejection.UnknownStep, s.validateObservationWrite(99, emptyMap()))
    }

    @Test
    fun stateDerivation() {
        assertEquals(RangeSessionState.ACTIVE, session(RangeSessionState.ACTIVE).state)
        assertEquals(RangeSessionState.COMPLETED, session(RangeSessionState.COMPLETED).state)
        assertEquals(RangeSessionState.ABANDONED, session(RangeSessionState.ABANDONED).state)
    }
}
