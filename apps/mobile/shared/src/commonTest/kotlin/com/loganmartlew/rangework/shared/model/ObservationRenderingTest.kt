package com.loganmartlew.rangework.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ObservationRenderingTest {

    // ── Strike grid ───────────────────────────────────────────────────────────

    @Test
    fun strikeColumnMirrorIsInvolution() {
        for (column in StrikeColumn.entries) {
            assertEquals(column, column.mirror().mirror())
        }
    }

    @Test
    fun strikeColumnsRightHandedAreIdentity() {
        assertEquals(
            listOf(StrikeColumn.HEEL, StrikeColumn.CENTER, StrikeColumn.TOE),
            strikeDisplayColumns(Handedness.RIGHT),
        )
    }

    @Test
    fun strikeColumnsLeftHandedMirror() {
        assertEquals(
            listOf(StrikeColumn.TOE, StrikeColumn.CENTER, StrikeColumn.HEEL),
            strikeDisplayColumns(Handedness.LEFT),
        )
    }

    @Test
    fun strikeRowsNeverMirror() {
        assertEquals(strikeDisplayRows(), listOf(StrikeRow.HIGH, StrikeRow.MIDDLE, StrikeRow.LOW))
    }

    // ── Shape grid ────────────────────────────────────────────────────────────

    @Test
    fun shapeDirectionMirrorIsInvolution() {
        for (direction in ShapeDirection.entries) {
            assertEquals(direction, direction.mirror().mirror())
        }
    }

    @Test
    fun shapeRowsRightHandedAreIdentity() {
        assertEquals(
            listOf(ShapeDirection.LEFT, ShapeDirection.STRAIGHT, ShapeDirection.RIGHT),
            shapeDisplayRows(Handedness.RIGHT),
        )
    }

    @Test
    fun shapeRowsLeftHandedMirror() {
        // The start line is a lateral axis, so it mirrors like the curve columns
        // — keeping Pull/Push in a consistent screen position across handednesses.
        assertEquals(
            listOf(ShapeDirection.RIGHT, ShapeDirection.STRAIGHT, ShapeDirection.LEFT),
            shapeDisplayRows(Handedness.LEFT),
        )
    }

    @Test
    fun shapeColumnsRightHandedAreIdentity() {
        assertEquals(
            listOf(ShapeDirection.LEFT, ShapeDirection.STRAIGHT, ShapeDirection.RIGHT),
            shapeDisplayColumns(Handedness.RIGHT),
        )
    }

    @Test
    fun shapeColumnsLeftHandedMirror() {
        assertEquals(
            listOf(ShapeDirection.RIGHT, ShapeDirection.STRAIGHT, ShapeDirection.LEFT),
            shapeDisplayColumns(Handedness.LEFT),
        )
    }

    // ── Golfer-term labels ──────────────────────────────────────────────────────

    @Test
    fun rightHandedCurveLabels() {
        assertEquals("Draw", ShapeFlight(ShapeDirection.STRAIGHT, ShapeDirection.LEFT).curveLabel(Handedness.RIGHT))
        assertEquals("Fade", ShapeFlight(ShapeDirection.STRAIGHT, ShapeDirection.RIGHT).curveLabel(Handedness.RIGHT))
        assertEquals("Straight", ShapeFlight(ShapeDirection.STRAIGHT, ShapeDirection.STRAIGHT).curveLabel(Handedness.RIGHT))
    }

    @Test
    fun leftHandedCurveLabelsAreMirrored() {
        // A physically leftward curve is a fade for a left-hander (draw for a right-hander).
        assertEquals("Fade", ShapeFlight(ShapeDirection.STRAIGHT, ShapeDirection.LEFT).curveLabel(Handedness.LEFT))
        assertEquals("Draw", ShapeFlight(ShapeDirection.STRAIGHT, ShapeDirection.RIGHT).curveLabel(Handedness.LEFT))
    }

    @Test
    fun rightHandedStartLabels() {
        assertEquals("Pull", ShapeFlight(ShapeDirection.LEFT, ShapeDirection.STRAIGHT).startLabel(Handedness.RIGHT))
        assertEquals("Push", ShapeFlight(ShapeDirection.RIGHT, ShapeDirection.STRAIGHT).startLabel(Handedness.RIGHT))
    }

    @Test
    fun leftHandedStartLabelsAreMirrored() {
        assertEquals("Push", ShapeFlight(ShapeDirection.LEFT, ShapeDirection.STRAIGHT).startLabel(Handedness.LEFT))
        assertEquals("Pull", ShapeFlight(ShapeDirection.RIGHT, ShapeDirection.STRAIGHT).startLabel(Handedness.LEFT))
    }

    @Test
    fun combinedGolferLabels() {
        assertEquals("Straight", ShapeFlight(ShapeDirection.STRAIGHT, ShapeDirection.STRAIGHT).golferLabel(Handedness.RIGHT))
        assertEquals("Draw", ShapeFlight(ShapeDirection.STRAIGHT, ShapeDirection.LEFT).golferLabel(Handedness.RIGHT))
        assertEquals("Push", ShapeFlight(ShapeDirection.RIGHT, ShapeDirection.STRAIGHT).golferLabel(Handedness.RIGHT))
        assertEquals("Pull Draw", ShapeFlight(ShapeDirection.LEFT, ShapeDirection.LEFT).golferLabel(Handedness.RIGHT))
    }
}
