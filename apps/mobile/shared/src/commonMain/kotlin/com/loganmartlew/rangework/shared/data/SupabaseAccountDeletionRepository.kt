package com.loganmartlew.rangework.shared.data

import com.loganmartlew.rangework.shared.repository.AccountDeletionRepository
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.functions.functions

class SupabaseAccountDeletionRepository(
    private val client: SupabaseClient,
) : AccountDeletionRepository {
    override suspend fun deleteAccount() {
        client.functions.invoke("delete-account")
    }
}
