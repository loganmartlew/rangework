package com.loganmartlew.rangework.shared.library

import com.loganmartlew.rangework.shared.data.InMemoryPracticeSessionRepository
import com.loganmartlew.rangework.shared.data.InMemoryPracticeUnitRepository
import com.loganmartlew.rangework.shared.model.ObservationType
import com.loganmartlew.rangework.shared.model.PracticeInstructionDraft
import com.loganmartlew.rangework.shared.model.PracticeSession
import com.loganmartlew.rangework.shared.model.PracticeSessionDraft
import com.loganmartlew.rangework.shared.model.PracticeSessionItemDraft
import com.loganmartlew.rangework.shared.model.PracticeUnitDraft
import com.loganmartlew.rangework.shared.model.ValidationTarget
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class PracticeLibraryTest {

    private fun createLibrary(): Pair<DefaultPracticeLibrary, InMemoryPracticeUnitRepository> {
        val unitRepo = InMemoryPracticeUnitRepository()
        // Share the unit repo so the in-memory session repo can deep-copy and
        // cascade-delete Inline Units, matching the server-side behaviour.
        val sessionRepo = InMemoryPracticeSessionRepository(unitRepo)
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
                successCriterion = "Lands on the green",
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
        assertEquals("Lands on the green", duplicated.successCriterion)
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
                        observationTypes = listOf(ObservationType.SHAPE, ObservationType.CONTACT),
                    ),
                ),
            ),
        ) as PracticeLibraryResult.Saved).value

        val duplicated = library.duplicateSession(saved.id)

        assertTrue(duplicated.id != saved.id, "Duplicate must have a new id")
        assertEquals("Morning block", duplicated.name)
        assertEquals(1, duplicated.items.size)
        // validated() normalizes Observation Types into catalog order (CONTACT before SHAPE).
        assertEquals(
            listOf(ObservationType.CONTACT, ObservationType.SHAPE),
            duplicated.items.first().observationTypes,
        )
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

    // ── Success-requires-criterion (Observation Types) ────────────────

    @Test
    fun saveSessionRejectsSuccessOnCriterionlessUnit() = kotlinx.coroutines.test.runTest {
        val (library, _) = createLibrary()

        val unit = (library.saveUnit(
            PracticeUnitDraft(
                title = "No criterion drill",
                instructions = listOf(PracticeInstructionDraft(order = 1, text = "Hit")),
            ),
        ) as PracticeLibraryResult.Saved).value

        val result = library.saveSession(
            PracticeSessionDraft(
                name = "Session",
                items = listOf(
                    PracticeSessionItemDraft(
                        practiceUnitId = unit.id,
                        order = 1,
                        repeatCount = 1,
                        observationTypes = listOf(ObservationType.SUCCESS),
                    ),
                ),
            ),
        )

        assertIs<PracticeLibraryResult.Invalid>(result)
        val invalid = result as PracticeLibraryResult.Invalid
        assertTrue(invalid.issues.any { it.target == ValidationTarget.ItemObservationTypes(0) })
    }

    @Test
    fun saveSessionAllowsSuccessWhenUnitHasCriterion() = kotlinx.coroutines.test.runTest {
        val (library, _) = createLibrary()

        val unit = (library.saveUnit(
            PracticeUnitDraft(
                title = "Criterion drill",
                instructions = listOf(PracticeInstructionDraft(order = 1, text = "Hit")),
                successCriterion = "inside 5m of the flag",
            ),
        ) as PracticeLibraryResult.Saved).value

        val result = library.saveSession(
            PracticeSessionDraft(
                name = "Session",
                items = listOf(
                    PracticeSessionItemDraft(
                        practiceUnitId = unit.id,
                        order = 1,
                        repeatCount = 1,
                        observationTypes = listOf(ObservationType.SUCCESS, ObservationType.SHAPE),
                    ),
                ),
            ),
        )

        assertIs<PracticeLibraryResult.Saved<*>>(result)
        val saved = (result as PracticeLibraryResult.Saved).value
        assertEquals(
            listOf(ObservationType.SUCCESS, ObservationType.SHAPE),
            saved.items.first().observationTypes,
        )
    }

    @Test
    fun validateSessionSurfacesSuccessRequiresCriterion() = kotlinx.coroutines.test.runTest {
        val (library, _) = createLibrary()

        val unit = (library.saveUnit(
            PracticeUnitDraft(
                title = "No criterion drill",
                instructions = listOf(PracticeInstructionDraft(order = 1, text = "Hit")),
            ),
        ) as PracticeLibraryResult.Saved).value

        // The sync validation surface must agree with saveSession, so the planner
        // can show the issue inline before the user hits Save.
        val issues = library.validateSession(
            PracticeSessionDraft(
                name = "Session",
                items = listOf(
                    PracticeSessionItemDraft(
                        practiceUnitId = unit.id,
                        order = 1,
                        repeatCount = 1,
                        observationTypes = listOf(ObservationType.SUCCESS),
                    ),
                ),
            ),
        )

        assertTrue(issues.any { it.target == ValidationTarget.ItemObservationTypes(0) })
    }

    @Test
    fun successValidationIgnoresMissingUnit() = kotlinx.coroutines.test.runTest {
        val (library, _) = createLibrary()

        // A unit id that was never saved (e.g. deleted between selection and save)
        // is not a criterion problem — it must not be mislabeled as one.
        val issues = library.validateSession(
            PracticeSessionDraft(
                name = "Session",
                items = listOf(
                    PracticeSessionItemDraft(
                        practiceUnitId = "missing-unit-id",
                        order = 1,
                        repeatCount = 1,
                        observationTypes = listOf(ObservationType.SUCCESS),
                    ),
                ),
            ),
        )

        assertTrue(issues.none { it.target == ValidationTarget.ItemObservationTypes(0) })
    }

    // ── Archiving lifecycle ────────────────────────────────────────────

    private suspend fun DefaultPracticeLibrary.saveTestSession(name: String = "Session"): PracticeSession {
        val unit = (saveUnit(
            PracticeUnitDraft(
                title = "Drill",
                instructions = listOf(PracticeInstructionDraft(order = 1, text = "Hit")),
            ),
        ) as PracticeLibraryResult.Saved).value

        return (saveSession(
            PracticeSessionDraft(
                name = name,
                items = listOf(
                    PracticeSessionItemDraft(
                        practiceUnitId = unit.id,
                        order = 1,
                        repeatCount = 1,
                    ),
                ),
            ),
        ) as PracticeLibraryResult.Saved).value
    }

    @Test
    fun archiveSessionSetsArchivedState() = kotlinx.coroutines.test.runTest {
        val (library, _) = createLibrary()
        val saved = library.saveTestSession()

        val archived = library.archiveSession(saved.id)

        assertTrue(archived.archivedAt != null)
        assertTrue(library.listSessions().none { it.id == saved.id })
        assertTrue(library.listArchivedSessions().any { it.id == saved.id })
    }

    @Test
    fun unarchiveSessionClearsArchivedState() = kotlinx.coroutines.test.runTest {
        val (library, _) = createLibrary()
        val saved = library.saveTestSession()
        library.archiveSession(saved.id)

        val unarchived = library.unarchiveSession(saved.id)

        assertEquals(null, unarchived.archivedAt)
        assertTrue(library.listSessions().any { it.id == saved.id })
        assertTrue(library.listArchivedSessions().none { it.id == saved.id })
    }

    @Test
    fun listSessionsExcludesArchived() = kotlinx.coroutines.test.runTest {
        val (library, _) = createLibrary()
        val unarchived = library.saveTestSession("Kept")
        val archived = library.saveTestSession("Tidied")
        library.archiveSession(archived.id)

        val sessions = library.listSessions()

        assertTrue(sessions.any { it.id == unarchived.id })
        assertTrue(sessions.none { it.id == archived.id })
    }

    @Test
    fun listArchivedSessionsReturnsOnlyArchived() = kotlinx.coroutines.test.runTest {
        val (library, _) = createLibrary()
        val unarchived = library.saveTestSession("Kept")
        val archived = library.saveTestSession("Tidied")
        library.archiveSession(archived.id)

        val archivedSessions = library.listArchivedSessions()

        assertTrue(archivedSessions.any { it.id == archived.id })
        assertTrue(archivedSessions.none { it.id == unarchived.id })
    }

    @Test
    fun getSessionReturnsArchivedSession() = kotlinx.coroutines.test.runTest {
        val (library, _) = createLibrary()
        val saved = library.saveTestSession()
        library.archiveSession(saved.id)

        val reloaded = library.getSession(saved.id)

        assertNotNull(reloaded)
        assertTrue(reloaded.archivedAt != null)
    }

    @Test
    fun saveSessionRejectsArchivedEdit() = kotlinx.coroutines.test.runTest {
        val (library, _) = createLibrary()
        val saved = library.saveTestSession()
        library.archiveSession(saved.id)

        var caught = false
        try {
            library.saveSession(
                PracticeSessionDraft(name = "Renamed", items = saved.items.map {
                    PracticeSessionItemDraft(
                        practiceUnitId = it.practiceUnitId,
                        order = it.order,
                        repeatCount = it.repeatCount,
                    )
                }),
                sessionId = saved.id,
            )
        } catch (e: Exception) {
            caught = true
        }
        assertTrue(caught, "Expected error editing an archived session")

        val stored = library.getSession(saved.id)
        assertEquals(saved.name, stored?.name)
    }

    @Test
    fun duplicateSessionOfArchivedProducesUnarchivedCopy() = kotlinx.coroutines.test.runTest {
        val (library, _) = createLibrary()
        val saved = library.saveTestSession()
        library.archiveSession(saved.id)

        val duplicated = library.duplicateSession(saved.id)

        assertTrue(duplicated.id != saved.id)
        assertEquals(null, duplicated.archivedAt)
        assertTrue(library.listSessions().any { it.id == duplicated.id })
    }

    @Test
    fun archiveSucceedsRegardlessOfState() = kotlinx.coroutines.test.runTest {
        val (library, _) = createLibrary()
        val saved = library.saveTestSession()

        val archived = library.archiveSession(saved.id)

        assertTrue(archived.archivedAt != null)
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

    // ── Inline Units ───────────────────────────────────────────────────

    /**
     * Builds a session with one library item and one Inline Unit (a unit scoped
     * to the session), returning (session, library unit id, inline unit id).
     * Inline units have no in-app creation path yet, so the fixture mints one by
     * saving a unit then scoping it to the session it belongs to.
     */
    private suspend fun DefaultPracticeLibrary.saveInlineFixture(
        unitRepo: InMemoryPracticeUnitRepository,
    ): Triple<PracticeSession, String, String> {
        val libraryUnit = (saveUnit(
            PracticeUnitDraft(
                title = "Library drill",
                instructions = listOf(PracticeInstructionDraft(order = 1, text = "Hit")),
            ),
        ) as PracticeLibraryResult.Saved).value

        val inlineUnit = (saveUnit(
            PracticeUnitDraft(
                title = "One-off drill",
                successCriterion = "lands soft",
                instructions = listOf(PracticeInstructionDraft(order = 1, text = "Chip", ballCount = 8)),
            ),
        ) as PracticeLibraryResult.Saved).value

        val session = (saveSession(
            PracticeSessionDraft(
                name = "Mixed session",
                items = listOf(
                    PracticeSessionItemDraft(practiceUnitId = libraryUnit.id, order = 1, repeatCount = 1),
                    PracticeSessionItemDraft(practiceUnitId = inlineUnit.id, order = 2, repeatCount = 1),
                ),
            ),
        ) as PracticeLibraryResult.Saved).value

        unitRepo.setScopedSession(inlineUnit.id, session.id)
        return Triple(session, libraryUnit.id, inlineUnit.id)
    }

    @Test
    fun listUnitsExcludesInlineUnits() = kotlinx.coroutines.test.runTest {
        val (library, unitRepo) = createLibrary()
        val (_, libraryUnitId, inlineUnitId) = library.saveInlineFixture(unitRepo)

        val listed = library.listUnits().map { it.id }
        assertTrue(libraryUnitId in listed, "Library unit stays in the library")
        assertTrue(inlineUnitId !in listed, "Inline unit is excluded from the library")
        // …but is still reachable by id (session detail, promotion, duplication).
        assertNotNull(library.getUnit(inlineUnitId))
    }

    @Test
    fun promoteUnitDetachesOwnership() = kotlinx.coroutines.test.runTest {
        val (library, unitRepo) = createLibrary()
        val (session, _, inlineUnitId) = library.saveInlineFixture(unitRepo)

        val promoted = library.promoteUnit(inlineUnitId)

        assertEquals(null, promoted.scopedToSessionId)
        assertTrue(library.listUnits().any { it.id == inlineUnitId }, "Promoted unit joins the library")
        // The session keeps referencing the same unit id.
        val reloaded = assertNotNull(library.getSession(session.id))
        assertTrue(reloaded.items.any { it.practiceUnitId == inlineUnitId })
    }

    @Test
    fun promoteIsOneWayContentUnchanged() = kotlinx.coroutines.test.runTest {
        val (library, unitRepo) = createLibrary()
        val (_, _, inlineUnitId) = library.saveInlineFixture(unitRepo)
        val before = assertNotNull(library.getUnit(inlineUnitId))

        val after = library.promoteUnit(inlineUnitId)

        // Identity and content unchanged — only ownership detaches.
        assertEquals(before.id, after.id)
        assertEquals(before.title, after.title)
        assertEquals(before.successCriterion, after.successCriterion)
        assertEquals(
            before.instructions.map { it.text to it.ballCount },
            after.instructions.map { it.text to it.ballCount },
        )
        // No demotion path exists — promoteUnit is the only scope-changing API.
    }

    @Test
    fun duplicateSessionDeepCopiesInlineUnits() = kotlinx.coroutines.test.runTest {
        val (library, unitRepo) = createLibrary()
        val (session, libraryUnitId, inlineUnitId) = library.saveInlineFixture(unitRepo)

        val copy = library.duplicateSession(session.id)

        assertTrue(copy.id != session.id)
        // The library item is shared (same id); the inline item points at a new unit.
        assertTrue(copy.items.any { it.practiceUnitId == libraryUnitId })
        val copiedInlineId = copy.items.map { it.practiceUnitId }
            .single { it != libraryUnitId }
        assertTrue(copiedInlineId != inlineUnitId, "Duplicate mints a new inline unit id")

        val sourceInline = assertNotNull(library.getUnit(inlineUnitId))
        val copiedInline = assertNotNull(library.getUnit(copiedInlineId))
        assertEquals(copy.id, copiedInline.scopedToSessionId)
        assertEquals("One-off drill", copiedInline.title)

        // Editing the copy's inline unit leaves the source's untouched.
        library.saveUnit(
            PracticeUnitDraft(
                title = "Edited copy",
                successCriterion = "lands soft",
                instructions = listOf(PracticeInstructionDraft(order = 1, text = "Chip", ballCount = 8)),
            ),
            unitId = copiedInlineId,
        )
        assertEquals("One-off drill", assertNotNull(library.getUnit(inlineUnitId)).title)
        assertEquals("Edited copy", assertNotNull(library.getUnit(copiedInlineId)).title)
        // The edit preserved the copy's scope (D5).
        assertEquals(copy.id, assertNotNull(library.getUnit(copiedInlineId)).scopedToSessionId)
    }

    @Test
    fun duplicateSessionOfLibraryOnlyIsIdentical() = kotlinx.coroutines.test.runTest {
        val (library, _) = createLibrary()

        val unit = (library.saveUnit(
            PracticeUnitDraft(
                title = "Putting",
                successCriterion = "inside 3ft",
                instructions = listOf(PracticeInstructionDraft(order = 1, text = "Hit 5ft putts")),
            ),
        ) as PracticeLibraryResult.Saved).value

        val source = (library.saveSession(
            PracticeSessionDraft(
                name = "Library-only block",
                notes = "keep it clean",
                items = listOf(
                    PracticeSessionItemDraft(
                        practiceUnitId = unit.id,
                        order = 1,
                        repeatCount = 3,
                        clubCode = "PW",
                        observationTypes = listOf(ObservationType.SUCCESS, ObservationType.CONTACT),
                    ),
                ),
            ),
        ) as PracticeLibraryResult.Saved).value

        val unitsBefore = library.listUnits().map { it.id }.toSet()
        val copy = library.duplicateSession(source.id)

        assertTrue(copy.id != source.id)
        assertEquals(null, copy.archivedAt)
        // Identical item projection; the library reference is shared (no new units).
        assertEquals(source.items.size, copy.items.size)
        val src = source.items.single()
        val dup = copy.items.single()
        assertEquals(src.practiceUnitId, dup.practiceUnitId)
        assertEquals(src.order, dup.order)
        assertEquals(src.repeatCount, dup.repeatCount)
        assertEquals(src.clubCode, dup.clubCode)
        assertEquals(src.observationTypes, dup.observationTypes)
        assertEquals(unitsBefore, library.listUnits().map { it.id }.toSet(), "No inline units minted")
    }

    @Test
    fun deleteSessionCascadesInlineUnits() = kotlinx.coroutines.test.runTest {
        val (library, unitRepo) = createLibrary()
        val (session, libraryUnitId, inlineUnitId) = library.saveInlineFixture(unitRepo)

        library.deleteSession(session.id)

        assertEquals(null, library.getUnit(inlineUnitId), "Inline unit dies with its session")
        assertNotNull(library.getUnit(libraryUnitId), "Library unit survives")
    }

    @Test
    fun editDroppingInlineUnitReapsOrphan() = kotlinx.coroutines.test.runTest {
        val (library, unitRepo) = createLibrary()

        val libraryUnit = (library.saveUnit(
            PracticeUnitDraft(
                title = "Library drill",
                instructions = listOf(PracticeInstructionDraft(order = 1, text = "Hit")),
            ),
        ) as PracticeLibraryResult.Saved).value
        val inlineUnit = (library.saveUnit(
            PracticeUnitDraft(
                title = "Doomed inline",
                instructions = listOf(PracticeInstructionDraft(order = 1, text = "Chip")),
            ),
        ) as PracticeLibraryResult.Saved).value
        val promotedUnit = (library.saveUnit(
            PracticeUnitDraft(
                title = "Kept via promotion",
                instructions = listOf(PracticeInstructionDraft(order = 1, text = "Pitch")),
            ),
        ) as PracticeLibraryResult.Saved).value

        val session = (library.saveSession(
            PracticeSessionDraft(
                name = "Session",
                items = listOf(
                    PracticeSessionItemDraft(practiceUnitId = libraryUnit.id, order = 1, repeatCount = 1),
                    PracticeSessionItemDraft(practiceUnitId = inlineUnit.id, order = 2, repeatCount = 1),
                    PracticeSessionItemDraft(practiceUnitId = promotedUnit.id, order = 3, repeatCount = 1),
                ),
            ),
        ) as PracticeLibraryResult.Saved).value

        unitRepo.setScopedSession(inlineUnit.id, session.id)
        unitRepo.setScopedSession(promotedUnit.id, session.id)
        // Promote one before the edit — its scope is now null.
        library.promoteUnit(promotedUnit.id)

        // Re-save the session with only the library item.
        library.saveSession(
            PracticeSessionDraft(
                name = "Session",
                items = listOf(
                    PracticeSessionItemDraft(practiceUnitId = libraryUnit.id, order = 1, repeatCount = 1),
                ),
            ),
            sessionId = session.id,
        )

        assertEquals(null, library.getUnit(inlineUnit.id), "Dropped inline unit is reaped")
        assertNotNull(library.getUnit(promotedUnit.id), "Promoted unit survives (scope already null)")
        assertTrue(library.listUnits().any { it.id == promotedUnit.id })
    }

    @Test
    fun inlineUnitDormantWhenArchived() = kotlinx.coroutines.test.runTest {
        val (library, unitRepo) = createLibrary()
        val (session, _, inlineUnitId) = library.saveInlineFixture(unitRepo)

        library.archiveSession(session.id)

        // Dormancy is derived from the owner — no extra state on the unit.
        assertTrue(library.listUnits().none { it.id == inlineUnitId }, "Still absent from the library")
        assertNotNull(library.getUnit(inlineUnitId), "Still reachable by id")
    }
}