package com.loganmartlew.rangework.android.config

import com.loganmartlew.rangework.android.BuildConfig
import com.loganmartlew.rangework.shared.config.AppEnvironment
import com.loganmartlew.rangework.shared.config.GoogleAuthConfig
import com.loganmartlew.rangework.shared.config.baselineEnvironment
import com.loganmartlew.rangework.shared.data.SupabaseEndpointConfig

data class AndroidAppAuthConfig(
    val environment: AppEnvironment,
    val googleWebClientId: String,
)

fun baselineAndroidAppAuthConfig(): AndroidAppAuthConfig {
    val supabaseConfig = SupabaseEndpointConfig(
        projectUrl = BuildConfig.SUPABASE_URL,
        anonKey = BuildConfig.SUPABASE_ANON_KEY,
    )
    val googleAuthConfig = GoogleAuthConfig(
        webClientId = BuildConfig.GOOGLE_WEB_CLIENT_ID,
    )

    return AndroidAppAuthConfig(
        environment = baselineEnvironment(
            supabaseConfig = supabaseConfig,
            googleAuthConfig = googleAuthConfig,
        ),
        googleWebClientId = googleAuthConfig.webClientId,
    )
}
