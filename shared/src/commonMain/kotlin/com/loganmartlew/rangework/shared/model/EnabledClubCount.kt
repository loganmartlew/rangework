package com.loganmartlew.rangework.shared.model

data class EnabledClubCount(val enabled: Int, val total: Int) {
    companion object {
        fun from(catalog: List<Club>, enabledCodes: Set<String>) = EnabledClubCount(
            enabled = catalog.count { it.code in enabledCodes },
            total = catalog.size,
        )
    }
}
