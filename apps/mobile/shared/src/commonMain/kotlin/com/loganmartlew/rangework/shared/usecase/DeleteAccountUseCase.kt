package com.loganmartlew.rangework.shared.usecase

import com.loganmartlew.rangework.shared.repository.AccountDeletionRepository

class DeleteAccountUseCase(
    private val repository: AccountDeletionRepository,
) {
    suspend operator fun invoke() = repository.deleteAccount()
}
