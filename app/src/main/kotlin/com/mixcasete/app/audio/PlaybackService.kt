package com.mixcasete.app.audio

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PlaybackService : MediaSessionService() {

    companion object {
        const val CHANNEL_ID = "mixcasete_playback"
        const val NOTIF_ID = 101
        const val ACT_TOGGLE = "toggle"
        const val ACT_NEXT = "next"
        const val ACT_PREV = "prev"
        const val ACT_STOP = "stop"
    }

    private var wakeLock: PowerManager.WakeLock? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var observeJob: Job? = null

    override fun onCreate() {
        super.onCreate()
        createChannel()
        val pm = getSystemService(POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MixCasete::YtAudio").apply {
            setReferenceCounted(false)
        }
        observeJob = scope.launch {
            YtBridge.state.collectLatest { st ->
                if (st.videoId != null) updateNotification(st)
            }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.getStringExtra("yt_action")) {
            ACT_TOGGLE -> YtBridge.emit(YtCmd.Toggle)
            ACT_NEXT -> YtBridge.emit(YtCmd.Next)
            ACT_PREV -> YtBridge.emit(YtCmd.Prev)
            ACT_STOP -> YtBridge.emit(YtCmd.Stop)
        }
        if (wakeLock?.isHeld != true) wakeLock?.acquire(60 * 60 * 60 * 1000L)
        startForeground(NOTIF_ID, buildNotification(YtBridge.state.value))
        return START_STICKY
    }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val ch = NotificationChannel(CHANNEL_ID, "Reproduccion", NotificationManager.IMPORTANCE_LOW)
            ch.setShowBadge(false)
            val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
            nm.createNotificationChannel(ch)
        }
    }

    private fun pi(action: String, req: Int): PendingIntent {
        val i = Intent(this, PlaybackService::class.java).setAction(action).putExtra("yt_action", action)
        val fl = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M)
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        else PendingIntent.FLAG_UPDATE_CURRENT
        return PendingIntent.getService(this, req, i, fl)
    }

    private fun buildNotification(st: YtState): Notification {
        val playPauseIcon = if (st.isPlaying) android.R.drawable.ic_media_pause else android.R.drawable.ic_media_play
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentTitle(st.title.ifBlank { "Mix.Casete" })
            .setContentText(st.artist)
            .setOnlyAlertOnce(true)
            .setOngoing(true)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .addAction(android.R.drawable.ic_media_previous, "Anterior", pi(ACT_PREV, 1))
            .addAction(playPauseIcon, if (st.isPlaying) "Pausa" else "Play", pi(ACT_TOGGLE, 2))
            .addAction(android.R.drawable.ic_media_next, "Siguiente", pi(ACT_NEXT, 3))
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Detener", pi(ACT_STOP, 4))
            .build()
    }

    private fun updateNotification(st: YtState) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !NotificationManagerCompat.from(this).areNotificationsEnabled()
        ) return
        NotificationManagerCompat.from(this).notify(NOTIF_ID, buildNotification(st))
    }

    override fun onTaskRemoved(rootIntent: Intent?) {
        // Mantener el servicio/notificacion
    }

    override fun onDestroy() {
        observeJob?.cancel()
        scope.cancel()
        if (wakeLock?.isHeld == true) wakeLock?.release()
        wakeLock = null
        super.onDestroy()
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = null
}
