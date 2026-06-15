# Rangework repository instructions

Rangework is an Android-first golf practice planning app. This repository is a small Kotlin monorepo with three important areas: `androidApp` for the Jetpack Compose Android shell, `shared` for Kotlin Multiplatform domain/data/auth logic, and `supabase` for backend configuration and SQL migrations. Trust this file as the default map of the repo and only search more broadly when it is missing something or has become outdated.

## Build and validation

- Primary local validation on Windows: `.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug`
- Equivalent macOS/Linux command: `./gradlew :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug`
- Android lint is available and working: `.\gradlew.bat :shared:lintDebug :androidApp:lintDebug`
- The local validation flow that succeeded here was: run the test-and-assemble command first, then run lint.
- CI is defined in `.github/workflows/android.yml` and currently runs the same test-and-assemble command after installing Android SDK platform 35 and build-tools 35.0.0.
- Module compilation targets Java 17 (`androidApp/build.gradle.kts` compile options and `shared/build.gradle.kts` `jvmToolchain(17)`), but CI currently runs on Temurin 21 successfully. Do not change SDK or toolchain versions casually.
- Gradle currently emits deprecation warnings about Gradle 9 compatibility during successful builds. Treat those as existing background noise unless your change touches the relevant build logic.

## Secrets and environment

- Never hardcode Supabase credentials, Google OAuth secrets, or service-role keys.
- `androidApp/build.gradle.kts` wires runtime config into `BuildConfig` from Gradle properties or environment variables.
- Provide `rangeworkSupabaseUrl`, `rangeworkSupabaseAnonKey`, and `rangeworkGoogleWebClientId` through user-level `~/.gradle/gradle.properties` or the matching environment variables documented in `README.md`.
- `supabase/config.toml` may keep project metadata and blank provider placeholders, but real provider secrets stay out of source control.
- Missing auth/data config should degrade to friendly setup messaging, not crashes. The current app already follows that pattern.

## Codebase map

- `README.md` is the quickest build/config summary.
- `baseline-plan.md` captures product scope and longer-term architectural intent; consult it before making structural product decisions.
- `androidApp/src/main/java/com/loganmartlew/rangework/android/MainActivity.kt` is the Android entry point.
- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/RangeworkApp.kt` is the main Compose app shell. It wires auth state, planner state, navigation, and phone/tablet layout behavior.
- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/AuthViewModel.kt` owns auth/session restore and sign-in/sign-out state.
- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/PracticePlannerViewModel.kt` owns planning screen loading, editing, save/delete flows, and setup messaging.
- `shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/auth` contains auth state/repository/foundation code.
- `shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/data/DataFoundation.kt` and `.../auth/AuthFoundation.kt` assemble use cases from repositories.
- `shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/data/Supabase*Repository.kt` map shared models to PostgREST tables.
- `shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/model` contains serializable domain models, draft models, and validation helpers.
- `shared/src/commonTest` and `androidApp/src/test` contain the main regression coverage for shared use cases and Android ViewModels.
- `supabase/migrations` defines the schema and RLS rules. Planning data currently centers on `practice_units`, `practice_unit_instructions`, `practice_sessions`, `practice_session_items`, and `user_preferences`.

## Working conventions

- Keep Android-only UI/platform code in `androidApp`.
- Keep business rules, normalization, validation, repository contracts, and Supabase mappings in `shared`.
- When schema or column names change, update the matching shared repository/data-model code in the same change.
- Preserve the auth-gated flow: unauthenticated or misconfigured states should still render coherent UI and status messaging.
- Prefer targeted edits over broad rewrites. There is already good coverage around shared use-case normalization and Android ViewModel behavior, so extend the nearest existing test when behavior changes.
- Keep README and CI aligned with any command or setup changes you introduce.
