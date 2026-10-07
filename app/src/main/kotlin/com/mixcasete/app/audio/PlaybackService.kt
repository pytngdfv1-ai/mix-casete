package com.mixcasete.app.audio

import android.content.Intent
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlaybackService : MediaSessionService() {

    companion object {
        private const val CHROME_UA =
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }

    private var player: ExoPlayer? = null
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()

        // HTTP robusto: UA de navegador + redirects + timeouts largos (evita throttling y cortes)
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent(CHROME_UA)
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(20_000)
            .setReadTimeoutMs(20_000)
        val dataSource = DefaultDataSource.Factory(this, http)
        val mediaFactory = DefaultMediaSourceFactory(this).setDataSourceFactory(dataSource)

        // Buffer grande: 50s min / 150s max -> reproduccion continua sin micro-cortes
        val p = ExoPlayer.Builder(this)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(50_000, 150_000, 2_500, 5_000)
                    .build()
            )
            .setMediaSourceFactory(mediaFactory)
            .build()

        player = p
        PlayerHolder.set(p)
        session = MediaSession.Builder(this, p).build()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onTaskRemoved(rootIntent: Intent?) {
        // No detener: la musica sigue en segundo plano
    }

    override fun onDestroy() {
        PlayerHolder.set(null)
        session?.release()
        player?.release()
        session = null
        player = null
        super.onDestroy()
    }
}
