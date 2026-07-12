package com.loganmartlew.rangework.shared.model

/**
 * The four club-family faces that replace the single generic [ClubfaceGlyph].
 * Each owns its own outline path, nine impact-dot positions matched to that
 * club's real face extent, and any marks (hosel, putter sight line). The
 * geometry is platform-neutral data so a future iOS renderer reuses it
 * without re-porting four faces (ADR 0006).
 */
enum class ClubGlyphShape {
    DRIVER, WOOD, IRON, PUTTER;

    companion object
}

/** A single segment of a path outline. */
sealed class PathSegment {
    data class MoveTo(val x: Float, val y: Float) : PathSegment()
    data class LineTo(val x: Float, val y: Float) : PathSegment()
    data class QuadraticTo(val cx: Float, val cy: Float, val x: Float, val y: Float) : PathSegment()
    data object Close : PathSegment()
}

/** A straight mark (hosel or putter sight line). */
data class LineMark(
    val x1: Float,
    val y1: Float,
    val x2: Float,
    val y2: Float,
    val strokeWidth: Float = 1.8f,
)

/** Groove lines: y-positions and horizontal extent. */
data class Grooves(
    val yPositions: List<Float>,
    val xStart: Float,
    val xEnd: Float,
    val strokeWidth: Float = 0.8f,
)

/**
 * The platform-neutral geometry for one club-family face. All coordinates are
 * in the canonical heel-left orientation (looks correct for a left-handed player
 * without mirror); the renderer mirrors the outline and marks for right-handed
 * players. Dot positions ([dotXs] / [dotYs]) are indexed by screen column / row
 * (already handedness-resolved by the caller) and placed outside the mirror.
 */
data class ClubGlyphGeometry(
    val viewBoxWidth: Float,
    val viewBoxHeight: Float,
    val outline: List<PathSegment>,
    /** Three x-positions, indexed by screen column (0=heel-side, 2=toe-side). */
    val dotXs: List<Float>,
    /** Three y-positions, indexed by screen row (0=high, 1=middle, 2=low). */
    val dotYs: List<Float>,
    val hosel: LineMark?,
    val sightLine: LineMark?,
    val grooves: Grooves?,
)

fun ClubGlyphShape.geometry(): ClubGlyphGeometry = when (this) {
    ClubGlyphShape.DRIVER -> ClubGlyphGeometry(
        viewBoxWidth = 48f,
        viewBoxHeight = 34f,
        outline = listOf(
            PathSegment.MoveTo(8f, 4f),
            PathSegment.QuadraticTo(8f, 1f, 18f, 1f),
            PathSegment.LineTo(36f, 2f),
            PathSegment.QuadraticTo(44f, 3f, 44f, 8f),
            PathSegment.LineTo(43f, 23f),
            PathSegment.QuadraticTo(42f, 30f, 35f, 30f),
            PathSegment.LineTo(12f, 29f),
            PathSegment.QuadraticTo(5f, 28f, 6f, 22f),
            PathSegment.Close,
        ),
        dotXs = listOf(14f, 27f, 40f),
        dotYs = listOf(6f, 16f, 25f),
        hosel = LineMark(x1 = 9f, y1 = 4f, x2 = 4f, y2 = -1f, strokeWidth = 1.8f),
        sightLine = null,
        grooves = Grooves(
            yPositions = listOf(9f, 16f, 23f),
            xStart = 11f,
            xEnd = 40f,
            strokeWidth = 0.8f,
        ),
    )

    ClubGlyphShape.WOOD -> ClubGlyphGeometry(
        viewBoxWidth = 48f,
        viewBoxHeight = 34f,
        outline = listOf(
            PathSegment.MoveTo(10f, 8f),
            PathSegment.QuadraticTo(10f, 4f, 18f, 3f),
            PathSegment.LineTo(34f, 4f),
            PathSegment.QuadraticTo(42f, 5f, 42f, 10f),
            PathSegment.LineTo(41f, 20f),
            PathSegment.QuadraticTo(40f, 26f, 34f, 27f),
            PathSegment.LineTo(14f, 26f),
            PathSegment.QuadraticTo(8f, 25f, 8f, 20f),
            PathSegment.Close,
        ),
        dotXs = listOf(15f, 27f, 38f),
        dotYs = listOf(8f, 15f, 23f),
        hosel = LineMark(x1 = 11f, y1 = 7f, x2 = 6f, y2 = 2f, strokeWidth = 1.8f),
        sightLine = null,
        grooves = Grooves(
            yPositions = listOf(10f, 16f, 22f),
            xStart = 12f,
            xEnd = 38f,
            strokeWidth = 0.8f,
        ),
    )

    ClubGlyphShape.IRON -> ClubGlyphGeometry(
        viewBoxWidth = 48f,
        viewBoxHeight = 34f,
        outline = listOf(
            PathSegment.MoveTo(18f, 4f),
            PathSegment.LineTo(32f, 5f),
            PathSegment.QuadraticTo(36f, 6f, 35f, 10f),
            PathSegment.LineTo(34f, 23f),
            PathSegment.QuadraticTo(33f, 28f, 29f, 28f),
            PathSegment.LineTo(17f, 27f),
            PathSegment.QuadraticTo(13f, 26f, 14f, 22f),
            PathSegment.LineTo(16f, 8f),
            PathSegment.QuadraticTo(17f, 4f, 18f, 4f),
            PathSegment.Close,
        ),
        dotXs = listOf(18f, 26f, 33f),
        dotYs = listOf(8f, 16f, 24f),
        hosel = LineMark(x1 = 16f, y1 = 4f, x2 = 12f, y2 = -1f, strokeWidth = 1.8f),
        sightLine = null,
        grooves = Grooves(
            yPositions = listOf(10f, 15f, 20f, 25f),
            xStart = 16f,
            xEnd = 34f,
            strokeWidth = 0.7f,
        ),
    )

    ClubGlyphShape.PUTTER -> ClubGlyphGeometry(
        viewBoxWidth = 48f,
        viewBoxHeight = 34f,
        outline = listOf(
            PathSegment.MoveTo(8f, 14f),
            PathSegment.LineTo(42f, 14f),
            PathSegment.QuadraticTo(45f, 14f, 45f, 17f),
            PathSegment.LineTo(45f, 23.5f),
            PathSegment.QuadraticTo(45f, 26.5f, 42f, 26.5f),
            PathSegment.LineTo(8f, 26.5f),
            PathSegment.QuadraticTo(5f, 26.5f, 5f, 23.5f),
            PathSegment.LineTo(5f, 17f),
            PathSegment.QuadraticTo(5f, 14f, 8f, 14f),
            PathSegment.Close,
        ),
        dotXs = listOf(11f, 25f, 39f),
        dotYs = listOf(15.5f, 20.5f, 25f),
        hosel = null,
        sightLine = LineMark(x1 = 10f, y1 = 20.5f, x2 = 40f, y2 = 20.5f, strokeWidth = 1.2f),
        grooves = null,
    )
}

/**
 * Resolve a club category to its glyph shape. Null (unknown club) falls back to
 * iron — the neutral fallback; a club code not in [enabledClubs] resolves the
 * same way. Six categories collapse to four shapes as:
 *   DRIVER → driver, WOOD/HYBRID → wood, IRON/WEDGE → iron, PUTTER → putter.
 */
fun ClubCategory?.toGlyphShape(): ClubGlyphShape = when (this) {
    ClubCategory.DRIVER -> ClubGlyphShape.DRIVER
    ClubCategory.WOOD -> ClubGlyphShape.WOOD
    ClubCategory.HYBRID -> ClubGlyphShape.WOOD
    ClubCategory.IRON -> ClubGlyphShape.IRON
    ClubCategory.WEDGE -> ClubGlyphShape.IRON
    ClubCategory.PUTTER -> ClubGlyphShape.PUTTER
    null -> ClubGlyphShape.IRON
}
