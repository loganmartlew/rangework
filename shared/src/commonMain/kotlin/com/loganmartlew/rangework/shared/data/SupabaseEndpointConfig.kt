package com.loganmartlew.rangework.shared.data

data class SupabaseEndpointConfig(
    val projectUrl: String = "",
    val anonKey: String = "",
) {
    val hasProjectUrl: Boolean = projectUrl.isNotBlank()
    val hasAnonKey: Boolean = anonKey.isNotBlank()
    val isConfigured: Boolean = hasProjectUrl && hasAnonKey

    val missingConfigurationLabels: List<String>
        get() = buildList {
            if (!hasProjectUrl) add("Supabase URL")
            if (!hasAnonKey) add("Supabase anon key")
        }
}
