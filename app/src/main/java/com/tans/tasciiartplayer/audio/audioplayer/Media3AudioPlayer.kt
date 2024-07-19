package com.tans.tasciiartplayer.audio.audioplayer

import android.os.Looper
import androidx.annotation.OptIn
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.media3.common.util.UnstableApi
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

@OptIn(UnstableApi::class)
object Media3AudioPlayer : SimpleBasePlayer(Looper.getMainLooper()) {

    private var isPlaying = false

    override fun getState(): State {
        // TODO: Test code.
        val metaData = MediaMetadata.Builder()
            .setTitle("Title")
            .setArtist("Artist")
            .build()
        val mediaItem = MediaItemData.Builder(1000)
            .setMediaMetadata(metaData)
            .setDurationUs(1000L * 1000L * 60L)
            .setIsSeekable(true)
            .build()
       return State.Builder()
           .setAvailableCommands(
               Player.Commands.Builder()
                   .add(COMMAND_PLAY_PAUSE)
                   .add(COMMAND_GET_METADATA)
                   .add(COMMAND_GET_AUDIO_ATTRIBUTES)
                   .add(COMMAND_GET_CURRENT_MEDIA_ITEM)
                   .build()
           )
           .setContentPositionMs(1000L * 30L)
           .setPlaylist(listOf(mediaItem))
           .setPlaybackState(STATE_READY)
           .setCurrentMediaItemIndex(0)
           .setPlayWhenReady(isPlaying, PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
           .build()
    }

    override fun handleSetPlayWhenReady(playWhenReady: Boolean): ListenableFuture<*> {
        isPlaying = !isPlaying
        return Futures.immediateVoidFuture()
    }

}