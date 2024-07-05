package com.tans.tasciiartplayer.audio

import com.tans.tmediaplayer.player.model.MediaInfo
import com.tans.tmediaplayer.player.tMediaPlayerState

sealed class AudioPlayerManagerState {

    data object NoSelectedList : AudioPlayerManagerState()

    data class SelectedPlayList(
        val audioList: AudioList,
        val currentPlayIndex: Int,
        val playerProgress: Long,
        val playerDuration: Long,
        val playerState: tMediaPlayerState,
        val playerMediaInfo: MediaInfo?
    ) : AudioPlayerManagerState()
}