# Driver is its own club category, separate from Wood

The clubs catalog splits Driver out of the `WOOD` category into a new `DRIVER`
`ClubCategory` (also home to a new Mini Driver club), rather than leaving driver as a
Wood distinguished only by its Club Code. Modern driver heads are functionally and
visually distinct from fairway woods, and the split lets the per-club strike-glyph shape
derive directly from `category` (`DRIVER` → driver face) with **no** `code == "driver"`
special-case — the same reason Mini Driver joins the category rather than being pattern-matched.

Coupling worth flagging: `category` is stored as text in `public.clubs` (a
`clubs_category_check` constraint) and deserialized straight into the KMP `ClubCategory`
enum. The enum value and the DB constraint must therefore ship **together** — an app build
without `DRIVER` in the enum throws when it reads a `DRIVER`-category row. On this
solo-maintained project the deploy ordering is manageable, but it is not free.

## Status

accepted

## Considered options

- **Keep driver in `WOOD`, special-case by Club Code (rejected).** The glyph mapping would
  branch on `code == "driver"` inside a `WOOD` category — fragile, and Mini Driver would
  need the identical special-case. The category split makes the mapping fall out of the
  domain enum instead.
- **New `DRIVER` category (chosen).** Shape derives from category; Mini Driver joins naturally.

## Consequences

- The `ClubCategory` enum, the `clubs_category_check` constraint, and the `driver` row's
  category all change together in one migration; `mini_driver` is seeded under `DRIVER`,
  with the catalog `sort_order` renumbered so the Driver family (driver, mini driver) leads
  the woods.
- Any exhaustive `when (category)` in the codebase must gain a `DRIVER` branch.
- Six categories now collapse to four strike-glyph shapes (`DRIVER`→driver, `WOOD`/`HYBRID`→wood,
  `IRON`/`WEDGE`→iron, `PUTTER`→putter); see ADR 0006.
