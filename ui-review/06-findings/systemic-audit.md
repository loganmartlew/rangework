# Rangework — Principal Product Design Review

A systemic review of the planning experience, holding Material 3 and Android-native patterns constant. Severity scale: **Critical** (blocks task / breaks trust), **High** (meaningful friction or confusion), **Medium** (noticeable but workable), **Low** (polish).

---

## 1. Overall strengths

The two-level model (units → sessions) is genuinely sound. It maps cleanly to how golfers actually think — drills as building blocks, sessions as the plan — and the app commits to it consistently across list, detail, and edit screens. That conceptual integrity is the most valuable asset here and shouldn't be disturbed.

The ball-count tally is the product's sharpest idea. Surfacing "18 balls" on the session card, in the summary, and live during editing answers the one question that actually matters before leaving the house. It's a real differentiator and it's threaded through the experience well.

Detail screens are well-structured: the Summary → Focus → Instructions ordering on a unit, and Summary → Items on a session, give a predictable reading rhythm. The list cards are scannable, showing count + ball total + a content preview. And the app correctly uses Android-native primitives throughout — bottom nav, FAB, M3 chips, segmented buttons, switches — so it already feels like it belongs on the platform.

---

## 2. Overall weaknesses

The systemic weaknesses cluster into four themes, which the rest of this review expands:

The app is **built but not yet onboarded**. It assumes you already understand units vs. sessions. There's no empty-state teaching, no first-run guidance beyond the welcome card, and the vocabulary ("unit," "item," "slot") is internally inconsistent.

It's **list-shaped, not workflow-shaped**. The core loop is "build a unit, then build a session from it," but nothing in the UI guides that sequence except a buried sentence on Overview. Cross-linking between the two halves is absent.

**Editing is heavier than it needs to be.** Full-screen forms, no inline validation visible, destructive actions sitting beside reorder controls, and duplicated detail/edit layouts increase both cognitive load and error risk.

**Destructive and irreversible actions lack confirmation affordances** (or they aren't shown), which is the single biggest trust risk in the build.

---

## 3. Major UX issues

**3.1 — No empty states that teach. (Severity: High)**
Every list screen presumably starts empty, but the screenshots suggest the only guidance a new user gets is the Overview welcome card. _Why it matters:_ the unit→session dependency is invisible. A user who lands on Sessions first cannot build anything and isn't told why or where to start. _Suggested solution:_ purpose-built empty states on Units and Sessions that name the dependency and route forward ("Sessions combine your units. Create a unit first →"). Empty Sessions should point at Units; empty Units should explain what a unit is.

**3.2 — Destructive actions lack guarded confirmation. (Severity: Critical)**
"Delete unit," "Delete session," and the per-instruction/per-item trash icons appear to act directly. Deleting a _unit_ that is referenced by a _session_ is especially dangerous — it can silently break session ball counts. _Why it matters:_ irreversible data loss with no undo erodes confidence fast, and the referential link makes it worse. _Suggested solution:_ M3 confirmation dialog for entity deletion; **Snackbar with Undo** for inline instruction/item removal (more native and less interruptive than a dialog for small reversible edits); and a specific warning when deleting a unit that's used in N sessions.

**3.3 — Reorder and delete controls are adjacent and unlabeled. (Severity: High)**
In Edit unit / Edit session, the up-chevron, down-chevron, and trash sit in a tight row at the card's bottom-right (Images 3, 4, 8). _Why it matters:_ the trash is the highest-consequence control yet shares a hit zone with the lowest-consequence ones (reorder), inviting misfires — and on a drill you've just typed out, an accidental delete is costly. _Suggested solution:_ separate destructive from non-destructive spatially; consider drag-handle reordering (more discoverable and native than chevrons) and move delete to an overflow or give it more isolation + an undo.

**3.4 — Repeat-count math is implicit. (Severity: Medium)**
A unit shows "3 balls"; the session item shows "Repeat 6× · 18 balls." The relationship (3 × 6) is never spelled out. _Why it matters:_ users can't sanity-check how a number was derived, which undercuts the very trust the ball-count feature is meant to build. _Suggested solution:_ show the formula inline ("3 balls × 6 = 18") at least in the edit context.

**3.5 — No way to act on a session at the range. (Severity: Medium, but strategically High)**
Sessions are _templates_ you build, but there's no visible "start / follow" mode. _Why it matters:_ the product's stated promise is "follow on the day," yet the artifact stops at planning. _Suggested solution:_ this is a roadmap item, not a screen fix — but the IA should leave a clear seam for a run/play mode so it isn't bolted on later.

---

## 4. Information architecture issues

**4.1 — Four top-level destinations with overlapping intent. (Severity: Medium)**
Overview, Units, Sessions, Settings. Overview largely _duplicates_ what Units and Sessions show (counts, entry points). _Why it matters:_ Overview competes with the two real workspaces for the same actions, and "two ways to start a unit" dilutes a clear primary path. _Suggested solution:_ decide whether Overview is a dashboard (read + launch) or redundant. If kept, make it strictly additive — surface things the lists can't (recent activity, "continue editing," totals across all sessions) rather than re-listing entry points.

**4.2 — The unit↔session relationship is one-directional and hidden. (Severity: High)**
From a session you can see which units it uses (Image 6), but from a _unit_ you cannot see which sessions depend on it. _Why it matters:_ this is the missing backbone of a two-level IA — without it, deletion is dangerous and reuse is invisible. _Suggested solution:_ add a "Used in N sessions" affordance on the unit detail, linking to those sessions. This single addition resolves several downstream issues (deletion safety, discoverability of sessions, perceived value of reuse).

**4.3 — Settings mixes account, appearance, units, and a 30-item club catalogue. (Severity: Medium)**
The club bag is a long scrolling toggle list (Images 10, 11) sitting in the same screen as theme and sign-out. _Why it matters:_ it's high-volume configuration buried alongside one-tap settings, and it directly governs what appears in dropdowns app-wide — a relationship users won't infer. _Suggested solution:_ give Club bag its own sub-screen with a count summary on the Settings row ("Club bag · 12 enabled"), and surface the connection ("Only enabled clubs appear in dropdowns").

---

## 5. Navigation issues

**5.1 — Inconsistent top-bar pattern between list and detail. (Severity: Medium)**
List screens show "Rangework · [Section]" with the logo; detail/edit screens show a back arrow + centered title (e.g. "Unit," "Edit unit"). The edit screens _also_ repeat the title as an H1 below the bar ("Edit unit" twice — Image 3). _Why it matters:_ the doubled title wastes vertical space on form-heavy screens and reads as a layout bug. _Suggested solution:_ drop the redundant in-body H1 on edit screens; let the app bar carry the title. Keep one consistent app-bar pattern per screen depth.

**5.2 — No persistent save / dirty-state affordance in long forms. (Severity: High)**
Save unit / Save session live at the very bottom of a scrolling form (Images 4, 8). _Why it matters:_ on a long edit, the primary action is offscreen, and there's no indication whether unsaved changes will survive a back-press. Combined with destructive controls mid-form, this is a real data-loss vector. _Suggested solution:_ either a pinned bottom save bar or app-bar save action, plus an "unsaved changes" guard on back/up navigation.

**5.3 — Tablet two-column layout has no analog on the rest of the IA. (Severity: Low)**
The spec notes the _overview_ expands to two columns on tablets, but nothing about how lists↔detail behave (list-detail pane is the native tablet pattern). _Why it matters:_ a half-adapted tablet layout feels unfinished. _Suggested solution:_ commit to a list-detail canonical pane layout for Units and Sessions on large screens.

---

## 6. Discoverability issues

**6.1 — Session duplication is invisible. (Severity: Medium)**
The spec lists "duplicate any session," but the session detail only shows Edit and Delete (Image 6). Duplicate presumably hides in the list's overflow (⋮). _Why it matters:_ duplication is the fastest path to a new session (the core reuse promise), yet it's the least visible action. _Suggested solution:_ surface Duplicate on the session detail alongside Edit/Delete, not only in the list overflow.

**6.2 — The club-bag → dropdown dependency is undiscoverable. (Severity: High)**
A user wondering "why isn't my wedge in the club picker?" has no path back to Settings. _Why it matters:_ a missing club in a dropdown reads as a bug, not a setting. _Suggested solution:_ in club dropdowns, add a trailing "Manage clubs" affordance that deep-links to the club bag.

**6.3 — Overflow menu contents are unknowable until tapped. (Severity: Low)**
List cards carry a ⋮ with unknown contents (likely edit/delete/duplicate). _Why it matters:_ hidden actions reduce confidence about what's possible. _Suggested solution:_ keep overflow, but ensure the highest-value action (open/edit) is the card tap itself, with overflow reserved for secondary actions — and keep its contents consistent across Units and Sessions.

---

## 7. Visual hierarchy issues

**7.1 — Primary vs. secondary button semantics are inverted in places. (Severity: Medium)**
On Overview, "New unit" is a light filled button and "New session" is green (Image 13); on detail screens "Edit" is light-filled and "Delete" is outlined-but-red-text. The green is also the nav-selection and "Add" color. _Why it matters:_ color is doing three jobs (brand accent, selected state, primary CTA) so emphasis no longer reliably signals "this is the main action." _Suggested solution:_ define a strict token hierarchy — one filled primary per screen, tonal for secondary, and reserve the error color exclusively for destructive. Don't let the accent green double as both "selected tab" and "primary CTA" without a rule.

**7.2 — Edit-form fields are low-contrast and uniformly weighted. (Severity: High — see also Accessibility)**
The outlined text fields on dark surfaces (Images 3, 7) have faint borders and labels; every field looks equally important, so Title, Notes, and Ball count carry the same visual weight. _Why it matters:_ users can't quickly find the field that matters, and faint outlines on near-black fail contrast. _Suggested solution:_ strengthen field-border and label contrast; consider grouping required vs. optional fields so the essential inputs lead.

**7.3 — Section cards and page background are too close in value. (Severity: Medium)**
On several screens the elevated cards barely separate from the background (Images 1, 6). _Why it matters:_ M3's tonal-elevation system is doing the grouping work, but the steps are too subtle to read as distinct containers. _Suggested solution:_ increase tonal-elevation contrast between surface levels so cards read as cards.

---

## 8. Cognitive load issues

**8.1 — Vocabulary is inconsistent across the same concept. (Severity: High)**
"Practice unit" / "unit" / "drill"; "session item" / "slot" / "item"; "focus" / "focus cue." _Why it matters:_ every synonym forces the user to re-map a concept they already learned — the single biggest avoidable load in a two-noun app whose entire value rests on those two nouns being crisp. _Suggested solution:_ lock a glossary — pick one term per concept (e.g. **Unit**, **Session**, **Session item**, **Focus cue**) and enforce it in every label, heading, and empty state.

**8.2 — Detail and Edit screens duplicate structure, doubling what's parsed. (Severity: Medium)**
A unit's detail (Summary/Focus/Instructions) and its edit form present nearly the same content in two layouts. _Why it matters:_ users mentally reconcile two representations of one object. _Suggested solution:_ keep them visually parallel so the edit screen reads as "the same thing, now editable," reducing re-orientation cost.

**8.3 — Numeric inputs (ball count, repeat count) are free-text. (Severity: Medium)**
Ball/Repeat counts appear as text fields (Images 3, 7). _Why it matters:_ free-text numerics invite empty, zero, or invalid values and force keyboard work for what's usually ±1 adjustments. _Suggested solution:_ native stepper / number-pad-constrained fields with sane min/defaults, removing a whole class of validation worry.

---

## 9. Accessibility concerns

**9.1 — Contrast on fields, hints, and secondary text. (Severity: High)**
Outlined-field borders, field labels, and tertiary card text (e.g. instruction previews, "Balls: 1") sit at low contrast on dark surfaces. _Why it matters:_ likely fails WCAG AA for text and UI-component contrast; affects everyone in sunlight — _especially relevant for an app used outdoors at a range._ _Suggested solution:_ audit against AA (4.5:1 text / 3:1 components); raise label, border, and helper-text contrast. Treat outdoor legibility as a first-class requirement, not a theme nicety.

**9.2 — Icon-only controls likely lack labels. (Severity: High)**
Chevron-up/down, trash, and ⋮ are icon-only (Images 4, 8). _Why it matters:_ without contentDescription they're invisible to TalkBack and ambiguous to everyone ("move where?"). _Suggested solution:_ add content descriptions ("Move instruction up," "Delete instruction," "More options for [unit name]") and ensure 48dp touch targets — the clustered controls look tight.

**9.3 — Destructive color carries meaning alone. (Severity: Medium)**
"Delete" and trash rely on red to signal danger. _Why it matters:_ color-only signaling fails for color-blind users. _Suggested solution:_ pair the color with an icon + explicit label, and gate with confirmation (ties to 3.2).

**9.4 — Segmented/toggle state may rely on color + checkmark only. (Severity: Low)**
Theme and Units segmented buttons (Image 9) show selection via fill + check. _Why it matters:_ generally OK, but verify the unselected/selected contrast delta meets component-contrast minimums. _Suggested solution:_ confirm AA on the segmented control's selected vs. unselected states.

---

## 10. Opportunities for delight

These are additive and native — no redesign required:

**Undo Snackbars** on every reversible delete. The single highest-confidence, lowest-cost win; it turns the scariest interactions into safe ones and is pure M3.

**"Used in N sessions" on units**, with a tap-through. Makes reuse _felt_, not just possible — it's the moment the two-level model pays off for the user.

**A live "what this costs you" line in session editing** — "This session: 18 balls · ~25 min" (if a rough per-ball time is reasonable). The ball count is already the star; giving it a time estimate makes the "before you leave the house" promise concrete.

**Smart defaults from the club bag and last-used values** — pre-fill default club, repeat counts, ball counts so the common path is mostly confirmation, not entry.

**A gentle first-run path** that walks: create a unit → build a session from it → see the total. Three steps, ending on the ball-count payoff, teaches the whole model in under a minute.

**Duplicate-and-tweak as a celebrated action** — since reuse is the core loop, making "duplicate session" prominent and instant rewards the behavior the product is built around.

---

## Systemic priorities (if tackling in order)

1. **Confirmation + Undo for all destructive actions** (3.2, 3.3, 9.3) — trust foundation.
2. **Lock the vocabulary** (8.1) — cheapest high-impact clarity fix.
3. **Make the unit↔session link bidirectional** (4.2, 6.1, delight) — unlocks safety, discoverability, and perceived value at once.
4. **Contrast & icon-labeling audit** (7.2, 9.1, 9.2) — accessibility + outdoor legibility.
5. **Teaching empty states + first-run** (3.1) — converts the strong model into an understood one.

The architecture is right. The work is making the model _legible and safe_ — not redrawing it.
