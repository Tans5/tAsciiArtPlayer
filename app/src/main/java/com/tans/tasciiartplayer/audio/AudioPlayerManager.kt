package com.tans.tasciiartplayer.audio

import com.tans.tasciiartplayer.AppLog
import com.tans.tasciiartplayer.AppSettings
import com.tans.tasciiartplayer.appGlobalCoroutineScope
import com.tans.tmediaplayer.player.tMediaPlayer
import com.tans.tmediaplayer.player.tMediaPlayerListener
import com.tans.tmediaplayer.player.tMediaPlayerState
import com.tans.tuiutils.state.CoroutineState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

object AudioPlayerManager : tMediaPlayerListener, CoroutineState<AudioPlayerManagerState> {

    override val stateFlow: MutableStateFlow<AudioPlayerManagerState> = MutableStateFlow(AudioPlayerManagerState())

    private val player: AtomicReference<tMediaPlayer?> by lazy {
        AtomicReference()
    }


    fun init() {
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
                        is PlayListState.SelectedPlayList -> listState.copy(playedIndexes = setOf(listState.currentPlayIndex))
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
                        playedIndexes = setOf(startIndex)
                    )
                }
                is PlayListState.SelectedPlayList -> {
                    if (list == s.playListState.audioList) {
                        // list not change
                        s.playListState.copy(
                            currentPlayIndex = startIndex,
                            playerProgress = 0L,
                            playerDuration = audio.mediaStoreAudio.duration,
                            playerState = tMediaPlayerState.NoInit,
                            playerMediaInfo = null,
                            playedIndexes = if (clearPlayedList) setOf(startIndex) else s.playListState.playedIndexes + setOf(startIndex)
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
                            playedIndexes = setOf(startIndex)
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
        if (state is tMediaPlayerState.Prepared) {
            player.play()
        }

        if (state is tMediaPlayerState.PlayEnd) {
            // TODO: load next audio.
        }

        if (state is tMediaPlayerState.Error) {
            AppLog.e(TAG, "Player error: $state")
        }

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

    private const val TAG = "AudioPlayerManager"
}