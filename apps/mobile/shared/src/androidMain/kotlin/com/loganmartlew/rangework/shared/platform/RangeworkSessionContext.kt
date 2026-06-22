package com.loganmartlew.rangework.shared.platform

import android.content.Context

object RangeworkSessionContext {
    @Volatile var appContext: Context? = null
}