package com.loganmartlew.rangework.android

import android.app.Application
import io.sentry.android.core.SentryAndroid

class RangeworkApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SentryAndroid.init(this) { options ->
            options.dsn =
                "https://c25ebd3aaf998399f242f58e3a7d5639@o4511601454284800.ingest.de.sentry.io/4511601459527760"
            options.tracesSampleRate = 1.0
            options.isDebug = BuildConfig.DEBUG
        }
    }
}
