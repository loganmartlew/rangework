# Rangework

Phase 2 auth foundation for the Android-first golf practice session planning app described in [baseline-plan.md](baseline-plan.md).

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

## Notes

- Java 17 is the Gradle toolchain target for Android and shared JVM compilation.
- The repository is remote-first and leaves room for future Supabase schema work and local persistence.
- `supabase/migrations` now contains the auth/profile foundation migration used by the Phase 2 scaffold.
