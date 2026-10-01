package com.mixcasete.app.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Eject
import androidx.compose.material.icons.filled.FastForward
import androidx.compose.material.icons.filled.FastRewind
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mixcasete.app.player.PlayState

@Composable
fun PlayerControlsOverlay(
    isPortrait: Boolean,
    playState: PlayState,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onEject: () -> Unit,
    onRewind: () -> Unit,
    onFastForward: () -> Unit,
    onToggleCalibration: () -> Unit
) {
    Box(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(60.dp)
                .align(Alignment.BottomCenter)
                .padding(bottom = if (isPortrait) 24.dp else 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            CassetteButton(icon = Icons.Filled.FastRewind, onClick = onRewind)
            CassetteButton(
                icon = if (playState == PlayState.PLAYING) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                onClick = onPlayPause
            )
            CassetteButton(icon = Icons.Filled.Stop, onClick = onStop)
            CassetteButton(icon = Icons.Filled.Eject, onClick = onEject)
            CassetteButton(icon = Icons.Filled.FastForward, onClick = onFastForward)
        }
        
        Text(
            text = "CALIB",
            color = Color.Red,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(8.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(Color.Black.copy(alpha = 0.6f))
                .padding(4.dp)
                .clickable { onToggleCalibration() }
        )
    }
}

@Composable
fun CassetteButton(icon: ImageVector, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val offsetY by animateDpAsState(targetValue = if (isPressed) 4.dp else 0.dp, animationSpec = tween(100))

    Box(
        modifier = Modifier
            .size(48.dp)
            .offset(y = offsetY)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFFE0E0E0))
            .drawWithContent {
                drawContent()
                drawRect(color = Color.Black, style = androidx.compose.ui.graphics.drawscope.Stroke(width = 4f))
            }
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = Color.Black, modifier = Modifier.size(24.dp))
    }
}
