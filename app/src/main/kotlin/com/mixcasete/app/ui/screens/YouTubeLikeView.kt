package com.mixcasete.app.ui.screens

import android.annotation.SuppressLint
import android.webkit.WebChromeClient
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.FiberManualRecord
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.mixcasete.app.audio.AudioPlayerViewModel
import com.mixcasete.app.audio.VideoResolver

private const val CHROME_UA =
    "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun YouTubeLikeView(viewModel: AudioPlayerViewModel, onExit: () -> Unit) {
    val context = LocalContext.current
    val cassette by viewModel.cassette.collectAsState()
    val currentVideoUrl by viewModel.currentVideoUrl.collectAsState()
    val currentPlaylist by viewModel.currentPlaylist.collectAsState()
    val currentIndex by viewModel.currentSongIndex.collectAsState()
    val isFavorite = currentPlaylist.getOrNull(currentIndex)?.isFavorite ?: false

    var videoStreamUrl by remember { mutableStateOf<String?>(null) }
    var useIframe by remember { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(false) }

    LaunchedEffect(currentVideoUrl) {
        val id = extractVideoIdLocal(currentVideoUrl)
        val url = id?.let { VideoResolver.resolveProgressiveVideoUrl(it) }
        videoStreamUrl = url
        useIframe = (url == null && id != null)
    }

    val exo = remember {
        ExoPlayer.Builder(context)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(30_000, 60_000, 5_000, 8_000)
                    .build()
            )
            .build()
    }

    DisposableEffect(Unit) {
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) { isPlaying = playing }
        }
        exo.addListener(listener)
        onDispose { exo.removeListener(listener); exo.release() }
    }

    LaunchedEffect(videoStreamUrl) {
        videoStreamUrl?.let {
            exo.setMediaItem(MediaItem.fromUri(it))
            exo.prepare()
            exo.playWhenReady = true
        }
    }

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onExit) {
                Icon(Icons.Filled.ArrowBack, contentDescription = "Volver al casete", tint = Color.White)
            }
            Text("Mix.Casete · Modo YouTube", color = Color(0xFFDDDDDD), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        if (videoStreamUrl != null) {
            AndroidView(
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exo
                        useController = true
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        setBackgroundColor(0xFF000000.toInt())
                    }
                }
            )
        } else if (useIframe) {
            // Fallback: reproductor OFICIAL de YouTube (siempre reproduce si el video permite embed)
            val videoId = extractVideoIdLocal(currentVideoUrl)
            AndroidView(
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                factory = { ctx ->
                    WebView(ctx).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.mediaPlaybackRequiresUserGesture = false
                        settings.userAgentString = CHROME_UA
                        webViewClient = WebViewClient()
                        webChromeClient = WebChromeClient()
                        setBackgroundColor(0xFF000000.toInt())
                        loadUrl("https://www.youtube-nocookie.com/embed/$videoId?autoplay=1&controls=1&rel=0&playsinline=1")
                    }
                }
            )
        } else {
            androidx.compose.foundation.layout.Box(
                Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color(0xFF111111)),
                contentAlignment = Alignment.Center
            ) {
                Text("Sin video disponible para este tema", color = Color(0xFF888888), fontSize = 13.sp)
            }
        }

        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(cassette.title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 2)
            Text(cassette.artist, color = Color(0xFFAAAAAA), fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))

            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.tvPrev() }) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Anterior", tint = Color.White)
                }
                IconButton(onClick = { if (exo.isPlaying) exo.pause() else exo.play() }) {
                    Icon(
                        if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = "Play/Pausa", tint = Color.White, modifier = Modifier.size(40.dp)
                    )
                }
                IconButton(onClick = { viewModel.tvNext() }) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Siguiente", tint = Color.White)
                }
                IconButton(onClick = { currentPlaylist.getOrNull(currentIndex)?.let { viewModel.toggleFavorite(it) } }) {
                    Icon(
                        if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = "Favorito", tint = if (isFavorite) Color(0xFFE57373) else Color.White
                    )
                }
                IconButton(onClick = { viewModel.recordCurrentTrack() }) {
                    Icon(Icons.Filled.FiberManualRecord, contentDescription = "Grabar", tint = Color(0xFFE53935))
                }
            }
        }
    }
}

private fun extractVideoIdLocal(url: String?): String? {
    if (url == null) return null
    return when {
        url.contains("watch?v=") -> url.substringAfter("watch?v=").substringBefore("&")
        url.contains("youtu.be/") -> url.substringAfter("youtu.be/").substringBefore("?")
        else -> null
    }
}
