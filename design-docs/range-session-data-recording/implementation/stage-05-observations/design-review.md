# Stage 5: Per-ball Observation Capture — Prototype Documentation & Decision Record

**Date:** 2026-07-12 (living document — updated as prototype review proceeds)
**Status:** prototype-driven review in progress
**Reference artifact:** [`prototype.html`](./prototype.html) — a self-contained interactive HTML
prototype (open in any browser, no server). **The prototype is the design reference for the
Stage 5 implementation plan**; this doc explains what it demonstrates, records decisions, and
flags what in it is authoritative vs. throwaway.

**Scope:** UI/interaction design only. Schema (Stage 1), shared models, encodings, and tally
logic (Stage 2) are shipped and fixed. Field test #1 did not happen (Stage 4 changes.md) — this
design leans on `design-decisions.md` §5–6 plus capture-ergonomics judgement, validated against
the prototype instead of range-tested patterns.

---

## Fixed constraints carried in from the agreed design

- The tally surface **is** the input surface — counts live on the chips/cells themselves.
- Auto-commit when every enabled type has a value; a **+1 always present** commits partial/empty.
- Absent means *unobserved*, never a default. Per-type denominators diverge and that's honest.
- Recording is never a precondition for completing a Step.
- Edit any completed ball while Active; undo-last (−) is a special case and **voids** the ball's
  observations; freeze on leaving Active. Corrections never un-complete a Step.
- Deferred-scope tripwire: **no charts, no judgement colors, no progress-toward-target framing.**
- D4 physical frame (Stage 2, shipped): stored values are player-independent; handedness affects
  rendering only. Strike grid columns mirror for lefties; shape grid keeps golfer-term labels in
  constant screen positions; **Direction never flips**.

---

## Using the prototype

Open `prototype.html` in a browser. Left rail:

- **Scenarios A–E** — the five states the review committed to drawing:
  - **A** — one enabled type (Direction), 7/10 balls: the common case under the restraint rule.
  - **B** — all six types, 12/20, criterion set: the honest worst case for card height.
  - **C** — criterion-less unit (Strike + Shape): no Success row anywhere, Block result is
    note-only (no manual count).
  - **D** — Strike only, 19/25 with a real distribution: mid-block heatmap with a visible toe
    bias — the coaching signal delivered with zero charts.
  - **E** — left-handed Strike + Shape: strike columns/labels mirror; shape labels stay put.
- **Configuration** — free-form: per-type checkboxes, criterion text (blank it and the Success
  row disappears, mirroring the shipped `enabledObservationTypes` filter), total balls, RH/LH.
- **Reset balls** — empty block.

Everything in the phone frame is live: staging, auto-commit, +1/− (including observation
voiding), the grid dialogs, the per-ball edit sheet (tap the ball instruction row), and the
block-complete read-only state. Styling uses the real design tokens
(`packages/design/tokens/color.tokens.json`, dark scheme, DM Sans/DM Mono).

---

## Interaction spec (what the prototype demonstrates)

### The structural call (§0)

The capture surface lives **inside the existing counter card**, between the ball readout and
the −/+1 row. Not a separate card, never collapsed. Reasons: (1) the existing counter +1 *is*
the design's always-present +1 — commit gesture and committed values share one container;
(2) auto-commit legibility — the number that ticks must be in the same glance as the tap that
caused it; (3) one thumb zone for the whole per-swing loop. Rejected alternative: a sibling
always-visible card (breaks 1 and 2; also recreates Stage 4's "competing collapsed affordances"
worry, which the merged design dissolves — capture is *active*, `BlockResultSection` stays the
only passive surface). Cost accepted: a tall card when many types are enabled (scenario B shows
it honestly); the restraint rule makes 0–2 types the common case, and vertical cost beats
hidden inputs — a hidden input at the range is a dead input.

### Row order

Success first (the one judgement — the headline), then chip-scale types in catalog order
(Contact, Distance, Direction), then the two grid launcher rows together at the bottom, nearest
the +1. Grouped by input pattern, not strict catalog order.

### Chip rows (Success, Contact, Distance, Direction)

Full-width single-select segmented row, equal segments, **never horizontally scrollable**. Each
segment: value glyph (see below) + label + live tally count (blank at zero). Row header: type
label left, per-type `observed/completed` denominator right. Success is the two-segment variant
with the criterion rubric captioned on the header, no glyph. Tap stages for the pending ball;
re-tap un-stages; tapping another segment moves the selection. The count is history, the fill
is *this* ball — the count never bumps until commit. Direction renders identically for both
handednesses (D4).

### Grid types (Strike Location, Shape)

Launcher rows in the capture stack (mini-grid glyph → value glyph + golfer-term summary when
staged, plus the denominator) open a **3×3 dialog** — one tap in, one tap out: a cell tap
stages the value and closes the dialog immediately (auto-commit may fire as it closes); re-tap
of the staged cell clears it; scrim tap dismisses without staging. Cells are the live heatmap:
neutral tonal fill scaled by count/max — magnitude, never judgement — with the count in the
cell corner and a value glyph in the body.

**Handedness in the dialogs:** strike columns render Heel·Center·Toe for RH, Toe·Center·Heel
for LH (anatomical labels visibly swap; rows never flip). Shape rows/columns keep golfer labels
(Pull/Straight/Push × Draw/Straight/Fade) in constant screen positions for both handednesses.

### Value glyphs (SVG)

Decision D3 below. Every selectable value carries a small monochrome illustration of what it
means, judgement-free (currentColor, no red/green):

- **Strike Location** — a clubface (front view, grooves, hosel on the heel side — the face
  mirrors for lefties so the hosel always sits over the heel column) with an impact dot at the
  cell's zone.
- **Shape** — a top-down ball-flight path against a dashed target line, drawn from the
  **physical** start/curve values. Consequence worth naming: a lefty's dialog keeps the same
  labels in the same places, but the flight curves visibly mirror (a lefty Pull Draw flies
  right) — more informative than label-identical cells, and true.
- **Direction** — a start-line arrow from the ball, angled per value; physical, never flips.
- **Distance** — landing dot above/below a dashed target line.
- **Contact** — side view: ground line + ball, with a dot at the contact point (turf before
  the ball for fat, ball equator/top for thin).
- **Success** — deliberately no glyph; it's a judgement, not a physical observation.

Glyphs appear on capture chips, in grid cells, on staged launcher rows, and in the edit sheet.

### Commit model

A **pending ball** = staged values held in UI state. Two commit paths, one outcome:
auto-commit fires when every enabled type is staged (staged segments flash the arm colour for
~300ms, then the ball commits); **+1 commits any time with whatever is staged** — partial or
empty, and an empty commit is byte-identical to never observing (Stage 1 D2). Commit = counter
ticks + observation written + tallies bump + staging clears, perceived as one event. Feedback
is the counter bump plus the staging release — no snackbar, no toast (haptic tick is an
implementation-plan nice-to-have). Neither button changes appearance based on staging.
A committed partial/empty ball looks exactly like any counted ball — no badges; skipping is
first-class. The only traces are diverging denominators and `—` in the edit sheet.

**Undo (−):** decrements the last ball *and voids its observations* (Stage 2 rule); voided
values do not return to staging; staging in progress survives untouched (staged = next ball,
− = last ball).

### Per-ball edit sheet

With capture enabled, ball-instruction rows in the step list become tappable (trailing `›`) and
open a modal bottom sheet scoped to that instruction's completed balls, **newest first** (the
likely target is "the one I just mis-tapped"). Each row: ball number + value summary in
enabled-type order (`—` per unobserved type, "not observed" for empty balls — this is where
partiality becomes visible). Tapping a ball expands it in place (single-expanded accordion) to
**the same input surfaces as capture**, pre-selected: taps write through immediately, re-tap of
a selected value removes it (the ball becomes unobserved for that type), grid launchers open
the same dialog in edit mode. No Save button (consistent with Stage 4 auto-save), no tally
counts on sheet chips (they'd read as this ball's data), no +1/− in the sheet. Sheet = 
*corrections* (values wrong, ball real); − = *retractions* (ball shouldn't exist).
Non-Active sessions render rows read-only (Stage 5 scope; history detail is Stage 6).

### Page placement

Block page order is unchanged: focus cue → counter+capture card → Finish → instruction list →
Notes → **Block result stays last, collapsed**. Cadence ordering: per-ball things top, in the
thumb loop; per-block things bottom, behind deliberate expansion. When Success is enabled the
manual count row in Block result is already suppressed (Stage 4 shipped rule), so no rival
success numbers exist; the §6.1 "one derived Success line" *is* the Hit count on the Success
chips.

### Block-complete state

When no incomplete ball steps remain: capture rows dim to read-only tallies (the capture
surface becomes a block-review surface), +1 becomes the "Block complete" state, − remains for
corrections, edit sheet stays available while Active.

---

## Decision log

| # | Date | Decision |
| --- | --- | --- |
| D1 | 2026-07-12 | **Merged counter+capture card (§0): accepted provisionally.** Logan wanted to see it rendered before committing; the prototype now renders it. Confirm on prototype review / first device build. Fallback if it fails on device: sibling always-visible card (changes container anatomy, not the commit model). |
| D2 | 2026-07-12 | **Prototype replaces the ASCII design review as the design reference.** `prototype.html` is iterated until Logan is happy; the planning and implementation agents treat it (plus this doc) as the Stage 5 UI spec. The ASCII wireframes were retired in favour of this document. |
| D3 | 2026-07-12 | **SVG value glyphs on all physical observation values** (Logan's request): clubface+impact-dot for Strike, physical flight paths for Shape, abstract start-line / landing-spot / contact-point glyphs for Direction, Distance, Contact. No glyph for Success. Glyphs are monochrome and judgement-free (tripwire). Side effect accepted: the lefty shape dialog is no longer pixel-identical to the righty one — labels stay put but flight curves mirror physically, which is correct and more informative. |

Pending — to be recorded here as prototype review proceeds: chip-row anatomy sign-off, grid
dialog behaviour sign-off, commit model sign-off, edit sheet sign-off, page placement sign-off,
and final confirmation of D1 once the prototype (and later a device build) has been handled.

---

## Prototype vs. production — what is and isn't authoritative

**Authoritative (implement this):**

- The interaction grammar: staging semantics, auto-commit trigger and arm-flash, +1/− meanings,
  one-tap-in/one-tap-out dialogs, edit-sheet write-through, block-complete read-only.
- Layout structure and ordering: merged card anatomy, row order, header/denominator placement,
  page order, sheet anatomy.
- The glyph vocabulary (what each glyph depicts), heatmap = magnitude-only fill, count-blank-at-
  zero, criterion rubric on the Success header.
- Handedness rendering rules (already shipped as Stage 2 functions — the prototype's JS mirrors
  `ObservationRendering.kt` and must not drift from it).

**Not authoritative (prototype convenience — implementation plan decides):**

- Exact dp values, corner radii, type scale mapping, animation curves/durations.
- The web-flavoured widgets: production uses Material 3 Compose components (segmented-button-
  like rows, `ModalBottomSheet`, `AlertDialog`/custom dialog for grids).
- Glyphs as inline SVG: production draws them in Compose (`Canvas`/`ImageVector`); the shapes
  in the prototype are the reference, not the format.
- The prototype stubs: notes/Block result cards are static shells; club swap, Finish, and the
  action-instruction row are decorative; persistence, ViewModel wiring, and freeze-matrix
  enforcement are Stage 2's shipped logic, not re-specified here.

## Open items for the implementation plan (not for this review)

- Exact chip label abbreviations and minimum-width behaviour on compact screens.
- Tonal ramp specifics for the heatmap fill (which Material roles, how many steps).
- Compose implementation of the glyph set (ImageVector vs Canvas; shared per-value drawing fns).
- Optional haptic tick on commit.
- Whether the 5a/5b split (capture+commit vs tallies+corrections) from the epic plan is taken.
