package com.loganmartlew.rangework.shared.auth

import com.loganmartlew.rangework.shared.data.SupabaseEndpointConfig
import com.loganmartlew.rangework.shared.data.SupabaseProfileRepository
import com.loganmartlew.rangework.shared.data.createRangeworkSupabaseClient
import com.loganmartlew.rangework.shared.usecase.GetUserProfileUseCase
import com.loganmartlew.rangework.shared.usecase.ObserveAuthStateUseCase
import com.loganmartlew.rangework.shared.usecase.RestoreAuthSessionUseCase
import com.loganmartlew.rangework.shared.usecase.SignInWithGoogleIdTokenUseCase
import com.loganmartlew.rangework.shared.usecase.SignOutUseCase
import io.github.jan.supabase.SupabaseClient

data class AuthFoundation(
    val observeAuthStateUseCase: ObserveAuthStateUseCase,
    val restoreAuthSessionUseCase: RestoreAuthSessionUseCase,
    val signInWithGoogleIdTokenUseCase: SignInWithGoogleIdTokenUseCase,
    val signOutUseCase: SignOutUseCase,
    val getUserProfileUseCase: GetUserProfileUseCase,
)

fun createAuthFoundation(config: SupabaseEndpointConfig): AuthFoundation? {
    if (!config.isConfigured) {
        return null
    }

    return createAuthFoundation(
        client = createRangeworkSupabaseClient(config),
    )
}

fun createAuthFoundation(client: SupabaseClient): AuthFoundation {
    val repository = SupabaseAuthRepository(client)
    val profileRepository = SupabaseProfileRepository(client)

    return AuthFoundation(
        observeAuthStateUseCase = ObserveAuthStateUseCase(repository),
        restoreAuthSessionUseCase = RestoreAuthSessionUseCase(repository),
        signInWithGoogleIdTokenUseCase = SignInWithGoogleIdTokenUseCase(repository),
        signOutUseCase = SignOutUseCase(repository),
        getUserProfileUseCase = GetUserProfileUseCase(profileRepository),
    )
}
