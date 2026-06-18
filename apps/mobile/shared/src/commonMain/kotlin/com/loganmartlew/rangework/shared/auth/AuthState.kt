package com.loganmartlew.rangework.shared.auth

import kotlinx.serialization.Serializable

@Serializable
sealed interface AuthState {
    @Serializable
    data object Restoring : AuthState

    @Serializable
    data object SignedOut : AuthState

    @Serializable
    data class SignedIn(
        val userId: String,
        val userEmail: String?,
    ) : AuthState

    @Serializable
    data class Error(
        val message: String,
    ) : AuthState
}
