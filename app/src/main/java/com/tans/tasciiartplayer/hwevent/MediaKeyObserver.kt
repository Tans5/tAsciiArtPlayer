package com.tans.tasciiartplayer.hwevent

import android.annotation.SuppressLint
import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.view.KeyEvent
import com.tans.tasciiartplayer.AppLog
import com.tans.tasciiartplayer.appGlobalCoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

object MediaKeyObserver {

    private val eventSubject: MutableSharedFlow<MediaKeyEvent> by lazy {
        MutableSharedFlow()
    }

    private val receiver: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {

            @Suppress("DEPRECATION")
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == Intent.ACTION_MEDIA_BUTTON) {
                    val keyEvent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT, KeyEvent::class.java)
                    } else {
                        intent.getParcelableExtra<KeyEvent>(Intent.EXTRA_KEY_EVENT)
                    }
                    if (keyEvent != null) {
                        if (keyEvent.action == KeyEvent.ACTION_DOWN) {
                            val mediaKeyEvent = when (keyEvent.keyCode) {
                                KeyEvent.KEYCODE_MEDIA_PLAY -> MediaKeyEvent.Play
                                KeyEvent.KEYCODE_MEDIA_PAUSE -> MediaKeyEvent.Pause
                                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> MediaKeyEvent.PlayOrPause
                                KeyEvent.KEYCODE_MEDIA_STOP -> MediaKeyEvent.Stop
                                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> MediaKeyEvent.PlayPrevious
                                KeyEvent.KEYCODE_MEDIA_NEXT -> MediaKeyEvent.PlayNext
                                else -> null
                            }
                            AppLog.d(TAG, "Receive media key event: $mediaKeyEvent")
                            if (mediaKeyEvent != null) {
                                appGlobalCoroutineScope.launch {
                                    eventSubject.emit(mediaKeyEvent)
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    @SuppressLint("UnspecifiedRegisterReceiverFlag")
    fun init(application: Application) {
        val filter = IntentFilter()
        filter.addAction(Intent.ACTION_MEDIA_BUTTON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            application.registerReceiver(receiver, filter, Context.RECEIVER_EXPORTED)
        } else {
            application.registerReceiver(receiver, filter)
        }
    }

    fun observeEvent(): Flow<MediaKeyEvent> = eventSubject

    enum class MediaKeyEvent {
        Play,
        Pause,
        PlayOrPause,
        Stop,
        PlayPrevious,
        PlayNext
    }

    private const val TAG = "MediaKeyObserver"
}