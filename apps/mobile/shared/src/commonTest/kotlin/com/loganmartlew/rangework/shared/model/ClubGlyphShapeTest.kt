package com.loganmartlew.rangework.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ClubGlyphShapeTest {

    @Test
    fun category_to_shape_mapping() {
        assertEquals(ClubGlyphShape.DRIVER, ClubCategory.DRIVER.toGlyphShape())
        assertEquals(ClubGlyphShape.WOOD, ClubCategory.WOOD.toGlyphShape())
        assertEquals(ClubGlyphShape.WOOD, ClubCategory.HYBRID.toGlyphShape())
        assertEquals(ClubGlyphShape.IRON, ClubCategory.IRON.toGlyphShape())
        assertEquals(ClubGlyphShape.IRON, ClubCategory.WEDGE.toGlyphShape())
        assertEquals(ClubGlyphShape.PUTTER, ClubCategory.PUTTER.toGlyphShape())
    }

    @Test
    fun null_category_falls_back_to_iron() {
        assertEquals(ClubGlyphShape.IRON, (null as ClubCategory?).toGlyphShape())
    }

    @Test
    fun all_six_categories_map_to_exactly_four_shapes() {
        val distinct = ClubCategory.entries.map { it.toGlyphShape() }.toSet()
        assertEquals(4, distinct.size)
        assertTrue(ClubGlyphShape.DRIVER in distinct)
        assertTrue(ClubGlyphShape.WOOD in distinct)
        assertTrue(ClubGlyphShape.IRON in distinct)
        assertTrue(ClubGlyphShape.PUTTER in distinct)
    }

    @Test
    fun every_shape_defines_nine_dot_positions() {
        for (shape in ClubGlyphShape.entries) {
            val g = shape.geometry()
            assertEquals(3, g.dotXs.size, "$shape dotXs")
            assertEquals(3, g.dotYs.size, "$shape dotYs")
        }
    }

    @Test
    fun every_shape_have_dots_within_face_bounds() {
        for (shape in ClubGlyphShape.entries) {
            val g = shape.geometry()
            val bounds = computeBounds(g.outline)
            for (x in g.dotXs) {
                assertTrue(x >= bounds.xMin && x <= bounds.xMax, "$shape dot x=$x outside [$bounds]")
            }
            for (y in g.dotYs) {
                assertTrue(y >= bounds.yMin && y <= bounds.yMax, "$shape dot y=$y outside [$bounds]")
            }
        }
    }

    @Test
    fun every_outline_is_closed() {
        for (shape in ClubGlyphShape.entries) {
            val g = shape.geometry()
            val last = g.outline.last()
            assertTrue(last is PathSegment.Close, "$shape outline should end with Close")
        }
    }

    @Test
    fun every_outline_has_non_empty_path() {
        for (shape in ClubGlyphShape.entries) {
            val g = shape.geometry()
            assertTrue(g.outline.size >= 5, "$shape outline should have at least 5 segments")
        }
    }

    @Test
    fun viewBox_is_consistent_across_shapes() {
        for (shape in ClubGlyphShape.entries) {
            val g = shape.geometry()
            assertEquals(48f, g.viewBoxWidth, "$shape viewBox width")
            assertEquals(34f, g.viewBoxHeight, "$shape viewBox height")
        }
    }

    @Test
    fun every_shape_has_hosel() {
        for (shape in ClubGlyphShape.entries) {
            assertNotNull(shape.geometry().hosel, "$shape should have hosel")
        }
    }

    @Test
    fun only_putter_has_sight_line() {
        assertNotNull(ClubGlyphShape.PUTTER.geometry().sightLine, "Putter should have sight line")
        for (shape in ClubGlyphShape.entries - ClubGlyphShape.PUTTER) {
            assertEquals(null, shape.geometry().sightLine, "$shape should not have sight line")
        }
    }

    @Test
    fun putter_has_no_grooves() {
        assertEquals(null, ClubGlyphShape.PUTTER.geometry().grooves)
    }

    @Test
    fun non_putters_have_grooves() {
        for (shape in ClubGlyphShape.entries - ClubGlyphShape.PUTTER) {
            assertNotNull(shape.geometry().grooves, "$shape should have grooves")
        }
    }

    private data class Bounds(val xMin: Float, val yMin: Float, val xMax: Float, val yMax: Float) {
        override fun toString() = "x[$xMin..$xMax] y[$yMin..$yMax]"
    }

    private fun computeBounds(outline: List<PathSegment>): Bounds {
        var xMin = Float.MAX_VALUE
        var yMin = Float.MAX_VALUE
        var xMax = Float.MIN_VALUE
        var yMax = Float.MIN_VALUE
        for (seg in outline) {
            when (seg) {
                is PathSegment.MoveTo -> { xMin = minOf(xMin, seg.x); xMax = maxOf(xMax, seg.x); yMin = minOf(yMin, seg.y); yMax = maxOf(yMax, seg.y) }
                is PathSegment.LineTo -> { xMin = minOf(xMin, seg.x); xMax = maxOf(xMax, seg.x); yMin = minOf(yMin, seg.y); yMax = maxOf(yMax, seg.y) }
                is PathSegment.QuadraticTo -> { xMin = minOf(xMin, seg.cx, seg.x); xMax = maxOf(xMax, seg.cx, seg.x); yMin = minOf(yMin, seg.cy, seg.y); yMax = maxOf(yMax, seg.cy, seg.y) }
                is PathSegment.Close -> {}
            }
        }
        return Bounds(xMin, yMin, xMax, yMax)
    }
}
