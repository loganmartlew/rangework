# Rangework

Rangework is an Android-first golf practice planning app. This monorepo uses pnpm/Turborepo to house the Android/KMP app, an MCP server on Cloudflare Workers, a marketing website, shared design tokens, and Supabase backend config.

## Modules at a glance

| Directory             | Purpose                                                                          |
| --------------------- | -------------------------------------------------------------------------------- |
| `apps/mobile/`        | Android/KMP app — `androidApp` (Compose UI) + `shared` (KMP business logic)      |
| `apps/mcp/`           | MCP server (Cloudflare Worker, TypeScript) — AI-assisted practice planning tools |
| `apps/site/`          | Marketing/support website (Astro + Svelte + Tailwind)                            |
| `packages/ui-tokens/` | Shared design tokens consumed by Android build and the site                      |
| `supabase/`           | DB schema, migrations, RLS rules, Edge Function config                           |
| `README.md`           | Quick build/config reference                                                     |
| `RANGEWORK.md`        | Product overview and feature descriptions                                        |
| `baseline-plan.md`    | Architectural intent; consult before structural product decisions                |

## Build and validation

### Android/KMP

```powershell
# Windows — run tests first, then lint
Set-Location apps/mobile
.\gradlew.bat :shared:testDebugUnitTest :shared:testReleaseUnitTest :androidApp:testDebugUnitTest :androidApp:testReleaseUnitTest :androidApp:assembleDebug
.\gradlew.bat :shared:lintDebug :androidApp:lintDebug
```

```bash
# macOS/Linux
cd apps/mobile && ./gradlew :shared:testDebugUnitTest :shared:testReleaseUnitTest :androidApp:testDebugUnitTest :androidApp:testReleaseUnitTest :androidApp:assembleDebug
```

### MCP server

```powershell
pnpm --filter @rangework/mcp test
pnpm --filter @rangework/mcp lint
```

### Site

```powershell
pnpm --filter @rangework/site build
pnpm --filter @rangework/site lint
```

CI is defined in `.github/workflows/android.yml`. It runs the pnpm/Turbo workspace build (debug variant only) after installing Android SDK platform 35 and build-tools 35.0.0 on Temurin Java 17. Gradle 9 deprecation warnings are background noise.

## Secrets and environment

- Never hardcode Supabase credentials, Google OAuth secrets, or service-role keys.
- Android: runtime config comes from `BuildConfig` wired via Gradle properties or env vars. Provide `rangeworkSupabaseUrl`, `rangeworkSupabaseAnonKey`, and `rangeworkGoogleWebClientId` via `~/.gradle/gradle.properties` or matching env vars (see `README.md`).
- MCP: requires `SUPABASE_URL` and `SUPABASE_ANON_KEY` as Cloudflare Worker env vars.
- `supabase/config.toml` may hold project metadata and blank provider placeholders; real secrets stay out of source control.
- Missing auth/data config must degrade to friendly setup messaging, not crashes.

## Working conventions

- Keep Android-only UI/platform code in `apps/mobile/androidApp/`.
- Keep KMP business rules, normalization, validation, repository contracts, and Supabase mappings in `apps/mobile/shared/`.
- When schema or column names change, update matching shared repository/data-model code in the same change.
- Preserve the auth-gated flow: unauthenticated or misconfigured states must still render coherent UI and status messaging.
- Prefer targeted edits over broad rewrites. Extend the nearest existing test when behavior changes.
- Keep `README.md` and CI aligned with any command or setup changes.

## Where to look — load only what you need

Read the relevant file before diving into an area. These contain file maps, patterns, and constraints for each module.

| When working on…                                        | Read                                   |
| ------------------------------------------------------- | -------------------------------------- |
| Compose UI, navigation, ViewModels, screens, components | `.agents/instructions/android-app.md`  |
| Text styling, fonts, numeric/timer display              | `.agents/instructions/typography.md`   |
| KMP shared models, use cases, repositories, validation  | `.agents/instructions/shared.md`       |
| SQL migrations, RLS, schema, Supabase config            | `.agents/instructions/supabase.md`     |
| Gradle, version catalog, CI workflows, monorepo tooling | `.agents/instructions/build-system.md` |
| `apps/mcp` (Cloudflare Worker, MCP tools, TypeScript)   | `.agents/instructions/mcp.md`          |
| `apps/site` (Astro marketing site)                      | `.agents/instructions/site.md`         |
| Product scope, feature behaviour, domain glossary       | `RANGEWORK.md`                         |

## Agent skills

### Issue tracker

Issues live in GitHub Issues (`gh` CLI); external PRs are not a triage surface. See `docs/agents/issue-tracker.md`.

### Triage labels

Default label vocabulary (`needs-triage`, `needs-info`, `ready-for-agent`, `ready-for-human`, `wontfix`). See `docs/agents/triage-labels.md`.

### Domain docs

Multi-context monorepo — `CONTEXT-MAP.md` at root points to per-module `CONTEXT.md` files under `apps/*/`. See `docs/agents/domain.md`.
