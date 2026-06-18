# Stage 2: Shared Models & Data Layer

## Objective

Build the complete Kotlin shared-module infrastructure for Range Sessions: domain models, snapshot models, progress computation helpers, repository interface, Supabase implementation, all use cases, and DataFoundation wiring. After this stage the shared module can start range sessions, read them, toggle step completions, compute progress, manage time entries, and handle finish/abandon — all testable with unit tests and no UI.

**Tickets:** RWK-19, RWK-21, RWK-22, RWK-23

## Dependencies

- **Stage 1** must be complete (database tables, RPC, and policies exist).
- Depends on existing shared module patterns: `DataFoundation`, `SupabaseEndpointConfig`, `RangeworkSupabaseClientFactory`, existing repository/use-case layering, `kotlinx.serialization`.
- Depends on existing Supabase client configuration (PostgREST, RPC call patterns).

## Affected Screens

None. This stage is shared-module only — no Android UI changes.

## Likely Files

### New files

| File | Purpose |
|---|---|
| `shared/src/commonMain/kotlin/.../model/RangeSession.kt` | `RangeSession` domain model |
| `shared/src/commonMain/kotlin/.../model/RangeSessionSnapshot.kt` | `RangeSessionSnapshot`, `SnapshotUnit`, `SnapshotInstruction`, `SnapshotStep` models |
| `shared/src/commonMain/kotlin/.../model/CompletedStep.kt` | `CompletedStep` model (`stepIndex`, `completedAt`) |
| `shared/src/commonMain/kotlin/.../model/ActiveRangeSessionSummary.kt` | Lightweight summary for Overview carousel |
| `shared/src/commonMain/kotlin/.../model/CompletedRangeSessionSummary.kt` | Summary for practice session detail history |
| `shared/src/commonMain/kotlin/.../model/RangeSessionProgress.kt` | Extension functions for progress computation |
| `shared/src/commonMain/kotlin/.../repository/RangeSessionRepository.kt` | Repository interface |
| `shared/src/commonMain/kotlin/.../data/SupabaseRangeSessionRepository.kt` | Supabase-backed implementation |
| `shared/src/commonMain/kotlin/.../usecase/RangeSessionUseCases.kt` | All range session use cases |
| `shared/src/commonTest/kotlin/.../usecase/RangeSessionUseCaseTest.kt` | Use case tests with hand-written fakes |
| `shared/src/commonTest/kotlin/.../model/RangeSessionProgressTest.kt` | Progress helper edge case tests |

### Modified files

| File | Change |
|---|---|
| `shared/src/commonMain/kotlin/.../data/DataFoundation.kt` | Add all range session use case fields to `DataFoundation` data class; wire in `createDataFoundation` factory |

## New Components Required

### Domain models

**`RangeSession`** — the core persisted entity:
- `id`, `sourceSessionId`, `sessionName`, `snapshot`, `snapshotVersion`, `completedSteps`, `clubOverrides`, `lastViewedStepIndex`, `startedAt`, `completedAt`, `abandonedAt`
- All fields use `@Serializable` and `@SerialName` annotations matching DB column names

**`RangeSessionSnapshot`** — immutable JSONB payload:
- `sessionNotes`, `units: List<SnapshotUnit>`, `steps: List<SnapshotStep>`

**`SnapshotUnit`** — template unit structure:
- `unitTitle`, `unitNotes`, `unitFocus`, `itemNotes`, `itemFocusCue`, `club`, `clubDisplayName`, `repeatCount`, `instructions: List<SnapshotInstruction>`

**`SnapshotInstruction`** — single instruction within a unit:
- `text`, `ballCount`

**`SnapshotStep`** — flattened, individually addressable step:
- `unitIndex`, `instructionIndex`, `repNumber`, `totalReps`, `instructionText`, `ballCount`, `club`, `clubDisplayName`, `unitTitle`, `notes`, `focusCue`

**`CompletedStep`** — completion entry:
- `stepIndex`, `completedAt`

**`ActiveRangeSessionSummary`** — for Overview banner:
- `id`, `sessionName`, `totalSteps`, `completedStepCount`, `startedAt`

**`CompletedRangeSessionSummary`** — for session detail history:
- `id`, `sessionName`, `totalSteps`, `completedStepCount`, `totalBalls`, `completedBalls`, `startedAt`, `completedAt`, `elapsedSeconds`

### Progress computation helpers

Extension functions on `RangeSession`:
- `completedStepCount()` — count of `completedSteps`
- `totalStepCount()` — `snapshot.steps.size`
- `completionPercentage()` — guards division by zero (return 0.0 if totalSteps is 0)
- `completedBalls()` — sum of `ballCount` for completed step indices, skipping null ballCounts
- `totalBalls()` — sum of all step `ballCount` values, skipping nulls
- `completedUnits()` — count of units where all associated steps (by `unitIndex`) are in `completedSteps`
- `isFullyComplete()` — `completedSteps.size == snapshot.steps.size`
- `isActive()` — `completedAt == null && abandonedAt == null`

### Repository interface

`RangeSessionRepository` with methods:
- `startSession(rangeSessionId, sessionId) → RangeSession`
- `getSession(rangeSessionId) → RangeSession?`
- `listActiveSessions() → List<ActiveRangeSessionSummary>`
- `listCompletedSessions(sessionId) → List<CompletedRangeSessionSummary>`
- `toggleStepComplete(rangeSessionId, stepIndex, completed) → RangeSession`
- `overrideStepClub(rangeSessionId, stepIndex, clubCode) → RangeSession`
- `updateLastViewedStep(rangeSessionId, stepIndex)`
- `finishSession(rangeSessionId) → RangeSession`
- `abandonSession(rangeSessionId)`
- `recordTimeEntry(rangeSessionId, enteredAt)`
- `closeTimeEntry(rangeSessionId, enteredAt, exitedAt)`
- `getElapsedSeconds(rangeSessionId) → Long`
- `hasActiveSessionsForTemplate(sessionId) → Boolean`

### Supabase repository implementation

`SupabaseRangeSessionRepository`:
- `startSession` → calls `start_range_session` RPC, deserializes returned JSONB
- `getSession` → PostgREST SELECT by ID
- `listActiveSessions` → SELECT with WHERE `completed_at IS NULL AND abandoned_at IS NULL`, returns lightweight summaries (needs `completed_steps` array length and snapshot step count — either via JSONB path queries or by fetching and computing client-side)
- `listCompletedSessions` → SELECT by `source_session_id` WHERE `completed_at IS NOT NULL AND abandoned_at IS NULL`
- `toggleStepComplete` → read `completed_steps`, add/remove `CompletedStep` entry, PATCH back
- `overrideStepClub` → read `club_overrides`, update map, PATCH back
- `updateLastViewedStep` → PATCH `last_viewed_step_index`
- `finishSession` → PATCH `completed_at = now()`
- `abandonSession` → PATCH `abandoned_at = now()`
- `recordTimeEntry` → INSERT into `range_session_time_entries`
- `closeTimeEntry` → UPDATE time entry row, set `exited_at`
- `getElapsedSeconds` → SELECT time entries, sum intervals client-side
- `hasActiveSessionsForTemplate` → SELECT count WHERE `source_session_id = sessionId AND completed_at IS NULL AND abandoned_at IS NULL`

Row DTOs: `RangeSessionRow`, `TimeEntryRow` with `@SerialName` for snake_case mapping.

### Use cases

| Use Case Class | Repository Method | Notes |
|---|---|---|
| `StartRangeSessionUseCase` | `startSession` | Generates UUID, delegates to repo |
| `GetRangeSessionUseCase` | `getSession` | Simple delegation |
| `ListActiveRangeSessionsUseCase` | `listActiveSessions` | Simple delegation |
| `ListCompletedRangeSessionsUseCase` | `listCompletedSessions` | Accepts sessionId parameter |
| `ToggleStepCompleteUseCase` | `toggleStepComplete` | Delegates with stepIndex and completed flag |
| `OverrideStepClubUseCase` | `overrideStepClub` | Delegates with stepIndex and clubCode |
| `UpdateLastViewedStepUseCase` | `updateLastViewedStep` | Fire-and-forget, no return value |
| `FinishRangeSessionUseCase` | `finishSession` | Returns updated session |
| `AbandonRangeSessionUseCase` | `abandonSession` | No return value |
| `RecordTimeEntryUseCase` | `recordTimeEntry` | Fire-and-forget |
| `CloseTimeEntryUseCase` | `closeTimeEntry` | Fire-and-forget |
| `GetElapsedSecondsUseCase` | `getElapsedSeconds` | Returns Long |
| `HasActiveRangeSessionsUseCase` | `hasActiveSessionsForTemplate` | For template deletion warning |

### DataFoundation changes

Add all 13 use case fields to the `DataFoundation` data class. Wire `SupabaseRangeSessionRepository` in `createDataFoundation(client)`.

### Test fakes

- `FakeRangeSessionRepository` implementing `RangeSessionRepository` with in-memory state
- Supports: storing sessions, toggling completions, tracking time entries, simulating start/finish/abandon

## Validation Checklist

- [ ] All new model classes compile and are `@Serializable`
- [ ] `@SerialName` annotations match the JSONB field names from the snapshot schema (version 1)
- [ ] `RangeSessionSnapshot` can deserialize a fixture JSON matching the RPC output
- [ ] Progress helpers: `completedStepCount()` returns correct count
- [ ] Progress helpers: `totalStepCount()` returns `snapshot.steps.size`
- [ ] Progress helpers: `completionPercentage()` returns 0.0 when total is 0 (no division by zero)
- [ ] Progress helpers: `completedBalls()` correctly skips steps with null `ballCount`
- [ ] Progress helpers: `totalBalls()` correctly skips null `ballCount` values
- [ ] Progress helpers: `completedUnits()` counts only fully-completed units
- [ ] Progress helpers: `isFullyComplete()` true when all steps completed, false otherwise
- [ ] Progress helpers: `isActive()` true when both timestamps null, false when either is set
- [ ] Repository interface compiles with all method signatures
- [ ] `SupabaseRangeSessionRepository` compiles and wires to Supabase client
- [ ] `DataFoundation` includes all new use case fields
- [ ] `createDataFoundation` wires all new use cases
- [ ] Use case tests pass with fake repository
- [ ] Progress helper tests cover edge cases: empty session (0 steps), all complete, no ball counts, mixed null/non-null ball counts
- [ ] `.\gradlew.bat :shared:testDebugUnitTest :shared:testReleaseUnitTest` passes
- [ ] `.\gradlew.bat :shared:lintDebug` passes

## Accessibility Requirements

Not applicable — this stage has no UI.

## Regression Risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| `DataFoundation` data class change breaks existing callers | Medium | All existing fields remain unchanged. New fields are additive. Update all call sites that construct `DataFoundation` (the factory function). |
| Snapshot JSONB deserialization fails due to field name mismatch with RPC output | Medium | Test with fixture JSON captured from a real RPC call in Stage 1. Ensure all `@SerialName` annotations match exactly. |
| `kotlinx.serialization` version doesn't support a needed feature (e.g., polymorphic JSONB, Instant serialization) | Low | The existing codebase already serializes `Instant` and JSONB fields. Follow the same patterns. |
| New test file naming or placement doesn't match existing test discovery | Low | Follow existing test file patterns in `shared/src/commonTest/`. |
| `SupabaseRangeSessionRepository` PostgREST calls use wrong table/column names | Medium | Cross-reference with migration file. Use constants or verify at compile time where possible. |
| `toggleStepComplete` read-modify-write creates race condition | Low | Online-only, single-device typical use. Accept last-write-wins. Document as known limitation. |
