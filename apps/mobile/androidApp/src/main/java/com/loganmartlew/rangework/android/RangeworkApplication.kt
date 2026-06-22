package com.loganmartlew.rangework.android

import android.app.Application
import com.loganmartlew.rangework.shared.platform.RangeworkSessionContext
import io.sentry.SentryOptions
import io.sentry.android.core.SentryAndroid

class RangeworkApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        RangeworkSessionContext.appContext = applicationContext

        SentryAndroid.init(this) { options ->
            options.dsn = BuildConfig.SENTRY_DSN
            options.environment = if (BuildConfig.DEBUG) "debug" else "production"
            options.isDebug = BuildConfig.DEBUG

            // Sample everything in debug, a small fraction in release.
            options.tracesSampleRate = if (BuildConfig.DEBUG) 1.0 else 0.2

            // Never attach IP / user identifiers automatically.
            options.isSendDefaultPii = false

            // Defensive scrub: drop user object and obvious PII before send.
            options.beforeSend = SentryOptions.BeforeSendCallback { event, _ ->
                event.user = null
                event.request = null
                event
            }
        }
    }
}
