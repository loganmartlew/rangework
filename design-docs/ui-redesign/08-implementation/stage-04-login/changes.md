# Stage 04 — Login redesign: changes

## Summary of changes

### `androidApp/.../ui/components/GoogleSignInButton.kt`

Upgraded from a bare `OutlinedButton` to a Google Identity-compliant white-filled button (B23):

- Added `BorderStroke(1.dp, #747775)` — Google's prescribed neutral border colour.
- Set `containerColor = Color.White`, `contentColor = Color(#1F1F1F)` via `ButtonDefaults.outlinedButtonColors` — explicitly off-theme per Google's branding spec.
- Added explicit `disabledContainerColor`/`disabledContentColor` with 38% alpha for disabled state.
- Extracted three colour constants (`GoogleButtonContainerColor`, `GoogleButtonContentColor`, `GoogleButtonBorderColor`) as file-private vals with a comment explaining the intentional off-theme choice.
- Existing `OutlinedButton` shape preserved so that the `border` parameter works natively.

### `androidApp/.../ui/RangeworkApp.kt`

Replaced the two-card `UnauthenticatedEntryScreen` with a single full-height centred column (B22, B24, B52, B53, B54, B55):

**Removed composables:**
- `OnboardingHeroCard` — the stacked card with `Badge("Welcome")`, `headlineLarge` title, `bodyLarge` detail, and nested `EntryHighlightCard("What you'll do here", …)`.
- `SignInActionsCard` — the second card containing the `Badge("Google sign-in")` chip, `headlineSmall "Pick up where you left off"` headline, `bodyMedium` support text, and the inline `OutlinedButton` with embedded Google logo.

**New `UnauthenticatedEntryScreen` layout:**
```
Scaffold (no TopAppBar, no NavigationBar)
└─ Column (fillMaxSize, 28dp horizontal padding, centred horizontally)
    ├─ Spacer (weight 1.2)
    ├─ BrandMarkContainer (84dp container / 60dp mark, twoColor = true)
    ├─ Spacer (24dp)
    ├─ Text (bootstrapMessage.headline, headlineMedium, centred)
    ├─ Spacer (12dp)
    ├─ Text (bootstrapMessage.detail, bodyLarge, onSurfaceVariant, centred, maxLines=2)
    ├─ Spacer (weight 1.6)
    ├─ GoogleSignInButton (enabled when not in progress and auth configured)
    ├─ [Spacer + LinearProgressIndicator when actionInProgress or Restoring]
    ├─ Spacer (16dp)
    ├─ LegalLine()
    └─ Spacer (16dp)
```

**New `LegalLine` composable:**
- `buildAnnotatedString` with `onSurfaceVariant` body text and `primary` + `TextDecoration.Underline` for "Terms" and "Privacy Policy" spans.
- `ClickableText` with `bodySmall` style centred — individual spans are annotated with `"TERMS"` and `"PRIVACY"` tags for future URL wiring.

**Import changes in `RangeworkApp.kt`:**

| Removed | Reason |
|---|---|
| `androidx.compose.foundation.Image` | Image now owned by `GoogleSignInButton` component |
| `androidx.compose.foundation.layout.Arrangement` | No longer used |
| `androidx.compose.material3.Badge` | Removed with cards |
| `androidx.compose.material3.Card`, `CardDefaults` | Removed with cards |
| `androidx.compose.material3.OutlinedButton` | Replaced by `GoogleSignInButton` component |
| `androidx.compose.ui.res.painterResource` | No longer needed directly |
| `com.loganmartlew.rangework.android.R` | No longer referenced |
| `...components.EntryHighlightCard` | Removed with `OnboardingHeroCard` |
| `...components.ScrollableScreen` | Login no longer scrollable |

| Added | Reason |
|---|---|
| `androidx.compose.foundation.text.ClickableText` | `LegalLine` inline links |
| `androidx.compose.ui.Alignment` | `horizontalAlignment` on Column |
| `androidx.compose.ui.text.SpanStyle`, `buildAnnotatedString`, `withStyle` | `LegalLine` |
| `androidx.compose.ui.text.style.TextAlign`, `TextDecoration`, `TextOverflow` | Layout text styling |
| `...components.GoogleSignInButton` | Sign-in button |

---

## Regression risks

**R1 — `ClickableText` soft-deprecated.** The compiler emits a warning (`deprecated — use Text or BasicText with LinkAnnotation`). The build is successful and the component is fully functional. Migrating to `LinkAnnotation` / `BasicText` is deferred to S10 (accessibility pass) when the policy URLs are also wired.

**R2 — `bootstrapMessage.detail` may exceed `maxLines = 2`.** The detail text is ~2–3 lines on narrow viewports; `TextOverflow.Ellipsis` clips gracefully. The misconfigured-auth detail ("Sign-in still needs to be enabled on this build…") is shorter and fits. No content loss in the primary path; verify on a 360dp-wide device.

**R3 — `BrandMarkContainer` `contentDescription = "Rangework mark"` announced by TalkBack.** The spec calls for the icon to be decorative on the login screen. The component's fixed description means TalkBack will announce it. Changing `contentDescription` to `null` requires a parameter addition to `BrandMarkContainer` — deferred to S10 (accessibility pass) rather than touching the shared component outside the login stage scope.

**R4 — `LegalLine` Terms/Privacy links are stubs.** The `onClick` handler is a no-op. Tapping "Terms" or "Privacy Policy" does nothing until URLs are published and wired. This is intentional for this stage.

**R5 — Pre-existing `menuAnchor` lint warnings.** Two warnings from `ClubPickerField.kt` and `SessionEditorScreen.kt` are unchanged from before S4.

---

## Validation checklist

- [x] `.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug` — BUILD SUCCESSFUL (67 tasks executed).
- [x] `.\gradlew.bat :androidApp:lintDebug` — BUILD SUCCESSFUL, no new lint errors (pre-existing `menuAnchor` warnings unchanged).
- [ ] Real Google sign-in completes and authenticates end-to-end (manual on device/emulator).
- [ ] Session restore still works after sign-in (stay-signed-in across launches preserved).
- [ ] Sign-in button renders with white container, multicolour G logo, `#747775` border, and dark label.
- [ ] CTA is visible without scrolling on common device heights (360dp–412dp viewport).
- [ ] Single headline only; no chip, no "Signed out." label, no nested sub-card, no second card.
- [ ] App icon (`BrandMarkContainer`) centred at 84dp with `twoColor = true` mark.
- [ ] Weighted spacers push the button to the lower third on a standard 5–6 inch portrait screen.
- [ ] `LegalLine` renders with "Terms" and "Privacy Policy" underlined in `primary` colour.
- [ ] **Misconfigured-auth path**: `bootstrapMessage.headline = "Range-ready planning"`, button disabled, no crash.
- [ ] `LinearProgressIndicator` visible during `actionInProgress` or `Restoring` state; hidden otherwise.
- [ ] `AuthViewModel` tests pass (no ViewModel code changed; confirm baseline still green).
- [ ] No `Arrangement`, `Badge`, `Card`, `CardDefaults`, `OutlinedButton`, `ScrollableScreen`, or `EntryHighlightCard` references remain in `RangeworkApp.kt`.
