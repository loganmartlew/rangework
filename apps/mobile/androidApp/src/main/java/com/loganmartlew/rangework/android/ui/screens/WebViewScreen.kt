package com.loganmartlew.rangework.android.ui.screens

import android.content.ActivityNotFoundException
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

private const val ALLOWED_HOST_SUFFIX = "rangework.app"

private val legalPageUrls = mapOf(
    "privacy-policy" to "https://rangework.app/privacy-policy",
    "terms-of-use" to "https://rangework.app/terms-of-use",
    "cookie-policy" to "https://rangework.app/cookie-policy",
)

@Composable
internal fun WebViewScreen(page: String) {
    val url = legalPageUrls[page] ?: "https://rangework.app"
    var isLoading by remember { mutableStateOf(true) }

    Box(modifier = Modifier.fillMaxSize()) {
        AndroidView(
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.allowFileAccess = false
                    settings.allowContentAccess = false
                    settings.domStorageEnabled = false

                    webViewClient = object : WebViewClient() {
                        override fun shouldOverrideUrlLoading(
                            view: WebView,
                            request: WebResourceRequest,
                        ): Boolean {
                            val host = request.url.host ?: return true
                            val onDomain = host == ALLOWED_HOST_SUFFIX || host.endsWith(".$ALLOWED_HOST_SUFFIX")
                            if (onDomain) return false // load in-app
                            return try { // off-domain -> system browser
                                context.startActivity(Intent(Intent.ACTION_VIEW, request.url))
                                true
                            } catch (_: ActivityNotFoundException) {
                                true // swallow; do not load in-app
                            }
                        }

                        override fun onPageStarted(view: WebView, url: String?, favicon: Bitmap?) {
                            isLoading = true
                        }
                        override fun onPageFinished(view: WebView, url: String?) {
                            isLoading = false
                        }
                    }
                    loadUrl(url)
                }
            },
            modifier = Modifier.fillMaxSize(),
        )
        if (isLoading) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }
    }
}
