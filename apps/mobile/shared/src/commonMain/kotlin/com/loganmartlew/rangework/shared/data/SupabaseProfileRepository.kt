package com.loganmartlew.rangework.shared.data

import com.loganmartlew.rangework.shared.model.UserProfile
import com.loganmartlew.rangework.shared.repository.ProfileRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

private const val PROFILES_TABLE = "profiles"

class SupabaseProfileRepository(
    private val client: SupabaseClient,
) : ProfileRepository {
    override suspend fun getUserProfile(): UserProfile = client.postgrest[PROFILES_TABLE]
        .select()
        .decodeList<ProfileRow>()
        .firstOrNull()
        ?.toModel()
        ?: UserProfile(firstName = null, lastName = null, displayName = null, email = null)
}

@Serializable
private data class ProfileRow(
    @SerialName("id") val id: String,
    @SerialName("email") val email: String?,
    @SerialName("display_name") val displayName: String?,
    @SerialName("first_name") val firstName: String?,
    @SerialName("last_name") val lastName: String?,
)

private fun ProfileRow.toModel() = UserProfile(
    firstName = firstName,
    lastName = lastName,
    displayName = displayName,
    email = email,
)
