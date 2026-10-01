package com.mixcasete.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mixcasete.app.player.PlayerViewModel
import com.mixcasete.app.ui.components.CassettePlayer

@Composable
fun SkinLayout(viewModel: PlayerViewModel = viewModel()) {
    val playState by viewModel.playState.collectAsState()
    val isLidOpen by viewModel.isLidOpen.collectAsState()
    val cassette by viewModel.cassette.collectAsState()
    val calibrationMode by viewModel.calibrationMode.collectAsState()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFF5F5DC)) // Fondo claro retro
    ) {
        val isPortrait = maxHeight > maxWidth

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            CassettePlayer(
                isPortrait = isPortrait,
                playState = playState,
                isLidOpen = isLidOpen,
                cassette = cassette,
                calibrationMode = calibrationMode,
                onPlayPause = viewModel::togglePlayPause,
                onStop = viewModel::stop,
                onEject = viewModel::eject,
                onRewind = viewModel::rewind,
                onFastForward = viewModel::fastForward,
                onToggleCalibration = viewModel::toggleCalibration
            )
        }
    }
}
