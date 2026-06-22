package com.loganmartlew.rangework.shared.data

import com.loganmartlew.rangework.shared.platform.RangeworkSessionContext
import io.github.jan.supabase.auth.SessionManager

internal actual fun rangeworkPlatformSessionManager(): SessionManager? {
    val ctx = RangeworkSessionContext.appContext ?: return null // fall back to default
    return EncryptedSessionManager(ctx)
}