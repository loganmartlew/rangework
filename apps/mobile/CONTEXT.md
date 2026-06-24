# Planning & Execution

The core golf practice planning domain. Defines the three-level hierarchy (Practice Unit → Practice Session → Range Session) and the vocabulary used across the Android app, KMP shared module, and Supabase schema.

## Language

### Planning layer

**Practice Unit**:
A reusable, self-contained drill. Owns an ordered list of Practice Instructions, optional notes, a Focus Cue, and an optional default Club.
_Avoid_: drill, exercise, activity

**Practice Instruction**:
A single ordered directive within a Practice Unit, with descriptive text and an optional Ball Count.
_Avoid_: step (reserved for execution Steps), task

**Practice Session**:
A reusable template that assembles Practice Units into a complete practice plan. Contains an ordered list of Session Items.
_Avoid_: session template, workout, program, plan

**Session Item**:
A slot in a Practice Session that references a Practice Unit and carries session-level overrides: Repeat Count, Club, notes, and Focus Cue.
_Avoid_: session unit, practice item

**Repeat Count**:
The number of times a Practice Unit's full instruction set is executed within a Session Item. Multiplies the Unit's total Ball Count to give the Item's contribution to session volume.
_Avoid_: reps, repetitions

**Focus Cue**:
A mental focus directive — the specific thing to concentrate on during a Practice Unit. Defined on the Practice Unit (stored as `focus`) and optionally overridden per Session Item (stored as `focus_cue`). The Session Item's value takes precedence during execution.
_Avoid_: focus, mental note, tip

**Ball Count**:
The number of balls expected for a single Practice Instruction. Summed across all instructions to give a Practice Unit's total; multiplied by Repeat Count to give a Session Item's total.
_Avoid_: shot count, ball total (for the per-instruction value)

### Execution layer

**Range Session**:
A live execution record created when a golfer starts a Practice Session. Contains a Snapshot of the session content and tracks which Steps have been completed. A Range Session is Active until it is either Completed or Abandoned.
_Avoid_: execution, run

**Snapshot**:
An immutable capture of a Practice Session's content taken at the moment a Range Session starts. Because the Snapshot is self-contained, edits to the source Practice Session have no effect on any in-progress or historical Range Session.
_Avoid_: frozen session, copy, record

**Step**:
An atomic unit of work inside a Range Session. Derived by expanding each Practice Instruction across its Repeat Count — one Step equals one instruction × one repetition of one Session Item. The Snapshot holds the flat ordered list of all Steps.
_Avoid_: rep (ambiguous), task, instruction (reserved for the planning layer)

**Completed Step**:
A record that a Step has been marked done, with a completion timestamp.

**Time Entry**:
A span of time during which the user was actively at the range within a single Range Session. Multiple Time Entries per Range Session accumulate to compute total time on range.
_Avoid_: session timer, time block, time segment

### Range Session lifecycle

A Range Session is in exactly one of three states:

- **Active** — started, not yet finished; the user is working through Steps.
- **Completed** — the user explicitly finished the session.
- **Abandoned** — the user discarded the session without completing it.

### Club management

**Club**:
A specific type of golf club in the system catalog, identified by a stable Club Code. Has a display name and category (Wood, Hybrid, Iron, Wedge, Putter).
_Avoid_: club type, equipment

**Club Code**:
The stable, lowercase-underscore identifier for a Club (e.g. `seven_iron`, `pitching_wedge`). All club assignments in practice data use Club Codes, not display names. Existing database columns named `club_reference` / `default_club_reference` are a misnomer and should be renamed to `club_code` / `default_club_code`.
_Avoid_: club reference, club key, club id

**Club Bag**:
The set of Clubs a user has enabled from the catalog. Only clubs in a user's Bag appear in UI selectors and are valid targets for club assignments.
_Avoid_: equipped clubs, my clubs

### Derived values

**Estimated Duration**:
A derived estimate of how long a Practice Session will take, computed from session total Ball Count at 15 seconds per ball.

### User

**User Preferences**:
A user's measurement settings: unit system (Imperial / Metric / Custom), distance unit (Yards / Meters), and speed unit (mph / km/h / m/s).
_Avoid_: settings (too broad)

### In-progress editing

**Draft**:
An in-progress, unsaved Practice Unit or Practice Session being created or edited. Never persisted until explicitly saved.
_Avoid_: unsaved state, in-flight, pending

**Validation Issue**:
A rule violation found in a Draft that prevents it from being saved.
_Avoid_: error, form error
