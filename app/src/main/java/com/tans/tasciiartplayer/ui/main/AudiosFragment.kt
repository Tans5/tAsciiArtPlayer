package com.tans.tasciiartplayer.ui.main

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.tans.tasciiartplayer.R
import com.tans.tasciiartplayer.audio.AudioListType
import com.tans.tasciiartplayer.audio.AudioManager
import com.tans.tasciiartplayer.audio.AudioPlayerManager
import com.tans.tasciiartplayer.audio.AudioPlayerManagerState
import com.tans.tasciiartplayer.audio.PlayListState
import com.tans.tasciiartplayer.databinding.AudiosFragmentBinding
import com.tans.tasciiartplayer.formatDuration
import com.tans.tasciiartplayer.ui.audioplayer.AlbumsDialog
import com.tans.tasciiartplayer.ui.audioplayer.ArtistsDialog
import com.tans.tasciiartplayer.ui.audioplayer.AudioListDialog
import com.tans.tmediaplayer.player.tMediaPlayerState
import com.tans.tuiutils.dialog.dp2px
import com.tans.tuiutils.fragment.BaseCoroutineStateFragment
import com.tans.tuiutils.view.clicks
import com.tans.tuiutils.view.refreshes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.Optional

class AudiosFragment : BaseCoroutineStateFragment<AudioPlayerManagerState>(AudioPlayerManagerState()) {

    override val layoutId: Int = R.layout.audios_fragment

    override fun CoroutineScope.firstLaunchInitDataCoroutine() {
        launch {
            AudioManager.refreshMediaStoreAudios()
        }

        launch {
            AudioPlayerManager.stateFlow().collect { s -> updateState { s } }
        }
    }

    @Suppress("OPT_IN_USAGE")
    @SuppressLint("SetTextI18n")
    override fun CoroutineScope.bindContentViewCoroutine(contentView: View) {
        val viewBinding = AudiosFragmentBinding.bind(contentView)

        viewBinding.swipeRefreshLayout.refreshes(this, Dispatchers.IO) {
            AudioManager.refreshMediaStoreAudios()
        }

        viewBinding.allAudiosLayout.clicks(this) {
            val d = AudioListDialog(AudioListType.AllAudios)
            d.showSafe(requireActivity().supportFragmentManager, "AudioListDialog#${System.currentTimeMillis()}")
        }

        viewBinding.myFavoritesLayout.clicks(this) {
            val d = AudioListDialog(AudioListType.LikeAudios)
            d.showSafe(requireActivity().supportFragmentManager, "AudioListDialog#${System.currentTimeMillis()}")
        }

        viewBinding.albumsLayout.clicks(this) {
            val d = AlbumsDialog()
            d.showSafe(requireActivity().supportFragmentManager, "AlbumsDialog#${System.currentTimeMillis()}")
        }

        viewBinding.artistsLayout.clicks(this) {
            val d = ArtistsDialog()
            d.showSafe(requireActivity().supportFragmentManager, "ArtistsDialog#${System.currentTimeMillis()}")
        }

        viewBinding.customPlaylistsLayout.clicks(this) {
            // TODO:
        }

        // Playing audio
        renderStateNewCoroutine({ it.playListState }) {
            when (it) {
                PlayListState.NoSelectedList -> {
                    viewBinding.playCard.visibility = View.INVISIBLE
                }
                is PlayListState.SelectedPlayList -> {
                    viewBinding.playCard.visibility = View.VISIBLE
                }
            }
        }

        ViewCompat.setOnApplyWindowInsetsListener(viewBinding.playCard) { v, insets ->
            val systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val lp = v.layoutParams as? MarginLayoutParams
            if (lp != null) {
                lp.bottomMargin = systemInsets.bottom + v.context.dp2px(10)
                v.layoutParams = lp
            }
            insets
        }

        // Audio info.
        renderStateNewCoroutine({
            val playListState = it.playListState as? PlayListState.SelectedPlayList
            val playIndex = playListState?.currentPlayIndex
            val audio = if (playIndex != null) {
                playListState.audioList.audios.getOrNull(playIndex)
            } else {
                null
            }
            Optional.ofNullable(audio)
        }) {
            if (it.isPresent) {
                val audio = it.get()
                viewBinding.audioTitleTv.text = audio.mediaStoreAudio.title
                viewBinding.audioArtistAlbumTv.text = "${audio.mediaStoreAudio.artist}-${audio.mediaStoreAudio.album}"
            } else {
                viewBinding.audioTitleTv.text = ""
                viewBinding.audioArtistAlbumTv.text = ""
            }
        }

        // Player State
        launch {
            stateFlow()
                .map {
                    val playerState = (it.playListState as? PlayListState.SelectedPlayList)?.playerState
                    Optional.ofNullable(playerState)
                }
                .distinctUntilChanged()
                .debounce(100L)
                .flowOn(Dispatchers.IO)
                .collect {
                    if (it.isPresent) {
                        val state = it.get()
                        if (state is tMediaPlayerState.Playing) {
                            viewBinding.audioPauseIv.visibility = View.VISIBLE
                            viewBinding.audioPlayIv.visibility = View.GONE
                        } else {
                            viewBinding.audioPauseIv.visibility = View.GONE
                            viewBinding.audioPlayIv.visibility = View.VISIBLE
                        }
                    } else {
                        viewBinding.audioPauseIv.visibility = View.GONE
                        viewBinding.audioPlayIv.visibility = View.VISIBLE
                    }
                }
        }

        // Player Next/Previous play
        renderStateNewCoroutine({
            val playListState = it.playListState as? PlayListState.SelectedPlayList
            if (playListState != null) {
                (playListState.playedIndexes.size > 1) to (playListState.nextPlayIndex != null)
            } else {
                false to false
            }
        }) { (canPlayPrevious, canPlayNext) ->
            viewBinding.audioPreviousLayout.isEnabled = canPlayPrevious
            viewBinding.audioPreviousIv.isEnabled = canPlayPrevious

            viewBinding.audioNextLayout.isEnabled = canPlayNext
            viewBinding.audioNextIv.isEnabled = canPlayNext
        }

        // Player Progress/Duration
        renderStateNewCoroutine({
            val playListState = it.playListState as? PlayListState.SelectedPlayList
            if (playListState!= null) {
                playListState.playerProgress to playListState.playerDuration
            } else {
                0L to 0L
            }
        }) { (progress, duration) ->
            viewBinding.audioPlayingProgressTv.text = progress.formatDuration()
            viewBinding.audioDurationTv.text = duration.formatDuration()

            val progressInPercent = if (progress > 0 && duration > 0) {
                ((progress.toFloat() / duration.toFloat()) * 100.0f).toInt()
            } else {
                0
            }
            viewBinding.audioSeekBar.progress = progressInPercent
        }


        viewBinding.audioPreviousLayout.clicks(this, 1000L) {
            AudioPlayerManager.playPrevious()
        }

        viewBinding.audioNextLayout.clicks(this, 1000L) {
            AudioPlayerManager.playNext()
        }

        viewBinding.audioPlayPauseLayout.clicks(this, 1000L) {
            val playListState = AudioPlayerManager.stateFlow.value.playListState
            if (playListState is PlayListState.SelectedPlayList && playListState.playerState is tMediaPlayerState.Playing) {
                AudioPlayerManager.pause()
            } else {
                AudioPlayerManager.play()
            }
        }
    }
}