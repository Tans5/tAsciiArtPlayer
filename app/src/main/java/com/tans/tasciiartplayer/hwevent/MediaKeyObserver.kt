package com.tans.tasciiartplayer.hwevent

import android.app.Application
import android.content.Intent
import android.os.Build
import android.support.v4.media.session.MediaSessionCompat
import android.view.KeyEvent
import com.tans.tasciiartplayer.AppLog
import com.tans.tasciiartplayer.appGlobalCoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

object MediaKeyObserver {

    private val eventSubject: MutableSharedFlow<MediaKeyEvent> by lazy {
        MutableSharedFlow()
    }

    private val mediaSession: AtomicReference<MediaSessionCompat?> by lazy {
        AtomicReference()
    }

    private val mediaSessionCallback: MediaSessionCompat.Callback by lazy {
        object : MediaSessionCompat.Callback() {
            @Suppress("DEPRECATION")
            override fun onMediaButtonEvent(intent: Intent?): Boolean {
                return if (intent?.action == Intent.ACTION_MEDIA_BUTTON) {
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
                                true
                            } else {
                                false
                            }
                        } else {
                            false
                        }
                    } else {
                        false
                    }
                } else {
                    false
                }
            }
        }
    }


    fun init(application: Application) {
        val newSession = MediaSessionCompat(application, "tAsciiArtMediaPlayer")
        newSession.isActive = true
        newSession.setCallback(mediaSessionCallback)
        mediaSession.getAndSet(newSession)?.release()
    }

    fun sessionActive(isActive: Boolean) {
        mediaSession.get()?.isActive = isActive
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