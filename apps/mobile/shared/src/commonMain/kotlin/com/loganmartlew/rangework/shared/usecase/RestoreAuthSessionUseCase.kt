package com.loganmartlew.rangework.shared.usecase

import com.loganmartlew.rangework.shared.auth.AuthRepository
import com.loganmartlew.rangework.shared.auth.AuthState

class RestoreAuthSessionUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(): AuthState = authRepository.restoreSession()
}
