package com.loganmartlew.rangework.shared.data

import com.loganmartlew.rangework.shared.auth.AuthFoundation
import com.loganmartlew.rangework.shared.auth.createAuthFoundation
import com.loganmartlew.rangework.shared.usecase.AbandonRangeSessionUseCase
import com.loganmartlew.rangework.shared.usecase.DeleteAccountUseCase
import com.loganmartlew.rangework.shared.usecase.CloseTimeEntryUseCase
import com.loganmartlew.rangework.shared.usecase.DeletePracticeSessionUseCase
import com.loganmartlew.rangework.shared.usecase.DuplicatePracticeSessionUseCase
import com.loganmartlew.rangework.shared.usecase.DeletePracticeUnitUseCase
import com.loganmartlew.rangework.shared.usecase.DuplicateUnitUseCase
import com.loganmartlew.rangework.shared.usecase.FinishRangeSessionUseCase
import com.loganmartlew.rangework.shared.usecase.GetClubCatalogUseCase
import com.loganmartlew.rangework.shared.usecase.GetEnabledClubsUseCase
import com.loganmartlew.rangework.shared.usecase.GetElapsedSecondsUseCase
import com.loganmartlew.rangework.shared.usecase.GetMeasurementPreferencesUseCase
import com.loganmartlew.rangework.shared.usecase.GetPracticeSessionUseCase
import com.loganmartlew.rangework.shared.usecase.GetPracticeUnitUseCase
import com.loganmartlew.rangework.shared.usecase.GetRangeSessionUseCase
import com.loganmartlew.rangework.shared.usecase.HasActiveRangeSessionsUseCase
import com.loganmartlew.rangework.shared.usecase.ListActiveRangeSessionsUseCase
import com.loganmartlew.rangework.shared.usecase.ListCompletedRangeSessionsUseCase
import com.loganmartlew.rangework.shared.usecase.ListPracticeSessionsUseCase
import com.loganmartlew.rangework.shared.usecase.ListPracticeUnitsUseCase
import com.loganmartlew.rangework.shared.usecase.OverrideStepClubUseCase
import com.loganmartlew.rangework.shared.usecase.RecordTimeEntryUseCase
import com.loganmartlew.rangework.shared.usecase.SaveMeasurementPreferencesUseCase
import com.loganmartlew.rangework.shared.usecase.SavePracticeSessionUseCase
import com.loganmartlew.rangework.shared.usecase.SavePracticeUnitUseCase
import com.loganmartlew.rangework.shared.usecase.SetClubEnabledUseCase
import com.loganmartlew.rangework.shared.usecase.StartRangeSessionUseCase
import com.loganmartlew.rangework.shared.usecase.ToggleStepCompleteUseCase
import com.loganmartlew.rangework.shared.usecase.UpdateLastViewedStepUseCase
import io.github.jan.supabase.SupabaseClient

data class DataFoundation(
    val listPracticeUnitsUseCase: ListPracticeUnitsUseCase,
    val getPracticeUnitUseCase: GetPracticeUnitUseCase,
    val savePracticeUnitUseCase: SavePracticeUnitUseCase,
    val deletePracticeUnitUseCase: DeletePracticeUnitUseCase,
    val duplicatePracticeUnitUseCase: DuplicateUnitUseCase,
    val listPracticeSessionsUseCase: ListPracticeSessionsUseCase,
    val getPracticeSessionUseCase: GetPracticeSessionUseCase,
    val savePracticeSessionUseCase: SavePracticeSessionUseCase,
    val deletePracticeSessionUseCase: DeletePracticeSessionUseCase,
    val duplicatePracticeSessionUseCase: DuplicatePracticeSessionUseCase,
    val getMeasurementPreferencesUseCase: GetMeasurementPreferencesUseCase,
    val saveMeasurementPreferencesUseCase: SaveMeasurementPreferencesUseCase,
    val getClubCatalogUseCase: GetClubCatalogUseCase,
    val getEnabledClubsUseCase: GetEnabledClubsUseCase,
    val setClubEnabledUseCase: SetClubEnabledUseCase,
    val startRangeSessionUseCase: StartRangeSessionUseCase,
    val getRangeSessionUseCase: GetRangeSessionUseCase,
    val listActiveRangeSessionsUseCase: ListActiveRangeSessionsUseCase,
    val listCompletedRangeSessionsUseCase: ListCompletedRangeSessionsUseCase,
    val toggleStepCompleteUseCase: ToggleStepCompleteUseCase,
    val overrideStepClubUseCase: OverrideStepClubUseCase,
    val updateLastViewedStepUseCase: UpdateLastViewedStepUseCase,
    val finishRangeSessionUseCase: FinishRangeSessionUseCase,
    val abandonRangeSessionUseCase: AbandonRangeSessionUseCase,
    val recordTimeEntryUseCase: RecordTimeEntryUseCase,
    val closeTimeEntryUseCase: CloseTimeEntryUseCase,
    val getElapsedSecondsUseCase: GetElapsedSecondsUseCase,
    val hasActiveRangeSessionsUseCase: HasActiveRangeSessionsUseCase,
    val deleteAccountUseCase: DeleteAccountUseCase,
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
    val rangeSessionRepository = SupabaseRangeSessionRepository(client)
    val accountDeletionRepository = SupabaseAccountDeletionRepository(client)

    return DataFoundation(
        listPracticeUnitsUseCase = ListPracticeUnitsUseCase(practiceUnitRepository),
        getPracticeUnitUseCase = GetPracticeUnitUseCase(practiceUnitRepository),
        savePracticeUnitUseCase = SavePracticeUnitUseCase(practiceUnitRepository),
        deletePracticeUnitUseCase = DeletePracticeUnitUseCase(practiceUnitRepository),
        duplicatePracticeUnitUseCase = DuplicateUnitUseCase(
            getPracticeUnitUseCase = GetPracticeUnitUseCase(practiceUnitRepository),
            savePracticeUnitUseCase = SavePracticeUnitUseCase(practiceUnitRepository),
        ),
        listPracticeSessionsUseCase = ListPracticeSessionsUseCase(practiceSessionRepository),
        getPracticeSessionUseCase = GetPracticeSessionUseCase(practiceSessionRepository),
        savePracticeSessionUseCase = SavePracticeSessionUseCase(practiceSessionRepository),
        deletePracticeSessionUseCase = DeletePracticeSessionUseCase(practiceSessionRepository),
        duplicatePracticeSessionUseCase = DuplicatePracticeSessionUseCase(
            getPracticeSessionUseCase = GetPracticeSessionUseCase(practiceSessionRepository),
            savePracticeSessionUseCase = SavePracticeSessionUseCase(practiceSessionRepository),
        ),
        getMeasurementPreferencesUseCase = GetMeasurementPreferencesUseCase(measurementPreferencesRepository),
        saveMeasurementPreferencesUseCase = SaveMeasurementPreferencesUseCase(measurementPreferencesRepository),
        getClubCatalogUseCase = GetClubCatalogUseCase(clubRepository),
        getEnabledClubsUseCase = GetEnabledClubsUseCase(clubRepository),
        setClubEnabledUseCase = SetClubEnabledUseCase(clubRepository),
        startRangeSessionUseCase = StartRangeSessionUseCase(rangeSessionRepository),
        getRangeSessionUseCase = GetRangeSessionUseCase(rangeSessionRepository),
        listActiveRangeSessionsUseCase = ListActiveRangeSessionsUseCase(rangeSessionRepository),
        listCompletedRangeSessionsUseCase = ListCompletedRangeSessionsUseCase(rangeSessionRepository),
        toggleStepCompleteUseCase = ToggleStepCompleteUseCase(rangeSessionRepository),
        overrideStepClubUseCase = OverrideStepClubUseCase(rangeSessionRepository),
        updateLastViewedStepUseCase = UpdateLastViewedStepUseCase(rangeSessionRepository),
        finishRangeSessionUseCase = FinishRangeSessionUseCase(rangeSessionRepository),
        abandonRangeSessionUseCase = AbandonRangeSessionUseCase(rangeSessionRepository),
        recordTimeEntryUseCase = RecordTimeEntryUseCase(rangeSessionRepository),
        closeTimeEntryUseCase = CloseTimeEntryUseCase(rangeSessionRepository),
        getElapsedSecondsUseCase = GetElapsedSecondsUseCase(rangeSessionRepository),
        hasActiveRangeSessionsUseCase = HasActiveRangeSessionsUseCase(rangeSessionRepository),
        deleteAccountUseCase = DeleteAccountUseCase(accountDeletionRepository),
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
