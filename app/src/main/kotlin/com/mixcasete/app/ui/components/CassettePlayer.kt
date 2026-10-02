package com.mixcasete.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import com.mixcasete.app.player.CassetteState
import com.mixcasete.app.player.PlayState

@Composable
fun CassettePlayer(
    isPortrait: Boolean,
    playState: PlayState,
    isLidOpen: Boolean,
    cassette: CassetteState,
    calibrationMode: Boolean,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onEject: () -> Unit,
    onRewind: () -> Unit,
    onFastForward: () -> Unit,
    onToggleCalibration: () -> Unit
) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(if (isPortrait) 16.dp else 8.dp),
        contentAlignment = Alignment.Center
    ) {
        val maxW = maxWidth
        val maxH = maxHeight

        val playerWidth = maxW * 0.9f
        val playerHeight = if (isPortrait) maxH * 0.85f else maxH * 0.95f

        Box(
            modifier = Modifier
                .width(playerWidth)
                .height(playerHeight)
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                drawPlayerBody(isPortrait, calibrationMode)
            }

            Box(modifier = Modifier.fillMaxSize()) {
                PlayerControlsOverlay(
                    isPortrait = isPortrait,
                    playState = playState,
                    onPlayPause = onPlayPause,
                    onStop = onStop,
                    onEject = onEject,
                    onRewind = onRewind,
                    onFastForward = onFastForward,
                    onToggleCalibration = onToggleCalibration
                )

                CassetteWindow(
                    modifier = Modifier
                        .fillMaxWidth(0.8f)
                        .fillMaxHeight(if (isPortrait) 0.4f else 0.6f)
                        .align(Alignment.TopCenter)
                        .padding(top = playerHeight * 0.15f),
                    isLidOpen = isLidOpen,
                    cassette = cassette,
                    playState = playState
                )
            }
        }
    }
}

fun DrawScope.drawPlayerBody(isPortrait: Boolean, calibrationMode: Boolean) {
    val w = size.width
    val h = size.height
    val stroke = Stroke(width = w * 0.02f)
    val mainColor = Color(0xFFE8E4D9)
    val lineColor = Color.Black

    // Carcasa redondeada de línea gruesa
    drawRoundRect(color = mainColor, size = size, cornerRadius = CornerRadius(w * 0.05f))
    drawRoundRect(color = lineColor, size = size, cornerRadius = CornerRadius(w * 0.05f), style = stroke)

    // Rejilla de altavoz (solo vertical)
    if (isPortrait) {
        drawSpeakerGrille(Offset(w * 0.1f, h * 0.02f), Size(w * 0.8f, h * 0.1f), lineColor)
    }

    // Franja con perillas y pantallita
    val stripTop = h * (if (isPortrait) 0.58f else 0.1f)
    val stripHeight = h * (if (isPortrait) 0.1f else 0.8f)

    if (isPortrait) {
        drawRect(color = Color(0xFF9DB59D), topLeft = Offset(w * 0.1f, stripTop), size = Size(w * 0.5f, stripHeight))
        drawRect(color = lineColor, topLeft = Offset(w * 0.1f, stripTop), size = Size(w * 0.5f, stripHeight), style = stroke)
        for (i in 0 until 3) {
            drawKnob(Offset(w * (0.7f + i * 0.1f), stripTop + stripHeight / 2), w * 0.04f, lineColor)
        }
    } else {
        for (i in 0 until 3) {
            drawKnob(Offset(w * 0.15f, stripTop + stripHeight * (0.2f + i * 0.3f)), w * 0.05f, lineColor)
        }
        drawRect(color = Color(0xFF9DB59D), topLeft = Offset(w * 0.7f, stripTop + stripHeight * 0.1f), size = Size(w * 0.25f, stripHeight * 0.8f))
        drawRect(color = lineColor, topLeft = Offset(w * 0.7f, stripTop + stripHeight * 0.1f), size = Size(w * 0.25f, stripHeight * 0.8f), style = stroke)
    }

    // Patas
    drawRect(color = lineColor, topLeft = Offset(w * 0.1f, h * 0.95f), size = Size(w * 0.1f, h * 0.05f))
    drawRect(color = lineColor, topLeft = Offset(w * 0.8f, h * 0.95f), size = Size(w * 0.1f, h * 0.05f))

    // Modo calibración (debug)
    if (calibrationMode) {
        drawRect(color = Color.Red.copy(alpha = 0.2f), size = size)
        drawRoundRect(color = Color.Red, topLeft = Offset(w * 0.08f, h * 0.12f), size = Size(w * 0.84f, h * 0.42f), style = Stroke(4f))
    }
}

fun DrawScope.drawSpeakerGrille(topLeft: Offset, size: Size, lineColor: Color) {
    val cols = 15
    val rows = 4
    val dotRadius = size.width / cols / 4
    for (r in 0 until rows) {
        for (c in 0 until cols) {
            drawCircle(
                color = lineColor,
                radius = dotRadius,
                center = Offset(topLeft.x + size.width * (c + 0.5f) / cols, topLeft.y + size.height * (r + 0.5f) / rows)
            )
        }
    }
}

fun DrawScope.drawKnob(center: Offset, radius: Float, color: Color) {
    drawCircle(color = color, radius = radius, center = center)
    drawCircle(color = Color.White, radius = radius * 0.8f, center = center)
    drawLine(color = color, start = center, end = Offset(center.x + radius * 0.6f, center.y - radius * 0.6f), strokeWidth = radius * 0.2f)
}
