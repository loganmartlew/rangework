# Rangework

Android-first golf practice session planning and execution app described in [baseline-plan.md](baseline-plan.md).

## Monorepo layout

This repository is a **pnpm/Turborepo monorepo**.

- `apps/mobile`: nested Gradle/Kotlin Multiplatform mobile app root
- `apps/mobile/androidApp`: Jetpack Compose Android shell (`com.loganmartlew.rangework.android`)
- `apps/mobile/shared`: Kotlin Multiplatform shared domain and data foundation (`com.loganmartlew.rangework.shared`)
- `packages/ui-tokens`: Style Dictionary design-token package for Android and future web consumers
- `supabase`: backend config, migrations, and seed data placeholders

## Common commands

### Monorepo root

```powershell
pnpm install
pnpm build
pnpm test
pnpm lint
```

### Windows PowerShell

```powershell
Set-Location apps/mobile
.\gradlew.bat :shared:testDebugUnitTest :shared:testReleaseUnitTest :androidApp:testDebugUnitTest :androidApp:testReleaseUnitTest :androidApp:assembleDebug :androidApp:assembleRelease
```

### macOS / Linux

```bash
cd apps/mobile
./gradlew :shared:testDebugUnitTest :shared:testReleaseUnitTest :androidApp:testDebugUnitTest :androidApp:testReleaseUnitTest :androidApp:assembleDebug :androidApp:assembleRelease
```

## CI

The baseline GitHub Actions workflow lives in `.github/workflows/android.yml`. It installs pnpm dependencies, builds shared tokens, and runs the same shared and Android unit-test plus assembly path used above for pull requests, pushes to `main`, and manual dispatches.

## Auth config

Provide these values through your user-level `~/.gradle/gradle.properties` file or environment variables so they stay out of source control:

| Gradle property | Environment variable | Purpose |
| --- | --- | --- |
| `rangeworkSupabaseUrl` | `RANGEWORK_SUPABASE_URL` | Supabase project URL |
| `rangeworkSupabaseAnonKey` | `RANGEWORK_SUPABASE_ANON_KEY` | Supabase anon key |
| `rangeworkGoogleWebClientId` | `RANGEWORK_GOOGLE_WEB_CLIENT_ID` | Google OAuth web client ID used by Credential Manager |

The repo-level Supabase CLI scaffold lives in `supabase/config.toml`. Keep the Google provider `client_id` aligned with `rangeworkGoogleWebClientId`, and inject the real provider secret through your Supabase project or local CLI config rather than source control.

## Notes

- Java 17 is the Gradle toolchain target for Android and shared JVM compilation.
- `apps/mobile/gradlew` triggers `packages/ui-tokens` generation automatically so Android Studio and direct Gradle builds stay in sync with token sources.
- The repository is remote-first and leaves room for future Supabase schema work and local persistence.
- `supabase/migrations` contains the auth/profile foundation, planning-data schema, club catalog, and atomic save RPCs used by the shared repositories.
- Android auth/session state is owned by a lifecycle-aware ViewModel so restore/sign-out work survives recomposition and configuration changes cleanly.
- The shared module defines serializable models, validation, use cases, repository contracts, and Supabase-backed foundations for practice units, session templates, clubs, measurement preferences, and range sessions.
- The Android app has a Material 3 Compose shell with auth-gated navigation, responsive phone/tablet layouts, and a full suite of screens for planning, session execution, settings, and an overview dashboard.
- Practice units and reusable session templates can be created, edited, deleted, reordered, and composed directly in the Android shell through the shared Supabase-backed planning foundation.
- Range sessions let users execute a practice session at the range: a snapshot is taken of the template at start time, steps are tracked individually with optimistic completion toggling, the last viewed step is persisted for resume, time on range is recorded, and sessions can be finished or abandoned.
