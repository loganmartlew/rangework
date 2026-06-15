package com.loganmartlew.rangework.shared.usecase

import com.loganmartlew.rangework.shared.config.GoogleAuthConfig
import com.loganmartlew.rangework.shared.config.baselineEnvironment
import com.loganmartlew.rangework.shared.data.SupabaseEndpointConfig
import kotlin.test.Test
import kotlin.test.assertTrue

class AppBootstrapMessageUseCaseTest {
    @Test
    fun defaultMessageUsesProductFacingCopy() {
        val message = AppBootstrapMessageUseCase().invoke()

        assertTrue(message.headline.contains("Range-ready planning"))
        assertTrue(message.detail.contains("focused practice planning"))
    }

    @Test
    fun configuredEnvironmentReportsProductReadyState() {
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

        assertTrue(message.headline.contains("Plan sharper range sessions"))
        assertTrue(message.detail.contains("pick up the same plan"))
    }
}
