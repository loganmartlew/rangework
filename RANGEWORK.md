# Rangework

Rangework is an Android app for golfers who want to practice with purpose. Instead of showing up at the range and hitting balls aimlessly, Rangework lets you plan your practice in advance — building structured drills and composing them into repeatable session templates you can follow on the day.

## What it does

The core idea is a three-level structure:

**Practice Units** are individual drills. Each unit has a name, an ordered list of instructions (with optional ball counts and an optional club per instruction), and optional context like notes, a mental focus cue, and a default club. Each instruction can set its own club to vary the club mid-drill — a wedge ladder (GW → SW → LW) as a single unit — while an instruction that sets no club of its own falls back to the unit's default club. A unit might be something like "50-yard pitch shots" with instructions for three different landing-zone targets, each consuming 10 balls.

**Practice Sessions** are templates that combine units into a full practice plan. You pick which units to include, set how many times each one repeats, and optionally override the club or add session-specific notes for each session item. The app tallies the total ball count for the session so you know exactly what you're signing up for before you leave the house.

**Range Sessions** are live execution records created when you start a practice session at the range. The practice session is snapshotted at launch, expanding into an ordered list of ball-granular steps (one per ball, plus check-off steps for no-ball directives). At the range you see one screen per block — the live view of a session item — with the focus cue front and centre and a ball counter tracking repetition. Progress is persisted continuously so you can close the app and resume right where you left off.

All data is saved to the cloud and tied to your account, so your plans are available whenever you open the app.

## Features

### Practice Units

- Create units with a title, ordered instructions, ball counts per instruction, an optional per-instruction club (falling back to the unit default when unset), notes, focus cue, and a default club
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
- The snapshot expands ball-granularly: an instruction for N balls becomes N ball steps, so each counted ball is a discrete, timestamped record; no-ball directives become single check-off steps
- One screen per block (session item): the focus cue is large and glanceable, the instruction list shows the structure of one pass with per-instruction tallies, and notes collapse out of the way
- Repetition is a ball counter — "+1" counts a ball (sweeping any preceding no-ball actions), "−1" undoes the last tap, and the button becomes "Done" when only check-offs remain
- Swap the club for an instruction mid-block; balls already hit keep the club they were hit with
- Navigate freely by swiping between blocks or jumping from the overview, which shows per-block progress; opening a session lands on the first incomplete block
- Finish from the overview at any time — finishing with incomplete steps asks whether to complete the remainder or finish as-is; progress is persisted after every action so you can close the app and resume where you left off
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
