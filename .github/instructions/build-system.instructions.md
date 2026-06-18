---
applyTo: "package.json,pnpm-workspace.yaml,turbo.json,apps/mobile/**/*.kts,apps/mobile/gradle/**/*.toml,apps/mobile/gradle.properties,.github/workflows/**/*.yml,README.md"
---

# Build system instructions

- This repo is a pnpm/Turborepo workspace with a nested Gradle Kotlin DSL Android/Kotlin Multiplatform project under `apps/mobile`. Keep shared Android/Kotlin build configuration centralized in `apps/mobile/gradle/libs.versions.toml`.
- `apps/mobile/settings.gradle.kts` includes exactly two modules today: `:androidApp` and `:shared`. Do not add or rename those modules casually because local tooling and CI assumptions follow that layout.
- Keep the documented validation commands in `README.md` aligned with `.github/workflows/android.yml`.
- Local mobile validation that works in this repo is `Set-Location apps/mobile; .\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug`, with lint available separately through `Set-Location apps/mobile; .\gradlew.bat :shared:lintDebug :androidApp:lintDebug`.
- Root workspace validation runs through pnpm and Turbo. Preserve that orchestration unless the task is specifically about changing the monorepo toolchain.
