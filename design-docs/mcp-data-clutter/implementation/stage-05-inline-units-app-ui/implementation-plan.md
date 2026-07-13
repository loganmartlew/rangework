# Stage 5: Inline Units app UI  ⛳ ship point 2 (with Stage 6)

**Epic:** [`../../epic-implementation-plan.md`](../../epic-implementation-plan.md)
**Design:** [`../../design-decisions.md`](../../design-decisions.md) (§6–§8)
**Vocabulary:** [`apps/mobile/CONTEXT.md`](../../../../apps/mobile/CONTEXT.md) — **Inline Unit**, **Promotion**
**Depends on:** Stage 4 (inline-units foundation) merged — `PracticeUnit.scopedToSessionId` / `isInline`,
`PracticeLibrary.promoteUnit(id)`, the unfiltered `getUnit(id)` that still returns inline units,
`listUnits()` already excluding them, and deep-copy `duplicateSession`.
**Status:** proposed — awaiting owner sign-off on **D1** (how inline units reach the app's unit-resolution
surface), **D3** (promote affordance placement/copy), and **D4** (edit-inline navigation surface). D2 and
D5 are recommendations confirmable at the same time. Per the epic pipelining rule this plan drafts now;
implementation waits for the Stage 4 merge.

## Objective

Give Inline Units a face in the app. Stage 4 made the data layer and shared core fully inline-aware —
inline units are minted (MCP, Stage 6), owned by their session, excluded from the library, reachable by
id, promotable, cascaded, deep-copied — but **nothing in the app can see or manage them**. Because every
app surface resolves a session's units from `plannerUiState.units` (which is `listUnits()`, and Stage 4
makes that exclude inline units), an MCP-created session that contains an inline unit currently renders
its item as **"Missing unit"** with a null ball count. Stage 5 closes that gap and wires the two
management affordances the design calls for:

1. **Session detail and editor render inline units as first-class session content** — correct title,
   ball count, instructions echo, executable/start gating — by resolving over library **and** inline
   units, not the library alone.
2. **Edit** an inline unit from the session editing screen, reusing the **existing unit editor**
   navigated by unit id (design §8 — the editor needs no inline-awareness).
3. **Promote** an inline unit to the library from within its session (design §7 — user-initiated,
   one-way); the promoted unit then appears in the library and the session keeps referencing the same id.
4. Confirm the **unit library continues to exclude inline units** — this falls out of Stage 4's
   repository filter; Stage 5 **verifies**, it does not re-implement.

This is the app half of **ship point 2**. After Stage 5 + Stage 6 merge, a user can plan via MCP with an
inline drill, run the session, see the drill correctly in the app, edit it, and promote it — all without
the drill ever cluttering the library until they choose.

**No MCP (Stage 6) work here, and — per D1's recommendation — no `shared`, `supabase`, or model changes.**
Stage 4 owns the whole data/KMP/RPC surface; Stage 5 is `androidApp` UI + ViewModel plumbing that consumes
Stage 4's existing `getUnit(id)` and `promoteUnit(id)`. (D1 records the app-only path and the shared-list
alternative the owner may prefer for scale.)

## Dependencies

- **Stage 4 merged** — the plan below calls `library.getUnit(id)` (unfiltered; returns inline units by
  id), `library.promoteUnit(id)`, and reads `PracticeUnit.scopedToSessionId` / `isInline`. It relies on
  `listUnits()` already excluding inline units and on deep-copy `duplicateSession` already producing
  independent inline copies.
- Existing objects read or modified: `PracticePlannerViewModel` / `PracticePlannerUiState`,
  `PlannerActions` (`UnitEditorActions`, `SessionEditorActions`), `RangeworkApp` (the `SessionDetail`,
  `SessionEdit`, and unit-edit wiring), `SessionDetailScreen`, `SessionEditorScreen`, and one or two
  small components for the inline marker / promote control.
- **Zero `apps/mcp` changes.** No new shared, DB, or model objects under D1's recommended path — the two
  new ViewModel operations (`loadInlineUnits`, `promoteUnit`) call Stage 4 APIs that already exist.

## Decisions (owner review)

### D1 — How inline units reach the app's unit-resolution surface: an `inlineUnits` list + `allUnits` / `findUnit`, hydrated eagerly at refresh _(the one structural call of this stage)_

The whole app resolves a session's units from `plannerUiState.units`:

- `SessionDetailScreen` — `unitsById = plannerUiState.units.associateBy(id)`; drives titles, ball
  subtotals, `derivedBallCount`, `estimateSessionDurationMinutes`, and the `isSessionExecutable`
  Start gate.
- `SessionEditorScreen` — `unitsById` for the selected-unit title and per-item ball subtotal.
- `PracticePlannerViewModel.editUnit(id)` — `units.firstOrNull { it.id == id } ?: return`.
- Aggregate surfaces that derive ball counts per session (session list rows, Overview "recent",
  duration estimates) also resolve over `units`.

Stage 4 removes inline units from `units`, so **all of the above break for an inline-unit session**
unless Stage 5 adds a resolution surface. **Recommendation: mirror Stage 2's archived-session pattern
exactly** — a parallel list on the UI state plus combined-lookup helpers:

```kotlin
// PracticePlannerUiState
val inlineUnits: List<PracticeUnit> = emptyList()

val allUnits: List<PracticeUnit> get() = units + inlineUnits
fun findUnit(id: String): PracticeUnit? = allUnits.firstOrNull { it.id == id }
```

Every unit-resolution site routes through `allUnits` / `findUnit`; **the library-facing surfaces stay on
`units`** — the unit library list, and the session-editor item **picker** (`availableUnits`), which must
never offer an inline unit as a choice for another slot (design §6: an inline unit can never be referenced
by another session). This is the same split Stage 2 drew: `sessions` stays library-facing, `allSessions`
resolves detail.

**Hydration — eager, app-only, at refresh.** There is no "list inline units" API (Stage 4 declined one;
inline units are reached by id through their session), so the app gathers them by id. In `refreshPlanning`
(and the post-save / post-duplicate reconciles), after `listSessions()`, collect every
`item.practiceUnitId` across all sessions that is **absent from `units`**, and `library.getUnit(id)` each;
keep the non-null results whose `isInline` is true as `inlineUnits`. A genuinely deleted/missing unit id
returns null and still renders "Missing unit" exactly as today — the fetch-by-id naturally separates
"inline" from "gone."

- **Why eager, not lazy-per-screen:** aggregate views (session-list ball counts, Overview) resolve over
  `units` at all times; lazy per-detail loading would leave those wrong until each session is opened. One
  extra hydration pass at refresh keeps every surface correct with a single code path. At single-user
  scale the number of distinct inline ids is small (a handful of MCP sessions), so the sequential
  `getUnit` fan-out is negligible; if it ever isn't, the alternative below is the upgrade.
- **Alternative (owner may prefer for scale):** add a thin shared `PracticeLibrary.listInlineUnits()`
  (owner-scoped `unitRepository` query for `scoped_to_session_id is not null`) and hydrate with one call
  instead of a fan-out. Rejected as the **default** only to keep Stage 5 app-only and honour Stage 4's
  "reached by id, no list surface" model — but it is a clean, small shared add if the fan-out reads badly.
  A third option (embed inline units on the `PracticeSession` model so they travel with the session) is
  rejected: it reshapes a core shared model for a capability the app otherwise reaches by id.

`inlineUnits` is cleared alongside `units` in every sign-out / fresh-load reset (same lines that already
zero `units`, `sessions`, etc.).

### D2 — Every unit-resolution site routes through `allUnits` / `findUnit`; pickers stay on `units` _(recommendation, falls out of D1)_

| Site | Change |
| --- | --- |
| `SessionDetailScreen` | `unitsById` built from `plannerUiState.allUnits`; `derivedBallCount`, `estimateSessionDurationMinutes`, and `isSessionExecutable` then resolve inline units correctly |
| `SessionEditorScreen` | `unitsById` (selected-unit title + subtotal) built from `allUnits`; **`availableUnits` (the picker dropdown) stays `plannerUiState.units`** — inline units are never offered as a slot choice |
| `PracticePlannerViewModel.editUnit(id)` | resolve via `findUnit(id)` so an inline unit can be opened in the editor (see D4) |
| `resolveWith(units)` editor helper | an inline unit being edited must resolve over `allUnits`, or a post-save re-list (which excludes inline) blanks the editor mid-edit (see D4) |

No visual change for library-only sessions — `allUnits == units` when a session has no inline units, so
existing rendering is byte-identical.

### D3 — Promote affordance: an "Inline" marker + a subordinate "Promote to library" action on the session **detail** item row _(design-flagged, §7)_

**Recommendation: on `SessionDetailScreen`'s item row, when `unit?.isInline == true`, show a small
neutral "Inline" marker chip and a low-emphasis `TextButton` "Promote to library."** Detail is the
read/manage surface a user lands on from the session list and from history; it is where "this drill only
lives in this session — keep it?" is the natural question. One tap promotes; no dialog (promotion is safe
and one-way-additive — the unit gains a library home, the session is unchanged). The row then re-renders
without the Inline marker/promote (the unit moved from `inlineUnits` to `units`), which is the
confirmation.

- **Marker copy:** chip label `Inline`; action label `Promote to library`; success notification
  `Promoted "<title>" to your library.`
- **Weight:** the promote control is `onSurfaceVariant`, text-only, aligned under the unit row like the
  existing focus-cue / notes lines — visibly subordinate, matching the epic's "quiet, user-initiated"
  stance. Never a prompt.
- **Alternative for owner:** co-locate promote with edit on the session **editor** item card instead of
  detail (both being "manage this inline unit" actions). Rejected as default because detail is the more
  frequented, lower-friction surface and editing is a heavier mode; but single-homing promote on the
  editor is defensible if the owner wants all inline management in one place. **Do not** double-home it —
  one promote surface avoids two code paths drifting.

### D4 — Edit an inline unit: reuse the existing unit editor, navigated from the session **editor** item card _(design-flagged, §8)_

Design §8 is explicit: app editing reuses the existing unit editor, navigated from the session editing
screen, keyed off unit id. **Recommendation: on `SessionEditorScreen`'s item card, when the selected unit
`isInline`, add an "Edit this unit" affordance** that navigates `RangeworkRoutes.unitEdit(unitId)` (the
existing route + `UnitEditorScreen`, reached today for library units via `unitActions.onEdit`). The editor
is unchanged — it saves the unit by id and Stage 4's `save_practice_unit` preserves `scoped_to_session_id`
(Stage 4 D5), so the unit stays inline and out of the library.

Two ViewModel wrinkles this exposes, both handled here (no editor change):

- **`editUnit(id)` must resolve inline units.** Today it does `units.firstOrNull { it.id == id } ?: return`
  — an inline id isn't in `units`, so edit would silently no-op. Route it through `findUnit(id)` (D2).
- **The save reconcile must not blank the editor.** `saveUnit`'s optimistic path appends to `units` and
  its reconcile calls `resolveWith(listUnits())`; for an inline unit, `listUnits()` excludes it, so
  `resolveWith` would reset the editor to blank and the optimistic unit would wrongly enter `units`.
  Fix: in `saveUnit`, branch on whether the resolved unit id is inline (present in `inlineUnits` /
  `findUnit(id)?.isInline`) — apply the optimistic update to `inlineUnits` rather than `units`, and on
  reconcile re-fetch that unit via `getUnit(id)` back into `inlineUnits` and `resolveWith(allUnits)`. A
  focused branch, spelled out in ViewModel plumbing below.

**Discoverability note:** because editing is on the *editor* card and promote is on the *detail* row
(D3), the inline marker chip should appear on **both** surfaces so the user can tell an item is inline
before deciding to edit or promote. Cheap: the `isInline` flag is already on the resolved unit.

### D5 — Library exclusion is verified, not re-implemented _(resolved, per Stage 4 D4)_

`UnitListScreen` reads `plannerUiState.units` (= `listUnits()`), which Stage 4 already filters to
`scoped_to_session_id is null`. Stage 5 adds no filter; the validation checklist confirms a promoted unit
**appears** in the library and an un-promoted inline unit **does not**. This is the "library continues to
exclude inline units — verify, don't re-implement" line from the epic (§Stage 5).

## Likely files

### Modified — androidApp UI

| File | Change |
| --- | --- |
| `ui/PracticePlannerViewModel.kt` | `inlineUnits` on `PracticePlannerUiState` + `allUnits` / `findUnit` helpers (D1); hydrate `inlineUnits` in `refreshPlanning` and the save/delete/duplicate reconciles; clear it in the sign-out/fresh-load resets; `editUnit` via `findUnit` (D4); `saveUnit` inline-aware optimistic + reconcile branch (D4); new `promoteUnit(id)` optimistic operation (D3) |
| `ui/PlannerActions.kt` | `UnitEditorActions` / `SessionEditorActions` gain the slots the screens need — `onPromoteUnit: (String) -> Unit`, and an `onEditInlineUnit: (String) -> Unit` nav hook (or reuse `UnitEditorActions.onEdit` + a nav lambda) |
| `ui/RangeworkApp.kt` | Wire `onPromoteUnit` → `plannerViewModel::promoteUnit`; wire the session-editor inline "Edit this unit" to `unitActions.onEdit(id)` + `navigate(unitEdit(id))`; pass the new session-detail/editor params |
| `ui/screens/SessionDetailScreen.kt` | `unitsById` from `allUnits` (D2); inline marker chip + subordinate "Promote to library" `TextButton` on inline item rows (D3) |
| `ui/screens/SessionEditorScreen.kt` | `unitsById` from `allUnits`, picker `availableUnits` stays `units` (D2); inline marker + "Edit this unit" affordance on inline item cards (D4) |
| `ui/components/*` | Small: an "Inline" marker chip (reuse `AssistChip`/`Surface` styling already in the item row) and, if extracted, a shared inline-affordance row; prefer inlining into the two screens over a new component unless reused |

### Modified — tests

| File | Change |
| --- | --- |
| `test/.../ui/PracticePlannerViewModelTest.kt` | inline hydration, `findUnit`, edit-inline (resolve + no-blank reconcile), and promote (optimistic move + revert) cases — see Test plan |

### Referenced (not modified)

| File | Purpose |
| --- | --- |
| `ui/screens/UnitEditorScreen.kt` | The existing editor reused for inline edit — **unchanged** (design §8) |
| `ui/screens/UnitListScreen.kt` | Library list — **unchanged**; verifies exclusion (D5) |
| `ui/RangeworkNavigation.kt` | `UnitEdit` route + `unitEdit(id)` already exist — reused, not added |
| `shared/.../library/PracticeLibrary.kt` | `getUnit(id)` (unfiltered) and `promoteUnit(id)` — Stage 4 APIs, called not changed |

## ViewModel plumbing (`PracticePlannerViewModel`)

**State:** add `val inlineUnits: List<PracticeUnit> = emptyList()` to `PracticePlannerUiState`, plus the
`allUnits` / `findUnit` helpers (D1). Add `inlineUnits = emptyList()` to every reset block that already
zeroes `units` (the `SignedIn` fresh-load and the `SignedOut`/`Error` blocks).

**Hydration helper** (private), called wherever `units`/`sessions` are (re)listed —
`refreshPlanning`, and the reconcile tails of `saveSession`, `deleteSession`, `duplicateSession`,
`saveUnit`, `deleteUnit`:

```kotlin
private suspend fun hydrateInlineUnits(
    library: PracticeLibrary,
    units: List<PracticeUnit>,
    sessions: List<PracticeSession>,
): List<PracticeUnit> {
    val known = units.mapTo(HashSet()) { it.id }
    val referenced = sessions.flatMap { it.items }.map { it.practiceUnitId }.toSet()
    return (referenced - known).mapNotNull { id -> library.getUnit(id) }
        .filter { it.isInline }
}
```

Each reconcile that today sets `units = …, sessions = …` also sets
`inlineUnits = hydrateInlineUnits(library, units, sessions)` and threads `allUnits` into the two
`resolveWith(...)` calls (so an in-flight inline edit isn't blanked). Failure of a `getUnit` is non-fatal
(the id falls back to "Missing unit"); wrap defensively like the other non-fatal loaders.

**`editUnit(id)`** — replace `_uiState.value.units.firstOrNull { it.id == id }` with
`_uiState.value.findUnit(id)`, so an inline unit opens in the editor (D4).

**`saveUnit()` inline branch (D4):** determine `isInlineEdit = editor.unitId != null &&
_uiState.value.findUnit(editor.unitId!!)?.isInline == true`.
- Optimistic: if `isInlineEdit`, replace the matching unit inside `inlineUnits` (not `units`) and leave
  `units` untouched; otherwise the existing `units` path is unchanged.
- Reconcile: after `library.saveUnit(...)`, re-list `units`/`sessions` as today **and**
  `inlineUnits = hydrateInlineUnits(...)` (which re-fetches the just-saved inline unit by id); resolve the
  editor with `resolveWith(allUnits)`. This keeps the edited inline unit present and out of the library.

**`promoteUnit(id)` (new, D3)** — mirror the optimistic `duplicate`/`delete`/`archive` shape:

1. Snapshot `previousUnits` / `previousInline`. Find the unit via `findUnit(id)`; bail with a
   notification if absent or already a library unit (`!isInline`).
2. Optimistically **move** it: `inlineUnits - it`, `units + it.copy(scopedToSessionId = null)`; set
   `PlannerStatus.Notification("Promoted \"$title\" to your library.")`.
3. Under `operationMutex` + `++operationToken`: `library.promoteUnit(id)`, then re-list `units`/`sessions`
   and re-hydrate `inlineUnits`; reconcile on the winning token. (`listUnits()` now includes the promoted
   unit; `hydrateInlineUnits` no longer returns it — the move settles for free.)
4. On failure, revert both `units` and `inlineUnits` and surface `plannerStatus(exception, "Promote
   failed.")`.

`duplicateSession` is unchanged — Stage 4's deep-copy RPC mints new inline units for the copy; the
reconcile's `hydrateInlineUnits` picks them up because the copy's items reference the new inline ids.

## Screen changes

**`SessionDetailScreen`** — build `unitsById` from `plannerUiState.allUnits`. In `SessionItemDetailRow`,
when `unit?.isInline == true`: render a neutral **`Inline`** marker chip alongside the existing repeat /
override chips, and a subordinate **`Promote to library`** `TextButton` (aligned under the row like the
notes line) calling `onPromoteUnit(unit.id)`. Everything else — ball pill, focus cue, observations —
renders identically because the inline unit now resolves.

**`SessionEditorScreen`** — build `unitsById` from `allUnits`; keep `availableUnits = plannerUiState.units`
so the picker never lists inline units. In `SessionItemEditorCard`, when `selectedUnit?.isInline == true`:
show the **`Inline`** marker and an **`Edit this unit`** affordance calling the nav hook
(`onEditInlineUnit(selectedUnit.id)` → `unitActions.onEdit(id)` + `navigate(unitEdit(id))`). The unit
picker for an inline item still shows its current title (resolved via `allUnits`); changing the selection
away from the inline unit is allowed and, on save, Stage 4's orphan-GC reaps the now-unreferenced inline
unit — acceptable and by design (§6).

## Test plan (`PracticePlannerViewModelTest`, in-memory foundation)

`runTest` + in-memory `DataFoundation`, as the file already builds. The in-memory unit repo must honour
Stage 4's semantics (inline units excluded from `list`, returned by `get`, `promoteUnit` = detach) —
Stage 4's tests establish this; Stage 5 builds on it.

- `inlineUnitsHydratedForSessionItems` — seed a session whose item references a scoped (inline) unit;
  `refreshPlanning`; assert the unit is absent from `uiState.units` but present in `inlineUnits`, and
  `findUnit(id)` returns it.
- `findUnitResolvesInlineAndLibrary` — `findUnit` returns both a library unit and an inline unit; returns
  null for an unknown id.
- `editInlineUnitOpensEditor` — `editUnit(inlineId)` populates `unitEditor` from the inline unit (locks
  the D4 `findUnit` routing; today's `units`-only lookup would no-op).
- `saveInlineUnitEditKeepsItInlineNotLibrary` — edit an inline unit's title and `saveUnit`; after the
  reconcile it is still in `inlineUnits` (refreshed), still absent from `units`, and the editor is not
  blanked (D4 reconcile branch).
- `promoteUnitMovesInlineToLibrary` — `promoteUnit(inlineId)`; the unit leaves `inlineUnits`, enters
  `units` (`scopedToSessionId == null`), and the owning session still references the same unit id.
- `promoteUnitOptimisticThenReconciles` — the move is visible synchronously before the coroutine settles
  (advance dispatcher for the reconcile), matching the duplicate/archive optimistic-test style.
- `promoteFailureRevertsBothLists` — with the library stubbed to throw on `promoteUnit`, `units` and
  `inlineUnits` return to their pre-promote contents and a notification is set.
- `duplicateSessionWithInlineUnitHydratesCopy` — duplicate a session containing an inline unit; after the
  reconcile the copy's new inline unit id is present in `inlineUnits` (the deep-copy from Stage 4 surfaces
  in the app).

UI-only behaviour (the inline marker chip, the promote `TextButton` placement, the editor "Edit this
unit" nav) is covered by the manual device walkthrough — it lives in the composables, not the ViewModel.

## Validation checklist

- [ ] `:androidApp:testDebugUnitTest` / `:androidApp:testReleaseUnitTest` green incl. the new
      `PracticePlannerViewModelTest` cases; `:shared` tests untouched and green.
- [ ] `:androidApp:assembleDebug` + `:androidApp:lintDebug` clean.
- [ ] **Device walkthrough (ship-point-2 app half):** create (via MCP, Stage 6 — or seed) a session with
      one library item and one inline unit → open the session detail → the inline item renders with its
      real title, ball count, and instructions echo (not "Missing unit"), marked **Inline** → Start gate
      reflects the inline unit's instructions.
- [ ] **Edit inline:** open the session editor → the inline item shows **Inline** + **Edit this unit** →
      tap it → the existing unit editor opens on that unit → change the title → save → return; the session
      shows the new title and the unit is **still not** in the library.
- [ ] **Promote:** on session detail, tap **Promote to library** on the inline item → notification →
      the Inline marker disappears → the unit now appears in the **unit library** list → the session still
      references it (title unchanged) → it is no longer promotable from the session.
- [ ] **Library exclusion (D5):** before promotion, the inline unit is **absent** from the unit library;
      a plain library unit and a promoted unit both appear. Session-list ball counts and Overview are
      correct for the inline-unit session (eager hydration).
- [ ] **Duplicate:** duplicate the inline-unit session → the copy shows its **own** inline unit (edit the
      copy's inline unit; the original's is untouched) — Stage 4 deep-copy surfaced in the app.
- [ ] **Delete:** delete the session → its (un-promoted) inline unit is gone; a promoted-then-kept unit
      survives in the library (Stage 4 cascade / promotion escape hatch, observed through the app).
- [ ] Library-only sessions render and edit **identically** to pre-Stage-5 (`allUnits == units`); no
      visual or behavioural change where no inline unit is present.

## Regression risks

| Risk | Likelihood | Mitigation |
| --- | --- | --- |
| Inline-unit session renders "Missing unit" / null ball count (resolution still on `units`) | **Medium** | D1/D2 route every resolution site through `allUnits`/`findUnit`; `inlineUnitsHydratedForSessionItems` test; walkthrough opens an inline session |
| Editing an inline unit no-ops or blanks the editor on save (list-based lookup + `resolveWith(units)`) | **Medium** | D4 `editUnit` via `findUnit`; `saveUnit` inline branch updates `inlineUnits` and re-hydrates; `editInlineUnit*` + `saveInlineUnitEditKeepsItInline*` tests |
| Aggregate ball counts (list/Overview) wrong for inline sessions | Low | Eager hydration at refresh (D1) keeps `allUnits` complete for all surfaces, not just opened detail |
| Inline unit offered as a slot choice in the session-editor picker (breaks single-owner invariant) | Low | Picker `availableUnits` stays `units` (D2); only the current selection resolves via `allUnits` |
| Promote optimistic move desyncs `units`/`inlineUnits` on failure | Low | Snapshot-and-revert both lists; `operationToken` guard; `promoteFailureRevertsBothLists` test |
| Promote double-homed (detail + editor) drifts into two behaviours | Low | D3 single-homes promote on detail; edit single-homes on editor; marker chip shared, actions not |
| Inline unit leaks into the unit library | Low | D5 relies on Stage 4's `listUnits()` filter unchanged; checklist confirms exclusion before and library presence after promotion |
| `getUnit` fan-out at refresh slows load | Very Low | Single-user, few inline ids; non-fatal per-id; D1 alternative (`listInlineUnits()` one-call) is the upgrade if it ever bites |

## On merge

Stage 5 + Stage 6 together are **ship point 2**. After both merge: run the epic's end-to-end walkthrough
(plan via MCP with an inline unit → run → conversationally promote the drill into a new session — the app
and MCP halves meeting), then proceed to the epic close checklist (`design-decisions.md` status line,
`apps/mobile/CONTEXT.md` vocabulary check, follow-up issues for §10 deferrals, project-memory update).
