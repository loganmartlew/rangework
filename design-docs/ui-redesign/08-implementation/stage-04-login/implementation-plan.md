# Stage 04 — Login redesign

> Roadmap stage **S4**. Fully isolated pre-auth screen; earliest end-to-end proof of the foundation. Backlog: B22, B23, B24, B52, B53, B54, B55. Spec: `07-redesigns/login-redesign.md`.

## Objective

Collapse the over-built two-card sign-in screen into the canonical Android pre-auth pattern: a single full-height centred column with one headline, one supporting line, and one primary action.

- Merge the two cards into one centred layout; remove all card chrome (B22).
- Remove the nested "What you'll do here" sub-card (B24), the "Signed out." status label (B52), and the "Google sign-in" chip (B53).
- One headline only — "Plan sharper range sessions"; drop the competing second headline (B54).
- Centre and enlarge the app icon to ~72–96dp (B55).
- Replace the custom outlined pill with the **Google Identity-compliant** sign-in button (B23) — a correctness/compliance fix, not just visual.
- Add a small Terms/Privacy legal line beneath the button.

## Dependencies

- **Upstream:** S1 (typography, tokens); S2 (`GoogleSignInButton`). No data dependency.
- **Downstream:** none. Can ship as soon as the Google button exists in S2.

## Affected screens

- **Login / sign-in** only. No bottom nav, no top app bar (pre-auth).

## Likely files

- The extracted login/sign-in screen composable under `androidApp/.../ui/` (the sign-in screen split out of `RangeworkApp.kt`).
- `androidApp/.../ui/RangeworkApp.kt` — root navigation switch between sign-in and authenticated shell (unchanged behaviour; verify the sign-in branch still renders the new screen).
- `androidApp/.../ui/AuthViewModel.kt` — sign-in trigger; only touched if the Google Identity button requires Credential Manager (`GetSignInWithGoogleOption`) rewiring.
- `androidApp/.../auth/AndroidAppAuthConfig.kt` + `BuildConfig` wiring — **do not hardcode** Supabase/Google values; keep config-driven (`CLAUDE.md`).
- `androidApp/.../ui/components/GoogleSignInButton.kt` (from S2).
- `androidApp/src/test/.../AuthViewModelTest` (or equivalent) — extend.

## New components required

- None new in this stage — `GoogleSignInButton` comes from S2. This stage assembles existing primitives: a full-height `Column` with weighted `Spacer`s, the app-icon `Image`/`Surface`, `Text` on the type scale, the Google button, and inline `AnnotatedString`/`ClickableText` Terms/Privacy links.

## Validation checklist

- [ ] `.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug` green.
- [ ] `.\gradlew.bat :androidApp:lintDebug` clean.
- [ ] Real Google sign-in completes and authenticates end-to-end (manual on device/emulator).
- [ ] Session restore still works after sign-in (stay-signed-in across launches preserved).
- [ ] Sign-in button matches Google's Identity branding spec (white container, multicolour G, prescribed type/padding).
- [ ] CTA is visible without scrolling on common device heights (no second-card scroll).
- [ ] Single headline only; no chip, no "Signed out." label, no nested sub-card.
- [ ] App icon centred at ~72–96dp.
- [ ] **Misconfigured-auth path** still degrades to friendly setup messaging, not a crash (`CLAUDE.md`).
- [ ] `AuthViewModel` tests pass / extended for any Credential Manager change.

## Accessibility requirements

- Google button meets contrast and 48dp target requirements in addition to branding.
- Terms/Privacy inline links are individually focusable with descriptive accessible names ("Terms", "Privacy Policy") and meet AA contrast as small text on dark.
- Headline/body use the type scale with `onBackground`/`onSurfaceVariant` per `CLAUDE.md` colour pairings; legal line at `bodySmall`/`onSurfaceVariant` must still clear AA.
- App icon marked as decorative (no redundant TalkBack announcement) unless it conveys state.

## Regression risks

- **R5 (primary):** the Google Identity-compliant button may require Credential Manager / `GetSignInWithGoogleOption` rewiring that can break sign-in. This is why Login is scheduled early and isolated — verify the full auth path before relying on it. Keep `AndroidAppAuthConfig`/`BuildConfig` wiring; no hardcoded secrets.
- Removing the second "Pick up where you left off" card must not remove any genuine returning-user state handling that the auth flow depended on — confirm it was purely cosmetic.
- Weighted-spacer vertical centring can misbehave on very small screens or with the keyboard — verify on a short viewport.
- Brand-mark proportion change (B55) should stay consistent with the in-app top-bar icon (consistency L3).
