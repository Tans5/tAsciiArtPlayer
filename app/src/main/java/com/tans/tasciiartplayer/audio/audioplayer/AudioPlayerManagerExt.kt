package com.tans.tasciiartplayer.audio.audioplayer

import com.tans.tasciiartplayer.audio.audiolist.AudioList
import com.tans.tasciiartplayer.audio.audiolist.AudioModel
import com.tans.tasciiartplayer.audio.audioplayer.PlayType.SingleLoopPlay
import com.tans.tmediaplayer.player.tMediaPlayerState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

@OptIn(FlowPreview::class)
fun CoroutineScope.observetMediaPlayerStateChanged(handle: suspend (playerState: tMediaPlayerState?) -> Unit) {
    launch {
        AudioPlayerManager.stateFlow()
            .map {
                val playerState = (it.playListState as? PlayListState.SelectedPlayList)?.playerState
                Optional.ofNullable(playerState)
            }
            .distinctUntilChanged()
            .debounce(100L)
            .flowOn(Dispatchers.IO)
            .collect {
                handle(it.getOrNull())
            }
    }
}

fun CoroutineScope.observePreviousAndNextSkipStateChanged(handle: suspend (canSkipPrevious: Boolean, canSkipNext: Boolean) -> Unit) {
    launch {
        AudioPlayerManager.stateFlow()
            .map {
                val playListState = it.playListState as? PlayListState.SelectedPlayList
                if (playListState != null && it.playType != SingleLoopPlay) {
                    (playListState.playedIndexes.size > 1) to (playListState.nextPlayIndex != null)
                } else {
                    false to false
                }
            }
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
            .collect {
                handle(it.first, it. second)
            }
    }
}

fun CoroutineScope.observeProgressAndDurationChanged(handle: suspend (progress: Long, duration: Long) -> Unit) {
    launch {
        AudioPlayerManager.stateFlow()
            .map {
                val playListState = it.playListState as? PlayListState.SelectedPlayList
                if (playListState!= null) {
                    playListState.playerProgress to playListState.playerDuration
                } else {
                    0L to 0L
                }
            }
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
            .collect {
                handle(it.first, it.second)
            }
    }
}

fun CoroutineScope.observeSelectedAudioListChanged(handle: suspend (audioList: AudioList?) -> Unit) {
    launch {
        AudioPlayerManager.stateFlow()
            .map {
                val audioList = (it.playListState as? PlayListState.SelectedPlayList)?.audioList
                Optional.ofNullable(audioList)
            }
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
            .collect {
                handle(it.getOrNull())
            }
    }
}

fun CoroutineScope.observePlayingAudioChanged(handle: suspend (audioModel: AudioModel?) -> Unit) {
    launch {
        AudioPlayerManager.stateFlow()
            .map {
                val playListState = it.playListState as? PlayListState.SelectedPlayList
                val playIndex = playListState?.currentPlayIndex
                val audio = if (playIndex != null) {
                    playListState.audioList.audios.getOrNull(playIndex)
                } else {
                    null
                }
                Optional.ofNullable(audio)
            }
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
            .collect {
                handle(it.getOrNull())
            }
    }
}

fun CoroutineScope.observePlayTypeChanged(handle: suspend (playType: PlayType) -> Unit) {
    launch {
        AudioPlayerManager.stateFlow()
         .map { it.playType }
         .distinctUntilChanged()
         .flowOn(Dispatchers.IO)
         .collect {
             handle(it)
         }
    }
}