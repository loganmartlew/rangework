package com.loganmartlew.rangework.shared.usecase

import com.loganmartlew.rangework.shared.auth.AuthRepository
import com.loganmartlew.rangework.shared.auth.AuthState

class SignInWithGoogleIdTokenUseCase(
    private val authRepository: AuthRepository,
) {
    suspend operator fun invoke(
        idToken: String,
        nonce: String? = null,
        accessToken: String? = null,
    ): AuthState = authRepository.signInWithGoogleIdToken(
        idToken = idToken,
        nonce = nonce,
        accessToken = accessToken,
    )
}
