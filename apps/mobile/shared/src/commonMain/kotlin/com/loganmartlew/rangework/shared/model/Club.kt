package com.loganmartlew.rangework.shared.model

import kotlinx.serialization.Serializable

@Serializable
enum class ClubCategory {
    DRIVER,
    WOOD,
    HYBRID,
    IRON,
    WEDGE,
    PUTTER,
}

@Serializable
data class Club(
    val code: String,
    val displayName: String,
    val category: ClubCategory,
    val sortOrder: Int,
)

private val parentheticalAbbreviation = Regex("""\(([^)]+)\)\s*$""")
private val numberedClub = Regex("""^(\d+)-(\w)""")

/**
 * Compact label for tight UI such as execution instruction rows. Wedges use
 * their parenthesised abbreviation ("Pitching Wedge (PW)" → "PW"), numbered
 * clubs compress to number plus type initial ("7-Iron" → "7I"), Driver and
 * Putter abbreviate to "DR"/"PT". Unrecognised names pass through unchanged,
 * so full names remain the fallback everywhere.
 */
fun clubShortLabel(displayName: String): String {
    parentheticalAbbreviation.find(displayName)?.let { return it.groupValues[1] }
    numberedClub.find(displayName)?.let {
        return it.groupValues[1] + it.groupValues[2].uppercase()
    }
    return when (displayName.trim().lowercase()) {
        "driver" -> "DR"
        "putter" -> "PT"
        else -> displayName
    }
}
