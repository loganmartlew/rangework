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
                PracticeInstructionDraft(order = 1, text = "", ballCount = -1),
            ),
        ).validationIssues()

        val targets = issues.map { it.target }
        assertTrue("Expected InstructionText(0) in $targets") { targets.contains(ValidationTarget.InstructionText(0)) }
        assertTrue("Expected InstructionBallCount(0) in $targets") { targets.contains(ValidationTarget.InstructionBallCount(0)) }
    }

    @Test
    fun practiceUnitValidationAllowsZeroBallCountAndRoundTrips() {
        val draft = PracticeUnitDraft(
            title = "Feel work",
            instructions = listOf(
                PracticeInstructionDraft(order = 1, text = "Five smooth practice swings", ballCount = 0),
                PracticeInstructionDraft(order = 2, text = "Hit 10 stock wedges", ballCount = 10),
                PracticeInstructionDraft(order = 3, text = "Visualise the shot"),
            ),
        )

        // Zero produces no Validation Issue; an absent Ball Count stays null (Uncounted).
        assertTrue("Expected no issues for a zero Ball Count: ${draft.validationIssues()}") {
            draft.validationIssues().none { it.target is ValidationTarget.InstructionBallCount }
        }

        val validated = draft.validated()
        assertEquals(0, validated.instructions[0].ballCount)
        assertEquals(10, validated.instructions[1].ballCount)
        assertEquals(null, validated.instructions[2].ballCount)
    }

    @Test
    fun practiceUnitValidationRejectsNegativeBallCount() {
        val issues = PracticeUnitDraft(
            title = "Wedge work",
            instructions = listOf(
                PracticeInstructionDraft(order = 1, text = "Hit balls", ballCount = -1),
            ),
        ).validationIssues()

        val targets = issues.map { it.target }
        assertTrue("Expected InstructionBallCount(0) in $targets") { targets.contains(ValidationTarget.InstructionBallCount(0)) }
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
        assertTrue("Expected ItemUnitReference(0) in $targets") { targets.contains(ValidationTarget.ItemUnitReference(0)) }
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
        assertTrue("Expected ItemRepeatCount(0) in $targets") { targets.contains(ValidationTarget.ItemRepeatCount(0)) }
    }

    @Test
    fun practiceUnitValidationDeduplicatesAttachedTags() {
        val validated = PracticeUnitDraft(
            title = "Gate drill",
            instructions = listOf(PracticeInstructionDraft(order = 1, text = "Hit through gate")),
            tagIds = listOf("tag-a", "  tag-a  ", "tag-b", "tag-a", "   "),
        ).validated()

        assertEquals(listOf("tag-a", "tag-b"), validated.tagIds)
    }

    @Test
    fun practiceUnitValidationRejectsNinthTag() {
        val nineTags = (1..9).map { "tag-$it" }
        assertFailsWith<SharedValidationException> {
            PracticeUnitDraft(
                title = "Gate drill",
                instructions = listOf(PracticeInstructionDraft(order = 1, text = "Hit through gate")),
                tagIds = nineTags,
            ).validated()
        }

        val issues = PracticeUnitDraft(
            title = "Gate drill",
            instructions = listOf(PracticeInstructionDraft(order = 1, text = "Hit through gate")),
            tagIds = nineTags,
        ).validationIssues()
        assertEquals(true, issues.any { it.target == ValidationTarget.Tags })
    }

    @Test
    fun practiceUnitValidationAllowsEightTags() {
        val eightTags = (1..8).map { "tag-$it" }
        val validated = PracticeUnitDraft(
            title = "Gate drill",
            instructions = listOf(PracticeInstructionDraft(order = 1, text = "Hit through gate")),
            tagIds = eightTags,
        ).validated()

        assertEquals(eightTags, validated.tagIds)
    }

    @Test
    fun practiceSessionValidationRejectsNinthTagAfterDeduplication() {
        // Eight distinct ids plus duplicates must pass; a ninth distinct id must fail.
        val eightWithDupes = (1..8).map { "tag-$it" } + listOf("tag-1", "tag-2")
        val validated = PracticeSessionDraft(
            name = "Short game",
            items = listOf(PracticeSessionItemDraft(practiceUnitId = "unit-1", order = 1, repeatCount = 1)),
            tagIds = eightWithDupes,
        ).validated()
        assertEquals((1..8).map { "tag-$it" }, validated.tagIds)

        assertFailsWith<SharedValidationException> {
            PracticeSessionDraft(
                name = "Short game",
                items = listOf(PracticeSessionItemDraft(practiceUnitId = "unit-1", order = 1, repeatCount = 1)),
                tagIds = (1..9).map { "tag-$it" },
            ).validated()
        }
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
