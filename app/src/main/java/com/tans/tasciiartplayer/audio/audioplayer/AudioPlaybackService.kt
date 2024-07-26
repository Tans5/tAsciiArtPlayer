package com.tans.tasciiartplayer.audio.audioplayer

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.content.getSystemService
import com.tans.tasciiartplayer.R
import com.tans.tasciiartplayer.ui.audioplayer.AudioPlayerActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancel

class AudioPlaybackService : Service(), CoroutineScope by CoroutineScope(Dispatchers.Main) {

//    private val smallNotificationRemoteViews: RemoteViews by lazy {
//        RemoteViews(this.packageName, R.layout.audio_play_small_notification_layout)
//    }
//
//    private val bigNotificationRemoteViews: RemoteViews by lazy {
//        RemoteViews(this.packageName, R.layout.audio_play_big_notification_layout)
//    }
//
//    private val broadcastReceiver: BroadcastReceiver by lazy {
//        object : BroadcastReceiver() {
//            override fun onReceive(context: Context?, intent: Intent?) {
//                when (intent?.action) {
//                    BroadcastAction.PlayPrevious.action -> {
//                        AudioPlayerManager.playPrevious()
//                    }
//                    BroadcastAction.PlayNext.action -> {
//                        AudioPlayerManager.playNext()
//                    }
//                    BroadcastAction.PlayOrPause.action -> {
//                        val playListState = AudioPlayerManager.stateFlow.value.playListState
//                        if (playListState is PlayListState.SelectedPlayList && playListState.playerState is tMediaPlayerState.Playing) {
//                            AudioPlayerManager.pause()
//                        } else {
//                            AudioPlayerManager.play()
//                        }
//                    }
//                    BroadcastAction.LikeOrDislike.action -> {
//                        val audio = (AudioPlayerManager.stateFlow.value.playListState as? PlayListState.SelectedPlayList)?.getCurrentPlayAudio()
//                        if (audio != null) {
//                            launch(Dispatchers.IO) {
//                                try {
//                                    if (audio.isLike) {
//                                        AudioListManager.unlikeAudio(audio.mediaStoreAudio.id)
//                                    } else {
//                                        AudioListManager.likeAudio(audio.mediaStoreAudio.id)
//                                    }
//                                } catch (e: Throwable) {
//                                    e.printStackTrace()
//                                }
//                            }
//                        }
//                    }
//                    BroadcastAction.ChangePlayType.action -> {
//                        val currentPlayType = AudioPlayerManager.stateFlow.value.playType
//                        val newPlayType = when (currentPlayType) {
//                            ListSequentialPlay -> ListLoopPlay
//                            ListLoopPlay -> ListRandomPlay
//                            ListRandomPlay -> SingleLoopPlay
//                            SingleLoopPlay -> ListSequentialPlay
//                            ListRandomLoopPlay -> ListSequentialPlay
//                        }
//                        AudioPlayerManager.changePlayType(newPlayType)
//                    }
//                    else -> {}
//                }
//            }
//        }
//    }

    override fun onCreate() {
        super.onCreate()
        val serviceHashCode = this.hashCode()
        val notificationManager = getSystemService<NotificationManager>()!!
        val isContainChannel = notificationManager.notificationChannels.any { it.id == NOTIFICATION_CHANNEL_ID }
        if (!isContainChannel) {
            notificationManager.createNotificationChannel(NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NotificationManager.IMPORTANCE_HIGH))
        }
//
//        bigNotificationRemoteViews.setOnClickPendingIntent(R.id.audio_previous_layout, buildPendingIntent(BroadcastAction.PlayPrevious))
//        bigNotificationRemoteViews.setOnClickPendingIntent(R.id.audio_next_layout, buildPendingIntent(BroadcastAction.PlayNext))
//        bigNotificationRemoteViews.setOnClickPendingIntent(R.id.audio_play_pause_layout, buildPendingIntent(BroadcastAction.PlayOrPause))
//        bigNotificationRemoteViews.setOnClickPendingIntent(R.id.like_card, buildPendingIntent(BroadcastAction.LikeOrDislike))
//        bigNotificationRemoteViews.setOnClickPendingIntent(R.id.play_type_card, buildPendingIntent(BroadcastAction.ChangePlayType))

        val notificationIntent = PendingIntent.getActivity(this, serviceHashCode, Intent(this, AudioPlayerActivity::class.java), PendingIntent.FLAG_IMMUTABLE)
        val notificationBuilder = NotificationCompat.Builder(this, NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.icon_notification)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(notificationIntent)
//            .setStyle(NotificationCompat.DecoratedCustomViewStyle())
//            .setCustomContentView(smallNotificationRemoteViews)
//            .setCustomBigContentView(bigNotificationRemoteViews)
            .setSilent(true)
            .setAutoCancel(false)
            .setOngoing(true)
        startForeground(serviceHashCode, notificationBuilder.build(), ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK)
        observePlayingMediaStoreAudioChanged { audio ->
            if (audio != null) {
                notificationBuilder.setContentTitle(audio.title)
                notificationBuilder.setContentText("${audio.artist}-${audio.album}")
                notificationManager.notify(serviceHashCode, notificationBuilder.build())
            } else {
                notificationManager.cancel(serviceHashCode)
            }
        }

        observeSelectedAudioListChanged {
            if (it == null) {
                stopSelf()
            }
        }

//        val broadcastFilters = IntentFilter().apply {
//            for (e in BroadcastAction.entries) {
//                addAction(e.action)
//            }
//        }
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
//            registerReceiver(broadcastReceiver, broadcastFilters, RECEIVER_NOT_EXPORTED)
//        } else {
//            registerReceiver(broadcastReceiver, broadcastFilters)
//        }
//
//        observePlayingAudioChanged { audio ->
//            if (audio != null) {
//                smallNotificationRemoteViews.setTextViewText(R.id.audio_title_tv, audio.mediaStoreAudio.title)
//                smallNotificationRemoteViews.setTextViewText(R.id.audio_artist_album_tv, "${audio.mediaStoreAudio.artist}-${audio.mediaStoreAudio.album}")
//
//                bigNotificationRemoteViews.setTextViewText(R.id.audio_title_tv, audio.mediaStoreAudio.title)
//                bigNotificationRemoteViews.setTextViewText(R.id.audio_artist_album_tv, "${audio.mediaStoreAudio.artist}-${audio.mediaStoreAudio.album}")
//                bigNotificationRemoteViews.setImageViewResource(R.id.like_iv, if (audio.isLike) R.drawable.icon_favorite_fill else R.drawable.icon_favorite_unfill)
//            } else {
//                smallNotificationRemoteViews.setTextViewText(R.id.audio_title_tv, "")
//                smallNotificationRemoteViews.setTextViewText(R.id.audio_artist_album_tv, "")
//
//                bigNotificationRemoteViews.setTextViewText(R.id.audio_title_tv, "")
//                bigNotificationRemoteViews.setTextViewText(R.id.audio_artist_album_tv, "")
//            }
//            notificationManager.notify(serviceHashCode, notificationBuilder.build())
//        }
//
//        observePlayTypeChanged {
//            bigNotificationRemoteViews.setImageViewResource(
//                R.id.play_type_iv, when (it) {
//                    ListSequentialPlay -> R.drawable.icon_audio_sequence_play
//                    ListLoopPlay -> R.drawable.icon_audio_list_loop_play
//                    ListRandomPlay -> R.drawable.icon_audio_random_play
//                    ListRandomLoopPlay -> R.drawable.icon_audio_random_play
//                    SingleLoopPlay -> R.drawable.icon_audio_single_loop_play
//                }
//            )
//            notificationManager.notify(serviceHashCode, notificationBuilder.build())
//        }
//
//        observetMediaPlayerStateChanged { state ->
//            if (state is tMediaPlayerState.Playing) {
//                bigNotificationRemoteViews.setViewVisibility(R.id.audio_play_iv, View.GONE)
//                bigNotificationRemoteViews.setViewVisibility(R.id.audio_pause_iv, View.VISIBLE)
//            } else {
//                bigNotificationRemoteViews.setViewVisibility(R.id.audio_play_iv, View.VISIBLE)
//                bigNotificationRemoteViews.setViewVisibility(R.id.audio_pause_iv, View.GONE)
//            }
//            notificationManager.notify(serviceHashCode, notificationBuilder.build())
//        }
//
//        observePreviousAndNextSkipStateChanged { canSkipPrevious, canSkipNext ->
//            bigNotificationRemoteViews.setBoolean(R.id.audio_previous_iv, "setEnabled", canSkipPrevious)
//            bigNotificationRemoteViews.setBoolean(R.id.audio_next_iv, "setEnabled", canSkipNext)
//            bigNotificationRemoteViews.setBoolean(R.id.audio_previous_layout, "setEnabled", canSkipPrevious)
//            bigNotificationRemoteViews.setBoolean(R.id.audio_next_layout, "setEnabled", canSkipNext)
//            notificationManager.notify(serviceHashCode, notificationBuilder.build())
//        }
//
//        observeProgressAndDurationChanged { progress, duration ->
//            bigNotificationRemoteViews.setTextViewText(R.id.audio_playing_progress_tv, progress.formatDuration())
//            bigNotificationRemoteViews.setTextViewText(R.id.audio_duration_tv, duration.formatDuration())
//
//            val progressInPercent = if (progress > 0 && duration > 0) {
//                ((progress.toFloat() / duration.toFloat()) * 100.0f).toInt()
//            } else {
//                0
//            }
//            bigNotificationRemoteViews.setProgressBar(R.id.audio_progress_bar, 100, progressInPercent, false)
//            notificationManager.notify(serviceHashCode, notificationBuilder.build())
//        }
    }

//    private fun buildPendingIntent(action: BroadcastAction): PendingIntent {
//        return PendingIntent.getBroadcast(
//            this,
//            hashCode(),
//            Intent().apply { this.action = action.action },
//            PendingIntent.FLAG_IMMUTABLE
//        )
//    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        cancel()
        // unregisterReceiver(broadcastReceiver)
    }

    companion object {
        private const val NOTIFICATION_CHANNEL_ID = "tAsciiArtPlayer"
        private const val NOTIFICATION_CHANNEL_NAME = "tAsciiArtPlayer"

//        private enum class BroadcastAction(val action: String) {
//            PlayPrevious("play_previous"),
//            PlayNext("play_next"),
//            PlayOrPause("play_or_pause"),
//            LikeOrDislike("like_or_dislike"),
//            ChangePlayType("change_play_type")
//        }
    }
}