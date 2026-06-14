package com.loganmartlew.rangework.shared.auth

sealed interface AuthState {
    data object Restoring : AuthState

    data object SignedOut : AuthState

    data class SignedIn(
        val userId: String,
        val userEmail: String?,
    ) : AuthState

    data class Error(
        val message: String,
    ) : AuthState
}
