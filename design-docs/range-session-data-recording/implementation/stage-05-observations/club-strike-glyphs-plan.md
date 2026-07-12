# Stage 5 follow-up: Per-club strike-glyph faces

**Parent stage:** [`implementation-plan.md`](./implementation-plan.md) + [`changes.md`](./changes.md) — the
`ClubfaceGlyph` shipped as a single front-view face (P3 glyph set); this replaces that one face with
four club-family faces and makes the shape follow the ball's club.
**Design reference:** [`prototype.html`](./prototype.html) — currently one `faceSvg`; this plan replaces
it with the four faces so the side-by-side invariant holds.
**Decisions:** [`docs/adr/0005-driver-is-its-own-club-category.md`](../../../../docs/adr/0005-driver-is-its-own-club-category.md),
[`docs/adr/0006-clubface-glyph-geometry-in-shared.md`](../../../../docs/adr/0006-clubface-glyph-geometry-in-shared.md).
Glossary: `Club` category list in [`apps/mobile/CONTEXT.md`](../../../../apps/mobile/CONTEXT.md) now leads with Driver.
**Status:** built 2026-07-12.

## Why

The shipped strike glyph is a single lopsided clubface — the owner reads it as "a wild shape for a golf
club," and it looks identical whether the ball was hit with a putter or a driver. Strike location is
only meaningful *relative to the club's face*, so the glyph should render the **club it was actually hit
with**. We replace the one face with four recognisable club-family faces (driver / wood / iron / putter),
each selected per-ball from the resolved club, and split **Driver** out of the Wood category so the
shape falls out of the domain enum instead of a club-code special-case (ADR 0005). The four faces'
geometry lives in `shared` as platform-neutral data so a future iOS app renders the same shapes without
re-porting them (ADR 0006).

## Objective

1. **Four club-family faces** replace the single `ClubfaceGlyph` outline: driver, wood, iron, putter.
   Front-view (the impact dot marks *where on the face*), bold-silhouette differentiation, minimal
   interior detail; **driver = a taller wood**. Each face owns its own outline **and** its own nine
   impact-dot positions, matched to that club's real face extent.
2. **Shape follows the ball's club.** The face is chosen from the ball's resolved `ClubCategory`
   (override → baked step club → unit default → null), mapped to a `ClubGlyphShape`. Never block-wide:
   each rendered glyph reflects the specific ball it represents.
3. **New `DRIVER` club category** + a seeded `mini_driver` club, via one migration.
4. **Geometry + mapping in `shared`**, one generic renderer in `androidApp`; the other five glyphs
   are untouched.

## Fixed rules

- **Category → shape mapping** (`ClubCategory.toGlyphShape()`): `DRIVER`→driver, `WOOD`→wood,
  `HYBRID`→wood, `IRON`→iron, `WEDGE`→iron, `PUTTER`→putter. **Null club → iron** (the neutral fallback);
  a club code not present in `enabledClubs` resolves as unknown → iron.
- **Per-ball club resolution** reuses the existing precedence (mirrors `buildInstructionRows`):
  `clubOverrides[stepIndex] ?? steps[stepIndex].club`, then the matching `Club.category` from
  `enabledClubs`. There is **no** new ViewModel state and no `shared` resolution helper for the *value*
  — the code→category lookup is a UI concern over data already in scope.
  - **Live capture surfaces** (staged value glyph, grid launcher, grid picker) use the **current ball**:
    the first incomplete Ball Step in the block in snapshot order. When the block has no incomplete Ball
    Step the live surfaces aren't shown, so no club is needed.
  - **Ball edit sheet** uses **each reviewed ball's own** step index — its stored override/baked club,
    so a corrected 8-iron ball and a driver ball in the same block show different faces.
- **Dot semantics unchanged.** Strike location stays the fixed 3×3 (`heel/center/toe × high/middle/low`);
  only the dot *positions in the viewBox* vary per shape. Handedness still mirrors the face and the
  impact dot is still placed by **screen** column (already handedness-resolved by the caller), outside
  the mirror — identical to today.
- **Grid picker cells** (the 3×3 legend in `ObservationGridDialog`) all render the **current ball's**
  shape; only the dot moves cell to cell.
- **Geometry is data, not drawing.** Each `ClubGlyphShape` is described in `shared` by a viewBox, an
  outline as ordered path commands, nine dot positions, and any marks (hosel line, putter sight line).
  `androidApp` walks that data onto a Compose `Canvas`; mirroring + screen-column dot placement stay in
  the renderer, not the data.
- **Catalog:** `mini_driver` = `Mini Driver`, category `DRIVER`, `default_enabled=false`, `sort_order=2`;
  `two_wood`…`putter` renumber `+1` so the Driver family leads the woods. `driver` flips `WOOD`→`DRIVER`.
  The `clubs_category_check` constraint gains `'DRIVER'`. Migration is idempotent (upsert), matching the
  existing seed.

## Likely files

### `shared/` (KMP)

| File | Change |
|---|---|
| `model/Club.kt` | Add `DRIVER` to `ClubCategory` (before `WOOD`, matching sort intent). |
| `model/ClubGlyphShape.kt` *(new)* | `enum class ClubGlyphShape { DRIVER, WOOD, IRON, PUTTER }`; `fun ClubCategory?.toGlyphShape(): ClubGlyphShape` with the null→iron fallback. Plus the platform-neutral geometry: per-shape viewBox, outline path commands, nine dot positions, marks. Pure data + a pure function — no Compose, no Android imports. |
| `commonTest/.../ClubGlyphShapeTest.kt` *(new)* | Table test of the six categories + null → expected shape; sanity that every shape defines exactly nine dot positions and a closed outline. |

### `supabase/`

| File | Change |
|---|---|
| `migrations/20260712120000_driver_club_category.sql` *(new)* | Drop/recreate `clubs_category_check` to add `'DRIVER'`; `update clubs set category='DRIVER' where code='driver'`; shift `sort_order` `+1` for `sort_order >= 2`; insert `mini_driver` at `sort_order=2` (`on conflict do update`). Reference-data only — no user rows, no snapshot rewrite (baked club **codes** are unchanged). |

### `androidApp/`

| File | Change |
|---|---|
| `ui/components/ObservationGlyphs.kt` | Rewrite `ClubfaceGlyph` to take a `ClubGlyphShape` (was: only `location` + `handedness`), and render by walking the shared geometry data (outline path, per-shape dot positions) — one generic path builder replacing the hardcoded `xs`/`ys`/face path. Mirror + screen-column dot logic preserved. The other five glyphs untouched. |
| `ui/components/ObservationCaptureSection.kt` | Thread the resolved current-ball `ClubGlyphShape` into `GridLauncherRow` → `GridValueGlyph` (new param), passed to `ClubfaceGlyph`. `GridValueGlyph`'s `STRIKE_LOCATION` branch gains the shape arg. |
| `ui/components/ObservationGridDialog.kt` | `cellGlyph` passes the current-ball shape into `ClubfaceGlyph` ([:205](../../../../apps/mobile/androidApp/src/main/java/com/loganmartlew/rangework/android/ui/components/ObservationGridDialog.kt#L205)); dialog signature gains a `ClubGlyphShape` param. |
| `ui/components/ExecutionBlockPage.kt` | Compute the **current-ball** `ClubGlyphShape` (first incomplete Ball Step → resolve club via `clubOverrides`/step club/`enabledClubs` → `toGlyphShape()`) and pass it into `ObservationCaptureSection` (which already receives `handedness`, so this is the same shape of plumbing). |
| `ui/components/BallEditSheet.kt` | Resolve **each ball row's own** shape from its step index and pass it into the row's `GridValueGlyph` / grid launcher. |
| `ui/screens/RangeSessionScreen.kt` | Pass the current-ball shape (and, for the grid dialog opened from the sheet, the reviewed ball's shape) where it wires `ObservationGridDialog` / the capture section. Uses `enabledClubs` + `clubOverrides` already in scope. |

### Design reference

| File | Change |
|---|---|
| `design-docs/.../stage-05-observations/prototype.html` | Replace the single `faceSvg` with four faces + a `clubGlyphShape(clubCode)` helper mirroring `toGlyphShape`, so the prototype demonstrates all four and the side-by-side invariant holds. |

## Behaviour

- **Recording a 7-iron ball:** capture card's staged strike glyph and the grid picker show the **iron**
  face; the dot sits at the chosen heel/toe × high/low zone. Commit, then a driver instruction's balls
  in the same block show the **driver** face.
- **Wedge / hybrid:** wedge balls render the iron face; hybrid balls render the wood face (ADR 0005/0006
  mapping).
- **Putter:** wide, short blade with a sight line; the nine zones spread wide and vertically compressed
  to fit the real face extent (per-shape dot grid).
- **Mixed-club block in the edit sheet:** each ball row shows the face for *that* ball's stored club, so
  a block that mixed clubs shows mixed faces — truthful to Club Override.
- **Left-handed player:** face mirrors as today (hosel/heel drawn on the heel column); dot still placed
  by screen column.

## Edge cases

- **Instruction with no club and no unit default** (null resolves): iron face. No crash, no blank glyph.
- **Baked club disabled from the bag after planning** (code not in `enabledClubs`): unknown → iron. If
  this proves too lossy in the field, expose the full catalog to the range screen — deferred, not built.
- **Block with no incomplete Ball Step** (complete / action-only): live capture surfaces aren't shown,
  so no current-ball shape is computed; edit sheet still resolves per reviewed ball.
- **Snapshot has baked club code but the catalog row's category changed** (`driver` now `DRIVER`):
  fine — resolution is by current catalog category, and the glyph is presentation only. No stored data
  is keyed on category.
- **`mini_driver` never enabled by default:** appears only for users who add it; renders the driver face.

## Validation checklist

- [ ] `.\gradlew.bat :shared:testDebugUnitTest :shared:testReleaseUnitTest :androidApp:testDebugUnitTest :androidApp:testReleaseUnitTest :androidApp:assembleDebug`
- [ ] `.\gradlew.bat :shared:lintDebug :androidApp:lintDebug`
- [ ] New `ClubGlyphShapeTest` green (mapping + geometry invariants).
- [ ] Grep for exhaustive `when (category` / `ClubCategory.` across `shared`, `androidApp`, `apps/mcp` — every non-else branch set gains `DRIVER` (compiler will flag Kotlin `when`s; check SQL/TS/site copy manually).
- [ ] Migration applies cleanly on a seeded DB; `driver` is `DRIVER`, `mini_driver` present at `sort_order=2`, woods renumbered, existing user-enabled rows intact.
- [ ] **Manual device flow:**
      **(A)** record strike with an iron, a wood, a driver, and a putter in turn — each shows the right
      face; dot lands in the tapped zone;
      **(B)** wedge → iron face, hybrid → wood face;
      **(C)** left-handed profile: face mirrors, dot column correct;
      **(D)** edit sheet on a mixed-club block: each ball row shows its own club's face;
      **(E)** instruction with no club: iron face, no crash;
      **(F)** enable `mini_driver`, assign it, record: driver face.

## Risks

| Risk | Likelihood | Mitigation |
|---|---|---|
| Enum/DB-constraint deploy ordering — an app build without `DRIVER` reads a `DRIVER` row and throws on deserialization | Low (solo project) | Ship the enum change and the migration together; ADR 0005 records the coupling |
| A `when (category)` elsewhere (MCP, site copy, tests) silently misses `DRIVER` | Medium | Compiler flags exhaustive Kotlin `when`s; explicit grep step in the checklist for SQL/TS/prose |
| Four faces legible at 24 dp is harder than at 40 dp | Medium | Silhouette-first design (proportion, not detail); validate at both sizes on device in flow (A) before finalising geometry |
| Geometry-as-shared-data diverges from the five inline glyphs and reads as inconsistent | Low | Deliberate; ADR 0006 is the record. Other glyphs migrate only if/when iOS lands |
| Per-shape dot grids drift out of sync with the outline (dot off the face) | Low–Medium | Geometry test asserts nine dots per shape; visual check in flow (A); dots authored against the same viewBox as the outline |
| Resolving club via `enabledClubs` only (not full catalog) mislabels disabled-but-baked clubs as iron | Low | Acceptable per null→iron rule; escalate to full-catalog exposure only if it bites in the field |
