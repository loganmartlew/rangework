# Rangework

Rangework is an Android-first golf practice planning app. This repository is a pnpm/Turborepo monorepo with `apps/mobile` for the nested Gradle/KMP app, `packages/ui-tokens` for shared design tokens, and `supabase` for backend configuration and SQL migrations.

## Build and validation

- Primary local validation on Windows: `Set-Location apps/mobile; .\gradlew.bat :shared:testDebugUnitTest :shared:testReleaseUnitTest :androidApp:testDebugUnitTest :androidApp:testReleaseUnitTest :androidApp:assembleDebug`
- Equivalent macOS/Linux: `cd apps/mobile && ./gradlew :shared:testDebugUnitTest :shared:testReleaseUnitTest :androidApp:testDebugUnitTest :androidApp:testReleaseUnitTest :androidApp:assembleDebug`
- Lint: `Set-Location apps/mobile; .\gradlew.bat :shared:lintDebug :androidApp:lintDebug`
- Run tests first, then lint.
- CI is defined in `.github/workflows/android.yml` and runs the pnpm/Turbo workspace build (debug variant only) after installing Android SDK platform 35 and build-tools 35.0.0. The release variant is validated by the manual release workflow.
- Module compilation targets Java 17; CI runs Temurin 17. Do not change SDK or toolchain versions casually.
- Gradle emits Gradle 9 deprecation warnings during successful builds — treat as background noise unless touching relevant build logic.

## Secrets and environment

- Never hardcode Supabase credentials, Google OAuth secrets, or service-role keys.
- `apps/mobile/androidApp/build.gradle.kts` wires runtime config into `BuildConfig` from Gradle properties or environment variables.
- Provide `rangeworkSupabaseUrl`, `rangeworkSupabaseAnonKey`, and `rangeworkGoogleWebClientId` via `~/.gradle/gradle.properties` or the matching environment variables documented in `README.md`.
- `supabase/config.toml` may keep project metadata and blank provider placeholders, but real provider secrets stay out of source control.
- Missing auth/data config should degrade to friendly setup messaging, not crashes.

## Codebase map

- `README.md` — quickest build/config summary.
- `baseline-plan.md` — product scope and architectural intent; consult before making structural product decisions.
- `androidApp/src/main/java/com/loganmartlew/rangework/android/MainActivity.kt` — Android entry point.
- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/RangeworkApp.kt` — main Compose app shell (auth state, planner state, navigation, phone/tablet layout).
- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/RangeworkNavigation.kt` — navigation type detection (bottom bar vs rail) and route/destination definitions.
- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/AuthViewModel.kt` — auth/session restore and sign-in/sign-out state.
- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/PracticePlannerViewModel.kt` — planning screen loading, editing, save/delete flows, setup messaging.
- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/SettingsViewModel.kt` — theme mode, measurement preferences, and club management state.
- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/screens/` — 9 screen composables: Overview, UnitList, UnitDetail, UnitEditor, SessionList, SessionDetail, SessionEditor, ManageClubs, Settings.
- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/components/` — 30+ reusable UI components (cards, FABs, pickers, dialogs, steppers, bars, etc.).
- `shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/auth` — auth state/repository/foundation code.
- `shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/data/DataFoundation.kt` and `.../auth/AuthFoundation.kt` — assemble use cases from repositories.
- `shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/repository/` — repository interfaces: `PracticeUnitRepository`, `PracticeSessionRepository`, `MeasurementPreferencesRepository`, `ClubRepository`.
- `shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/data/Supabase*Repository.kt` — Supabase-backed implementations for all four repository interfaces plus `SupabaseAuthRepository`.
- `shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/usecase/` — use cases for units, sessions, clubs, measurement preferences, auth (observe, restore, sign-in, sign-out), and app bootstrap messaging.
- `shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/model` — serializable domain models, draft models, validation helpers.
- `shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/config/` — `AppEnvironment` and `GoogleAuthConfig`.
- `shared/src/commonTest` and `androidApp/src/test` — main regression coverage for shared use cases and Android ViewModels.
- `supabase/migrations` — schema and RLS rules. Planning data centers on `practice_units`, `practice_unit_instructions`, `practice_sessions`, `practice_session_items`, `user_preferences`, `clubs` (catalog), and `user_enabled_clubs`. Includes atomic save RPCs.

## Working conventions

- Keep Android-only UI/platform code in `androidApp`.
- Keep business rules, normalization, validation, repository contracts, and Supabase mappings in `shared`.
- When schema or column names change, update matching shared repository/data-model code in the same change.
- Preserve the auth-gated flow: unauthenticated or misconfigured states must still render coherent UI and status messaging.
- Prefer targeted edits over broad rewrites. Extend the nearest existing test when behavior changes.
- Keep README and CI aligned with any command or setup changes.

## Android app (`androidApp/**`)

- `androidApp` owns Compose UI, navigation, ViewModels, Activity lifecycle wiring, Android auth integration, resources, and theming.
- Keep business rules out of composables. `RangeworkApp.kt` orchestrates state and navigation; normalization and persistence stay in shared use cases/repositories.
- Preserve the existing auth-gated flow: `AuthViewModel` drives sign-in state, `PracticePlannerViewModel` reacts to `AuthState`, and root navigation switches between sign-in and authenticated shells.
- Preserve the responsive navigation pattern: bottom bar on compact widths, navigation rail on expanded widths.
- Runtime auth configuration comes from `AndroidAppAuthConfig` and `BuildConfig`; do not hardcode Supabase or Google sign-in values in source files or resources.
- Follow the existing Material 3 Compose style and theme setup under `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/theme`.
- When changing UI or ViewModel behavior, prefer extending tests under `androidApp/src/test/java/com/loganmartlew/rangework/android`.

## Shared module (`shared/**`)

- `shared` is the Kotlin Multiplatform core. It owns domain models, draft models, validation, use cases, repository interfaces, auth/data foundations, and Supabase-backed repository adapters.
- Keep `commonMain` platform-agnostic. Android-specific integrations belong in `androidMain` or `androidApp`, not in shared common code.
- Layering: models and validation helpers feed use cases → use cases normalize input and orchestrate repositories → repository adapters map to Supabase/PostgREST.
- Put trimming, normalization, ordering fixes, and validation in shared use cases or model helpers — not repeated in the Android UI layer.
- Shared models are serialized with `kotlinx.serialization`; keep wire/storage-facing changes compatible with repository row mappings.
- When changing repository behavior or model validation, add or update focused tests in `shared/src/commonTest`.

## Supabase (`supabase/**`)

- Treat `supabase` as the source of truth for backend schema and local project configuration.
- Prefer new timestamped migration files for schema evolution; do not silently rewrite existing migration history unless the task explicitly calls for it.
- Keep row-level security and ownership rules intact. The schema scopes access through `auth.uid()`, `profiles`, and owner/user foreign keys.
- Follow existing migration patterns: explicit constraints, `updated_at` triggers via `public.set_updated_at()`, indexes on ownership/sort-order access paths.
- Planning schema changes require matching updates in `shared/src/commonMain/.../data/Supabase*Repository.kt` and related model/use-case code. Do not change SQL in isolation if Kotlin mappings depend on it.
- `supabase/config.toml` can contain project metadata and disabled provider placeholders, but never commit real provider secrets or service-role credentials.

## Build system (`build.gradle.kts`, `settings.gradle.kts`, `gradle/**`, `.github/workflows/**`)

- This is a Gradle Kotlin DSL Android/KMP project. Keep shared build configuration centralized, especially SDK/version values in `gradle/libs.versions.toml`.
- `settings.gradle.kts` includes exactly two modules: `:androidApp` and `:shared`. Do not add or rename modules casually.
- Keep the documented validation command in `README.md` aligned with `.github/workflows/android.yml`.
- CI installs Android SDK platform 35, build-tools 35.0.0, and runs on Temurin Java 17 — the same toolchain target used by module compilation. Preserve that compatibility unless specifically upgrading the toolchain.

## Typography (`androidApp/src/main/java/**/ui/**/*.kt`, `**/ui/theme/*.kt`)

### Typefaces

| Family  | Variable file   | Weights used    | Role                            |
| ------- | --------------- | --------------- | ------------------------------- |
| DM Sans | `dm_sans_*.ttf` | 300 · 400 · 500 | All UI text via MaterialTheme   |
| DM Mono | `dm_mono_*.ttf` | 400 · 500       | Numeric and timer contexts only |

Font files live in `androidApp/src/main/res/font/`. `FontFamily` declarations and all `TextStyle` definitions are in `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/theme/Type.kt`.

### Applying text styles

Standard UI text — use `MaterialTheme.typography.*`:

```kotlin
Text(text = session.name, style = MaterialTheme.typography.headlineMedium)
Text(text = step.instruction, style = MaterialTheme.typography.bodyMedium)
Text(text = "Start session", style = MaterialTheme.typography.labelLarge)
```

Numeric and timer text — use `RangeworkMono`:

```kotlin
Text(text = formatTimer(remainingSeconds), style = RangeworkMono.large, color = MaterialTheme.colorScheme.secondary)
Text(text = "20 balls", style = RangeworkMono.medium)
Text(text = "×12 reps", style = RangeworkMono.small, color = MaterialTheme.colorScheme.onSurfaceVariant)
```

### When to use DM Mono (`RangeworkMono`)

Use `RangeworkMono` when the text is any of: countdown/elapsed timer, ball/rep count, distance/carry value, percentage/rate metric, step/unit position, performance log value, settings value for a numeric field.

Use `MaterialTheme.typography` (DM Sans) when the text is any of: name/title/label, instructional prose, chip/tag/category label, navigation/button/action text, metadata/descriptive copy, section header/screen title.

**Edge cases:**

- Mixed lines (e.g. `Balls: 20`): label in `bodyMedium`, value in `RangeworkMono.medium` via `AnnotatedString` or two adjacent `Text` composables.
- Empty/placeholder states: use `bodyMedium` or `bodySmall` in `onSurfaceVariant`. Do not use mono for placeholder text.
- Input fields: use `bodyLarge` for `TextField` content regardless of whether it accepts numbers. Exception: dedicated numeric steppers may use `RangeworkMono.medium` for the displayed value only.

### Colour pairings

| Style                        | Primary colour token  | Secondary use                      |
| ---------------------------- | --------------------- | ---------------------------------- |
| `headlineLarge/Medium/Small` | `onBackground`        | —                                  |
| `titleLarge/Medium`          | `onSurface`           | `onSurfaceVariant` for de-emphasis |
| `bodyLarge/Medium`           | `onSurface`           | `onSurfaceVariant` for secondary   |
| `bodySmall`                  | `onSurfaceVariant`    | `onSurface` when prominent         |
| `labelLarge`                 | `onSurface`           | `onPrimary` when inside a button   |
| `labelMedium/Small`          | `onSurfaceVariant`    | uppercase for section headers      |
| `RangeworkMono.large`        | `secondary` (#386044) | `onSurface` for neutral metrics    |
| `RangeworkMono.medium`       | `onSurface`           | `secondary` for highlighted values |
| `RangeworkMono.small`        | `onSurfaceVariant`    | `onSurface` for prominent inline   |

### Typography rules

- Do not use `RangeworkMono` for non-numeric text.
- Do not use `MaterialTheme.typography` for timer or metric values.
- Do not hardcode `fontFamily = DmMono` at call sites — always go through `RangeworkMono.*`.
- Do not use font weights other than those declared in the `FontFamily` (300, 400, 500 for DM Sans; 400, 500 for DM Mono).
- Do not apply `textTransform` inside a `TextStyle` — apply `.uppercase()` at the call site.
- Do not use `sp` values not listed in the spec; raise for design review instead.
- Do not create anonymous `TextStyle(fontFamily = DmSans, fontSize = 15.sp)` inline — all styles must be named and defined in `Type.kt`.

### Theme integration

`RangeworkTypography` is wired into `MaterialTheme` via `RangeworkTheme`. `RangeworkMono` is a plain Kotlin object — import it directly:

```kotlin
import com.loganmartlew.rangework.android.ui.theme.RangeworkMono
```

If a new text style is needed: check whether an existing role fits first; if genuinely new, add it to `RangeworkMono` or add a named extension to `Type.kt`. Never create anonymous inline styles.
