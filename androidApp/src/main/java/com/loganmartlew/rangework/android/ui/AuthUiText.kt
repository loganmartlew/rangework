package com.loganmartlew.rangework.android.ui

import com.loganmartlew.rangework.shared.auth.AuthState
import com.loganmartlew.rangework.shared.config.AppEnvironment

internal fun authStateMessage(authState: AuthState): String = when (authState) {
    AuthState.Restoring -> "Checking for your saved sign-in."
    AuthState.SignedOut -> "Sign in to start planning your next range session."
    is AuthState.Error -> authState.message
    is AuthState.SignedIn -> authState.userEmail?.let { "Signed in as $it." }
        ?: "You're signed in."
}

internal fun missingConfigMessage(environment: AppEnvironment): String =
    "Sign-in is not available in this build yet."
