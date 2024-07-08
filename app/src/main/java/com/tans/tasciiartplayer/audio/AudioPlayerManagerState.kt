package com.tans.tasciiartplayer.audio

import com.tans.tmediaplayer.player.model.MediaInfo
import com.tans.tmediaplayer.player.tMediaPlayerState

data class AudioPlayerManagerState(
    val playListState: PlayListState = PlayListState.NoSelectedList,
    val playType: PlayType = PlayType.ListSequentialPlay
)

enum class PlayType {
    ListSequentialPlay,
    ListLoopPlay,
    ListRandomPlay,
    ListRandomLoopPlay,
    SingleLoopPlay
}

sealed class PlayListState {

    data object NoSelectedList : PlayListState()

    data class SelectedPlayList(
        val audioList: AudioList,
        val currentPlayIndex: Int,
        val playerProgress: Long,
        val playerDuration: Long,
        val playerState: tMediaPlayerState,
        val playerMediaInfo: MediaInfo?,
        val playedIndexes: Set<Int>
    ) : PlayListState()
}

fun PlayListState.SelectedPlayList.getCurrentPlayAudio(): AudioModel = this.audioList.audios[this.currentPlayIndex]