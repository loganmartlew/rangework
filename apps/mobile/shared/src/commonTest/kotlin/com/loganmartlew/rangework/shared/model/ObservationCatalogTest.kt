package com.loganmartlew.rangework.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ObservationCatalogTest {

    @Test
    fun observationTypeRoundTripsAllStageOneIds() {
        val ids = listOf("success", "strike_location", "contact", "shape", "distance", "direction")
        for (id in ids) {
            val type = ObservationType.fromId(id)
            assertEquals(id, type?.id, "id $id should round-trip")
        }
        assertEquals(ids.size, ObservationType.entries.size, "no extra types beyond the Stage 1 catalog")
    }

    @Test
    fun observationTypeFromUnknownIdIsNull() {
        assertNull(ObservationType.fromId("rating"))
        assertNull(ObservationType.fromId(""))
        assertNull(ObservationType.fromId("SUCCESS"))
    }

    @Test
    fun successVocabularyIsExactlyHitMiss() {
        assertEquals(setOf("hit", "miss"), ObservationType.SUCCESS.vocabulary())
    }

    @Test
    fun contactVocabularyMatchesCatalog() {
        assertEquals(
            setOf("very_fat", "fat", "flush", "thin", "very_thin"),
            ObservationType.CONTACT.vocabulary(),
        )
    }

    @Test
    fun distanceVocabularyMatchesCatalog() {
        assertEquals(
            setOf("way_short", "short", "on", "long", "way_long"),
            ObservationType.DISTANCE.vocabulary(),
        )
    }

    @Test
    fun directionVocabularyMatchesCatalog() {
        assertEquals(
            setOf("way_left", "left", "on_line", "right", "way_right"),
            ObservationType.DIRECTION.vocabulary(),
        )
    }

    @Test
    fun strikeLocationHasNineHandednessNeutralValues() {
        val vocab = ObservationType.STRIKE_LOCATION.vocabulary()
        assertEquals(9, vocab.size)
        assertTrue("high_heel" in vocab)
        assertTrue("middle_center" in vocab)
        assertTrue("low_toe" in vocab)
        // Every id round-trips through the typed value.
        for (location in StrikeLocation.all) {
            assertEquals(location, StrikeLocation.fromId(location.id))
        }
        assertNull(StrikeLocation.fromId("high_shank"))
    }

    @Test
    fun shapeFlightHasNinePhysicalValues() {
        val vocab = ObservationType.SHAPE.vocabulary()
        assertEquals(9, vocab.size)
        assertTrue("straight_right" in vocab)
        assertTrue("left_left" in vocab)
        for (flight in ShapeFlight.all) {
            assertEquals(flight, ShapeFlight.fromId(flight.id))
        }
        assertNull(ShapeFlight.fromId("banana_slice"))
    }

    @Test
    fun acceptsChecksVocabularyMembership() {
        assertTrue(ObservationType.SUCCESS.accepts("hit"))
        assertFalse(ObservationType.SUCCESS.accepts("maybe"))
        assertTrue(ObservationType.SHAPE.accepts("straight_straight"))
        assertFalse(ObservationType.SHAPE.accepts("hit"))
    }

    @Test
    fun handednessRoundTripsWireValues() {
        assertEquals(Handedness.RIGHT, Handedness.fromId("RIGHT"))
        assertEquals(Handedness.LEFT, Handedness.fromId("LEFT"))
        assertNull(Handedness.fromId("AMBI"))
    }
}
