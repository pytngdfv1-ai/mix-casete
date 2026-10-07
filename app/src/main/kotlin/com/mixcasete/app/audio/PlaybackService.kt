package com.mixcasete.app.audio

import android.content.Intent
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class PlaybackService : MediaSessionService() {

    private var player: ExoPlayer? = null
    private var session: MediaSession? = null

    override fun onCreate() {
        super.onCreate()
        val p = ExoPlayer.Builder(this)
            .setLoadControl(
                DefaultLoadControl.Builder()
                    .setBufferDurationsMs(30_000, 60_000, 5_000, 8_000)
                    .build()
            )
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
