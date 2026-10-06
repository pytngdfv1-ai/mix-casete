package com.mixcasete.app.ui

import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Tv
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.mixcasete.app.audio.AudioPlayerViewModel
import com.mixcasete.app.tv.TvActivity
import com.mixcasete.app.ui.components.CassettePlayer
import com.mixcasete.app.ui.screens.LoginContent
import com.mixcasete.app.ui.screens.PlaylistContent
import com.mixcasete.app.ui.screens.SearchContent
import com.mixcasete.app.ui.screens.YouTubeLikeView
import kotlin.math.roundToInt

data class Zone(val x0: Float, val y0: Float, val x1: Float, val y1: Float) {
    val w: Float get() = x1 - x0
    val h: Float get() = y1 - y0
}
data class PlayerZones(
    val speaker: Zone?, val screen: Zone?, val knobs: Zone?, val window: Zone,
    val functionBar: Zone, val keyboard: Zone, val sideGrille: Zone?, val legL: Zone, val legR: Zone
) {
    fun namedZones(): List<Pair<String, Zone>> = listOfNotNull(
        speaker?.let { "speaker" to it }, screen?.let { "screen" to it }, knobs?.let { "knobs" to it },
        "window" to window, "functionBar" to functionBar, "keyboard" to keyboard, sideGrille?.let { "side" to it }
    )
}
fun portraitZones() = PlayerZones(
    speaker = Zone(0.07f, 0.015f, 0.93f, 0.335f), screen = Zone(0.07f, 0.35f, 0.60f, 0.405f),
    knobs = Zone(0.63f, 0.345f, 0.93f, 0.41f), window = Zone(0.08f, 0.425f, 0.92f, 0.715f),
    functionBar = Zone(0.08f, 0.73f, 0.92f, 0.775f), keyboard = Zone(0.13f, 0.79f, 0.87f, 0.955f),
    sideGrille = Zone(0.895f, 0.79f, 0.965f, 0.90f), legL = Zone(0.10f, 0.965f, 0.20f, 1.0f), legR = Zone(0.80f, 0.965f, 0.90f, 1.0f)
)
fun landscapeZones() = PlayerZones(
    speaker = null, screen = null, knobs = null, window = Zone(0.015f, 0.015f, 0.985f, 0.72f),
    functionBar = Zone(0.015f, 0.735f, 0.985f, 0.79f), keyboard = Zone(0.015f, 0.805f, 0.985f, 0.955f),
    sideGrille = null, legL = Zone(0.06f, 0.965f, 0.14f, 1.0f), legR = Zone(0.86f, 0.965f, 0.94f, 1.0f)
)
fun Modifier.fillZone(zone: Zone): Modifier = this.then(
    Modifier.layout { measurable, constraints ->
        val pw = constraints.maxWidth; val ph = constraints.maxHeight
        val zw = (zone.w * pw).roundToInt(); val zh = (zone.h * ph).roundToInt()
        val placeable = measurable.measure(Constraints.fixed(zw, zh))
        layout(pw, ph) { placeable.placeRelative((zone.x0 * pw).roundToInt(), (zone.y0 * ph).roundToInt()) }
    }
)

@Composable
fun SkinLayout(viewModel: AudioPlayerViewModel = viewModel()) {
    val context = LocalContext.current
    val playState by viewModel.playState.collectAsState()
    val isLidOpen by viewModel.isLidOpen.collectAsState()
    val cassette by viewModel.cassette.collectAsState()
    val calibrationMode by viewModel.calibrationMode.collectAsState()
    val errorInfo by viewModel.errorInfo.collectAsState()
    val recMessage by viewModel.recMessage.collectAsState()
    val showSearch by viewModel.showSearchScreen.collectAsState()
    val showPlaylist by viewModel.showPlaylistScreen.collectAsState()
    val showLogin by viewModel.showLoginScreen.collectAsState()
    val isShuffle by viewModel.isShuffleEnabled.collectAsState()
    val repeatMode by viewModel.repeatMode.collectAsState()
    val currentPlaylist by viewModel.currentPlaylist.collectAsState()
    val currentSongIndex by viewModel.currentSongIndex.collectAsState()
    val currentVideoUrl by viewModel.currentVideoUrl.collectAsState()

    var youTubeMode by remember { mutableStateOf(false) }
    val isFavorite = currentPlaylist.getOrNull(currentSongIndex)?.isFavorite ?: false

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize().background(Color(0xFF121212)),
        contentAlignment = Alignment.Center
    ) {
        if (youTubeMode) {
            YouTubeLikeView(viewModel = viewModel, onExit = { youTubeMode = false })
        } else {
            val isPortrait = maxHeight > maxWidth
            val zones = if (isPortrait) portraitZones() else landscapeZones()
            val bodyW: Dp; val bodyH: Dp
            if (isPortrait) { val h = maxHeight * 0.88f; val w = maxWidth * 0.92f; bodyH = h; bodyW = if (w < h * 0.62f) w else h * 0.62f }
            else { val w = maxWidth * 0.96f; val h = maxHeight * 0.92f; bodyW = w; bodyH = if (h < w * 0.48f) h else w * 0.48f }

            Box(modifier = Modifier.width(bodyW).height(bodyH)) {
                CassettePlayer(
                    zones = zones, playState = playState, isLidOpen = isLidOpen, cassette = cassette,
                    calibrationMode = calibrationMode, errorInfo = errorInfo, recMessage = recMessage,
                    isShuffle = isShuffle, repeatMode = repeatMode, isFavorite = isFavorite,
                    onPlayPause = viewModel::togglePlayPause, onStop = viewModel::stop, onRecord = viewModel::recordCurrentTrack,
                    onRewind = viewModel::rewind, onFastForward = viewModel::fastForward, onToggleCalibration = viewModel::toggleCalibration,
                    onSearch = viewModel::toggleSearchScreen, onLists = viewModel::togglePlaylistScreen,
                    onPrev = viewModel::playPreviousSong, onNext = viewModel::playNextSong,
                    onShuffle = viewModel::toggleShuffle, onRepeat = viewModel::cycleRepeatMode,
                    onFavorite = { currentPlaylist.getOrNull(currentSongIndex)?.let { viewModel.toggleFavorite(it) } },
                    onShare = { viewModel.pauseForTv(); context.startActivity(Intent(context, TvActivity::class.java).putExtra("videoUrl", currentVideoUrl)) }
                )
            }

            IconButton(
                onClick = { viewModel.pauseForTv(); youTubeMode = true },
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp).size(40.dp).clip(CircleShape).background(Color(0xFF3A3A3A))
            ) { Icon(Icons.Filled.Tv, contentDescription = "Modo YouTube", tint = Color(0xFF81C784)) }
        }

        if (showSearch) OverlayPanel(isPortrait = maxHeight > maxWidth, onClose = viewModel::toggleSearchScreen) { SearchContent(viewModel) }
        else if (showPlaylist) OverlayPanel(isPortrait = maxHeight > maxWidth, onClose = viewModel::togglePlaylistScreen) { PlaylistContent(viewModel) }
        if (showLogin) LoginContent(viewModel)
    }
}

@Composable
private fun OverlayPanel(isPortrait: Boolean, onClose: () -> Unit, content: @Composable () -> Unit) {
    Box(
        modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.55f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onClose)
    ) {
        Box(
            modifier = Modifier.then(
                if (isPortrait) Modifier.align(Alignment.BottomCenter).fillMaxWidth().fillMaxHeight(0.65f)
                else Modifier.align(Alignment.CenterEnd).fillMaxHeight().fillMaxWidth(0.45f)
            ).background(Color(0xFF1C1C1C))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = { })
        ) {
            Box(Modifier.fillMaxSize().padding(top = 40.dp)) { content() }
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd).padding(6.dp).size(30.dp).clip(CircleShape).background(Color(0xFF3A3A3A))) {
                Icon(Icons.Filled.Close, contentDescription = "Cerrar", tint = Color(0xFFD6D6D6), modifier = Modifier.size(16.dp))
            }
        }
    }
}
