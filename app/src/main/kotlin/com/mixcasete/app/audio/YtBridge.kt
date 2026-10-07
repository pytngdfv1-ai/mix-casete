package com.mixcasete.app.audio

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

/** Estado del reproductor oficial (lo reporta el WebView via bridge JS). */
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

/** Comandos que la UI y la notificacion envian al WebView. */
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

    private val _commands = MutableSharedFlow<YtCmd>(extraBufferCapacity = 16)
    val commands: SharedFlow<YtCmd> = _commands.asSharedFlow()

    // Controlador real (lo setea el WebView al montarse)
    var controller: YtController? = null
    var pendingId: String? = null

    fun emit(cmd: YtCmd) { _commands.tryEmit(cmd) }

    fun setState(update: (YtState) -> YtState) { _state.value = update(_state.value) }

    interface YtController {
        fun load(id: String)
        fun play()
        fun pause()
        fun seek(sec: Float)
        fun stop()
    }
}
