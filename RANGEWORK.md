# Rangework

Rangework is an Android app for golfers who want to practice with purpose. Instead of showing up at the range and hitting balls aimlessly, Rangework lets you plan your practice in advance — building structured drills and composing them into repeatable session templates you can follow on the day.

## What it does

The core idea is a three-level structure:

**Practice Units** are individual drills. Each unit has a name, an ordered list of instructions (with optional ball counts per instruction), and optional context like notes, a mental focus cue, and a default club. A unit might be something like "50-yard pitch shots" with instructions for three different landing-zone targets, each consuming 10 balls.

**Practice Sessions** are templates that combine units into a full practice plan. You pick which units to include, set how many times each one repeats, and optionally override the club or add session-specific notes for each session item. The app tallies the total ball count for the session so you know exactly what you're signing up for before you leave the house.

**Range Sessions** are live execution records created when you start a practice session at the range. The practice session is snapshotted at launch, expanding into an ordered list of individual steps (one per instruction × repetition). You work through each step one at a time, marking them complete as you go. Progress is persisted continuously so you can close the app and resume right where you left off.

All data is saved to the cloud and tied to your account, so your plans are available whenever you open the app.

## Features

### Practice Units

- Create units with a title, ordered instructions, ball counts per instruction, notes, focus cue, and a default club
- View a list of all your units, including instruction count and total ball count at a glance
- Open a unit to see its full detail
- Edit or delete any unit

### Practice Sessions

- Create sessions by adding units, setting repeat counts per unit, and optionally overriding the club, notes, or focus cue for each session item
- See a live running ball count total while building the session
- View a list of all your sessions with their unit lineup and total ball count
- Open a session to see the full breakdown
- Edit, delete, or duplicate any session

### Range Sessions (execution)

- Start a range session directly from any practice session
- A snapshot of the session — units, instructions, clubs, notes, and focus cues — is captured at start time so the record is immune to later edits
- Steps are flattened from the snapshot (unit × repetition × instruction) and presented one at a time
- Mark each step complete with a single tap; completing the current step auto-advances to the next
- Navigate freely with Previous / Next controls or jump to any step
- Progress and the last viewed step index are persisted after every action — close the app and resume exactly where you left off
- Time on range is recorded automatically and can be reviewed later
- Finish a session when all work is done, or abandon it to discard the record
- View active (in-progress) sessions and completed session history per template
- The screen stays on while a range session is active

### Settings

- **Theme** — choose Light, Dark, or follow the system setting
- **Distance and speed units** — switch between Yards/Metres and mph/km/h/m/s
- **Club bag** — enable or disable clubs from a catalogue of 30 clubs across woods, hybrids, irons, wedges, and putter; only enabled clubs appear in dropdowns throughout the app
- **Account** — view your signed-in email and sign out

### Account and data

- Sign in with Google (required to use the app)
- All practice data is private to your account and stored securely in the cloud
- Session is remembered between app launches — you stay signed in until you choose to sign out

### Layout

- On phones, navigation sits at the bottom of the screen
- On tablets, navigation moves to a side rail and the overview screen expands into a two-column layout
