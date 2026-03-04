package com.deivid.telegramvideo.ui.player

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.deivid.telegramvideo.R

/**
 * Serviço em foreground para manter a reprodução de vídeo ativa em background.
 */
class VideoPlayerService : Service() {

    companion object {
        const val CHANNEL_ID = "video_player_channel"
        const val NOTIFICATION_ID = 1001
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startForeground(NOTIFICATION_ID, createNotification())
        return START_NOT_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "Reprodução de Vídeo",
            NotificationManager.IMPORTANCE_LOW
        ).apply {
            description = "Notificação de reprodução de vídeo do Telegram"
            setShowBadge(false)
        }

        val notificationManager = getSystemService(NotificationManager::class.java)
        notificationManager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.app_name))
            .setContentText("Reproduzindo vídeo…")
            .setSmallIcon(R.drawable.ic_play_notification)
            .setOngoing(true)
            .setSilent(true)
            .build()
    }
}
