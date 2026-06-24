package com.loganmartlew.rangework.android.ui

import com.loganmartlew.rangework.shared.repository.AccountDeletionRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
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
        val viewModel = DeleteAccountViewModel(repository = FakeAccountDeletionRepository())
        assertEquals(DeleteAccountUiState.Idle, viewModel.uiState.value)
    }

    @Test
    fun missingFoundationProducesError() = runTest {
        val viewModel = DeleteAccountViewModel(repository = null)
        viewModel.deleteAccount()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is DeleteAccountUiState.Error)
    }

    @Test
    fun successTransitionsToDeletedState() = runTest {
        val viewModel = DeleteAccountViewModel(repository = FakeAccountDeletionRepository())
        viewModel.deleteAccount()
        advanceUntilIdle()
        assertEquals(DeleteAccountUiState.Deleted, viewModel.uiState.value)
    }

    @Test
    fun repositoryFailureTransitionsToError() = runTest {
        val viewModel = DeleteAccountViewModel(
            repository = FakeAccountDeletionRepository(throws = RuntimeException("Network error")),
        )
        viewModel.deleteAccount()
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertTrue(state is DeleteAccountUiState.Error)
        assertEquals("Network error", (state as DeleteAccountUiState.Error).message)
    }

    @Test
    fun clearErrorResetsToIdle() = runTest {
        val viewModel = DeleteAccountViewModel(
            repository = FakeAccountDeletionRepository(throws = RuntimeException("err")),
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
        val viewModel = DeleteAccountViewModel(
            repository = FakeAccountDeletionRepository(onDelete = { callCount++ }),
        )
        viewModel.deleteAccount()
        viewModel.deleteAccount()
        advanceUntilIdle()
        assertEquals(1, callCount)
    }
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
