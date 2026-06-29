# Rangework

Android-first golf practice session planning and execution app described in [baseline-plan.md](baseline-plan.md).

## Monorepo layout

This repository is a **pnpm/Turborepo monorepo**.

- `apps/mobile`: nested Gradle/Kotlin Multiplatform mobile app root
- `apps/mobile/androidApp`: Jetpack Compose Android shell (`com.loganmartlew.rangework.android`)
- `apps/mobile/shared`: Kotlin Multiplatform shared domain and data foundation (`com.loganmartlew.rangework.shared`)
- `apps/mcp`: MCP (Model Context Protocol) server on Cloudflare Workers for AI Session Creation
- `packages/design`: design-token and brand-asset pipeline package for Android and web consumers
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
.\gradlew.bat :shared:testDebugUnitTest :shared:testReleaseUnitTest :androidApp:testDebugUnitTest :androidApp:testReleaseUnitTest :androidApp:assembleDebug
```

### macOS / Linux

```bash
cd apps/mobile
./gradlew :shared:testDebugUnitTest :shared:testReleaseUnitTest :androidApp:testDebugUnitTest :androidApp:testReleaseUnitTest :androidApp:assembleDebug
```

## CI

The baseline GitHub Actions workflow lives in `.github/workflows/android.yml`. It installs pnpm dependencies, builds shared tokens, and runs the shared and Android unit-test plus debug assembly path shown above for pull requests, pushes to `main`, and manual dispatches. Release builds are validated by the separate manual release workflow.

## Releasing to Google Play

Releases are cut by manually dispatching the **Release** workflow (`.github/workflows/release.yml`) from GitHub Actions. It builds a signed AAB and APK, attaches both to a GitHub Release, and uploads the AAB to Google Play.

Dispatch inputs:

| Input     | Description                                                        |
| --------- | ----------------------------------------------------------------- |
| `version` | Semver to release (e.g. `1.0.0`). Drives the tag and `versionCode`. |
| `track`   | Google Play track: `internal` (default), `closed`, or `production`. |

`versionCode` is derived as `MAJOR*10000 + MINOR*100 + PATCH`, so bump `version` each release — Play rejects duplicate version codes. The APK is built and attached to the GitHub Release for debugging only; Play receives the AAB.

GitHub secrets required (in addition to the signing/Supabase secrets the build already uses):

| Secret                     | Where to get it                                                                                  |
| -------------------------- | ------------------------------------------------------------------------------------------------ |
| `PLAY_SERVICE_ACCOUNT_JSON` | Google Cloud → Service Accounts → JSON key, with the Google Play Android Developer API enabled and the account invited in Play Console → Users and permissions (Release to testing tracks). |

First-time setup: the Google Play Publishing API cannot create the first release on a new app, so the **first AAB must be uploaded manually** through the Play Console UI (Internal testing). After that, the workflow publishes automatically on the chosen track.

Testing-track flow for new personal developer accounts: start on **internal testing** to validate the pipeline, then move to **closed testing** with the required number of testers opted in for 14 continuous days (check the exact count in Play Console) before applying for **production** access.

## Auth config

Provide these values through your user-level `~/.gradle/gradle.properties` file or environment variables so they stay out of source control:

| Gradle property              | Environment variable             | Purpose                                               |
| ---------------------------- | -------------------------------- | ----------------------------------------------------- |
| `rangeworkSupabaseUrl`       | `RANGEWORK_SUPABASE_URL`         | Supabase project URL                                  |
| `rangeworkSupabaseAnonKey`   | `RANGEWORK_SUPABASE_ANON_KEY`    | Supabase anon key                                     |
| `rangeworkGoogleWebClientId` | `RANGEWORK_GOOGLE_WEB_CLIENT_ID` | Google OAuth web client ID used by Credential Manager |

The repo-level Supabase CLI scaffold lives in `supabase/config.toml`. Keep the Google provider `client_id` aligned with `rangeworkGoogleWebClientId`, and inject the real provider secret through your Supabase project or local CLI config rather than source control.

### Android OAuth clients (signing-certificate registration)

Credential Manager's "Sign in with Google" only issues an ID token when the calling app's **package name + signing-certificate SHA-1** is registered as an **Android OAuth 2.0 client** in the *same* Google Cloud project as `rangeworkGoogleWebClientId`. The web client ID alone is not enough — if the SHA-1 isn't registered, the account picker still appears but no credential is returned and sign-in silently fails.

Because the install path determines which key signs the app, register a SHA-1 for **each** path you distribute through (Google Cloud Console → APIs & Services → Credentials → OAuth client ID → Android, package `com.loganmartlew.rangework.android`):

| Install path                          | Signing key                     | Where to get the SHA-1                                                                 |
| ------------------------------------- | ------------------------------- | ------------------------------------------------------------------------------------- |
| Android Studio / `adb` debug install  | Local debug keystore            | `keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey` (pw `android`) |
| Google Play (any track, incl. internal) | **Play App Signing** key      | Play Console → app → Test and release → Setup → App integrity → **App signing key certificate** |
| Sideloaded release `.apk` from a GitHub Release | Upload key (the CI keystore) | Play Console → same **App integrity** page → **Upload key certificate**                |

> The most common production-only failure ("works over adb, won't sign in from Play internal testing") is a missing **App signing key** SHA-1 — Play re-signs the uploaded AAB with a key whose fingerprint differs from your upload/debug keys.

## Local development with Supabase

### Prerequisites

- [Docker Desktop](https://www.docker.com/products/docker-desktop/) (running)
- [Supabase CLI](https://supabase.com/docs/guides/cli) (`pnpm add -g supabase` or `scoop install supabase`)

### One-time setup

1. Copy `supabase/.env.example` to `supabase/.env` and fill in your Google OAuth client secret from the [Google Cloud Console](https://console.cloud.google.com/apis/credentials).
2. Copy `apps/mobile/.env.example` to `apps/mobile/.env` — the defaults are pre-configured for local development.
3. Run `supabase start` from the repo root. The first run pulls Docker images (~2-3 min). It prints the local URL, anon key, and service role key.

### Daily workflow

```bash
pnpm supabase:start
```

This starts local Supabase (idempotent) and prints the status. When you're done:

```powershell
supabase stop
```

Data persists between stops/starts. To wipe everything and start fresh:

```powershell
supabase db reset
```

### Creating migrations

```powershell
supabase migration new <name>
```

Write your SQL in the generated file under `supabase/migrations/`, then apply it locally with `supabase db reset`.

### Deploying migrations to production

Trigger the **Deploy Supabase migrations** workflow from GitHub Actions. It links to the cloud project, runs a dry-run to preview pending migrations, then pushes them.

GitHub secrets required:

| Secret                  | Where to get it                          |
| ----------------------- | ---------------------------------------- |
| `SUPABASE_PROJECT_REF`  | Your Supabase project URL slug           |
| `SUPABASE_ACCESS_TOKEN` | supabase.com → Account → Access Tokens   |
| `SUPABASE_DB_PASSWORD`  | Supabase dashboard → Settings → Database |

## Notes

- Java 17 is the Gradle toolchain target for Android and shared JVM compilation.
- `apps/mobile/gradlew` triggers `packages/design` generation automatically so Android Studio and direct Gradle builds stay in sync with token sources.
- The repository is remote-first and leaves room for future Supabase schema work and local persistence.
- `supabase/migrations` contains the auth/profile foundation, planning-data schema, club catalog, and atomic save RPCs used by the shared repositories.
- Android auth/session state is owned by a lifecycle-aware ViewModel so restore/sign-out work survives recomposition and configuration changes cleanly.
- The shared module defines serializable models, validation, use cases, repository contracts, and Supabase-backed foundations for practice units, session templates, clubs, measurement preferences, and range sessions.
- The Android app has a Material 3 Compose shell with auth-gated navigation, responsive phone/tablet layouts, and a full suite of screens for planning, session execution, settings, and an overview dashboard.
- Practice units and reusable session templates can be created, edited, deleted, reordered, and composed directly in the Android shell through the shared Supabase-backed planning foundation.
- Range sessions let users execute a practice session at the range: a snapshot is taken of the template at start time, steps are tracked individually with optimistic completion toggling, the last viewed step is persisted for resume, time on range is recorded, and sessions can be finished or abandoned.
