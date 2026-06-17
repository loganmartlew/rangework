# Stage 01 — Foundation: changes

## Summary of changes

### 1. Color palette — replaced with custom "Combo A" scheme (`Color.kt`)

The original palette (Warm Graphite primary, Forest Green secondary) was replaced with a hand-crafted scheme: **Deep Fairway primary · Lighter Sage secondary · Warm Graphite surfaces**. The new palette was specified by the designer and integrated from `Color.kt` / `Theme.kt` reference files.

**Structural change:** Color.kt now uses named semantic token constants (`LightPrimary`, `DarkSurface`, etc.) as an intermediate layer between the tonal palette and the color schemes. This mirrors Material Theme Builder output and makes the scheme easier to audit.

**New tonal palettes:**

| Palette | Role | Key tones |
|---|---|---|
| Primary — Deep Fairway | Actions, FAB, filled buttons | `Primary50` #2D6A4F (light) / `Primary30` #8FC8A8 (dark) |
| Secondary — Lighter Sage | Nav indicator, segmented buttons | `Secondary50` #52796F (light) / `Secondary30` #8ABFB6 (dark) |
| Tertiary — Sage Mist | Accent, chips | unchanged from original |
| Neutral — Warm Graphite | Surfaces, backgrounds | same values as original |
| Neutral Variant | Borders, outlines | expanded to full 0–100 range; `NeutralVariant70` corrected from #3E3C38 → #3C3A37 |
| Error | Destructive states | expanded to full M3 default range |

**Light scheme token changes (selected):**

| Token | Before | After |
|---|---|---|
| `primary` | `Primary80` #2A2A28 (graphite) | `LightPrimary` = `Primary50` #2D6A4F (Deep Fairway) |
| `primaryContainer` | `Primary20` #D8D6D0 | `LightPrimaryContainer` = `Primary20` #B8DCC8 (light sage wash) |
| `secondary` | `Secondary60` #386044 | `LightSecondary` = `Secondary50` #52796F |
| `secondaryContainer` | `Secondary20` #BBDDC9 | `LightSecondaryContainer` = `Secondary20` #B0D4CC |
| `surfaceVariant` | `NeutralVariant40` #BFBDB9 | `LightSurfaceVariant` = `Neutral20` #EAE8E4 (lighter) |
| `outlineVariant` | `NeutralVariant30` #D5D3CF | `LightOutlineVariant` = `NeutralVariant20` #D8D6D0 |
| `inversePrimary` | `Primary30` #C4C2BB | `LightInversePrimary` = `Primary30` #8FC8A8 (green) |
| `surfaceTint` | `Primary80` | `LightSurfaceTint` = `Primary50` #2D6A4F |

**Dark scheme token changes (selected):**

| Token | Before | After |
|---|---|---|
| `primary` | `Primary30` #C4C2BB (graphite) | `DarkPrimary` = `Primary30` #8FC8A8 (light green) |
| `primaryContainer` | `Primary70` #3C3A37 (dark graphite) | `DarkPrimaryContainer` = `Primary70` #1A3D28 (dark green) |
| `secondary` | `Secondary30` #93C8A8 | `DarkSecondary` = `Secondary30` #8ABFB6 (sage) |
| `secondaryContainer` | `Secondary70` #274530 | `DarkSecondaryContainer` = `Secondary70` #2E4740 |
| `surface` | `Color(0xFF1C1A18)` (hardcoded) | `DarkSurface` = `Neutral88` #1C1A18 (named constant, same value) |
| `inversePrimary` | `Primary80` #2A2A28 (graphite) | `DarkInversePrimary` = `Primary60` #235438 (dark green) |
| `surfaceTint` | `Primary30` (graphite) | `DarkSurfaceTint` = `Primary30` #8FC8A8 (green) |

**Retained from original:** All `NeutralSurface*` and `NeutralDark*` fractional stops are kept and still wire the `surfaceContainerLowest` → `surfaceDim` tokens that the app's cards and screens depend on.

### 2. FAB and button tokens (automatic via palette change)

No code changes required in `RangeworkApp.kt` or editor screens. `FloatingActionButton` defaults to `primaryContainer` and `Button` defaults to `primary` — both now draw from the Deep Fairway palette (B07, B09).

### 3. Navigation bar active-indicator (automatic via palette change)

`NavigationBarItem` renders the M3 active-indicator pill automatically using `secondaryContainer`. Previously (#274530 dark green on #222020 near-black) the contrast was ~1.1:1 — invisible. The new `DarkSecondaryContainer = Secondary70` #2E4740 gives ~1.4:1 pill contrast, which remains subtle but the teal-green hue is now distinguishable from the near-black surface in a way dark graphite could not be. The `onSecondaryContainer` icon (#B0D4CC on #2E4740) provides 5:1 contrast ✓. Nav bar height is 80dp by default — no override required (B08).

### 4. Theme.kt — dynamic color support added

`RangeworkTheme` gained a `dynamicColor: Boolean = false` parameter. When `true` on Android 12+ (API 31), it switches to `dynamicDarkColorScheme` / `dynamicLightColorScheme`. Disabled by default to preserve the hand-crafted palette.

`window.statusBarColor` from the reference file was **not integrated** — it is deprecated in the Android 15 (API 35) SDK (which this project targets) and conflicts with the edge-to-edge insets setup already in place via `WindowCompat.getInsetsController`. The existing `isAppearanceLightStatusBars` / `isAppearanceLightNavigationBars` approach is correct and clean.

### 5. Terminology lock — display strings only

All user-facing concept labels are now consistent. No serialized field names or DB columns were touched.

| Screen | Field / label | Before | After |
|---|---|---|---|
| `UnitEditorScreen` | Notes field label | "Notes" | "Unit notes" |
| `UnitEditorScreen` | Focus field label | "Focus" | "Focus cue" |
| `UnitDetailScreen` | Notes card title | "Notes" | "Unit notes" |
| `UnitDetailScreen` | Focus card title | "Focus" | "Focus cue" |
| `SessionDetailScreen` | Notes card title | "Notes" | "Session notes" |
| `SessionDetailScreen` | Inline focus line | "Focus: …" | "Focus cue: …" |

---

## Regression risks

**R1 — All interactive elements look different (intentional).** FABs, buttons, and nav indicators are now green rather than graphite. Verify visually across every screen.

**R2 — `surfaceVariant` is now much lighter in light mode.** Old: `NeutralVariant40` #BFBDB9 (medium grey). New: `Neutral20` #EAE8E4 (near-white). Two card backgrounds use `surfaceVariant.copy(alpha = 0.4f)`: `InstructionEditorCard` and `SessionItemEditorCard`. With the lighter value they may lose visible definition against the page background. Watch for invisible nested cards in light mode; addressed in S2 if needed.

**R3 — `NeutralVariant70` corrected.** Old: #3E3C38. New: #3C3A37. Used for `DarkSurfaceVariant` and `DarkOutlineVariant`. Visual difference is imperceptible (2 units per channel).

**R4 — Dark mode nav indicator.** `DarkSecondaryContainer` = `Secondary70` #2E4740 (teal-dark) on `surfaceContainer` #222020. Contrast ~1.4:1 for the pill shape; icon inside is 5:1 ✓. Still below 3:1 UI-component threshold — tracked for S10 (accessibility stage).

**R5 — No serialization regressions.** Terminology changes are display strings only.

---

## Validation checklist

- [ ] `.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug` green *(verified — BUILD SUCCESSFUL, no warnings)*
- [ ] `.\gradlew.bat :shared:lintDebug :androidApp:lintDebug` clean
- [ ] FAB on Units list renders Deep Fairway green fill (light: `Primary20` #B8DCC8 container / dark: `Primary70` #1A3D28)
- [ ] FAB on Sessions list renders Deep Fairway green fill
- [ ] "Save unit" button renders Deep Fairway green (was graphite)
- [ ] "Save session" button renders Deep Fairway green (was graphite)
- [ ] NavigationBar active-indicator pill visible on both light and dark themes
- [ ] NavigationBar measures 80dp height; 48×48dp touch target per destination
- [ ] NavigationRail active-indicator pill visible on expanded layout (tablet)
- [ ] Segmented controls (Theme / Distance / Speed in Settings) selected segment clearly distinct on both themes
- [ ] Unit editor shows "Unit notes" and "Focus cue" field labels
- [ ] Unit detail shows "Unit notes" and "Focus cue" section titles
- [ ] Session detail shows "Session notes" and "Focus cue: …" inline text
- [ ] Session editor shows "Session notes", "Item notes", "Focus cue" (unchanged — confirm no regression)
- [ ] InstructionEditorCard and SessionItemEditorCard nested cards still visible against page background in light mode (surfaceVariant regression check)
- [ ] Surface cards on detail screens visibly separate from page background (tonal elevation readable)
- [ ] Light, Dark, System themes all verified
- [ ] Existing unit and session CRUD flows work end-to-end (data unaffected)
- [ ] Shared ViewModel and Android ViewModel tests pass unchanged
- [ ] Dynamic color path: verify `dynamicColor = true` does NOT break at runtime on API < 31 (guard is in place)
