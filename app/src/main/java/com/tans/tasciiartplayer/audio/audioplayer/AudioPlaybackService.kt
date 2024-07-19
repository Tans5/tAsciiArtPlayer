package com.tans.tasciiartplayer.audio.audioplayer

import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService

class AudioPlaybackService : MediaSessionService() {

    private val session: MediaSession by lazy {
        MediaSession.Builder(
            this,
            Media3AudioPlayer)
            .build()
    }

    override fun onCreate() {
        super.onCreate()
        addSession(session)
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession {
        return session
    }

}