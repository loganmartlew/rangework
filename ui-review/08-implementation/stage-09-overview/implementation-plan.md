# Stage 09 — Overview redesign (integrator)

> Roadmap stage **S9**. The integrator screen — reuses stat blocks and list cards, navigates into the finished lists/details, and is the most data-coupled. Backlog: B25, B26, B27, B28, B34, B51. Spec: `07-redesigns/overview-redesign.md`.

## Objective

Turn the passive dashboard (which mostly restates the bottom nav) into a launchpad, and split first-run from returning-user so each case is purposeful.

- Small TopAppBar, title-only "Overview" (B34).
- **Returning user:** a compact greeting strip (email as caption); drop the multi-line workspace blurb and the duplicate "New unit / New session" buttons (B28) — creation lives on each tab's FAB.
- **Stat cards** become tappable navigation: Units · N and Sessions · N as proper `Card`s with a numeral + chevron that route to their tabs (B25, B51).
- **Next-move card** becomes contextual (B26), branching on actual state: no units → build first unit; units-no-sessions → combine into a session; both → open most recent / picker; just-edited → resume editing.
- **Recently used** — a horizontal `LazyRow` of the 3–5 most recently opened/edited units and sessions, each routing to detail (B27). The screen's load-bearing repeat-use feature.
- **First-run state** (zero units *and* sessions): collapse to a focused welcome — icon, headline, short value prop, single "Create your first unit" CTA; hide stat row / Next move / recents.

## Dependencies

- **Upstream:** S1 (tokens, nav), S2 (`StatBlock`, `ListEntryCard`/recent card, `EmptyState`, Next-move tonal card), S3 (recents signal, next-move state, counts).
- **Live destinations:** S5 (stat taps → lists) and S6 (recents → details) must exist so navigation lands somewhere real. Scheduled last for this reason.
- **Downstream:** none.

## Affected screens

- **Overview** (returning + first-run states). Navigates into Units/Sessions tabs (S5) and Unit/Session detail (S6), and into create flows.

## Likely files

- Extracted `OverviewScreen` composable under `androidApp/.../ui/`.
- `androidApp/.../ui/RangeworkApp.kt` — stat-card tab navigation, recents → detail, next-move contextual routing, first-run branch; tablet two-column overview preserved.
- `androidApp/.../ui/PracticePlannerViewModel.kt` — counts, recents list, next-move state, last-edited signal (reads from S3).
- S2 components + S3 data (consumed).
- `androidApp/src/test/...` — extend for next-move branch resolution and first-run vs returning state.

## New components required

- None new — the `RecentCard` is a configuration of S2's `ListEntryCard`/`OutlinedCard` with an `AssistChip` type tag; stat cards are S2 `StatBlock`s made clickable; the Next-move card is the S2 tonal `Card` + `FilledTonalButton`. The first-run state reuses `EmptyState`.

## Validation checklist

- [ ] `.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug` green.
- [ ] `.\gradlew.bat :androidApp:lintDebug` clean.
- [ ] Tapping a stat card navigates to the corresponding tab (Units/Sessions).
- [ ] Tapping a recent card opens the correct unit/session detail.
- [ ] Next-move card resolves correctly for all four states: no units / units-no-sessions / both / just-edited — and its button performs the right action.
- [ ] First-run (zero units + zero sessions) shows the single-CTA welcome; stat row / Next move / recents hidden.
- [ ] Returning user no longer sees the duplicate New unit / New session buttons or the long blurb.
- [ ] Recents degrade gracefully (strip hidden) if the S3 recents source is unavailable.
- [ ] Greeting shows name + email caption.
- [ ] Phone + **tablet two-column** overview both verified (the one screen the spec says expands to two columns).
- [ ] Terminology consistent with the rest of the app (re-check after this final screen).

## Accessibility requirements

- Stat cards and recent cards are single accessible click targets with descriptive names ("Units, 1, open Units" / "{name}, {type}, open").
- Type chip (Unit / Session) conveys category by text, not colour alone.
- Stat numerals use `RangeworkMono`; greeting/labels/metadata use DM Sans (`CLAUDE.md`).
- Next-move button has a clear accessible label matching its contextual action.
- `LazyRow` recents are keyboard/TalkBack traversable; 48dp targets.
- First-run CTA is a standard 48dp button with AA-contrast text.

## Regression risks

- **R4:** recents depends on the riskiest S3 capability — the screen must ship even if recents slips (hide the strip), so don't make the rest of Overview depend on it.
- **R11:** scope creep — the "briefing"/run-mode delight ideas belong to S11/F-4, not here. Keep to the redesign spec.
- **R10:** tablet two-column overview is explicitly in scope (`RANGEWORK.md`) — verify both width classes; restructuring must not break the rail/two-column pattern.
- Navigation targets (S5/S6) must be final — if a destination route changes after this stage, Overview links break.
- Contextual Next-move reading "just-edited" state depends on the last-edited signal from S3 being reliable; fall back to a safe default branch if absent.
- First-run vs returning branch must key off real emptiness (zero + zero), not a transient loading state — guard against a flash of the wrong layout during load.
