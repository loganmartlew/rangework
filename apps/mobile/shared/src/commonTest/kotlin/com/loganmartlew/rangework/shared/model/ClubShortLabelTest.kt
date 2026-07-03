package com.loganmartlew.rangework.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals

class ClubShortLabelTest {

    @Test
    fun wedges_use_parenthesised_abbreviation() {
        assertEquals("PW", clubShortLabel("Pitching Wedge (PW)"))
        assertEquals("AW", clubShortLabel("Approach Wedge (AW)"))
        assertEquals("ULW", clubShortLabel("Ultra Lob Wedge (ULW)"))
    }

    @Test
    fun numbered_clubs_compress_to_number_plus_initial() {
        assertEquals("7I", clubShortLabel("7-Iron"))
        assertEquals("3W", clubShortLabel("3-Wood"))
        assertEquals("4H", clubShortLabel("4-Hybrid"))
        assertEquals("11W", clubShortLabel("11-Wood"))
    }

    @Test
    fun driver_and_putter_abbreviate() {
        assertEquals("DR", clubShortLabel("Driver"))
        assertEquals("PT", clubShortLabel("Putter"))
    }

    @Test
    fun unrecognised_names_pass_through() {
        assertEquals("Chipper", clubShortLabel("Chipper"))
        assertEquals("", clubShortLabel(""))
    }
}
