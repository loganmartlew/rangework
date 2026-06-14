package com.loganmartlew.rangework.shared.auth

import com.loganmartlew.rangework.shared.data.SupabaseEndpointConfig
import com.loganmartlew.rangework.shared.data.createRangeworkSupabaseClient
import com.loganmartlew.rangework.shared.usecase.ObserveAuthStateUseCase
import com.loganmartlew.rangework.shared.usecase.RestoreAuthSessionUseCase
import com.loganmartlew.rangework.shared.usecase.SignInWithGoogleIdTokenUseCase
import com.loganmartlew.rangework.shared.usecase.SignOutUseCase

data class AuthFoundation(
    val observeAuthStateUseCase: ObserveAuthStateUseCase,
    val restoreAuthSessionUseCase: RestoreAuthSessionUseCase,
    val signInWithGoogleIdTokenUseCase: SignInWithGoogleIdTokenUseCase,
    val signOutUseCase: SignOutUseCase,
)

fun createAuthFoundation(config: SupabaseEndpointConfig): AuthFoundation? {
    if (!config.isConfigured) {
        return null
    }

    val repository = SupabaseAuthRepository(
        client = createRangeworkSupabaseClient(config),
    )

    return AuthFoundation(
        observeAuthStateUseCase = ObserveAuthStateUseCase(repository),
        restoreAuthSessionUseCase = RestoreAuthSessionUseCase(repository),
        signInWithGoogleIdTokenUseCase = SignInWithGoogleIdTokenUseCase(repository),
        signOutUseCase = SignOutUseCase(repository),
    )
}
