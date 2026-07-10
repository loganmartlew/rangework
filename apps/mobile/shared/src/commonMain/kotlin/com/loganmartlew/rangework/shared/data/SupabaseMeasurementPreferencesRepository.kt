package com.loganmartlew.rangework.shared.data

import com.loganmartlew.rangework.shared.model.DistanceUnit
import com.loganmartlew.rangework.shared.model.Handedness
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
) : MeasurementPreferencesRepository() {
    override suspend fun get(): MeasurementPreferences = client.postgrest[USER_PREFERENCES_TABLE]
        .select()
        .decodeList<UserPreferencesRow>()
        .firstOrNull()
        ?.toModel()
        ?: MeasurementPreferences.Imperial

    override suspend fun persist(validated: MeasurementPreferences): MeasurementPreferences {
        client.postgrest[USER_PREFERENCES_TABLE].upsert(
            UserPreferencesInsertRow(
                unitSystem = validated.unitSystem,
                distanceUnit = validated.distanceUnit,
                speedUnit = validated.speedUnit,
                handedness = validated.handedness,
            ),
        ) {
            onConflict = "user_id"
        }
        return get()
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
    // Read as a raw string and mapped with a RIGHT fallback so a value this app
    // version doesn't recognise degrades rather than crashing the whole
    // preferences load — the wire-tolerance rule applied to new wire values.
    val handedness: String? = null,
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
    val handedness: Handedness,
)

private fun UserPreferencesRow.toModel(): MeasurementPreferences = MeasurementPreferences(
    unitSystem = unitSystem,
    distanceUnit = distanceUnit,
    speedUnit = speedUnit,
    handedness = handedness?.let(Handedness::fromId) ?: Handedness.RIGHT,
)
