# Batches — composition, status, dispositions

**Proposed under decision D7 — confirm before creating issues.** Batching is by test
harness / review surface, not severity, so each batch is one branch, one agent context,
one PR.

Status values: `pending` → `confirmed` / `dismissed` → `in-progress` → `fixed` (per bug);
batch status tracks the pipeline stage.

## Pipeline batches

### Batch: mcp — run first (rig shakedown; smallest, fastest feedback)

Issue: [#47](https://github.com/loganmartlew/rangework/issues/47) (`needs-verification`)

TypeScript, `pnpm --filter @rangework/mcp test` / `lint`.

| Bug | Summary | Status | Disposition / fix log |
| --- | ------- | ------ | --------------------- |
| B6 | No length caps on free-text fields; no pagination on `list_units`/`list_sessions` | pending | — |

### Batch: supabase-schema

Issue: [#48](https://github.com/loganmartlew/rangework/issues/48) (`needs-verification`)

One migration PR, reviewed as a unit. Verification is mostly static evidence from the
migration files (plus local Supabase SQL tests where available) — judgment-heavy, so
review stage matters more here than elsewhere.

| Bug | Summary | Status | Disposition / fix log |
| --- | ------- | ------ | --------------------- |
| B4 | Audit log O(N²) on `completed_steps` (and `block_results`, `club_overrides`) column diffs | pending | — |
| B5 | `profiles` INSERT/DELETE RLS policies unreachable — no matching grants | pending | — |
| B8 | Missing FK indexes: `practice_session_items.practice_unit_id`, `range_session_time_entries.range_session_id` | pending | — |
| B16 | Inline-unit orphan GC race in `save_practice_session` (no session-row lock) | pending | — |
| B20 | `observations.step_index` unvalidated against snapshot bounds | pending | — |

### Batch: shared-validation

Issue: [#49](https://github.com/loganmartlew/rangework/issues/49) (`needs-verification`)

KMP shared module, validation/decode bounds. `:shared` unit tests.

| Bug | Summary | Status | Disposition / fix log |
| --- | ------- | ------ | --------------------- |
| B14 | Snapshot expansion unbounded — no upper limit on ball/repeat counts | pending | — |
| B15 | `executionBlocks()` silently drops steps with out-of-range `unitIndex` | pending | — |
| B17 | `restoreUnit` loses inline scoping (`PracticeUnitDraft` has no `scopedToSessionId`) | pending | — |

### Batch: shared-repo

Issue: [#50](https://github.com/loganmartlew/rangework/issues/50) (`needs-verification`)

`SupabaseRangeSessionRepository` guarded-write issues. B1 and B7 likely share one
guarded-RPC pattern (mirror `set_range_session_steps_completion`); may include a
migration — coordinate with supabase-schema batch ordering.

| Bug | Summary | Status | Disposition / fix log |
| --- | ------- | ------ | --------------------- |
| B1 | `finishSession`/`abandonSession` unguarded — abandon-after-finish destroys history | pending | — |
| B3 | `closeTimeEntry` timestamp-string equality match; no open-entry filter; silent time loss | pending | — |
| B7 | `overrideStepClubs`/`saveBlockResult` still client-side read-modify-write | pending | — |

### Batch: android-ui

Issue: [#51](https://github.com/loganmartlew/rangework/issues/51) (`needs-verification`)

Compose/ViewModel. `:androidApp` tests + lint. Largest UI-test-setup cost — run on a
fresh Claude window / full Codex headroom.

| Bug | Summary | Status | Disposition / fix log |
| --- | ------- | ------ | --------------------- |
| B2 | Double-tap "Start session" creates duplicate range sessions (no VM in-flight guard) | pending | — |
| B9 | Drag-to-reorder hardcoded `headerCount` offsets (latent) | pending | — |
| B18 | Validation errors misrouted (`UnitInstructions` → title) or swallowed (`Tags`) | pending | — |
| B19 | Identical consecutive error snackbars don't re-show (keyed on message string) | pending | — |

## Outside the pipeline

### Quick wins — fixed by hand (plan.md Phase 1)

| Bug | Summary | Status |
| --- | ------- | ------ |
| B10 | Local MCP `deploy` script skips the R2 methodology upload | pending |
| B11 | og:image is SVG; social previews break | pending |
| B12 | All store CTAs point at the Play Store homepage | pending |

### Deferred — need human decisions (plan.md Phase 2)

| Bug | Summary | Issue |
| --- | ------- | ----- |
| B13 | Cookie policy references nonexistent consent mechanism | [#44](https://github.com/loganmartlew/rangework/issues/44) |
| B21 | delete-account not atomic across services | [#45](https://github.com/loganmartlew/rangework/issues/45) |
| B22 | Planning saves last-write-wins across devices | [#46](https://github.com/loganmartlew/rangework/issues/46) |

## Batching decisions (D7, resolved 2026-07-15)

- B3's spec covers only the `closeTimeEntry` filter hardening; the related lifecycle/
  rotation items are split into the android-ui batch or a follow-up, decided at
  spec-writing time.
- shared-repo carries its own guarded-RPC migrations (client change + RPC in one PR);
  supabase-schema runs first.
- B9: confirmation via a unit test on the index-mapping function is acceptable; if
  there's no good testing surface, dismiss to tech-debt rather than forcing a Compose
  UI test.
