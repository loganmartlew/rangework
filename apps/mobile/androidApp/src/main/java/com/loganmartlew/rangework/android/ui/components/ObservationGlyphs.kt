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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.Dp
import com.loganmartlew.rangework.shared.model.ContactValue
import com.loganmartlew.rangework.shared.model.DirectionValue
import com.loganmartlew.rangework.shared.model.DistanceValue
import com.loganmartlew.rangework.shared.model.Handedness
import com.loganmartlew.rangework.shared.model.ShapeDirection
import com.loganmartlew.rangework.shared.model.ShapeFlight
import com.loganmartlew.rangework.shared.model.StrikeLocation
import com.loganmartlew.rangework.shared.model.StrikeRow
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
 * A clubface (front view) with an impact dot at the strike zone. The hosel sits on
 * the heel side; the face art mirrors for a left-handed player so the heel is
 * always drawn where the heel column sits. The dot is placed by *screen* column
 * (already handedness-resolved by the caller), so it is not part of the mirror.
 * viewBox 48×34.
 */
@Composable
internal fun ClubfaceGlyph(
    location: StrikeLocation,
    handedness: Handedness,
    height: Dp,
    modifier: Modifier = Modifier,
) {
    val color = LocalContentColor.current
    val screenColumns = strikeDisplayColumns(handedness)
    val screenCol = screenColumns.indexOf(location.column)
    val screenRow = StrikeRow.entries.indexOf(location.row)
    val mirror = handedness == Handedness.LEFT
    val xs = floatArrayOf(15f, 25f, 35f)
    val ys = floatArrayOf(12.5f, 18f, 23.5f)
    GlyphCanvas(vbW = 48f, vbH = 34f, height = height, modifier = modifier) { s ->
        // Reflect x around the viewBox centre (translate(48,0) scale(-1,1)) for LH.
        fun rx(x: Float): Float = (if (mirror) 48f - x else x) * s
        fun ry(y: Float): Float = y * s

        val face = Path().apply {
            moveTo(rx(14f), ry(6f))
            lineTo(rx(38f), ry(8f))
            quadraticTo(rx(44f), ry(9f), rx(44f), ry(15f))
            lineTo(rx(43.5f), ry(23f))
            quadraticTo(rx(43f), ry(29f), rx(37f), ry(29f))
            lineTo(rx(10f), ry(29f))
            quadraticTo(rx(5f), ry(29f), rx(5.5f), ry(24f))
            lineTo(rx(11f), ry(9f))
            quadraticTo(rx(11.5f), ry(6f), rx(14f), ry(6f))
            close()
        }
        drawPath(
            face,
            color = color.copy(alpha = color.alpha * 0.8f),
            style = Stroke(width = 1.6f * s),
        )
        // Hosel (heel side).
        drawLine(
            color = color.copy(alpha = color.alpha * 0.8f),
            start = Offset(rx(12.5f), ry(6.5f)),
            end = Offset(rx(8f), ry(1.5f)),
            strokeWidth = 1.8f * s,
            cap = StrokeCap.Round,
        )
        // Grooves.
        val grooveColor = color.copy(alpha = color.alpha * 0.25f)
        drawLine(grooveColor, Offset(rx(14f), ry(14f)), Offset(rx(41f), ry(15f)), strokeWidth = 0.8f * s)
        drawLine(grooveColor, Offset(rx(12.5f), ry(19f)), Offset(rx(41f), ry(19.7f)), strokeWidth = 0.8f * s)
        drawLine(grooveColor, Offset(rx(11f), ry(24f)), Offset(rx(40f), ry(24.4f)), strokeWidth = 0.8f * s)
        // Impact dot — screen-positioned, outside the mirror.
        drawCircle(
            color = color,
            radius = 3f * s,
            center = Offset(xs[screenCol] * s, ys[screenRow] * s),
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
