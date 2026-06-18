# Requirements Questions — Round 1

## Q1: Concurrency Policy (RWK-17, RWK-24)

Can a user have multiple runs in progress at the same time, or only one?

If only one: when the user taps "Start Session" while a run is already active, should we prompt them to resume the existing run, or offer to abandon it and start fresh?

**Answer:** Yes, multiple allowed.

---

## Q2: What Does "Pause" Mean? (RWK-17, RWK-23)

Is there a meaningful difference between "paused" and simply leaving the app with a run in progress? Or is pause just the implicit state when the user isn't actively on the execution screen?

Put another way: does the user ever need to explicitly tap a "Pause" button, or is the run always resumable from wherever they left off regardless?

**Answer:** No explicit pause.

---

## Q3: Run History After Completion (RWK-18)

When a run is completed or abandoned, should it be kept in a viewable history, or permanently deleted?

If kept: is viewing past runs in scope for this epic, or just storing them for a future feature?

**Answer:** Completed runs should be stored, abandoned runs should not.

---

## Q4: Elapsed Time — Wall Clock vs. Active Time (RWK-16, RWK-22)

Should elapsed time measure wall-clock time from when the run was started (including breaks, app backgrounding, etc.), or only the time the user was actively on the execution screen?

**Answer:** Active screen, but should keep running when the device screen is off. Timestamp based rather than interval based.

---

## Q5: Out-of-Order Completion Behavior (RWK-14, RWK-15, RWK-23)

The tickets mention supporting out-of-order completion (skipping ahead). When a user skips ahead and completes step 5 while on step 2:

- Does the "current position" jump to step 5, or stay at step 2?
- Are the skipped steps (3 and 4) left as "remaining" for the user to come back to?
- Can the user freely jump to any step, or only navigate forward/backward one at a time?

**Answer:** There should be no explicit 'Current position' state, just which step the user is on. Steps are left as incomplete. The user can jump to any step, but the main phone UI should only have next/previous. Possibly a drawer to view all steps and jump between.

---

## Q6: Offline Support Scope (RWK-21)

How important is offline support for the initial release? Full offline capability (start a run with no connection, queue all writes) adds significant complexity.

Options:

- **Full offline**: runs can be started and progressed entirely offline, synced later
- **Graceful degradation**: require connectivity to start a run, but tolerate brief drops mid-session (queue completion toggles)
- **Online-only for MVP**: require connectivity throughout, add offline later

**Answer:** Online only.

---

## Q7: Empty / Unrunnable Sessions (RWK-13)

When a session has zero units, or all units have zero instructions, should the "Start Session" button be:

- Hidden entirely
- Visible but disabled with an explanation
- Visible, and show a warning/toast when tapped

**Answer:** Visible but disabled with an explanation

---

## Q8: Disabled Clubs in Session Templates (RWK-13)

If a session template references clubs the user has since disabled, should we:

- Start the run anyway (the snapshot preserves the club name as-is)
- Warn the user before starting
- Block the start until they fix the session

**Answer:** Start the run anyway (the snapshot preserves the club name as-is), but users should be able to change the club in the run.

---

## Q9: Editing During Execution (RWK-19, RWK-20)

The snapshot is immutable. Can the user make any ad-hoc changes during a run (e.g., adding a note, adjusting a ball count), or is the run strictly read-only + mark-complete?

**Answer:** Read only + mark complete

---

## Q10: Navigation During Execution (RWK-14)

Can the user navigate away from the execution screen to view other parts of the app (e.g., check another session, view settings) and come back? Or is execution a focused/locked mode?

**Answer:** Yes they can navigate away

---

## Q11: Screen Keep-Awake (RWK-14)

The ticket mentions keeping the device awake and readable in outdoor conditions. Is screen keep-awake a hard requirement for the initial release, or a nice-to-have?

(Outdoor readability is largely a theme/contrast concern — is there anything specific beyond keep-awake you want here?)

**Answer:** Yes enable keep-awake for session run screen, but must function correctly if the user manually turns off phone or switches apps

---

## Q12: Completion Summary Content (RWK-18)

When a run finishes, the summary shows balls hit, time, and completion %. Should it also include:

- A per-unit breakdown?
- Which items were skipped (for abandoned runs)?
- Any kind of "share" or export option?

Or is a simple stats summary sufficient for now?

**Answer:** Sufficient for now

---

## Q13: Where Does the "Start Session" Action Live? (RWK-13)

The ticket says "from the session detail screen." Should there also be a quick-start action from:

- The session list (e.g., a play button on each card)?
- The overview/home screen?

Or only from the detail screen?

**Answer:** Also an action on home screen

---

## Q14: Surfacing In-Progress Runs (RWK-17, RWK-24)

"Surface in-progress runs prominently on app open" — where exactly?

- A banner/card on the Overview (home) screen?
- At the top of the session list?
- A persistent floating element?
- A dedicated "Active Run" tab or section?

**Answer:** A banner/card on the Overview (home) screen

---
