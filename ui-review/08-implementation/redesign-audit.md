# Rangework Redesign — Delivery Audit (v1)

An independent review comparing the **delivered redesign** (S1–S10 code on `main`) against what the redesign program **intended to deliver** (`07-redesigns/*` specs, the `08-implementation/master-roadmap.md` change inventory, and the backlog items B01–B60 each spec cites).

This audit looks for inconsistencies, missing UI tokens/features, and places where the implementation diverges from the planned spec. Findings are ordered by **user impact, highest first**.

> **Scope note — no v1 screenshots were available.** `08-implementation/screenshots-v1/` exists but is **empty**. There were no new screenshots to review, so this audit is a **code-vs-spec** review only. Visual-only properties (exact spacing, real-device contrast, font scaling, IME/keyboard overlap) could not be verified and are called out where relevant.
>
> **Not repeated here:** the items already catalogued in `deferred-changes.md` (run mode, drag-to-reorder gesture, session-detail waypoint, "used in N sessions", in-editor undo, session-item notes on detail, swipe gestures, tablet two-pane, Terms/Privacy URLs, collapsing app bar, login-mark a11y, `ClickableText` deprecation). This audit surfaces discrepancies **not** already self-reported in that file.
>
> **S11 coverage:** Items fully resolved by the S11 implementation plan have been removed from this list. Finding #9 (top bar type) is partially addressed by S11-5 for detail screens only — see the updated entry for what remains.

---

## Summary table

| #   | Finding                                                                                                                                | Spec / source                     | Impact  |
| --- | -------------------------------------------------------------------------------------------------------------------------------------- | --------------------------------- | ------- |
| 1   | Empty lists show **two competing create affordances**; sparse-list Extended FAB (B57) never delivered                                  | unit/session-list specs, B02, B57 | High    |
| 2   | Ball count is **not the dominant numeral** in detail summary strips                                                                    | unit/session-detail, B13          | High    |
| 3   | Unit-detail summary has **2 of 3 stat blocks** (no default-club block)                                                                 | unit-detail spec                  | Medium  |
| 4   | **Twin editors diverge** — session editor doesn't reuse `ReorderableItemRow`                                                           | master-roadmap §1.2, R8           | Medium  |
| 5   | **Terminology lock regressed** — notes no longer scope-prefixed except in session editor                                               | systemic 8.1, S1                  | Medium  |
| 6   | Session-item **focus cue is unlabelled and untinted**                                                                                  | session-detail, B16/B60           | Medium  |
| 7   | **`CenterAlignedTopAppBar`** on list/editor/settings/overview — spec'd left-aligned M3 Small bar _(detail screens addressed by S11-5)_ | list/editor screens, B34          | Low-med |
| 8   | List cards use filled `Card`, not `OutlinedCard`; tier order/type differ                                                               | unit/session-list spec            | Low-med |
| 9   | **FocusCard has no icon**                                                                                                              | unit/session-detail, B16          | Low     |
| 10  | Overview "Next move" eyebrow still at **0.7 alpha** (the dimming S10 fixed elsewhere)                                                  | S10, systemic 9.1                 | Low     |
| 11  | Overview **recents omit spec'd metadata**; "Resume editing" drops entity name                                                          | overview spec, B26/B27            | Low     |
| 12  | Editor screens hold an **unused `SnackbarHostState`** (no in-editor save/undo snackbar)                                                | unit/session-edit, B41/B18        | Low     |

---

## High impact

### 1. Empty lists show two competing create affordances; the sparse-list Extended FAB (B57) was never delivered

**Spec.** Both list redesigns are explicit on two points:

- _"The FAB hides in this state to avoid two competing affordances; the inline button is the single clear action (B02)."_
- _"On sparse lists (1–2 items) it becomes an Extended FAB … collapsing to a standard FAB once the list grows (B57)."_

**Delivered.** The logic is inverted. In [RangeworkApp.kt:539](androidApp/src/main/java/com/loganmartlew/rangework/android/ui/RangeworkApp.kt:539) the Extended FAB is shown **when the list is empty**, and a standard FAB is shown whenever the list is non-empty — _regardless of length_. The empty-state card ([EmptyStateCard.kt:58](androidApp/src/main/java/com/loganmartlew/rangework/android/ui/components/EmptyStateCard.kt:58)) always renders its own `FilledTonalButton` CTA. So on an empty Units or Sessions screen the user sees **both** "Create your first unit" (card) **and** an Extended "New unit" FAB — the exact double-affordance B02 set out to remove. Conversely, B57's actual benefit (a labelled FAB while the list is short, e.g. 1–2 items) is never realized, because any non-empty list collapses straight to the icon-only FAB.

This was documented as intentional in `stage-05/changes.md` ("list screens show the extended variant while their list is empty"), but it contradicts the two backlog items the stage was meant to satisfy.

**Fix direction.** Hide the FAB entirely when the list is empty (let the card CTA stand alone); show the Extended FAB when `1..2` items exist; collapse to the standard FAB at 3+.

### 2. Ball count is not the dominant numeral in detail summary strips (B13's core intent)

**Spec.** Both detail redesigns make ball count the headline fact: _"Ball count is the number a golfer actually plans around, so it gets the largest type on the screen"_ — `headlineSmall`/`displaySmall` for the ball total, smaller captions/numerals for the rest (B13).

**Delivered.** `BriefingRow`/`StatBlock` render **every** stat at the same `RangeworkMono.medium` size ([StatBlock.kt:28](androidApp/src/main/java/com/loganmartlew/rangework/android/ui/components/StatBlock.kt:28)). On Unit detail (balls · instructions) and Session detail (balls · units · est. time) the three figures are visually equal — the ball total has no typographic dominance. The single most-emphasized "scan anchor" the redesign promised is absent; the briefing reads as a row of equally-weighted numbers.

**Fix direction.** Give `StatBlock` a prominence/size variant and render the ball total larger than the supporting stats.

---

## Medium impact

### 3. Unit-detail summary shows 2 of the 3 spec'd stat blocks

The unit-detail spec calls for a three-block summary: _"a prominent total ball count … instruction count, and the default club as a chip (B13)."_ The implementation's `BriefingRow` carries only **balls + instructions** ([UnitDetailScreen.kt:51](androidApp/src/main/java/com/loganmartlew/rangework/android/ui/screens/UnitDetailScreen.kt:51)); the default club is relegated to a separate "Default club" `EntryHighlightCard` further down. This breaks the intended briefing parity with Session detail (which does render three blocks) and demotes the club from an at-a-glance fact to a buried card.

### 4. Twin editors diverge — the session editor does not reuse `ReorderableItemRow`

The master roadmap lists `ReorderableItemRow` as a shared S2 component consumed by _both_ editors, with R8 ("twins drift") explicitly called out as a risk. The Unit editor uses it ([UnitEditorScreen.kt:225](androidApp/src/main/java/com/loganmartlew/rangework/android/ui/screens/UnitEditorScreen.kt:225)). The Session editor **reimplements** a different row layout inline — drag-handle + badge + subtotal + delete in a header row, with the ↑/↓ chevrons in a _separate_ trailing row ([SessionEditorScreen.kt:280](androidApp/src/main/java/com/loganmartlew/rangework/android/ui/screens/SessionEditorScreen.kt:280)-415). The two editors therefore place their reorder/delete controls differently and will drift further with any future change — the precise outcome the shared-component strategy was meant to prevent. Note: S11-1 wires the drag gesture on both editors but does not consolidate them onto a shared component — the structural divergence remains after S11.

### 5. Terminology lock regressed — notes are no longer scope-prefixed (except in the session editor)

S1 applied the terminology lock (systemic 8.1): "Notes" → **"Unit notes"** / **"Session notes"**. Later stages overwrote it inconsistently:

- Unit editor field label is **"Notes"** ([UnitEditorScreen.kt:109](androidApp/src/main/java/com/loganmartlew/rangework/android/ui/screens/UnitEditorScreen.kt:109)).
- Unit detail card title is **"Notes"** ([UnitDetailScreen.kt:60](androidApp/src/main/java/com/loganmartlew/rangework/android/ui/screens/UnitDetailScreen.kt:60)).
- Session detail card title is **"Notes"** ([SessionDetailScreen.kt:75](androidApp/src/main/java/com/loganmartlew/rangework/android/ui/screens/SessionDetailScreen.kt:75)).
- Only the **session editor** still scopes them ("Session notes", "Item notes").

So the same concept is labelled two different ways across the app — the inconsistency the lock was supposed to eliminate.

### 6. Session-item focus cue is unlabelled and untinted

The session-detail spec wants each item's focus cue rendered as _"a focus-cue line … tinted to stand apart (B16, B60)"_ with an icon. The implementation prints the cue as a plain `bodySmall`/`onSurfaceVariant` line with **no label, no icon, and no tint** ([SessionDetailScreen.kt:202](androidApp/src/main/java/com/loganmartlew/rangework/android/ui/screens/SessionDetailScreen.kt:202)). To a user it reads as an anonymous grey sentence beneath the item rather than a distinguished mental cue. (Unit detail does tint its cue via `FocusCard`; session items don't — another twin inconsistency.)

---

## Low impact

### 7. `CenterAlignedTopAppBar` on list/editor/settings/overview screens — spec'd left-aligned M3 Small bar

Every spec specifies _"TopAppBar (M3 Small, pinned). Title 'X' only (B34)"_ — the M3 small top app bar left-aligns its title after the leading element. The shell uses `CenterAlignedTopAppBar` globally ([RangeworkApp.kt:584](androidApp/src/main/java/com/loganmartlew/rangework/android/ui/RangeworkApp.kt:584)), centering the title on every screen. S11-5 will switch **detail screens** to `MediumTopAppBar` (which left-aligns its collapsed title), but list, editor, settings, and overview screens will remain center-aligned after S11. Whether centering is a deliberate design choice or a spec deviation is worth confirming against the wireframes. Also note that detail-screen titles set no `maxLines`/overflow, so long unit/session names may not truncate as the spec required.

### 8. List cards use a filled `Card`, not the spec'd clickable `OutlinedCard`; tier order and type differ

The list specs call for a clickable `OutlinedCard` with Tier 1 `titleMedium`, and a **single metadata row** combining the club pill + "N instructions · N balls". The delivered `ListEntryCard` ([ListEntryCard.kt:38](androidApp/src/main/java/com/loganmartlew/rangework/android/ui/components/ListEntryCard.kt:38)) is a filled `Card` (`surface`), the title is `titleSmall`, and the count lives on its own subtitle line **above** a separate metadata row that holds only the club chip — so club and counts are on different lines rather than the one structured metadata row the spec described. Cosmetic, but it's a structural deviation from the three-tier template and from "scan identically" parity.

### 9. FocusCard has no icon

The detail specs want the Focus card to carry a _"target/center-focus icon"_ (B16). The delivered `FocusCard` is label + value text only, no icon ([FocusCard.kt:26](androidApp/src/main/java/com/loganmartlew/rangework/android/ui/components/FocusCard.kt:26)).

### 10. Overview "Next move" eyebrow still uses 0.7 alpha — the dimming S10 removed elsewhere

S10's headline contrast fix was removing `.copy(alpha = 0.7f)` from the FocusCard label because it failed AA in dark mode. The identical pattern survives in the Overview Next-move eyebrow: `onPrimaryContainer.copy(alpha = 0.7f)` ([OverviewScreen.kt:296](androidApp/src/main/java/com/loganmartlew/rangework/android/ui/screens/OverviewScreen.kt:296)). The S10 sweep didn't cover the Overview surface (built in S9), so the same likely-sub-AA label dimming remains in one place.

### 11. Overview recents omit spec'd metadata; "Resume editing" drops the entity name

- The overview spec says each recent card shows _"the name, type chip (Unit / Session), and headline metadata (ball count, club)."_ Delivered unit recents show balls **or** instruction count but never the club; session recents show only unit count (no ball count) ([OverviewScreen.kt:354](androidApp/src/main/java/com/loganmartlew/rangework/android/ui/screens/OverviewScreen.kt:354)-372).
- The type chip is a hand-rolled `Box` rather than the spec'd `AssistChip`.
- The contextual Next-move "just edited" branch reads _"Resume editing your unit"_ rather than the spec's name-bearing _"Resume editing 'Test'"_ ([OverviewScreen.kt:268](androidApp/src/main/java/com/loganmartlew/rangework/android/ui/screens/OverviewScreen.kt:268)) — the personalization the contextual card was meant to provide is lost.

### 12. Editor screens hold an unused `SnackbarHostState`

Both editors create a local `SnackbarHostState` and wire a `SnackbarHost` ([UnitEditorScreen.kt:67](androidApp/src/main/java/com/loganmartlew/rangework/android/ui/screens/UnitEditorScreen.kt:67), [SessionEditorScreen.kt:97](androidApp/src/main/java/com/loganmartlew/rangework/android/ui/screens/SessionEditorScreen.kt:97)) but nothing is ever shown through it — the save confirmation (B41) comes from the shell-level status snackbar after navigation, and in-editor delete-undo (B18) is deferred and explicitly out of S11 scope. The host is dead code today; it's the visible loose end of the B41/B18 work and a hook someone may wrongly assume is functional.

---

## Verified non-issues (checked, not problems)

- **Number-badge numbering is consistent.** Detail screens pass `instruction.order`/`item.order` while editors pass `index + 1`; this is _not_ an off-by-one, because `DraftValidation` normalizes `order = index + 1` on save ([DraftValidation.kt:48](shared/src/commonMain/kotlin/com/loganmartlew/rangework/shared/model/DraftValidation.kt:48)), so persisted orders are 1-based and the two paths agree.
- **Session sticky total includes duration** ("N balls · ~M min") per B12/B15 ([PlannerFormatting.kt:12](androidApp/src/main/java/com/loganmartlew/rangework/android/ui/PlannerFormatting.kt:12)).
- **Google sign-in button** is Identity-compliant (white container, multicolour G, border) per B23.
- **Settings structure** (Preferences → Clubs → Account → About), the single Club-bag row with enabled count, the Sign-out list item, and the new Manage clubs screen (grouped subheaders, search, presets, per-club switch semantics) match the settings spec well.
- **Dependency-aware empty/add-item routing** (B19) is correctly wired on both the Sessions list and the Session editor.
- **Dark-mode navigation active-indicator pill is effectively invisible (~1.4:1)** In reality the pill is easily visible despite a low contrast ratio. Do not change this.

---

## What landed well

The redesign delivered the large majority of its scope: the M3 tonal palette and surface tokens, the green primary/`primaryContainer` FAB and Save buttons (B07/B09), the docked Save bar, the sticky session total with duration (B12/B15), the three-tier list cards with overflow menus and Duplicate (B04/B29/B30), dependency-aware empty states (B19), app-bar Edit/overflow-Delete with confirm + entity-level undo (B35/B06), number badges (B21), steppers (B05), progressive-disclosure expanders (B40), the contextual Next-move + recents Overview (B25/B26/B27), the full Settings restructure + Manage clubs screen (B10/B17/B44/B46/B47/B59), and the Google Identity sign-in button (B23). The gaps above are mostly **degree-of-fidelity** misses (prominence, tinting, single-vs-double affordance, terminology drift) rather than missing screens — but several (#1, #2, #3) are user-felt and worth closing before this is considered "done."
