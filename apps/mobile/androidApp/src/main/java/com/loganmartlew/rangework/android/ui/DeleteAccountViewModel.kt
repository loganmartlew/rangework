package com.loganmartlew.rangework.android.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.loganmartlew.rangework.shared.repository.AccountDeletionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class DeleteAccountUiState {
    object Idle : DeleteAccountUiState()
    object Working : DeleteAccountUiState()
    object Deleted : DeleteAccountUiState()
    data class Error(val message: String) : DeleteAccountUiState()
}

class DeleteAccountViewModel(
    private val repository: AccountDeletionRepository?,
) : ViewModel() {
    private val _uiState = MutableStateFlow<DeleteAccountUiState>(DeleteAccountUiState.Idle)
    val uiState: StateFlow<DeleteAccountUiState> = _uiState.asStateFlow()

    fun deleteAccount() {
        val repo = repository ?: run {
            _uiState.value = DeleteAccountUiState.Error("Account deletion is not available in this build.")
            return
        }
        if (_uiState.value is DeleteAccountUiState.Working) return
        _uiState.value = DeleteAccountUiState.Working

        viewModelScope.launch {
            try {
                repo.deleteAccount()
                _uiState.value = DeleteAccountUiState.Deleted
            } catch (e: Exception) {
                _uiState.value = DeleteAccountUiState.Error(
                    e.message ?: "Account deletion failed. Please try again."
                )
            }
        }
    }

    fun clearError() {
        if (_uiState.value is DeleteAccountUiState.Error) {
            _uiState.value = DeleteAccountUiState.Idle
        }
    }

    companion object {
        fun factory(repository: AccountDeletionRepository?): ViewModelProvider.Factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    require(modelClass.isAssignableFrom(DeleteAccountViewModel::class.java)) {
                        "Unsupported ViewModel class: ${modelClass.name}"
                    }
                    return DeleteAccountViewModel(repository) as T
                }
            }
    }
}
