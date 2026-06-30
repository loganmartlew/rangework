package com.loganmartlew.rangework.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class TagSlugTest {
    @Test
    fun lowercasesAndUnderscoresSpaces() {
        assertEquals("short_game", slugifyTag("Short Game"))
    }

    @Test
    fun trimsAndCollapsesPunctuationAndWhitespace() {
        assertEquals("short_game", slugifyTag("  Short — Game!!  "))
        assertEquals("short_game", slugifyTag("short___game"))
        assertEquals("short_game", slugifyTag("Short\tGame"))
    }

    @Test
    fun stripsLeadingAndTrailingNonAlphanumerics() {
        assertEquals("putting", slugifyTag("**putting**"))
        assertEquals("putting", slugifyTag("_putting_"))
    }

    @Test
    fun isIdempotentOnAnAlreadyValidSlug() {
        assertEquals("full_swing", slugifyTag("full_swing"))
    }

    @Test
    fun returnsNullForInputWithNoAlphanumericContent() {
        assertNull(slugifyTag("   "))
        assertNull(slugifyTag("---"))
        assertNull(slugifyTag(""))
    }

    @Test
    fun keepsDigits() {
        assertEquals("3_wood_work", slugifyTag("3 Wood work"))
    }
}
