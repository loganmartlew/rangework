package com.loganmartlew.rangework.shared.usecase

import kotlin.test.Test
import kotlin.test.assertTrue

class AppBootstrapMessageUseCaseTest {
    @Test
    fun detailCapturesBaselineServices() {
        val message = AppBootstrapMessageUseCase().invoke()

        assertTrue(message.detail.contains("Supabase"))
        assertTrue(message.detail.contains("Google sign-in"))
    }
}
