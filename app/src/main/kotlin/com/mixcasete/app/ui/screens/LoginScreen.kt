package com.mixcasete.app.ui.screens

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.mixcasete.app.audio.AudioPlayerViewModel

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun LoginContent(viewModel: AudioPlayerViewModel) {
    val context = LocalContext.current

    DisposableEffect(Unit) {
        onDispose {
            CookieManager.getInstance().flush()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF101010))
    ) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                WebView(ctx).apply {
                    settings.javaScriptEnabled = true
                    settings.domStorageEnabled = true
                    settings.userAgentString = settings.userAgentString
                    CookieManager.getInstance().setAcceptCookie(true)
                    CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                    webViewClient = WebViewClient()
                    loadUrl("https://accounts.google.com/ServiceLogin?service=youtube")
                }
            }
        )

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .background(Color.Black.copy(alpha = 0.7f))
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Login YouTube (cookies persistentes)",
                color = Color(0xFFD6D6D6),
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = {
                CookieManager.getInstance().flush()
                context.getSharedPreferences("mixcasete", android.content.Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean("yt_logged_in", true)
                    .apply()
                viewModel.toggleLoginScreen()
            }) {
                Text("Listo", color = Color(0xFF81C784), fontSize = 12.sp)
            }
            TextButton(onClick = { viewModel.toggleLoginScreen() }) {
                Text("Cerrar", color = Color(0xFFE57373), fontSize = 12.sp)
            }
        }
    }
}
