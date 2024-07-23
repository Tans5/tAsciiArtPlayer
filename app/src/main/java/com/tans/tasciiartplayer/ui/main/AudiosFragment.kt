package com.tans.tasciiartplayer.ui.main

import android.annotation.SuppressLint
import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import android.widget.SeekBar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.tans.tasciiartplayer.R
import com.tans.tasciiartplayer.audio.audiolist.AudioListType
import com.tans.tasciiartplayer.audio.audiolist.AudioListManager
import com.tans.tasciiartplayer.audio.audioplayer.AudioPlayerManager
import com.tans.tasciiartplayer.audio.audioplayer.PlayListState
import com.tans.tasciiartplayer.audio.audioplayer.PlayType.*
import com.tans.tasciiartplayer.audio.audioplayer.getCurrentPlayAudio
import com.tans.tasciiartplayer.audio.audioplayer.observePlayTypeChanged
import com.tans.tasciiartplayer.audio.audioplayer.observePlayingAudioChanged
import com.tans.tasciiartplayer.audio.audioplayer.observePreviousAndNextSkipStateChanged
import com.tans.tasciiartplayer.audio.audioplayer.observeProgressAndDurationChanged
import com.tans.tasciiartplayer.audio.audioplayer.observeSelectedAudioListChanged
import com.tans.tasciiartplayer.audio.audioplayer.observetMediaPlayerStateChanged
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
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AudiosFragment : BaseCoroutineStateFragment<Unit>(Unit) {

    override val layoutId: Int = R.layout.audios_fragment

    override fun CoroutineScope.firstLaunchInitDataCoroutine() {
        launch(Dispatchers.IO) {
            AudioListManager.refreshMediaStoreAudios()
        }
    }

    @SuppressLint("SetTextI18n")
    override fun CoroutineScope.bindContentViewCoroutine(contentView: View) {
        val viewBinding = AudiosFragmentBinding.bind(contentView)

        viewBinding.swipeRefreshLayout.refreshes(this, Dispatchers.IO) {
            AudioListManager.refreshMediaStoreAudios()
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
        observeSelectedAudioListChanged {
            if (it == null) {
                viewBinding.playCard.visibility = View.INVISIBLE
            } else {
                viewBinding.playCard.visibility = View.VISIBLE
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
        observePlayingAudioChanged { audio ->
            if (audio != null) {
                viewBinding.audioTitleTv.text = audio.mediaStoreAudio.title
                viewBinding.audioArtistAlbumTv.text = "${audio.mediaStoreAudio.artist}-${audio.mediaStoreAudio.album}"
                viewBinding.likeIv.setImageResource(if (audio.isLike) R.drawable.icon_favorite_fill else R.drawable.icon_favorite_unfill)
            } else {
                viewBinding.audioTitleTv.text = ""
                viewBinding.audioArtistAlbumTv.text = ""
                viewBinding.likeIv.setImageResource(R.color.white)
            }
        }

        // Audio play type
        observePlayTypeChanged {
            viewBinding.playTypeIv.setImageResource(
                when (it) {
                    ListSequentialPlay -> R.drawable.icon_audio_sequence_play
                    ListLoopPlay -> R.drawable.icon_audio_list_loop_play
                    ListRandomPlay -> R.drawable.icon_audio_random_play
                    ListRandomLoopPlay -> R.drawable.icon_audio_random_play
                    SingleLoopPlay -> R.drawable.icon_audio_single_loop_play
                }
            )
        }

        // Player State
        observetMediaPlayerStateChanged { state ->
            if (state != null) {
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

        // Player Next/Previous play
        observePreviousAndNextSkipStateChanged { canSkipPrevious, canSkipNext ->
            viewBinding.audioPreviousLayout.isEnabled = canSkipPrevious
            viewBinding.audioPreviousIv.isEnabled = canSkipPrevious

            viewBinding.audioNextLayout.isEnabled = canSkipNext
            viewBinding.audioNextIv.isEnabled = canSkipNext
        }

        var isPlayerSbInTouching = false

        // Player Progress/Duration
        observeProgressAndDurationChanged { progress, duration ->
            viewBinding.audioPlayingProgressTv.text = progress.formatDuration()
            viewBinding.audioDurationTv.text = duration.formatDuration()

            if (!isPlayerSbInTouching) {
                val progressInPercent = if (progress > 0 && duration > 0) {
                    ((progress.toFloat() / duration.toFloat()) * 100.0f).toInt()
                } else {
                    0
                }
                viewBinding.audioSeekBar.progress = progressInPercent
            }
        }

        viewBinding.likeCard.clicks(this, 1000L) {
            val audio = (AudioPlayerManager.stateFlow.value.playListState as? PlayListState.SelectedPlayList)?.getCurrentPlayAudio()
            if (audio != null) {
                withContext(Dispatchers.IO) {
                    try {
                        if (audio.isLike) {
                            AudioListManager.unlikeAudio(audio.mediaStoreAudio.id)
                        } else {
                            AudioListManager.likeAudio(audio.mediaStoreAudio.id)
                        }
                    } catch (e: Throwable) {
                        e.printStackTrace()
                    }
                }
            }
        }

        viewBinding.playlistCard.clicks(this, 1000L) {
            val listType = (AudioPlayerManager.stateFlow.value.playListState as? PlayListState.SelectedPlayList)?.audioList?.audioListType
            if (listType != null) {
                val d = AudioListDialog(listType)
                d.showSafe(requireActivity().supportFragmentManager, "AudioListDialog${System.currentTimeMillis()}")
            }
        }

        viewBinding.playTypeCard.clicks(this, 1000L) {
            val currentPlayType = AudioPlayerManager.stateFlow.value.playType
            val newPlayType = when (currentPlayType) {
                ListSequentialPlay -> ListLoopPlay
                ListLoopPlay -> ListRandomPlay
                ListRandomPlay -> SingleLoopPlay
                SingleLoopPlay -> ListSequentialPlay
                ListRandomLoopPlay -> ListSequentialPlay
            }
            AudioPlayerManager.changePlayType(newPlayType)
        }

        viewBinding.audioPreviousLayout.clicks(this, 1000L, Dispatchers.IO) {
            AudioPlayerManager.playPrevious()
        }

        viewBinding.audioNextLayout.clicks(this, 1000L, Dispatchers.IO) {
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

        viewBinding.audioSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                isPlayerSbInTouching = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                isPlayerSbInTouching = false
                val duration = (AudioPlayerManager.stateFlow.value.playListState as? PlayListState.SelectedPlayList)?.playerDuration
                if (duration != null && duration > 0L) {
                    val progressF = seekBar.progress.toFloat() / seekBar.max.toFloat()
                    val requestMediaProgress = (progressF * duration.toDouble()).toLong()
                    AudioPlayerManager.seekTo(requestMediaProgress)
                }
            }
        })

        viewBinding.closeCard.clicks(this, 1000L) {
            AudioPlayerManager.removeAudioList()
        }

        viewBinding.playCard.clicks(this, 1000L) {
            // TODO:
        }
    }
}