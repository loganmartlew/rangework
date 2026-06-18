# RWK-11: Session Execution — Non-Technical Requirements

This document captures every non-technical product requirement for the Session Execution epic, derived from Jira tickets RWK-11 through RWK-24 and two rounds of requirements questions.

---

## 1. Starting a Run

### 1.1 Entry Points

- **Session detail screen**: A "Start Session" action is available on the session detail screen. *(RWK-13)*
- **Overview (home) screen**: A FAB opens a session picker/list allowing the user to choose which session to start. *(RWK-13, Q13, Q21)*

### 1.2 Snapshot Creation

- Starting a run creates a new execution instance containing a frozen snapshot of the session template at that moment: all units, instructions, clubs, notes, focus cues, repeat counts, and ball allocations. *(RWK-13, RWK-20)*
- Repeat counts are expanded into discrete, individually addressable steps at snapshot time. *(RWK-20)*
- The snapshot is fully decoupled from the template. Subsequent edits or deletion of the template do not affect the run. *(RWK-13, RWK-19)*
- The run records a start timestamp and links back to the source session. *(RWK-13)*

### 1.3 Disabled Clubs

- If the session template references clubs the user has since disabled, the run starts anyway. The snapshot preserves the club name as-is. *(RWK-13, Q8)*

### 1.4 Empty / Unrunnable Sessions

- If a session has zero units or all units have zero instructions, the "Start Session" action is visible but disabled, with an explanation of why the session cannot be started. *(RWK-13, Q7)*

### 1.5 Post-Start Navigation

- After starting a run, the user lands on the live execution screen at the first instruction. *(RWK-13)*

---

## 2. Concurrency

### 2.1 Multiple Runs Allowed

- A user can have multiple runs in progress simultaneously. *(Q1)*
- A user can start a second run of the same session template while a first run of that session is still active. *(Q17)*
- There is no limit on concurrent active runs. *(Q1, Q17)*

---

## 3. Execution Screen — Stepping Through Instructions

### 3.1 Instruction Display

- Instructions are presented one at a time, in the correct order: units in session order, instructions within each unit in order, with repeat counts expanded into discrete steps. *(RWK-14)*
- The current instruction displays its ball count, club, notes, and focus cue from the snapshot. *(RWK-14)*
- Unit context is shown: which unit the step belongs to, step position within the unit, and rep number (e.g., "Wedge Warmup — Step 3 of 8, Rep 2 of 3"). *(RWK-14, Q24)*

### 3.2 Navigation

- Forward and backward navigation between instructions via next/previous controls on the main phone UI. *(RWK-14, Q5)*
- A side drawer allows viewing all steps and jumping to any step directly. *(Q5, Q20)*
- **Tablet layout**: The drawer equivalent is permanently mounted on the left side of the screen. *(Q20)*
- The user can freely jump to any step — there is no enforced linear progression. *(Q5)*

### 3.3 No Explicit "Current Position" State

- There is no system-tracked "current position." The app simply shows whichever step the user is viewing. *(Q5)*
- When resuming a run, the app should restore to the step the user was last viewing. *(RWK-17)*

### 3.4 Screen Behavior

- The execution screen keeps the device awake (screen-on) while it is displayed. *(RWK-14, Q11)*
- The app must function correctly if the user manually turns off the phone screen or switches apps — keep-awake is a convenience, not a hard lock. *(Q11)*

---

## 4. Marking Items Complete & Progress Tracking

### 4.1 Completion Toggling

- Each instruction/rep (discrete step) can be toggled between complete and incomplete. *(RWK-15)*
- Completion state persists to the cloud immediately (online-only for this release). *(RWK-15, Q6)*
- Completing the current item advances the view to the next incomplete step. *(RWK-15)*

### 4.2 Out-of-Order Completion

- Steps can be completed in any order. Skipped steps remain as incomplete for the user to come back to. *(RWK-15, Q5)*

### 4.3 Visual States

- Steps are visually distinguished as **complete** or **incomplete**. *(RWK-15)*
- The step the user is currently viewing is visually highlighted in the drawer/step list. *(RWK-15)*

### 4.4 Undo

- Toggling a completed item back to incomplete serves as the undo mechanism. No separate undo action is required. *(RWK-15)*

---

## 5. Club Override

- During a run, the user can swap the club on any individual step. *(Q8, Q15)*
- The club change applies only to that specific step, not to all steps using the original club. *(Q15)*
- All other snapshot data (ball count, notes, focus cue, instruction text) remains read-only. *(Q9, Q15)*

---

## 6. Run Progress Overview

### 6.1 Metrics Displayed

- Balls completed vs. total ball count (from snapshot). *(RWK-16)*
- Instructions/steps completed vs. total. *(RWK-16)*
- Per-unit completion indicator. *(RWK-16)*
- Overall completion percentage. *(RWK-16)*
- Elapsed active time. *(RWK-16, Q4)*

### 6.2 Ball Count Edge Cases

- Instructions with no ball count assigned are still completable steps but do not contribute to the balls completed/total tally. *(RWK-16, RWK-22)*
- If all instructions have no ball count, the ball metric is omitted or shows "N/A" rather than dividing by zero. *(RWK-22)*

### 6.3 Elapsed Time Behavior

- The timer starts when the user first enters the execution screen. *(Q4, Q19)*
- The timer keeps running if the device screen turns off but the app remains foregrounded. *(Q19)*
- The timer pauses when the user navigates away from the execution screen to another part of the app. *(Q19)*
- The timer resumes when the user returns to the execution screen. *(Q19)*
- Time tracking is timestamp-based (recording enter/exit timestamps) rather than interval-based. *(Q4)*

### 6.4 Accessibility

- The progress overview is reachable without losing the current step position. *(RWK-16)*

---

## 7. Persistence & Resume

### 7.1 Cloud Persistence

- Runs are saved to the cloud. Online connectivity is required (no offline support for this release). *(RWK-17, Q6)*

### 7.2 Resume Behavior

- In-progress runs persist across app launches. *(RWK-17)*
- Resuming restores the step the user was last viewing and all completion state. *(RWK-17)*

### 7.3 No Explicit Pause

- There is no "Pause" button or explicit paused state. A run is either in progress or finished (completed/abandoned). *(Q2, Q16)*
- Run status can be inferred from run data (e.g., in progress = some but not all steps completed and not abandoned). *(Q16)*

### 7.4 App Navigation

- The user can navigate away from the execution screen to other parts of the app (settings, other sessions, etc.) and return to the run at any time. *(Q10)*

---

## 8. Completing a Run

### 8.1 Full Completion

- When the user completes the final step (or all steps are marked complete), a "Finish session" action is presented. *(RWK-18)*
- The finish summary shows: balls hit, elapsed time, and completion percentage. *(RWK-18, Q12)*
- A simple stats summary is sufficient — no per-unit breakdown, skip list, or share/export for this release. *(Q12)*

### 8.2 Abandoning a Run

- The user can end a run early at any time. *(RWK-18)*
- A simple confirmation dialog is shown (e.g., "Are you sure? Progress will be lost."). *(Q25)*
- Abandoned runs are deleted — they are not stored in history. *(Q3)*

### 8.3 Post-Finish

- Completed and abandoned runs are removed from the active runs list. *(RWK-18)*
- An end timestamp is recorded on completed runs. *(RWK-18)*
- The user can immediately start a fresh run of the same session, but there is no special "Run again" UI on the completion summary. *(Q26)*

---

## 9. Run Lifecycle States

- A run's status is inferred from its data rather than stored as an explicit state machine field. *(Q16)*
- Logical states: **in progress** (started, not all steps complete, not abandoned), **completed** (all steps complete or explicitly finished), **abandoned** (ended early — record is then deleted). *(RWK-23, Q16, Q3)*
- There is no "not started" or "paused" state. *(Q2, Q16)*

---

## 10. Surfacing Active Runs

### 10.1 Overview (Home) Screen

- In-progress runs are shown as a banner/card carousel on the Overview screen. *(RWK-24, Q14)*
- If multiple runs are active, all are shown in a scrollable list/carousel. *(Q18)*
- Tapping a run card navigates directly to the execution screen for that run. *(RWK-24)*

### 10.2 App Launch

- On app open, the system queries for in-progress runs and surfaces them on the Overview screen. *(RWK-24)*

---

## 11. Run History

### 11.1 Completed Runs

- Completed runs are stored persistently. *(Q3)*
- Past completed runs are viewable on the session detail screen for their source session. *(Q22)*

### 11.2 Abandoned Runs

- Abandoned runs are not stored. They are deleted on abandonment. *(Q3)*

---

## 12. Template Deletion with Active Runs

- If the user deletes a session template that has active runs, a warning is shown but deletion is not blocked. *(Q23)*
- Active runs of the deleted template continue to function — the snapshot is self-contained. *(RWK-13, Q23)*
- The run continues to show the original session name from the snapshot. *(Q23)*
- There is no special indication on the run or its history that the source template was deleted. *(Q23)*

---

## 13. Connectivity

- This release is online-only. Starting a run, toggling completion, and all other operations require an active network connection. *(Q6)*
- Offline support (write queuing, replay on reconnect) is deferred to a future release. *(Q6)*

---

## 14. Layout & Responsiveness

### 14.1 Phone Layout

- Single-instruction view with next/previous navigation. *(RWK-14, Q5)*
- Side drawer for step list and jump-to navigation (opened on demand). *(Q20)*

### 14.2 Tablet Layout

- The step list / drawer equivalent is permanently mounted on the left side of the screen. *(Q20)*
- The current instruction detail occupies the remaining space on the right. *(Q20)*

---

## Ticket Cross-Reference

| Requirement Area | Primary Tickets | Clarified In |
|---|---|---|
| Starting a run | RWK-13, RWK-20 | Q7, Q8, Q13, Q21 |
| Concurrency | RWK-17, RWK-24 | Q1, Q17 |
| Execution screen & stepping | RWK-14 | Q5, Q11, Q20, Q24 |
| Marking complete & progress | RWK-15, RWK-16, RWK-22 | Q4, Q5, Q19 |
| Club override | — | Q8, Q9, Q15 |
| Persistence & resume | RWK-17, RWK-21 | Q2, Q6, Q10, Q16 |
| Completing / abandoning | RWK-18 | Q3, Q12, Q25, Q26 |
| State machine | RWK-23 | Q2, Q16 |
| Active run surfacing | RWK-24 | Q14, Q18 |
| Run history | — | Q3, Q22 |
| Template deletion | — | Q23 |
| Layout | RWK-14 | Q20 |
| Data model & schema | RWK-19 | Q15, Q16 |
