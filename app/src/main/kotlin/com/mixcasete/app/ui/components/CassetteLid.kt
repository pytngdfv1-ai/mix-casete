package com.mixcasete.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun CassetteLid(
    modifier: Modifier = Modifier,
    isOpen: Boolean
) {
    val rotationX by animateFloatAsState(
        targetValue = if (isOpen) -75f else 0f,
        animationSpec = tween(durationMillis = 600),
        label = "lidRotation"
    )

    Box(
        modifier = modifier
            .graphicsLayer {
                this.rotationX = rotationX
                this.transformOrigin = TransformOrigin(0.5f, 0f) // Bisagra superior
                cameraDistance = 12f * density
            }
            .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
            .background(Color.Transparent)
            .drawWithContent {
                drawContent()
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.3f),
                            Color.Transparent,
                            Color.White.copy(alpha = 0.1f)
                        )
                    )
                )
                drawRect(color = Color.White.copy(alpha = 0.5f), size = size, style = Stroke(width = 4f))
            }
    )
}
