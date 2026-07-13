# Stage 6: Inline Units MCP (parallel with Stage 5)

**Epic:** [`../../epic-implementation-plan.md`](../../epic-implementation-plan.md)
**Design:** [`../../design-decisions.md`](../../design-decisions.md) (§6–§9)
**Vocabulary:** [`apps/mobile/CONTEXT.md`](../../../../apps/mobile/CONTEXT.md) — **Inline Unit**, **Promotion**
**Status:** proposed — awaiting owner sign-off on **D1** (`create_session` inline-item schema),
**D2** (`promote_unit` tool shape + not-found), **D4** (`list_units` inline-exclusion filter — a
correctness gate, not a preference) and **D6** (coaching-guide wording). D3 and D5 are
recommendations, not gates.

## Objective

Expose Inline Units (the ownership model landed in Stage 4) on the MCP tool surface so an AI
planning conversation can (a) mint a one-off drill *inside* a session instead of cluttering the
library, and (b) promote such a drill to the library when the player later asks to keep it. Three
tool changes plus one new tool:

- `create_session` items accept `practice_unit_id` **or** an embedded `inline_unit` definition
  (the `create_unit` input shape) — the server mints the unit scoped to the session atomically,
  via the Stage 4 `save_practice_session` v4 RPC.
- `promote_unit(id)` — a new explicit-verb tool that detaches ownership (sets
  `scoped_to_session_id` null), user-initiated only.
- `list_sessions` items gain an `inline: boolean` marker so the model can find a promotable
  inline unit through the session it lives in.
- `list_units` gains a `scoped_to_session_id is null` filter so inline units never leak into the
  library listing (see D4 — this is the one behaviour that is *not* free from Stage 4).

Tool descriptions and the coaching guide teach the two behaviours the model must get right:
inline-by-default for session-specific drills, and promotion only on an explicit player ask.

This stage runs **concurrently with Stage 5** (app UI) — different apps, zero file overlap (epic
parallelism table). It depends on Stage 4 (schema + RPC) being merged. It is **sequential after
Stage 3** — both edit `coaching-guide.md`, `server.ts`, `list-sessions.ts`, `.agents/instructions/
mcp.md`, and the regression script, so Stage 6 rebases on Stage 3's merge and continues the
version bump (see Dependencies).

Covers: the `create_session` inline-item schema + validation; `promote_unit` tool + `UNIT_NOT_FOUND`
reuse; the `list_units` inline filter; the `inline` marker on `list_sessions` items; coaching-guide
"inline units & promotion" guidance and a `methodology_version` bump; unit tests and regression
additions. **No `apps/mobile` and no `supabase/` changes** — the RPC and column are Stage 4's.

## Dependencies

- **Upstream: Stage 4 merged** — `practice_units.scoped_to_session_id`, `save_practice_session`
  v4 (inline mint + orphan GC), and the cascade FK must exist. `create_session` builds the
  `inline_unit` payload the v4 RPC consumes; `promote_unit` and `list_units` read/write the
  column directly via PostgREST (MCP does not go through `PracticeLibrary`).
- **Upstream: Stage 3 merged** (sequential 3 → 6, epic table) — Stage 3 already reworked
  `list-sessions.ts`, `server.ts`, `coaching-guide.md`, `tool-errors.ts`, and `regression.ts`.
  Rebase on Stage 3 so this stage's edits stack cleanly (e.g. the `list_sessions` `inline` marker
  sits alongside Stage 3's `archived` marker; the coaching-guide version bumps from Stage 3's
  `2.4.0`, not `2.3.0`).
- Not blocked by Stage 5. Can draft as soon as this plan is approved (pipelining rule); implement
  once Stages 3 and 4 are merged.
- Existing objects read or modified: `create-session.ts` (+ test), `list-units.ts` (+ test),
  `list-sessions.ts` (+ test), `server.ts`, `coaching-guide.md`, `scripts/regression.ts`,
  `.agents/instructions/mcp.md`. `UNIT_NOT_FOUND` already exists in `ErrorCodes` (no new code).
  RLS on `practice_units` unchanged (owner-scoped column write, existing policy covers it).

## Decisions (owner review)

### D1 — `create_session` inline-item schema: `practice_unit_id` *or* `inline_unit` per item _(epic-flagged: MCP tool shape)_

**Recommendation: each `items` element carries either `practice_unit_id` (reference an existing
library unit) _or_ an `inline_unit` object (mint a new unit owned by this session), exactly one of
the two.** The `inline_unit` shape is `create_unit`'s input verbatim, so the model already knows it
(design §8) and Stage 4 D1 already fixed the RPC-side contract. This stage is the tool-schema and
validation half of that contract.

Zod expression — make both keys optional and enforce exactly-one in the handler (a Zod union over
two object variants also works, but produces noisier client-facing schema errors than a
hand-written check with a field-scoped `VALIDATION_ERROR`, which matches the rest of this tool):

```jsonc
// items[] element — reference form (unchanged; every existing call)
{ "practice_unit_id": "…", "order": 1, "repeat_count": 1, "club_code": "…", "observation_types": […] }

// items[] element — inline form (new)
{
  "inline_unit": {
    "title": "…",
    "instructions": [ { "order": 1, "text": "…", "ball_count": 10, "club_code": "…" } ],
    "focus": "…", "notes": "…", "default_club_code": "…",
    "success_criterion": "…", "tag_codes": ["…"]
  },
  "order": 1, "repeat_count": 1, "observation_types": […]
}
```

**Handler changes** (all additive; reference-only payloads keep their exact current path):

1. **Exactly-one guard** — per item, reject if it has neither or both of `practice_unit_id` and
   `inline_unit` (`VALIDATION_ERROR`, field `items[idx]`).
2. **Reference validation** unchanged for `practice_unit_id` items — but the owned-units set it
   validates against must **exclude inline units** (D5), so an inline unit's id can't be smuggled
   in as a cross-session reference (design §6: inline units are never referenceable elsewhere).
3. **Inline-unit validation** reuses `create_unit`'s rules on the embedded definition: non-empty
   `title`; 1–10 `instructions` each with a positive-integer unique `order`, non-empty `text`,
   nonnegative-integer optional `ball_count`; every club code (`default_club_code` + per-instruction
   `club_code`) validated against the catalog; `tag_codes` resolved via `resolveTagCodes`. **Extract
   these into a shared `validateInlineUnit()` / `buildInlineUnitJsonb()` helper** so `create_unit`
   and `create_session` share one implementation rather than duplicating ~80 lines (see Likely
   files — refactor `create_unit` onto the same helper in this stage to keep them from drifting).
4. **`observation_types` success check** (existing) — for a `practice_unit_id` item the criterion
   comes from the pre-fetched unit (unchanged); for an `inline_unit` item it comes from the embedded
   `success_criterion`. Same friendly `VALIDATION_ERROR` when `success` is requested without a
   criterion.
5. **Override hygiene** (`sameAsBase` dropping club/notes/focus equal to the unit) applies only to
   `practice_unit_id` items — an inline unit *is* the base, so its item-level overrides have nothing
   to copy from. Per-item `club_code`/`focus_cue`/`notes` on an inline item are still honoured
   verbatim (they may legitimately differ from the inline unit's own values); simplest correct rule:
   skip `sameAsBase` when there is no referenced unit.
6. **Payload to the RPC** — build each `p_items` element as today for reference items; for inline
   items emit `{ inline_unit: <built jsonb>, order, repeat_count, observation_types? }` per Stage 4
   D1's shape. The client no longer needs to `crypto.randomUUID()` inline unit ids — the RPC mints
   them (Stage 4 step 3).

**Why not a union / discriminated `z.union`:** a hand-rolled exactly-one check yields the
field-scoped `VALIDATION_ERROR` the rest of `create_session` returns, and keeps the inline branch
validating with the *same* code path as `create_unit` (the shared helper) rather than a parallel
Zod schema that could drift from `create_unit`'s rules. Called here; flag if the owner prefers the
union for schema self-documentation.

### D2 — `promote_unit(id)`: PostgREST `scoped_to_session_id → null`, `UNIT_NOT_FOUND` on miss

**Recommendation: a new tool `promote_unit` that sets `scoped_to_session_id = null` on the row via
a PostgREST update, owner-scoped by RLS — no new SQL.** This mirrors Stage 3's `archive_session`
mechanism (direct update, re-read via `select`, structured return) and Stage 4's `promoteUnit`
(which is `setScopedSession(id, null)` on the KMP side). Promotion is content- and
identity-preserving — only the ownership pointer changes (design §7).

```ts
const { data, error } = await ctx.supabaseClient
  .from('practice_units')
  .update({ scoped_to_session_id: null })
  .eq('id', id)
  .select('id, title, scoped_to_session_id')
  .maybeSingle();
```

- **Input:** `unit_id: z.string()`, described as "the `id` of an inline unit, found via a
  session's items in `list_sessions` (items where `inline` is true)".
- **Success return:** `{ "unit": { "id": "…", "title": "…", "inline": false } }` — re-read from the
  update so the model can confirm the drill is now a library unit.
- **Not-found:** the update matches zero rows (missing id, or another user's row filtered by RLS)
  and resolves to `data: null` → return **`UNIT_NOT_FOUND`** (already in `ErrorCodes` — the natural
  code, reused; no new error needed, unlike Stage 3's `SESSION_NOT_FOUND`).
- **Already a library unit** (`scoped_to_session_id` already null): the update is a harmless no-op
  that still returns the row; return success with `inline: false`. **Idempotent, not an error** —
  promotion is one-way (design §7) and re-promoting a library unit is meaningless but safe; a
  friendly success beats a confusing error. (No demotion path exists, so this can never *re-scope*.)
- **DB error:** `DATABASE_ERROR`.

**One-way, user-initiated:** the tool description and coaching guide must state promotion is only
invoked when the player asks to keep/reuse a drill — never proactively (design §7, D6).

### D3 — `list_sessions` inline marker: an `inline` boolean per item

**Recommendation: every item in `list_sessions` output gains `inline: boolean`**, derived from the
referenced unit's `scoped_to_session_id != null`. This is how the model discovers a promotable
drill: it reads a session's items, sees which are inline, and calls `promote_unit` with that
`unit_id`. Symmetric with Stage 3's always-present `archived` boolean on sessions.

`list_sessions` already surfaces inline units incidentally — it fetches referenced units by id
(`practice_units … .in('id', unitIds)` at [`list-sessions.ts:100`](../../../../apps/mcp/src/tools/list-sessions.ts#L100)),
a path that is **not** filtered by scope, so an inline unit's title already appears. The only change
is to also `select('id, title, scoped_to_session_id')`, carry the flag through `unitMap`, and emit
`inline: session.<item>.unit.scoped_to_session_id != null` on each item object
([`list-sessions.ts:239`](../../../../apps/mcp/src/tools/list-sessions.ts#L239)). The raw
`scoped_to_session_id` stays server-side (informational noise the model doesn't need — the boolean
is the marker). Ball-count and tag logic untouched.

### D4 — `list_units` must add its own inline-exclusion filter _(correctness gate — not free from Stage 4)_

**This is the one place the epic bullet "`list_units` untouched — pure library" is misleading for
MCP, and it must not be missed.** Stage 4 D4 puts the library-exclusion filter at the **KMP
repository `list()` choke point** — but MCP's `list_units` does **not** go through the KMP
repository. It is a direct PostgREST query
([`list-units.ts:36`](../../../../apps/mcp/src/tools/list-units.ts#L36)):

```ts
.from('practice_units').select('id, title, notes, focus, default_club_code')
```

With no filter, once Stage 4 lands, **every inline unit would appear in `list_units`** — exactly
the library clutter this epic exists to prevent, re-introduced through the MCP surface. So Stage 6
**must** add `.is('scoped_to_session_id', null)` to that query. The design intent ("`list_units`
stays pure library", §8) is satisfied by *adding* this filter, not by leaving the code alone.

- Add `.is('scoped_to_session_id', null)` to the initial `practice_units` select in `list_units`.
- **`create_session`'s owned-units pre-fetch** ([`create-session.ts:153`](../../../../apps/mcp/src/tools/create-session.ts#L153))
  is a separate direct query used to validate `practice_unit_id` references — it must **also**
  exclude inline units so an inline unit can't be referenced as a library unit (D5). Same filter.
- No `include_inline` flag — inline units are reached only through their session's detail
  (design §8); there is no library caller that wants them.

This decision is a gate because omitting it silently defeats the feature; it is called out here
rather than buried in the file table so the owner and implementer both see it.

### D5 — reference validation excludes inline units (resolved, rides D4)

An `inline_unit` means *create new*; a `practice_unit_id` means *reference an existing **library**
unit*. Because D4 filters the owned-units pre-fetch to `scoped_to_session_id is null`, an inline
unit's id passed as a `practice_unit_id` falls out of the owned set and returns the existing
`UNIT_NOT_FOUND` ("unit … not found or does not belong to you"). That is the correct outcome
(design §6: inline units can never be referenced by another session) and needs no new code beyond
D4's filter. Noted so the behaviour is intentional, not accidental.

### D6 — Coaching-guide wording _(epic-flagged: shapes model behaviour)_

The guide must teach: **inline-by-default** for a one-off drill built to fill a slot in the session
being planned (use `inline_unit`), **`create_unit` + reference** only when the player wants a
reusable drill or one that already exists; and **promotion etiquette** — `promote_unit` is invoked
**only when the player asks to keep/reuse** an inline drill, never proactively. Draft wording in
[Coaching-guide changes](#coaching-guide-changes) below for async review — this is the surface that
changes what the model *does*.

## Likely files

### New

| File | Purpose |
| --- | --- |
| `apps/mcp/src/tools/promote-unit.ts` | Registers `promote_unit` (PostgREST `scoped_to_session_id → null`, re-read, structured return) |
| `apps/mcp/src/tests/promote-unit.test.ts` | Unit tests: promote inline → library, idempotent on library unit, not-found, DB error |
| `apps/mcp/src/validation/inline-units.ts` | Shared `validateInlineUnit()` + `buildInlineUnitJsonb()` extracted from `create_unit` (D1.3) — the single implementation `create_unit` and `create_session` both use |

### Modified

| File | Change |
| --- | --- |
| `apps/mcp/src/tools/create-session.ts` | Accept `inline_unit` per item (D1); exactly-one guard; exclude inline units from the reference pre-fetch (D4/D5); inline `success_criterion` source for the observation check; build the `inline_unit` RPC payload; extend description |
| `apps/mcp/src/tools/create-unit.ts` | Refactor its instruction/club/tag validation and jsonb build onto the shared `inline-units.ts` helper (behaviour-preserving) so the two tools can't drift |
| `apps/mcp/src/tools/list-units.ts` | Add `.is('scoped_to_session_id', null)` to the initial select (D4) |
| `apps/mcp/src/tools/list-sessions.ts` | `select('… , scoped_to_session_id')` on the units fetch; carry the flag through `unitMap`; add `inline` boolean per item (D3) |
| `apps/mcp/src/server.ts` | Import + register `promote_unit` |
| `apps/mcp/src/tests/create-session.test.ts` | New cases: inline-item mint payload, exactly-one guard, inline `success` criterion, inline-id-as-reference → `UNIT_NOT_FOUND` |
| `apps/mcp/src/tests/create-unit.test.ts` | Re-run against the refactored helper (assert unchanged behaviour) |
| `apps/mcp/src/tests/list-units.test.ts` | Assert inline units are excluded (mock a scoped row; confirm the `is('scoped_to_session_id', null)` filter / its absence from output) |
| `apps/mcp/src/tests/list-sessions.test.ts` | Assert items carry `inline: true`/`false` per the referenced unit's scope |
| `apps/mcp/methodology/coaching-guide.md` | "Inline units & promotion" guidance; runbook + data-format notes; bump `methodology_version` (from Stage 3's `2.4.0` → `2.5.0`) |
| `apps/mcp/scripts/regression.ts` | Add `promote_unit` to the `tools/list` expectation; exercise create-with-inline → find via `list_sessions` (`inline: true`) → `promote_unit` → appears in `list_units`, session still references it |
| `.agents/instructions/mcp.md` | Add `promote_unit` + `inline-units.ts` to the file map and Tools reference; note `list_units` excludes inline units and `create_session` accepts `inline_unit` |

### Referenced (not modified)

| File | Purpose |
| --- | --- |
| `apps/mcp/src/tools/create-unit.ts` | The `inline_unit` shape (its input schema) + the validation being extracted |
| `apps/mcp/src/tools/archive-session.ts` | Stage 3's explicit-verb mutation-tool pattern (update + re-read + structured return) `promote_unit` mirrors |
| `supabase/migrations/20260713130000_inline_units.sql` | Stage 4's `scoped_to_session_id` column + `save_practice_session` v4 the tools read/write and call |

## Tool details

### `create_session` — inline items

- Input schema (see D1): each `items` element gains an optional `inline_unit` object (the
  `create_unit` input shape) and `practice_unit_id` becomes optional; the handler enforces
  exactly-one. `order`, `repeat_count`, `club_code`, `focus_cue`, `notes`, `observation_types`
  unchanged and apply to both forms.
- Description addition (draft): "Each item is either a reference to an existing library unit
  (`practice_unit_id` from `list_units`/`create_unit`) **or** an `inline_unit` — a one-off drill
  created and owned by this session, never added to the library. Use `inline_unit` for a drill you
  built just to fill a slot in this session; use `practice_unit_id` for a reusable drill the player
  will want again. An `inline_unit` uses the same shape as `create_unit`'s input."
- Returns `{ session_id }` unchanged (the minted inline unit ids are internal; the model reaches
  them later through `list_sessions`).

### `promote_unit(id)`

- Input `unit_id: z.string()`; success `{ unit: { id, title, inline: false } }`; `UNIT_NOT_FOUND`
  on zero-row update; idempotent on an already-library unit; `DATABASE_ERROR` on failure (D2).
- Description (draft, tighten in review): "Promote an inline unit to a reusable library unit:
  detach it from its owning session so it appears in `list_units` and can be reused in other
  sessions. The session that contained it keeps using it, unchanged. One-way — there is no demote.
  **Only call this when the player asks to keep or reuse a specific drill** (e.g. 'save that
  Tuesday drill', 'I want to reuse the gate drill'). Find the unit's `id` in the `items` of
  `list_sessions` where `inline` is true."

### `list_sessions` / `list_units` changes

- `list_sessions`: add `inline` boolean per item (D3); everything else identical.
- `list_units`: add the `scoped_to_session_id is null` filter (D4); output shape unchanged (inline
  units simply never appear).

## Coaching-guide changes

Draft for D6 review. Edits to `methodology/coaching-guide.md` (rebased on Stage 3's version):

1. **Bump `methodology_version`** `2.4.0` → `2.5.0` (new tool + inline/promotion guidance). If
   Stage 3 has not merged when this implements, reconcile to one bump above whatever Stage 3
   shipped — the regression/version check keys off this value.

2. **New subsection under §3 (Design principles)** — draft:

   > ### Inline drills vs library drills
   >
   > When you build a drill purely to fill a slot in the session you're planning right now, create
   > it **inline**: pass an `inline_unit` on the `create_session` item instead of calling
   > `create_unit`. An inline drill is owned by that one session, never enters the library, and is
   > exactly what the player wants for a one-off — it keeps the library clean.
   >
   > - **Default to inline for session-specific, one-off drills.** Reach for `create_unit` (a
   >   library drill) only when the player wants something reusable, or when you're reusing a drill
   >   that already exists (`list_units`).
   > - An `inline_unit` takes the same fields as `create_unit`: `title`, `instructions`, optional
   >   `focus`, `notes`, `default_club_code`, `success_criterion`, `tag_codes`.
   > - Inline drills don't show up in `list_units`. You'll see them inside their session's items in
   >   `list_sessions` (each item has an `inline` flag).
   >
   > ### Promoting a drill (keeping it)
   >
   > If the player later asks to **keep or reuse** an inline drill ("save that Tuesday drill", "use
   > the gate drill again today"), call `promote_unit` with the unit's `id` (from the session's
   > items in `list_sessions`, where `inline` is true). Promotion moves the drill into the library;
   > the session it came from keeps using it, unchanged.
   >
   > - **Only promote when the player asks.** Never promote a drill on your own initiative — inline
   >   is the right home for a one-off.
   > - Promotion is one-way; there is no demote.

3. **Tool runbook note** — in §4 step 6/7, add: prefer an `inline_unit` on the `create_session`
   item for a new one-off drill (skip `create_unit`); use `create_unit` only for reusable/existing
   drills. In §4 step 3, note `list_units` shows library drills only; inline drills appear in
   `list_sessions` items.

4. **Data-format rules** — add: a `create_session` item takes `practice_unit_id` **or**
   `inline_unit` (same shape as `create_unit`), not both; `promote_unit(unit_id)` moves an inline
   drill into the library, player-initiated only.

## Test plan

### `promote-unit.test.ts` (mirror `archive-session.test.ts` structure)

- `promote_unit detaches scope and returns inline: false` — mock `from('practice_units').update()
  .eq().select().maybeSingle()` returning a row with `scoped_to_session_id: null`; assert the
  update payload is `{ scoped_to_session_id: null }` and the response is `{ unit: { inline: false } }`.
- `promote_unit is idempotent on an already-library unit` — mock returns a row already null;
  success, `inline: false`, not an error.
- `promote_unit returns UNIT_NOT_FOUND when the row is missing` — `maybeSingle` resolves
  `{ data: null, error: null }`; assert `isError` + `code: UNIT_NOT_FOUND`.
- `promote_unit returns DATABASE_ERROR on a DB failure`.

### `create-session.test.ts` additions

- `inline item builds an inline_unit RPC payload` — one item with `inline_unit`, one with
  `practice_unit_id`; assert the `p_items` element for the inline one carries `inline_unit` (built
  jsonb) and no `practice_unit_id`, and the reference one is unchanged.
- `rejects an item with neither practice_unit_id nor inline_unit` → `VALIDATION_ERROR`, field
  `items[idx]`.
- `rejects an item with both` → `VALIDATION_ERROR`.
- `inline success observation reads the embedded success_criterion` — inline item with
  `observation_types: ["success"]` and a `success_criterion` succeeds; the same without a criterion
  → the friendly `VALIDATION_ERROR`.
- `an inline unit's id used as a practice_unit_id reference is rejected` → `UNIT_NOT_FOUND` (D4/D5;
  the pre-fetch excludes scoped rows, so the id isn't in the owned set).
- Existing reference-only cases unchanged (the inline branch is additive).

### `create-unit.test.ts`

- Re-run unchanged against the extracted helper — behaviour-preserving refactor gate (no new
  assertions unless a gap surfaces).

### `list-units.test.ts` additions

- `excludes inline units` — mock the query builder; assert `.is('scoped_to_session_id', null)` is
  applied (spy on `.is`), or a scoped row supplied by the mock is absent from output.

### `list-sessions.test.ts` additions

- `items carry an inline flag` — mixed mock where one referenced unit has a non-null
  `scoped_to_session_id`; assert that item is `inline: true` and a library-referencing item is
  `inline: false`.

### Regression additions (`scripts/regression.ts`)

- Add `promote_unit` to the expected `tools/list` names.
- New section after `create_session`: `create_session` with one `inline_unit` item; `list_sessions`
  shows that session's item with `inline: true` and the inline unit does **not** appear in
  `list_units`; `promote_unit` the inline unit's id; assert it now appears in `list_units` and the
  session still references it (item `inline: false`). Leaves the promoted unit in the library
  (acceptable end state, or note it for the test-data cleanup pass).

## Validation checklist

- [ ] `pnpm --filter @rangework/mcp test` green — new `promote-unit.test.ts`, extended
      `create-session` / `list-units` / `list-sessions` tests, and unchanged `create-unit` tests
- [ ] `pnpm --filter @rangework/mcp typecheck` clean
- [ ] `pnpm --filter @rangework/mcp lint` clean
- [ ] `tools/list` exposes `promote_unit`
- [ ] Regression (live Worker, Stage 4 migration applied): create-with-inline → `list_sessions`
      item `inline: true` → inline unit absent from `list_units` → `promote_unit` → present in
      `list_units`, session still references it
- [ ] `create_session` with a mixed payload (one `practice_unit_id`, one `inline_unit`) creates the
      session and mints the inline unit scoped to it (Stage 4 RPC does the mint); a plain
      reference-only payload behaves byte-identically to before
- [ ] `get_coaching_guide` returns the bumped `methodology_version` and the inline/promotion guidance
- [ ] Manual conversation smoke: model uses `inline_unit` for a one-off drill (not `create_unit`);
      does **not** promote unprompted; promotes only on an explicit "keep that drill" ask

## Regression risks

| Risk | Likelihood | Mitigation |
| --- | --- | --- |
| `list_units` leaks inline units into the library (filter omitted) — silently defeats the epic | **Medium** | D4 is a gate: explicit `.is('scoped_to_session_id', null)` on `list_units` **and** the `create_session` reference pre-fetch; `list-units.test.ts` asserts exclusion; regression confirms the inline unit is absent pre-promotion |
| `create_session` inline branch regresses reference-only saves (the tool is validation-heavy) | Low | Inline handling is additive and skipped when no `inline_unit` present; existing reference-only tests stay green unchanged; extract-and-reuse keeps `create_unit`'s validated rules |
| Extracted `inline-units.ts` helper drifts from `create_unit`'s original behaviour during refactor | Low | `create-unit.test.ts` re-run unchanged as the behaviour-preserving gate; refactor is mechanical (move, don't rewrite) |
| An inline unit's id smuggled in as a `practice_unit_id` reference (cross-session reuse) | Low | D5: reference pre-fetch excludes scoped rows → existing `UNIT_NOT_FOUND`; explicit test |
| Model promotes proactively / treats inline as a lesser drill | Medium | D6 guide wording ("only when the player asks", "inline is the right home for a one-off"); tool description restates it; manual conversation smoke |
| Model over-uses `create_unit`, defeating the declutter goal | Medium | D6 "default to inline for one-off drills"; runbook step nudges `inline_unit` first; this is guidance-shaped, tune after field use |
| `methodology_version` not bumped, cached guide served | Low | Checklist confirms via `get_coaching_guide`; version bump is part of the guide edit; reconcile with Stage 3's version on rebase |
| `list_sessions` `inline` marker collides with Stage 3's `archived` marker on rebase | Low | Both are additive per-object booleans on different levels (session vs item); rebase-on-Stage-3 dependency called out; tests cover both |

## On merge

Record outcomes in `changes.md` in this stage folder (per the operating model). This is the
epic's **ship point 2** trailing stage together with Stage 5 — after both merge, run the epic's
end-to-end walkthrough #2 (plan via MCP with an inline unit → run → conversationally promote the
drill) and perform the epic close (update `design-decisions.md` status, confirm
`apps/mobile/CONTEXT.md` vocabulary, file §10 deferred-item issues, update project memory). Confirm
`.agents/instructions/mcp.md` reflects `promote_unit`, the `inline_unit` option on `create_session`,
and the `list_units` library-only guarantee.
