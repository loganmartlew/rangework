---
applyTo: "androidApp/**/*.kt,androidApp/**/*.kts,androidApp/**/*.xml"
---

# Android app instructions

- `androidApp` owns Compose UI, navigation, ViewModels, Activity lifecycle wiring, Android auth integration, resources, and theming.
- Keep business rules out of composables where possible. `RangeworkApp.kt` should orchestrate state and navigation, while normalization and persistence stay in shared use cases/repositories.
- Preserve the existing auth-gated flow: `AuthViewModel` drives sign-in state, `PracticePlannerViewModel` reacts to `AuthState`, and root navigation switches between sign-in and authenticated shells.
- Preserve the current responsive navigation pattern: bottom bar on compact widths and navigation rail on expanded widths.
- Runtime auth configuration comes from `AndroidAppAuthConfig` and `BuildConfig`; do not hardcode Supabase or Google sign-in values in source files or resources.
- Follow the existing Material 3 Compose style and theme setup under `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/theme`.
- When changing UI or ViewModel behavior, prefer extending tests under `androidApp/src/test/java/com/loganmartlew/rangework/android` rather than introducing ad hoc manual-only behavior.
