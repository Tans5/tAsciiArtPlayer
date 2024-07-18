package com.tans.tasciiartplayer.audio

import android.os.SystemClock
import com.tans.tasciiartplayer.AppLog
import com.tans.tasciiartplayer.AppSettings
import com.tans.tasciiartplayer.appGlobalCoroutineScope
import com.tans.tmediaplayer.player.tMediaPlayer
import com.tans.tmediaplayer.player.tMediaPlayerListener
import com.tans.tmediaplayer.player.tMediaPlayerState
import com.tans.tuiutils.state.CoroutineState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference
import kotlin.random.Random

object AudioPlayerManager : tMediaPlayerListener, CoroutineState<AudioPlayerManagerState> {

    override val stateFlow: MutableStateFlow<AudioPlayerManagerState> = MutableStateFlow(AudioPlayerManagerState())

    private val player: AtomicReference<tMediaPlayer?> by lazy {
        AtomicReference()
    }

    fun init() {
        // Init player
        appGlobalCoroutineScope.launch {
            // This player never release.
            val p = tMediaPlayer(
                audioOutputChannel = AppSettings.getAudioOutputChannels(),
                audioOutputSampleBitDepth = AppSettings.getAudioOutputSampleFormat(),
                audioOutputSampleRate = AppSettings.getAudioOutputSampleRate(),
                enableVideoHardwareDecoder = false
            )
            player.getAndSet(p)?.release()
            p.setListener(this@AudioPlayerManager)
        }

        // Observe AudioList
        appGlobalCoroutineScope.launch {
            AudioManager.stateFlow()
                .distinctUntilChanged()
                .map { it to it.getAllPlayList() }
                .collectLatest { (managerState, allPlayList) ->
                    val state = stateFlow.value
                    val playListState = state.playListState
                    if (playListState is PlayListState.SelectedPlayList) {
                        val minePlayList = playListState.audioList
                        val managerPlayList = allPlayList[minePlayList.audioListType]
                        if (managerPlayList != minePlayList) {
                            // PlayList changed.
                            if (managerPlayList == null) {
                                // Current play list was removed.
                                removeAudioList()
                            } else {
                                val currentPlayAudio = playListState.getCurrentPlayAudio()
                                val managerPlayAudio = managerPlayList.audios.find { it.mediaStoreAudio.id == currentPlayAudio.mediaStoreAudio.id }
                                if (managerPlayAudio == null) {
                                    // Current play audio was removed from list.
                                    // Play first audio in the list.
                                    playAudioList(list = managerPlayList, startIndex = 0, clearPlayedList = true)
                                } else {
                                    // Current play audio was not removed from list.
                                    updateState { s ->
                                        val currentPlayIndex = managerPlayList.audios.indexOf(managerPlayAudio)
                                        s.copy(
                                            playListState = playListState.copy(
                                                audioList = managerPlayList,
                                                currentPlayIndex = currentPlayIndex,
                                                playedIndexes = listOf(currentPlayIndex),
                                                nextPlayIndex = computeNextPlayIndex(
                                                    playType = s.playType,
                                                    playedIndexes = listOf(currentPlayIndex),
                                                    currentPlayIndex = currentPlayIndex,
                                                    playListSize = managerPlayList.audios.size
                                                )
                                            )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
        }
    }

    fun playPrevious() {
        val state = stateFlow.value
        val playListState = state.playListState
        if (playListState is PlayListState.SelectedPlayList && playListState.playedIndexes.size > 1) {
            val newPlayedIndexes = playListState.playedIndexes.dropLast(1)
            val newCurrentPlayIndex = newPlayedIndexes.last()
            val newPlayedAudio = playListState.audioList.audios.getOrNull(newCurrentPlayIndex)
            if (newPlayedAudio != null) {
                updateState { s ->
                    s.copy(
                        playListState = playListState.copy(
                            playedIndexes = newPlayedIndexes,
                            currentPlayIndex = newCurrentPlayIndex,
                            nextPlayIndex = computeNextPlayIndex(
                                playType = s.playType,
                                playedIndexes = newPlayedIndexes,
                                currentPlayIndex = newCurrentPlayIndex,
                                playListSize = playListState.audioList.audios.size
                            )
                        )
                    )
                }
                ensurePlayer().prepare(newPlayedAudio.mediaStoreAudio.file?.canonicalPath ?: "")
            }
        }
    }

    fun playNext() {
        val state = stateFlow.value
        val playListState = state.playListState
        if (playListState is PlayListState.SelectedPlayList && playListState.nextPlayIndex != null) {
            playAudioList(
                list = playListState.audioList,
                startIndex = playListState.nextPlayIndex,
                clearPlayedList = false
            )
        }
    }

    fun play() {
        ensurePlayer().play()
    }

    fun pause() {
        ensurePlayer().pause()
    }

    fun seekTo(position: Long) {
        ensurePlayer().seekTo(position)
    }

    fun changePlayType(playType: PlayType) {
        updateState { s ->
            if (s.playType != playType) {
                val listState = s.playListState
                s.copy(
                    playType = playType,
                    playListState = when (listState) {
                        PlayListState.NoSelectedList -> PlayListState.NoSelectedList
                        is PlayListState.SelectedPlayList -> listState.copy(
                            playedIndexes = listOf(listState.currentPlayIndex),
                            nextPlayIndex = computeNextPlayIndex(
                                playType = playType,
                                playedIndexes = listOf(listState.currentPlayIndex),
                                currentPlayIndex = listState.currentPlayIndex,
                                playListSize = listState.audioList.audios.size
                            )
                        )
                    }
                )
            } else {
                s
            }
        }
    }

    fun removeAudioList() {
        val managerState = stateFlow.value
        if (managerState.playListState is PlayListState.SelectedPlayList) {
            val player = ensurePlayer()
            val playerState = player.getState()
            if (playerState is tMediaPlayerState.Playing ||
                playerState is tMediaPlayerState.Paused) {
                player.stop()
            }
            updateState { it.copy(playListState = PlayListState.NoSelectedList) }
        }
    }

    fun playAudioList(list: AudioList, startIndex: Int, clearPlayedList: Boolean = true) {
        val allPlayList = AudioManager.stateFlow.value.getAllPlayList()
        val listFromManager = allPlayList[list.audioListType]
        if (listFromManager != list) {
            AppLog.e(TAG, "Check list fail: ${list.audioListType}")
            return
        }
        val audio = list.audios.getOrNull(startIndex)
        if (audio == null) {
            AppLog.e(TAG, "Wrong play index $startIndex for list: $list")
            return
        }
        updateState { s ->
            val playListState = when (s.playListState) {
                PlayListState.NoSelectedList -> {
                    PlayListState.SelectedPlayList(
                        audioList = list,
                        currentPlayIndex = startIndex,
                        playerProgress = 0L,
                        playerDuration = audio.mediaStoreAudio.duration,
                        playerState = tMediaPlayerState.NoInit,
                        playerMediaInfo = null,
                        playedIndexes = listOf(startIndex),
                        nextPlayIndex = computeNextPlayIndex(
                            playType = s.playType,
                            playedIndexes = listOf(startIndex),
                            currentPlayIndex = startIndex,
                            playListSize = list.audios.size)
                    )
                }
                is PlayListState.SelectedPlayList -> {
                    if (list == s.playListState.audioList) {
                        val newPlayedIndexes = if (clearPlayedList) {
                            listOf(startIndex)
                        } else {
                            val oldList = s.playListState.playedIndexes
                            val i = oldList.indexOf(startIndex)
                            if (i >= 0) {
                                oldList.subList(0, i + 1)
                            } else {
                                oldList + startIndex
                            }
                        }
                        // list not change
                        s.playListState.copy(
                            currentPlayIndex = startIndex,
                            playerProgress = 0L,
                            playerDuration = audio.mediaStoreAudio.duration,
                            playerState = tMediaPlayerState.NoInit,
                            playerMediaInfo = null,
                            playedIndexes = newPlayedIndexes,
                            nextPlayIndex = computeNextPlayIndex(
                                playType = s.playType,
                                playedIndexes = newPlayedIndexes,
                                currentPlayIndex = startIndex,
                                playListSize = list.audios.size
                            )
                        )
                    } else {
                        // list changed
                        PlayListState.SelectedPlayList(
                            audioList = list,
                            currentPlayIndex = startIndex,
                            playerProgress = 0L,
                            playerDuration = audio.mediaStoreAudio.duration,
                            playerState = tMediaPlayerState.NoInit,
                            playerMediaInfo = null,
                            playedIndexes = listOf(startIndex),
                            nextPlayIndex = computeNextPlayIndex(
                                playType = s.playType,
                                playedIndexes = listOf(startIndex),
                                currentPlayIndex = startIndex,
                                playListSize = list.audios.size
                            )
                        )
                    }
                }
            }
            s.copy(playListState = playListState)
        }
        ensurePlayer().prepare(audio.mediaStoreAudio.file?.canonicalPath ?: "")
     }

    override fun onPlayerState(state: tMediaPlayerState) {

        val player = ensurePlayer()

        val lastManagerState = stateFlow.value

        if (lastManagerState.playListState is PlayListState.SelectedPlayList && lastManagerState.playListState.playerState != state) {
            updateSelectedPlayListState(
                errorState = {
                    player.stop()
                    // Shouldn't be this state.
                    AppLog.e(TAG, "Player state update, but no playlist active.")
                }
            ) {
                it.copy(
                    playerState = state,
                    playerMediaInfo = player.getMediaInfo()
                )
            }
        }

        if (state is tMediaPlayerState.Prepared) {
            player.play()
        }

        if (state is tMediaPlayerState.PlayEnd) {
            val s = stateFlow.value
            if (s.playListState is PlayListState.SelectedPlayList) {
                val nextPlayIndex = s.playListState.nextPlayIndex
                if (nextPlayIndex != null) {
                    playAudioList(
                        list = s.playListState.audioList,
                        startIndex = nextPlayIndex,
                        clearPlayedList = false
                    )
                }
            }
        }

        if (state is tMediaPlayerState.Error) {
            AppLog.e(TAG, "Player error: $state")
            removeAudioList()
        }
    }

    override fun onProgressUpdate(progress: Long, duration: Long) {
        updateSelectedPlayListState(
            errorState = {
                ensurePlayer().stop()
                // Shouldn't be this state.
                AppLog.e(TAG, "Player state update, but no playlist active.")
            }
        ) {
            it.copy(
                playerProgress = progress,
                playerDuration = duration
            )
        }
    }

    private fun ensurePlayer(): tMediaPlayer {
        return player.get() ?: error("Player is null, check init state.")
    }

    private fun updateSelectedPlayListState(errorState: (() -> Unit)? = null, success: (s: PlayListState.SelectedPlayList) -> PlayListState.SelectedPlayList) {
       updateState { s ->
           val playListState = when (s.playListState) {
               PlayListState.NoSelectedList -> {
                   errorState?.invoke()
                   PlayListState.NoSelectedList
               }

               is PlayListState.SelectedPlayList -> {
                   success(s.playListState)
               }
           }
           s.copy(playListState = playListState)
        }
    }

    private fun computeNextPlayIndex(
        playType: PlayType,
        playedIndexes: List<Int>,
        currentPlayIndex: Int,
        playListSize: Int
    ): Int? {
        if (currentPlayIndex >= playListSize || currentPlayIndex < 0) {
            return null
        }
        return when (playType) {
            PlayType.ListSequentialPlay -> {
                val next = currentPlayIndex + 1
                if (next >= playListSize) {
                    null
                } else {
                    next
                }
            }
            PlayType.ListLoopPlay -> {
                (currentPlayIndex + 1) % playListSize
            }
            PlayType.ListRandomPlay, PlayType.ListRandomLoopPlay -> {
                val canUseIndexes = (0 until playListSize)
                    .filter { !playedIndexes.contains(it) }
                    .toList()
                    .let {
                        if (playType == PlayType.ListRandomLoopPlay) {
                            (0 until playListSize).toList()
                        } else {
                            it
                        }
                    }
                if (canUseIndexes.isEmpty()) {
                    null
                } else {
                    return canUseIndexes.random(Random(SystemClock.uptimeMillis()))
                }

            }
            PlayType.SingleLoopPlay -> {
                currentPlayIndex
            }
        }
    }


    private const val TAG = "AudioPlayerManager"
}