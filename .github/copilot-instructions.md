# Rangework repository instructions

Rangework is an Android-first golf practice planning app. This repository is a pnpm/Turborepo monorepo with `apps/mobile` for the nested Gradle/KMP app, `packages/ui-tokens` for shared design tokens, and `supabase` for backend configuration and SQL migrations. Trust this file as the default map of the repo and only search more broadly when it is missing something or has become outdated.

## Build and validation

- Primary local validation on Windows: `Set-Location apps/mobile; .\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug`
- Equivalent macOS/Linux command: `cd apps/mobile && ./gradlew :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug`
- Android lint is available and working: `Set-Location apps/mobile; .\gradlew.bat :shared:lintDebug :androidApp:lintDebug`
- The local validation flow that succeeded here was: run the test-and-assemble command first, then run lint.
- CI is defined in `.github/workflows/android.yml` and runs `pnpm mobile:build && pnpm mobile:test && pnpm mobile:lint`, scoped to the mobile package and its dependencies only.
- Module compilation targets Java 17 (`apps/mobile/androidApp/build.gradle.kts` compile options and `apps/mobile/shared/build.gradle.kts` `jvmToolchain(17)`), and CI installs Node/pnpm before running Turbo-driven validation. Do not change SDK or toolchain versions casually.
- Gradle currently emits deprecation warnings about Gradle 9 compatibility during successful builds. Treat those as existing background noise unless your change touches the relevant build logic.

## Secrets and environment

- Never hardcode Supabase credentials, Google OAuth secrets, or service-role keys.
- `apps/mobile/androidApp/build.gradle.kts` wires runtime config into `BuildConfig` from Gradle properties or environment variables and consumes generated token outputs from `packages/ui-tokens`.
- Provide `rangeworkSupabaseUrl`, `rangeworkSupabaseAnonKey`, and `rangeworkGoogleWebClientId` through user-level `~/.gradle/gradle.properties` or the matching environment variables documented in `README.md`.
- `supabase/config.toml` may keep project metadata and blank provider placeholders, but real provider secrets stay out of source control.
- Missing auth/data config should degrade to friendly setup messaging, not crashes. The current app already follows that pattern.

## Codebase map

- `README.md` is the quickest build/config summary.
- `baseline-plan.md` captures product scope and longer-term architectural intent; consult it before making structural product decisions.
- `apps/mobile/androidApp/src/main/java/com/loganmartlew/rangework/android/MainActivity.kt` is the Android entry point.
- `apps/mobile/androidApp/src/main/java/com/loganmartlew/rangework/android/ui/RangeworkApp.kt` is the main Compose app shell. It wires auth state, planner state, navigation, and phone/tablet layout behavior.
- `apps/mobile/androidApp/src/main/java/com/loganmartlew/rangework/android/ui/AuthViewModel.kt` owns auth/session restore and sign-in/sign-out state.
- `apps/mobile/androidApp/src/main/java/com/loganmartlew/rangework/android/ui/PracticePlannerViewModel.kt` owns planning screen loading, editing, save/delete flows, and setup messaging.
- `apps/mobile/shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/auth` contains auth state/repository/foundation code.
- `apps/mobile/shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/data/DataFoundation.kt` and `.../auth/AuthFoundation.kt` assemble use cases from repositories.
- `apps/mobile/shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/data/Supabase*Repository.kt` map shared models to PostgREST tables.
- `apps/mobile/shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/model` contains serializable domain models, draft models, and validation helpers.
- `apps/mobile/shared/src/commonTest` and `apps/mobile/androidApp/src/test` contain the main regression coverage for shared use cases and Android ViewModels.
- `supabase/migrations` defines the schema and RLS rules. Planning data currently centers on `practice_units`, `practice_unit_instructions`, `practice_sessions`, `practice_session_items`, and `user_preferences`.
- `apps/mobile/androidApp/src/main/java/com/loganmartlew/rangework/android/ui/components/` contains 30+ reusable UI components (cards, FABs, pickers, dialogs, steppers, bars, etc.).
- `apps/mcp/src/index.ts` is the MCP server entry point (Cloudflare Workers fetch handler).
- `apps/mcp/src/server.ts` constructs the MCP server instance and registers tools.
- `apps/mcp/src/tools/ping.ts` defines the `ping` health-check tool.
- `apps/mcp/src/tests/` contains unit and integration tests for the MCP server.

## Working conventions

- Keep Android-only UI/platform code in `apps/mobile/androidApp`.
- Keep business rules, normalization, validation, repository contracts, and Supabase mappings in `apps/mobile/shared`.
- When schema or column names change, update the matching shared repository/data-model code in the same change.
- Preserve the auth-gated flow: unauthenticated or misconfigured states should still render coherent UI and status messaging.
- Prefer targeted edits over broad rewrites. There is already good coverage around shared use-case normalization and Android ViewModel behavior, so extend the nearest existing test when behavior changes.
- Keep README and CI aligned with any command or setup changes you introduce.
