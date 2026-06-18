# RWK-11: Session Execution — Change List

## Epic Summary

Transform Practice Sessions from static templates into interactive, real-time execution workflows. Users start a "run" of a session at the driving range and step through every instruction, tracking progress as they go.

---

## Feature Changes

### 1. Start a Session Run (RWK-13)

- Add an action on the session detail screen to start a live execution run
- Create an execution instance containing a frozen snapshot of the session template (units, instructions, clubs, notes, focus cues, repeat counts, ball allocations)
- Record a start timestamp and link back to the source session
- Navigate the user into the live execution screen at the first instruction
- Validate the session is runnable before starting (non-empty units/instructions)

### 2. Step Through Instructions (RWK-14)

- Build a core execution view that presents run instructions one-by-one in order
- Expand repeat counts into discrete steps so each rep is individually addressable
- Display the current instruction's ball count, club, notes, and focus cue
- Show unit boundaries and "rep X of Y" context
- Support forward and backward navigation between instructions
- Keep the device awake during execution

### 3. Mark Items Complete & Track Progress (RWK-15)

- Allow each instruction/rep to be toggled complete and incomplete
- Persist completion state to the cloud immediately
- Visually distinguish complete, current/in-progress, and remaining steps
- Advance to the next instruction on completion
- Support out-of-order completion (skipping ahead)
- Support undo / un-marking a completed item

### 4. Run Progress Overview (RWK-16)

- Show a summary of overall run progress (balls hit vs. total, instructions completed vs. total, units completed, elapsed time)
- Update tallies live as items are completed
- Show per-unit and overall completion indicators
- Accessible without losing the current instruction position

### 5. Resume, Pause & Persist a Run (RWK-17)

- Persist in-progress runs across app launches
- Surface in-progress runs prominently on app open
- Allow explicit pause and resume
- Restore instruction position and all completion state on resume
- Define and enforce a concurrency policy for simultaneous runs

### 6. Complete or Abandon a Run (RWK-18)

- Trigger a "Finish session" action when all items are done, showing a summary (balls hit, time, completion %)
- Allow early end with confirmation, recording partial progress
- Remove finished/abandoned runs from the active list
- Store completion status and end timestamp on the run
- Distinguish between full completion and early abandonment

---

## Backend / Data Changes

### 7. Session Run Data Model & Schema (RWK-19)

- Define the SessionRun entity: ID, source session ID, owner, status (in-progress / paused / completed / abandoned), timestamps, current position pointer
- Define a denormalized snapshot sub-schema for units, instructions, clubs, notes, focus cues, repeat counts, ball allocations
- Define per-instruction/per-rep completion state records
- Apply schema versioning for forward compatibility

### 8. Snapshot Generation Logic (RWK-20)

- Deep-copy a session template into an immutable run snapshot at start time
- Resolve all template references (units, clubs) into concrete stored values
- Expand repeat counts into discrete, individually addressable steps
- Ensure deterministic ordering so position pointers are stable

### 9. Cloud Persistence & Sync Layer for Runs (RWK-21)

- Extend cloud storage to store, update, and retrieve session runs
- Implement optimistic local writes with background cloud sync
- Define conflict resolution strategy for multi-device use
- Implement offline write queue with replay on reconnect
- Ensure per-step atomic writes for completion state

### 10. Progress Calculation & Aggregation Logic (RWK-22)

- Compute balls completed vs. total (handling null ball counts)
- Compute instructions completed vs. total, units complete, overall completion %
- Compute elapsed time from start timestamp
- Reusable logic shared between live overview and finish summary screens

### 11. Execution State Machine (RWK-23)

- Define run-level states: not_started → in_progress ⇄ paused → completed | abandoned
- Define per-step states: remaining → current → complete (with out-of-order support)
- Define "current position" semantics
- Enforce legal state transitions
- Expose navigation primitives (next, previous, jump-to)

### 12. Active-Run Resolution, Concurrency & Routing (RWK-24)

- Query for in-progress runs on app launch
- Enforce concurrency policy (one active run vs. multiple)
- Handle "start while one already active" — prompt to resume or confirm replace
- Surface in-progress runs prominently on the Overview screen
- Provide deep-link / routing to jump directly into an active run
