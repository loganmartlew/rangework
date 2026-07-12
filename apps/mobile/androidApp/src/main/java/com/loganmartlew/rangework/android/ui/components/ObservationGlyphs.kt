package com.loganmartlew.rangework.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import com.loganmartlew.rangework.shared.model.ClubGlyphGeometry
import com.loganmartlew.rangework.shared.model.ClubGlyphShape
import com.loganmartlew.rangework.shared.model.ContactValue
import com.loganmartlew.rangework.shared.model.DirectionValue
import com.loganmartlew.rangework.shared.model.DistanceValue
import com.loganmartlew.rangework.shared.model.Handedness
import com.loganmartlew.rangework.shared.model.PathSegment
import com.loganmartlew.rangework.shared.model.ShapeDirection
import com.loganmartlew.rangework.shared.model.ShapeFlight
import com.loganmartlew.rangework.shared.model.StrikeLocation
import com.loganmartlew.rangework.shared.model.StrikeRow
import com.loganmartlew.rangework.shared.model.geometry
import com.loganmartlew.rangework.shared.model.strikeDisplayColumns

/**
 * The Stage 5 value-glyph set (design D3), drawn in Compose. Each is a small
 * monochrome illustration of what a value physically *is* — judgement-free (always
 * [LocalContentColor], never red/green): a start-line arrow, a landing dot, a
 * contact point, a top-down flight path, a clubface with an impact dot, and the
 * nine-dot mini-grid launcher marker.
 *
 * Geometry is ported verbatim from the prototype's SVGs (their viewBoxes are the
 * reference geometry per the design review), so a side-by-side check against
 * `prototype.html` holds. Coordinates are expressed in viewBox units and scaled to
 * the requested [height]; each glyph derives its width from the viewBox aspect
 * ratio. The parametric ones (flight, clubface) mirror by construction for a
 * left-handed player rather than needing per-value assets.
 */

// ── Chip glyphs (viewBox 28×18) ───────────────────────────────────────────────

/** Start-line arrow from the ball, angled per value. Physical — never flips (D4). */
@Composable
internal fun DirectionGlyph(value: DirectionValue, height: Dp, modifier: Modifier = Modifier) {
    val color = LocalContentColor.current
    val (x, y) = when (value) {
        DirectionValue.WAY_LEFT -> 3.5f to 6.5f
        DirectionValue.LEFT -> 8.5f to 3.5f
        DirectionValue.ON_LINE -> 14f to 2.5f
        DirectionValue.RIGHT -> 19.5f to 3.5f
        DirectionValue.WAY_RIGHT -> 24.5f to 6.5f
    }
    GlyphCanvas(vbW = 28f, vbH = 18f, height = height, modifier = modifier) { s ->
        drawLine(
            color = color,
            start = Offset(14f * s, 15.5f * s),
            end = Offset(x * s, y * s),
            strokeWidth = 1.8f * s,
            cap = StrokeCap.Round,
        )
        drawCircle(color = color, radius = 2.1f * s, center = Offset(x * s, y * s))
    }
}

/** Landing spot (dot) relative to the target-depth line (dashed). */
@Composable
internal fun DistanceGlyph(value: DistanceValue, height: Dp, modifier: Modifier = Modifier) {
    val color = LocalContentColor.current
    val y = when (value) {
        DistanceValue.WAY_SHORT -> 15.5f
        DistanceValue.SHORT -> 12.5f
        DistanceValue.ON -> 9f
        DistanceValue.LONG -> 5.5f
        DistanceValue.WAY_LONG -> 2.5f
    }
    GlyphCanvas(vbW = 28f, vbH = 18f, height = height, modifier = modifier) { s ->
        drawLine(
            color = color.copy(alpha = color.alpha * 0.55f),
            start = Offset(6f * s, 9f * s),
            end = Offset(22f * s, 9f * s),
            strokeWidth = 1.2f * s,
            pathEffect = dash(2.5f * s, 2.5f * s),
        )
        drawCircle(color = color, radius = 2.4f * s, center = Offset(14f * s, y * s))
    }
}

/** Side view: ground line + ball; the dot is where the club met turf/ball. */
@Composable
internal fun ContactGlyph(value: ContactValue, height: Dp, modifier: Modifier = Modifier) {
    val color = LocalContentColor.current
    val (x, y) = when (value) {
        ContactValue.VERY_FAT -> 6f to 14.6f
        ContactValue.FAT -> 10.5f to 13.6f
        ContactValue.FLUSH -> 15f to 12.4f
        ContactValue.THIN -> 18f to 9.6f
        ContactValue.VERY_THIN -> 16.8f to 6.9f
    }
    GlyphCanvas(vbW = 28f, vbH = 18f, height = height, modifier = modifier) { s ->
        drawLine(
            color = color.copy(alpha = color.alpha * 0.45f),
            start = Offset(2f * s, 13f * s),
            end = Offset(26f * s, 13f * s),
            strokeWidth = 1.2f * s,
        )
        drawCircle(
            color = color.copy(alpha = color.alpha * 0.7f),
            radius = 3f * s,
            center = Offset(15f * s, 9.8f * s),
            style = Stroke(width = 1.3f * s),
        )
        drawCircle(color = color, radius = 1.9f * s, center = Offset(x * s, y * s))
    }
}

// ── Grid-value glyphs ─────────────────────────────────────────────────────────

/**
 * Top-down ball flight against a dashed target line, drawn from the *physical*
 * start/curve, so a lefty's Pull Draw visibly flies right (labels stay put, curves
 * mirror with reality). viewBox 40×48.
 */
@Composable
internal fun FlightGlyph(flight: ShapeFlight, height: Dp, modifier: Modifier = Modifier) {
    val color = LocalContentColor.current
    val s0 = flight.start.axis()
    val c0 = flight.curve.axis()
    val x3 = 20f + s0 * 9f + c0 * 9f
    GlyphCanvas(vbW = 40f, vbH = 48f, height = height, modifier = modifier) { s ->
        drawLine(
            color = color.copy(alpha = color.alpha * 0.28f),
            start = Offset(20f * s, 43f * s),
            end = Offset(20f * s, 6f * s),
            strokeWidth = 1f * s,
            pathEffect = dash(2f * s, 3f * s),
        )
        val path = Path().apply {
            moveTo(20f * s, 43f * s)
            cubicTo(
                (20f + s0 * 6f) * s, 31f * s,
                (20f + s0 * 9f + c0 * 2.5f) * s, 17f * s,
                x3 * s, 6f * s,
            )
        }
        drawPath(path, color = color, style = Stroke(width = 2.2f * s, cap = StrokeCap.Round))
        drawCircle(color = color, radius = 2f * s, center = Offset(x3 * s, 6f * s))
        drawCircle(
            color = color.copy(alpha = color.alpha * 0.6f),
            radius = 1.7f * s,
            center = Offset(20f * s, 43f * s),
        )
    }
}

/**
 * A clubface (front view) with an impact dot at the strike zone. The face outline,
 * hosel/sight-line marks, and grooves are selected from the per-shape geometry data
 * in [shared] and walked onto a Compose Canvas by a single generic renderer. The
 * outline is stored heel-left; it mirrors for a right-handed player so the hosel
 * sits on the correct side. The dot is placed by *screen* column (already
 * handedness-resolved by the caller), outside the mirror — identical to the
 * original single-face renderer. viewBox 48×34 for every shape.
 */
@Composable
internal fun ClubfaceGlyph(
    location: StrikeLocation,
    shape: ClubGlyphShape,
    handedness: Handedness,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val color = LocalContentColor.current
    val geometry = shape.geometry()
    val screenColumns = strikeDisplayColumns(handedness)
    val screenCol = screenColumns.indexOf(location.column)
    val screenRow = StrikeRow.entries.indexOf(location.row)
    val mirror = handedness == Handedness.RIGHT
    GlyphCanvas(
        vbW = geometry.viewBoxWidth,
        vbH = geometry.viewBoxHeight,
        height = height,
        modifier = modifier,
    ) { s ->
        fun rx(x: Float): Float = (if (mirror) geometry.viewBoxWidth - x else x) * s
        fun ry(y: Float): Float = y * s

        val face = buildPath(geometry.outline, ::rx, ::ry)
        drawPath(
            face,
            color = color.copy(alpha = color.alpha * 0.8f),
            style = Stroke(width = 1.6f * s, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )

        // Hosel or sight-line mark.
        geometry.hosel?.let { h ->
            drawLine(
                color = color.copy(alpha = color.alpha * 0.8f),
                start = Offset(rx(h.x1), ry(h.y1)),
                end = Offset(rx(h.x2), ry(h.y2)),
                strokeWidth = h.strokeWidth * s,
                cap = StrokeCap.Round,
            )
        }
        geometry.sightLine?.let { sl ->
            drawLine(
                color = color.copy(alpha = color.alpha * 0.3f),
                start = Offset(rx(sl.x1), ry(sl.y1)),
                end = Offset(rx(sl.x2), ry(sl.y2)),
                strokeWidth = sl.strokeWidth * s,
                cap = StrokeCap.Round,
            )
        }

        // Grooves.
        geometry.grooves?.let { g ->
            val grooveColor = color.copy(alpha = color.alpha * 0.25f)
            for (line in g.lines) {
                drawLine(
                    grooveColor,
                    Offset(rx(line.xStart), ry(line.y)),
                    Offset(rx(line.xEnd), ry(line.y)),
                    strokeWidth = g.strokeWidth * s,
                    cap = StrokeCap.Round,
                )
            }
        }

        // Impact dot — screen-positioned, outside the mirror.
        drawCircle(
            color = color,
            radius = 3f * s,
            center = Offset(geometry.dotXs[screenCol] * s, geometry.dotYs[screenRow] * s),
        )
    }
}

/** The nine-dot marker on an un-staged grid launcher row. Square. */
@Composable
internal fun MiniGridGlyph(height: Dp, modifier: Modifier = Modifier) {
    val color = LocalContentColor.current.copy(alpha = LocalContentColor.current.alpha * 0.55f)
    GlyphCanvas(vbW = 18f, vbH = 18f, height = height, modifier = modifier) { s ->
        val positions = floatArrayOf(4f, 9f, 14f)
        for (cy in positions) {
            for (cx in positions) {
                drawCircle(color = color, radius = 1.4f * s, center = Offset(cx * s, cy * s))
            }
        }
    }
}

// ── Internals ─────────────────────────────────────────────────────────────────

/** Walks the platform-neutral [PathSegment] list onto a Compose [Path]. */
private fun buildPath(
    segments: List<PathSegment>,
    xf: (Float) -> Float,
    yf: (Float) -> Float,
): Path = Path().apply {
    for (seg in segments) {
        when (seg) {
            is PathSegment.MoveTo -> moveTo(xf(seg.x), yf(seg.y))
            is PathSegment.LineTo -> lineTo(xf(seg.x), yf(seg.y))
            is PathSegment.QuadraticTo -> quadraticTo(xf(seg.cx), yf(seg.cy), xf(seg.x), yf(seg.y))
            is PathSegment.Close -> close()
        }
    }
}

private fun ShapeDirection.axis(): Float = when (this) {
    ShapeDirection.LEFT -> -1f
    ShapeDirection.STRAIGHT -> 0f
    ShapeDirection.RIGHT -> 1f
}

private fun DrawScope.dash(on: Float, off: Float): PathEffect =
    PathEffect.dashPathEffect(floatArrayOf(on, off), 0f)

@Composable
private fun GlyphCanvas(
    vbW: Float,
    vbH: Float,
    height: Dp,
    modifier: Modifier = Modifier,
    draw: DrawScope.(scale: Float) -> Unit,
) {
    val width = height * (vbW / vbH)
    Canvas(modifier = modifier.size(width, height)) {
        // Aspect ratio matches the viewBox, so a single scale maps both axes.
        val scale = size.height / vbH
        draw(scale)
    }
}
