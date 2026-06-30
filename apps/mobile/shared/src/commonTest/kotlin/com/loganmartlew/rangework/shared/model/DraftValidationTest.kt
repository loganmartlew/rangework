package com.loganmartlew.rangework.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class DraftValidationTest {
    @Test
    fun practiceUnitValidationTrimsFieldsAndReindexesInstructions() {
        val validated = PracticeUnitDraft(
            title = "  Wedge ladder  ",
            instructions = listOf(
                PracticeInstructionDraft(
                    order = 9,
                    text = "  Hit 10 stock wedges  ",
                ),
                PracticeInstructionDraft(
                    order = 3,
                    text = "  Finish with 5 flighted shots ",
                    ballCount = 5,
                ),
            ),
            notes = "  Keep tempo steady  ",
            focus = "  Start line  ",
            defaultClubCode = "  GW  ",
        ).validated()

        assertEquals("Wedge ladder", validated.title)
        assertEquals("Keep tempo steady", validated.notes)
        assertEquals("Start line", validated.focus)
        assertEquals("GW", validated.defaultClubCode)
        assertEquals(listOf(1, 2), validated.instructions.map(PracticeInstructionDraft::order))
        assertEquals("Finish with 5 flighted shots", validated.instructions.first().text)
        assertEquals(5, validated.instructions.first().ballCount)
    }

    @Test
    fun practiceUnitValidationRejectsMissingInstructionText() {
        assertFailsWith<SharedValidationException> {
            PracticeUnitDraft(
                title = "Putting ladder",
                instructions = listOf(
                    PracticeInstructionDraft(
                        order = 1,
                        text = "   ",
                    ),
                ),
            ).validated()
        }
    }

    @Test
    fun practiceSessionValidationAllowsDuplicateUnitReferencesAndNormalizesItems() {
        val validated = PracticeSessionDraft(
            name = "  Scoring session  ",
            notes = "  Alternate tempo work  ",
            items = listOf(
                PracticeSessionItemDraft(
                    practiceUnitId = "  unit-2  ",
                    order = 8,
                    repeatCount = 2,
                    clubCode = "  54*  ",
                    notes = "  Start here  ",
                    focusCue = "  Landing spot  ",
                ),
                PracticeSessionItemDraft(
                    practiceUnitId = "unit-2",
                    order = 2,
                    repeatCount = 1,
                ),
            ),
        ).validated()

        assertEquals("Scoring session", validated.name)
        assertEquals("Alternate tempo work", validated.notes)
        assertEquals(listOf(1, 2), validated.items.map(PracticeSessionItemDraft::order))
        assertEquals(listOf("unit-2", "unit-2"), validated.items.map(PracticeSessionItemDraft::practiceUnitId))
        assertEquals(listOf(1, 2), validated.items.map(PracticeSessionItemDraft::repeatCount))
        assertEquals("54*", validated.items.last().clubCode)
        assertEquals("Start here", validated.items.last().notes)
        assertEquals("Landing spot", validated.items.last().focusCue)
    }

    @Test
    fun practiceUnitValidationIssuesReturnsFieldIdsForBlankTitle() {
        val issues = PracticeUnitDraft(
            title = "   ",
            instructions = listOf(PracticeInstructionDraft(order = 1, text = "Do something")),
        ).validationIssues()

        assertEquals(1, issues.size)
        assertEquals(ValidationTarget.UnitTitle, issues.first().target)
    }

    @Test
    fun practiceUnitValidationIssuesReturnsFieldIdForEmptyInstructions() {
        val issues = PracticeUnitDraft(
            title = "Wedge work",
            instructions = emptyList(),
        ).validationIssues()

        assertEquals(1, issues.size)
        assertEquals(ValidationTarget.UnitInstructions, issues.first().target)
    }

    @Test
    fun practiceUnitValidationIssuesReturnsIndexedFieldIdsForInvalidInstructions() {
        val issues = PracticeUnitDraft(
            title = "Wedge work",
            instructions = listOf(
                PracticeInstructionDraft(order = 1, text = "", ballCount = 0),
            ),
        ).validationIssues()

        val targets = issues.map { it.target }
        assertTrue(targets.contains(ValidationTarget.InstructionText(0))) { "Expected InstructionText(0) in $targets" }
        assertTrue(targets.contains(ValidationTarget.InstructionBallCount(0))) { "Expected InstructionBallCount(0) in $targets" }
    }

    @Test
    fun practiceSessionValidationIssuesReturnsFieldIdForBlankName() {
        val issues = PracticeSessionDraft(
            name = "  ",
            items = listOf(
                PracticeSessionItemDraft(practiceUnitId = "unit-1", order = 1, repeatCount = 1),
            ),
        ).validationIssues()

        assertEquals(1, issues.size)
        assertEquals(ValidationTarget.SessionName, issues.first().target)
    }

    @Test
    fun practiceSessionValidationIssuesReturnsIndexedFieldIdsForMissingUnit() {
        val issues = PracticeSessionDraft(
            name = "My session",
            items = listOf(
                PracticeSessionItemDraft(practiceUnitId = "  ", order = 1, repeatCount = 1),
            ),
        ).validationIssues()

        val targets = issues.map { it.target }
        assertTrue(targets.contains(ValidationTarget.ItemUnitReference(0))) { "Expected ItemUnitReference(0) in $targets" }
    }

    @Test
    fun practiceSessionValidationIssuesReturnsIndexedFieldIdsForNonPositiveCounts() {
        val issues = PracticeSessionDraft(
            name = "My session",
            items = listOf(
                PracticeSessionItemDraft(
                    practiceUnitId = "unit-1",
                    order = 1,
                    repeatCount = 0,
                ),
            ),
        ).validationIssues()

        val targets = issues.map { it.target }
        assertTrue(targets.contains(ValidationTarget.ItemRepeatCount(0))) { "Expected ItemRepeatCount(0) in $targets" }
    }

    @Test
    fun measurementPreferencesValidationAppliesSystemDefaults() {
        assertEquals(
            MeasurementPreferences.Imperial,
            MeasurementPreferences(
                unitSystem = UnitSystem.IMPERIAL,
                distanceUnit = DistanceUnit.METERS,
                speedUnit = SpeedUnit.METRES_PER_SECOND,
            ).validated(),
        )

        assertEquals(
            MeasurementPreferences(
                unitSystem = UnitSystem.CUSTOM,
                distanceUnit = DistanceUnit.METERS,
                speedUnit = SpeedUnit.MILES_PER_HOUR,
            ),
            MeasurementPreferences(
                unitSystem = UnitSystem.CUSTOM,
                distanceUnit = DistanceUnit.METERS,
                speedUnit = SpeedUnit.MILES_PER_HOUR,
            ).validated(),
        )
    }
}
