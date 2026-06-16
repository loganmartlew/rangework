package com.loganmartlew.rangework.shared.data

import com.loganmartlew.rangework.shared.model.Club
import com.loganmartlew.rangework.shared.model.ClubCategory
import com.loganmartlew.rangework.shared.repository.ClubRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val CLUBS_TABLE = "clubs"
private const val USER_ENABLED_CLUBS_TABLE = "user_enabled_clubs"

class SupabaseClubRepository(
    private val client: SupabaseClient,
) : ClubRepository {

    override suspend fun listCatalog(): List<Club> =
        client.postgrest[CLUBS_TABLE]
            .select {
                order("sort_order", Order.ASCENDING)
            }
            .decodeList<ClubRow>()
            .map(ClubRow::toModel)

    override suspend fun getEnabledClubCodes(): Set<String> =
        client.postgrest[USER_ENABLED_CLUBS_TABLE]
            .select()
            .decodeList<UserEnabledClubRow>()
            .map(UserEnabledClubRow::clubCode)
            .toSet()

    override suspend fun setClubEnabled(code: String, enabled: Boolean) {
        if (enabled) {
            client.postgrest[USER_ENABLED_CLUBS_TABLE].upsert(
                UserEnabledClubInsertRow(clubCode = code),
            ) {
                onConflict = "user_id,club_code"
            }
        } else {
            client.postgrest[USER_ENABLED_CLUBS_TABLE].delete {
                filter {
                    eq("club_code", code)
                }
            }
        }
    }
}

@Serializable
private data class ClubRow(
    val code: String,
    @SerialName("display_name")
    val displayName: String,
    val category: ClubCategory,
    @SerialName("sort_order")
    val sortOrder: Int,
)

@Serializable
private data class UserEnabledClubRow(
    @SerialName("user_id")
    val userId: String,
    @SerialName("club_code")
    val clubCode: String,
)

@Serializable
private data class UserEnabledClubInsertRow(
    @SerialName("club_code")
    val clubCode: String,
)

private fun ClubRow.toModel(): Club = Club(
    code = code,
    displayName = displayName,
    category = category,
    sortOrder = sortOrder,
)
