# Rangework Redesign — Deferred & Unimplemented Changes

Exhaustive list of items that were proposed in the prioritised backlog (`06-findings/prioritized-roadmap.md`) or noted within individual stage `changes.md` files but were not implemented across S1–S10. Ordered by user impact, highest to lowest.

Backlog IDs (B01–B60) reference `prioritized-roadmap.md`. F-series IDs reference Section F of `08-implementation/master-roadmap.md`. Stage notes (e.g. "S7 deferred") reference the `Deferred / Regression risks` sections of the relevant `changes.md`.

---

## 1. Run / "Follow at the range" mode

**Source:** Systemic finding 3.5; master roadmap F-4; S11 item  
**Backlog:** Not assigned a B-ID — treated as a strategic gap, not a UI polish item  
**Impact:** The app's stated promise is to let golfers plan _and_ follow a session at the range. Currently the app stops at planning. There is no screen for executing a session (step sequencing, countdown timer, ball counts ticked off in real time). This is the single largest functional gap between what the product promises and what it delivers.  
**Why deferred:** Scope — requires a new run-mode surface with per-step sequencing, timers, and one-handed outdoor controls. Kept outside the redesign programme. The underlying data model (instructions, ball counts, focus cues, clubs) already supports it.  
**Pre-requisites when ready:** IA seam reserved in S6 Session detail (accessible from the briefing strip); `RangeworkMono` timer display already specified; no schema change anticipated.

---

## 2. Drag-to-reorder gesture (B01)

**Source:** Backlog B01; S2 R3; S7 R3  
**Backlog rank:** 27 · ROI 20.25 · Impact 9 (highest impact in the entire backlog)  
**Impact:** The `DragHandle` icon is rendered on every instruction and session-item row in both editors, but it is a visual affordance only — the touch gesture is not wired. Reordering requires tapping the ↑/↓ chevrons instead. Users who attempt to drag will get no feedback and no result.  
**Why deferred:** Requires a reorder-gesture library (or custom gesture handling) that must be compatibility-spiked against the project's Compose/Java 17/SDK 35 toolchain before adoption. The risk of a toolchain incompatibility was high enough that the spike was left for a follow-up. The chevron fallback remains as the accessible mechanism (required by the Material a11y guidance regardless).  
**Pre-requisites when ready:** Compatibility spike first; then replace the chevron-based callback in `ReorderableItemRow.kt` with a gesture-backed drag. Do not bump SDK/toolchain casually (`CLAUDE.md`).

---

## 3. Remove the standalone Session-detail waypoint screen (B11 / F-1)

**Source:** Backlog B11; master roadmap F-1; S11 item  
**Backlog rank:** 44 · ROI 9.8 · Impact 7  
**Impact:** The Sessions list → Session detail → Edit Session navigation creates a "read-only preview" waypoint that adds a tap with little benefit. The backlog flags this as a navigational dead-end. Although the Session detail screen was substantially redesigned in S6 (briefing strip, structured item rows, app-bar actions), the decision of whether to remove it entirely was never made. Users still pass through an extra screen before they can act.  
**Why deferred:** Conflict: the S6 redesign _improved_ the detail screen as a standalone destination, while B11 proposes deleting it. Building both in sequence would waste work. The decision gate (F-1) was required before S6 but was not resolved.  
**Pre-requisites when ready:** Product decision logged first. If "remove": migrate the briefing strip and item rows into an expandable card on the Sessions list or a read-state within the editor; update the navigation graph in `RangeworkApp.kt`; verify deep-link and back-stack behaviour.

---

## 4. "Used in N sessions" reverse link (F-3)

**Source:** Systemic finding 4.2; master roadmap F-3; S11 item  
**Backlog:** Not assigned a B-ID — classified as a "delight / strategic" item  
**Impact:** From a Unit detail screen, a user has no way to see which sessions use that unit. This matters in two situations: (1) before deleting a unit — there is no warning that N sessions depend on it; (2) when reusing a unit — there is no path from the unit to the sessions built around it.  
**Why deferred:** Absent from every redesign spec. Requires a new reverse query in `shared/data/Supabase*Repository.kt`, a use case in `DataFoundation.kt`, and a new affordance on `UnitDetailScreen`. RLS scoping of the reverse query is non-trivial.  
**Pre-requisites when ready:** New `shared/commonTest` coverage for the reverse query; deletion-safety warning wired into the unit delete confirm dialog; ownership-scoped PostgREST query confirmed correct before shipping.

---

## 5. Undo snackbar for instruction / session-item deletes within editors (B18 — partial)

**Source:** Backlog B18; S7 deferred note  
**Backlog rank:** 17 · ROI 28.0 · Impact 7  
**Impact:** When a user deletes an individual instruction (in the Unit editor) or a session item (in the Session editor), the row disappears immediately with no undo path. The entity-level undo (deleting a whole unit or session from the detail screen) was implemented in S6 via the `UndoSnackbar` + `restoreUnit`/`restoreSession` mechanism, but within-editor sub-item deletion is permanent.  
**Why deferred:** Requires per-editor `SnackbarHostState` wiring and a restore callback that re-inserts the deleted item at the correct index in the in-progress draft — described in S7 as "major VM surgery" relative to the stage scope. The 48 dp delete icon target (B38) was delivered; the undo is the outstanding half of B18.  
**Pre-requisites when ready:** Per-editor snackbar host; transient deleted-item state held in the ViewModel until snackbar dismisses; `ReorderableItemRow` delete callback plumbed through to the restore path.

---

## 6. Session item notes displayed on session detail

**Source:** S6 regression risk note  
**Backlog:** Implicit in the session detail spec; no dedicated B-ID  
**Impact:** The Session item editor collects per-item notes (accessible via the "More options" expander). Those notes are not surfaced anywhere on the Session detail screen. A user reviewing their session plan before going to the range cannot see any per-item notes they wrote.  
**Why deferred:** Not in the S6 item-row spec. The structured `SessionItemDetailRow` shows unit name, ball count, repeat count, and club override, but notes were intentionally omitted from the spec to keep the row compact.  
**Pre-requisites when ready:** Decide display format (always visible, or collapsed behind a "More" toggle on the row); then extend `SessionItemDetailRow` in `SessionDetailScreen.kt`.

---

## 7. Swipe-to-edit/delete on list cards (B31)

**Source:** Backlog B31  
**Backlog rank:** 48 · ROI 7.5 · Impact 5  
**Impact:** Android users expect swipe gestures as a shortcut for edit/delete on list items. The redesigned list cards are tappable and have overflow menus, but there is no swipe affordance. Discoverability of the delete action relies entirely on users opening the overflow.  
**Why deferred:** Not mentioned in any stage implementation plan or changes file. It was low-confidence (Conf 6) and relatively high effort (Eff 4) in the backlog, placing it at rank 48. The tappable card body and overflow menu were prioritised instead.  
**Pre-requisites when ready:** Compose `SwipeToDismiss` or equivalent; visual reveal state (edit icon on one side, delete on the other); must not conflict with the existing card tap-to-detail navigation.

---

## 8. Tablet list-detail pane (F-2)

**Source:** Systemic finding 5.3; master roadmap F-2; S11 item  
**Backlog:** Not assigned a B-ID  
**Impact:** On expanded-width devices (tablets), the app uses a navigation rail and a two-column Overview, but lists (Units, Sessions) and detail screens remain single-pane. The half-adapted layout means tablet users navigate through the same tap-depth as phone users despite having available screen real estate for a side-by-side list/detail.  
**Why deferred:** No redesign spec defines a canonical two-pane scaffold. Requires its own design pass before implementation. The responsive nav pattern (compact bottom bar / expanded rail) is preserved and must not regress.  
**Pre-requisites when ready:** Spec for two-pane list-detail on expanded width; new scaffold layout in `RangeworkApp.kt`; focus-order and back-behaviour for TalkBack across panes defined explicitly.

---

## 9. Terms & Privacy Policy URL wiring (LegalLine — from S4)

**Source:** S4 regression risk R4  
**Backlog:** Not assigned a B-ID  
**Impact:** The login screen now shows "Terms" and "Privacy Policy" as underlined, tappable links inside `LegalLine`. Tapping either does nothing — the `onClick` handler is a no-op stub. Users who tap expecting to read the policies will get no response.  
**Why deferred:** Policy URLs were not yet published at the time of S4 implementation. The `ClickableText` annotations are already in place with `"TERMS"` and `"PRIVACY"` tags; only the URL wiring and intent/WebView launch remain.  
**Pre-requisites when ready:** Publish policy URLs; wire tags in `LegalLine`'s `onClick` to open the URLs via an `Intent` or an in-app WebView. Also note the `ClickableText` deprecation below (item 11).

---

## 10. Collapsing TopAppBar on detail screens (B56)

**Source:** Backlog B56  
**Backlog rank:** 59 · ROI 5.0 · Impact 4  
**Impact:** The detail screens (Unit detail, Session detail) use a fixed-height `CenterAlignedTopAppBar`. The Material 3 spec supports a `LargeTopAppBar` / `MediumTopAppBar` that collapses as the user scrolls, keeping the screen title visible but recovering vertical space when reading content. This is a polish item — the current fixed bar is functional.  
**Why deferred:** Lowest-ROI item in the backlog (rank 59, confidence 5). The effort (Eff 4) is disproportionate to the incremental polish gain. Never scheduled into any stage.  
**Pre-requisites when ready:** Replace `CenterAlignedTopAppBar` in the shared shell (`RangeworkApp.kt`) with `LargeTopAppBar` or `MediumTopAppBar` with `scrollBehavior`; verify trailing actions (Edit, overflow) still render correctly when collapsed.

---

## 11. Login app icon — decorative TalkBack description (from S4/S10)

**Source:** S4 regression risk R3; S10 validation checklist (not addressed)  
**Backlog:** Not assigned a B-ID — accessibility fix  
**Impact:** The Rangework mark on the login screen is purely decorative, but `BrandMarkContainer` has a hard-coded `contentDescription = "Rangework mark"`. TalkBack announces this to screen-reader users on every visit to the login screen, adding a meaningless focus stop before reaching the sign-in button.  
**Why deferred:** Fixing it requires adding a `contentDescription` parameter to the shared `BrandMarkContainer` component (which is used on other screens where the description may be appropriate). The change was deferred from S4 to S10 but was not addressed in S10 — only the contrast and non-colour destructive-action fixes were made.  
**Pre-requisites when ready:** Add an optional `contentDescription: String? = "Rangework mark"` parameter to `BrandMarkContainer`; pass `null` at the login call site; verify TalkBack on both login and any other `BrandMarkContainer` usage.

---

## 12. `ClickableText` → `LinkAnnotation` / `BasicText` migration (from S4/S10)

**Source:** S4 regression risk R1; S10 validation checklist (not addressed)  
**Backlog:** Not assigned a B-ID — technical debt / accessibility  
**Impact:** `LegalLine` in the login screen uses the Compose `ClickableText` API, which is soft-deprecated. The compiler emits a deprecation warning. The correct replacement is `Text` or `BasicText` with `LinkAnnotation`. This is a low-urgency item with no user-visible behaviour difference until the Terms/Privacy URLs are wired (item 9 above), at which point using `LinkAnnotation` also improves TalkBack's handling of the inline links.  
**Why deferred:** Purely technical; the component is fully functional. Deferred from S4 to S10 but not addressed in S10 — the S10 sweep focused on contrast, touch targets, and non-colour destructive signals.  
**Pre-requisites when ready:** Replace `ClickableText` with `Text` and `LinkAnnotation` / `AnnotatedString` in `LegalLine` inside `RangeworkApp.kt`. Best done together with item 9 (URL wiring) to avoid touching the composable twice.

---

## Summary table

| #   | Item                                        | Backlog ID    | Impact                                   | Stage           | Implement                                                      |
| --- | ------------------------------------------- | ------------- | ---------------------------------------- | --------------- | -------------------------------------------------------------- |
| 1   | Run / "follow at the range" mode            | F-4           | Highest — core missing feature           | S11             | No. This is a large feature that requires a more detailed plan |
| 2   | Drag-to-reorder gesture                     | B01           | 9 / 10 — visual only, gesture missing    | S11             | Yes                                                            |
| 3   | Remove Session-detail waypoint              | B11 / F-1     | 7 / 10 — navigation dead-end unresolved  | S11             | No. This screen should remain                                  |
| 4   | "Used in N sessions" reverse link           | F-3           | High — deletion safety, reuse visibility | S11             | Yes                                                            |
| 5   | Undo for instruction/item delete in editors | B18 (partial) | 7 / 10 — permanent data loss in editors  | S7 deferred     | No. This is a large feature that requires a more detailed plan |
| 6   | Session item notes on session detail        | —             | Medium — planned data not shown          | S6 deferred     | Yes                                                            |
| 7   | Swipe-to-edit/delete on lists               | B31           | 5 / 10 — discoverability gap             | Never scheduled | Yes                                                            |
| 8   | Tablet list-detail pane                     | F-2           | Medium — tablet UX gap                   | S11             | No. We will do a proper tablet UI pass at another stage        |
| 9   | Terms & Privacy URL wiring                  | —             | Low-medium — dead links                  | S4 stub         | No                                                             |
| 10  | Collapsing TopAppBar on detail              | B56           | 4 / 10 — polish only                     | Never scheduled | Yes                                                            |
| 11  | Login icon decorative a11y description      | —             | Low — TalkBack annoyance on login        | S4→S10 missed   | Yes                                                            |
| 12  | `ClickableText` deprecation migration       | —             | None (user-facing) — technical debt      | S4→S10 missed   | Yes                                                            |
