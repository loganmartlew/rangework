# Stage 10 — Accessibility hardening sweep

> Roadmap stage **S10**. Cross-cutting verification + gap-fill after all screens land. Backlog: B38, B39; systemic 9.1–9.4; Material-audit a11y items.

## Objective

Most accessibility is baked into S1 (contrast, error token) and S2 (content descriptions, 48dp targets), but a final end-to-end pass catches integration gaps that only appear once screens are assembled. This stage runs the whole app through TalkBack, an automated contrast/target audit, and a manual outdoor-legibility check, then fixes any remaining gaps.

- Full content-description audit on every icon-only control across all screens (reorder, delete, overflow, app-bar actions, FAB, club switches) (systemic 9.2, B39).
- 48dp minimum touch-target audit on instruction/item icon buttons, card overflow, steppers (B38; consistency M5).
- Contrast re-measure (AA: 4.5:1 text / 3:1 components) on fields, helper text, secondary card text, pills, and the Focus-card tint — under bright/outdoor conditions, a first-class requirement for a range app (systemic 9.1).
- Destructive paths never colour-only — confirm icon + label + confirmation everywhere (systemic 9.3).
- Segmented-control selected/unselected contrast delta verified (systemic 9.4).
- Club rows confirmed at ≥48–56dp `ListItem` height with full-row `toggleable` semantics (Material audit).

## Dependencies

- **Upstream:** S4–S9 (all screens must exist to sweep them). Relies on the a11y foundations from S1 and S2.
- **Downstream:** none — this is the closing quality gate.

## Affected screens

- **All** — Login, Overview, Units/Sessions lists, Unit/Session detail, Unit/Session edit, Settings, Manage clubs, nav bar.

## Likely files

- Targeted touch-ups across the extracted screen composables under `androidApp/.../ui/` and the S2 components in `androidApp/.../ui/components/` (add/fix `contentDescription`, `semantics`, `minimumInteractiveComponentSize`, token swaps for contrast).
- `androidApp/.../ui/theme/*` — only if a contrast measurement forces a token adjustment (coordinate back to S1 intent; keep palette stable).
- No new behaviour; this is corrective.

## New components required

- None. Gap-fill only — modifies existing S2 components and screen compositions.

## Validation checklist

- [ ] `.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug` green.
- [ ] `.\gradlew.bat :androidApp:lintDebug` clean (resolve accessibility lint warnings).
- [ ] **Full TalkBack pass** through every screen and the core flows (sign in → create unit → create session → edit → delete/undo → settings/clubs) with no unlabeled or ambiguous control.
- [ ] Every icon-only control announces a meaningful description.
- [ ] Automated/manual target audit: all interactive elements ≥48dp.
- [ ] Contrast meter ≥ AA on fields, helper text, secondary text, pills, Focus-card tint — re-checked in a simulated bright/high-brightness condition.
- [ ] Every destructive action pairs colour with icon + label + confirmation.
- [ ] Segmented controls (Theme, Distance, Speed) pass selected/unselected component contrast.
- [ ] Club switch rows: TalkBack reads "{club}, enabled/disabled"; row height ≥48dp.
- [ ] Font scaling: verify layouts hold at large system font sizes (no clipping of `RangeworkMono` numerals or labels).
- [ ] Phone + tablet both swept.

## Accessibility requirements

- This *is* the accessibility stage; the checklist above is the requirement set. Standard: WCAG AA for text (4.5:1) and UI components (3:1); 48dp targets; non-colour-only signalling; TalkBack-complete labelling; outdoor legibility treated as a primary use condition, not a theme nicety.

## Regression risks

- A contrast fix that adjusts a shared token re-enters R1 territory (app-wide visual shift) — prefer per-component adjustments; escalate token changes back through the S1 intent and re-run visual regression.
- Adding `semantics`/`contentDescription` can change TalkBack reading order or merge/clear semantics unexpectedly — verify focus order after edits.
- Enlarging a touch target can shift layout/spacing — re-check the affected card/row visually.
- Large-font-scale fixes must not violate the `CLAUDE.md` rule against undeclared `sp` values — adapt layout, not by inventing new type sizes.
- Sweeping late means fixes touch many screens at once — keep changes minimal and isolated so they don't regress freshly-landed screen stages.
