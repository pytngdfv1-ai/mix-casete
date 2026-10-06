package com.mixcasete.app.ui.components

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView

// User-Agent de Chrome REAL (sin el marcador "wv" del WebView): corrige el Error 153
private const val CHROME_UA =
    "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun EmbedFallback(videoId: String, modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            WebView(ctx).apply {
                val cm = CookieManager.getInstance()
                cm.setAcceptCookie(true)
                cm.setAcceptThirdPartyCookies(this, true)
                cm.setCookie(".youtube.com", "SOCS=CAI; path=/; secure")
                cm.flush()
                settings.javaScriptEnabled = true
                settings.domStorageEnabled = true
                settings.mediaPlaybackRequiresUserGesture = false
                settings.userAgentString = CHROME_UA
                webViewClient = WebViewClient()
                webChromeClient = WebChromeClient()
                loadUrl("https://www.youtube.com/embed/$videoId?autoplay=1&controls=1&rel=0&playsinline=1")
            }
        }
    )
}
