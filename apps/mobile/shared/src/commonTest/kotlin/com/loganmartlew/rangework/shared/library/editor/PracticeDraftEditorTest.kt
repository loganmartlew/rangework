package com.loganmartlew.rangework.shared.library.editor

import com.loganmartlew.rangework.shared.model.ObservationType
import com.loganmartlew.rangework.shared.model.PracticeSessionDraft
import com.loganmartlew.rangework.shared.model.PracticeUnitDraft
import com.loganmartlew.rangework.shared.model.ValidationIssue
import com.loganmartlew.rangework.shared.model.ValidationTarget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class PracticeDraftEditorTest {

    // ── reviewUnit ─────────────────────────────────────────────────────────

    @Test
    fun reviewUnit_validInput_returnsValid() {
        val input = PracticeUnitDraftInput(
            title = "Iron Consistency",
            instructions = listOf(
                PracticeInstructionDraftInput(order = 1, text = "Hit 10 balls at 75%", ballCount = "10"),
            ),
        )
        val result = PracticeDraftEditor.reviewUnit(input)
        assertIs<DraftReview.Valid<PracticeUnitDraft>>(result)
        assertEquals("Iron Consistency", result.draft.title)
    }

    @Test
    fun reviewUnit_blankTitle_placesUnitTitleError() {
        val input = PracticeUnitDraftInput(
            title = "  ",
            instructions = listOf(PracticeInstructionDraftInput(order = 1, text = "Hit balls")),
        )
        val result = PracticeDraftEditor.reviewUnit(input)
        val invalid = assertIs<DraftReview.Invalid<*>>(result)
        assertTrue(invalid.issues.any { it.target == ValidationTarget.UnitTitle })
        val editor = invalid.input as PracticeUnitDraftInput
        assertTrue(editor.titleError != null)
    }

    @Test
    fun reviewUnit_noInstructions_placesUnitInstructionsError() {
        val input = PracticeUnitDraftInput(title = "Solo", instructions = emptyList())
        val result = PracticeDraftEditor.reviewUnit(input)
        val invalid = assertIs<DraftReview.Invalid<*>>(result)
        assertTrue(invalid.issues.any { it.target == ValidationTarget.UnitInstructions })
    }

    @Test
    fun reviewUnit_blankInstructionText_placesInstructionTextError() {
        val input = PracticeUnitDraftInput(
            title = "Valid",
            instructions = listOf(PracticeInstructionDraftInput(order = 1, text = "")),
        )
        val result = PracticeDraftEditor.reviewUnit(input)
        val invalid = assertIs<DraftReview.Invalid<*>>(result)
        assertTrue(invalid.issues.any { it.target == ValidationTarget.InstructionText(0) })
        val editor = invalid.input as PracticeUnitDraftInput
        assertTrue(editor.instructions[0].textError != null)
    }

    @Test
    fun reviewUnit_nonNumericBallCount_parsedAsIssueOnCorrectIndex() {
        val input = PracticeUnitDraftInput(
            title = "Valid",
            instructions = listOf(
                PracticeInstructionDraftInput(order = 1, text = "First", ballCount = "5"),
                PracticeInstructionDraftInput(order = 2, text = "Second", ballCount = "abc"),
            ),
        )
        val result = PracticeDraftEditor.reviewUnit(input)
        val invalid = assertIs<DraftReview.Invalid<*>>(result)
        assertTrue(invalid.issues.any { it.target == ValidationTarget.InstructionBallCount(1) })
        val editor = invalid.input as PracticeUnitDraftInput
        assertNull(editor.instructions[0].ballCountError)
        assertTrue(editor.instructions[1].ballCountError != null)
    }

    @Test
    fun reviewUnit_negativeBallCount_parsedAsIssue() {
        val input = PracticeUnitDraftInput(
            title = "Valid",
            instructions = listOf(PracticeInstructionDraftInput(order = 1, text = "Hit", ballCount = "-3")),
        )
        val result = PracticeDraftEditor.reviewUnit(input)
        val invalid = assertIs<DraftReview.Invalid<*>>(result)
        assertTrue(invalid.issues.any { it.target == ValidationTarget.InstructionBallCount(0) })
    }

    @Test
    fun reviewUnit_emptyBallCount_isValid() {
        val input = PracticeUnitDraftInput(
            title = "Valid",
            instructions = listOf(PracticeInstructionDraftInput(order = 1, text = "Hit", ballCount = "")),
        )
        val result = PracticeDraftEditor.reviewUnit(input)
        assertIs<DraftReview.Valid<*>>(result)
    }

    @Test
    fun reviewUnit_zeroBallCount_isValidAndParsesAsZero() {
        val input = PracticeUnitDraftInput(
            title = "Valid",
            instructions = listOf(PracticeInstructionDraftInput(order = 1, text = "Practice swings", ballCount = "0")),
        )
        val result = PracticeDraftEditor.reviewUnit(input)
        val valid = assertIs<DraftReview.Valid<PracticeUnitDraft>>(result)
        assertEquals(0, valid.draft.instructions[0].ballCount)
    }

    // ── reviewSession ──────────────────────────────────────────────────────

    @Test
    fun reviewSession_validInput_returnsValid() {
        val input = PracticeSessionDraftInput(
            name = "Monday Plan",
            items = listOf(
                PracticeSessionItemDraftInput(order = 1, practiceUnitId = "unit-1", repeatCount = "3"),
            ),
        )
        val result = PracticeDraftEditor.reviewSession(input)
        assertIs<DraftReview.Valid<PracticeSessionDraft>>(result)
        assertEquals("Monday Plan", result.draft.name)
    }

    @Test
    fun reviewSession_blankName_placesSessionNameError() {
        val input = PracticeSessionDraftInput(
            name = "",
            items = listOf(
                PracticeSessionItemDraftInput(order = 1, practiceUnitId = "unit-1", repeatCount = "1"),
            ),
        )
        val result = PracticeDraftEditor.reviewSession(input)
        val invalid = assertIs<DraftReview.Invalid<*>>(result)
        assertTrue(invalid.issues.any { it.target == ValidationTarget.SessionName })
        val editor = invalid.input as PracticeSessionDraftInput
        assertTrue(editor.nameError != null)
    }

    @Test
    fun reviewSession_emptyRepeatCount_parsedAsIssueOnCorrectIndex() {
        val input = PracticeSessionDraftInput(
            name = "Plan",
            items = listOf(
                PracticeSessionItemDraftInput(order = 1, practiceUnitId = "unit-1", repeatCount = "2"),
                PracticeSessionItemDraftInput(order = 2, practiceUnitId = "unit-2", repeatCount = ""),
            ),
        )
        val result = PracticeDraftEditor.reviewSession(input)
        val invalid = assertIs<DraftReview.Invalid<*>>(result)
        assertTrue(invalid.issues.any { it.target == ValidationTarget.ItemRepeatCount(1) })
        val editor = invalid.input as PracticeSessionDraftInput
        assertNull(editor.items[0].repeatCountError)
        assertTrue(editor.items[1].repeatCountError != null)
    }

    @Test
    fun reviewSession_nonNumericRepeatCount_parsedAsIssue() {
        val input = PracticeSessionDraftInput(
            name = "Plan",
            items = listOf(
                PracticeSessionItemDraftInput(order = 1, practiceUnitId = "unit-1", repeatCount = "x"),
            ),
        )
        val result = PracticeDraftEditor.reviewSession(input)
        val invalid = assertIs<DraftReview.Invalid<*>>(result)
        assertTrue(invalid.issues.any { it.target == ValidationTarget.ItemRepeatCount(0) })
    }

    @Test
    fun reviewSession_missingUnitReference_placesItemUnitReferenceError() {
        val input = PracticeSessionDraftInput(
            name = "Plan",
            items = listOf(
                PracticeSessionItemDraftInput(order = 1, practiceUnitId = "", repeatCount = "1"),
            ),
        )
        val result = PracticeDraftEditor.reviewSession(input)
        val invalid = assertIs<DraftReview.Invalid<*>>(result)
        assertTrue(invalid.issues.any { it.target == ValidationTarget.ItemUnitReference(0) })
        val editor = invalid.input as PracticeSessionDraftInput
        assertTrue(editor.items[0].unitError != null)
    }

    @Test
    fun reviewSession_zeroRepeatCount_failsRuleValidation() {
        val input = PracticeSessionDraftInput(
            name = "Plan",
            items = listOf(
                PracticeSessionItemDraftInput(order = 1, practiceUnitId = "unit-1", repeatCount = "0"),
            ),
        )
        val result = PracticeDraftEditor.reviewSession(input)
        val invalid = assertIs<DraftReview.Invalid<*>>(result)
        assertTrue(invalid.issues.any { it.target == ValidationTarget.ItemRepeatCount(0) })
    }

    @Test
    fun reviewSession_multipleItems_indexSpecificErrorsIsolated() {
        val input = PracticeSessionDraftInput(
            name = "Plan",
            items = listOf(
                PracticeSessionItemDraftInput(order = 1, practiceUnitId = "unit-1", repeatCount = "2"),
                PracticeSessionItemDraftInput(order = 2, practiceUnitId = "", repeatCount = "1"),
                PracticeSessionItemDraftInput(order = 3, practiceUnitId = "unit-3", repeatCount = "3"),
            ),
        )
        val result = PracticeDraftEditor.reviewSession(input)
        val invalid = assertIs<DraftReview.Invalid<*>>(result)
        val editor = invalid.input as PracticeSessionDraftInput
        assertNull(editor.items[0].unitError)
        assertTrue(editor.items[1].unitError != null)
        assertNull(editor.items[2].unitError)
    }

    // ── Stage 3 authored fields ────────────────────────────────────────────

    @Test
    fun reviewUnit_successCriterion_carriedIntoDraft() {
        val input = PracticeUnitDraftInput(
            title = "Wedge ladder",
            instructions = listOf(PracticeInstructionDraftInput(order = 1, text = "Hit", ballCount = "5")),
            successCriterion = "Lands inside 3 paces",
        )
        val valid = assertIs<DraftReview.Valid<PracticeUnitDraft>>(PracticeDraftEditor.reviewUnit(input))
        assertEquals("Lands inside 3 paces", valid.draft.successCriterion)
    }

    @Test
    fun reviewSession_observationTypes_carriedIntoDraft() {
        val input = PracticeSessionDraftInput(
            name = "Plan",
            items = listOf(
                PracticeSessionItemDraftInput(
                    order = 1,
                    practiceUnitId = "unit-1",
                    repeatCount = "2",
                    observationTypes = listOf(ObservationType.SHAPE, ObservationType.CONTACT),
                ),
            ),
        )
        val valid = assertIs<DraftReview.Valid<PracticeSessionDraft>>(PracticeDraftEditor.reviewSession(input))
        assertEquals(listOf(ObservationType.SHAPE, ObservationType.CONTACT), valid.draft.items[0].observationTypes)
    }

    @Test
    fun placeSessionErrors_observationTypeIssue_landsOnObservationTypesErrorNotUnitError() {
        val input = PracticeSessionDraftInput(
            name = "Plan",
            items = listOf(
                PracticeSessionItemDraftInput(order = 1, practiceUnitId = "unit-1", repeatCount = "1"),
            ),
        )
        val placed = PracticeDraftEditor.placeSessionErrors(
            input,
            listOf(
                ValidationIssue(
                    target = ValidationTarget.ItemObservationTypes(0),
                    message = "Needs a criterion.",
                ),
            ),
        )
        assertEquals("Needs a criterion.", placed.items[0].observationTypesError)
        assertNull(placed.items[0].unitError)
    }
}
