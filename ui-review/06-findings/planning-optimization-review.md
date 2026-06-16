Looking at Rangework through that single lens — _does this help the golfer feel more prepared?_ — the app is currently a competent data manager that stops short of being a practice companion. It captures structure well but rarely converts it into something a golfer can feel, anticipate, or act on. Here's the review, screen by screen, with the administrative-vs-helpful tension called out each time.

## The core problem

The app treats a practice plan as a _record_ rather than a _briefing_. Almost every screen answers "what did I enter?" when the golfer is really asking "what am I about to go do?" That gap — between stored data and a felt sense of readiness — is where the app feels administrative.

The clearest symptom: a session is described entirely in **balls and repeat counts**. Eighteen balls, repeat 6x, 7-Iron. That's an inventory, not a plan. A golfer standing on the mat doesn't experience "18 balls"; they experience a sequence of intentions over a span of time. Nothing in the app translates the data into that lived shape.

## Overview (Images 12–13)

The signed-out welcome screen is genuinely good — it sets intent ("plan sharper range sessions") and frames the value before asking for anything. Keep that tone.

The signed-in Overview is where it slips. "1 saved building block ready to reuse" and "1 structured plan assembled from your live units" are counts of inventory. They tell the golfer how much they've _administered_, not whether they're _ready_. The most prepared-making thing this screen could do is surface the actual next session: "Your next plan: Test — 18 balls, ~25 min, 7-Iron focus" with a single tap to open it. Right now the Overview makes you navigate _to_ readiness rather than handing it to you.

The "Next move" card is instructional scaffolding aimed at empty states. Once a golfer has units and sessions, it's stale advice taking up the most valuable real estate on the screen.

## Units list & detail (Images 1–2)

The list card is strong — instruction count, ball count, and a preview of the instructions at a glance. This is the most "prepared-making" surface in the app because it shows _content_, not just metadata.

The detail screen (Image 2) is where administrative creep shows. "Instruction 1 / Instruction 2 / Instruction 3" labels are filing-cabinet language. The golfer never thinks "now I'll do Instruction 2"; they think "now the draw." The numbered scaffolding adds cognitive load without adding meaning — the instruction text already carries the identity. Letting the instruction _be_ the heading (with a quiet step indicator) reads as a practice sequence rather than a form readout.

"Balls: 1" repeated three times is also doing very little. When every instruction is one ball, the ball count is noise; it earns its place only when counts vary.

## Unit & session editors (Images 3–4, 7–8)

These are honest Material 3 forms and they work. Two observations through the readiness lens:

The **trash icon sits at the bottom-right of each instruction card**, immediately below the reorder chevrons (Images 4, 8). That's a destructive action one fat-thumb away from "move down." Material guidance would pull delete further from the frequent controls or require a confirm.

More importantly, the editors are where "balls" could become "time." If you know a golfer hits roughly a ball every 30–40 seconds, the session builder could show an _estimated duration_ alongside the live ball tally. "18 balls · ~11 min" is far more preparing than "18 balls" alone — it answers "do I have time for this before work?" which is a real planning question. This is additive, uses existing typography, and turns an abstract count into something schedulable.

## Session detail (Image 6)

This is the screen with the most untapped potential and it's currently the most administrative. "1 item · 18 balls" then "Repeat 6x · Club: 7-Iron · 18 balls" is a spreadsheet row. A golfer reading this on the drive over learns almost nothing about what their practice will _feel_ like.

What would make it preparing:

- **A sense of arc.** Show the session as an ordered list of what you'll actually do, in sequence, the way you'll do it — the instructions surfaced (not just the unit name), so the plan reads as a script you can follow on the mat.
- **The focus cue made prominent.** "Club face control" is the single most actionable thing in this whole unit, and right now it's buried one level down in the unit detail and absent from the session view entirely. The mental cue is what a golfer actually carries to the range. It deserves to be visible on the plan they'll glance at.
- **Total commitment up front.** Balls _and_ estimated time, so the plan feels sized.

## Settings (Images 9–11)

Settings are clean and correctly scoped. One small note in the clubs list (Images 10–11): "Ultra Lob Wedge (ULW)" and "11-Wood" exist in a 30-club catalogue, which is thorough, but the long scroll is pure administration. A "common bag" default or a search/filter would reduce the setup tax. Minor — this screen isn't where readiness is won or lost.

## The single highest-leverage change

If I could change one thing: **make the session detail screen a briefing, not a record.** Same data, reframed. Instead of "Repeat 6x · 18 balls," show the golfer the sequence they'll execute, the club in hand, the focus cue to hold in mind, and how long it'll take. Everything needed is already in the data model — focus cues, instructions, ball counts, clubs. The app collects all the right things; it just files them away instead of handing them back as a plan you can walk in and run.

That reframe — from _what you entered_ to _what you're about to do_ — is the difference between Rangework feeling like a database and feeling like the reason a golfer practices with purpose.

Want me to sketch a concrete redesign of the session detail screen (still Material 3, existing palette and type), so you can see the briefing framing applied?
