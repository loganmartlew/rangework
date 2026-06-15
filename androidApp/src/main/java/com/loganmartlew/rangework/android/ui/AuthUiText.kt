package com.loganmartlew.rangework.android.ui

import com.loganmartlew.rangework.shared.auth.AuthState
import com.loganmartlew.rangework.shared.config.AppEnvironment
import com.loganmartlew.rangework.shared.config.missingConfigurationLabels

internal fun authStateMessage(authState: AuthState): String = when (authState) {
    AuthState.Restoring -> "Restoring any saved Supabase session."
    AuthState.SignedOut -> "No active Supabase session on this device."
    is AuthState.Error -> authState.message
    is AuthState.SignedIn -> authState.userEmail?.let { "Signed in as $it." }
        ?: "Signed in as ${authState.userId}."
}

internal fun missingConfigMessage(environment: AppEnvironment): String =
    "Auth config is incomplete: ${environment.missingConfigurationLabels.joinToString()}. " +
        "Set rangeworkSupabaseUrl, rangeworkSupabaseAnonKey, and rangeworkGoogleWebClientId to continue."
