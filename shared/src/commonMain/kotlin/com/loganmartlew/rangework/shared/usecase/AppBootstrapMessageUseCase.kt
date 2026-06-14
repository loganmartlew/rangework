package com.loganmartlew.rangework.shared.usecase

import com.loganmartlew.rangework.shared.config.AppEnvironment
import com.loganmartlew.rangework.shared.config.baselineEnvironment
import com.loganmartlew.rangework.shared.config.isAuthConfigured
import com.loganmartlew.rangework.shared.config.missingConfigurationLabels
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
        val configurationDetail = if (environment.isAuthConfigured) {
            "Supabase Auth and Google sign-in are configured for the first authenticated scaffold."
        } else {
            "Missing local auth config: ${environment.missingConfigurationLabels.joinToString()}. " +
                "Set rangeworkSupabaseUrl, rangeworkSupabaseAnonKey, and rangeworkGoogleWebClientId " +
                "with Gradle properties or environment variables to enable end-to-end sign-in."
        }

        return AppBootstrapMessage(
            headline = if (environment.isAuthConfigured) {
                "$platformName auth foundation ready"
            } else {
                "$platformName auth scaffold ready"
            },
            detail = "Shared Kotlin logic is wired for a ${environment.backendLabel} backend with ${environment.authProviderLabel} as the launch auth path. $configurationDetail",
        )
    }
}
