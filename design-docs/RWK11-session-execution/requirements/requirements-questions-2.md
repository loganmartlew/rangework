# Requirements Questions — Round 2

Follow-ups based on Round 1 answers.

---

## Q15: Club Change Contradicts Read-Only (from Q8 + Q9)

Q8 says users should be able to change the club during a run, but Q9 says the run is read-only + mark-complete. Which takes priority?

Options:

- Strictly read-only: the snapshot is completely frozen, club names from disabled clubs are preserved as-is
- Read-only with club override: everything is frozen except the user can swap the club on a given step (e.g., they forgot their 7-iron today)

If club override: does the change apply to just that one step, or all steps using that club?

**Answer:** Read-only with club, and just that step.

---

## Q16: State Machine Without Pause (from Q2)

With no explicit pause, the run lifecycle simplifies to: **in_progress → completed | abandoned**. The "paused" state from RWK-23 is dropped — a run is either active or finished.

Is that correct? Or do you still want a "paused" state that the system sets automatically (e.g., when the user leaves the execution screen)?

**Answer:** I don't care about paused. The state of a run can be inferred from the rest of the run data in most cases, which eliminates the need for a state machine. E.g: In Progress when some but not all steps completed.

---

## Q17: Multiple Active Runs — Same Session? (from Q1)

Multiple concurrent runs are allowed. Can the user start a second run of the _same_ session template while a first run of that session is still active?

**Answer:** Yes

---

## Q18: Multiple Active Runs — Banner Display (from Q1 + Q14)

If multiple runs can be in progress, how should the Overview banner handle it?

- Show only the most recent active run
- Show all active runs as a scrollable list/carousel
- Show a count with a "View all" action

**Answer:** Show all active runs as a scrollable list/carousel

---

## Q19: Elapsed Time Tracking Detail (from Q4)

You said "active screen time, timestamp-based, keeps running with screen off." To clarify:

- Timer starts when the user enters the execution screen
- Timer keeps running if the device screen turns off (but the app is still foregrounded / not killed)
- Timer pauses when the user navigates away from the execution screen to another part of the app
- Timer resumes when they return to the execution screen

Is that the intended behavior? Or should the timer run continuously from first entry until the run is completed/abandoned, regardless of navigation?

**Answer:** The described behaviouir is correct

---

## Q20: Step Drawer / Jump-To UI (from Q5)

You mentioned "possibly a drawer to view all steps and jump between." For this epic:

- Is the drawer in scope, or should we just build next/previous navigation and add the drawer later?
- If in scope: is this a side drawer, bottom sheet, or a separate list screen?

**Answer:** Drawer is in scope. Side drawer. Tablet view should have the drawer equivalent mounted on the left side permanently.

---

## Q21: Home Screen Start Action (from Q13)

What does the "Start Session" action on the home screen look like?

- A prominent button that opens a session picker / list to choose which session to start
- Quick-start cards for recent or favorited sessions
- Something else

**Answer:** FAB, A prominent button that opens a session picker / list to choose which session to start

---

## Q22: Completed Run History View (from Q3)

Completed runs are stored. Is a screen to view past completed runs in scope for this epic, or are we just persisting the data for a future feature?

**Answer:** Past runs should be viewable on the session details screen

---

## Q23: Deleting a Session Template with Active Runs

If the user deletes a session template that has one or more active runs, the snapshot keeps the run intact. But:

- Should the run still show the original session name?
- After the run finishes, is there any indication it came from a now-deleted template?
- Should we prevent deletion of templates with active runs, or just let it happen silently?

**Answer:** Yes show the original name, no indication it was deleted, show a warning but do not prevent deletion.

---

## Q24: Unit Context During Execution (RWK-14)

When stepping through instructions, should the execution screen show which unit the current step belongs to? For example: "Wedge Warmup — Step 3 of 8, Rep 2 of 3"

Or is just the instruction content (club, ball count, notes, focus cue) sufficient?

**Answer:** Yes that info should be shown about unit/step

---

## Q25: Abandon Confirmation (RWK-18)

When the user taps "Abandon" on a run:

- Simple confirmation dialog ("Are you sure? Progress will be lost.")?
- Show how much progress will be lost (e.g., "You've completed 12 of 30 steps")?
- Allow adding a reason or note for why they stopped?

**Answer:** Simple confirmation dialog

---

## Q26: Re-Running a Completed Session

After completing a run of a session, can the user immediately start a fresh run of the same session? Any special UI for this (e.g., "Run again" on the completion summary)?

**Answer:** Yes, but no special UI for it.

---
