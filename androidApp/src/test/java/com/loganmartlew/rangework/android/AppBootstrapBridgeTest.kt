package com.loganmartlew.rangework.android

import com.loganmartlew.rangework.shared.usecase.AppBootstrapMessageUseCase
import org.junit.Assert.assertTrue
import org.junit.Test

class AppBootstrapBridgeTest {
    @Test
    fun sharedMessageMentionsSupabaseBaseline() {
        val message = AppBootstrapMessageUseCase().invoke()

        assertTrue(message.detail.contains("Supabase"))
    }
}
