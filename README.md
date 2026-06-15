# Rangework

Phase 5 planning workflow implementation for the Android-first golf practice session planning app described in [baseline-plan.md](baseline-plan.md).

## Modules

- `androidApp`: Jetpack Compose Android shell (`com.loganmartlew.rangework.android`)
- `shared`: Kotlin Multiplatform shared domain and data foundation (`com.loganmartlew.rangework.shared`)
- `supabase`: backend config, migrations, and seed data placeholders

## Common commands

### Windows PowerShell

```powershell
.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug
```

### macOS / Linux

```bash
./gradlew :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug
```

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
- The repository is remote-first and leaves room for future Supabase schema work and local persistence.
- `supabase/migrations` now contains the auth/profile foundation plus the Phase 3 planning-data schema used by the shared repositories.
- Android auth/session state is owned by a lifecycle-aware ViewModel so restore/sign-out work survives recomposition and configuration changes cleanly.
- The shared module now defines serializable models, validation, repository contracts, and Supabase-backed foundations for practice units, session templates, and user measurement preferences.
- The Android app now has a Material 3 Compose shell with auth-gated navigation plus compact and expanded layout foundations for phone and tablet flows.
- Practice units and reusable session templates can now be created, edited, deleted, reordered, and composed directly in the Android shell through the shared Supabase-backed planning foundation.
