# An Inline Unit is a `practice_units` row owned by a session via a nullable reference, with cascade delete

An **Inline Unit** — a Practice Unit minted during AI planning purely to fill a slot in the
session being planned — is stored as an ordinary `public.practice_units` row carrying a nullable
`scoped_to_session_id` owning reference. Null means library citizen (every unit before this
epic); a non-null value means the row is session content owned by that one session. The owning
reference is `references practice_sessions (id) on delete cascade`, so an inline unit's life is
tied to its session: it dies when the session is deleted. The unit library excludes inline units
at a single repository choke point (`list()` filters `scoped_to_session_id is null`); `get(id)`
stays unfiltered so a session can load its inline units by id. **Promotion** — the one escape
hatch — is `set scoped_to_session_id = null`, detaching ownership while leaving content and
identity unchanged. Duplication is a deep copy: each inline unit is copied and owned by the new
session, so repeated duplication yields independent near-identical inline units by design (no
dedup). MCP `create_session` is the only creation path this epic (app-side inline creation is
deferred).

This is the deliberately surprising part: a row in `practice_units` that is invisible to the
library and vanishes with its session, unlike every other unit which is a durable, freely
referenceable library citizen.

## Status

accepted

## Considered options

- **Ownership via nullable session reference + cascade delete (chosen).** Inline units reuse all
  existing unit machinery (instructions, tags, the editor, the save RPC), can never orphan (the
  cascade guarantees it), and have a clean one-way escape hatch (Promotion). The item→unit
  `on delete restrict` FK forces a delete-ordering guarantee, handled by a before-delete trigger
  that removes the session's items first.
- **A "hidden from library" boolean with an independent lifecycle (rejected).** Recreates the
  clutter invisibly: hidden units accumulate forever with no cleanup story, and nothing ties a
  one-off unit's death to the session that spawned it (design §6).
- **A separate `inline_units` table (rejected).** Loses the shared instructions/tags/editor/save
  machinery — the model would have to learn two shapes and the app two editors — for no benefit
  over a scoped reference (design §6).

## Consequences

- **Cascade delete means a unit the user thought was "theirs" vanishes with the session.**
  Promotion is the designed mitigation — anything worth keeping has a deliberate way out first
  (design §7). In-app delete copy should say inline units go with the session.
- **The protective item→unit `restrict` FK forces a delete-ordering guarantee.** Deleting a
  session fires both the item cascade and the inline-unit cascade; a before-delete trigger on
  `practice_sessions` removes the items first so the inline-unit cascade cannot trip the
  restrict. The FK is not relaxed — it guards shared library units from deletion.
- **Duplication is deep copy**, so duplicating a one-off five times yields five independent
  inline units, each dying with its session. Accepted, not deduplicated — repeated duplication of
  a drill is precisely the signal to promote it (design §6).
- **The save RPC gains a mint-and-GC contract** MCP and duplication both depend on: an item's
  optional `inline_unit` object mints an owner-scoped unit atomically, and an orphan GC reaps
  scoped units a save dropped. This is hard to reverse once MCP embeds inline definitions.
- **The library-exclusion filter lives at one repository choke point**, so no caller can leak an
  inline unit into the library; `get(id)` stays unfiltered so session detail, promotion, and
  duplication keep working.
