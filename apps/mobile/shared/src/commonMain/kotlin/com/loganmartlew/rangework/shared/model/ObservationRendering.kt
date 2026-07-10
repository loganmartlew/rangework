package com.loganmartlew.rangework.shared.model

/**
 * Handedness rendering transforms for the perspective-dependent Observation
 * surfaces. Pure functions consumed by the capture/history UI (Stages 5–6);
 * they never touch stored values (the D4 physical frame — see [ObservationCatalog]).
 *
 * `RIGHT` is the identity for every transform here. `LEFT` mirrors the *columns*
 * of the strike and shape grids (rows are handedness-independent) and flips the
 * golfer-term labels. Every mirror is an involution: applying it twice returns
 * the original layout.
 */

/** Face rows top-to-bottom — identical for both handednesses. */
fun strikeDisplayRows(): List<StrikeRow> = StrikeRow.entries.toList()

/**
 * Face columns left-to-right as the player looks down at the club face.
 * RIGHT: heel · center · toe. LEFT: toe · center · heel.
 */
fun strikeDisplayColumns(handedness: Handedness): List<StrikeColumn> =
    StrikeColumn.entries.toList().let { columns ->
        if (handedness == Handedness.LEFT) columns.map(StrikeColumn::mirror) else columns
    }

/**
 * Shape start-line rows top-to-bottom. Unlike the strike grid's high/middle/low
 * rows — a genuinely handedness-independent vertical axis — the start line is a
 * lateral (left/right) axis, so it mirrors for a left-handed player exactly like
 * the curve columns do, keeping a given start line (Pull / Push) in a consistent
 * screen position across handednesses. RIGHT: left · straight · right.
 * LEFT: right · straight · left.
 */
fun shapeDisplayRows(handedness: Handedness): List<ShapeDirection> =
    ShapeDirection.entries.toList().let { rows ->
        if (handedness == Handedness.LEFT) rows.map(ShapeDirection::mirror) else rows
    }

/**
 * Shape curvature columns left-to-right. RIGHT: left · straight · right.
 * LEFT: right · straight · left — so a player's draw/fade sits in a consistent
 * screen position across handednesses.
 */
fun shapeDisplayColumns(handedness: Handedness): List<ShapeDirection> =
    ShapeDirection.entries.toList().let { columns ->
        if (handedness == Handedness.LEFT) columns.map(ShapeDirection::mirror) else columns
    }

/** A physical direction re-expressed as if the player were right-handed. */
private fun ShapeDirection.asRightHanded(handedness: Handedness): ShapeDirection =
    if (handedness == Handedness.LEFT) mirror() else this

/**
 * The curvature term a golfer would use: Draw (curves to the target side),
 * Fade (curves away), or Straight. For a right-handed player a leftward curve is
 * a draw; the mapping flips for a left-handed player.
 */
fun ShapeFlight.curveLabel(handedness: Handedness): String =
    when (curve.asRightHanded(handedness)) {
        ShapeDirection.LEFT -> "Draw"
        ShapeDirection.RIGHT -> "Fade"
        ShapeDirection.STRAIGHT -> "Straight"
    }

/**
 * The start-line term a golfer would use: Pull (started left of target for a
 * right-hander), Push (started right), or Straight. Flips for a left-hander.
 */
fun ShapeFlight.startLabel(handedness: Handedness): String =
    when (start.asRightHanded(handedness)) {
        ShapeDirection.LEFT -> "Pull"
        ShapeDirection.RIGHT -> "Push"
        ShapeDirection.STRAIGHT -> "Straight"
    }

/**
 * The combined golfer-term label for a flight, e.g. "Pull Draw", "Fade",
 * "Push", or "Straight". Presentation-level; the individual [startLabel] /
 * [curveLabel] carry the load-bearing meaning.
 */
fun ShapeFlight.golferLabel(handedness: Handedness): String {
    val startTerm = startLabel(handedness)
    val curveTerm = curveLabel(handedness)
    val startStraight = start == ShapeDirection.STRAIGHT
    val curveStraight = curve == ShapeDirection.STRAIGHT
    return when {
        startStraight && curveStraight -> "Straight"
        curveStraight -> startTerm
        startStraight -> curveTerm
        else -> "$startTerm $curveTerm"
    }
}
