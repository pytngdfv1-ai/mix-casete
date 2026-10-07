package com.mixcasete.app.audio

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

data class YtState(
    val videoId: String? = null,
    val title: String = "",
    val artist: String = "",
    val isPlaying: Boolean = false,
    val positionSec: Float = 0f,
    val durationSec: Float = 0f,
    val ready: Boolean = false,
    val error: Int? = null
)

sealed class YtCmd {
    object Play : YtCmd()
    object Pause : YtCmd()
    object Toggle : YtCmd()
    object Next : YtCmd()
    object Prev : YtCmd()
    object Stop : YtCmd()
    data class Seek(val sec: Float) : YtCmd()
    data class Load(val id: String, val title: String, val artist: String) : YtCmd()
}

object YtBridge {
    private val _state = MutableStateFlow(YtState())
    val state: StateFlow<YtState> = _state.asStateFlow()

    // El tema que DEBE estar sonando. Lo setea emit() de forma deterministica,
    // independiente de si el WebView ya esta montado. Rompe el deadlock de montaje.
    private val _activeId = MutableStateFlow<String?>(null)
    val activeId: StateFlow<String?> = _activeId.asStateFlow()

    private val _commands = MutableSharedFlow<YtCmd>(extraBufferCapacity = 16)
    val commands: SharedFlow<YtCmd> = _commands.asSharedFlow()

    var controller: YtController? = null
    var pendingId: String? = null

    fun emit(cmd: YtCmd) {
        when (cmd) {
            is YtCmd.Load -> {
                // Setear estado SIEMPRE, aunque el WebView no exista todavia
                _state.value = _state.value.copy(
                    videoId = cmd.id, title = cmd.title, artist = cmd.artist,
                    positionSec = 0f, durationSec = 0f, error = null
                )
                _activeId.value = cmd.id
                if (controller == null) pendingId = cmd.id
            }
            YtCmd.Stop -> {
                _state.value = _state.value.copy(videoId = null, isPlaying = false)
                _activeId.value = null
                pendingId = null
            }
            else -> { /* Play/Pause/Toggle/Seek/Next/Prev: solo ejecucion, sin cambiar activeId */ }
        }
        _commands.tryEmit(cmd)
    }

    fun setState(update: (YtState) -> YtState) { _state.value = update(_state.value) }

    interface YtController {
        fun load(id: String)
        fun play()
        fun pause()
        fun seek(sec: Float)
        fun stop()
    }
}
