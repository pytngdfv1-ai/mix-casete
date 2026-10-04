package com.mixcasete.app.tv

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.WindowManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay

class TvActivity : ComponentActivity() {

    @SuppressLint("SetJavaScriptEnabled")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        WindowCompat.setDecorFitsSystemWindows(window, false)
        val controller = WindowInsetsControllerCompat(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        val videoUrl = intent.getStringExtra("videoUrl")

        setContent {
            TvScreen(videoUrl = videoUrl, onExit = { finish() })
        }
    }
}

@Composable
fun TvScreen(videoUrl: String?, onExit: () -> Unit) {
    var showControls by remember { mutableStateOf(true) }

    LaunchedEffect(showControls) {
        if (showControls) {
            delay(3000)
            showControls = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { showControls = true }
            )
    ) {
        val videoId = extractVideoId(videoUrl)

        if (videoId != null) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        webViewClient = WebViewClient()
                        setBackgroundColor(0xFF000000.toInt())
                        loadDataWithBaseURL(
                            "https://www.youtube.com",
                            buildEmbedHtml(videoId),
                            "text/html",
                            "utf-8",
                            null
                        )
                    }
                }
            )
        }

        if (showControls) {
            IconButton(
                onClick = onExit,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(16.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Filled.Close, contentDescription = "Salir de TV", tint = Color.White)
            }
        }
    }
}

private fun extractVideoId(url: String?): String? {
    if (url == null) return null
    return try {
        when {
            url.contains("watch?v=") -> url.substringAfter("watch?v=").substringBefore("&")
            url.contains("youtu.be/") -> url.substringAfter("youtu.be/").substringBefore("?")
            else -> null
        }
    } catch (e: Exception) {
        null
    }
}

private fun buildEmbedHtml(videoId: String): String {
    return """
        <html>
        <head>
        <style>
          html,body{margin:0;padding:0;background:#000;height:100%;overflow:hidden}
          .wrap{position:fixed;top:50%;left:50%;transform:translate(-50%,-50%);width:100vw;height:56.25vw;max-height:100vh;max-width:177.78vh}
          iframe{width:100%;height:100%;border:0;display:block}
        </style>
        </head>
        <body>
        <div class="wrap">
          <iframe src="https://www.youtube.com/embed/$videoId?autoplay=1&controls=0&modestbranding=1&rel=0&showinfo=0&iv_load_policy=3&fs=0&playsinline=1"
            allow="autoplay; encrypted-media; fullscreen"></iframe>
        </div>
        </body>
        </html>
    """.trimIndent()
}
