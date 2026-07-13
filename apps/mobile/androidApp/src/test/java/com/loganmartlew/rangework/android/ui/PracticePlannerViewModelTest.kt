package com.loganmartlew.rangework.android.ui

import com.loganmartlew.rangework.shared.auth.AuthState
import com.loganmartlew.rangework.shared.config.baselineEnvironment
import com.loganmartlew.rangework.shared.data.DataFoundation
import com.loganmartlew.rangework.shared.library.DefaultPracticeLibrary
import com.loganmartlew.rangework.shared.recording.DefaultRangeSessionRecorder
import com.loganmartlew.rangework.shared.model.BlockResult
import com.loganmartlew.rangework.shared.model.MeasurementPreferences
import com.loganmartlew.rangework.shared.model.Observation
import com.loganmartlew.rangework.shared.model.ObservationType
import com.loganmartlew.rangework.shared.model.PracticeInstruction
import com.loganmartlew.rangework.shared.model.PracticeSession
import com.loganmartlew.rangework.shared.model.PracticeSessionDraft
import com.loganmartlew.rangework.shared.model.PracticeSessionItem
import com.loganmartlew.rangework.shared.model.PracticeSessionItemDraft
import com.loganmartlew.rangework.shared.model.PracticeUnit
import com.loganmartlew.rangework.shared.model.PracticeUnitDraft
import com.loganmartlew.rangework.shared.model.ActiveRangeSessionSummary
import com.loganmartlew.rangework.shared.model.CompletedRangeSessionSummary
import com.loganmartlew.rangework.shared.model.RangeSession
import com.loganmartlew.rangework.shared.repository.AccountDeletionRepository
import com.loganmartlew.rangework.shared.repository.ClubRepository
import com.loganmartlew.rangework.shared.repository.MeasurementPreferencesRepository
import com.loganmartlew.rangework.shared.repository.PracticeSessionRepository
import com.loganmartlew.rangework.shared.repository.PracticeUnitRepository
import com.loganmartlew.rangework.shared.repository.RangeSessionRepository
import com.loganmartlew.rangework.android.ui.PlannerStatus
import com.loganmartlew.rangework.shared.model.NextMoveState
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class PracticePlannerViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun signedInRefreshLoadsUnitsAndSessions() = runTest {
        val repositories = FakePlannerRepositories()
        repositories.units += sampleUnit()
        repositories.sessions += sampleSession()

        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )

        viewModel.onAuthStateChanged(
            AuthState.SignedIn(
                userId = "user-1",
                userEmail = "logan@example.com",
            ),
        )
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.units.size)
        assertEquals(1, viewModel.uiState.value.sessions.size)
        val status = viewModel.uiState.value.status
        assertTrue("Expected Info status with 'ready'", status is PlannerStatus.Info && status.text.contains("ready"))
    }

    @Test
    fun saveUnitPersistsNormalizedDraft() = runTest {
        val repositories = FakePlannerRepositories()
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )

        viewModel.onAuthStateChanged(
            AuthState.SignedIn(
                userId = "user-1",
                userEmail = "logan@example.com",
            ),
        )
        advanceUntilIdle()

        viewModel.updateUnitTitle("  Distance wedges ")
        viewModel.updateInstructionText(0, "  Hit 10 wedges ")
        viewModel.updateInstructionBallCount(0, "10")
        viewModel.saveUnit()
        advanceUntilIdle()

        assertEquals("Distance wedges", repositories.savedUnitDrafts.single().title)
        assertEquals(10, repositories.savedUnitDrafts.single().instructions.single().ballCount)
        val saveStatus = viewModel.uiState.value.status
        assertTrue("Expected Notification with 'Saved'", saveStatus is PlannerStatus.Notification && saveStatus.text.contains("Saved"))
        assertEquals(1, viewModel.uiState.value.units.size)
    }

    @Test
    fun missingPlanningTableShowsFriendlySetupMessage() = runTest {
        val repositories = FakePlannerRepositories(
            listUnitsException = IllegalStateException(
                "Could not find the table 'public.practice_units' in the schema cache (Perhaps you meant the table 'public.profiles').",
            ),
        )
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )

        viewModel.onAuthStateChanged(
            AuthState.SignedIn(
                userId = "user-1",
                userEmail = "logan@example.com",
            ),
        )
        advanceUntilIdle()

        assertEquals(
            PlannerStatus.SchemaNotReady,
            viewModel.uiState.value.status,
        )
    }

    @Test
    fun planningPermissionDeniedShowsFriendlySetupMessage() = runTest {
        val repositories = FakePlannerRepositories(
            listUnitsException = IllegalStateException(
                "permission denied for table practice_units",
            ),
        )
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )

        viewModel.onAuthStateChanged(
            AuthState.SignedIn(
                userId = "user-1",
                userEmail = "logan@example.com",
            ),
        )
        advanceUntilIdle()

        assertEquals(
            PlannerStatus.SchemaNotReady,
            viewModel.uiState.value.status,
        )
    }

    @Test
    fun signOutClearsLoadedPlanningState() = runTest {
        val repositories = FakePlannerRepositories()
        repositories.units += sampleUnit()
        repositories.sessions += sampleSession()
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )

        viewModel.onAuthStateChanged(
            AuthState.SignedIn(
                userId = "user-1",
                userEmail = "logan@example.com",
            ),
        )
        advanceUntilIdle()
        viewModel.editUnit("unit-1")

        viewModel.onAuthStateChanged(AuthState.SignedOut)

        assertTrue(viewModel.uiState.value.units.isEmpty())
        assertTrue(viewModel.uiState.value.sessions.isEmpty())
        assertEquals(PracticeUnitEditorState(), viewModel.uiState.value.unitEditor)
        assertEquals(PracticeSessionEditorState(), viewModel.uiState.value.sessionEditor)
        assertEquals(null, viewModel.uiState.value.status)
    }

    @Test
    fun signedOutSaveAttemptPromptsForSignIn() = runTest {
        val repositories = FakePlannerRepositories()
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )

        viewModel.updateUnitTitle("Distance wedges")
        viewModel.saveUnit()
        advanceUntilIdle()

        assertTrue(repositories.savedUnitDrafts.isEmpty())
        val signedOutStatus = viewModel.uiState.value.status
        assertEquals(
            "Sign in before changing practice plans.",
            (signedOutStatus as? PlannerStatus.Notification)?.text,
        )
    }

    @Test
    fun saveUnitWithBlankTitlePopulatesErrorAndAborts() = runTest {
        val repositories = FakePlannerRepositories()
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        advanceUntilIdle()

        viewModel.updateInstructionText(0, "Hit some balls")
        viewModel.saveUnit()
        advanceUntilIdle()

        assertTrue(repositories.savedUnitDrafts.isEmpty())
        val titleError = viewModel.uiState.value.unitEditor.titleError
        assertTrue("Expected titleError to be non-null", titleError != null)
    }

    @Test
    fun editingUnitTitleClearsItsError() = runTest {
        val repositories = FakePlannerRepositories()
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        advanceUntilIdle()

        viewModel.updateInstructionText(0, "Do something")
        viewModel.saveUnit()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.unitEditor.titleError != null)

        viewModel.updateUnitTitle("Wedge work")
        assertEquals(null, viewModel.uiState.value.unitEditor.titleError)
    }

    @Test
    fun isUnitEditorDirtyTogglesCorrectly() = runTest {
        val repositories = FakePlannerRepositories()
        repositories.units += sampleUnit()
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isUnitEditorDirty)

        viewModel.editUnit("unit-1")
        assertEquals(false, viewModel.uiState.value.isUnitEditorDirty)

        viewModel.updateUnitTitle("Changed title")
        assertEquals(true, viewModel.uiState.value.isUnitEditorDirty)
    }

    @Test
    fun deleteUnitRemovesFromList() = runTest {
        val repositories = FakePlannerRepositories()
        repositories.units += sampleUnit()
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.units.size)

        viewModel.deleteUnit("unit-1")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.units.isEmpty())
        val status = viewModel.uiState.value.status
        assertTrue("Expected Notification with 'Deleted'", status is PlannerStatus.Notification && status.text.contains("Deleted"))
    }

    @Test
    fun duplicateUnitSetsDuplicatedUnitId() = runTest {
        val repositories = FakePlannerRepositories()
        repositories.units += sampleUnit()
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        advanceUntilIdle()
        assertEquals(null, viewModel.uiState.value.duplicatedUnitId)

        viewModel.duplicateUnit("unit-1")
        advanceUntilIdle()

        val duplicatedId = viewModel.uiState.value.duplicatedUnitId
        assertTrue("Expected duplicatedUnitId to be set", duplicatedId != null)
        assertEquals(2, viewModel.uiState.value.units.size)
    }

    @Test
    fun hasLoadedFlipsAfterFirstSuccessfulRefresh() = runTest {
        val repositories = FakePlannerRepositories()
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )

        assertEquals(false, viewModel.uiState.value.hasLoaded)

        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.hasLoaded)
    }

    @Test
    fun hasLoadedFlipsAfterFailedRefresh() = runTest {
        val repositories = FakePlannerRepositories(
            listUnitsException = RuntimeException("Network error"),
        )
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )

        assertEquals(false, viewModel.uiState.value.hasLoaded)

        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        advanceUntilIdle()

        assertEquals(true, viewModel.uiState.value.hasLoaded)
        assertEquals(false, viewModel.uiState.value.isLoading)
    }

    @Test
    fun signOutInvalidatesInFlightRefresh() = runTest {
        val repositories = FakePlannerRepositories(listDelayMs = 50)
        repositories.units += sampleUnit()
        repositories.sessions += sampleSession()
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )

        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        assertTrue(viewModel.uiState.value.isLoading)
        assertEquals(false, viewModel.uiState.value.hasLoaded)

        viewModel.onAuthStateChanged(AuthState.SignedOut)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.units.isEmpty())
        assertTrue(viewModel.uiState.value.sessions.isEmpty())
        assertEquals(false, viewModel.uiState.value.hasLoaded)
        assertEquals(null, viewModel.uiState.value.status)
    }

    @Test
    fun intBallCountStepperUpdatesInstructionAndPersistsOnSave() = runTest {
        val repositories = FakePlannerRepositories()
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        advanceUntilIdle()

        viewModel.updateUnitTitle("Putting drills")
        viewModel.updateInstructionText(0, "Hit 5ft putts")
        viewModel.updateInstructionBallCount(0, 12) // Int overload from CountStepper
        viewModel.saveUnit()
        advanceUntilIdle()

        assertEquals(12, repositories.savedUnitDrafts.single().instructions.single().ballCount)
        assertEquals("12", viewModel.uiState.value.unitEditor.instructions.first().ballCount)
    }

    @Test
    fun intBallCountStepperClampsToZeroMin() = runTest {
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = FakePlannerRepositories().toDataFoundation(),
        )

        viewModel.updateInstructionBallCount(0, 0) // exactly at min
        assertEquals("0", viewModel.uiState.value.unitEditor.instructions.first().ballCount)

        viewModel.updateInstructionBallCount(0, 5)
        assertEquals("5", viewModel.uiState.value.unitEditor.instructions.first().ballCount)
    }

    @Test
    fun settingAndClearingInstructionClubFlowsThroughToSavedPayload() = runTest {
        val repositories = FakePlannerRepositories()
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        advanceUntilIdle()

        // Set a per-instruction club and save.
        viewModel.updateUnitTitle("Wedge ladder")
        viewModel.updateInstructionText(0, "Hit gap wedges")
        viewModel.updateInstructionClubCode(0, "gap_wedge")
        viewModel.saveUnit()
        advanceUntilIdle()

        assertEquals("gap_wedge", repositories.savedUnitDrafts.last().instructions.single().clubCode)
        assertEquals("gap_wedge", viewModel.uiState.value.unitEditor.instructions.first().clubCode)

        // Clear the per-instruction club back to "use default" and save again.
        viewModel.updateInstructionClubCode(0, "")
        viewModel.saveUnit()
        advanceUntilIdle()

        assertEquals(null, repositories.savedUnitDrafts.last().instructions.single().clubCode)
        assertEquals("", viewModel.uiState.value.unitEditor.instructions.first().clubCode)
    }

    @Test
    fun moveInstructionReordersToArbitraryIndex() = runTest {
        val repositories = FakePlannerRepositories()
        repositories.units += sampleUnit().copy(
            instructions = listOf(
                PracticeInstruction(id = "instruction-1", order = 1, text = "First", ballCount = 5),
                PracticeInstruction(id = "instruction-2", order = 2, text = "Second", ballCount = 10),
                PracticeInstruction(id = "instruction-3", order = 3, text = "Third", ballCount = 15),
            ),
        )
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        advanceUntilIdle()

        viewModel.editUnit("unit-1")
        viewModel.moveInstruction(0, 2)

        val instructions = viewModel.uiState.value.unitEditor.instructions
        assertEquals(listOf("Second", "Third", "First"), instructions.map { it.text })
        assertEquals(listOf(1, 2, 3), instructions.map { it.order })
    }

    @Test
    fun intRepeatCountStepperUpdatesSessionItemAndPersistsOnSave() = runTest {
        val repositories = FakePlannerRepositories()
        repositories.units += sampleUnit()
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        advanceUntilIdle()

        viewModel.beginNewSession()
        viewModel.updateSessionName("Morning block")
        viewModel.addSessionItem()
        viewModel.updateSessionItemUnit(0, "unit-1")
        viewModel.updateSessionItemRepeatCount(0, 4) // Int overload from CountStepper
        viewModel.saveSession()
        advanceUntilIdle()

        assertEquals(4, repositories.savedSessionDrafts.single().items.single().repeatCount)
        assertEquals("4", viewModel.uiState.value.sessionEditor.items.first().repeatCount)
    }

    @Test
    fun isSessionEditorDirtyTogglesCorrectly() = runTest {
        val repositories = FakePlannerRepositories()
        repositories.units += sampleUnit()
        repositories.sessions += sampleSession()
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        advanceUntilIdle()

        assertEquals(false, viewModel.uiState.value.isSessionEditorDirty)

        viewModel.editSession("session-1")
        assertEquals(false, viewModel.uiState.value.isSessionEditorDirty)

        viewModel.updateSessionName("Changed name")
        assertEquals(true, viewModel.uiState.value.isSessionEditorDirty)
    }

    @Test
    fun nextMoveStateIsNoUnitsWhenNoDataLoaded() = runTest {
        val repositories = FakePlannerRepositories()
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "test@example.com"))
        advanceUntilIdle()

        assertEquals(NextMoveState.NoUnits, viewModel.uiState.value.nextMoveState)
    }

    @Test
    fun nextMoveStateIsUnitsNoSessionsWithUnitsOnly() = runTest {
        val repositories = FakePlannerRepositories()
        repositories.units += sampleUnit()
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "test@example.com"))
        advanceUntilIdle()

        assertEquals(NextMoveState.UnitsNoSessions, viewModel.uiState.value.nextMoveState)
    }

    @Test
    fun nextMoveStateIsBothWhenUnitsAndSessionsExist() = runTest {
        val repositories = FakePlannerRepositories()
        repositories.units += sampleUnit()
        repositories.sessions += sampleSession()
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "test@example.com"))
        advanceUntilIdle()

        assertEquals(NextMoveState.Both, viewModel.uiState.value.nextMoveState)
    }

    @Test
    fun nextMoveStateIsResumeEditingAfterSavingUnitWhenSessionsExist() = runTest {
        val repositories = FakePlannerRepositories()
        repositories.units += sampleUnit()
        repositories.sessions += sampleSession()
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "test@example.com"))
        advanceUntilIdle()

        viewModel.editUnit("unit-1")
        viewModel.updateUnitTitle("Updated title")
        viewModel.saveUnit()
        advanceUntilIdle()

        val nextMoveState = viewModel.uiState.value.nextMoveState
        assertTrue("Expected ResumeEditing", nextMoveState is NextMoveState.ResumeEditing)
        nextMoveState as NextMoveState.ResumeEditing
        assertTrue("Expected isUnit=true", nextMoveState.isUnit)
        assertEquals("Updated title", nextMoveState.entityName)
    }

    @Test
    fun saveUnitOptimisticallyUpdatesStateBeforeCoroutine() = runTest {
        val repositories = FakePlannerRepositories()
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        advanceUntilIdle()

        viewModel.updateUnitTitle("Short game")
        viewModel.updateInstructionText(0, "Hit 10 wedges")
        viewModel.updateInstructionBallCount(0, "10")
        viewModel.saveUnit()
        // Do NOT call advanceUntilIdle — check optimistic state

        val state = viewModel.uiState.value
        assertTrue("savedUnitId should be set immediately", state.savedUnitId != null)
        assertEquals(1, state.units.size)
        assertEquals("Short game", state.units.first().title)
        assertFalse("isSaving should not be set for optimistic saves", state.isSaving)

        advanceUntilIdle()
    }

    @Test
    fun saveUnitRevertsOnFailure() = runTest {
        val repositories = FakePlannerRepositories(saveUnitException = RuntimeException("Network error"))
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        advanceUntilIdle()

        viewModel.updateUnitTitle("Short game")
        viewModel.updateInstructionText(0, "Hit 10 wedges")
        viewModel.saveUnit()
        advanceUntilIdle()

        assertTrue("Units should revert to empty on failure", viewModel.uiState.value.units.isEmpty())
        val status = viewModel.uiState.value.status
        assertTrue("Expected error notification", status is PlannerStatus.Notification && status.text.contains("failed"))
    }

    @Test
    fun deleteUnitOptimisticallyRemovesBeforeCoroutine() = runTest {
        val repositories = FakePlannerRepositories()
        repositories.units += sampleUnit()
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.units.size)

        viewModel.deleteUnit("unit-1")
        // Do NOT call advanceUntilIdle — check optimistic state

        assertTrue("Unit should be removed immediately", viewModel.uiState.value.units.isEmpty())
        assertFalse("isSaving should not be set for optimistic deletes", viewModel.uiState.value.isSaving)

        advanceUntilIdle()
    }

    @Test
    fun deleteUnitForeignKeyViolationRevertsWithSpecificMessage() = runTest {
        val repositories = FakePlannerRepositories(
            deleteUnitException = RuntimeException("violates foreign key constraint"),
        )
        repositories.units += sampleUnit()
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        advanceUntilIdle()

        viewModel.deleteUnit("unit-1")
        advanceUntilIdle()

        assertEquals("Unit should be restored after FK violation", 1, viewModel.uiState.value.units.size)
        val status = viewModel.uiState.value.status
        assertTrue(
            "Expected FK violation message",
            status is PlannerStatus.Notification && status.text.contains("used by one or more sessions"),
        )
    }

    @Test
    fun saveSessionOptimisticallyUpdatesStateBeforeCoroutine() = runTest {
        val repositories = FakePlannerRepositories()
        repositories.units += sampleUnit()
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        advanceUntilIdle()

        viewModel.beginNewSession()
        viewModel.updateSessionName("Morning block")
        viewModel.addSessionItem()
        viewModel.updateSessionItemUnit(0, "unit-1")
        viewModel.saveSession()
        // Do NOT call advanceUntilIdle — check optimistic state

        val state = viewModel.uiState.value
        assertTrue("savedSessionId should be set immediately", state.savedSessionId != null)
        assertEquals(1, state.sessions.size)
        assertEquals("Morning block", state.sessions.first().name)
        assertFalse("isSaving should not be set for optimistic saves", state.isSaving)

        advanceUntilIdle()
    }

    @Test
    fun deleteSessionOptimisticallyRemovesBeforeCoroutine() = runTest {
        val repositories = FakePlannerRepositories()
        repositories.units += sampleUnit()
        repositories.sessions += sampleSession()
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        advanceUntilIdle()
        assertEquals(1, viewModel.uiState.value.sessions.size)

        viewModel.deleteSession("session-1")
        // Do NOT call advanceUntilIdle — check optimistic state

        assertTrue("Session should be removed immediately", viewModel.uiState.value.sessions.isEmpty())
        assertFalse("isSaving should not be set for optimistic deletes", viewModel.uiState.value.isSaving)

        advanceUntilIdle()
    }

    @Test
    fun firstRunStateRequiresHasLoadedToBeTrue() = runTest {
        val repositories = FakePlannerRepositories()
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )

        assertFalse("hasLoaded should be false before first auth", viewModel.uiState.value.hasLoaded)
        assertFalse(
            "isFirstRun must not be true until hasLoaded",
            viewModel.uiState.value.hasLoaded &&
                viewModel.uiState.value.units.isEmpty() &&
                viewModel.uiState.value.sessions.isEmpty(),
        )

        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "test@example.com"))
        advanceUntilIdle()

        assertTrue("hasLoaded should be true after successful refresh", viewModel.uiState.value.hasLoaded)
        assertTrue(
            "isFirstRun should be true after load with empty data",
            viewModel.uiState.value.hasLoaded &&
                viewModel.uiState.value.units.isEmpty() &&
                viewModel.uiState.value.sessions.isEmpty(),
        )
    }

    // ── Stage 3: authoring inputs ──────────────────────────────────────

    @Test
    fun successCriterionEditSavesAndRoundTripsThroughEditor() = runTest {
        val repositories = FakePlannerRepositories()
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        advanceUntilIdle()

        viewModel.updateUnitTitle("Wedge accuracy")
        viewModel.updateInstructionText(0, "Hit 10 wedges")
        viewModel.updateInstructionBallCount(0, "10")
        viewModel.updateUnitSuccessCriterion("Lands inside 3 paces")
        viewModel.saveUnit()
        advanceUntilIdle()

        assertEquals("Lands inside 3 paces", repositories.savedUnitDrafts.single().successCriterion)
        assertEquals("Lands inside 3 paces", viewModel.uiState.value.unitEditor.successCriterion)
    }

    @Test
    fun toggleSessionItemObservationTypeAddsThenRemoves() = runTest {
        val repositories = FakePlannerRepositories()
        repositories.units += sampleUnit()
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        advanceUntilIdle()

        viewModel.beginNewSession()
        viewModel.addSessionItem()
        viewModel.updateSessionItemUnit(0, "unit-1")

        viewModel.toggleSessionItemObservationType(0, ObservationType.SHAPE)
        assertEquals(
            listOf(ObservationType.SHAPE),
            viewModel.uiState.value.sessionEditor.items[0].observationTypes,
        )

        viewModel.toggleSessionItemObservationType(0, ObservationType.SHAPE)
        assertTrue(viewModel.uiState.value.sessionEditor.items[0].observationTypes.isEmpty())
    }

    @Test
    fun switchingUnitStripsOnlySuccessWhenNewUnitLacksCriterion() = runTest {
        val repositories = FakePlannerRepositories()
        repositories.units += sampleUnit().copy(id = "unit-crit", successCriterion = "Green in regulation")
        repositories.units += sampleUnit().copy(id = "unit-plain", title = "No criterion")
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        advanceUntilIdle()

        viewModel.beginNewSession()
        viewModel.addSessionItem()
        viewModel.updateSessionItemUnit(0, "unit-crit")
        viewModel.toggleSessionItemObservationType(0, ObservationType.SUCCESS)
        viewModel.toggleSessionItemObservationType(0, ObservationType.SHAPE)

        viewModel.updateSessionItemUnit(0, "unit-plain")

        val types = viewModel.uiState.value.sessionEditor.items[0].observationTypes
        assertFalse("SUCCESS must be stripped on a criterion-less unit", types.contains(ObservationType.SUCCESS))
        assertTrue("Other types survive the switch", types.contains(ObservationType.SHAPE))
    }

    @Test
    fun switchingToActionOnlyUnitClearsAllObservationTypes() = runTest {
        val repositories = FakePlannerRepositories()
        repositories.units += sampleUnit()
        // Action-only unit: a single 0-ball instruction → derivedBallCount == 0.
        repositories.units += sampleUnit().copy(
            id = "unit-action",
            title = "Setup routine",
            instructions = listOf(
                PracticeInstruction(
                    id = "instruction-action",
                    order = 1,
                    text = "Rehearse the takeaway",
                    ballCount = 0,
                ),
            ),
        )
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        advanceUntilIdle()

        viewModel.beginNewSession()
        viewModel.addSessionItem()
        viewModel.updateSessionItemUnit(0, "unit-1")
        viewModel.toggleSessionItemObservationType(0, ObservationType.SHAPE)

        // The action-only unit never offers observations, so switching to it must
        // not leave types stranded invisibly (the picker is hidden for it).
        viewModel.updateSessionItemUnit(0, "unit-action")

        assertTrue(
            "All types cleared when switching to an action-only unit",
            viewModel.uiState.value.sessionEditor.items[0].observationTypes.isEmpty(),
        )
    }

    @Test
    fun staleSuccessSaveLandsErrorOnObservationTypesSlotAndClearsWithoutErrors() = runTest {
        val repositories = FakePlannerRepositories()
        repositories.units += sampleUnit() // no criterion
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        advanceUntilIdle()

        viewModel.beginNewSession()
        viewModel.updateSessionName("Scoring block")
        viewModel.addSessionItem()
        viewModel.updateSessionItemUnit(0, "unit-1")
        // Toggle SUCCESS on after selecting the unit so the unit-switch strip
        // doesn't remove it — mimicking a stale selection reaching save.
        viewModel.toggleSessionItemObservationType(0, ObservationType.SUCCESS)
        viewModel.saveSession()
        advanceUntilIdle()

        assertTrue("Success-without-criterion must not persist", repositories.savedSessionDrafts.isEmpty())
        val item = viewModel.uiState.value.sessionEditor.items[0]
        assertTrue("Error lands on the observation-type slot", item.observationTypesError != null)
        assertEquals(null, item.unitError)
        assertEquals(null, item.withoutErrors().observationTypesError)
    }

    // ── Stage 2: archiving ─────────────────────────────────────────────

    @Test
    fun archiveSessionMovesSessionToArchivedList() = runTest {
        val repositories = FakePlannerRepositories()
        repositories.units += sampleUnit()
        repositories.sessions += sampleSession()
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        advanceUntilIdle()

        viewModel.archiveSession("session-1")
        advanceUntilIdle()

        assertTrue(
            "Session should no longer be in the default list",
            viewModel.uiState.value.sessions.none { it.id == "session-1" },
        )
        val archived = viewModel.uiState.value.archivedSessions.singleOrNull { it.id == "session-1" }
        assertTrue("Expected the session present in archivedSessions", archived != null)
        assertEquals(true, archived?.isArchived)
    }

    @Test
    fun unarchiveSessionRestoresToDefaultList() = runTest {
        val repositories = FakePlannerRepositories()
        repositories.units += sampleUnit()
        repositories.sessions += sampleSession()
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        advanceUntilIdle()

        viewModel.archiveSession("session-1")
        advanceUntilIdle()
        viewModel.unarchiveSession("session-1")
        advanceUntilIdle()

        assertTrue(
            "Session should no longer be in archivedSessions",
            viewModel.uiState.value.archivedSessions.none { it.id == "session-1" },
        )
        val restored = viewModel.uiState.value.sessions.singleOrNull { it.id == "session-1" }
        assertTrue("Expected the session back in the default list", restored != null)
        assertEquals(false, restored?.isArchived)
    }

    @Test
    fun loadArchivedSessionsPopulatesState() = runTest {
        val repositories = FakePlannerRepositories()
        repositories.sessions += sampleSession()
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.archivedSessions.isEmpty())

        repositories.sessions[0] = repositories.sessions[0].copy(
            archivedAt = Instant.parse("2026-06-16T00:00:00Z"),
        )
        viewModel.loadArchivedSessions()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.archivedSessions.size)
    }

    @Test
    fun loadArchivedSessionsHydratesInlineUnitsAddedAfterInitialRefresh() = runTest {
        val repositories = FakePlannerRepositories()
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        advanceUntilIdle()

        repositories.units += sampleUnit().copy(
            id = "unit-inline-1",
            title = "Archived inline drill",
            scopedToSessionId = "session-1",
        )
        repositories.sessions += sampleSession().copy(
            archivedAt = Instant.parse("2026-06-16T00:00:00Z"),
            items = listOf(
                PracticeSessionItem(
                    id = "session-item-1",
                    practiceUnitId = "unit-inline-1",
                    order = 1,
                    repeatCount = 1,
                ),
            ),
        )

        viewModel.loadArchivedSessions()
        advanceUntilIdle()

        assertEquals("Archived inline drill", viewModel.uiState.value.findUnit("unit-inline-1")?.title)
    }

    @Test
    fun archiveSessionOptimisticallyUpdatesBeforeReconcile() = runTest {
        val repositories = FakePlannerRepositories()
        repositories.units += sampleUnit()
        repositories.sessions += sampleSession()
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        advanceUntilIdle()

        viewModel.archiveSession("session-1")
        // Do NOT call advanceUntilIdle — check optimistic state

        assertTrue(
            "Session should be moved out of the default list immediately",
            viewModel.uiState.value.sessions.none { it.id == "session-1" },
        )
        assertTrue(
            "Session should appear in archivedSessions immediately",
            viewModel.uiState.value.archivedSessions.any { it.id == "session-1" },
        )

        advanceUntilIdle()
    }

    @Test
    fun archiveFailureRevertsBothLists() = runTest {
        val repositories = FakePlannerRepositories(archiveSessionException = RuntimeException("Network error"))
        repositories.units += sampleUnit()
        repositories.sessions += sampleSession()
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        advanceUntilIdle()

        viewModel.archiveSession("session-1")
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.sessions.size)
        assertTrue(viewModel.uiState.value.archivedSessions.isEmpty())
        val status = viewModel.uiState.value.status
        assertTrue(
            "Expected an archive-failed notification",
            status is PlannerStatus.Notification && status.text.contains("failed"),
        )
    }

    @Test
    fun findSessionResolvesArchivedSession() = runTest {
        val repositories = FakePlannerRepositories()
        repositories.units += sampleUnit()
        repositories.sessions += sampleSession()
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        advanceUntilIdle()

        viewModel.archiveSession("session-1")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.sessions.none { it.id == "session-1" })
        val found = viewModel.uiState.value.findSession("session-1")
        assertTrue("findSession should resolve an archived session", found != null)
        assertEquals(true, found?.isArchived)
    }

    @Test
    fun deleteArchivedSessionRemovesItFromArchivedList() = runTest {
        val repositories = FakePlannerRepositories()
        repositories.sessions += sampleSession().copy(archivedAt = Instant.parse("2026-06-16T00:00:00Z"))
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        advanceUntilIdle()

        viewModel.deleteSession("session-1")
        assertTrue(viewModel.uiState.value.archivedSessions.none { it.id == "session-1" })

        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.sessions.none { it.id == "session-1" })
        assertTrue(viewModel.uiState.value.archivedSessions.none { it.id == "session-1" })
        assertTrue(repositories.sessions.none { it.id == "session-1" })
    }

    @Test
    fun loadArchivedSessionsDoesNotRestoreDataAfterSignOut() = runTest {
        val repositories = FakePlannerRepositories()
        repositories.sessions += sampleSession().copy(archivedAt = Instant.parse("2026-06-16T00:00:00Z"))
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        advanceUntilIdle()

        viewModel.loadArchivedSessions()
        viewModel.onAuthStateChanged(AuthState.SignedOut)
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.archivedSessions.isEmpty())
    }

    // ── Stage 5: inline units ────────────────────────────────────────────

    @Test
    fun inlineUnitsHydratedForSessionItems() = runTest {
        val repositories = FakePlannerRepositories()
        repositories.units += sampleUnit().copy(id = "unit-inline-1", scopedToSessionId = "session-1")
        repositories.sessions += sampleSession().copy(
            items = listOf(
                PracticeSessionItem(id = "session-item-1", practiceUnitId = "unit-inline-1", order = 1, repeatCount = 1),
            ),
        )
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        advanceUntilIdle()

        assertTrue(
            "Inline unit should be excluded from units",
            viewModel.uiState.value.units.none { it.id == "unit-inline-1" },
        )
        assertEquals(1, viewModel.uiState.value.inlineUnits.size)
        assertEquals("unit-inline-1", viewModel.uiState.value.inlineUnits.single().id)
        assertTrue(viewModel.uiState.value.findUnit("unit-inline-1") != null)
    }

    @Test
    fun findUnitResolvesInlineAndLibrary() = runTest {
        val repositories = FakePlannerRepositories()
        repositories.units += sampleUnit()
        repositories.units += sampleUnit().copy(id = "unit-inline-1", scopedToSessionId = "session-1")
        repositories.sessions += sampleSession().copy(
            items = listOf(
                PracticeSessionItem(id = "session-item-1", practiceUnitId = "unit-inline-1", order = 1, repeatCount = 1),
            ),
        )
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        advanceUntilIdle()

        assertEquals("unit-1", viewModel.uiState.value.findUnit("unit-1")?.id)
        assertEquals("unit-inline-1", viewModel.uiState.value.findUnit("unit-inline-1")?.id)
        assertEquals(null, viewModel.uiState.value.findUnit("unknown"))
    }

    @Test
    fun editInlineUnitOpensEditor() = runTest {
        val repositories = FakePlannerRepositories()
        repositories.units += sampleUnit().copy(id = "unit-inline-1", title = "Inline drill", scopedToSessionId = "session-1")
        repositories.sessions += sampleSession().copy(
            items = listOf(
                PracticeSessionItem(id = "session-item-1", practiceUnitId = "unit-inline-1", order = 1, repeatCount = 1),
            ),
        )
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        advanceUntilIdle()

        viewModel.editUnit("unit-inline-1")

        assertEquals("unit-inline-1", viewModel.uiState.value.unitEditor.unitId)
        assertEquals("Inline drill", viewModel.uiState.value.unitEditor.title)
    }

    @Test
    fun saveInlineUnitEditKeepsItInlineNotLibrary() = runTest {
        val repositories = FakePlannerRepositories()
        repositories.units += sampleUnit().copy(id = "unit-inline-1", title = "Inline drill", scopedToSessionId = "session-1")
        repositories.sessions += sampleSession().copy(
            items = listOf(
                PracticeSessionItem(id = "session-item-1", practiceUnitId = "unit-inline-1", order = 1, repeatCount = 1),
            ),
        )
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        advanceUntilIdle()

        viewModel.editUnit("unit-inline-1")
        viewModel.updateUnitTitle("Renamed inline drill")
        viewModel.saveUnit()
        advanceUntilIdle()

        assertEquals(1, viewModel.uiState.value.inlineUnits.size)
        assertEquals("Renamed inline drill", viewModel.uiState.value.inlineUnits.single().title)
        assertTrue(viewModel.uiState.value.units.none { it.id == "unit-inline-1" })
        assertEquals("Renamed inline drill", viewModel.uiState.value.unitEditor.title)
        assertEquals("unit-inline-1", viewModel.uiState.value.unitEditor.unitId)
    }

    @Test
    fun saveInlineUnitEditPreservesDirtySessionDraft() = runTest {
        val repositories = FakePlannerRepositories()
        repositories.units += sampleUnit().copy(id = "unit-inline-1", title = "Inline drill", scopedToSessionId = "session-1")
        repositories.sessions += sampleSession().copy(
            items = listOf(
                PracticeSessionItem(id = "session-item-1", practiceUnitId = "unit-inline-1", order = 1, repeatCount = 1),
            ),
        )
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        advanceUntilIdle()

        viewModel.editSession("session-1")
        viewModel.updateSessionName("Unsaved session name")
        viewModel.updateSessionNotes("Unsaved session notes")
        viewModel.editUnit("unit-inline-1")
        viewModel.updateUnitTitle("Renamed inline drill")
        viewModel.saveUnit()
        advanceUntilIdle()

        assertEquals("Unsaved session name", viewModel.uiState.value.sessionEditor.name)
        assertEquals("Unsaved session notes", viewModel.uiState.value.sessionEditor.notes)
    }

    @Test
    fun promoteUnitMovesInlineToLibrary() = runTest {
        val repositories = FakePlannerRepositories()
        repositories.units += sampleUnit().copy(id = "unit-inline-1", title = "Inline drill", scopedToSessionId = "session-1")
        repositories.sessions += sampleSession().copy(
            items = listOf(
                PracticeSessionItem(id = "session-item-1", practiceUnitId = "unit-inline-1", order = 1, repeatCount = 1),
            ),
        )
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        advanceUntilIdle()

        viewModel.promoteUnit("unit-inline-1")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.inlineUnits.none { it.id == "unit-inline-1" })
        val promoted = viewModel.uiState.value.units.singleOrNull { it.id == "unit-inline-1" }
        assertTrue("Promoted unit should be in the library", promoted != null)
        assertEquals(false, promoted?.isInline)
        assertTrue(
            "Session should still reference the promoted unit",
            viewModel.uiState.value.sessions.single { it.id == "session-1" }.items.any { it.practiceUnitId == "unit-inline-1" },
        )
    }

    @Test
    fun promoteUnitOptimisticThenReconciles() = runTest {
        val repositories = FakePlannerRepositories()
        repositories.units += sampleUnit().copy(id = "unit-inline-1", scopedToSessionId = "session-1")
        repositories.sessions += sampleSession().copy(
            items = listOf(
                PracticeSessionItem(id = "session-item-1", practiceUnitId = "unit-inline-1", order = 1, repeatCount = 1),
            ),
        )
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        advanceUntilIdle()

        viewModel.promoteUnit("unit-inline-1")
        // Do NOT call advanceUntilIdle — check optimistic state

        assertTrue(viewModel.uiState.value.inlineUnits.none { it.id == "unit-inline-1" })
        assertTrue(viewModel.uiState.value.units.any { it.id == "unit-inline-1" })

        advanceUntilIdle()
    }

    @Test
    fun promoteFailureRevertsBothLists() = runTest {
        val repositories = FakePlannerRepositories(promoteUnitException = RuntimeException("Network error"))
        repositories.units += sampleUnit().copy(id = "unit-inline-1", scopedToSessionId = "session-1")
        repositories.sessions += sampleSession().copy(
            items = listOf(
                PracticeSessionItem(id = "session-item-1", practiceUnitId = "unit-inline-1", order = 1, repeatCount = 1),
            ),
        )
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        advanceUntilIdle()

        viewModel.promoteUnit("unit-inline-1")
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value.units.none { it.id == "unit-inline-1" })
        assertEquals(1, viewModel.uiState.value.inlineUnits.size)
        val status = viewModel.uiState.value.status
        assertTrue(
            "Expected a promote-failed notification",
            status is PlannerStatus.Notification && status.text.contains("failed"),
        )
    }

    @Test
    fun duplicateSessionWithInlineUnitHydratesCopy() = runTest {
        val repositories = FakePlannerRepositories()
        repositories.units += sampleUnit().copy(id = "unit-inline-1", scopedToSessionId = "session-1")
        repositories.sessions += sampleSession().copy(
            items = listOf(
                PracticeSessionItem(id = "session-item-1", practiceUnitId = "unit-inline-1", order = 1, repeatCount = 1),
            ),
        )
        val viewModel = PracticePlannerViewModel(
            environment = baselineEnvironment(),
            dataFoundation = repositories.toDataFoundation(),
        )
        viewModel.onAuthStateChanged(AuthState.SignedIn(userId = "user-1", userEmail = "logan@example.com"))
        advanceUntilIdle()

        viewModel.duplicateSession("session-1")
        advanceUntilIdle()

        val duplicatedId = viewModel.uiState.value.duplicatedSessionId
        assertTrue("Expected duplicatedSessionId to be set", duplicatedId != null)
        assertEquals(2, viewModel.uiState.value.inlineUnits.size)
    }
}

private class FakePlannerRepositories(
    private val listUnitsException: Throwable? = null,
    private val saveUnitException: Throwable? = null,
    private val deleteUnitException: Throwable? = null,
    private val archiveSessionException: Throwable? = null,
    private val promoteUnitException: Throwable? = null,
    private val listDelayMs: Long = 0L,
) {
    val units = mutableListOf<PracticeUnit>()
    val sessions = mutableListOf<PracticeSession>()
    val savedUnitDrafts = mutableListOf<PracticeUnitDraft>()
    val savedSessionDrafts = mutableListOf<PracticeSessionDraft>()
    var listUnitsCallCount = 0
    var listSessionsCallCount = 0

    val unitRepository: PracticeUnitRepository = object : PracticeUnitRepository() {
        override suspend fun list(): List<PracticeUnit> {
            listUnitsCallCount += 1
            if (listDelayMs > 0) delay(listDelayMs)
            listUnitsException?.let { throw it }
            // Mirrors Stage 4's repository choke point: Inline Units are excluded
            // from the library list and reached only by id.
            return units.filter { it.scopedToSessionId == null }
        }

        override suspend fun get(id: String): PracticeUnit? = units.firstOrNull { it.id == id }

        override suspend fun persist(validated: PracticeUnitDraft, unitId: String?): PracticeUnit {
            saveUnitException?.let { throw it }
            savedUnitDrafts += validated
            // Stage 4 D5: editing an existing unit preserves its scope — an Inline
            // Unit being edited stays inline through save.
            val existingScopedSessionId = unitId?.let { id -> units.firstOrNull { it.id == id }?.scopedToSessionId }
            val unit = PracticeUnit(
                id = unitId ?: "unit-${units.size + 1}",
                title = validated.title,
                instructions = validated.instructions.mapIndexed { index, instruction ->
                    PracticeInstruction(
                        id = "instruction-$index",
                        order = index + 1,
                        text = instruction.text,
                        ballCount = instruction.ballCount,
                        clubCode = instruction.clubCode,
                    )
                },
                notes = validated.notes,
                focus = validated.focus,
                defaultClubCode = validated.defaultClubCode,
                successCriterion = validated.successCriterion,
                createdAt = Instant.parse("2026-06-15T00:00:00Z"),
                updatedAt = Instant.parse("2026-06-15T00:00:00Z"),
                scopedToSessionId = existingScopedSessionId,
            )
            units.removeAll { existing -> existing.id == unit.id }
            units += unit
            return unit
        }

        override suspend fun setScopedSession(id: String, sessionId: String?): PracticeUnit {
            promoteUnitException?.let { throw it }
            val existing = units.first { it.id == id }
            val updated = existing.copy(scopedToSessionId = sessionId)
            units.removeAll { unit -> unit.id == id }
            units += updated
            return updated
        }

        override suspend fun delete(id: String) {
            deleteUnitException?.let { throw it }
            units.removeAll { unit -> unit.id == id }
        }
    }

    val sessionRepository: PracticeSessionRepository = object : PracticeSessionRepository() {
        override suspend fun list(): List<PracticeSession> {
            listSessionsCallCount += 1
            if (listDelayMs > 0) delay(listDelayMs)
            return sessions.filter { !it.isArchived }
        }

        override suspend fun listArchived(): List<PracticeSession> = sessions.filter { it.isArchived }

        override suspend fun get(id: String): PracticeSession? = sessions.firstOrNull { it.id == id }

        override suspend fun persist(validated: PracticeSessionDraft, sessionId: String?): PracticeSession {
            savedSessionDrafts += validated
            val session = PracticeSession(
                id = sessionId ?: "session-${sessions.size + 1}",
                name = validated.name,
                items = validated.items.mapIndexed { index, item ->
                    PracticeSessionItem(
                        id = "session-item-$index",
                        practiceUnitId = item.practiceUnitId,
                        order = index + 1,
                        repeatCount = item.repeatCount,
                        clubCode = item.clubCode,
                        notes = item.notes,
                        focusCue = item.focusCue,
                        observationTypes = item.observationTypes,
                    )
                },
                notes = validated.notes,
                createdAt = Instant.parse("2026-06-15T00:00:00Z"),
                updatedAt = Instant.parse("2026-06-15T00:00:00Z"),
            )
            sessions.removeAll { existing -> existing.id == session.id }
            sessions += session
            return session
        }

        override suspend fun setArchived(id: String, archivedAt: Instant?): PracticeSession {
            archiveSessionException?.let { throw it }
            val existing = sessions.first { it.id == id }
            val updated = existing.copy(archivedAt = archivedAt)
            sessions.removeAll { it.id == id }
            sessions += updated
            return updated
        }

        override suspend fun duplicate(id: String): PracticeSession {
            val source = sessions.first { it.id == id }
            val newId = "session-dup-${sessions.size + 1}"
            // Mirrors Stage 4's deep copy: an item whose unit is an Inline Unit
            // owned by the source session gets its own clone owned by the copy;
            // library references are shared, not cloned.
            val copiedItems = source.items.map { item ->
                val unit = units.firstOrNull { it.id == item.practiceUnitId }
                if (unit != null && unit.scopedToSessionId == id) {
                    val clone = unit.copy(id = "unit-inline-dup-${units.size + 1}", scopedToSessionId = newId)
                    units += clone
                    item.copy(id = "session-item-dup-${units.size}", practiceUnitId = clone.id)
                } else {
                    item
                }
            }
            val copy = source.copy(
                id = newId,
                items = copiedItems,
                archivedAt = null,
            )
            sessions += copy
            return copy
        }

        override suspend fun delete(id: String) {
            sessions.removeAll { it.id == id }
        }
    }

    val practiceLibrary = DefaultPracticeLibrary(unitRepository, sessionRepository)

    val clubRepository: ClubRepository = object : ClubRepository {
        override suspend fun listCatalog() = emptyList<com.loganmartlew.rangework.shared.model.Club>()
        override suspend fun getEnabledClubCodes() = emptySet<String>()
        override suspend fun setClubEnabled(code: String, enabled: Boolean) = Unit
    }

    val tagRepository: com.loganmartlew.rangework.shared.repository.TagRepository =
        object : com.loganmartlew.rangework.shared.repository.TagRepository {
            override suspend fun list() = emptyList<com.loganmartlew.rangework.shared.model.Tag>()
            override suspend fun createOrGet(name: String) =
                com.loganmartlew.rangework.shared.model.Tag(
                    id = "tag-$name",
                    code = name.lowercase(),
                    displayName = name,
                    isDefault = false,
                )
            override suspend fun rename(tagId: String, newName: String) =
                com.loganmartlew.rangework.shared.model.Tag(tagId, newName.lowercase(), newName, false)
            override suspend fun delete(tagId: String) = Unit
            override suspend fun attachmentCounts(tagId: String) =
                com.loganmartlew.rangework.shared.model.TagAttachmentCounts(0, 0)
        }

    fun toDataFoundation(): DataFoundation {
        val stubMeasurementPreferencesRepository = object : MeasurementPreferencesRepository() {
            override suspend fun get() = MeasurementPreferences.Imperial
            override suspend fun persist(validated: MeasurementPreferences) = validated
        }
        val stubRangeSessionRepository = StubRangeSessionRepository()
        return DataFoundation(
            practiceLibrary = practiceLibrary,
            measurementPreferencesRepository = stubMeasurementPreferencesRepository,
            clubRepository = clubRepository,
            tagRepository = tagRepository,
            rangeSessionRepository = stubRangeSessionRepository,
            rangeSessionRecorder = DefaultRangeSessionRecorder(stubRangeSessionRepository),
            accountDeletionRepository = object : AccountDeletionRepository {
                override suspend fun deleteAccount() = Unit
            },
        )
    }
}

private class StubRangeSessionRepository : RangeSessionRepository {
    override suspend fun start(sessionId: String): RangeSession =
        error("Not implemented in stub")

    override suspend fun getSession(rangeSessionId: String): RangeSession? = null

    override suspend fun listActiveSessions(): List<ActiveRangeSessionSummary> = emptyList()

    override suspend fun listCompletedSessions(sessionId: String): List<CompletedRangeSessionSummary> = emptyList()

    override suspend fun setStepsCompletion(
        rangeSessionId: String,
        stepIndices: List<Int>,
        completed: Boolean,
    ): RangeSession = error("Not implemented in stub")

    override suspend fun overrideStepClubs(
        rangeSessionId: String,
        stepIndices: List<Int>,
        clubCode: String,
    ): RangeSession = error("Not implemented in stub")

    override suspend fun finishSession(rangeSessionId: String): RangeSession =
        error("Not implemented in stub")

    override suspend fun abandonSession(rangeSessionId: String) = Unit

    override suspend fun saveSessionNote(rangeSessionId: String, note: String?): RangeSession =
        error("Not implemented in stub")

    override suspend fun saveBlockResult(
        rangeSessionId: String,
        unitIndex: Int,
        result: BlockResult,
    ): RangeSession = error("Not implemented in stub")

    override suspend fun listObservations(rangeSessionId: String): List<Observation> = emptyList()

    override suspend fun upsertObservation(
        rangeSessionId: String,
        stepIndex: Int,
        values: Map<String, String>,
    ): Observation = error("Not implemented in stub")

    override suspend fun deleteObservations(rangeSessionId: String, stepIndices: List<Int>) = Unit

    override suspend fun recordTimeEntry(rangeSessionId: String, enteredAt: kotlinx.datetime.Instant) = Unit

    override suspend fun closeTimeEntry(
        rangeSessionId: String,
        enteredAt: kotlinx.datetime.Instant,
        exitedAt: kotlinx.datetime.Instant,
    ) = Unit

    override suspend fun getElapsedSeconds(rangeSessionId: String): Long = 0L

    override suspend fun hasActiveSessionsForTemplate(sessionId: String): Boolean = false
}

private fun sampleUnit(): PracticeUnit = PracticeUnit(
    id = "unit-1",
    title = "Wedge ladder",
    instructions = listOf(
        PracticeInstruction(
            id = "instruction-1",
            order = 1,
            text = "Hit 10 wedges",
            ballCount = 10,
        ),
    ),
    createdAt = Instant.parse("2026-06-15T00:00:00Z"),
    updatedAt = Instant.parse("2026-06-15T00:00:00Z"),
)

private fun sampleSession(): PracticeSession = PracticeSession(
    id = "session-1",
    name = "Scoring block",
    items = listOf(
        PracticeSessionItem(
            id = "session-item-1",
            practiceUnitId = "unit-1",
            order = 1,
            repeatCount = 2,
            clubCode = "lob_wedge",
            notes = "Start here",
        ),
    ),
    createdAt = Instant.parse("2026-06-15T00:00:00Z"),
    updatedAt = Instant.parse("2026-06-15T00:00:00Z"),
)
