package com.loganmartlew.rangework.shared.model

import kotlinx.datetime.Instant

sealed interface RecentItem {
    val id: String
    val updatedAt: Instant

    data class Unit(val practiceUnit: PracticeUnit) : RecentItem {
        override val id: String get() = practiceUnit.id
        override val updatedAt: Instant get() = practiceUnit.updatedAt
    }

    data class Session(val practiceSession: PracticeSession) : RecentItem {
        override val id: String get() = practiceSession.id
        override val updatedAt: Instant get() = practiceSession.updatedAt
    }
}

fun recentItems(
    units: List<PracticeUnit>,
    sessions: List<PracticeSession>,
    limit: Int = 5,
): List<RecentItem> {
    val all = units.map { RecentItem.Unit(it) } + sessions.map { RecentItem.Session(it) }
    return all
        .sortedByDescending { it.updatedAt }
        .take(limit)
}
