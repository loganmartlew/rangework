package com.loganmartlew.rangework.shared.data

import com.loganmartlew.rangework.shared.auth.AuthFoundation
import com.loganmartlew.rangework.shared.auth.createAuthFoundation
import com.loganmartlew.rangework.shared.usecase.DeletePracticeSessionUseCase
import com.loganmartlew.rangework.shared.usecase.DeletePracticeUnitUseCase
import com.loganmartlew.rangework.shared.usecase.GetClubCatalogUseCase
import com.loganmartlew.rangework.shared.usecase.GetEnabledClubsUseCase
import com.loganmartlew.rangework.shared.usecase.GetMeasurementPreferencesUseCase
import com.loganmartlew.rangework.shared.usecase.GetPracticeSessionUseCase
import com.loganmartlew.rangework.shared.usecase.GetPracticeUnitUseCase
import com.loganmartlew.rangework.shared.usecase.ListPracticeSessionsUseCase
import com.loganmartlew.rangework.shared.usecase.ListPracticeUnitsUseCase
import com.loganmartlew.rangework.shared.usecase.SaveMeasurementPreferencesUseCase
import com.loganmartlew.rangework.shared.usecase.SavePracticeSessionUseCase
import com.loganmartlew.rangework.shared.usecase.SavePracticeUnitUseCase
import com.loganmartlew.rangework.shared.usecase.SetClubEnabledUseCase
import io.github.jan.supabase.SupabaseClient

data class DataFoundation(
    val listPracticeUnitsUseCase: ListPracticeUnitsUseCase,
    val getPracticeUnitUseCase: GetPracticeUnitUseCase,
    val savePracticeUnitUseCase: SavePracticeUnitUseCase,
    val deletePracticeUnitUseCase: DeletePracticeUnitUseCase,
    val listPracticeSessionsUseCase: ListPracticeSessionsUseCase,
    val getPracticeSessionUseCase: GetPracticeSessionUseCase,
    val savePracticeSessionUseCase: SavePracticeSessionUseCase,
    val deletePracticeSessionUseCase: DeletePracticeSessionUseCase,
    val getMeasurementPreferencesUseCase: GetMeasurementPreferencesUseCase,
    val saveMeasurementPreferencesUseCase: SaveMeasurementPreferencesUseCase,
    val getClubCatalogUseCase: GetClubCatalogUseCase,
    val getEnabledClubsUseCase: GetEnabledClubsUseCase,
    val setClubEnabledUseCase: SetClubEnabledUseCase,
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
    val practiceUnitRepository = SupabasePracticeUnitRepository(client)
    val practiceSessionRepository = SupabasePracticeSessionRepository(client)
    val measurementPreferencesRepository = SupabaseMeasurementPreferencesRepository(client)
    val clubRepository = SupabaseClubRepository(client)

    return DataFoundation(
        listPracticeUnitsUseCase = ListPracticeUnitsUseCase(practiceUnitRepository),
        getPracticeUnitUseCase = GetPracticeUnitUseCase(practiceUnitRepository),
        savePracticeUnitUseCase = SavePracticeUnitUseCase(practiceUnitRepository),
        deletePracticeUnitUseCase = DeletePracticeUnitUseCase(practiceUnitRepository),
        listPracticeSessionsUseCase = ListPracticeSessionsUseCase(practiceSessionRepository),
        getPracticeSessionUseCase = GetPracticeSessionUseCase(practiceSessionRepository),
        savePracticeSessionUseCase = SavePracticeSessionUseCase(practiceSessionRepository),
        deletePracticeSessionUseCase = DeletePracticeSessionUseCase(practiceSessionRepository),
        getMeasurementPreferencesUseCase = GetMeasurementPreferencesUseCase(measurementPreferencesRepository),
        saveMeasurementPreferencesUseCase = SaveMeasurementPreferencesUseCase(measurementPreferencesRepository),
        getClubCatalogUseCase = GetClubCatalogUseCase(clubRepository),
        getEnabledClubsUseCase = GetEnabledClubsUseCase(clubRepository),
        setClubEnabledUseCase = SetClubEnabledUseCase(clubRepository),
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
