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
                    clubReference = "  56*  ",
                    repCount = 10,
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
            tags = listOf(" wedges ", "tempo", "wedges"),
            defaultBallCount = 15,
        ).validated()

        assertEquals("Wedge ladder", validated.title)
        assertEquals("Keep tempo steady", validated.notes)
        assertEquals("Start line", validated.focus)
        assertEquals("GW", validated.defaultClubReference)
        assertEquals(listOf("wedges", "tempo"), validated.tags)
        assertEquals(listOf(1, 2), validated.instructions.map(PracticeInstructionDraft::order))
        assertEquals("Finish with 5 flighted shots", validated.instructions.first().text)
        assertEquals("56*", validated.instructions.last().clubReference)
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
                    notes = "  Start here  ",
                    focusCue = "  Landing spot  ",
                    restSeconds = 30,
                    overrideBallCount = 12,
                ),
                PracticeSessionItemDraft(
                    practiceUnitId = "unit-2",
                    order = 2,
                ),
            ),
        ).validated()

        assertEquals("Scoring session", validated.name)
        assertEquals("Alternate tempo work", validated.notes)
        assertEquals(listOf(1, 2), validated.items.map(PracticeSessionItemDraft::order))
        assertEquals(listOf("unit-2", "unit-2"), validated.items.map(PracticeSessionItemDraft::practiceUnitId))
        assertEquals("Start here", validated.items.last().notes)
        assertEquals("Landing spot", validated.items.last().focusCue)
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
