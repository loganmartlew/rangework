package com.loganmartlew.rangework.shared.model

/**
 * OR filter semantics for tags: an empty selection matches everything; otherwise
 * an item matches when it carries at least one of the selected tag ids.
 */
fun tagFilterMatches(itemTagIds: Collection<String>, selectedTagIds: Set<String>): Boolean =
    selectedTagIds.isEmpty() || itemTagIds.any(selectedTagIds::contains)

/**
 * Filter a list of tagged items by the selected tag ids using OR semantics.
 * `tagIdsOf` extracts the tag ids carried by each item.
 */
fun <T> List<T>.filteredByAnyTag(
    selectedTagIds: Set<String>,
    tagIdsOf: (T) -> Collection<String>,
): List<T> = filter { tagFilterMatches(tagIdsOf(it), selectedTagIds) }
