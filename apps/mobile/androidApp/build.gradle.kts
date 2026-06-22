plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.sentry)
}

val tokenBuildTask = rootProject.tasks.named("generateUiTokens")
val tokenKotlinDir = rootProject.rootDir.resolve("../../packages/ui-tokens/generated/android/kotlin")
val tokenResDir = rootProject.rootDir.resolve("../../packages/ui-tokens/generated/android/res")

val dotEnv: Map<String, String> = rootProject.rootDir.resolve(".env")
    .takeIf { it.exists() }
    ?.readLines()
    ?.filter { it.isNotBlank() && !it.trimStart().startsWith("#") }
    ?.mapNotNull { line ->
        val idx = line.indexOf('=')
        if (idx > 0) line.substring(0, idx).trim() to line.substring(idx + 1).trim() else null
    }
    ?.toMap()
    ?: emptyMap()

fun org.gradle.api.provider.ProviderFactory.optionalBuildConfigValue(
    gradlePropertyName: String,
    environmentVariableName: String,
): String = gradleProperty(gradlePropertyName)
    .orElse(environmentVariable(environmentVariableName))
    .orElse(dotEnv[environmentVariableName] ?: "")
    .get()

fun String.asBuildConfigString(): String = buildString(length + 2) {
    append('"')
    append(this@asBuildConfigString.replace("\\", "\\\\").replace("\"", "\\\""))
    append('"')
}

val supabaseUrl = providers.optionalBuildConfigValue(
    gradlePropertyName = "rangeworkSupabaseUrl",
    environmentVariableName = "RANGEWORK_SUPABASE_URL",
)

val supabaseAnonKey = providers.optionalBuildConfigValue(
    gradlePropertyName = "rangeworkSupabaseAnonKey",
    environmentVariableName = "RANGEWORK_SUPABASE_ANON_KEY",
)

val googleWebClientId = providers.optionalBuildConfigValue(
    gradlePropertyName = "rangeworkGoogleWebClientId",
    environmentVariableName = "RANGEWORK_GOOGLE_WEB_CLIENT_ID",
)

val resolvedVersionName = providers.optionalBuildConfigValue(
    gradlePropertyName = "rangeworkVersionName",
    environmentVariableName = "RANGEWORK_VERSION_NAME",
).ifEmpty { "0.1.0" }

val resolvedVersionCode = providers.optionalBuildConfigValue(
    gradlePropertyName = "rangeworkVersionCode",
    environmentVariableName = "RANGEWORK_VERSION_CODE",
).ifEmpty { "1" }

val releaseKeystorePath = providers.optionalBuildConfigValue(
    gradlePropertyName = "rangeworkReleaseKeystorePath",
    environmentVariableName = "RANGEWORK_RELEASE_KEYSTORE_PATH",
)
val releaseKeystorePassword = providers.optionalBuildConfigValue(
    gradlePropertyName = "rangeworkReleaseKeystorePassword",
    environmentVariableName = "RANGEWORK_RELEASE_KEYSTORE_PASSWORD",
)
val releaseKeyAlias = providers.optionalBuildConfigValue(
    gradlePropertyName = "rangeworkReleaseKeyAlias",
    environmentVariableName = "RANGEWORK_RELEASE_KEY_ALIAS",
)
val releaseKeyPassword = providers.optionalBuildConfigValue(
    gradlePropertyName = "rangeworkReleaseKeyPassword",
    environmentVariableName = "RANGEWORK_RELEASE_KEY_PASSWORD",
)
val canSign = releaseKeystorePath.isNotEmpty()
    && releaseKeystorePassword.isNotEmpty()
    && releaseKeyAlias.isNotEmpty()
    && releaseKeyPassword.isNotEmpty()

android {
    namespace = "com.loganmartlew.rangework.android"
    compileSdk = libs.versions.compileSdk.get().toInt()

    sourceSets.getByName("main") {
        java.srcDir(tokenKotlinDir)
        res.srcDir(tokenResDir)
    }

    defaultConfig {
        applicationId = "com.loganmartlew.rangework.android"
        minSdk = libs.versions.minSdk.get().toInt()
        targetSdk = libs.versions.targetSdk.get().toInt()
        versionCode = resolvedVersionCode.toIntOrNull() ?: 1
        versionName = resolvedVersionName

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        buildConfigField("String", "SUPABASE_URL", supabaseUrl.asBuildConfigString())
        buildConfigField("String", "SUPABASE_ANON_KEY", supabaseAnonKey.asBuildConfigString())
        buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", googleWebClientId.asBuildConfigString())
    }

    if (canSign) {
        signingConfigs {
            create("release") {
                storeFile = file(releaseKeystorePath)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro",
            )
            if (canSign) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

sentry {
    includeSourceContext = true
    org = "logan-martlew"
    projectName = "rangework-android"
    authToken = System.getenv("SENTRY_AUTH_TOKEN") ?: dotEnv["SENTRY_AUTH_TOKEN"]
}

tasks.named("preBuild").configure {
    dependsOn(tokenBuildTask)
}

dependencies {
    implementation(projects.shared)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.core.splashscreen)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(libs.androidx.datastore.preferences)
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.compose.material3)
    implementation(libs.google.material)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.reorderable)
    implementation(libs.kotlinx.datetime)

    testImplementation(libs.junit4)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
