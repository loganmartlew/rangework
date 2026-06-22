# Stage 3 — MCP Tool Contracts

> **Epic:** [RWK-4 — AI Session Creation](https://loganmartlew.atlassian.net/browse/RWK-4)
> **Stage 3 ticket:** [RWK-31 — MCP tools (read + write)](https://loganmartlew.atlassian.net/browse/RWK-31)
> **Status:** Implementation complete — manual MCP Inspector gate pending

This document records the final input/output schemas for all five Stage 3 MCP tools, as implemented in `apps/mcp/src/tools/`. These are the source-of-truth contracts for Stage 4+ work.

---

## Common conventions

- All key names are **snake_case** (matches PostgREST wire format).
- `order` (not `sort_order`) is used in all tool I/O — matches the RPC JSONB key.
- Timestamps (`created_at`, `updated_at`) are **omitted** from all tool output.
- All tool errors use `isError: true` on the MCP content block with a structured JSON body:

```ts
{
  code: string      // "VALIDATION_ERROR" | "UNKNOWN_CLUB_CODE" | "UNIT_NOT_FOUND" | "DATABASE_ERROR"
  message: string   // human-readable, LLM-facing
  data?: {
    field?: string
    valid_codes?: string[]
    invalid_unit_ids?: string[]
  }
}
```

---

## `get_user_clubs`

**Description:** Returns the clubs currently enabled in the user's bag. Call this at the start of a planning session to learn which clubs are available. Use the `code` field (not `display_name`) in all subsequent tool calls that accept a club reference.

### Input

No parameters.

### Output

```ts
{
  clubs: Array<{
    code: string         // catalog primary key, e.g. "seven_iron"
    display_name: string // human-readable, e.g. "7 Iron"
    category: string     // raw enum: "IRON" | "WOOD" | "WEDGE" | "PUTTER" | "HYBRID" | etc.
  }>
}
```

Ordered by `clubs.sort_order ASC` (driver → putter progression). Returns `{ clubs: [] }` when the user has no enabled clubs.

### Data source

`user_enabled_clubs` joined to `clubs`, scoped by RLS to the authenticated user. `sort_order` is excluded from output (internal ordering field only).

---

## `list_units`

**Description:** Returns all of the user's practice units, including full instruction text, ball counts, club assignment, and coaching notes. Call this before creating new units to avoid duplication and to find units that can be reused in a new session. If `has_uncounted_instructions` is true, some instructions have no ball count — treat `total_ball_count` as a partial estimate.

### Input

No parameters.

### Output

```ts
{
  units: Array<{
    id: string
    title: string
    notes: string | null
    focus: string | null
    default_club_reference: string | null   // catalog code
    instruction_count: number               // total instruction count including uncounted
    total_ball_count: number | null         // null if any instruction has no ball_count
    has_uncounted_instructions: boolean
    instructions: Array<{
      order: number          // 1-based, unique within unit (mapped from sort_order)
      text: string
      ball_count: number | null
    }>
  }>
}
```

Ordered by `updated_at DESC`. Returns `{ units: [] }` when the user has no units.

### Ball count computation

- `instruction_count` = `COUNT(*)` of all instructions (including those with null `ball_count`)
- `has_uncounted_instructions` = `instructions.some(i => i.ball_count == null)`
- `total_ball_count` = `has_uncounted_instructions ? null : sum(ball_counts)`

### Data source

Two queries joined in-memory: `practice_units` (ordered `updated_at DESC`) + `practice_unit_instructions` (ordered `sort_order ASC`), scoped by RLS.

---

## `list_sessions`

**Description:** Returns all of the user's practice sessions, including their item lineup, club overrides, repeat counts, and coaching notes. Call this to understand how the user's existing sessions are structured before creating a new one. If `has_uncounted_items` is true, one or more units in the session have no ball count on some instructions — treat the total as a partial estimate.

### Input

No parameters.

### Output

```ts
{
  sessions: Array<{
    id: string
    name: string
    notes: string | null
    total_ball_count: number | null  // null if any referenced unit has uncounted instructions
    has_uncounted_items: boolean
    items: Array<{
      order: number           // 1-based, unique within session (mapped from sort_order)
      unit_id: string         // UUID
      unit_title: string      // joined from practice_units
      repeat_count: number
      club_reference: string | null
      notes: string | null
      focus_cue: string | null
    }>
  }>
}
```

Ordered by `updated_at DESC`. Returns `{ sessions: [] }` when the user has no sessions.

### Ball count computation

- `has_uncounted_items` = any referenced unit has `has_uncounted_instructions = true`
- `total_ball_count` = `has_uncounted_items ? null : sum(item.repeat_count × unit.total_ball_count)`

### Data source

Three queries joined in-memory: `practice_sessions` + `practice_session_items` + `practice_units` (for title and ball counts), all scoped by RLS.

---

## `create_unit`

**Description:** Creates a new practice unit (a single drill) in the user's account. A unit has a title, one to ten step-by-step instructions (each with optional ball count), optional coaching focus, and an optional default club. Returns the new unit's id — save this to use in `create_session`. Club references must use the `code` field from `get_user_clubs`, not the display name.

### Input

```ts
{
  title: string                    // required; non-empty after trim
  instructions: Array<{            // required; 1–10 items
    order: number                  // required; positive integer, unique within array
    text: string                   // required; non-empty after trim
    ball_count?: number            // optional; positive integer if provided
  }>
  focus?: string                   // optional; coaching cue or swing thought
  notes?: string                   // optional; context or reminders
  default_club_reference?: string  // optional; must exist in clubs catalog
}
```

### Output

```ts
{ unit_id: string }  // UUID of the newly created unit
```

### Validation errors

| Condition | `code` | `message` | `data` |
|---|---|---|---|
| `title` empty after trim | `VALIDATION_ERROR` | "title must not be empty" | `{ field: "title" }` |
| `instructions` empty | `VALIDATION_ERROR` | "at least one instruction is required" | `{ field: "instructions" }` |
| `instructions` > 10 | `VALIDATION_ERROR` | "a unit may have at most 10 instructions" | `{ field: "instructions" }` |
| Duplicate `order` values | `VALIDATION_ERROR` | "instruction order values must be unique" | `{ field: "instructions" }` |
| `instructions[n].text` empty after trim | `VALIDATION_ERROR` | "instruction text must not be empty" | `{ field: "instructions[n].text" }` |
| Unknown `default_club_reference` | `UNKNOWN_CLUB_CODE` | "Unknown club code: \<code\>" | `{ field: "default_club_reference", valid_codes: [...] }` |
| RPC FK violation (club code) | `UNKNOWN_CLUB_CODE` | "Unknown club code: \<code\>" | `{ field: "default_club_reference", valid_codes: [...] }` |
| Other RPC error | `DATABASE_ERROR` | "Failed to create unit. Please try again." | — |

### Data path

1. Validate input (Zod + manual trim/uniqueness checks).
2. Validate `default_club_reference` against full `clubs` catalog if provided.
3. Generate `unit_id` via `crypto.randomUUID()`.
4. Call `save_practice_unit(p_unit_id, p_title, p_notes, p_focus, p_default_club_reference, p_instructions)`.
   - `ball_count` key is **omitted** (not set to null) in the JSONB when not provided.
5. Return `{ unit_id }`.

> **Idempotency:** each call generates a fresh UUID — retries create separate records.

---

## `create_session`

**Description:** Creates a new practice session in the user's account. A session is an ordered list of practice units with optional per-item club overrides, coaching cues, and repeat counts. Call `list_units` or `create_unit` first to get unit ids. Returns the new session's id. Each item's `practice_unit_id` must be a unit that belongs to this user.

### Input

```ts
{
  name: string              // required; non-empty after trim
  items: Array<{            // required; 1+ items
    practice_unit_id: string  // required; UUID of a unit owned by this user
    order: number             // required; positive integer, unique within array
    repeat_count: number      // required; positive integer
    club_reference?: string   // optional; must exist in clubs catalog
    focus_cue?: string        // optional; per-item coaching cue
    notes?: string            // optional; per-item reminder
  }>
  notes?: string            // optional; session-level notes
}
```

### Output

```ts
{ session_id: string }  // UUID of the newly created session
```

### Validation errors

| Condition | `code` | `message` | `data` |
|---|---|---|---|
| `name` empty after trim | `VALIDATION_ERROR` | "name must not be empty" | `{ field: "name" }` |
| `items` empty | `VALIDATION_ERROR` | "at least one item is required" | `{ field: "items" }` |
| Duplicate `order` values | `VALIDATION_ERROR` | "item order values must be unique" | `{ field: "items" }` |
| Unknown `practice_unit_id` | `UNIT_NOT_FOUND` | "unit \<id\> not found or does not belong to you" | `{ invalid_unit_ids: [...] }` |
| Unknown `club_reference` | `UNKNOWN_CLUB_CODE` | "Unknown club code: \<code\>" | `{ field: "items[n].club_reference", valid_codes: [...] }` |
| RPC error | `DATABASE_ERROR` | "Failed to create session. Please try again." | — |

### Data path

1. Validate input (Zod + manual trim/uniqueness checks).
2. Pre-fetch user's unit ids from `practice_units` (RLS scopes to owner).
3. Validate all `practice_unit_id` values against the fetched set.
4. Validate all `club_reference` values against full `clubs` catalog if any are provided.
5. Generate `session_id` via `crypto.randomUUID()`.
6. Call `save_practice_session(p_session_id, p_name, p_notes, p_items)`.
   - Optional item fields (`club_reference`, `notes`, `focus_cue`) are **omitted** (not set to null) in the JSONB when not provided.
7. Return `{ session_id }`.

> **Idempotency:** each call generates a fresh UUID — retries create separate records.

---

## Shared validation helper: `club-codes.ts`

### `fetchAllClubCodes(supabaseClient): Promise<string[]>`

Fetches all club codes from the `clubs` catalog ordered by `sort_order ASC`. Called once per tool invocation that needs club validation. No per-Worker caching (deferred to Stage 4+).

### `validateClubCode(code, allCodes, field, userEnabledCodes?): CallToolResult | null`

Returns `null` if `code` is in `allCodes`. Returns an `UNKNOWN_CLUB_CODE` error result otherwise. `valid_codes` in the error `data` is set to `userEnabledCodes` if provided, otherwise `allCodes`.

---

## Shared error factory: `tool-errors.ts`

### `toolError(code, message, data?): CallToolResult`

Returns `{ content: [{ type: 'text', text: JSON.stringify({ code, message, data? }) }], isError: true }`.

### `ErrorCodes`

```ts
{
  VALIDATION_ERROR: 'VALIDATION_ERROR',
  UNKNOWN_CLUB_CODE: 'UNKNOWN_CLUB_CODE',
  UNIT_NOT_FOUND: 'UNIT_NOT_FOUND',
  DATABASE_ERROR: 'DATABASE_ERROR',
}
```
