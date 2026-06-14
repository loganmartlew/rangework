package com.loganmartlew.rangework.shared.auth

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.Google
import io.github.jan.supabase.auth.providers.builtin.IDToken
import io.github.jan.supabase.auth.status.SessionStatus
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

class SupabaseAuthRepository(
    private val client: SupabaseClient,
) : AuthRepository {
    override val authStates: Flow<AuthState> = client.auth.sessionStatus
        .map(::toAuthState)
        .distinctUntilChanged()

    override suspend fun restoreSession(): AuthState = client.auth.sessionStatus
        .filterNot { status -> status is SessionStatus.Initializing }
        .map(::toAuthState)
        .first()

    override suspend fun signInWithGoogleIdToken(
        idToken: String,
        accessToken: String?,
    ): AuthState {
        client.auth.signInWith(IDToken) {
            provider = Google
            this.idToken = idToken
            if (accessToken != null) {
                this.accessToken = accessToken
            }
        }

        return toAuthState(client.auth.sessionStatus.value)
    }

    override suspend fun signOut() {
        client.auth.signOut()
    }

    private fun toAuthState(status: SessionStatus): AuthState = when (status) {
        is SessionStatus.Authenticated -> AuthState.SignedIn(
            userId = status.session.user?.id ?: "unknown-user",
            userEmail = status.session.user?.email,
        )
        is SessionStatus.Initializing -> AuthState.Restoring
        is SessionStatus.NotAuthenticated -> AuthState.SignedOut
        is SessionStatus.RefreshFailure -> AuthState.Error(
            message = "Saved Supabase session refresh failed. Sign in again.",
        )
    }
}
