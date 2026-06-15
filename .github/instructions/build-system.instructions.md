---
applyTo: "build.gradle.kts,settings.gradle.kts,gradle/**/*.toml,gradle.properties,.github/workflows/**/*.yml,README.md"
---

# Build system instructions

- This repo is a Gradle Kotlin DSL Android/Kotlin Multiplatform project. Keep shared build configuration centralized where it already is, especially SDK/version values in `gradle/libs.versions.toml`.
- `settings.gradle.kts` includes exactly two modules today: `:androidApp` and `:shared`. Do not add or rename modules casually because the instructions, tests, and CI assumptions all follow that layout.
- Keep the documented validation command in `README.md` aligned with `.github/workflows/android.yml`.
- Local validation that works in this repo is `.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug`, with lint available separately through `.\gradlew.bat :shared:lintDebug :androidApp:lintDebug`.
- CI currently installs Android SDK platform 35, build-tools 35.0.0, and runs on Java 21 while module compilation targets Java 17. Preserve that compatibility unless the task is specifically about upgrading the toolchain.
