package com.loganmartlew.rangework.shared.data

import com.loganmartlew.rangework.shared.auth.AuthFoundation
import com.loganmartlew.rangework.shared.auth.createAuthFoundation
import com.loganmartlew.rangework.shared.library.DefaultPracticeLibrary
import com.loganmartlew.rangework.shared.library.PracticeLibrary
import com.loganmartlew.rangework.shared.repository.AccountDeletionRepository
import com.loganmartlew.rangework.shared.repository.ClubRepository
import com.loganmartlew.rangework.shared.repository.MeasurementPreferencesRepository
import com.loganmartlew.rangework.shared.repository.RangeSessionRepository
import com.loganmartlew.rangework.shared.repository.TagRepository
import io.github.jan.supabase.SupabaseClient

data class DataFoundation(
    val practiceLibrary: PracticeLibrary,
    val measurementPreferencesRepository: MeasurementPreferencesRepository,
    val clubRepository: ClubRepository,
    val tagRepository: TagRepository,
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

fun createDataFoundation(client: SupabaseClient): DataFoundation {
    val unitRepository = SupabasePracticeUnitRepository(client)
    val sessionRepository = SupabasePracticeSessionRepository(client)
    return DataFoundation(
        practiceLibrary = DefaultPracticeLibrary(unitRepository, sessionRepository),
        measurementPreferencesRepository = SupabaseMeasurementPreferencesRepository(client),
        clubRepository = SupabaseClubRepository(client),
        tagRepository = SupabaseTagRepository(client),
        rangeSessionRepository = SupabaseRangeSessionRepository(client),
        accountDeletionRepository = SupabaseAccountDeletionRepository(client),
    )
}

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
