package com.mixcasete.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mixcasete.app.player.CassetteState
import com.mixcasete.app.player.PlayState
import com.mixcasete.app.ui.PlayerZones
import com.mixcasete.app.ui.fillZone
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

@Composable
fun CassettePlayer(
    zones: PlayerZones,
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
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawBody(zones)
        }

        LcdScreen(
            modifier = Modifier.fillZone(zones.screen),
            playState = playState,
            progress = cassette.progress
        )

        KnobsStrip(modifier = Modifier.fillZone(zones.knobs))

        CassetteWindow(
            modifier = Modifier.fillZone(zones.window),
            isLidOpen = isLidOpen,
            cassette = cassette,
            playState = playState
        )

        Keyboard(
            modifier = Modifier.fillZone(zones.keyboard),
            playState = playState,
            onPlayPause = onPlayPause,
            onStop = onStop,
            onEject = onEject,
            onRewind = onRewind,
            onFastForward = onFastForward
        )

        Text(
            text = "CALIB",
            color = Color.Red,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(2.dp)
                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(3.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
                .clickable { onToggleCalibration() }
        )

        if (calibrationMode) {
            for ((name, zone) in zones.namedZones()) {
                Box(Modifier.fillZone(zone).border(1.dp, Color.Red)) {
                    Text(name, color = Color.Red, fontSize = 8.sp)
                }
            }
        }
    }
}

private fun DrawScope.drawBody(zones: PlayerZones) {
    val w = size.width
    val h = size.height
    val line = Color(0xFF111111)
    val shell = Color(0xFFE9E4D8)

    for (leg in listOf(zones.legL, zones.legR)) {
        drawRect(color = line, topLeft = Offset(leg.x0 * w, leg.y0 * h), size = Size(leg.w * w, leg.h * h))
    }

    drawRoundRect(color = shell, size = Size(w, h * 0.965f), cornerRadius = CornerRadius(w * 0.06f))
    drawRoundRect(color = line, size = Size(w, h * 0.965f), cornerRadius = CornerRadius(w * 0.06f), style = Stroke(width = w * 0.022f))

    zones.speaker?.let { sp ->
        val sx = sp.x0 * w
        val sy = sp.y0 * h
        val sw = sp.w * w
        val sh = sp.h * h
        drawRoundRect(color = Color.White, topLeft = Offset(sx, sy), size = Size(sw, sh), cornerRadius = CornerRadius(sw * 0.04f))
        drawRoundRect(color = line, topLeft = Offset(sx, sy), size = Size(sw, sh), cornerRadius = CornerRadius(sw * 0.04f), style = Stroke(width = w * 0.012f))
        val step = sw / 24f
        val cx = sx + sw * 0.36f
        val cy = sy + sh * 0.52f
        val clusterR = sw * 0.20f
        var yy = sy + step
        while (yy < sy + sh - step * 0.5f) {
            var xx = sx + step
            while (xx < sx + sw - step * 0.5f) {
                val dx = xx - cx
                val dy = yy - cy
                if (dx * dx + dy * dy < clusterR * clusterR) {
                    drawCircle(color = line, radius = step * 0.32f, center = Offset(xx, yy))
                } else {
                    drawCircle(color = line, radius = step * 0.30f, center = Offset(xx, yy), style = Stroke(width = step * 0.14f))
                }
                xx += step
            }
            yy += step
        }
    }

    run {
        val sg = zones.segments
        val sx = sg.x0 * w
        val sy = sg.y0 * h
        val sw = sg.w * w
        val sh = sg.h * h
        val n = 5
        val gap = sw * 0.012f
        val segW = (sw - gap * (n - 1)) / n
        for (i in 0 until n) {
            drawRect(color = line, topLeft = Offset(sx + i * (segW + gap), sy), size = Size(segW, sh))
        }
    }

    run {
        val g = zones.sideGrille
        val gx = g.x0 * w
        val gy = g.y0 * h
        val gw = g.w * w
        val gh = g.h * h
        for (i in 0 until 4) {
            drawRect(color = line, topLeft = Offset(gx, gy + gh * i / 4f + gh * 0.1f), size = Size(gw, gh * 0.12f))
        }
    }

    run {
        val wz = zones.window
        val wx = wz.x0 * w
        val wy = wz.y0 * h
        val ww = wz.w * w
        val wh = wz.h * h
        val bev = w * 0.018f
        drawRoundRect(color = Color(0xFFC9C4B8), topLeft = Offset(wx - bev, wy - bev), size = Size(ww + bev * 2, wh + bev * 2), cornerRadius = CornerRadius(bev * 2))
        drawRoundRect(color = line, topLeft = Offset(wx - bev, wy - bev), size = Size(ww + bev * 2, wh + bev * 2), cornerRadius = CornerRadius(bev * 2), style = Stroke(width = w * 0.010f))
        drawRoundRect(color = line, topLeft = Offset(wx, wy), size = Size(ww, wh), cornerRadius = CornerRadius(bev), style = Stroke(width = w * 0.008f))
        drawLine(line, Offset(wx - bev, wy - bev), Offset(wx + bev * 0.6f, wy + bev * 0.6f), strokeWidth = w * 0.006f)
        drawLine(line, Offset(wx + ww + bev, wy - bev), Offset(wx + ww - bev * 0.6f, wy + bev * 0.6f), strokeWidth = w * 0.006f)
        drawLine(line, Offset(wx - bev, wy + wh + bev), Offset(wx + bev * 0.6f, wy + wh - bev * 0.6f), strokeWidth = w * 0.006f)
        drawLine(line, Offset(wx + ww + bev, wy + wh + bev), Offset(wx + ww - bev * 0.6f, wy + wh - bev * 0.6f), strokeWidth = w * 0.006f)
    }
}

@Composable
fun LcdScreen(modifier: Modifier, playState: PlayState, progress: Float) {
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(color = Color(0xFF111111))
            drawRect(color = Color(0xFFA8C0A0), topLeft = Offset(w * 0.03f, h * 0.12f), size = Size(w * 0.94f, h * 0.76f))
            drawRect(color = Color(0xFF223322), topLeft = Offset(w * 0.06f, h * 0.55f), size = Size(w * 0.88f, h * 0.22f), style = Stroke(width = h * 0.04f))
            drawRect(color = Color(0xFF223322), topLeft = Offset(w * 0.06f, h * 0.55f), size = Size(w * 0.88f * progress, h * 0.22f))
        }
        Text(
            text = when (playState) {
                PlayState.PLAYING -> "PLAY ${(progress * 100).toInt()}%"
                PlayState.PAUSED -> "PAUSE ${(progress * 100).toInt()}%"
                PlayState.STOPPED -> "STOP"
                PlayState.EJECTED -> "EJECT"
            },
            color = Color(0xFF1C2B1C),
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 8.dp, top = 2.dp)
        )
    }
}

@Composable
fun KnobsStrip(modifier: Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        val r = min(w / 9f, h * 0.42f)
        for (i in 0 until 3) {
            val cx = w * (0.2f + i * 0.3f)
            val cy = h * 0.5f
            drawCircle(color = Color(0xFF111111), radius = r, center = Offset(cx, cy))
            drawCircle(color = Color.White, radius = r * 0.82f, center = Offset(cx, cy))
            val ang = (-60 + i * 45).toDouble() * PI / 180.0
            drawLine(
                color = Color(0xFF111111),
                start = Offset(cx, cy),
                end = Offset(cx + (cos(ang) * r * 0.7).toFloat(), cy + (sin(ang) * r * 0.7).toFloat()),
                strokeWidth = r * 0.22f
            )
        }
    }
}
