package com.mixcasete.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mixcasete.app.player.PlayState

@Composable
fun Keyboard(
    modifier: Modifier,
    playState: PlayState,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onEject: () -> Unit,
    onRewind: () -> Unit,
    onFastForward: () -> Unit
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        KeyButton(icon = Icons.Filled.FastRewind, label = "REW", onClick = onRewind, modifier = Modifier.weight(1f))
        KeyButton(icon = Icons.Filled.PlayArrow, label = "PLAY", onClick = onPlayPause, modifier = Modifier.weight(1f))
        KeyButton(icon = Icons.Filled.Pause, label = "PAUSE", onClick = onPlayPause, modifier = Modifier.weight(1f))
        KeyButton(icon = Icons.Filled.Stop, label = "STOP", onClick = onStop, modifier = Modifier.weight(1f))
        KeyButton(icon = Icons.Filled.Eject, label = "EJECT", onClick = onEject, modifier = Modifier.weight(1f))
        KeyButton(icon = Icons.Filled.FastForward, label = "FF", onClick = onFastForward, modifier = Modifier.weight(1f))
    }
}

@Composable
fun KeyButton(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()

    Box(
        modifier = modifier
            .fillMaxHeight()
            .padding(horizontal = 3.dp)
            .padding(bottom = if (isPressed) 0.dp else 2.dp)
            .background(if (isPressed) Color(0xFF4A4A4A) else Color(0xFF303030), RoundedCornerShape(4.dp))
            .border(width = 2.dp, color = Color(0xFFC9C9C9), shape = RoundedCornerShape(4.dp))
            .clickable(interactionSource = interactionSource, indication = null, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = Color(0xFFD6D6D6),
                modifier = Modifier.fillMaxHeight(0.45f)
            )
            Text(label, fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color(0xFFD6D6D6))
        }
    }
}
