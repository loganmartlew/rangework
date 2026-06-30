package com.loganmartlew.rangework.shared.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TagFilterTest {
    @Test
    fun emptySelectionMatchesEverything() {
        assertTrue(tagFilterMatches(itemTagIds = emptyList(), selectedTagIds = emptySet()))
        assertTrue(tagFilterMatches(itemTagIds = listOf("a"), selectedTagIds = emptySet()))
    }

    @Test
    fun matchesWhenItemHasAnySelectedTag() {
        assertTrue(tagFilterMatches(itemTagIds = listOf("a", "b"), selectedTagIds = setOf("b", "c")))
    }

    @Test
    fun doesNotMatchWhenNoOverlap() {
        assertFalse(tagFilterMatches(itemTagIds = listOf("a", "b"), selectedTagIds = setOf("x", "y")))
        assertFalse(tagFilterMatches(itemTagIds = emptyList(), selectedTagIds = setOf("a")))
    }

    @Test
    fun filtersListWithOrSemantics() {
        data class Item(val name: String, val tagIds: List<String>)

        val items = listOf(
            Item("putting", listOf("putting")),
            Item("chipping", listOf("chipping", "short_game")),
            Item("driving", listOf("driving")),
            Item("untagged", emptyList()),
        )

        val result = items.filteredByAnyTag(setOf("putting", "short_game")) { it.tagIds }

        assertEquals(listOf("putting", "chipping"), result.map(Item::name))
    }
}
