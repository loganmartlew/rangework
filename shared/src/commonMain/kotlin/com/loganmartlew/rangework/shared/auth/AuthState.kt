package com.loganmartlew.rangework.shared.auth

import kotlinx.datetime.Instant

sealed interface AuthState {
    data object SignedOut : AuthState

    data class SignedIn(
        val userId: String,
        val sessionStartedAt: Instant,
    ) : AuthState
}
