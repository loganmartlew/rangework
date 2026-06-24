package com.loganmartlew.rangework.shared.data

import com.loganmartlew.rangework.shared.auth.AuthFoundation
import com.loganmartlew.rangework.shared.auth.createAuthFoundation
import com.loganmartlew.rangework.shared.repository.AccountDeletionRepository
import com.loganmartlew.rangework.shared.repository.ClubRepository
import com.loganmartlew.rangework.shared.repository.MeasurementPreferencesRepository
import com.loganmartlew.rangework.shared.repository.PracticeSessionRepository
import com.loganmartlew.rangework.shared.repository.PracticeUnitRepository
import com.loganmartlew.rangework.shared.repository.RangeSessionRepository
import io.github.jan.supabase.SupabaseClient

data class DataFoundation(
    val practiceUnitRepository: PracticeUnitRepository,
    val practiceSessionRepository: PracticeSessionRepository,
    val measurementPreferencesRepository: MeasurementPreferencesRepository,
    val clubRepository: ClubRepository,
    val rangeSessionRepository: RangeSessionRepository,
    val accountDeletionRepository: AccountDeletionRepository,
)

data class RangeworkFoundation(
    val authFoundation: AuthFoundation,
    val dataFoundation: DataFoundation,
)

fun createDataFoundation(config: SupabaseEndpointConfig): DataFoundation? {
    if (!config.isConfigured) {
        return null
    }

    return createDataFoundation(
        client = createRangeworkSupabaseClient(config),
    )
}

fun createDataFoundation(client: SupabaseClient): DataFoundation = DataFoundation(
    practiceUnitRepository = SupabasePracticeUnitRepository(client),
    practiceSessionRepository = SupabasePracticeSessionRepository(client),
    measurementPreferencesRepository = SupabaseMeasurementPreferencesRepository(client),
    clubRepository = SupabaseClubRepository(client),
    rangeSessionRepository = SupabaseRangeSessionRepository(client),
    accountDeletionRepository = SupabaseAccountDeletionRepository(client),
)

fun createRangeworkFoundation(config: SupabaseEndpointConfig): RangeworkFoundation? {
    if (!config.isConfigured) {
        return null
    }

    return createRangeworkFoundation(
        client = createRangeworkSupabaseClient(config),
    )
}

fun createRangeworkFoundation(client: SupabaseClient): RangeworkFoundation = RangeworkFoundation(
    authFoundation = createAuthFoundation(client),
    dataFoundation = createDataFoundation(client),
)
