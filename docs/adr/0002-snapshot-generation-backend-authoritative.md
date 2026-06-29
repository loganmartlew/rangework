# Snapshot generation is authoritative in the backend

Range Session Snapshot/Step expansion — turning a Practice Session's Session Items
(Practice Instructions × Repeat Count) into the flat ordered list of Steps — is
performed exclusively by the Supabase `start_range_session` RPC. The shared Kotlin
module treats `RangeSessionSnapshot` as an opaque, trusted blob and does not duplicate
the expansion logic. The RPC runs atomically inside the same transaction that creates
the `range_sessions` row, which a client-side builder cannot replicate without multiple
round-trips. This keeps the expansion logic — the most complex piece of SQL in the
project, reading across four tables — in a single place and avoids the dual-maintenance
burden of coordinating Kotlin and PL/pgSQL implementations.

## Status

accepted

## Considered options

- **Move/duplicate expansion into the shared Kotlin module.** Rejected. A pure-Kotlin
  `SnapshotBuilder` would pin the expansion invariant in testable, platform-agnostic
  code, and contract tests could reconcile it against the RPC output. However, this
  introduces a second source of truth that must be kept in sync — any schema or
  expansion-rule change would require coordinated updates in both Kotlin and
  PL/pgSQL. The RPC's atomic read-and-insert also can't be replicated client-side
  without multiple round-trips.

- **Backend stays authoritative (chosen).** The RPC is the single source of truth.
  The shared module treats the snapshot as opaque. One place to maintain, one
  transaction to reason about. The trade-off is that the shared module cannot
  independently validate snapshot structure at build time — mitigated by the RPC's
  deterministic behaviour and the fact that it is exercised on every Range Session
  start.

## Consequences

- **The shared module has no snapshot builder.** `RangeSessionSnapshot`,
  `SnapshotUnit`, `SnapshotInstruction`, and `SnapshotStep` remain pure data classes
  with `@Serializable` annotations. No expansion or validation logic is added to
  `apps/mobile/shared`.
- **The RPC is the single source of truth for snapshot structure.** Any change to
  the snapshot schema (e.g. a version 2) must be implemented in the RPC first; the
  Kotlin models then follow the RPC's output shape.
- **No contract tests between Kotlin and the RPC are needed**, since there is no
  second implementation to reconcile against.
- **A future web frontend would consume snapshots from the backend**, not build them
  client-side. If the expansion logic later moves from the Supabase RPC to a
  dedicated API, it would remain server-side — the shared module's opaque-DTO
  treatment is unchanged by that migration.
