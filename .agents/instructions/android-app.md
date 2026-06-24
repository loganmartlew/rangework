# Android app instructions

`apps/mobile/androidApp` owns Compose UI, navigation, ViewModels, Activity lifecycle wiring, Android auth integration, resources, and theming.

## File map

- `apps/mobile/androidApp/src/main/java/com/loganmartlew/rangework/android/MainActivity.kt` — Android entry point.
- `apps/mobile/androidApp/src/main/java/com/loganmartlew/rangework/android/ui/RangeworkApp.kt` — main Compose app shell (auth state, planner state, navigation, phone/tablet layout).
- `apps/mobile/androidApp/src/main/java/com/loganmartlew/rangework/android/ui/RangeworkNavigation.kt` — navigation type detection (bottom bar vs rail) and route/destination definitions.
- `apps/mobile/androidApp/src/main/java/com/loganmartlew/rangework/android/ui/AuthViewModel.kt` — auth/session restore and sign-in/sign-out state.
- `apps/mobile/androidApp/src/main/java/com/loganmartlew/rangework/android/ui/PracticePlannerViewModel.kt` — planning screen loading, editing, save/delete flows, setup messaging.
- `apps/mobile/androidApp/src/main/java/com/loganmartlew/rangework/android/ui/SettingsViewModel.kt` — theme mode, measurement preferences, and club management state.
- `apps/mobile/androidApp/src/main/java/com/loganmartlew/rangework/android/ui/screens/` — 9 screen composables: Overview, UnitList, UnitDetail, UnitEditor, SessionList, SessionDetail, SessionEditor, ManageClubs, Settings.
- `apps/mobile/androidApp/src/main/java/com/loganmartlew/rangework/android/ui/components/` — 30+ reusable UI components (cards, FABs, pickers, dialogs, steppers, bars, etc.).
- `apps/mobile/androidApp/src/main/java/com/loganmartlew/rangework/android/ui/theme/` — Material 3 theme, color scheme, type definitions (`Type.kt`), `RangeworkMono` object.
- `apps/mobile/androidApp/src/test/java/com/loganmartlew/rangework/android/` — ViewModel unit tests.

## Conventions

- Keep business rules out of composables. `RangeworkApp.kt` orchestrates state and navigation; normalization and persistence stay in shared use cases/repositories.
- Preserve the auth-gated flow: `AuthViewModel` drives sign-in state, `PracticePlannerViewModel` reacts to `AuthState`, and root navigation switches between sign-in and authenticated shells.
- Preserve the responsive navigation pattern: bottom bar on compact widths, navigation rail on expanded widths.
- Runtime auth configuration comes from `AndroidAppAuthConfig` and `BuildConfig`; do not hardcode Supabase or Google sign-in values in source files or resources.
- Follow the existing Material 3 Compose style and theme setup under `apps/mobile/androidApp/src/main/java/com/loganmartlew/rangework/android/ui/theme`.
- When changing UI or ViewModel behavior, prefer extending tests under `apps/mobile/androidApp/src/test/java/com/loganmartlew/rangework/android`.

## Typography

For text styling rules (when to use `RangeworkMono` vs `MaterialTheme.typography`, color pairings, prohibited patterns), read `.agents/instructions/typography.md`.
