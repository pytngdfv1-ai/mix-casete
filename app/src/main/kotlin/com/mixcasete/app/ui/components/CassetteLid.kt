package com.mixcasete.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp

@Composable
fun CassetteLid(modifier: Modifier, isOpen: Boolean) {
    val rotationX by animateFloatAsState(
        targetValue = if (isOpen) -72f else 0f,
        animationSpec = tween(durationMillis = 650),
        label = "lid"
    )
    Box(
        modifier = modifier
            .graphicsLayer {
                this.rotationX = rotationX
                this.transformOrigin = TransformOrigin(0.5f, 0f)
                cameraDistance = 8f * density
            }
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0x14FFFFFF))
            .border(width = 2.dp, color = Color(0x99FFFFFF), shape = RoundedCornerShape(4.dp))
            .drawWithContent {
                drawContent()
                drawLine(
                    color = Color(0x55FFFFFF),
                    start = Offset(size.width * 0.10f, size.height * 0.85f),
                    end = Offset(size.width * 0.45f, size.height * 0.10f),
                    strokeWidth = size.width * 0.05f
                )
                drawLine(
                    color = Color(0x33FFFFFF),
                    start = Offset(size.width * 0.30f, size.height * 0.90f),
                    end = Offset(size.width * 0.65f, size.height * 0.12f),
                    strokeWidth = size.width * 0.02f
                )
            }
    )
}
