package com.loganmartlew.rangework.android.ui

import com.loganmartlew.rangework.shared.data.DataFoundation
import com.loganmartlew.rangework.shared.model.ActiveRangeSessionSummary
import com.loganmartlew.rangework.shared.model.CompletedRangeSessionSummary
import com.loganmartlew.rangework.shared.model.RangeSession
import com.loganmartlew.rangework.shared.repository.AccountDeletionRepository
import com.loganmartlew.rangework.shared.repository.ClubRepository
import com.loganmartlew.rangework.shared.repository.MeasurementPreferencesRepository
import com.loganmartlew.rangework.shared.repository.PracticeSessionRepository
import com.loganmartlew.rangework.shared.repository.PracticeUnitRepository
import com.loganmartlew.rangework.shared.repository.RangeSessionRepository
import com.loganmartlew.rangework.shared.usecase.AbandonRangeSessionUseCase
import com.loganmartlew.rangework.shared.usecase.CloseTimeEntryUseCase
import com.loganmartlew.rangework.shared.usecase.DeleteAccountUseCase
import com.loganmartlew.rangework.shared.usecase.DeletePracticeSessionUseCase
import com.loganmartlew.rangework.shared.usecase.DeletePracticeUnitUseCase
import com.loganmartlew.rangework.shared.usecase.DuplicatePracticeSessionUseCase
import com.loganmartlew.rangework.shared.usecase.DuplicateUnitUseCase
import com.loganmartlew.rangework.shared.usecase.FinishRangeSessionUseCase
import com.loganmartlew.rangework.shared.usecase.GetClubCatalogUseCase
import com.loganmartlew.rangework.shared.usecase.GetElapsedSecondsUseCase
import com.loganmartlew.rangework.shared.usecase.GetEnabledClubsUseCase
import com.loganmartlew.rangework.shared.usecase.GetMeasurementPreferencesUseCase
import com.loganmartlew.rangework.shared.usecase.GetPracticeSessionUseCase
import com.loganmartlew.rangework.shared.usecase.GetPracticeUnitUseCase
import com.loganmartlew.rangework.shared.usecase.GetRangeSessionUseCase
import com.loganmartlew.rangework.shared.usecase.HasActiveRangeSessionsUseCase
import com.loganmartlew.rangework.shared.usecase.ListActiveRangeSessionsUseCase
import com.loganmartlew.rangework.shared.usecase.ListCompletedRangeSessionsUseCase
import com.loganmartlew.rangework.shared.usecase.ListPracticeSessionsUseCase
import com.loganmartlew.rangework.shared.usecase.ListPracticeUnitsUseCase
import com.loganmartlew.rangework.shared.usecase.OverrideStepClubUseCase
import com.loganmartlew.rangework.shared.usecase.RecordTimeEntryUseCase
import com.loganmartlew.rangework.shared.usecase.SaveMeasurementPreferencesUseCase
import com.loganmartlew.rangework.shared.usecase.SavePracticeSessionUseCase
import com.loganmartlew.rangework.shared.usecase.SavePracticeUnitUseCase
import com.loganmartlew.rangework.shared.usecase.SetClubEnabledUseCase
import com.loganmartlew.rangework.shared.usecase.StartRangeSessionUseCase
import com.loganmartlew.rangework.shared.usecase.ToggleStepCompleteUseCase
import com.loganmartlew.rangework.shared.usecase.UpdateLastViewedStepUseCase
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class DeleteAccountViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun initialStateIsIdle() {
        val viewModel = createViewModel(FakeAccountDeletionRepository())
        assertEquals(DeleteAccountUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun missingFoundationProducesError() = runTest {
        val viewModel = DeleteAccountViewModel(dataFoundation = null)
        viewModel.deleteAccount()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is DeleteAccountUiState.Error)
    }

    @Test
    fun successTransitionsToDeletedState() = runTest {
        val viewModel = createViewModel(FakeAccountDeletionRepository())
        viewModel.deleteAccount()
        advanceUntilIdle()
        assertEquals(DeleteAccountUiState.Deleted, viewModel.uiState.value)
    }

    @Test
    fun repositoryFailureTransitionsToError() = runTest {
        val viewModel = createViewModel(
            FakeAccountDeletionRepository(throws = RuntimeException("Network error")),
        )
        viewModel.deleteAccount()
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state is DeleteAccountUiState.Error)
        assertEquals("Network error", (state as DeleteAccountUiState.Error).message)
    }

    @Test
    fun clearErrorResetsToIdle() = runTest {
        val viewModel = createViewModel(
            FakeAccountDeletionRepository(throws = RuntimeException("err")),
        )
        viewModel.deleteAccount()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is DeleteAccountUiState.Error)
        viewModel.clearError()
        assertEquals(DeleteAccountUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun concurrentCallsAreIgnoredWhileWorking() = runTest {
        var callCount = 0
        val viewModel = createViewModel(FakeAccountDeletionRepository(onDelete = { callCount++ }))
        viewModel.deleteAccount()
        viewModel.deleteAccount()
        advanceUntilIdle()
        assertEquals(1, callCount)
    }

    private fun createViewModel(repo: FakeAccountDeletionRepository): DeleteAccountViewModel =
        DeleteAccountViewModel(dataFoundation = fakeFoundationWithDeletionRepo(repo))
}

private class FakeAccountDeletionRepository(
    private val throws: Exception? = null,
    private val onDelete: (() -> Unit)? = null,
) : AccountDeletionRepository {
    override suspend fun deleteAccount() {
        onDelete?.invoke()
        throws?.let { throw it }
    }
}

private fun fakeFoundationWithDeletionRepo(
    accountDeletionRepo: AccountDeletionRepository,
): DataFoundation {
    val emptyUnitRepo = object : PracticeUnitRepository {
        override suspend fun listPracticeUnits() = emptyList<com.loganmartlew.rangework.shared.model.PracticeUnit>()
        override suspend fun getPracticeUnit(unitId: String) = null
        override suspend fun savePracticeUnit(draft: com.loganmartlew.rangework.shared.model.PracticeUnitDraft, unitId: String?) = throw NotImplementedError()
        override suspend fun deletePracticeUnit(unitId: String) = Unit
    }
    val emptySessionRepo = object : PracticeSessionRepository {
        override suspend fun listPracticeSessions() = emptyList<com.loganmartlew.rangework.shared.model.PracticeSession>()
        override suspend fun getPracticeSession(sessionId: String) = null
        override suspend fun savePracticeSession(draft: com.loganmartlew.rangework.shared.model.PracticeSessionDraft, sessionId: String?) = throw NotImplementedError()
        override suspend fun deletePracticeSession(sessionId: String) = Unit
    }
    val emptyPrefsRepo = object : MeasurementPreferencesRepository {
        override suspend fun getMeasurementPreferences() = com.loganmartlew.rangework.shared.model.MeasurementPreferences.Imperial
        override suspend fun saveMeasurementPreferences(preferences: com.loganmartlew.rangework.shared.model.MeasurementPreferences) = preferences
    }
    val emptyClubRepo = object : ClubRepository {
        override suspend fun listCatalog() = emptyList<com.loganmartlew.rangework.shared.model.Club>()
        override suspend fun getEnabledClubCodes() = emptySet<String>()
        override suspend fun setClubEnabled(code: String, enabled: Boolean) = Unit
    }
    val nopRangeRepo = object : RangeSessionRepository {
        override suspend fun startSession(rangeSessionId: String, sessionId: String): RangeSession = error("nop")
        override suspend fun getSession(rangeSessionId: String): RangeSession? = null
        override suspend fun listActiveSessions(): List<ActiveRangeSessionSummary> = emptyList()
        override suspend fun listCompletedSessions(sessionId: String): List<CompletedRangeSessionSummary> = emptyList()
        override suspend fun toggleStepComplete(rangeSessionId: String, stepIndex: Int, completed: Boolean): RangeSession = error("nop")
        override suspend fun overrideStepClub(rangeSessionId: String, stepIndex: Int, clubCode: String): RangeSession = error("nop")
        override suspend fun updateLastViewedStep(rangeSessionId: String, stepIndex: Int) = Unit
        override suspend fun finishSession(rangeSessionId: String): RangeSession = error("nop")
        override suspend fun abandonSession(rangeSessionId: String) = Unit
        override suspend fun recordTimeEntry(rangeSessionId: String, enteredAt: Instant) = Unit
        override suspend fun closeTimeEntry(rangeSessionId: String, enteredAt: Instant, exitedAt: Instant) = Unit
        override suspend fun getElapsedSeconds(rangeSessionId: String): Long = 0L
        override suspend fun hasActiveSessionsForTemplate(sessionId: String): Boolean = false
    }
    return DataFoundation(
        listPracticeUnitsUseCase = ListPracticeUnitsUseCase(emptyUnitRepo),
        getPracticeUnitUseCase = GetPracticeUnitUseCase(emptyUnitRepo),
        savePracticeUnitUseCase = SavePracticeUnitUseCase(emptyUnitRepo),
        deletePracticeUnitUseCase = DeletePracticeUnitUseCase(emptyUnitRepo),
        duplicatePracticeUnitUseCase = DuplicateUnitUseCase(
            getPracticeUnitUseCase = GetPracticeUnitUseCase(emptyUnitRepo),
            savePracticeUnitUseCase = SavePracticeUnitUseCase(emptyUnitRepo),
        ),
        listPracticeSessionsUseCase = ListPracticeSessionsUseCase(emptySessionRepo),
        getPracticeSessionUseCase = GetPracticeSessionUseCase(emptySessionRepo),
        savePracticeSessionUseCase = SavePracticeSessionUseCase(emptySessionRepo),
        deletePracticeSessionUseCase = DeletePracticeSessionUseCase(emptySessionRepo),
        duplicatePracticeSessionUseCase = DuplicatePracticeSessionUseCase(
            getPracticeSessionUseCase = GetPracticeSessionUseCase(emptySessionRepo),
            savePracticeSessionUseCase = SavePracticeSessionUseCase(emptySessionRepo),
        ),
        getMeasurementPreferencesUseCase = GetMeasurementPreferencesUseCase(emptyPrefsRepo),
        saveMeasurementPreferencesUseCase = SaveMeasurementPreferencesUseCase(emptyPrefsRepo),
        getClubCatalogUseCase = GetClubCatalogUseCase(emptyClubRepo),
        getEnabledClubsUseCase = GetEnabledClubsUseCase(emptyClubRepo),
        setClubEnabledUseCase = SetClubEnabledUseCase(emptyClubRepo),
        startRangeSessionUseCase = StartRangeSessionUseCase(nopRangeRepo),
        getRangeSessionUseCase = GetRangeSessionUseCase(nopRangeRepo),
        listActiveRangeSessionsUseCase = ListActiveRangeSessionsUseCase(nopRangeRepo),
        listCompletedRangeSessionsUseCase = ListCompletedRangeSessionsUseCase(nopRangeRepo),
        toggleStepCompleteUseCase = ToggleStepCompleteUseCase(nopRangeRepo),
        overrideStepClubUseCase = OverrideStepClubUseCase(nopRangeRepo),
        updateLastViewedStepUseCase = UpdateLastViewedStepUseCase(nopRangeRepo),
        finishRangeSessionUseCase = FinishRangeSessionUseCase(nopRangeRepo),
        abandonRangeSessionUseCase = AbandonRangeSessionUseCase(nopRangeRepo),
        recordTimeEntryUseCase = RecordTimeEntryUseCase(nopRangeRepo),
        closeTimeEntryUseCase = CloseTimeEntryUseCase(nopRangeRepo),
        getElapsedSecondsUseCase = GetElapsedSecondsUseCase(nopRangeRepo),
        hasActiveRangeSessionsUseCase = HasActiveRangeSessionsUseCase(nopRangeRepo),
        deleteAccountUseCase = DeleteAccountUseCase(accountDeletionRepo),
    )
}
