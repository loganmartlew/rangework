package com.loganmartlew.rangework.shared.config

import com.loganmartlew.rangework.shared.data.SupabaseEndpointConfig

data class AppEnvironment(
    val backendLabel: String,
    val authProviderLabel: String,
    val supabaseConfig: SupabaseEndpointConfig,
    val googleAuthConfig: GoogleAuthConfig,
)

val AppEnvironment.missingConfigurationLabels: List<String>
    get() = supabaseConfig.missingConfigurationLabels + googleAuthConfig.missingConfigurationLabels

val AppEnvironment.isAuthConfigured: Boolean
    get() = missingConfigurationLabels.isEmpty()

fun baselineEnvironment(
    supabaseConfig: SupabaseEndpointConfig = SupabaseEndpointConfig(),
    googleAuthConfig: GoogleAuthConfig = GoogleAuthConfig(),
): AppEnvironment = AppEnvironment(
    backendLabel = "Supabase",
    authProviderLabel = "Google sign-in",
    supabaseConfig = supabaseConfig,
    googleAuthConfig = googleAuthConfig,
)
