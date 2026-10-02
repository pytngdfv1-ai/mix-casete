package com.mixcasete.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mixcasete.app.player.PlayerViewModel
import com.mixcasete.app.ui.components.CassettePlayer
import kotlin.math.roundToInt

data class Zone(val x0: Float, val y0: Float, val x1: Float, val y1: Float) {
    val w: Float get() = x1 - x0
    val h: Float get() = y1 - y0
}

data class PlayerZones(
    val speaker: Zone?,
    val screen: Zone,
    val knobs: Zone,
    val window: Zone,
    val segments: Zone,
    val keyboard: Zone,
    val sideGrille: Zone,
    val legL: Zone,
    val legR: Zone
) {
    fun namedZones(): List<Pair<String, Zone>> = listOfNotNull(
        speaker?.let { "speaker" to it },
        "screen" to screen,
        "knobs" to knobs,
        "window" to window,
        "segments" to segments,
        "keyboard" to keyboard,
        "side" to sideGrille
    )
}

fun portraitZones() = PlayerZones(
    speaker = Zone(0.07f, 0.015f, 0.93f, 0.335f),
    screen = Zone(0.07f, 0.35f, 0.60f, 0.405f),
    knobs = Zone(0.63f, 0.345f, 0.93f, 0.41f),
    window = Zone(0.08f, 0.425f, 0.92f, 0.715f),
    segments = Zone(0.08f, 0.73f, 0.92f, 0.775f),
    keyboard = Zone(0.13f, 0.79f, 0.87f, 0.955f),
    sideGrille = Zone(0.895f, 0.79f, 0.965f, 0.90f),
    legL = Zone(0.10f, 0.965f, 0.20f, 1.0f),
    legR = Zone(0.80f, 0.965f, 0.90f, 1.0f)
)

fun landscapeZones() = PlayerZones(
    speaker = null,
    screen = Zone(0.635f, 0.08f, 0.955f, 0.30f),
    knobs = Zone(0.635f, 0.33f, 0.955f, 0.47f),
    window = Zone(0.045f, 0.06f, 0.60f, 0.80f),
    segments = Zone(0.635f, 0.50f, 0.955f, 0.56f),
    keyboard = Zone(0.10f, 0.845f, 0.90f, 0.975f),
    sideGrille = Zone(0.895f, 0.60f, 0.965f, 0.72f),
    legL = Zone(0.06f, 0.965f, 0.14f, 1.0f),
    legR = Zone(0.86f, 0.965f, 0.94f, 1.0f)
)

fun Modifier.fillZone(zone: Zone): Modifier = this.then(
    Modifier.layout { measurable, constraints ->
        val pw = constraints.maxWidth
        val ph = constraints.maxHeight
        val zw = (zone.w * pw).roundToInt()
        val zh = (zone.h * ph).roundToInt()
        val placeable = measurable.measure(Constraints.fixed(zw, zh))
        layout(pw, ph) {
            placeable.placeRelative((zone.x0 * pw).roundToInt(), (zone.y0 * ph).roundToInt())
        }
    }
)

@Composable
fun SkinLayout(viewModel: PlayerViewModel = viewModel()) {
    val playState by viewModel.playState.collectAsState()
    val isLidOpen by viewModel.isLidOpen.collectAsState()
    val cassette by viewModel.cassette.collectAsState()
    val calibrationMode by viewModel.calibrationMode.collectAsState()

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFEFEAD8)),
        contentAlignment = Alignment.Center
    ) {
        val isPortrait = maxHeight > maxWidth
        val zones = if (isPortrait) portraitZones() else landscapeZones()

        val bodyW: Dp
        val bodyH: Dp
        if (isPortrait) {
            val h = maxHeight * 0.88f
            val w = maxWidth * 0.92f
            bodyH = h
            bodyW = if (w < h * 0.62f) w else h * 0.62f
        } else {
            val w = maxWidth * 0.96f
            val h = maxHeight * 0.92f
            bodyW = w
            bodyH = if (h < w * 0.48f) h else w * 0.48f
        }

        Box(modifier = Modifier.width(bodyW).height(bodyH)) {
            CassettePlayer(
                zones = zones,
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
