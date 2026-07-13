# Stage 2: Archiving app UI  ⛳ ship point 1

**Epic:** [`../../epic-implementation-plan.md`](../../epic-implementation-plan.md)
**Design:** [`../../design-decisions.md`](../../design-decisions.md) (§3–§5)
**Vocabulary:** [`apps/mobile/CONTEXT.md`](../../../../apps/mobile/CONTEXT.md) — **Archived**
**Depends on:** Stage 1 (archiving foundation) merged — `archiveSession` / `unarchiveSession` /
`listArchivedSessions` on `PracticeLibrary`, and the repository listing split.
**Status:** proposed — awaiting owner sign-off on D2 (archived entry point) and D3 (finish-screen
affordance) below; D1 and D4 are recommendations that can be confirmed at the same time. Ready for
implementation once Stage 1 is merged and these are confirmed.

## Objective

Give archiving a face. Stage 1 made the data layer and shared core fully archive-aware but nothing
can set the state; Stage 2 wires the four surfaces the design calls for and closes the loop:

1. **Archive / unarchive** actions on the session **detail** top-bar overflow (and, per D1, the
   session-list row overflow).
2. A separate **Archived destination** screen reached by a quiet entry point off the session list;
   its rows offer view, duplicate, unarchive, delete — **no start, no edit**.
3. A deliberately **secondary "Archive this session" affordance on the Range Session finish
   screen**, archiving the *source template* and self-hiding when that template is already archived
   (or gone).
4. **State-aware session detail**: an archived session renders its state plainly and gates
   edit/start behind unarchive.

This is **ship point 1** — after merge a user can plan via MCP (or by hand), run, archive from the
finish screen, find the session under Archived, duplicate/unarchive/re-run it, and the default list
stays clean with history intact.

**No MCP (Stage 3) and no Inline Unit work here.** No `shared` or `supabase` changes are expected —
Stage 1 owns the whole data/KMP surface; this stage is `androidApp` UI + ViewModel plumbing only.

## Dependencies

- **Stage 1 merged** — the plan below calls `library.archiveSession(id)`,
  `library.unarchiveSession(id)`, `library.listArchivedSessions()`, and relies on
  `PracticeLibrary.listSessions()` already excluding archived rows and on `PracticeSession.archivedAt`
  / `isArchived`. (Per the epic pipelining rule this plan drafts now; implementation waits for the
  Stage 1 merge.)
- Existing objects read or modified: `PracticePlannerViewModel` / `PracticePlannerUiState`,
  `RangeworkApp` (shell top-bar `actionsContent`, the `Sessions` / `SessionDetail` composables, the
  `RangeSession` composable), `SessionListScreen`, `SessionDetailScreen`, `FinishSummaryContent`,
  `RangeSessionScreen`, `OverflowMenu`, `RangeworkNavigation` (new route), `SessionEditorActions`.
- No new shared or DB objects. `RangeSession.sourceSessionId` (existing) is the finish-screen hook.

## Decisions (owner review)

### D1 — Archive/unarchive action surfaces: detail overflow + list-row overflow _(recommendation)_

**Recommendation: add Archive to the two existing overflow menus, not a new bespoke control.**

- **Session detail** (shell `MediumTopAppBar` `actionsContent`, `RangeworkApp` ~line 848): the
  session overflow already carries Duplicate + Delete. Add **Archive** for an unarchived session and
  **Unarchive** for an archived one; **hide the Edit `IconButton`** (the pencil) when the session is
  archived (design §3 — no edit until unarchived). Duplicate and Delete stay for both states.
- **Session list row** (`SessionListScreen` → `ListEntryCard` → `OverflowMenu`): add an **Archive**
  item to the row overflow for the (always-unarchived) default list. Cheap tidy-from-the-list path,
  and it matches how delete/duplicate already live on the row.

`OverflowMenu` gains an optional `onArchive: (() -> Unit)? = null` **and** `onUnarchive: (() -> Unit)?
= null` slot (rendered above Delete, below Duplicate; archive uses a neutral archive icon, not the
error-tinted styling). Both default null so every existing call site is unaffected.

**Why overflow, not a primary button:** archiving is a tidy-up, not a headline action — it belongs in
the same quiet menu as duplicate/delete, never competing with Start. Swipe gestures are left alone
(swipe-left is already delete; overloading it would be error-prone).

### D2 — Archived destination entry point: a self-hiding footer row on the session list _(epic-flagged)_

**Recommendation: a quiet "Archived (N)" footer row at the bottom of the session list, shown only
when N > 0.** Tapping it navigates to a new `ArchivedSessionsScreen`.

- The Sessions tab has **no per-screen app bar** (the shell provides only the bottom bar / rail), so
  a top-bar overflow item has no natural home here — a footer row is the idiomatic quiet entry. It
  sits after the last session card (before the existing bottom `Spacer(96.dp)`), styled as a
  low-emphasis text/`TextButton` row with a small archive icon and the count, e.g. **`⌸ Archived · 3`**.
- **Self-hiding when empty** keeps the feature invisible until it's earned its place — no dead entry
  point for users who never archive. Count comes from `plannerUiState.archivedSessions.size`, loaded
  on entry to the Sessions tab (see ViewModel plumbing).

**Copy for review:** row label `Archived` with a `· N` count suffix; screen title **"Archived
sessions"**; empty-screen fallback (only reachable transiently if the last item is unarchived while
open) "No archived sessions." Alternative offered for owner: an overflow item is not viable without
adding an app bar to the tab, so footer row is the recommendation unless the owner wants an app bar.

### D3 — Finish-screen affordance: a low-emphasis text button below Done, self-hiding _(epic-flagged)_

**Recommendation: a single `TextButton` "Archive this session" placed directly below the primary
Done button in `FinishSummaryContent`, shown only when the source template is present and
unarchived.** Never a dialog, never a prompt, no confirmation step — one tap archives; the button
then vanishes (the template leaves the unarchived list), which is the confirmation.

Visibility is derived **entirely in `RangeworkApp`** at the `RangeSession` composable, with no change
to `RangeSessionViewModel`:

```kotlin
val sourceId = rangeSessionUiState.rangeSession?.sourceSessionId
val archivable = sourceId != null &&
    plannerUiState.sessions.any { it.id == sourceId }   // unarchived list only → self-hides
val onArchiveTemplate: (() -> Unit)? =
    if (archivable) { { plannerViewModel.archiveSession(sourceId!!) } } else null
```

`FinishSummaryContent` gains `onArchiveSession: (() -> Unit)? = null`; it renders the `TextButton`
only when non-null. Because `plannerUiState.sessions` excludes archived rows, a template that is
**already archived, or deleted (`sourceSessionId` set-null), or a run with no source** yields `null`
and the affordance simply never appears — that is the design's "self-hides when already archived"
with no extra state to track.

**Mock** (phone finish screen, below the stat card):

```
┌─────────────────────────────────────┐
│           Session Complete          │
│         Wedge ladder — Tue          │
│  ┌───────────────────────────────┐  │
│  │ Balls hit          58 of 60   │  │
│  │ Steps completed        11/12  │  │
│  │ Completion              97%   │  │
│  │ Time                   24:31  │  │
│  └───────────────────────────────┘  │
│                                     │
│  [        Done  (filled)        ]   │   ← primary, unchanged
│         Archive this session        │   ← TextButton, onSurfaceVariant, no box
└─────────────────────────────────────┘
```

The label is text-only, `onSurfaceVariant`, no container — visibly subordinate to Done, matching the
passive Block-Result-note stance (§4). Owner tuning knobs after field use: label wording, and
whether a one-line "Archived — tidy" confirmation briefly replaces the row vs. it just disappearing.
Recommendation: **just disappear** (quietest); revisit only if it feels like nothing happened.

### D4 — Archived session detail resolves state-aware, from a combined lookup _(recommendation)_

Stage 1's `listSessions()` now excludes archived, so **both** `SessionDetailScreen`
(`plannerUiState.sessions.firstOrNull { it.id == sessionId }`) and the shell top-bar's
`currentSessionId` lookup (`RangeworkApp` ~line 849) would resolve an archived session to `null` and
render "Session not found". Fix once, at the source: add

```kotlin
val PracticePlannerUiState.allSessions: List<PracticeSession> get() = sessions + archivedSessions
fun PracticePlannerUiState.findSession(id: String): PracticeSession? =
    allSessions.firstOrNull { it.id == id }
```

and route both lookups through `findSession`. Then `SessionDetailScreen` branches on
`session.isArchived`:

- **Archived:** replace the primary **Start** button with a quiet inline notice — an
  `EntryHighlightCard` or banner "This session is archived. Unarchive to start or edit it." with an
  **Unarchive** button; the shell top-bar hides Edit and shows Unarchive in the overflow (D1). Items
  and history render exactly as today (fully viewable, design §3).
- **Unarchived:** unchanged, plus Archive in the top-bar overflow (D1).

Archiving/unarchiving **from detail stays on the detail screen** — the combined lookup keeps it
resolvable, and the in-place state flip (Start↔Unarchive notice, Edit shown↔hidden) is the clearest
possible demonstration that nothing was lost. No navigation surprise.

## Likely files

### New

| File | Purpose |
| --- | --- |
| `.../ui/screens/ArchivedSessionsScreen.kt` | The Archived destination: list of archived sessions; rows offer view / duplicate / unarchive / delete |

### Modified — androidApp UI

| File | Change |
| --- | --- |
| `ui/PracticePlannerViewModel.kt` | `archivedSessions` in `PracticePlannerUiState`; `allSessions` / `findSession` helpers; `archiveSession(id)`, `unarchiveSession(id)`, `loadArchivedSessions()`; refresh paths repopulate `archivedSessions` |
| `ui/PlannerActions.kt` | `SessionEditorActions` gains `onArchive`, `onUnarchive`, `onLoadArchived` |
| `ui/RangeworkApp.kt` | Wire the new actions; shell top-bar Archive/Unarchive + Edit-gating via `findSession`; footer-row nav to Archived; `Sessions`/`SessionDetail`/`ArchivedSessions` composables; finish-screen `onArchiveSession` derivation |
| `ui/RangeworkNavigation.kt` | New `SessionsArchived = "sessions/archived"` route |
| `ui/screens/SessionListScreen.kt` | Row-overflow Archive (D1); "Archived (N)" footer row (D2) |
| `ui/screens/SessionDetailScreen.kt` | State-aware: `findSession` lookup, archived banner + Unarchive, Start gated when archived (D4) |
| `ui/components/OverflowMenu.kt` | Optional `onArchive` / `onUnarchive` slots |
| `ui/components/FinishSummaryContent.kt` | Optional `onArchiveSession` → subordinate `TextButton` (D3) |
| `ui/screens/RangeSessionScreen.kt` | Thread `onArchiveSession` to `FinishSummaryContent` |

### Modified — tests

| File | Change |
| --- | --- |
| `test/.../ui/PracticePlannerViewModelTest.kt` | Archive/unarchive/list-archived/optimistic-revert/`findSession` cases (see Test plan) |

### Referenced (not modified)

| File | Purpose |
| --- | --- |
| `ui/components/ListEntryCard.kt` | Row card reused (unarchived list + archived screen); passes overflow slots through |
| `ui/RangeSessionViewModel.kt` | `RangeSession.sourceSessionId` read for the finish affordance — **not modified** |

## ViewModel plumbing (`PracticePlannerViewModel`)

State: add `val archivedSessions: List<PracticeSession> = emptyList()` to `PracticePlannerUiState`,
plus the `allSessions` / `findSession` helpers (D4).

`loadArchivedSessions()` mirrors the existing `loadActiveRangeSessions()` shape (fire-and-forget,
non-fatal): `archivedSessions = library.listArchivedSessions()`. Called on entry to the Sessions tab
and the Archived screen so the footer count and the archived list are fresh.

`archiveSession(id)` / `unarchiveSession(id)` follow the optimistic pattern already used by
`duplicateSession` / `deleteSession`:

1. Snapshot `previousSessions` / `previousArchived`.
2. Optimistically **move** the session between the two lists (archive: `sessions - it` →
   `archivedSessions + it.copy(archivedAt = now)`; unarchive: the reverse with `archivedAt = null`),
   set a `PlannerStatus.Notification("Archived \"$name\".")` / `Unarchived …`.
3. `library.archiveSession(id)` / `unarchiveSession(id)`, then re-list **both** `listSessions()` and
   `listArchivedSessions()` under the `operationMutex` + `operationToken` guard already used
   everywhere in this VM; reconcile on the winning token.
4. On failure, revert both lists and surface `plannerStatus(exception, "Archive failed.")`.

The default-list refresh paths (`refreshPlanning`, and the post-save/-delete/-duplicate reconciles)
also repopulate `archivedSessions` so the footer count never drifts. `duplicateSession` is unchanged
— a duplicate of an archived session is already unarchived (Stage 1) and lands in `sessions`.

Guard/gating note: the **start** and **edit** entry points are gated in the UI here (Start hidden on
archived detail; Edit icon hidden). Stage 1's `saveSession` edit-guard and the `start_range_session`
DB guard are the belt-and-braces behind this UI.

## Archived screen (`ArchivedSessionsScreen`)

Same `RefreshableScrollableScreen` + `ListEntryCard` shell as `SessionListScreen`, reading
`plannerUiState.archivedSessions`. Per-row actions via the card/overflow: **view** (`onClick` →
`SessionDetail` route — resolves via `findSession`), **Unarchive**, **Duplicate**, **Delete**. **No
Edit, no Start** — the overflow simply omits `onEdit` and there is no Start affordance on a row.
Empty state: "No archived sessions." Delete reuses `DeleteConfirmationDialog`; consider copy noting
history stays grouped (deferred wording — the epic's cascade-copy concern is a Stage 5 Inline-Unit
matter, not archiving).

## Test plan (`PracticePlannerViewModelTest`, in-memory foundation)

Extend the existing session tests (`runTest`, in-memory `DataFoundation` as the file already builds).

- `archiveSessionMovesSessionToArchivedList` — save → `archiveSession`; assert absent from
  `uiState.sessions`, present in `archivedSessions`, `isArchived == true`.
- `unarchiveSessionRestoresToDefaultList` — archive then unarchive; back in `sessions`, gone from
  `archivedSessions`.
- `loadArchivedSessionsPopulatesState` — with a pre-archived row, `loadArchivedSessions()` fills
  `archivedSessions`.
- `archiveSessionOptimisticallyUpdatesBeforeReconcile` — assert the move is visible synchronously
  before the coroutine settles (advance dispatcher for the reconcile), matching the duplicate-test
  style.
- `archiveFailureRevertsBothLists` — with a repository stubbed to throw, both lists return to their
  pre-archive contents and a notification is set.
- `findSessionResolvesArchivedSession` — `findSession(archivedId)` returns the archived session even
  though it is absent from `sessions` (locks the D4 lookup that keeps detail working).

UI-only behaviour (finish-affordance visibility derivation, footer-row self-hide, detail state
branch) is covered by the manual device walkthrough below — it lives in `RangeworkApp`/composables,
not the ViewModel.

## Validation checklist

- [ ] `:androidApp:testDebugUnitTest` / `:androidApp:testReleaseUnitTest` green incl. the new
      `PracticePlannerViewModelTest` cases; `:shared` tests untouched and green.
- [ ] `:androidApp:assembleDebug` + `:androidApp:lintDebug` clean.
- [ ] **Device walkthrough (ship-point-1 loop):** plan a session (MCP or by hand) → start → finish →
      tap "Archive this session" on the finish screen → button disappears → session is gone from the
      default list → appears under the "Archived (1)" footer → open it (detail shows archived state,
      Start replaced by the unarchive notice, Edit hidden) → Duplicate (copy lands unarchived in the
      main list) → Unarchive (returns to the main list, Start/Edit back) → re-run it.
- [ ] Finish-screen affordance **self-hides** when the source template was archived before the run,
      when the run has no source template, and immediately after tapping it.
- [ ] Range Session **history still groups** under the archived template on its detail screen.
- [ ] Footer row hidden when there are zero archived sessions; count accurate after archive/unarchive.
- [ ] Archived-screen rows expose only view/duplicate/unarchive/delete — **no start, no edit**.
- [ ] Archiving with an Active Range Session in flight succeeds and does not disturb the running
      session (design §3 — belt-and-braces confirmation; the run is Snapshot-immune).

## Regression risks

| Risk | Likelihood | Mitigation |
| --- | --- | --- |
| Archived session opens as "Session not found" (detail + shell top-bar resolve from the now-filtered `sessions`) | Medium | D4's single `findSession` choke point routes both lookups; `findSessionResolvesArchivedSession` test; walkthrough opens an archived detail |
| Finish affordance shows for an already-archived / deleted template | Low | Visibility derived solely from `plannerUiState.sessions` (unarchived); no independent flag to drift; explicit self-hide checklist items |
| Optimistic archive/unarchive desyncs the two lists on failure | Low | Snapshot-and-revert both lists; `operationToken` guard as elsewhere in the VM; `archiveFailureRevertsBothLists` test |
| Footer count stale after archive from another surface | Low | Every default-list reflist path repopulates `archivedSessions`; `loadArchivedSessions()` on tab entry |
| Edit reachable on an archived session (top-bar pencil, list swipe-to-edit) | Low | Edit `IconButton` hidden when archived; archived screen row omits `onEdit`; Stage 1 `saveSession` guard is the backstop. (Swipe-to-edit exists only on the default list, which never contains archived rows.) |
| `OverflowMenu` new slots break existing call sites | Very Low | Both new params default null; unit/session/archived callers opt in explicitly |
```
