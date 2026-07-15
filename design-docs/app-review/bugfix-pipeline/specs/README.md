# Per-bug specs

One file per pipeline bug, written to the template approved under D10
([../plan.md](../plan.md)). Each spec is **self-contained**: it carries the full finding
text, so an agent never needs to open `../potential-bugs.md`.

| Spec | Batch | Bug |
| ---- | ----- | --- |
| [b06](b06-mcp-length-caps-pagination.md) | mcp | No length caps / no pagination on list tools |
| [b04](b04-audit-log-quadratic-diffs.md) | supabase-schema | Audit log O(N²) on `completed_steps` |
| [b05](b05-profiles-rls-grant-mismatch.md) | supabase-schema | `profiles` INSERT/DELETE policies unreachable |
| [b08](b08-missing-fk-indexes.md) | supabase-schema | Missing FK-support indexes |
| [b16](b16-inline-unit-orphan-gc-race.md) | supabase-schema | Inline-unit orphan GC race |
| [b20](b20-observation-step-index-bounds.md) | supabase-schema | `observations.step_index` unvalidated |
| [b14](b14-snapshot-expansion-unbounded.md) | shared-validation | Snapshot expansion unbounded |
| [b15](b15-execution-blocks-drops-steps.md) | shared-validation | `executionBlocks()` drops out-of-range steps |
| [b17](b17-restore-unit-loses-inline-scoping.md) | shared-validation | `restoreUnit` loses inline scoping |
| [b01](b01-finish-abandon-unguarded.md) | shared-repo | Finish/abandon transitions unguarded |
| [b03](b03-close-time-entry-match.md) | shared-repo | `closeTimeEntry` timestamp-string match |
| [b07](b07-client-read-modify-write.md) | shared-repo | Client-side read-modify-write on two columns |
| [b02](b02-duplicate-range-sessions.md) | android-ui | Double-tap creates duplicate range sessions |
| [b09](b09-drag-reorder-header-offsets.md) | android-ui | Drag-to-reorder hardcoded header offsets |
| [b18](b18-validation-errors-misrouted.md) | android-ui | Validation errors misrouted / swallowed |
| [b19](b19-duplicate-error-snackbars.md) | android-ui | Identical consecutive snackbars don't re-show |

## Confirmation methods are not uniform

The D10 template's default — "confirmed means a committed failing test; otherwise DISMISS" —
holds only where a test surface exists. It does for **mcp** (vitest), **shared-validation**
(`:shared` commonTest), and **android-ui** (`:androidApp` unit tests over the existing fakes).

It does **not** for **supabase-schema** or **shared-repo**: the repo has no pgTAP suite, no
`supabase/tests/`, and no fake/mock Supabase client in `:shared` — and the rig runs without
Docker (D2), so `supabase start` isn't available either. Those two batches confirm by
**static evidence** instead: the verify stage must quote the exact lines that prove the
claim, and may dismiss only when the evidence contradicts the finding. Never dismiss a bug
in those batches merely for lack of a test.

Consequence to respect: for those two batches the "test goes green" signal doesn't exist, so
the review stage and the human PR review are the only anti-gaming guards. Specs there carry
a stricter scope boundary to compensate.
