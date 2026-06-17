# Stage 11 — Deferred / decision-gated work

> Roadmap stage **S11**. Not scheduled into S1–S10 — each item needs a product decision before it becomes implementable. Backlog: B11; systemic 4.2, 5.3, 3.5; planning review.

## Objective

Capture the four pieces of work the redesign program deliberately did **not** schedule, with enough detail that each can be turned into a real stage once its decision gate is resolved. This file is a holding plan, not a build order. Nothing here should start until the corresponding decision (Section F of the master roadmap) is made.

The four items:

1. **F-1 — Remove the standalone Session-detail waypoint (B11).** The Session-detail redesign (S6) improves the screen as it stands; B11 proposes deleting it as a navigational dead-end (list → detail → edit, where detail adds little). These conflict and must be decided **before S6**, not after.
2. **F-2 — Tablet list-detail pane.** Systemic 5.3 flags a half-adapted tablet layout; no redesign specifies a canonical list-detail two-pane for Units/Sessions. Needs its own design.
3. **F-3 — "Used in N sessions" reverse link.** Systemic 4.2 / delight: from a unit, see (and tap through to) the sessions that depend on it. High strategic value (deletion safety, reuse visibility) but absent from every redesign spec. Needs a `shared` reverse query.
4. **F-4 — Run / "follow at the range" mode.** Systemic 3.5 + planning review: the app stops at planning; the stated promise is "follow on the day." Leave an IA seam now; build later.

## Dependencies

- **F-1:** decision must precede **S6**. If "remove," the S6 briefing strip + structured item rows migrate into the Edit read-state / an expandable list card instead of a standalone screen.
- **F-2:** depends on S5 (lists) and S6 (details) existing; needs a responsive-design spec first.
- **F-3:** depends on a new `shared` reverse query; surfaces on Unit detail (S6) once built.
- **F-4:** depends on the data model already capturing instructions/ball counts/clubs/focus cues (it does); needs an IA seam reserved during the screen stages.

## Affected screens

- F-1: Sessions list → Session detail → Session edit (navigation graph).
- F-2: Units list / Sessions list ↔ detail on expanded width classes.
- F-3: Unit detail (new affordance) → Sessions.
- F-4: a new run/play surface reached from Session detail.

## Likely files

- **F-1:** `androidApp/.../ui/RangeworkApp.kt` (route removal/redirect), `SessionDetailScreen` (delete or fold into edit read-state), `SessionsListScreen` (expandable card option).
- **F-2:** `RangeworkApp.kt` responsive layout (the existing compact/expanded split), list + detail composables to support a two-pane scaffold.
- **F-3:** `shared/.../data/Supabase*Repository.kt` + a new reverse-lookup use case in `DataFoundation.kt`; `shared/src/commonTest`; `UnitDetailScreen` affordance.
- **F-4:** new run-mode screen(s) under `androidApp/.../ui/`; possibly new `shared` derivations (per-step sequencing) — no schema change anticipated.

## New components required

- **F-1:** none (removal/relocation).
- **F-2:** a responsive list-detail pane scaffold (new layout pattern) — the one genuinely new structural component in the deferred set.
- **F-3:** a "Used in N sessions" affordance on Unit detail (reuses S2 `ListEntryCard`/chips for the linked sessions).
- **F-4:** run-mode surface (timer/step sequencer) — would reuse `RangeworkMono` for counts/timers per `CLAUDE.md`, but needs its own design pass.

## Validation checklist

- [ ] **Decisions recorded** (product owner) for F-1, F-2, F-3, F-4 before any of this work starts.
- [ ] For F-1 specifically: decision logged **before S6** begins.
- [ ] Each item, when promoted, gets its own stage plan mirroring this template (objective → regression risks) and its own `commonTest`/UI tests.
- [ ] No item here is merged as part of S1–S10.
- [ ] If F-1 = remove: confirm no functionality (duplicate, focus-cue display, briefing) is lost in the relocation.
- [ ] If F-3 built: RLS-correct reverse query scoped to `auth.uid()`; deletion-safety warning ("used in N sessions") wired into the delete flow.

## Accessibility requirements

- Same AA / 48dp / non-colour-only / TalkBack standards as S10 apply to anything eventually built here.
- F-2 two-pane: focus order and back behaviour across panes must be defined for TalkBack and keyboard.
- F-4 run-mode: timer/step announcements and large-touch controls for outdoor one-handed use are a primary requirement.

## Regression risks

- **R6 / F-1:** the highest-risk gate — building the S6 Session-detail redesign and then removing the screen wastes work. Resolve first. Removing a screen also changes deep-link/back-stack behaviour.
- **R10 / F-2:** a list-detail pane is a significant responsive-layout change that can regress the existing compact/expanded behaviour the spec requires.
- **F-3:** a reverse query adds a read path and a deletion-safety branch — must not slow unit detail load or break existing delete behaviour; ownership scoping is critical.
- **F-4:** largest scope; risks pulling the whole program toward a feature build. Keep strictly out of the redesign program until explicitly prioritized (R11).
- General: because these are deferred, the surrounding code will have moved on — any promoted item must re-baseline against the then-current screens before starting.
