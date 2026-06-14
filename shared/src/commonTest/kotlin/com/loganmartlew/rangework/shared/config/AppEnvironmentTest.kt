package com.loganmartlew.rangework.shared.config

import com.loganmartlew.rangework.shared.data.SupabaseEndpointConfig
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class AppEnvironmentTest {
    @Test
    fun missingConfigurationLabelsTrackEachUnsetAuthField() {
        val environment = baselineEnvironment()

        assertEquals(
            listOf("Supabase URL", "Supabase anon key", "Google web client ID"),
            environment.missingConfigurationLabels,
        )
    }

    @Test
    fun authConfiguredBecomesTrueWhenEveryFieldIsPresent() {
        val environment = baselineEnvironment(
            supabaseConfig = SupabaseEndpointConfig(
                projectUrl = "https://rangework.supabase.co",
                anonKey = "anon-key",
            ),
            googleAuthConfig = GoogleAuthConfig(
                webClientId = "google-web-client-id",
            ),
        )

        assertTrue(environment.isAuthConfigured)
    }
}
