# Clubface strike-glyph geometry lives in `shared` as platform-neutral data

The strike-location glyph renders one of four club-family faces — driver, wood, iron,
putter — each with its own outline **and** its own nine impact-dot positions matched to
that club's real face extent (a putter's dots spread wide and short; a driver's spread
taller). Unlike the other five observation glyphs (Direction, Distance, Contact, Flight,
MiniGrid), which hardcode their Compose Canvas geometry inline in `androidApp`, the
clubface's geometry is described as **platform-neutral data in `shared`** — viewBox, the
outline as a list of path commands, the nine dot positions, and any sight-line/hosel marks
— and `androidApp` renders it with a single generic Canvas renderer. This is so a future
iOS app reuses the shape geometry through the shared KMP module instead of hand-porting
four faces (the richest glyph in the set) into SwiftUI/CoreGraphics.

The club-family classification (`ClubGlyphShape` and the `ClubCategory` → shape mapping)
lives in `shared` for the same reason. Handedness mirroring and screen-column dot
resolution stay in the per-platform renderer, not in the data.

## Status

accepted

## Considered options

- **Draw inline in `androidApp` like the other five glyphs (rejected).** Consistent with
  the sibling glyphs, but iOS would re-port the richest glyph by hand — four outlines plus
  four dot grids — which is exactly the duplication this feature was asked to avoid.
- **Geometry-as-shared-data, clubface only (chosen).** Eliminates the duplication that
  bites hardest. The five simpler glyphs stay Android-only for now.

## Consequences

- **Deliberate inconsistency:** one glyph's geometry lives in `shared`, five don't. Accepted;
  revisit — and consider migrating the others — if and when an iOS app materialises.
- The per-ball resolved `ClubCategory` (override → step club → unit default → null) must be
  threaded down to the glyph through `ObservationCaptureSection`, `GridLauncherRow` /
  `GridValueGlyph`, `ObservationGridDialog`, and the ball edit sheet. A null club falls back
  to the iron shape.
- `prototype.html` (the geometry reference-of-record, with a stated side-by-side invariant)
  must be updated in lockstep to carry the four faces.
