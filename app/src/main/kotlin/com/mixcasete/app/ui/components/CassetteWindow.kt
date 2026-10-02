package com.mixcasete.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.mixcasete.app.player.CassetteState
import com.mixcasete.app.player.PlayState

@Composable
fun CassetteWindow(
    modifier: Modifier,
    isLidOpen: Boolean,
    cassette: CassetteState,
    playState: PlayState
) {
    Box(
        modifier = modifier.background(Color(0xFF101010)),
        contentAlignment = Alignment.Center
    ) {
        BoxWithConstraints(contentAlignment = Alignment.Center) {
            val cw = minOf(maxWidth * 0.94f, maxHeight * 0.94f * 1.6f)
            val ch = cw / 1.6f
            Box(Modifier.size(cw, ch)) {
                Cassette(modifier = Modifier.fillMaxSize(), cassette = cassette, playState = playState)
                CassetteLid(modifier = Modifier.fillMaxSize(), isOpen = isLidOpen)
            }
        }
    }
}
