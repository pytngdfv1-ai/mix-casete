package com.mixcasete.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.mixcasete.app.audio.CassetteState
import com.mixcasete.app.audio.ErrorInfo
import com.mixcasete.app.audio.PlayState
import com.mixcasete.app.audio.RepeatMode
import com.mixcasete.app.ui.PlayerZones
import com.mixcasete.app.ui.fillZone
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val ShellDark = Color(0xFF262626)
private val TrimSilver = Color(0xFFC9C9C9)
private val PanelDark = Color(0xFF1B1B1B)
private val DetailDark = Color(0xFF444444)
private val ActiveGreen = Color(0xFF81C784)
// Paleta del casete real (foto): cinta cafe-negruzca + corona blanco-hueso
private val TapeDark = Color(0xFF140D08)
private val TapeMid = Color(0xFF2A1B10)
private val CrownWhite = Color(0xFFECECEC)
private val HubHole = Color(0xFF0C0C0C)

@Composable
fun CassettePlayer(
    zones: PlayerZones,
    playState: PlayState,
    isLidOpen: Boolean,
    cassette: CassetteState,
    calibrationMode: Boolean,
    errorInfo: ErrorInfo?,
    recMessage: String?,
    isShuffle: Boolean,
    repeatMode: RepeatMode,
    isFavorite: Boolean,
    onPlayPause: () -> Unit,
    onStop: () -> Unit,
    onRecord: () -> Unit,
    onRewind: () -> Unit,
    onFastForward: () -> Unit,
    onToggleCalibration: () -> Unit,
    onSearch: () -> Unit,
    onLists: () -> Unit,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onShuffle: () -> Unit,
    onRepeat: () -> Unit,
    onFavorite: () -> Unit,
    onShowVideo: () -> Unit
) {
    // Angulo de giro: avanza solo mientras suena (carretes reales girando).
    var angle by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(playState) {
        if (playState == PlayState.PLAYING) {
            var a = angle
            while (true) {
                a = (a + 5f) % 360f
                angle = a
                delay(33)
            }
        }
    }

    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) { drawBody(zones) }

        zones.screen?.let { LcdScreen(modifier = Modifier.fillZone(it), playState = playState, progress = cassette.progress) }
        zones.knobs?.let { KnobsStrip(modifier = Modifier.fillZone(it)) }

        CassetteWindow(
            modifier = Modifier.fillZone(zones.window),
            cassette = cassette,
            playState = playState,
            angle = angle
        )

        FunctionBar(
            modifier = Modifier.fillZone(zones.functionBar),
            isShuffle = isShuffle, repeatMode = repeatMode, isFavorite = isFavorite,
            onSearch = onSearch, onLists = onLists, onPrev = onPrev, onNext = onNext,
            onShuffle = onShuffle, onRepeat = onRepeat, onFavorite = onFavorite
        )

        // Keyboard() vive en PlayerControls.kt -> NO se duplica aca.
        Keyboard(
            modifier = Modifier.fillZone(zones.keyboard),
            playState = playState,
            onPlayPause = onPlayPause, onStop = onStop, onRecord = onRecord,
            onRewind = onRewind, onFastForward = onFastForward
        )

        IconButton(
            onClick = onShowVideo,
            modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp).size(36.dp)
        ) {
            Icon(Icons.Filled.Videocam, contentDescription = "Ver video", tint = ActiveGreen)
        }

        recMessage?.let { msg ->
            Text(msg, color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopCenter).padding(8.dp)
                    .background(Color(0xFFB71C1C).copy(alpha = 0.85f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp))
        }

        errorInfo?.let { info ->
            Text("ERROR: ${info.message.take(60)}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp)
                    .background(Color.Red.copy(alpha = 0.8f), RoundedCornerShape(4.dp))
                    .padding(horizontal = 8.dp, vertical = 4.dp))
        }

        Text("CALIB", color = Color.Red, fontSize = 9.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopEnd).padding(2.dp)
                .background(Color.Black.copy(alpha = 0.55f), RoundedCornerShape(3.dp))
                .padding(horizontal = 6.dp, vertical = 2.dp)
                .clickable { onToggleCalibration() })

        if (calibrationMode) {
            for ((name, zone) in zones.namedZones()) {
                Box(Modifier.fillZone(zone).border(1.dp, Color.Red)) { Text(name, color = Color.Red, fontSize = 8.sp) }
            }
        }
    }
}

@Composable
fun CassetteWindow(modifier: Modifier, cassette: CassetteState, playState: PlayState, angle: Float) {
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // fondo de la ventana (plastico transparente -> deja ver el video detras)
            drawRoundRect(color = Color(0xFF050505).copy(alpha = 0.35f), size = Size(w, h), cornerRadius = CornerRadius(w * 0.03f))
            drawRoundRect(color = TrimSilver.copy(alpha = 0.45f), size = Size(w, h), cornerRadius = CornerRadius(w * 0.03f), style = Stroke(width = w * 0.006f))

            // Geometria tipo foto: carrete IZQUIERDO grande, DERECHO chico, mismos ejes Y
            val cy = h * 0.46f
            val rL = min(w * 0.21f, h * 0.30f)
            val rR = min(w * 0.155f, h * 0.225f)
            val xL = w * 0.33f
            val xR = w * 0.67f

            // cinta tensa entre carretes (parte baja) + rueditas de guia
            val tapeY = cy + rL * 0.92f
            drawRect(color = TapeDark, topLeft = Offset(xL, tapeY - rL * 0.05f), size = Size(xR - xL, rL * 0.10f))
            drawGuideWheel(xL - rL * 0.55f, tapeY + rL * 0.18f, rL * 0.13f, angle)
            drawGuideWheel(xR + rR * 0.55f, tapeY + rR * 0.18f, rR * 0.15f, -angle)

            drawReel(xL, cy, rL, angle)
            drawReel(xR, cy, rR, -angle * (rL / rR)) // el chico gira mas rapido (conserva cinta)

            // simbolo play/pausa entre carretes
            val midX = w * 0.5f
            if (playState == PlayState.PLAYING) {
                drawRect(color = TrimSilver, topLeft = Offset(midX - rL * 0.07f, cy - rL * 0.16f), size = Size(rL * 0.05f, rL * 0.32f))
                drawRect(color = TrimSilver, topLeft = Offset(midX + rL * 0.02f, cy - rL * 0.16f), size = Size(rL * 0.05f, rL * 0.32f))
            } else {
                val p = Path().apply {
                    moveTo(midX - rL * 0.09f, cy - rL * 0.15f)
                    lineTo(midX + rL * 0.12f, cy)
                    lineTo(midX - rL * 0.09f, cy + rL * 0.15f)
                    close()
                }
                drawPath(p, TrimSilver)
            }

            // reflejos del acrilico
            drawLine(Color.White.copy(alpha = 0.08f), Offset(w * 0.16f, h * 0.08f), Offset(w * 0.40f, h * 0.92f), strokeWidth = w * 0.04f)
            drawLine(Color.White.copy(alpha = 0.05f), Offset(w * 0.28f, h * 0.08f), Offset(w * 0.52f, h * 0.92f), strokeWidth = w * 0.025f)
        }

        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 6.dp, start = 8.dp, end = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(cassette.title, color = Color(0xFFF2F2F2), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 2, textAlign = TextAlign.Center)
            Text(cassette.artist, color = Color(0xFFDDDDDD), fontSize = 10.sp, maxLines = 1)
            if (cassette.sourceLabel.isNotBlank()) {
                Text("● ${cassette.sourceLabel}", color = ActiveGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

// Carrete real: cinta oscura + corona blanca dentada (aro + dientes radiales + buje con pestañas)
private fun DrawScope.drawReel(cx: Float, cy: Float, r: Float, angle: Float) {
    // disco de cinta
    drawCircle(color = TapeDark, radius = r, center = Offset(cx, cy))
    drawCircle(color = TapeMid, radius = r * 0.92f, center = Offset(cx, cy), style = Stroke(width = r * 0.05f))

    val cr = r * 0.46f          // radio exterior de la corona
    val dw = r * 0.085f         // ancho de diente
    val slots = 18

    rotate(degrees = angle, pivot = Offset(cx, cy)) {
        // aro exterior blanco
        drawCircle(color = CrownWhite, radius = cr, center = Offset(cx, cy), style = Stroke(width = r * 0.085f))
        // dientes radiales blancos (del aro hacia el buje)
        for (i in 0 until slots) {
            rotate(degrees = i * 360f / slots, pivot = Offset(cx, cy)) {
                drawRect(
                    color = CrownWhite,
                    topLeft = Offset(cx - dw / 2f, cy - cr + r * 0.02f),
                    size = Size(dw, cr * 0.42f)
                )
            }
        }
        // buje blanco
        drawCircle(color = CrownWhite, radius = cr * 0.55f, center = Offset(cx, cy))
        // hueco central
        drawCircle(color = HubHole, radius = cr * 0.30f, center = Offset(cx, cy))
        // 3 pestanas blancas dentro del hueco (como la foto)
        for (i in 0 until 3) {
            rotate(degrees = i * 120f, pivot = Offset(cx, cy)) {
                drawRect(
                    color = CrownWhite,
                    topLeft = Offset(cx - cr * 0.07f, cy - cr * 0.30f),
                    size = Size(cr * 0.14f, cr * 0.16f)
                )
            }
        }
    }
}

// Ruedita de guia de la cinta (abajo, a los lados)
private fun DrawScope.drawGuideWheel(cx: Float, cy: Float, r: Float, angle: Float) {
    drawCircle(color = Color(0xFF3A3A3A), radius = r, center = Offset(cx, cy))
    rotate(degrees = angle, pivot = Offset(cx, cy)) {
        drawCircle(color = CrownWhite.copy(alpha = 0.8f), radius = r * 0.55f, center = Offset(cx, cy), style = Stroke(width = r * 0.18f))
        for (i in 0 until 6) {
            rotate(degrees = i * 60f, pivot = Offset(cx, cy)) {
                drawRect(color = CrownWhite.copy(alpha = 0.8f), topLeft = Offset(cx - r * 0.06f, cy - r * 0.55f), size = Size(r * 0.12f, r * 0.22f))
            }
        }
        drawCircle(color = HubHole, radius = r * 0.22f, center = Offset(cx, cy))
    }
}

@Composable
fun FunctionBar(
    modifier: Modifier, isShuffle: Boolean, repeatMode: RepeatMode, isFavorite: Boolean,
    onSearch: () -> Unit, onLists: () -> Unit, onPrev: () -> Unit, onNext: () -> Unit,
    onShuffle: () -> Unit, onRepeat: () -> Unit, onFavorite: () -> Unit
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.SpaceEvenly, verticalAlignment = Alignment.CenterVertically) {
        IconButton(onClick = onSearch, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.Search, "Buscar", tint = TrimSilver) }
        IconButton(onClick = onLists, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.QueueMusic, "Listas", tint = TrimSilver) }
        IconButton(onClick = onPrev, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.SkipPrevious, "Anterior", tint = TrimSilver) }
        IconButton(onClick = onNext, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.SkipNext, "Siguiente", tint = TrimSilver) }
        IconButton(onClick = onShuffle, modifier = Modifier.weight(1f)) { Icon(Icons.Filled.Shuffle, "Aleatorio", tint = if (isShuffle) ActiveGreen else Color(0xFF777777)) }
        IconButton(onClick = onRepeat, modifier = Modifier.weight(1f)) {
            Icon(if (repeatMode == RepeatMode.ONE) Icons.Filled.RepeatOne else Icons.Filled.Repeat, "Repetir", tint = if (repeatMode == RepeatMode.OFF) Color(0xFF777777) else ActiveGreen)
        }
        IconButton(onClick = onFavorite, modifier = Modifier.weight(1f)) {
            Icon(if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder, "Favorito", tint = if (isFavorite) Color(0xFFE57373) else TrimSilver)
        }
    }
}

@Composable
fun LcdScreen(modifier: Modifier, playState: PlayState, progress: Float) {
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height
            drawRect(color = TrimSilver)
            drawRect(color = Color(0xFFA8C0A0), topLeft = Offset(w * 0.03f, h * 0.12f), size = Size(w * 0.94f, h * 0.76f))
            drawRect(color = Color(0xFF223322), topLeft = Offset(w * 0.06f, h * 0.55f), size = Size(w * 0.88f, h * 0.22f), style = Stroke(width = h * 0.04f))
            drawRect(color = Color(0xFF223322), topLeft = Offset(w * 0.06f, h * 0.55f), size = Size(w * 0.88f * progress, h * 0.22f))
        }
        Text(
            when (playState) {
                PlayState.PLAYING -> "PLAY ${(progress * 100).toInt()}%"
                PlayState.PAUSED -> "PAUSE ${(progress * 100).toInt()}%"
                PlayState.STOPPED -> "STOP"
                PlayState.EJECTED -> "EJECT"
                PlayState.ERROR -> "ERROR"
            },
            color = Color(0xFF1C2B1C), fontSize = 10.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.align(Alignment.TopStart).padding(start = 8.dp, top = 2.dp)
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
            drawCircle(color = TrimSilver, radius = r, center = Offset(cx, cy))
            drawCircle(color = Color(0xFF1A1A1A), radius = r * 0.82f, center = Offset(cx, cy))
            val ang = (-60 + i * 45).toDouble() * PI / 180.0
            drawLine(color = TrimSilver, start = Offset(cx, cy), end = Offset(cx + (cos(ang) * r * 0.7).toFloat(), cy + (sin(ang) * r * 0.7).toFloat()), strokeWidth = r * 0.22f)
        }
    }
}

private fun DrawScope.drawBody(zones: PlayerZones) {
    val w = size.width
    val h = size.height
    val line = TrimSilver
    val shell = ShellDark
    for (leg in listOf(zones.legL, zones.legR)) {
        drawRect(color = Color(0xFF0A0A0A), topLeft = Offset(leg.x0 * w, leg.y0 * h), size = Size(leg.w * w, leg.h * h))
    }
    drawRoundRect(color = shell, size = Size(w, h * 0.965f), cornerRadius = CornerRadius(w * 0.06f))
    drawRoundRect(color = line, size = Size(w, h * 0.965f), cornerRadius = CornerRadius(w * 0.06f), style = Stroke(width = w * 0.012f))
    zones.speaker?.let { sp ->
        val sx = sp.x0 * w; val sy = sp.y0 * h; val sw = sp.w * w; val sh = sp.h * h
        drawRoundRect(color = PanelDark, topLeft = Offset(sx, sy), size = Size(sw, sh), cornerRadius = CornerRadius(sw * 0.04f))
        drawRoundRect(color = line, topLeft = Offset(sx, sy), size = Size(sw, sh), cornerRadius = CornerRadius(sw * 0.04f), style = Stroke(width = w * 0.008f))
        val step = sw / 24f; val cx = sx + sw * 0.36f; val cy = sy + sh * 0.52f; val clusterR = sw * 0.20f
        var yy = sy + step
        while (yy < sy + sh - step * 0.5f) {
            var xx = sx + step
            while (xx < sx + sw - step * 0.5f) {
                val dx = xx - cx; val dy = yy - cy
                if (dx * dx + dy * dy < clusterR * clusterR) drawCircle(color = line, radius = step * 0.32f, center = Offset(xx, yy))
                else drawCircle(color = line.copy(alpha = 0.55f), radius = step * 0.30f, center = Offset(xx, yy), style = Stroke(width = step * 0.12f))
                xx += step
            }
            yy += step
        }
    }
    zones.sideGrille?.let { g ->
        val gx = g.x0 * w; val gy = g.y0 * h; val gw = g.w * w; val gh = g.h * h
        for (i in 0 until 4) drawRect(color = DetailDark, topLeft = Offset(gx, gy + gh * i / 4f + gh * 0.1f), size = Size(gw, gh * 0.12f))
    }
    run {
        val wz = zones.window
        val wx = wz.x0 * w; val wy = wz.y0 * h; val ww = wz.w * w; val wh = wz.h * h
        val bev = w * 0.018f
        drawRoundRect(color = Color(0xFF3A3A3A), topLeft = Offset(wx - bev, wy - bev), size = Size(ww + bev * 2, wh + bev * 2), cornerRadius = CornerRadius(bev * 2))
        drawRoundRect(color = line, topLeft = Offset(wx - bev, wy - bev), size = Size(ww + bev * 2, wh + bev * 2), cornerRadius = CornerRadius(bev * 2), style = Stroke(width = w * 0.008f))
        drawRoundRect(color = line, topLeft = Offset(wx, wy), size = Size(ww, wh), cornerRadius = CornerRadius(bev), style = Stroke(width = w * 0.006f))
        drawLine(line, Offset(wx - bev, wy - bev), Offset(wx + bev * 0.6f, wy + bev * 0.6f), strokeWidth = w * 0.005f)
        drawLine(line, Offset(wx + ww + bev, wy - bev), Offset(wx + ww - bev * 0.6f, wy + bev * 0.6f), strokeWidth = w * 0.005f)
        drawLine(line, Offset(wx - bev, wy + wh + bev), Offset(wx + bev * 0.6f, wy + wh - bev * 0.6f), strokeWidth = w * 0.005f)
        drawLine(line, Offset(wx + ww + bev, wy + wh + bev), Offset(wx + ww - bev * 0.6f, wy + wh - bev * 0.6f), strokeWidth = w * 0.005f)
    }
}
