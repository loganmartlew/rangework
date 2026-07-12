package com.loganmartlew.rangework.shared.model

import kotlin.math.hypot

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

/** A single groove line at a given height, with its own horizontal extent so it can taper to match the outline. */
data class GrooveLine(val xStart: Float, val xEnd: Float, val y: Float)

/** Groove lines drawn across the face. */
data class Grooves(
    val lines: List<GrooveLine>,
    val strokeWidth: Float = 0.8f,
)

/**
 * Builds a straight-edged polygon outline with every corner rounded by
 * [radius]: each vertex is replaced by a quadratic curve between the two
 * points `radius` back along its adjacent edges (control point = the
 * original vertex), clamped so a corner never eats more than half of
 * either adjacent edge.
 */
private fun roundedPolygon(points: List<Pair<Float, Float>>, radius: Float): List<PathSegment> {
    val n = points.size
    val segments = mutableListOf<PathSegment>()
    for (i in points.indices) {
        val (px, py) = points[(i - 1 + n) % n]
        val (cx, cy) = points[i]
        val (nx, ny) = points[(i + 1) % n]
        val toPrevX = px - cx
        val toPrevY = py - cy
        val toNextX = nx - cx
        val toNextY = ny - cy
        val dPrev = hypot(toPrevX, toPrevY).let { if (it > 0f) it else 1f }
        val dNext = hypot(toNextX, toNextY).let { if (it > 0f) it else 1f }
        val rr = minOf(radius, dPrev / 2f, dNext / 2f).coerceAtLeast(0f)
        val inX = cx + toPrevX / dPrev * rr
        val inY = cy + toPrevY / dPrev * rr
        val outX = cx + toNextX / dNext * rr
        val outY = cy + toNextY / dNext * rr
        segments += if (i == 0) PathSegment.MoveTo(inX, inY) else PathSegment.LineTo(inX, inY)
        segments += PathSegment.QuadraticTo(cx, cy, outX, outY)
    }
    segments += PathSegment.Close
    return segments
}

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
    // Straight-edged trapezoid — wide crown, narrower sole — with rounded
    // corners, ported from a user-supplied reference SVG's true corner
    // points (their small corner fillets discarded; [roundedPolygon]
    // reproduces the rounding from the polygon + a radius instead).
    ClubGlyphShape.DRIVER -> ClubGlyphGeometry(
        viewBoxWidth = 48f,
        viewBoxHeight = 34f,
        outline = roundedPolygon(
            points = listOf(5f to 8.4f, 43f to 8.4f, 39.4f to 25.6f, 8.6f to 25.6f),
            radius = 7f,
        ),
        dotXs = listOf(14f, 24.5f, 35f),
        dotYs = listOf(12f, 17f, 22f),
        hosel = LineMark(x1 = 7.11f, y1 = 10.11f, x2 = 4.11f, y2 = 4.91f, strokeWidth = 2f),
        sightLine = null,
        grooves = Grooves(
            lines = listOf(
                GrooveLine(13f, 35f, 12.5f),
                GrooveLine(13f, 35f, 15.5f),
                GrooveLine(13f, 35f, 18.5f),
                GrooveLine(13f, 35f, 21.5f),
            ),
            strokeWidth = 0.8f,
        ),
    )

    // Same trapezoid family as the driver, just much shallower.
    ClubGlyphShape.WOOD -> ClubGlyphGeometry(
        viewBoxWidth = 48f,
        viewBoxHeight = 34f,
        outline = roundedPolygon(
            points = listOf(5f to 12f, 43f to 12f, 40.3f to 25.4f, 7.7f to 25.4f),
            radius = 7f,
        ),
        dotXs = listOf(14f, 24.5f, 35f),
        dotYs = listOf(15f, 18.7f, 22.5f),
        hosel = LineMark(x1 = 7.05f, y1 = 13.68f, x2 = 4.05f, y2 = 8.48f, strokeWidth = 2f),
        sightLine = null,
        grooves = Grooves(
            lines = listOf(
                GrooveLine(12f, 36f, 16.2f),
                GrooveLine(12f, 36f, 18.7f),
                GrooveLine(12f, 36f, 21.2f),
            ),
            strokeWidth = 0.8f,
        ),
    )

    // Roughly a triangle: short heel edge, long toe edge (~2.6x the heel),
    // flat in-line sole, diagonal topline closing toe-top to heel-top.
    ClubGlyphShape.IRON -> ClubGlyphGeometry(
        viewBoxWidth = 48f,
        viewBoxHeight = 34f,
        outline = roundedPolygon(
            points = listOf(10.34f to 21.23f, 11.34f to 29f, 37.67f to 29f, 37.67f to 9f),
            radius = 6f,
        ),
        dotXs = listOf(15f, 24f, 34f),
        dotYs = listOf(15f, 20f, 25f),
        hosel = LineMark(x1 = 11.36f, y1 = 21.8f, x2 = 8.36f, y2 = 16.61f, strokeWidth = 2f),
        sightLine = null,
        // The toe edge (right) sits at a constant x, but the topline tapers
        // the left boundary in toward the toe as y decreases, so each
        // higher line starts further right to stay inside the outline.
        grooves = Grooves(
            lines = listOf(
                GrooveLine(14f, 34f, 25f),
                GrooveLine(14f, 34f, 22.5f),
                GrooveLine(18.5f, 34f, 20f),
                GrooveLine(24f, 34f, 17.5f),
                GrooveLine(29.5f, 34f, 15f),
            ),
            strokeWidth = 0.7f,
        ),
    )

    // Simple rounded rectangle; the sight line is a short vertical alignment
    // tick at face centre rather than a horizontal line (which read as a groove).
    ClubGlyphShape.PUTTER -> ClubGlyphGeometry(
        viewBoxWidth = 48f,
        viewBoxHeight = 34f,
        outline = roundedPolygon(
            points = listOf(5f to 13f, 43f to 13f, 43f to 21.79f, 5f to 21.79f),
            radius = 3.1f,
        ),
        dotXs = listOf(13f, 24f, 35f),
        dotYs = listOf(15.5f, 17.5f, 19.5f),
        hosel = LineMark(x1 = 7.05f, y1 = 12.68f, x2 = 4.05f, y2 = 7.48f, strokeWidth = 2f),
        sightLine = LineMark(x1 = 24f, y1 = 14f, x2 = 24f, y2 = 18.5f, strokeWidth = 1.6f),
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
