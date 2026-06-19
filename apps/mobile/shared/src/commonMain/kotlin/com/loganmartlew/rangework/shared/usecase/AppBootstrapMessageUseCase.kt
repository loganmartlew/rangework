package com.loganmartlew.rangework.shared.usecase

import com.loganmartlew.rangework.shared.config.AppEnvironment
import com.loganmartlew.rangework.shared.config.baselineEnvironment
import com.loganmartlew.rangework.shared.config.isAuthConfigured
import com.loganmartlew.rangework.shared.platform.currentPlatformName

data class AppBootstrapMessage(
    val headline: String,
    val detail: String,
)

class AppBootstrapMessageUseCase {
    operator fun invoke(
        environment: AppEnvironment = baselineEnvironment(),
    ): AppBootstrapMessage {
        val platformName = currentPlatformName()
        return AppBootstrapMessage(
            headline = if (environment.isAuthConfigured) {
                "Plan sharper range sessions"
            } else {
                "Range-ready planning"
            },
            detail = if (environment.isAuthConfigured) {
                "Build repeatable units, assemble them into sessions, and pick up the same plan on $platformName."
            } else {
                "Rangework is set up for focused practice planning. Enable sign-in before plans can sync across devices."
            },
        )
    }
}
