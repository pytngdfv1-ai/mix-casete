package com.mixcasete.app.ui.screens

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mixcasete.app.audio.AudioPlayerViewModel
import com.mixcasete.app.audio.PlayState
import com.mixcasete.app.ui.components.OfficialPlayer

@Composable
fun YouTubeLikeView(viewModel: AudioPlayerViewModel, onExit: () -> Unit) {
    val cassette by viewModel.cassette.collectAsState()
    val playState by viewModel.playState.collectAsState()
    val currentPlaylist by viewModel.currentPlaylist.collectAsState()
    val currentIndex by viewModel.currentSongIndex.collectAsState()
    val isFavorite = currentPlaylist.getOrNull(currentIndex)?.isFavorite ?: false

    Column(Modifier.fillMaxSize().background(Color.Black)) {
        Row(modifier = Modifier.fillMaxWidth().padding(8.dp), verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onExit) { Icon(Icons.Filled.ArrowBack, contentDescription = "Volver al casete", tint = Color.White) }
            Text("Mix.Casete · Modo YouTube", color = Color(0xFFDDDDDD), fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }

        OfficialPlayer(viewModel = viewModel, modifier = Modifier.fillMaxWidth().aspectRatio(16f / 9f))

        Column(Modifier.fillMaxWidth().padding(16.dp)) {
            Text(cassette.title, color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold, maxLines = 2)
            Text(cassette.artist, color = Color(0xFFAAAAAA), fontSize = 13.sp, modifier = Modifier.padding(top = 4.dp))
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = { viewModel.tvPrev() }) { Icon(Icons.Filled.SkipPrevious, "Anterior", tint = Color.White) }
                IconButton(onClick = { viewModel.togglePlayPause() }) {
                    Icon(if (playState == PlayState.PLAYING) Icons.Filled.Pause else Icons.Filled.PlayArrow, "Play/Pausa", tint = Color.White, modifier = Modifier.size(40.dp))
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
