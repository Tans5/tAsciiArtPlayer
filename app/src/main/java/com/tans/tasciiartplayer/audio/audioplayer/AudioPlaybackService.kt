package com.tans.tasciiartplayer.audio.audioplayer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.widget.RemoteViews
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.tans.tasciiartplayer.R
import com.tans.tasciiartplayer.ui.main.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

class AudioPlaybackService : Service(), CoroutineScope by CoroutineScope(Dispatchers.Main) {

    private val remoteViews: RemoteViews by lazy {
        RemoteViews(this.packageName, R.layout.audio_play_notification_layout)
    }

    override fun onCreate() {
        super.onCreate()
        val serviceHashCode = this.hashCode()
        val notificationManager = getSystemService<NotificationManager>()!!
        val isContainChannel = notificationManager.notificationChannels.any { it.id == NOTIFICATION_CHANNEL_ID }
        if (!isContainChannel) {
            notificationManager.createNotificationChannel(NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH))
        }
        val notificationIntent = PendingIntent.getActivity(this, serviceHashCode, Intent(this, MainActivity::class.java), PendingIntent.FLAG_UPDATE_CURRENT)
        val notification = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            // TODO: Replace it
            .setSmallIcon(R.mipmap.ic_launcher)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(notificationIntent)
            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
            .setCustomContentView(remoteViews)
            .setSilent(true)
            .build()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(serviceHashCode, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        } else {
            startForeground(serviceHashCode, notification)
        }

        launch {
            // TODO: update remotes ui
        }

    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        cancel()
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "tAsciiArtPlayer"
        private const val NOTIFICATION_CHANNEL_NAME = "tAsciiArtPlayer"
    }
}