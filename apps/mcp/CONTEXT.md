# Coaching

The AI coaching conversation protocol. Defines vocabulary specific to the MCP server and the planning dialogue — the language of coaching sessions, not of the data model. All planning-layer terms (Practice Unit, Practice Session, Session Item, Club Code, etc.) carry the same meaning as defined in [Planning & Execution](../mobile/CONTEXT.md).

## Language

**Player**:
The Rangework user as seen from the coaching context — the person receiving coaching from the AI. Use "player" in coaching dialogue and in coaching-layer code; use "user" when talking about authentication or data ownership.
_Avoid_: user (in coaching dialogue), golfer, client

**Drill**:
A player-facing synonym for Practice Unit, used in coaching dialogue to describe an individual practice exercise. "Drill" is appropriate when talking with the player; "Practice Unit" is the canonical term in code and data.
_Avoid_: exercise, activity

**Coaching Guide**:
The methodology document that defines how an AI coach should structure planning conversations. Stored in R2, versioned, and loaded per-request. The Coaching Guide is the source of truth for coaching behaviour; methodology must not be hardcoded in TypeScript.
_Avoid_: system prompt, methodology document, instructions

**Ball Budget**:
The number of balls a player has available for a session. Gathered via conversation; never stored. Determines how the session's volume is distributed across Session Items.
_Avoid_: ball limit, ball allowance, ball count (reserved for per-instruction counts)

**Facility**:
The physical practice location and the areas available to the player (driving range, short-game area, putting green, or combinations thereof). Gathered via conversation; never stored. Determines which types of drills are possible.
_Avoid_: course, location, venue

**Range Session (coaching view)**:
A completed execution of a Practice Session on the range, surfaced to the coach read-only via `list_range_sessions` / `get_range_session`. Only **Completed** sessions are visible — Active and Abandoned ones never appear (invisibility, not filtering). Observation counts are always reported against their own denominator ("11 of 18 observed"): an observed-ball count is never phrased as a rate of all balls hit. Values are stored from a canonical (right-handed) perspective and passed through opaquely.
_Avoid_: range session log, practice history, results (for the count itself)

**User Context**:
The authenticated user identity and Supabase client passed to every MCP tool handler at request time. Established by JWT validation before any tool executes.
_Avoid_: session context, request context, auth context
