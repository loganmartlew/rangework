package com.loganmartlew.rangework.shared.config

data class GoogleAuthConfig(
    val webClientId: String = "",
) {
    val isConfigured: Boolean = webClientId.isNotBlank()

    val missingConfigurationLabels: List<String>
        get() = if (isConfigured) emptyList() else listOf("Google web client ID")
}
