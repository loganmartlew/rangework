# Stage 01 — Foundation: M3 tokens, chrome & terminology

> Roadmap stage **S1**. Cross-cutting design-system layer that every later stage depends on. Backlog: B07, B08, B36, B48, B43-token; systemic 7.x / 8.1 / 9.1 / 9.4; Material-audit surface hierarchy.

## Objective

Establish a correct Material 3 foundation so that every redesigned screen inherits the right tokens instead of overriding them locally. Three coordinated changes:

1. **Surface & colour tokens** — replace the flat custom dark surfaces with M3 tonal-elevation tokens (`surfaceContainerLowest/Low/.../High`) so cards read as cards; set the FAB container to `primaryContainer` (B07); confirm a strict button token hierarchy (one `FilledButton`/`primary` per screen, tonal for secondary, `error` reserved for destructive); verify segmented-control selected/unselected contrast (9.4).
2. **Chrome** — Navigation bar gains the M3 active-indicator pill and 80dp height (B08); `OutlinedTextField` regains a visible 1dp rest border and standardized 16dp field spacing defaults (B36, B48).
3. **Terminology & string lock** — one term per concept across all user-facing strings: **Unit**, **Session**, **Session item**, **Focus cue**, and scope-prefixed notes ("Unit notes" / "Session notes" / "Item notes"). Display strings only — **no serialized model field or DB column renames** (systemic 8.1; consistency M4/M6).

This stage changes *tokens and strings*, not layouts. Existing screens must keep working; they simply look more M3-correct.

## Dependencies

- **Upstream:** none. This is the root of the graph.
- **Downstream:** S2 (components), S3, S4 and every screen stage depend on the tokens and strings finalized here.

## Affected screens

All of them (token/string changes are global): Login, Overview, Units/Sessions lists, Unit/Session detail, Unit/Session edit, Settings, nav bar. No screen is *restructured* here.

## Likely files

- `androidApp/src/main/java/com/loganmartlew/rangework/android/ui/theme/Type.kt` — confirm typography roles used by the consistency audit exist (`titleMedium`, `labelMedium`, `bodySmall`, etc.); add named styles only if a genuinely new role is required (per `CLAUDE.md` typography rules — no anonymous inline styles).
- `androidApp/.../ui/theme/Color.kt` / `Theme.kt` (or equivalent in the theme package) — surface container tokens, `primaryContainer`/`onPrimaryContainer`, `error`/`onError`, segmented contrast.
- `androidApp/.../ui/RangeworkApp.kt` — `NavigationBar`/`NavigationBarItem` height + active indicator; default `OutlinedTextField` styling if centralized here.
- Any shared theme defaults / `RangeworkTheme` wrapper, plus a possible `RangeworkTextFieldDefaults` helper for the restored border + spacing.
- `androidApp/src/main/res/values/strings.xml` (and any inline Compose string literals) — terminology lock.
- **Do not touch:** `shared/.../model/*` serialization keys, `supabase/migrations/*` column names.

## New components required

None that are screen-level. Possible foundation helpers:

- `RangeworkTextFieldDefaults` (or shared `OutlinedTextField` wrapper) carrying the restored rest border + 16dp spacing — consumed later by S2/S7.
- Token aliases if the codebase currently hardcodes colours (replace hardcoded values with `MaterialTheme.colorScheme.*`).

The reusable composables themselves are built in S2; this stage only prepares the tokens they consume.

## Validation checklist

- [ ] `.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug` green.
- [ ] `.\gradlew.bat :shared:lintDebug :androidApp:lintDebug` clean (tests first, then lint).
- [ ] Visual regression: every existing screen (Login → Settings) still renders coherently — no broken layouts from token swaps.
- [ ] Nav bar renders the active-indicator pill and measures 80dp; 48×48dp per destination preserved.
- [ ] FAB shows `primaryContainer` (green) fill with `onPrimaryContainer` icon on both list screens.
- [ ] `OutlinedTextField`s show a visible border at rest (before focus) on dark surface.
- [ ] Section/detail cards visibly separate from the page background (tonal-elevation delta readable).
- [ ] Terminology audit: grep confirms no concept uses two words anywhere in user-facing strings; serialized keys unchanged.
- [ ] Existing Android ViewModel tests and shared use-case tests still pass unchanged.
- [ ] Phone (compact) **and** tablet (expanded rail) both verified — responsive nav pattern intact.
- [ ] Light, Dark, and System themes all verified (token changes must hold across all three).

## Accessibility requirements

- Run a contrast/AA baseline (target 4.5:1 text / 3:1 UI components) on field borders, field labels, helper text, and secondary card text — this is the outdoor-legibility foundation for the whole app (systemic 9.1).
- Verify segmented-control selected vs unselected contrast delta meets component-contrast minimums (systemic 9.4).
- Confirm restored field borders raise low-contrast inputs above AA (B36 doubles as an accessibility fix).
- Establish that `error` colour is a token (not a hardcoded coral) so destructive affordances can be paired with icon + label downstream (9.3).

## Regression risks

- **R1 (primary):** an app-wide token swap can cause visual regressions on screens not yet redesigned. Mitigate with full visual regression before merge; change tokens, not per-screen overrides; keep the existing green accent and palette stable.
- **R7:** terminology lock could accidentally rename a `kotlinx.serialization` key or Supabase column → data breakage. Constrain edits to display strings; never touch wire/storage names.
- Dynamic-color (Material You) interaction: changing surface tokens must be checked with dynamic colour both on and off (ties to B43).
- Typography: adding a new style risks violating the `CLAUDE.md` "no anonymous inline styles / only declared weights" rules — route any new role through `Type.kt`/`RangeworkMono`.
- Nav-bar height change can shift content insets on every screen — re-check bottom padding interplay with the later 96dp FAB padding (B37).
