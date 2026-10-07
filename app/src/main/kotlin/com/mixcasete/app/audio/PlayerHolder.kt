package com.mixcasete.app.audio

import androidx.media3.exoplayer.ExoPlayer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Puente entre el servicio (dueño del reproductor) y la UI (superficie de video). */
object PlayerHolder {
    private val _player = MutableStateFlow<ExoPlayer?>(null)
    val player: StateFlow<ExoPlayer?> = _player.asStateFlow()
    fun set(p: ExoPlayer?) { _player.value = p }
}
