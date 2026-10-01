package com.mixcasete.app.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class PlayState {
    STOPPED, PLAYING, PAUSED, EJECTED
}

data class CassetteState(
    val title: String = "Mix Tape Vol. 1",
    val artist: String = "DJ Retro",
    val progress: Float = 0f // 0.0 a 1.0
)

class PlayerViewModel : ViewModel() {
    private val _playState = MutableStateFlow(PlayState.STOPPED)
    val playState: StateFlow<PlayState> = _playState.asStateFlow()

    private val _isLidOpen = MutableStateFlow(false)
    val isLidOpen: StateFlow<Boolean> = _isLidOpen.asStateFlow()

    private val _cassette = MutableStateFlow(CassetteState())
    val cassette: StateFlow<CassetteState> = _cassette.asStateFlow()

    private val _calibrationMode = MutableStateFlow(false)
    val calibrationMode: StateFlow<Boolean> = _calibrationMode.asStateFlow()

    fun togglePlayPause() {
        if (_isLidOpen.value) return // No reproducir si la tapa está abierta
        if (_playState.value == PlayState.PLAYING) {
            _playState.value = PlayState.PAUSED
        } else {
            _playState.value = PlayState.PLAYING
            startSimulation()
        }
    }

    fun stop() {
        _playState.value = PlayState.STOPPED
        _cassette.value = _cassette.value.copy(progress = 0f)
    }

    fun eject() {
        _playState.value = PlayState.EJECTED
        _isLidOpen.value = !_isLidOpen.value
    }

    fun rewind() {
        val newProgress = (_cassette.value.progress - 0.1f).coerceAtLeast(0f)
        _cassette.value = _cassette.value.copy(progress = newProgress)
    }

    fun fastForward() {
        val newProgress = (_cassette.value.progress + 0.1f).coerceAtMost(1f)
        _cassette.value = _cassette.value.copy(progress = newProgress)
    }

    fun toggleCalibration() {
        _calibrationMode.value = !_calibrationMode.value
    }

    private fun startSimulation() {
        viewModelScope.launch {
            while (_playState.value == PlayState.PLAYING) {
                delay(100)
                val current = _cassette.value.progress
                if (current < 1f) {
                    _cassette.value = _cassette.value.copy(progress = (current + 0.001f).coerceAtMost(1f))
                } else {
                    _playState.value = PlayState.STOPPED
                    break
                }
            }
        }
    }
}
