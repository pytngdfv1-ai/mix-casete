package com.mixcasete.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mixcasete.app.R
import com.mixcasete.app.player.CassetteState
import com.mixcasete.app.player.PlayState
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun Cassette(modifier: Modifier, cassette: CassetteState, playState: PlayState) {
    val isPlaying = playState == PlayState.PLAYING
    val rot = remember { Animatable(0f) }
    LaunchedEffect(isPlaying) {
        if (isPlaying) {
            while (true) {
                rot.animateTo(rot.value + 360f, tween(1800, easing = LinearEasing))
            }
        }
    }
    val rotation = rot.value

    BoxWithConstraints(modifier = modifier.clip(RoundedCornerShape(6.dp))) {
        val cw = maxWidth
        val ch = maxHeight
        Box(Modifier.fillMaxSize()) {
            Image(
                painter = painterResource(id = R.drawable.cassette_photo),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
                colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.15f) })
            )
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.45f)))

            Canvas(Modifier.fillMaxSize()) {
                val w = size.width
                val h = size.height
                for (i in 0 until 7) {
                    val x = w * (0.465f + i * 0.012f)
                    drawLine(
                        color = Color(0xFFDDDDDD),
                        start = Offset(x, h * 0.44f),
                        end = Offset(x, h * 0.52f),
                        strokeWidth = w * 0.004f
                    )
                }
            }

            Reel(
                modifier = Modifier
                    .size(ch * 0.66f)
                    .align(Alignment.TopStart)
                    .offset(x = cw * 0.325f - ch * 0.33f, y = ch * 0.50f - ch * 0.33f),
                rotation = rotation,
                tapeFraction = 1f - cassette.progress
            )
            Reel(
                modifier = Modifier
                    .size(ch * 0.66f)
                    .align(Alignment.TopStart)
                    .offset(x = cw * 0.675f - ch * 0.33f, y = ch * 0.50f - ch * 0.33f),
                rotation = rotation,
                tapeFraction = cassette.progress
            )

            // ETIQUETA: se dibuja al final para quedar POR ENCIMA de la cinta y los rodillos
            Box(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .align(Alignment.TopCenter)
                    .padding(top = ch * 0.05f)
                    .background(Color(0xDD141414), RoundedCornerShape(3.dp))
                    .padding(vertical = ch * 0.015f),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(cassette.title, color = Color(0xFFF2F2F2), fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                    Text(cassette.artist, color = Color(0xFFBDBDBD), fontSize = 9.sp, maxLines = 1)
                }
            }
        }
    }
}

@Composable
fun Reel(modifier: Modifier, rotation: Float, tapeFraction: Float) {
    // Paquete de cinta (no gira visualmente): anillos concéntricos
    Canvas(modifier) {
        val s = size.minDimension
        val c = Offset(size.width / 2, size.height / 2)
        val tapeR = s * (0.24f + 0.23f * tapeFraction)
        drawCircle(color = Color(0xFF241A10), radius = tapeR, center = c)
        for (i in 1..4) {
            drawCircle(
                color = Color(0xFF3A2A1A),
                radius = tapeR * (i / 5f),
                center = c,
                style = Stroke(width = s * 0.006f)
            )
        }
        drawCircle(color = Color(0xFF120C06), radius = tapeR, center = c, style = Stroke(width = s * 0.015f))
    }

    // Parte giratoria: hub blanco con radios, engranaje negro y pin metálico
    Canvas(modifier.graphicsLayer { rotationZ = rotation }) {
        val s = size.minDimension
        val c = Offset(size.width / 2, size.height / 2)
        val hubR = s * 0.21f

        drawCircle(color = Color(0xFFE8E8E8), radius = hubR, center = c)
        drawCircle(color = Color(0xFFB5B5B5), radius = hubR, center = c, style = Stroke(width = s * 0.008f))

        for (i in 0 until 12) {
            val a = i * PI / 6.0
            drawLine(
                color = Color(0xFFC9C9C9),
                start = Offset(c.x + cos(a).toFloat() * hubR * 0.55f, c.y + sin(a).toFloat() * hubR * 0.55f),
                end = Offset(c.x + cos(a).toFloat() * hubR * 0.92f, c.y + sin(a).toFloat() * hubR * 0.92f),
                strokeWidth = s * 0.012f
            )
        }

        val cogR = hubR * 0.52f
        drawCircle(color = Color(0xFF111111), radius = cogR, center = c)
        for (i in 0 until 6) {
            val a = i * PI / 3.0
            drawCircle(
                color = Color(0xFF111111),
                radius = cogR * 0.30f,
                center = Offset(c.x + cos(a).toFloat() * cogR * 1.05f, c.y + sin(a).toFloat() * cogR * 1.05f)
            )
        }

        drawCircle(color = Color(0xFFF2F2F2), radius = cogR * 0.45f, center = c)
        drawCircle(color = Color(0xFF8F8F8F), radius = cogR * 0.20f, center = c)
        drawCircle(color = Color(0xFF5E5E5E), radius = cogR * 0.10f, center = c)
    }
}
