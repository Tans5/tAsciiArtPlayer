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

    override val stateFlow: MutableStateFlow<AudioPlayerManagerState> = MutableStateFlow(AudioPlayerManagerState.NoSelectedList)

    private val player: AtomicReference<tMediaPlayer?> by lazy {
        AtomicReference()
    }


    fun init() {
        appGlobalCoroutineScope.launch {
            val p = tMediaPlayer(
                audioOutputChannel = AppSettings.getAudioOutputChannels(),
                audioOutputSampleBitDepth = AppSettings.getAudioOutputSampleFormat(),
                audioOutputSampleRate = AppSettings.getAudioOutputSampleRate(),
                enableVideoHardwareDecoder = false
            )
            p.setListener(this@AudioPlayerManager)
            player.getAndSet(p)?.release()
        }
    }

    override fun onPlayerState(state: tMediaPlayerState) {
        val player = ensurePlayer()
        if (state is tMediaPlayerState.Prepared) {
            player.play()
        }
        if (state is tMediaPlayerState.PlayEnd) {
            // TODO: load next audio.
        }
        updateState { s ->
            when (s) {
                AudioPlayerManagerState.NoSelectedList -> {
                    // Shouldn't be this state.
                    AppLog.e(TAG, "Player state update, but not playlist active.")
                    s
                }
                is AudioPlayerManagerState.SelectedPlayList -> {
                    s.copy(
                        playerState = state,
                        playerMediaInfo = player.getMediaInfo()
                    )
                }
            }
        }
    }

    override fun onProgressUpdate(progress: Long, duration: Long) {
//        updateState { s ->
//            when (s) {
//                AudioPlayerManagerState.NoSelectedList -> {
//                    // Shouldn't be this state.
//                    AppLog.e(TAG, "Player state update, but not playlist active.")
//                    s
//                }
//                is AudioPlayerManagerState.SelectedPlayList -> {
//                    s.copy(
//                        playerState = state,
//                        playerMediaInfo = player.getMediaInfo()
//                    )
//                }
//            }
//        }
    }

    private fun ensurePlayer(): tMediaPlayer {
        return player.get() ?: error("Player is null, check init state.")
    }

//    private fun updateSelectedPlayList(success: (s: AudioPlayerManagerState.SelectedPlayList) -> AudioPlayerManagerState.SelectedPlayList, errorState: (() -> Unit)? = null) {
//        updateState {
//
//        }
//    }

    private const val TAG = "AudioPlayerManager"
}