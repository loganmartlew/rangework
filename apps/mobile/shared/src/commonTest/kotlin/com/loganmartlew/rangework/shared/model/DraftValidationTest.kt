package com.loganmartlew.rangework.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

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
            defaultClubReference = "  GW  ",
        ).validated()

        assertEquals("Wedge ladder", validated.title)
        assertEquals("Keep tempo steady", validated.notes)
        assertEquals("Start line", validated.focus)
        assertEquals("GW", validated.defaultClubReference)
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
                    clubReference = "  54*  ",
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
        assertEquals("54*", validated.items.last().clubReference)
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
        assertEquals("title", issues.first().field)
    }

    @Test
    fun practiceUnitValidationIssuesReturnsFieldIdForEmptyInstructions() {
        val issues = PracticeUnitDraft(
            title = "Wedge work",
            instructions = emptyList(),
        ).validationIssues()

        assertEquals(1, issues.size)
        assertEquals("instructions", issues.first().field)
    }

    @Test
    fun practiceUnitValidationIssuesReturnsIndexedFieldIdsForInvalidInstructions() {
        val issues = PracticeUnitDraft(
            title = "Wedge work",
            instructions = listOf(
                PracticeInstructionDraft(order = 1, text = "", ballCount = 0),
            ),
        ).validationIssues()

        val fields = issues.map { it.field }
        assert(fields.contains("instructions[0].text")) { "Expected instructions[0].text in $fields" }
        assert(fields.contains("instructions[0].ballCount")) { "Expected instructions[0].ballCount in $fields" }
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
        assertEquals("name", issues.first().field)
    }

    @Test
    fun practiceSessionValidationIssuesReturnsIndexedFieldIdsForMissingUnit() {
        val issues = PracticeSessionDraft(
            name = "My session",
            items = listOf(
                PracticeSessionItemDraft(practiceUnitId = "  ", order = 1, repeatCount = 1),
            ),
        ).validationIssues()

        val fields = issues.map { it.field }
        assert(fields.contains("items[0].practiceUnitId")) { "Expected items[0].practiceUnitId in $fields" }
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

        val fields = issues.map { it.field }
        assert(fields.contains("items[0].repeatCount")) { "Expected items[0].repeatCount in $fields" }
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
