package com.loganmartlew.rangework.shared.config

data class AppEnvironment(
    val backendLabel: String,
    val authProviderLabel: String,
)

fun baselineEnvironment(): AppEnvironment = AppEnvironment(
    backendLabel = "Supabase",
    authProviderLabel = "Google sign-in",
)
