package com.mixcasete.app.audio

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow

// Estado de CONTROL: cambia solo en load/play/pause/ended/error/stop.
// NO incluye el tick de progreso (ese va por positionFlow/durationFlow)
// para que un reporte de tiempo no recomponga el arbol que contiene el WebView.
data class YtState(
    val videoId: String? = null,
    val title: String = "",
    val artist: String = "",
    val isPlaying: Boolean = false,
    val stopped: Boolean = false,
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

    // Tick de progreso: lo consume SOLO CassettePlayer (hoja). Barato, sin cascada.
    private val _position = MutableStateFlow(0f)
    val position: StateFlow<Float> = _position.asStateFlow()
    private val _duration = MutableStateFlow(0f)
    val duration: StateFlow<Float> = _duration.asStateFlow()

    private val _activeId = MutableStateFlow<String?>(null)
    val activeId: StateFlow<String?> = _activeId.asStateFlow()

    private val _commands = MutableSharedFlow<YtCmd>(extraBufferCapacity = 16)
    val commands: SharedFlow<YtCmd> = _commands.asSharedFlow()

    var controller: YtController? = null
    var pendingId: String? = null

    fun setPosition(sec: Float) { _position.value = sec }
    fun setDuration(d: Float) { _duration.value = d }
    fun setState(update: (YtState) -> YtState) { _state.value = update(_state.value) }

    fun emit(cmd: YtCmd) {
        when (cmd) {
            is YtCmd.Load -> {
                _state.value = _state.value.copy(
                    videoId = cmd.id, title = cmd.title, artist = cmd.artist,
                    isPlaying = false, stopped = false, error = null
                )
                _position.value = 0f; _duration.value = 0f
                _activeId.value = cmd.id
                if (controller == null) pendingId = cmd.id
            }
            YtCmd.Stop -> {
                // NO limpiamos activeId: asi el WebView NO se desmonta ni se destruye.
                // Solo pausa + reset de posicion; PLAY reanuda con el gesto ya concedido.
                _state.value = _state.value.copy(isPlaying = false, stopped = true)
                _position.value = 0f
            }
            YtCmd.Play, YtCmd.Toggle -> {
                _state.value = _state.value.copy(stopped = false)
            }
            else -> { /* Pause/Seek/Next/Prev: solo ejecucion */ }
        }
        _commands.tryEmit(cmd)
    }

    interface YtController {
        fun load(id: String)
        fun play()
        fun pause()
        fun seek(sec: Float)
        fun stop()
    }
}
