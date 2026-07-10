# Stage 4: Prose Capture  ⛳ ship point 1

**Epic:** [`../../epic-implementation-plan.md`](../../epic-implementation-plan.md)
**Design:** [`../../design-decisions.md`](../../design-decisions.md)
**Stage 2 record:** [`../stage-02-models/changes.md`](../stage-02-models/changes.md) — **implemented 2026-07-10 on this branch.** The recorder surface, models, and rules consumed below are defined there; names below are reconciled against the shipped code.
**Status:** draft — no epic-flagged owner decisions for this stage; two plan-level UI calls recorded below (P1, P2) for a quick read

## Objective

Make the feature field-usable. Three capture surfaces, all prose-first, all Android-UI-only
(`androidApp/` — no `shared/`, SQL, or MCP changes):

1. **Block Result affordance** on the block screen — passive, never prompted; note always;
   manual "X of Y balls" success count only when the snapshot unit has a Success Criterion
   and the Success Observation Type is not enabled.
2. **Session Note capture at finish** — on the existing finish summary, never mandatory.
3. **Post-completion editing from history** — completed sessions become tappable in history
   and open a minimal detail screen where both note kinds stay editable and counts render
   frozen.

After this stage merges: **field test #1** (epic validation gate) — its impressions feed the
Stage 5 capture-UI design review.

## Dependencies

- **Stage 2 present on this branch.** Everything domain-shaped is consumed, not built:
  `RangeSessionRecorder` (`saveSessionNote`, `saveBlockNote`, `saveManualCount` — all
  `(rangeSessionId, …)` / block methods keyed by `unitIndex: Int`) via
  `DataFoundation.rangeSessionRecorder`; `BlockResult`, `RangeSession.sessionNote`/`blockResults`,
  `supportsDataCapture`; `RangeSessionRecordingRules` freeze/validation predicates;
  `BlockSuccessCount` provenance selection; `SnapshotUnit.successCriterion` +
  `enabledObservationTypes`.
  **Names verified against shipped code — no reconciliation outstanding.** Two shape facts to
  build against: recorder methods return `RecordingResult<T>` (a sealed `Success`/`Rejected`, not
  exceptions — a rejection is data that lands in the snackbar per "UI hides, shared rejects"); and
  `RecordingRejection` lives in the `model` package, not `recording` (import site only).
- **Stage 3 is not a code dependency.** No file overlap (Stage 3 touches editors/settings;
  this stage touches execution/history). But the count affordance is only *reachable* once
  some unit has a criterion — authored via Stage 3's editor field or Stage 7's `create_unit`.
  Implementation can proceed in either order; the epic sequences 3 → 4 and nothing here
  fights that.
- Existing UI patterns reused: `CollapsibleNotes` (expandable card, `ExecutionBlockPage.kt`),
  `CountStepper`, `RangeSessionHistoryItem`, `FinishSummaryContent`, ViewModel factory +
  optimistic-write-with-revert conventions from `RangeSessionViewModel`.

## Plan-level calls (P1, P2) — recorded, not gates

### P1 — Post-completion editing surface: a minimal completed-session detail screen

The epic puts "editable afterwards from history" and "post-completion editability for both
notes" in this stage, but the history detail *screen* is Stage 6 (ship point 2). These meet
in the middle exactly where the epic's parallelism table already pointed: *"Stage 6, if
split: notes/results view after Stage 4."*

**Chosen:** build the notes/results half of the history detail screen now, as this stage's
editing surface. New route `range-session-history/{rangeSessionId}` → `RangeSessionHistoryScreen`:
finish-style stats header, editable Session Note, one card per block (unit title, editable
note, frozen manual count where one exists). Stage 6 then *extends this screen* with
observation summaries and provenance/denominator labeling instead of building from scratch.

**Rejected:** a bottom sheet hanging off the history row. Editing one session note fits a
sheet; N per-block note editors do not, and the sheet would be thrown away the moment
Stage 6 builds the real screen.

**Consequence for Stage 6:** its remaining scope is observation summaries + provenance
labeling ("X of Y observed") + any polish — note this in Stage 6's plan when drafted.

### P2 — Save semantics: explicit save for notes, immediate save for the count

- **Notes (block + session): explicit Save button, enabled when dirty.** The repository
  write is a read-merge-write PATCH (Stage 2); save-on-keystroke would spam it and
  save-on-focus-loss is invisible and unverifiable at the range with a glove on. A visible
  Save that flips to a saved state is predictable, and failure has an obvious retry.
- **Manual count: writes on stepper commit** (each tap, optimistic with revert on failure —
  same pattern as step completion). A count is one integer; deferring it behind the note's
  Save button would couple two independent fields.
- **Finish-summary special case:** the Done button also flushes a dirty unsaved note (one
  write, then navigate; on failure stay put + snackbar). Nothing typed at the finish moment
  gets silently lost, without adding a confirm dialog.

## Fixed in this plan

- **Gate everything on `supportsDataCapture`** (snapshot v3, Stage 2 predicate). v1/v2
  sessions render exactly as today: no result section, no finish note field, and their
  history rows stay non-tappable (P1 screen would have nothing editable to show; Stage 6
  may revisit when there are stats worth opening).
- **Passive means invisible-until-wanted.** The Block Result section is a collapsed
  expandable card (same pattern as the existing unit-notes card). No badges, no prompts, no
  finish-time nag about blocks without results. Deferred-scope tripwire from the epic applies:
  no judgement colors, no progress framing.
- **Manual-count-only rendering.** The count row renders only when provenance is
  `Manual`-eligible per shared rules (criterion present, Success type not enabled, block has
  Ball Steps). Derived counts (`success` enabled) render **nothing** in this stage — the
  tally surface is Stage 5's, and "0 of 0 observed" before capture UI exists is noise. The
  block note is unaffected.
- **Unset ≠ 0** (the Zero-vs-Uncounted philosophy): the count row idles as "Not counted"
  with an *Add count* affordance; only then does the stepper appear (starting at 0), with a
  *Remove count* action writing `null`. A saved 0 renders as "0 of Y" — a real datum.
- **Show the rubric where the number is entered:** the count row includes the snapshot
  unit's `successCriterion` text verbatim (it is the success rule; without it "X of Y" is
  the bare number the design disallows).
- **UI hides, shared rejects.** Visibility predicates mirror `RangeSessionRecordingRules`,
  and recorder rejections (should they ever surface) land in the existing snackbar channel.
  No freeze/validation logic re-implemented in composables.
- **No timer/time-entry coupling on the history screen.** `RangeSessionViewModel` records
  time entries on screen enter — reusing it for completed sessions would credit practice
  time to finished sessions. The history screen gets its own small ViewModel.

## Likely files

### New — `apps/mobile/androidApp/src/main/java/com/loganmartlew/rangework/android/`

| File | Purpose |
|---|---|
| `ui/components/BlockResultSection.kt` | Collapsed-by-default card on the block page: "Block result" header; expanded = note field + Save, and (when Manual-eligible) the count row (criterion text, Not counted / Add count / stepper / Remove count). Props: `blockResult`, eligibility flags, dirty-save callbacks. `rememberSaveable` for draft text + expansion |
| `ui/components/SessionNoteCard.kt` | Reusable note editor card (multiline field + Save-when-dirty + saving/saved state); used on finish summary and history screen |
| `ui/CompletedRangeSessionViewModel.kt` | Loads a completed session by id (repo `getSession`), exposes note/block-note edits through the recorder, optimistic update + revert + notification; factory like `RangeSessionViewModel.factory` |
| `ui/screens/RangeSessionHistoryScreen.kt` | P1 screen: stats header (reuse `FinishSummaryContent`'s stat rows or extract), `SessionNoteCard`, per-block cards (unit title, block note via the same editor, frozen count display "X of Y balls" when a manual count exists) |

### New — tests `apps/mobile/androidApp/src/test/java/com/loganmartlew/rangework/android/`

| File | Purpose |
|---|---|
| `CompletedRangeSessionViewModelTest.kt` | Load; note + block-note save happy path; failure reverts + notifies; count edit attempts rejected/absent (frozen) |

### Modified

| File | Change |
|---|---|
| `ui/RangeSessionViewModel.kt` | + `rangeSessionRecorder` constructor param (nullable, like the repo) + factory; `saveBlockNote(blockIndex, note)`, `saveManualCount(blockIndex, count: Int?)`, `saveSessionNote(note)` — blockIndex → `ExecutionBlock.unitIndex`; optimistic `rangeSession` update, revert + snackbar on failure; small UiState additions (e.g. `isSavingSessionNote`) |
| `ui/components/ExecutionBlockPage.kt` | Render `BlockResultSection` between the instruction list and the unit-notes card, gated on `supportsDataCapture`; thread `blockResults` + callbacks through |
| `ui/screens/RangeSessionScreen.kt` | Thread the new callbacks/props into `ExecutionBlockPage` (all layouts); pass session-note state into the finish-summary branch |
| `ui/components/FinishSummaryContent.kt` | + `SessionNoteCard` (v3 only) between the stats card and Done; Done flushes dirty note (P2) |
| `ui/components/RangeSessionHistoryItem.kt` | + optional `onClick`; chevron affordance when clickable; semantics note |
| `ui/screens/SessionDetailScreen.kt` | History rows clickable for v3 sessions → `onOpenRangeSessionHistory(id)` (gated on the new `supportsDataCapture` on `CompletedRangeSessionSummary` — see the confirmed shared touch) |
| `ui/RangeworkNavigation.kt` | + `RangeSessionHistory = "range-session-history/{rangeSessionId}"` + builder (distinct from the execution route `range-sessions/{id}`) |
| `ui/RangeworkApp.kt` | Wire recorder into `RangeSessionViewModel.factory`; new composable route + `CompletedRangeSessionViewModel`; thread `onOpenRangeSessionHistory` to `SessionDetailScreen` |
| `test/.../RangeSessionViewModelTest.kt` | Fake recorder; new save methods: happy path, failure revert, v2 rejection, null-recorder no-op |

### Confirmed small `shared/` touch (Stage 2 did not add it)

`CompletedRangeSessionSummary` needs a `supportsDataCapture: Boolean` (or `snapshotVersion: Int`)
so history rows know whether to be tappable — **verified: the shipped Stage 2 summary model still
carries neither.** This stage adds the field + mapping in
`SupabaseRangeSessionRepository.listCompletedSessions`. Simpler than the original flag assumed:
that query already `decodeList<RangeSession>()`s the **full** session row, and Stage 2 added
`RangeSession.supportsDataCapture`, so the mapping is literally
`supportsDataCapture = session.supportsDataCapture` — no extra `select`, no schema change. Add a
default value on the new field so fixtures/fakes keep compiling, and note it in `changes.md`. This
is the only permitted `shared/` edit.

## Behaviour specs

### Block Result section (Active session, v3)

- Collapsed: single "Block result" header row (expand chevron), nothing else. Never opens
  itself, never badges.
- Expanded, all blocks: note field (multiline, `rememberSaveable` draft) + Save button
  enabled when draft ≠ saved value. Save → recorder `saveBlockNote`; blank/whitespace saves
  as clear (shared normalizes to null / drops the key). Failure: revert to last saved,
  snackbar.
- Expanded, Manual-eligible blocks only (criterion ≠ null, `success` not enabled, has Ball
  Steps): count row beneath the note — criterion text, then "Not counted" + *Add count*, or
  stepper `0..totalBalls` + "of Y balls" + *Remove count*. Stepper commits write immediately
  (optimistic, revert on failure). Bounds enforced in UI; shared validation is backstop.
- Not rendered at all: v1/v2 session, or (count row only) action-only block / criterion-less
  unit / success-enabled item.

### Session Note at finish (v3)

- Note card on the finish summary between stats and Done. Save-when-dirty; Done flushes
  dirty note first (stay + snackbar on failure). The session is already Completed when the
  summary shows — freeze matrix permits session-note writes (prose editable when Completed);
  that ordering is by design, not an accident to "fix".

### History screen (Completed, v3)

- Entry: tap a v3 history row in `SessionDetailScreen`. Back = pop.
- Session note + per-block notes: same editor component, same save semantics — Completed
  state permits both (freeze matrix).
- Manual counts: display only, "X of Y balls" + criterion text — no stepper, no Add/Remove.
  No count affordance of any kind appears for blocks without a saved manual count.
- No observation content, no provenance labels beyond the manual display — Stage 6.

## Edge cases

- v1/v2 session mid-flight when this ships: block screen and finish summary identical to
  today; history row not tappable.
- Multiple blocks from the same Practice Unit: results keyed by `unitIndex` (snapshot unit
  *entry*), so each block edits its own result — assert in VM test.
- Rotation / process death mid-draft: `rememberSaveable` keeps unsaved text; saved state
  re-derives from the session model.
- Two rapid stepper taps: writes serialize through the existing optimistic pattern;
  read-merge-write on the repo side (Stage 2) keeps sibling keys intact.
- Note saved, then block swept to zero by −1 taps: note is independent of completion
  (design §2 — result is about the block, not tied to steps); it stays.
- Finish with a dirty *block* note (user typed, never saved, then finished from overview):
  accepted loss for v1 — the block screen's Save is the contract; only the finish-summary
  session note gets the Done-flush special case. (Mention in `changes.md` if field test #1
  surfaces it as a real annoyance.)
- Recorder null (misconfigured foundation): affordances render but saves no-op into the
  existing "not available" degradation path — consistent with repo-null handling elsewhere.
- Abandoned sessions: never listed in history (`listCompletedSessions` filters), so the
  locked state needs no UI here.

## Validation checklist

- [ ] Block result section: collapsed by default; note save round-trips; blank save clears;
      failure reverts + snackbars (VM test)
- [ ] Count row only on Manual-eligible blocks; Add/Remove count distinguishes null from 0;
      stepper bounds `0..totalBalls`; immediate write + revert on failure (VM test)
- [ ] Same-unit-twice session: block results land under distinct `unitIndex` keys (VM test)
- [ ] Finish summary: note card renders (v3 only); Done flushes dirty note; failed flush
      stays put (VM test)
- [ ] History: v3 row opens the new screen; both note kinds editable; counts frozen display;
      v2 row inert (VM + screen-level assertions where practical)
- [ ] v2 session: zero new affordances anywhere (VM test with v2 fixture)
- [ ] `.\gradlew.bat :shared:testDebugUnitTest :shared:testReleaseUnitTest :androidApp:testDebugUnitTest :androidApp:testReleaseUnitTest :androidApp:assembleDebug` passes
- [ ] `.\gradlew.bat :shared:lintDebug :androidApp:lintDebug` passes
- [ ] **Manual device flow (local stack):** start a v3 session containing a criterion unit,
      a criterion-less unit, and an action-only block → block notes on all three; count on
      the criterion block only (add, change, remove, re-add); finish with a session note;
      reopen from history; edit both note kinds; confirm the count is frozen; open an old
      v2 completed session's parent and confirm its row is inert
- [ ] After merge: **field test #1** — real range session, impressions written down for the
      Stage 5 design review (epic gate; not a PR blocker)

## Regression risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| `RangeSessionViewModel` constructor/factory change breaks wiring + existing tests | Certain (mechanical) | Nullable param with null default keeps old call sites compiling; test fakes named in files table |
| Text field inside the horizontal pager: keyboard/swipe interplay | Medium | Section collapsed by default; expansion is deliberate; verify swipe-while-focused in manual flow |
| Finish-summary Done flow change loses the one-tap exit | Low | Done without a dirty note is byte-for-byte today's behaviour |
| History row click changes `SessionDetailScreen` prop plumbing | Low | Optional callback, default no-op |
| `CompletedRangeSessionSummary` field addition ripples through fakes | Low (confirmed needed; mapping is `session.supportsDataCapture`) | Default value on the new field keeps fixtures/fakes compiling |
| Scope creep toward Stage 5/6 (tallies, derived counts, provenance labels) | Medium | Manual-only rendering rule fixed in plan; derived renders nothing here |
