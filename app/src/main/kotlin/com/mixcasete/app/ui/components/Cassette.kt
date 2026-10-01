package com.mixcasete.app.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
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
fun Cassette(
    modifier: Modifier = Modifier,
    cassette: CassetteState,
    playState: PlayState
) {
    val infiniteTransition = rememberInfiniteTransition(label = "reels")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    val isPlaying = playState == PlayState.PLAYING

    Box(
        modifier = modifier
            .aspectRatio(1.6f)
            .clip(RoundedCornerShape(8.dp))
            .background(Color(0xFF222222))
    ) {
        // Imagen de fondo del casete (oscurecida)
        Image(
            painter = painterResource(id = R.drawable.cassette_photo),
            contentDescription = "Casete",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            colorFilter = ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.1f) })
        )

        // Etiqueta central
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .fillMaxHeight(0.4f)
                .align(Alignment.Center)
                .background(Color(0xFF111111))
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(cassette.title, color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 1)
                Text(cassette.artist, color = Color.LightGray, fontSize = 10.sp, maxLines = 1)
            }
        }

        // Carretes (Reels)
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Reel(rotation = if (isPlaying) rotation else 0f, tapeRadiusFraction = 1f - cassette.progress)
            Reel(rotation = if (isPlaying) rotation else 0f, tapeRadiusFraction = cassette.progress)
        }
    }
}

@Composable
fun Reel(modifier: Modifier = Modifier, rotation: Float, tapeRadiusFraction: Float) {
    Box(modifier = modifier.aspectRatio(1f).fillMaxHeight(0.6f)) {
        // Disco de cinta
        Canvas(modifier = Modifier.fillMaxSize()) {
            val maxRadius = size.minDimension / 2
            val tapeRadius = maxRadius * (0.4f + tapeRadiusFraction * 0.5f)
            drawCircle(color = Color(0xFF3A2A1A), radius = tapeRadius, center = Offset(size.width / 2, size.height / 2))
        }
        
        // Eje giratorio
        Canvas(modifier = Modifier.fillMaxSize().graphicsLayer { rotationZ = rotation }) {
            val center = Offset(size.width / 2, size.height / 2)
            val hubRadius = size.minDimension * 0.2f
            drawCircle(color = Color.White, radius = hubRadius, center = center)
            for (i in 0 until 6) {
                val angle = i * (2 * PI / 6)
                val x = (cos(angle) * hubRadius * 0.6f).toFloat()
                val y = (sin(angle) * hubRadius * 0.6f).toFloat()
                drawCircle(color = Color.Black, radius = hubRadius * 0.2f, center = Offset(center.x + x, center.y + y))
            }
        }
    }
}
