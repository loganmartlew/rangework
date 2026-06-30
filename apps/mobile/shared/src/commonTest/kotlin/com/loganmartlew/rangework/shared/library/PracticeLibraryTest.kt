package com.loganmartlew.rangework.shared.library

import com.loganmartlew.rangework.shared.data.InMemoryPracticeSessionRepository
import com.loganmartlew.rangework.shared.data.InMemoryPracticeUnitRepository
import com.loganmartlew.rangework.shared.model.PracticeInstructionDraft
import com.loganmartlew.rangework.shared.model.PracticeSessionDraft
import com.loganmartlew.rangework.shared.model.PracticeSessionItemDraft
import com.loganmartlew.rangework.shared.model.PracticeUnitDraft
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PracticeLibraryTest {

    private fun createLibrary(): Pair<DefaultPracticeLibrary, InMemoryPracticeUnitRepository> {
        val unitRepo = InMemoryPracticeUnitRepository()
        val sessionRepo = InMemoryPracticeSessionRepository()
        val library = DefaultPracticeLibrary(unitRepo, sessionRepo)
        return library to unitRepo
    }

    // ── saveUnit normalizes and validates ──────────────────────────────

    @Test
    fun saveUnitNormalizesDraft() = kotlinx.coroutines.test.runTest {
        val (library, unitRepo) = createLibrary()

        val result = library.saveUnit(
            draft = PracticeUnitDraft(
                title = "  Distance wedges ",
                instructions = listOf(
                    PracticeInstructionDraft(
                        order = 4,
                        text = "  Hit 10 balls  ",
                    ),
                ),
            ),
        )

        assertIs<PracticeLibraryResult.Saved<*>>(result)
        val saved = (result as PracticeLibraryResult.Saved).value
        assertEquals("Distance wedges", saved.title)
        assertEquals(listOf(1), saved.instructions.map { it.order })
        assertEquals("Hit 10 balls", saved.instructions.first().text)
        assertEquals(1, unitRepo.drafts.size)
        assertEquals("Distance wedges", unitRepo.drafts.single().title)
    }

    // ── saveUnit returns Invalid on bad data ──────────────────────────

    @Test
    fun saveUnitReturnsInvalidOnBlankTitle() = kotlinx.coroutines.test.runTest {
        val (library, _) = createLibrary()

        val result = library.saveUnit(
            draft = PracticeUnitDraft(
                title = "   ",
                instructions = listOf(
                    PracticeInstructionDraft(order = 1, text = "Hit balls"),
                ),
            ),
        )

        assertIs<PracticeLibraryResult.Invalid>(result)
        val invalid = result as PracticeLibraryResult.Invalid
        assertTrue(invalid.issues.any { it.field == "title" })
    }

    @Test
    fun saveUnitReturnsInvalidOnBlankInstructionText() = kotlinx.coroutines.test.runTest {
        val (library, _) = createLibrary()

        val result = library.saveUnit(
            draft = PracticeUnitDraft(
                title = "My drill",
                instructions = listOf(
                    PracticeInstructionDraft(order = 1, text = "   "),
                ),
            ),
        )

        assertIs<PracticeLibraryResult.Invalid>(result)
        val invalid = result as PracticeLibraryResult.Invalid
        assertTrue(invalid.issues.any { it.field == "instructions[0].text" })
    }

    @Test
    fun saveUnitReturnsInvalidOnNegativeBallCount() = kotlinx.coroutines.test.runTest {
        val (library, _) = createLibrary()

        val result = library.saveUnit(
            draft = PracticeUnitDraft(
                title = "My drill",
                instructions = listOf(
                    PracticeInstructionDraft(order = 1, text = "Hit", ballCount = -1),
                ),
            ),
        )

        assertIs<PracticeLibraryResult.Invalid>(result)
        val invalid = result as PracticeLibraryResult.Invalid
        assertTrue(invalid.issues.any { it.field == "instructions[0].ballCount" })
    }

    @Test
    fun saveUnitPersistsZeroBallCount() = kotlinx.coroutines.test.runTest {
        val (library, unitRepo) = createLibrary()

        val result = library.saveUnit(
            draft = PracticeUnitDraft(
                title = "Feel work",
                instructions = listOf(
                    PracticeInstructionDraft(order = 1, text = "Five practice swings", ballCount = 0),
                ),
            ),
        )

        assertIs<PracticeLibraryResult.Saved<*>>(result)
        val saved = (result as PracticeLibraryResult.Saved).value
        assertEquals(0, saved.instructions.single().ballCount)
        assertEquals(0, unitRepo.drafts.single().instructions.single().ballCount)
    }

    @Test
    fun saveUnitReturnsInvalidWhenNoInstructions() = kotlinx.coroutines.test.runTest {
        val (library, _) = createLibrary()

        val result = library.saveUnit(
            draft = PracticeUnitDraft(
                title = "My drill",
                instructions = emptyList(),
            ),
        )

        assertIs<PracticeLibraryResult.Invalid>(result)
        val invalid = result as PracticeLibraryResult.Invalid
        assertTrue(invalid.issues.any { it.field == "instructions" })
    }

    // ── validateUnit (sync) ───────────────────────────────────────────

    @Test
    fun validateUnitReturnsIssuesWithoutPersisting() = kotlinx.coroutines.test.runTest {
        val (library, unitRepo) = createLibrary()

        val issues = library.validateUnit(
            PracticeUnitDraft(
                title = "   ",
                instructions = listOf(
                    PracticeInstructionDraft(order = 1, text = "", ballCount = -1),
                ),
            ),
        )

        assertTrue(issues.isNotEmpty())
        assertTrue(unitRepo.drafts.isEmpty()) // nothing persisted
    }

    // ── duplicateUnit ─────────────────────────────────────────────────

    @Test
    fun duplicateUnitCreatesCopyWithNewId() = kotlinx.coroutines.test.runTest {
        val (library, _) = createLibrary()

        // Save a unit first
        val saved = (library.saveUnit(
            PracticeUnitDraft(
                title = "Chip shots",
                notes = "Tight lies",
                focus = "Weight forward",
                defaultClubCode = "PW",
                instructions = listOf(
                    PracticeInstructionDraft(order = 1, text = "Grip down", ballCount = 10),
                ),
            ),
        ) as PracticeLibraryResult.Saved).value

        val duplicated = library.duplicateUnit(saved.id)

        assertTrue(duplicated.id != saved.id, "Duplicate must have a new id")
        assertEquals("Chip shots", duplicated.title)
        assertEquals("Tight lies", duplicated.notes)
        assertEquals("Weight forward", duplicated.focus)
        assertEquals("PW", duplicated.defaultClubCode)
        assertEquals(1, duplicated.instructions.size)
        assertEquals("Grip down", duplicated.instructions.first().text)
        assertEquals(10, duplicated.instructions.first().ballCount)
        // Instruction gets a new id
        assertTrue(duplicated.instructions.none { it.id == saved.instructions.first().id })
    }

    @Test
    fun duplicateUnitThrowsWhenNotFound() = kotlinx.coroutines.test.runTest {
        val (library, _) = createLibrary()

        var caught = false
        try {
            library.duplicateUnit("nonexistent")
        } catch (e: Exception) {
            caught = true
            assertTrue(e.message?.contains("not found") == true)
        }
        assertTrue(caught, "Expected error for missing unit")
    }

    // ── restoreUnit ──────────────────────────────────────────────────

    @Test
    fun restoreUnitPersistsUnderOriginalId() = kotlinx.coroutines.test.runTest {
        val (library, _) = createLibrary()

        val saved = (library.saveUnit(
            PracticeUnitDraft(
                title = "Wedge ladder",
                instructions = listOf(
                    PracticeInstructionDraft(order = 1, text = "Hit 10 stock wedges"),
                ),
            ),
        ) as PracticeLibraryResult.Saved).value

        // Delete it
        library.deleteUnit(saved.id)
        assertTrue(library.listUnits().isEmpty())

        // Restore
        val restored = library.restoreUnit(saved)
        assertEquals(saved.id, restored.id)
        assertEquals("Wedge ladder", restored.title)

        val reloaded = library.getUnit(saved.id)
        assertNotNull(reloaded)
    }

    // ── delete / list / get ──────────────────────────────────────────

    @Test
    fun deleteUnitRemovesEntity() = kotlinx.coroutines.test.runTest {
        val (library, _) = createLibrary()

        val saved = (library.saveUnit(
            PracticeUnitDraft(
                title = "Temp drill",
                instructions = listOf(PracticeInstructionDraft(order = 1, text = "Do something")),
            ),
        ) as PracticeLibraryResult.Saved).value

        assertEquals(1, library.listUnits().size)

        library.deleteUnit(saved.id)

        assertTrue(library.listUnits().isEmpty())
        assertEquals(null, library.getUnit(saved.id))
    }

    // ── Session ──────────────────────────────────────────────────────

    @Test
    fun saveSessionNormalizesAndReturnsSaved() = kotlinx.coroutines.test.runTest {
        val (library, _) = createLibrary()

        // Save a unit to reference
        val unit = (library.saveUnit(
            PracticeUnitDraft(
                title = "Putting",
                instructions = listOf(PracticeInstructionDraft(order = 1, text = "Hit 5ft putts")),
            ),
        ) as PracticeLibraryResult.Saved).value

        val result = library.saveSession(
            PracticeSessionDraft(
                name = "  Short game block ",
                items = listOf(
                    PracticeSessionItemDraft(
                        practiceUnitId = "  ${unit.id} ",
                        order = 9,
                        repeatCount = 3,
                        clubCode = "  PW ",
                    ),
                ),
                notes = "  Repeat twice ",
            ),
        )

        assertIs<PracticeLibraryResult.Saved<*>>(result)
        val saved = (result as PracticeLibraryResult.Saved).value
        assertEquals("Short game block", saved.name)
        assertEquals("Repeat twice", saved.notes)
        assertEquals(1, saved.items.size)
        assertEquals(unit.id, saved.items.first().practiceUnitId)
        assertEquals(1, saved.items.first().order)
        assertEquals(3, saved.items.first().repeatCount)
        assertEquals("PW", saved.items.first().clubCode)
    }

    @Test
    fun saveSessionReturnsInvalidOnBlankName() = kotlinx.coroutines.test.runTest {
        val (library, _) = createLibrary()

        val result = library.saveSession(
            PracticeSessionDraft(
                name = "   ",
                items = listOf(
                    PracticeSessionItemDraft(
                        practiceUnitId = "unit-1",
                        order = 1,
                        repeatCount = 1,
                    ),
                ),
            ),
        )

        assertIs<PracticeLibraryResult.Invalid>(result)
        val invalid = result as PracticeLibraryResult.Invalid
        assertTrue(invalid.issues.any { it.field == "name" })
    }

    @Test
    fun saveSessionReturnsInvalidOnMissingUnitReference() = kotlinx.coroutines.test.runTest {
        val (library, _) = createLibrary()

        val result = library.saveSession(
            PracticeSessionDraft(
                name = "My session",
                items = listOf(
                    PracticeSessionItemDraft(
                        practiceUnitId = "   ",
                        order = 1,
                        repeatCount = 1,
                    ),
                ),
            ),
        )

        assertIs<PracticeLibraryResult.Invalid>(result)
        val invalid = result as PracticeLibraryResult.Invalid
        assertTrue(invalid.issues.any { it.field == "items[0].practiceUnitId" })
    }

    @Test
    fun saveSessionReturnsInvalidOnZeroRepeatCount() = kotlinx.coroutines.test.runTest {
        val (library, _) = createLibrary()

        val result = library.saveSession(
            PracticeSessionDraft(
                name = "My session",
                items = listOf(
                    PracticeSessionItemDraft(
                        practiceUnitId = "unit-1",
                        order = 1,
                        repeatCount = 0,
                    ),
                ),
            ),
        )

        assertIs<PracticeLibraryResult.Invalid>(result)
        val invalid = result as PracticeLibraryResult.Invalid
        assertTrue(invalid.issues.any { it.field == "items[0].repeatCount" })
    }

    @Test
    fun validateSessionReturnsIssuesWithoutPersisting() = kotlinx.coroutines.test.runTest {
        val (library, _) = createLibrary()

        val issues = library.validateSession(
            PracticeSessionDraft(
                name = "   ",
                items = emptyList(),
            ),
        )

        assertTrue(issues.isNotEmpty())
    }

    @Test
    fun duplicateSessionCreatesCopyWithNewId() = kotlinx.coroutines.test.runTest {
        val (library, _) = createLibrary()

        val unit = (library.saveUnit(
            PracticeUnitDraft(
                title = "Drill",
                instructions = listOf(PracticeInstructionDraft(order = 1, text = "Hit")),
            ),
        ) as PracticeLibraryResult.Saved).value

        val saved = (library.saveSession(
            PracticeSessionDraft(
                name = "Morning block",
                items = listOf(
                    PracticeSessionItemDraft(
                        practiceUnitId = unit.id,
                        order = 1,
                        repeatCount = 2,
                    ),
                ),
            ),
        ) as PracticeLibraryResult.Saved).value

        val duplicated = library.duplicateSession(saved.id)

        assertTrue(duplicated.id != saved.id, "Duplicate must have a new id")
        assertEquals("Morning block", duplicated.name)
        assertEquals(1, duplicated.items.size)
    }

    @Test
    fun restoreSessionPersistsUnderOriginalId() = kotlinx.coroutines.test.runTest {
        val (library, _) = createLibrary()

        val unit = (library.saveUnit(
            PracticeUnitDraft(
                title = "Drill",
                instructions = listOf(PracticeInstructionDraft(order = 1, text = "Hit")),
            ),
        ) as PracticeLibraryResult.Saved).value

        val saved = (library.saveSession(
            PracticeSessionDraft(
                name = "Evening session",
                items = listOf(
                    PracticeSessionItemDraft(
                        practiceUnitId = unit.id,
                        order = 1,
                        repeatCount = 1,
                    ),
                ),
            ),
        ) as PracticeLibraryResult.Saved).value

        library.deleteSession(saved.id)

        val restored = library.restoreSession(saved)
        assertEquals(saved.id, restored.id)
        assertEquals("Evening session", restored.name)
    }

    @Test
    fun deleteSessionRemovesEntity() = kotlinx.coroutines.test.runTest {
        val (library, _) = createLibrary()

        val unit = (library.saveUnit(
            PracticeUnitDraft(
                title = "Drill",
                instructions = listOf(PracticeInstructionDraft(order = 1, text = "Hit")),
            ),
        ) as PracticeLibraryResult.Saved).value

        val saved = (library.saveSession(
            PracticeSessionDraft(
                name = "Temp session",
                items = listOf(
                    PracticeSessionItemDraft(
                        practiceUnitId = unit.id,
                        order = 1,
                        repeatCount = 1,
                    ),
                ),
            ),
        ) as PracticeLibraryResult.Saved).value

        assertEquals(1, library.listSessions().size)

        library.deleteSession(saved.id)

        assertTrue(library.listSessions().isEmpty())
    }
}