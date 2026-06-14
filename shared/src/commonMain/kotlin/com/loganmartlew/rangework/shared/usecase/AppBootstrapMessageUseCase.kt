package com.loganmartlew.rangework.shared.usecase

import com.loganmartlew.rangework.shared.config.baselineEnvironment
import com.loganmartlew.rangework.shared.platform.currentPlatformName

data class AppBootstrapMessage(
    val headline: String,
    val detail: String,
)

class AppBootstrapMessageUseCase {
    operator fun invoke(): AppBootstrapMessage {
        val environment = baselineEnvironment()
        val platformName = currentPlatformName()

        return AppBootstrapMessage(
            headline = "$platformName scaffold ready",
            detail = "Shared Kotlin logic is wired for a ${environment.backendLabel} backend with ${environment.authProviderLabel} as the launch auth path.",
        )
    }
}
