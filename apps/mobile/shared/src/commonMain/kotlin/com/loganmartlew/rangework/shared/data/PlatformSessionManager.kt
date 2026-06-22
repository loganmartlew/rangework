package com.loganmartlew.rangework.shared.data

import io.github.jan.supabase.auth.SessionManager

/** Returns a platform-encrypted session manager, or null to use the supabase-kt default. */
internal expect fun rangeworkPlatformSessionManager(): SessionManager?