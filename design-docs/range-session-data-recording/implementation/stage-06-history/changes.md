# Stage 6: History Detail — changes

**Status:** implemented 2026-07-13. Builds green: `:shared` + `:androidApp` debug/release unit
tests, `:androidApp:assembleDebug`, and `:shared:lintDebug` + `:androidApp:lintDebug`.

## What shipped

All four fixed-plan items (gating, render order, summary-card eligibility, denominators,
read-only, load-failure degrade, `successCount`-driven provenance row) and all four plan-level
calls (P1–P4). Android-only, no `shared/` changes — the plan's dependencies (tallies,
`successCount`, rendering transforms, the recorder read) all shipped in Stages 2/5.

### New — `androidApp/.../ui/components/`

- `ObservationSummarySection.kt` — the per-block read-only summary card: `orderedCaptureTypes`
  chip rows (`ObservationChipRow`, `enabled = false`, no staged value/arming, not dimmed) for the
  scale types, inline `ObservationGridContent` (P2) with an `ObservationRowHeader` denominator row
  for Strike/Shape. No card title — the rows are self-labeling.

### Modified — `androidApp/`

- `ui/components/ObservationGridDialog.kt` — P2 extraction: the grid body (axis headers, 3×3
  heatmap, corner counts, glyphs) moved into `internal fun ObservationGridContent`, taking an
  optional `onCell` handler; cells render with no `clickable` modifier at all when `onCell` is
  null (not just a disabled one) so history renders fully inert, no ripple. `ObservationGridDialog`
  is now a thin `Dialog` wrapper — pixel-identical capture behaviour, verified by the existing
  manual-flow checks plus the full test suite staying green.
- `ui/components/ObservationCaptureSection.kt` — `isChipType` and `ObservationRowHeader` bumped
  `private` → `internal` so the new summary component can reuse the exact capture vocabulary
  (`orderedCaptureTypes` was already `internal`).
- `ui/CompletedRangeSessionViewModel.kt` — `+ measurementPreferencesRepository` ctor param
  (defaulted null; factory updated). UiState: `+ observationsByStep`, `+ handedness`,
  `+ observationsUnavailable`. `load()`: for v3 sessions, loads observations via the recorder
  (failure → `observationsUnavailable = true` + `"Couldn't load observations."` notification,
  session/notes still usable) and handedness (failure/null repo → RIGHT, no notification) —
  mirrors `RangeSessionViewModel.loadSession`'s pattern. v1/v2 sessions skip both loads entirely.
- `ui/screens/RangeSessionHistoryScreen.kt` — per block: `FrozenCountRow` replaced by
  `SuccessProvenanceRow`, driven by `ExecutionBlock.successCount(...)` instead of reading
  `blockResult.manualCount` directly (closes the one path where a stray manual count on a
  Success-enabled block could render as legitimate). Renders `"{hits} of {observed} observed"`
  (Derived, even at 0/0), `"{count} of {totalBalls} balls"` (Manual, same wording as before), or
  nothing (`None`) — criterion text stays above, unchanged. `ObservationSummarySection` inserted
  after the provenance row, gated on `session.supportsDataCapture`, `!observationsUnavailable`,
  and the block structurally having ≥1 Ball Step; the component itself no-ops when the block has
  no enabled types, so zero-type and action-only blocks render byte-identical to Stage 4.
  New `resolveBlockGlyphShape` (module-private): unlike the live block screen's "current ball,"
  history has none, so it resolves from the block's *first* Ball Step, club overrides respected,
  `IRON` fallback. `enabledClubs: List<Club>` added as a screen parameter (defaulted `emptyList()`)
  for this resolution.
- `ui/RangeworkApp.kt` — history route: passes `measurementPreferencesRepository` into
  `CompletedRangeSessionViewModel.factory` and the same enabled-club-catalog expression the
  range-session route already uses into the screen.
- `test/.../CompletedRangeSessionViewModelTest.kt` — `FakeRepo` gained an `observations` map and
  `shouldFailOnObservations`; new `FakeHandednessRepo` (named distinctly from
  `RangeSessionViewModelTest`'s same-purpose fake — Kotlin top-level `private` classes still
  collide at the JVM class-file level across files in one package, unlike `private` functions).
  New cases: v3 loads observations + handedness; v3 observation-load failure sets
  `observationsUnavailable` + notifies while notes still save; v3 handedness fallback to RIGHT
  without a preferences repo (no notification); v2 session skips both loads even when a fake would
  answer; null recorder skips the load without crashing.

## Deviations from plan

- **Grid inert state:** the plan said cells render "inert (no ripple, no staged border)" when
  read-only; implemented as *no `clickable` modifier at all* (not a disabled one) when `onCell` is
  null, which is the stronger form of the same requirement and avoids a wasted disabled-clickable
  allocation.

## Not done here (plan says so)

- Manual device flow (A)–(G) — needs the physical device build; not a code-review blocker.
- Epic-close reminders (`design-decisions.md` status, `CONTEXT.md` vocabulary check, follow-up
  issues) — deferred until Stage 7 and field test #2 are also done.
