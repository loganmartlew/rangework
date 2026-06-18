plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
}

val repoRootDir = rootDir.parentFile.parentFile
val pnpmExecutable = if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
    "pnpm.cmd"
} else {
    "pnpm"
}

tasks.register<Exec>("generateUiTokens") {
    group = "build setup"
    description = "Builds shared design tokens for Android and web consumers."
    workingDir = repoRootDir
    commandLine(pnpmExecutable, "--filter", "@rangework/ui-tokens", "build")
}

tasks.register<Delete>("clean") {
    delete(layout.buildDirectory)
}
