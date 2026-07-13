# Planning & Execution

The core golf practice planning domain. Defines the three-level hierarchy (Practice Unit → Practice Session → Range Session) and the vocabulary used across the Android app, KMP shared module, and Supabase schema.

## Language

### Planning layer

**Practice Unit**:
A reusable, self-contained drill. Owns an ordered list of Practice Instructions, optional notes, a Focus Cue, and an optional default Club. The default Club is the fallback for any Practice Instruction that does not set its own Club.
_Avoid_: drill, exercise, activity

**Practice Instruction**:
A single ordered directive within a Practice Unit, with descriptive text, an optional Ball Count, and an optional Club. The Club lets a single drill use different clubs across its instructions (e.g. a wedge ladder: GW, then SW, then LW). When an instruction carries no Club, it falls back to its Practice Unit's default Club.
_Avoid_: step (reserved for execution Steps), task

**Practice Session**:
A reusable template that assembles Practice Units into a complete practice plan. Contains an ordered list of Session Items.
_Avoid_: session template, workout, program, plan

**Archived**:
A reversible dormant state for a Practice Session that is no longer in rotation. An Archived session is excluded from default session listings but remains fully viewable and duplicable, and its Range Session history is untouched; it cannot be edited or used to start a Range Session until unarchived. Archiving a session whose Range Session is still Active is allowed — the Snapshot makes the running session immune. The unarchived state deliberately has no special name (never "Active", which belongs to the Range Session lifecycle). Archive sits before delete, not in place of it.
_Avoid_: deleted, hidden, inactive, active (for the unarchived state)

**Inline Unit**:
A Practice Unit owned by exactly one Practice Session, minted during planning (currently only by the Coaching context) to fill a slot in that specific session. It never appears in the unit library and can never be referenced by another session. Its lifecycle follows its owner: it goes dormant when the session is Archived, is deleted when the session is deleted, and is deep-copied (a new Inline Unit owned by the new session) when the session is duplicated. The only way out of ownership is Promotion.
_Avoid_: one-off unit, scoped unit, session-local unit, private unit

**Promotion**:
The one-way, user-initiated act of converting an Inline Unit into an ordinary library Practice Unit. Promotion detaches the unit from its owning session without changing its content or identity; the session keeps referencing the same unit. It happens only at the user's request — via an affordance on the unit within its session, or conversationally through the Coaching context — never silently. There is no demotion.
_Avoid_: publish, save to library, demote

**Session Item**:
A slot in a Practice Session that references a Practice Unit and carries session-level overrides: Repeat Count, Club, notes, and Focus Cue.
_Avoid_: session unit, practice item

**Repeat Count**:
The number of times a Practice Unit's full instruction set is executed within a Session Item. Multiplies the Unit's total Ball Count to give the Item's contribution to session volume.
_Avoid_: reps, repetitions

**Focus Cue**:
A mental focus directive — the specific thing to concentrate on during a Practice Unit. Defined on the Practice Unit (stored as `focus`) and optionally overridden per Session Item (stored as `focus_cue`). The Session Item's value takes precedence during execution.
_Avoid_: focus, mental note, tip

**Success Criterion**:
An optional, unit-defined statement of what counts as a successful ball for that drill (e.g. "inside 5m of the 60m flag"), expressed as text. Lives on the Practice Unit only — deliberately not overridable per Session Item — so success counts stay comparable across every Range Session of that unit. Editing a unit's Success Criterion starts a new measurement baseline; it never reinterprets history, because each Snapshot keeps the criterion that was in force when the Range Session started.
_Avoid_: goal, target, success metric, KPI

**Ball Count**:
The number of balls expected for a single Practice Instruction. Three states are distinct and must not be conflated:
- A **positive** count — N balls are hit.
- **Zero** — a deliberate no-ball directive, such as practice swings for feel. Definite, not missing.
- **Uncounted** (absent / `null`) — the count is unknown, not specified. Distinct from Zero: Zero means "deliberately none," Uncounted means "we don't know."

Positive and zero counts are summed across all instructions to give a Practice Unit's total; multiplied by Repeat Count to give a Session Item's total. How an Uncounted instruction affects an enclosing total is context-specific (see Uncounted).
_Avoid_: shot count, ball total (for the per-instruction value); do not treat Zero and Uncounted as the same.

**Uncounted**:
The state of a Ball Count that has not been specified (`null`). Means "we don't know how many balls," as opposed to Zero, which means "deliberately none." Treatment of Uncounted within a total is not yet uniform across contexts: the Coaching context propagates it — any Unit or Session with an Uncounted instruction reports an Uncounted (partial-estimate) total, surfaced as `has_uncounted_instructions` / `has_uncounted_items` — while the mobile Planning & Execution totals currently treat an Uncounted instruction as zero when summing.
_Avoid_: zero, empty, unavailable

### Tagging

**Tag**:
A label drawn from a single shared vocabulary, attached to Practice Units and Practice Sessions to organise and filter them (and to give the Coaching context a vocabulary for discovering relevant content). A Tag attached to a Practice Session expresses that session's own goal or focus, which is set independently and may differ from the Tags of the Units it contains.
_Avoid_: label, category, topic, keyword

**Default Tag**:
A Tag in the app-defined global catalog, owned by no user and identified by a stable code (e.g. `short_game`, `putting`). Shared by every user; the same Default Tag row is referenced by all. Mirrors the Club catalog pattern.
_Avoid_: system tag, built-in tag, preset

**Custom Tag**:
A Tag created and owned by a single user, extending the shared vocabulary beyond the Default Tags. Visible and usable only by its owner; may be renamed or deleted by the owner.
_Avoid_: user tag, personal label

**Tag Code**:
The lowercase-underscore slug identifying a Tag (e.g. `short_game`). For a Default Tag the code is curated and globally stable; for a Custom Tag it is slugged from the name and unique per owner. The code is the dedup key (typing a name that slugs to an existing code reuses that Tag) and the handle the Coaching context uses, but Tag attachments reference a Tag by its identity, not its code, so a Custom Tag can be renamed without breaking attachments. Custom Tag codes carry no cross-user meaning.
_Avoid_: tag slug, tag key, tag id

### Execution layer

**Range Session**:
A live execution record created when a golfer starts a Practice Session. Contains a Snapshot of the session content and tracks which Steps have been completed. A Range Session is Active until it is either Completed or Abandoned.
_Avoid_: execution, run

**Snapshot**:
An immutable capture of a Practice Session's content taken at the moment a Range Session starts. Because the Snapshot is self-contained, edits to the source Practice Session have no effect on any in-progress or historical Range Session.
_Avoid_: frozen session, copy, record

**Step**:
An atomic unit of work inside a Range Session — one ball hit, or one discrete no-ball action. Derived by expanding each Practice Instruction across its Repeat Count and its Ball Count: a positive Ball Count N yields N Ball Steps (one per ball); Zero and Uncounted each yield a single Action Step. The Snapshot holds the flat ordered list of all Steps. (Snapshots created before ball-granular expansion hold the older, coarser Steps — one per instruction × repetition — and keep that shape forever.)
_Avoid_: rep (ambiguous), task, instruction (reserved for the planning layer)

**Ball Step**:
A Step representing exactly one ball. Completing it is the atomic record that one ball was hit against a specific Practice Instruction.
_Avoid_: ball, shot, tick

**Action Step**:
A Step representing a discrete no-ball directive — a Zero Ball Count (deliberate no-ball work, e.g. rehearsal swings) or an Uncounted one (volume unknown; recorded as done/not-done only, so any actual balls hit against it go unrecorded).
_Avoid_: checklist item, non-ball step

**Block**:
The execution-time view of one Session Item within a Range Session: the grouping of that item's Steps, with its own progress, rendered as one screen. Planning-side the same content is a Session Item; a Block is its live counterpart, the way a Range Session is the live counterpart of a Practice Session.
_Avoid_: session item (planning-side), unit screen, drill block

**Completed Step**:
A record that a Step has been marked done, with a completion timestamp.

**Observation Type**:
A named, app-defined kind of thing a player can watch for on each ball — e.g. shot shape, strike location, turf contact, or success against the unit's Success Criterion. Each Observation Type has a fixed, app-defined set of values; how a value is entered (chip row, face-zone grid) is presentation, not part of the type. Observation Types are enabled per Session Item when building a Practice Session — zero, one, or several at once — and the enabled set is baked into the Snapshot at Range Session start. Where Focus Cue says what to think about, enabled Observation Types say what to watch for. Observations record what happened, never whether it was good — with one deliberate exception: the Success type, a binary Hit/Miss judged by the player against the unit's Success Criterion text. It is the only type that records goodness, and no multi-value type ever feeds a success count.

The v1 catalog:
- **Success** — Hit / Miss against the unit's Success Criterion; only offerable when the unit defines one.
- **Strike Location** — where on the clubface the ball was struck; nine zones (heel/center/toe × high/middle/low).
- **Contact** — how the strike found the turf; ordered scale: Very Fat / Fat / Flush / Thin / Very Thin.
- **Shape** — the nine-flight ball-flight matrix (start line × curvature).
- **Distance** — finish relative to target depth; ordered scale: Way Short / Short / On / Long / Way Long.
- **Direction** — finish relative to target line; ordered scale: Way Left / Left / On Line / Right / Way Right. Dispersion is not an Observation Type — a single ball has a miss direction; dispersion is the emergent spread of a block's Direction Observations.

Shape and Direction values (and Strike Location rendering) are handedness-sensitive: stored canonically, presented per the user's handedness preference.
_Avoid_: dimension, metric, measure, measurement (reserved for possible future instrument-captured data), tracker, criterion (as the type name — the type is Success; the unit-level rubric is the Success Criterion), dispersion (as a per-ball value)

**Observation**:
One recorded value for one Observation Type on one completed Ball Step (e.g. Shape = fade). Observations attach to the Completed Step; a ball may carry an Observation for every enabled type, some, or none — an absent Observation means that ball was not observed for that type, never a default value. Recording an Observation must never be a precondition for completing a Step. While the Range Session is Active, any completed ball's Observations may be corrected; once the session leaves Active they freeze. Correcting an Observation never un-completes the Step or alters its completion timestamp — what happened and what was observed about it are separate records.
_Avoid_: reading, data point, ball record

**Block Result**:
An optional, player-entered record attached to one Block: a free-text note, plus — only when the unit defines a Success Criterion — a success count against that criterion. The note is always available on every Block, entered through a passive affordance, never prompted or required. The count has exactly one source: entered manually (X of the block's Y balls) when the item did not enable the Success Observation Type, or derived read-only from Hit/Miss Observations (X of Z observed balls, denominated in observed balls and labeled as such) when it did. A Block whose unit has no Success Criterion takes a note only; a bare count with no criterion is disallowed because the number would be meaningless across sessions. The note remains editable after the session is Completed; the manual count freezes at completion, like Observations — prose is reflection, counts are data.
_Avoid_: block score, block note (the note is only one part), result prompt

**Session Note**:
A single optional free-text reflection recorded against a Range Session as a whole (e.g. at finish), capturing overall impressions rather than any one Block's outcome. Remains editable after the session is Completed — reflection legitimately happens later. An Abandoned session keeps whatever was recorded before abandonment but takes no further edits of any kind.
_Avoid_: session summary, journal entry

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
A specific type of golf club in the system catalog, identified by a stable Club Code. Has a display name and category (Driver, Wood, Hybrid, Iron, Wedge, Putter).
_Avoid_: club type, equipment

**Club Code**:
The stable, lowercase-underscore identifier for a Club (e.g. `seven_iron`, `pitching_wedge`). All club assignments in practice data use Club Codes, not display names.
_Avoid_: club reference, club key, club id

**Club Bag**:
The set of Clubs a user has enabled from the catalog. Only clubs in a user's Bag appear in UI selectors and are valid targets for club assignments.
_Avoid_: equipped clubs, my clubs

**Club Resolution**:
The precedence used to determine the Club for a single Step, in two phases that never coexist at runtime. At Range Session start, the three planning layers collapse into the Snapshot: the Session Item's Club, then the Practice Instruction's Club, then the Practice Unit's default Club (`sessionItem.club ?? instruction.club ?? unit.defaultClub`) — the resolved Club is baked into each Step. During execution, exactly one further layer exists: a per-Step Club Override, which takes precedence over the Step's baked Club for that Step only. A Session Item Club is therefore a whole-unit override that flattens every instruction's Club to one Club for that session; the Practice Unit default fills only instructions that set no Club of their own.
_Avoid_: club fallback, club inheritance

**Club Override**:
A per-Step, execution-time replacement of the Step's baked Club, recorded on the Range Session. Stored per Step so each ball keeps the Club it was actually hit with; the swap gesture in the UI applies one choice to an instruction's remaining incomplete Steps by writing an override for each.
_Avoid_: runtime club, club swap (the gesture, not the record)

### Derived values

**Estimated Duration**:
A derived estimate of how long a Practice Session will take, computed from session total Ball Count at 15 seconds per ball.

### User

**User Preferences**:
A user's measurement and presentation settings: unit system (Imperial / Metric / Custom), distance unit (Yards / Meters), speed unit (mph / km/h / m/s), and handedness (right-/left-handed — orients perspective-dependent input surfaces such as a strike-location grid).
_Avoid_: settings (too broad)

### In-progress editing

**Draft**:
An in-progress, unsaved Practice Unit or Practice Session being created or edited. Never persisted until explicitly saved.
_Avoid_: unsaved state, in-flight, pending

**Validation Issue**:
A rule violation found in a Draft that prevents it from being saved.
_Avoid_: error, form error
