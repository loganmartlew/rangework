# Build system instructions

This is a pnpm/Turborepo monorepo with a nested Gradle Kotlin DSL Android/KMP project under `apps/mobile`.

## Structure

- Root pnpm workspace orchestrates all JS/TS packages via `turbo.json`.
- `apps/mobile/settings.gradle.kts` includes exactly two Gradle modules: `:androidApp` and `:shared`. Do not add or rename modules casually.
- Shared Android/Kotlin build configuration is centralized in `apps/mobile/gradle/libs.versions.toml`.

## Commands

```powershell
# Android — tests then lint (Windows)
Set-Location apps/mobile
.\gradlew.bat :shared:testDebugUnitTest :shared:testReleaseUnitTest :androidApp:testDebugUnitTest :androidApp:testReleaseUnitTest :androidApp:assembleDebug
.\gradlew.bat :shared:lintDebug :androidApp:lintDebug

# MCP server
pnpm --filter @rangework/mcp test
pnpm --filter @rangework/mcp lint

# Site
pnpm --filter @rangework/site build
pnpm --filter @rangework/site lint
```

## CI

Defined in `.github/workflows/android.yml`. Runs the pnpm/Turbo workspace build (debug variant only) after installing Android SDK platform 35 and build-tools 35.0.0 on Temurin Java 17. The release variant is validated by the manual release workflow (`.github/workflows/release.yml`).

- Module compilation targets Java 17 (`jvmToolchain(17)`). Do not change SDK or toolchain versions casually.
- Keep the documented validation command in `README.md` aligned with `.github/workflows/android.yml`.
- Gradle 9 deprecation warnings during successful builds are background noise.
