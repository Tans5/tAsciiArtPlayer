package com.tans.tasciiartplayer.audio.audioplayer

import com.tans.tasciiartplayer.audio.audiolist.AudioList
import com.tans.tasciiartplayer.audio.audiolist.AudioListManager
import com.tans.tasciiartplayer.audio.audiolist.AudioListType
import com.tans.tasciiartplayer.audio.audiolist.AudioModel
import com.tans.tasciiartplayer.audio.audioplayer.PlayType.SingleLoopPlay
import com.tans.tasciiartplayer.glide.MediaImageModel
import com.tans.tmediaplayer.player.tMediaPlayerState
import com.tans.tuiutils.mediastore.MediaStoreAudio
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

@OptIn(FlowPreview::class)
fun CoroutineScope.observePlayingtMediaPlayerStateChanged(handle: suspend (playerState: tMediaPlayerState?) -> Unit) {
    launch {
        AudioPlayerManager.stateFlow()
            .map {
                val playerState = (it.playListState as? PlayListState.SelectedPlayList)?.playerState
                Optional.ofNullable(playerState)
            }
            .filter { it.getOrNull().let { s -> s != tMediaPlayerState.NoInit && s !is tMediaPlayerState.Prepared }}
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

fun CoroutineScope.observePlayingProgressAndDurationChanged(handle: suspend (progress: Long, duration: Long) -> Unit) {
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

fun playingAudioChangedFlow(): Flow<Optional<AudioModel>> = AudioPlayerManager.stateFlow()
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

suspend fun playingAudioArtistList(): AudioList? {
    val audio = playingAudioChangedFlow().firstOrNull()?.getOrNull()
    return if (audio != null) {
        AudioListManager.stateFlow.value.artistAudioLists.find { (it.audioListType as AudioListType.ArtistAudios).artistId == audio.mediaStoreAudio.artistId }
    } else {
        null
    }
}

suspend fun playingAudioAlbumList(): AudioList? {
    val audio = playingAudioChangedFlow().firstOrNull()?.getOrNull()
    return if (audio != null) {
        AudioListManager.stateFlow.value.albumAudioLists.find { (it.audioListType as AudioListType.AlbumAudios).albumId == audio.mediaStoreAudio.albumId }
    } else {
        null
    }
}

fun CoroutineScope.observePlayingMediaStoreAudioChanged(handle: suspend (audioModel: MediaStoreAudio?) -> Unit) {
    launch {
        playingAudioChangedFlow()
            .map {
                val audio = it.getOrNull()?.mediaStoreAudio
                Optional.ofNullable(audio)
            }
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
            .collect {
                handle(it.getOrNull())
            }
    }
}

fun CoroutineScope.observePlayingLikeStateChanged(handle: suspend (isLike: Boolean) -> Unit) {
    launch {
        playingAudioChangedFlow()
            .map {
                it.getOrNull()?.isLike ?: false
            }
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
            .collect{
                handle(it)
            }
    }
}

fun CoroutineScope.observePlayingImageModelChanged(handle: suspend (imageModel: MediaImageModel?) -> Unit) {
    launch {
        playingAudioChangedFlow()
            .map {
                Optional.ofNullable(it.getOrNull()?.glideLoadModel)
            }
            .distinctUntilChanged()
            .flowOn(Dispatchers.IO)
            .collect {
                handle(it.getOrNull())
            }
    }
}

fun CoroutineScope.observeSelectedAudioListPlayTypeChanged(handle: suspend (playType: PlayType) -> Unit) {
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

fun CoroutineScope.observePlayingNextPlayAudio(handle: suspend (audio: MediaStoreAudio?) -> Unit) {
    launch {
        AudioPlayerManager.stateFlow()
            .map {
                val playListState = it.playListState as? PlayListState.SelectedPlayList
                val playIndex = playListState?.nextPlayIndex
                val audio = if (playIndex != null) {
                    playListState.audioList.audios.getOrNull(playIndex)?.mediaStoreAudio
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