package com.loganmartlew.rangework.shared.auth

import kotlinx.coroutines.flow.Flow

interface AuthRepository {
    val authStates: Flow<AuthState>

    suspend fun restoreSession(): AuthState

    suspend fun signInWithGoogleIdToken(
        idToken: String,
        nonce: String? = null,
        accessToken: String? = null,
    ): AuthState

    suspend fun signOut()
}
