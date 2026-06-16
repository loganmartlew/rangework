package com.loganmartlew.rangework.shared.data

import com.loganmartlew.rangework.shared.model.DistanceUnit
import com.loganmartlew.rangework.shared.model.MeasurementPreferences
import com.loganmartlew.rangework.shared.model.SpeedUnit
import com.loganmartlew.rangework.shared.model.UnitSystem
import com.loganmartlew.rangework.shared.repository.MeasurementPreferencesRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val USER_PREFERENCES_TABLE = "user_preferences"

class SupabaseMeasurementPreferencesRepository(
    private val client: SupabaseClient,
) : MeasurementPreferencesRepository {
    override suspend fun getMeasurementPreferences(): MeasurementPreferences = client.postgrest[USER_PREFERENCES_TABLE]
        .select()
        .decodeList<UserPreferencesRow>()
        .firstOrNull()
        ?.toModel()
        ?: MeasurementPreferences.Imperial

    override suspend fun saveMeasurementPreferences(
        preferences: MeasurementPreferences,
    ): MeasurementPreferences {
        client.postgrest[USER_PREFERENCES_TABLE].upsert(
            UserPreferencesInsertRow(
                unitSystem = preferences.unitSystem,
                distanceUnit = preferences.distanceUnit,
                speedUnit = preferences.speedUnit,
            ),
        ) {
            onConflict = "user_id"
        }
        return getMeasurementPreferences()
    }
}

@Serializable
private data class UserPreferencesRow(
    @SerialName("user_id")
    val userId: String,
    @SerialName("unit_system")
    val unitSystem: UnitSystem,
    @SerialName("distance_unit")
    val distanceUnit: DistanceUnit,
    @SerialName("speed_unit")
    val speedUnit: SpeedUnit,
)

@Serializable
private data class UserPreferencesInsertRow(
    @SerialName("user_id") val userId: String? = null,
    @SerialName("unit_system")
    val unitSystem: UnitSystem,
    @SerialName("distance_unit")
    val distanceUnit: DistanceUnit,
    @SerialName("speed_unit")
    val speedUnit: SpeedUnit,
)

private fun UserPreferencesRow.toModel(): MeasurementPreferences = MeasurementPreferences(
    unitSystem = unitSystem,
    distanceUnit = distanceUnit,
    speedUnit = speedUnit,
)
