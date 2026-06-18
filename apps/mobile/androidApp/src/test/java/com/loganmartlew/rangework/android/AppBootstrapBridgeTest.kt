package com.loganmartlew.rangework.android

import com.loganmartlew.rangework.shared.usecase.AppBootstrapMessageUseCase
import org.junit.Assert.assertTrue
import org.junit.Test

class AppBootstrapBridgeTest {
    @Test
    fun sharedMessageUsesProductCopy() {
        val message = AppBootstrapMessageUseCase().invoke()

        assertTrue(message.headline.contains("Range-ready planning"))
        assertTrue(message.detail.contains("plans can sync across devices"))
    }
}
