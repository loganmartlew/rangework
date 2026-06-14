package com.loganmartlew.rangework.shared.usecase

import com.loganmartlew.rangework.shared.auth.AuthRepository
import com.loganmartlew.rangework.shared.auth.AuthState

class SignInWithGoogleIdTokenUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(
        idToken: String,
        accessToken: String? = null,
    ): AuthState = authRepository.signInWithGoogleIdToken(
        idToken = idToken,
        accessToken = accessToken,
    )
}
