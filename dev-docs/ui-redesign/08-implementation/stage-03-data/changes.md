# Stage 03 — Data enablers: changes

## Summary of changes

### 1. `DuplicateUnitUseCase` (B29)

**New class** in `shared/src/commonMain/.../usecase/PracticeUnitUseCases.kt`.

Mirrors `DuplicatePracticeSessionUseCase`. Fetches the original unit via `GetPracticeUnitUseCase`, deep-copies all fields and instructions into a `PracticeUnitDraft`, then saves via `SavePracticeUnitUseCase` to produce an independent copy with a new server-assigned id. Instruction ids are also regenerated (new rows inserted by the RPC).

**Wired in:**
- `DataFoundation` — new `duplicatePracticeUnitUseCase: DuplicateUnitUseCase` field.
- `createDataFoundation(client)` — instantiated with fresh `GetPracticeUnitUseCase` / `SavePracticeUnitUseCase` delegates, same pattern as the session duplicate.
- `PracticePlannerViewModel` — new `duplicateUnit(unitId)` method and `duplicatedUnitId: String?` state field (mirrors `duplicateSession` / `duplicatedSessionId`). `clearDuplicatedUnitId()` added for consumers.

**Test files updated** (compile fix): `PracticePlannerViewModelTest.kt` and `SettingsViewModelTest.kt` both construct `DataFoundation` by hand — both now pass `duplicatePracticeUnitUseCase`.

---

### 2. `estimateSessionDurationMinutes` (B15)

**New pure function** added to `shared/src/commonMain/.../model/PracticePlanningMetrics.kt`.

Signature: `estimateSessionDurationMinutes(session: PracticeSession, unitsById: Map<String, PracticeUnit>): Int`

Algorithm: total balls (`session.derivedBallCount(unitsById)`) × `SECONDS_PER_BALL` (15), rounded to nearest minute with `(total + 30) / 60`. Returns 0 when there are no balls. Items with no matching unit contribute zero balls (graceful degradation).

`SECONDS_PER_BALL = 15` is a named top-level constant — product assumption, flagged for sign-off. Duration display strings (e.g. "~15 min") are left to the UI layer.

---

### 3. `EnabledClubCount` (B44)

**New model** in `shared/src/commonMain/.../model/EnabledClubCount.kt`.

```kotlin
data class EnabledClubCount(val enabled: Int, val total: Int)
```

Companion factory `EnabledClubCount.from(catalog, enabledCodes)` derives counts from the already-loaded club catalog and enabled-codes set — no extra network round-trip.

Exposed on `PracticePlannerUiState` as a computed property:
```kotlin
val enabledClubCount: EnabledClubCount
    get() = EnabledClubCount.from(clubCatalog, enabledClubCodes)
```

Intended display format (UI layer responsibility): "12 of 30 clubs enabled".

---

### 4. `RecentItem` / `recentItems()` (B27)

**New sealed interface and function** in `shared/src/commonMain/.../model/RecentItems.kt`.

```kotlin
sealed interface RecentItem {
    val id: String
    val updatedAt: Instant
    data class Unit(val practiceUnit: PracticeUnit) : RecentItem { ... }
    data class Session(val practiceSession: PracticeSession) : RecentItem { ... }
}

fun recentItems(units, sessions, limit = 5): List<RecentItem>
```

Derives recency from the existing `updatedAt` timestamps on `PracticeUnit` and `PracticeSession` — **no migration required**. Returns an interleaved, most-recent-first list bounded to `limit` items. Returns empty when both lists are empty (graceful degradation — S9 can hide the strip).

Exposed on `PracticePlannerUiState` as a computed property:
```kotlin
val recentItems: List<RecentItem>
    get() = recentItems(units, sessions)
```

---

### 5. `NextMoveState` / `resolveNextMoveState()` (B26)

**New sealed interface and function** in `shared/src/commonMain/.../model/NextMoveState.kt`.

```kotlin
sealed interface NextMoveState {
    data object NoUnits : NextMoveState
    data object UnitsNoSessions : NextMoveState
    data object Both : NextMoveState
    data class ResumeEditing(val entityId: String, val isUnit: Boolean) : NextMoveState
}
```

Priority ordering: `NoUnits` → `UnitsNoSessions` → `ResumeEditing` (if a unit or session was just saved) → `Both`.

Exposed on `PracticePlannerUiState` as a computed property driven by `savedUnitId` / `savedSessionId`:
```kotlin
val nextMoveState: NextMoveState
    get() = resolveNextMoveState(units, sessions, savedUnitId, savedSessionId)
```

Once those transient fields are consumed (via `consumeSaved*Id()`), the state falls back to `Both`.

---

### 6. Tests — `Stage03DataEnablerTest.kt`

New test file at `shared/src/commonTest/.../usecase/Stage03DataEnablerTest.kt` — 17 tests covering all five capabilities:

| Group | Tests |
|---|---|
| `DuplicateUnitUseCase` | Independent copy (new id, deep-copied instructions, no shared references) |
| `estimateSessionDurationMinutes` | Zero balls → 0 min; small count (4 balls → 1 min); large count (180 balls → 45 min); missing unit → 0 |
| `EnabledClubCount` | Partial enable; none enabled; all enabled; enabled codes not in catalog ignored |
| `recentItems` | Most-recent-first ordering; mixed units+sessions; bounded by limit; empty inputs |
| `resolveNextMoveState` | All five branches: NoUnits, UnitsNoSessions, Both, ResumeEditing(unit), ResumeEditing(session); NoUnits trumps lastSavedUnitId |

---

## Regression risks

**R1 — `DataFoundation` is a data class with a new required field.** Any call site constructing `DataFoundation(...)` directly will fail to compile until updated. Two test files were updated in this stage (`PracticePlannerViewModelTest.kt`, `SettingsViewModelTest.kt`). If other call sites exist outside this repo, they will break.

**R2 — `PracticePlannerUiState` has three new computed properties.** These are derived-only (no stored state) so they do not affect serialization or equality semantics of the data class. However, any code that pattern-matches on all fields (e.g., exhaustive `copy(...)` or reflection-based tests) may need updating.

**R3 — `duplicatedUnitId` added to sign-out reset path.** The reset in `onAuthStateChanged` now clears `duplicatedUnitId = null`. If the reset was previously tested for exhaustiveness, those tests may need a new expected field.

**R4 — `SECONDS_PER_BALL = 15` is a product assumption.** The constant is named and top-level so it is tunable, but screens consuming `estimateSessionDurationMinutes` will show different numbers if this value changes. No sign-off has been obtained from product; flag before S6 ships the duration display.

**R5 — No Supabase migration was added.** Recency uses existing `updatedAt` columns — no schema change, no RLS change, no serialization change.

---

## Validation checklist

- [x] `.\gradlew.bat :shared:testDebugUnitTest :androidApp:testDebugUnitTest :androidApp:assembleDebug` — BUILD SUCCESSFUL
- [x] `.\gradlew.bat :shared:lintDebug :androidApp:lintDebug` — BUILD SUCCESSFUL (clean)
- [x] `commonTest`: duplicate-unit produces an independent copy (new id, deep-copied instructions, no shared references)
- [x] `commonTest`: `estimateSessionDurationMinutes` table test across 0, small (4 balls), and large (180 balls) counts
- [x] `commonTest`: `estimateSessionDurationMinutes` returns 0 when unit not found in map
- [x] `commonTest`: club enabled-count matches the enabled set; ignores codes not in catalog
- [x] `commonTest`: `recentItems` returns most-recent-first, interleaves units and sessions, is bounded by `limit`
- [x] `commonTest`: `resolveNextMoveState` resolves all five branches correctly; `NoUnits` takes priority over `lastSavedUnitId`
- [ ] Verify `SECONDS_PER_BALL = 15` with product before S6 ships the duration display
- [ ] Manual smoke: sign-in → units load → `enabledClubCount` computed property shows correct totals in Settings (S8)
- [ ] Manual smoke: after saving a unit, `nextMoveState` is `ResumeEditing`; after consuming the id, falls back to `Both`
- [ ] Manual smoke: `recentItems` strip on Overview (S9) shows 5 most-recently-edited items in correct order
- [ ] Manual smoke: duplicating a unit from the Units list creates an independent copy and navigates to it (S5)
- [ ] No breaking change to existing serialized models / wire format — confirmed (all new types are in-memory only)
- [ ] Auth-gated flow preserved: `duplicateUnit()` guards on `activeUserId` and `dataFoundation` null checks
