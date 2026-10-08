package com.mixcasete.app.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickablepackage com.mixcasete.app.ui.components

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
import androidx.compose.material.icons.filled.Forward
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Rewind
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Stop
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
private val ReelBrown = Color(0xFF3A2418)
private val ReelRing = Color(0xFF5A3A28)
private val GearWhite = Color(0xFFE8E8E8)

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
    // Angulo de giro de los carretes: avanza solo mientras suena.
    var angle by remember { mutableFloatStateOf(0f) }
    LaunchedEffect(playState) {
        if (playState == PlayState.PLAYING) {
            var a = angle
            while (true) {
                a = (a + 6f) % 360f
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
            isLidOpen = isLidOpen,
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
fun CassetteWindow(modifier: Modifier, isLidOpen: Boolean, cassette: CassetteState, playState: PlayState, angle: Float) {
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
            // marco interior de la ventana
            drawRoundRect(color = Color(0xFF0E0E0E), size = Size(w, h), cornerRadius = CornerRadius(w * 0.03f))
            drawRoundRect(color = TrimSilver.copy(alpha = 0.5f), size = Size(w, h), cornerRadius = CornerRadius(w * 0.03f), style = Stroke(width = w * 0.006f))

            val reelY = h * 0.62f
            val r = min(w * 0.16f, h * 0.26f)
            val leftCx = w * 0.34f
            val rightCx = w * 0.66f
            drawReel(leftCx, reelY, r, angle, clockwise = true)
            drawReel(rightCx, reelY, r, angle, clockwise = false)

            // simbolo play/pausa entre carretes
            val midX = w * 0.5f
            if (playState == PlayState.PLAYING) {
                drawRect(color = TrimSilver, topLeft = Offset(midX - r * 0.10f, reelY - r * 0.22f), size = Size(r * 0.07f, r * 0.44f))
                drawRect(color = TrimSilver, topLeft = Offset(midX + r * 0.03f, reelY - r * 0.22f), size = Size(r * 0.07f, r * 0.44f))
            } else {
                val p = Path().apply {
                    moveTo(midX - r * 0.12f, reelY - r * 0.20f)
                    lineTo(midX + r * 0.16f, reelY)
                    lineTo(midX - r * 0.12f, reelY + r * 0.20f)
                    close()
                }
                drawPath(p, TrimSilver)
            }

            // reflejo diagonal del acrilico
            drawLine(Color.White.copy(alpha = 0.10f), Offset(w * 0.18f, h * 0.10f), Offset(w * 0.42f, h * 0.92f), strokeWidth = w * 0.05f)
            drawLine(Color.White.copy(alpha = 0.06f), Offset(w * 0.30f, h * 0.10f), Offset(w * 0.54f, h * 0.92f), strokeWidth = w * 0.03f)
        }

        Column(
            modifier = Modifier.align(Alignment.TopCenter).padding(top = h010()),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(cassette.title, color = Color(0xFFF2F2F2), fontSize = 12.sp, fontWeight = FontWeight.Bold, maxLines = 2, textAlign = TextAlign.Center)
            Text(cassette.artist, color = Color(0xFFBBBBBB), fontSize = 10.sp, maxLines = 1)
            if (cassette.sourceLabel.isNotBlank()) {
                Text("● ${cassette.sourceLabel}", color = ActiveGreen, fontSize = 9.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

private fun h010() = 6.dp // pequeno offset superior para el texto

private fun DrawScope.drawReel(cx: Float, cy: Float, r: Float, angle: Float, clockwise: Boolean) {
    val dir = if (clockwise) angle else -angle
    // cuerpo marron del carrete
    drawCircle(color = ReelBrown, radius = r, center = Offset(cx, cy))
    drawCircle(color = ReelRing, radius = r * 0.78f, center = Offset(cx, cy), style = Stroke(width = r * 0.06f))
    drawCircle(color = ReelBrown, radius = r * 0.62f, center = Offset(cx, cy))
    // marcador radial para que se note el giro del carrete
    rotate(dir, center = Offset(cx, cy)) {
        drawLine(color = ReelRing, start = Offset(cx, cy - r * 0.62f), end = Offset(cx, cy - r * 0.30f), strokeWidth = r * 0.06f)
    }
    // engranaje blanco dentado (rotado)
    rotate(dir, center = Offset(cx, cy)) {
        val gr = r * 0.42f
        val teeth = 8
        val toothLen = gr * 0.34f
        for (i in 0 until teeth) {
            val a = (i * 360f / teeth)
            rotate(a, center = Offset(cx, cy)) {
                drawRoundRect(
                    color = GearWhite,
                    topLeft = Offset(cx - gr * 0.16f, cy - gr - toothLen),
                    size = Size(gr * 0.32f, toothLen + gr * 0.2f),
                    cornerRadius = CornerRadius(gr * 0.08f)
                )
            }
        }
        drawCircle(color = GearWhite, radius = gr, center = Offset(cx, cy))
        drawCircle(color = Color(0xFF222222), radius = gr * 0.30f, center = Offset(cx, cy))
        drawCircle(color = GearWhite, radius = gr * 0.12f, center = Offset(cx, cy))
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
fun Keyboard(
    modifier: Modifier, playState: PlayState,
    onPlayPause: () -> Unit, onStop: () -> Unit, onRecord: () -> Unit,
    onRewind: () -> Unit, onFastForward: () -> Unit
) {
    Row(modifier = modifier, horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        KeyButton(Modifier.weight(1f), Icons.Filled.Rewind, "REW", TrimSilver, onRewind)
        KeyButton(Modifier.weight(1f), if (playState == PlayState.PLAYING) Icons.Filled.PlayArrow else Icons.Filled.PlayArrow, "PLAY", TrimSilver, onPlayPause)
        KeyButton(Modifier.weight(1f), Icons.Filled.Stop, "PAUSE", TrimSilver, onPlayPause) // pausa via toggle
        KeyButton(Modifier.weight(1f), Icons.Filled.Stop, "STOP", TrimSilver, onStop)
        KeyButton(Modifier.weight(1f), Icons.Filled.Stop, "REC", Color(0xFFE53935), onRecord, filledDot = true)
        KeyButton(Modifier.weight(1f), Icons.Filled.Forward, "FF", TrimSilver, onFastForward)
    }
}

@Composable
private fun KeyButton(modifier: Modifier, icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, tint: Color, onClick: () -> Unit, filledDot: Boolean = false) {
    Column(
        modifier = modifier
            .border(1.dp, TrimSilver.copy(alpha = 0.6f), RoundedCornerShape(3.dp))
            .clickable { onClick() }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (filledDot) {
            Box(Modifier.size(14.dp).background(tint, RoundedCornerShape(50)))
        } else {
            Icon(icon, contentDescription = label, tint = tint, modifier = Modifier.size(16.dp))
        }
        Text(label, color = TrimSilver, fontSize = 8.sp, fontWeight = FontWeight.Bold, modifier = Modifier.padding(top = 2.dp))
    }
}

@Composable
fun LcdScreen(modifier: Modifier, playState: PlayState, progress: Float) {
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
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
        val w = size.width; val h = size.height
        val r = min(w / 9f, h * 0.42f)
        for (i in 0 until 3) {
            val cx = w * (0.2f + i * 0.3f); val cy = h * 0.5f
            drawCircle(color = TrimSilver, radius = r, center = Offset(cx, cy))
            drawCircle(color = Color(0xFF1A1A1A), radius = r * 0.82f, center = Offset(cx, cy))
            val ang = (-60 + i * 45).toDouble() * PI / 180.0
            drawLine(color = TrimSilver, start = Offset(cx, cy), end = Offset(cx + (cos(ang) * r * 0.7).toFloat(), cy + (sin(ang) * r * 0.7).toFloat()), strokeWidth = r * 0.22f)
        }
    }
}

private fun DrawScope.drawBody(zones: PlayerZones) {
    val w = size.width; val h = size.height
    val line = TrimSilver; val shell = ShellDark
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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import com.mixcasete.app.audio.CassetteState
import com.mixcasete.app.audio.ErrorInfo
import com.mixcasete.app.audio.PlayState
import com.mixcasete.app.audio.RepeatMode
import com.mixcasete.app.ui.PlayerZones
import com.mixcasete.app.ui.fillZone
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

private val ShellDark = Color(0xFF262626)
private val TrimSilver = Color(0xFFC9C9C9)
private val PanelDark = Color(0xFF1B1B1B)
private val DetailDark = Color(0xFF444444)
private val ActiveGreen = Color(0xFF81C784)

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
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) { drawBody(zones) }

        zones.screen?.let { LcdScreen(modifier = Modifier.fillZone(it), playState = playState, progress = cassette.progress) }
        zones.knobs?.let { KnobsStrip(modifier = Modifier.fillZone(it)) }

        CassetteWindow(
            modifier = Modifier.fillZone(zones.window),
            isLidOpen = isLidOpen,
            cassette = cassette,
            playState = playState
        )

        FunctionBar(
            modifier = Modifier.fillZone(zones.functionBar),
            isShuffle = isShuffle, repeatMode = repeatMode, isFavorite = isFavorite,
            onSearch = onSearch, onLists = onLists, onPrev = onPrev, onNext = onNext,
            onShuffle = onShuffle, onRepeat = onRepeat, onFavorite = onFavorite
        )

        Keyboard(
            modifier = Modifier.fillZone(zones.keyboard),
            playState = playState,
            onPlayPause = onPlayPause, onStop = onStop, onRecord = onRecord,
            onRewind = onRewind, onFastForward = onFastForward
        )

        // Icono VIDEO: da la vuelta al casete y muestra el video pleno
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
            Text("ERROR: ${info.message.take(50)}", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold,
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

private fun DrawScope.drawBody(zones: PlayerZones) {
    val w = size.width; val h = size.height
    val line = TrimSilver; val shell = ShellDark
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

@Composable
fun LcdScreen(modifier: Modifier, playState: PlayState, progress: Float) {
    Box(modifier) {
        Canvas(Modifier.fillMaxSize()) {
            val w = size.width; val h = size.height
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
        val w = size.width; val h = size.height
        val r = min(w / 9f, h * 0.42f)
        for (i in 0 until 3) {
            val cx = w * (0.2f + i * 0.3f); val cy = h * 0.5f
            drawCircle(color = TrimSilver, radius = r, center = Offset(cx, cy))
            drawCircle(color = Color(0xFF1A1A1A), radius = r * 0.82f, center = Offset(cx, cy))
            val ang = (-60 + i * 45).toDouble() * PI / 180.0
            drawLine(color = TrimSilver, start = Offset(cx, cy), end = Offset(cx + (cos(ang) * r * 0.7).toFloat(), cy + (sin(ang) * r * 0.7).toFloat()), strokeWidth = r * 0.22f)
        }
    }
}
