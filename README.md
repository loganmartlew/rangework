# Rangework

Phase 1 scaffold for the Android-first golf practice session planning app described in [baseline-plan.md](baseline-plan.md).

## Modules

- `androidApp`: Jetpack Compose Android shell (`com.loganmartlew.rangework.android`)
- `shared`: Kotlin Multiplatform shared domain and data foundation (`com.loganmartlew.rangework.shared`)
- `supabase`: backend config, migrations, and seed data placeholders

## Common commands

### Windows PowerShell

```powershell
.\gradlew.bat :shared:testDebugUnitTest
.\gradlew.bat :androidApp:assembleDebug
```

### macOS / Linux

```bash
./gradlew :shared:testDebugUnitTest
./gradlew :androidApp:assembleDebug
```

## Notes

- Java 17 is the Gradle toolchain target for Android and shared JVM compilation.
- The repository is remote-first and leaves room for future Supabase schema work and local persistence.
