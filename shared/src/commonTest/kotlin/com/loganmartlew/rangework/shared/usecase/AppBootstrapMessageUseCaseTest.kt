package com.loganmartlew.rangework.shared.usecase

import com.loganmartlew.rangework.shared.config.GoogleAuthConfig
import com.loganmartlew.rangework.shared.config.baselineEnvironment
import com.loganmartlew.rangework.shared.data.SupabaseEndpointConfig
import kotlin.test.Test
import kotlin.test.assertTrue

class AppBootstrapMessageUseCaseTest {
    @Test
    fun detailExplainsMissingLocalAuthConfigurationByDefault() {
        val message = AppBootstrapMessageUseCase().invoke()

        assertTrue(message.detail.contains("Supabase"))
        assertTrue(message.detail.contains("Google sign-in"))
        assertTrue(message.detail.contains("rangeworkSupabaseUrl"))
    }

    @Test
    fun configuredEnvironmentReportsReadyState() {
        val message = AppBootstrapMessageUseCase().invoke(
            baselineEnvironment(
                supabaseConfig = SupabaseEndpointConfig(
                    projectUrl = "https://rangework.supabase.co",
                    anonKey = "anon-key",
                ),
                googleAuthConfig = GoogleAuthConfig(
                    webClientId = "google-web-client-id",
                ),
            ),
        )

        assertTrue(message.headline.contains("auth foundation ready"))
        assertTrue(message.detail.contains("configured"))
    }
}
