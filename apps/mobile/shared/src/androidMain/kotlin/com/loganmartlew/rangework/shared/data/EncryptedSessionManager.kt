package com.loganmartlew.rangework.shared.data

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import io.github.jan.supabase.auth.SessionManager
import io.github.jan.supabase.auth.user.UserSession
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class EncryptedSessionManager(context: Context) : SessionManager {
    private val json = Json

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "rangework_secure_session",
        MasterKey.Builder(context).setKeyScheme(MasterKey.KeyScheme.AES256_GCM).build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )

    override suspend fun saveSession(session: UserSession) {
        prefs.edit().putString("session", json.encodeToString(session)).apply()
    }

    override suspend fun loadSession(): UserSession? {
        return prefs.getString("session", null)?.let { json.decodeFromString(it) }
    }

    override suspend fun deleteSession() {
        prefs.edit().remove("session").apply()
    }
}