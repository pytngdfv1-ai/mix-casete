package com.mixcasete.app.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MergingMediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.PlayerView
import com.mixcasete.app.audio.AudioPlayerViewModel
import com.mixcasete.app.audio.PlayState
import com.mixcasete.app.audio.VideoResolver
import com.mixcasete.app.audio.VideoStream
import com.mixcasete.app.ui.components.EmbedFallback

@Composable
fun YouTubeLikeView(viewModel: AudioPlayerViewModel, onExit: () -> Unit) {
    val context = LocalContext.current
    val cassette by viewModel.cassette.collectAsState()
    val currentVideoUrl by viewModel.currentVideoUrl.collectAsState()
    val currentPlaylist by viewModel.currentPlaylist.collectAsState()
    val currentIndex by viewModel.currentSongIndex.collectAsState()
    val isFavorite = currentPlaylist.getOrNull(currentIndex)?.isFavorite ?: false

    var stream by remember { mutableStateOf<VideoStream?>(null) }
    var isPlaying by remember { mutableStateOf(false) }

    LaunchedEffect(currentVideoUrl) {
        stream = extractId(currentVideoUrl)?.let { VideoResolver.resolve(it) }
    }

    val exo = remember {
        ExoPlayer.Builder(context)
            .setLoadControl(DefaultLoadControl.Builder().setBufferDurationsMs(30_000, 60_000, 5_000, 8_000).build())
            .build()
    }
    DisposableEffect(Unit) {
        val l = object : Player.Listener { override fun onIsPlayingChanged(p: Boolean) { isPlaying = p } }
        exo.addListener(l)
        onDispose { exo.removeListener(l); exo.release() }
    }

    LaunchedEffect(stream) {
        val s = stream ?: return@LaunchedEffect
        val factory = DefaultDataSource.Factory(context)
        if (s.progressiveUrl != null) {
            exo.setMediaItem(MediaItem.fromUri(s.progressiveUrl))
        } else if (s.videoUrl != null && s.audioUrl != null) {
            val video = ProgressiveMediaSource.Factory(factory).createMediaSource(MediaItem.fromUri(s.videoUrl))
            val audio = ProgressiveMediaSource.Factory(factory).createMediaSource(MediaItem.fromUri(s.audioUrl))
            exo.setMediaSource(MergingMediaSource(video, audio))
        } else return@LaunchedEffect
        exo.prepare()
        exo.playWhenReady = true
    }

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onExit) { Icon(Icons.Filled.ArrowBack, "Volver al casete", tint = Color.White) }
            Text("Mix.Casete · Modo YouTube", color = Color(0xFFDDDDDD), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        val vid = extractId(currentVideoUrl)
        when {
            stream != null && stream!!.playable -> AndroidView(
                modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f),
                factory = { ctx ->
                    PlayerView(ctx).apply {
                        player = exo; useController = true
                        resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                        setBackgroundColor(0xFF000000.toInt())
                    }
                }
            )
            vid != null -> EmbedFallback(videoId = vid, modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f))
            else -> Box(
                Modifier.fillMaxWidth().aspectRatio(16f / 9f).background(Color(0xFF111111)),
                contentAlignment = Alignment.Center
            ) { Text("Reproduce un tema primero", color = Color(0xFF888888), fontSize = 13.sp) }
        }

        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(cassette.title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 2)
            Text(cassette.artist, color = Color(0xFFAAAAAA), fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.tvPrev() }) { Icon(Icons.Filled.SkipPrevious, "Anterior", tint = Color.White) }
                IconButton(onClick = { if (exo.isPlaying) exo.pause() else exo.play() }) {
                    Icon(if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Play/Pausa", tint = Color.White, modifier = Modifier.size(40.dp))
                }
                IconButton(onClick = { viewModel.tvNext() }) { Icon(Icons.Filled.SkipNext, "Siguiente", tint = Color.White) }
                IconButton(onClick = { currentPlaylist.getOrNull(currentIndex)?.let { viewModel.toggleFavorite(it) } }) {
                    Icon(if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, "Favorito", tint = if (isFavorite) Color(0xFFE57373) else Color.White)
                }
                IconButton(onClick = { viewModel.recordCurrentTrack() }) { Icon(Icons.Filled.FiberManualRecord, "Grabar", tint = Color(0xFFE53935)) }
            }
        }
    }
}

private fun extractId(url: String?): String? {
    if (url == null) return null
    return when {
        url.contains("watch?v=") -> url.substringAfter("watch?v=").substringBefore("&")
        url.contains("youtu.be/") -> url.substringAfter("youtu.be/").substringBefore("?")
        else -> null
    }
}
