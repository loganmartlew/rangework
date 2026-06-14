package com.loganmartlew.rangework.shared.usecase

import com.loganmartlew.rangework.shared.auth.AuthRepository

class SignOutUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke() = authRepository.signOut()
}
