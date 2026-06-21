package com.loganmartlew.rangework.shared.data

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.functions.Functions
import io.github.jan.supabase.postgrest.Postgrest

fun createRangeworkSupabaseClient(config: SupabaseEndpointConfig): SupabaseClient {
    require(config.isConfigured) {
        "Supabase config is incomplete: ${config.missingConfigurationLabels.joinToString()}"
    }

    return createSupabaseClient(
        supabaseUrl = config.projectUrl,
        supabaseKey = config.anonKey,
    ) {
        install(Auth)
        install(Postgrest)
        install(Functions)
    }
}
