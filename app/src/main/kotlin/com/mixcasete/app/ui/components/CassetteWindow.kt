package com.mixcasete.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.mixcasete.app.player.CassetteState
import com.mixcasete.app.player.PlayState

@Composable
fun CassetteWindow(
    modifier: Modifier = Modifier,
    isLidOpen: Boolean,
    cassette: CassetteState,
    playState: PlayState
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF111111))
            .drawWithContent {
                drawContent()
                drawRoundRect(
                    color = Color.Black,
                    cornerRadius = CornerRadius(12.dp.toPx()),
                    style = Stroke(width = 8f)
                )
            },
        contentAlignment = Alignment.Center
    ) {
        Cassette(
            cassette = cassette,
            playState = playState,
            modifier = Modifier
                .fillMaxSize()
                .padding(8.dp)
        )
        
        CassetteLid(
            modifier = Modifier.fillMaxSize(),
            isOpen = isLidOpen
        )
    }
}
