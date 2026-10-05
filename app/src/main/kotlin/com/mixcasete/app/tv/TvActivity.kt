package com.mixcasete.app.tv

import android.annotation.SuppressLint
import android.content.pm.ActivityInfo
import android.os.Bundle
import android.provider.Settings
import android.view.WindowManager
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import kotlinx.coroutines.delay

// User-Agent de Chrome real (SIN el marcador "wv" de WebView) para que YouTube no lo bloquee
private const val CHROME_UA =
    "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

class TvActivity : ComponentActivity() {

    @SuppressLint("SetJavaScriptEnabled", "SourceLockedOrientationActivity")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE

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
    val context = LocalContext.current
    var showControls by remember { mutableStateOf(true) }
    var showConnect by remember { mutableStateOf(false) }

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
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.allowContentAccess = true
                        settings.userAgentString = CHROME_UA
                        webViewClient = WebViewClient()
                        webChromeClient = WebChromeClient()
                        setBackgroundColor(0xFF000000.toInt())
                        // youtube-nocookie + solo parametros validos actuales (evita Error 153)
                        loadUrl(
                            "https://www.youtube-nocookie.com/embed/$videoId" +
                                "?autoplay=1&controls=0&rel=0&playsinline=1&modestbranding=1"
                        )
                    }
                }
            )
        } else {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Sin video para mostrar\n(conecta un tema de YouTube primero)",
                    color = Color(0xFF888888),
                    fontSize = 14.sp
                )
            }
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

            IconButton(
                onClick = { showConnect = true },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                Icon(Icons.Filled.Settings, contentDescription = "Conectar", tint = Color.White)
            }
        }

        if (showConnect) {
            AlertDialog(
                onDismissRequest = { showConnect = false },
                title = { Text("Conectar a una pantalla") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = "Elige cómo compartir el video con tu TV:",
                            fontSize = 12.sp,
                            color = Color(0xFFBBBBBB)
                        )
                        Spacer(Modifier.height(4.dp))
                        TextButton(onClick = {
                            showConnect = false
                            launchSystemSettings(context, Settings.ACTION_CAST_SETTINGS)
                        }) {
                            Text("Transmitir (Cast / Chromecast / TV)")
                        }
                        TextButton(onClick = {
                            showConnect = false
                            launchSystemSettings(context, "android.settings.WIFI_DISPLAY_SETTINGS")
                        }) {
                            Text("Wireless Display / Smart View (duplicar)")
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showConnect = false }) { Text("Cerrar") }
                }
            )
        }
    }
}

private fun launchSystemSettings(context: android.content.Context, action: String) {
    try {
        context.startActivity(android.content.Intent(action))
    } catch (e: Exception) {
        try {
            context.startActivity(android.content.Intent(Settings.ACTION_DISPLAY_SETTINGS))
        } catch (e2: Exception) {
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
