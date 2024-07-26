package com.tans.tasciiartplayer.ui.audioplayer

import android.annotation.SuppressLint
import android.view.View
import android.widget.SeekBar
import com.bumptech.glide.Glide
import com.tans.tasciiartplayer.R
import com.tans.tasciiartplayer.audio.audiolist.AudioListManager
import com.tans.tasciiartplayer.audio.audioplayer.AudioPlayerManager
import com.tans.tasciiartplayer.audio.audioplayer.PlayListState
import com.tans.tasciiartplayer.audio.audioplayer.PlayType.ListLoopPlay
import com.tans.tasciiartplayer.audio.audioplayer.PlayType.ListRandomLoopPlay
import com.tans.tasciiartplayer.audio.audioplayer.PlayType.ListRandomPlay
import com.tans.tasciiartplayer.audio.audioplayer.PlayType.ListSequentialPlay
import com.tans.tasciiartplayer.audio.audioplayer.PlayType.SingleLoopPlay
import com.tans.tasciiartplayer.audio.audioplayer.getCurrentPlayAudio
import com.tans.tasciiartplayer.audio.audioplayer.observePlayingImageModelChanged
import com.tans.tasciiartplayer.audio.audioplayer.observePlayingLikeStateChanged
import com.tans.tasciiartplayer.audio.audioplayer.observePlayingNextPlayAudio
import com.tans.tasciiartplayer.audio.audioplayer.observeSelectedAudioListPlayTypeChanged
import com.tans.tasciiartplayer.audio.audioplayer.observePlayingMediaStoreAudioChanged
import com.tans.tasciiartplayer.audio.audioplayer.observePreviousAndNextSkipStateChanged
import com.tans.tasciiartplayer.audio.audioplayer.observePlayingProgressAndDurationChanged
import com.tans.tasciiartplayer.audio.audioplayer.observeSelectedAudioListChanged
import com.tans.tasciiartplayer.audio.audioplayer.observePlayingtMediaPlayerStateChanged
import com.tans.tasciiartplayer.databinding.AudioPlayerActivityBinding
import com.tans.tasciiartplayer.formatDuration
import com.tans.tmediaplayer.player.tMediaPlayerState
import com.tans.tuiutils.activity.BaseCoroutineStateActivity
import com.tans.tuiutils.systembar.annotation.SystemBarStyle
import com.tans.tuiutils.view.clicks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@SystemBarStyle(statusBarThemeStyle = 1, navigationBarThemeStyle = 1)
class AudioPlayerActivity : BaseCoroutineStateActivity<Unit>(Unit) {

    override val layoutId: Int = R.layout.audio_player_activity

    override fun CoroutineScope.firstLaunchInitDataCoroutine() {
        launch(Dispatchers.Main) {
            observeSelectedAudioListChanged {
                if (it == null) {
                    finish()
                }
            }
        }
    }

    @SuppressLint("SetTextI18n")
    override fun CoroutineScope.bindContentViewCoroutine(contentView: View) {
        val viewBinding = AudioPlayerActivityBinding.bind(contentView)

        // Audio info.
        observePlayingMediaStoreAudioChanged { audio ->
            if (audio != null) {
                viewBinding.audioTitleTv.text = audio.title
                viewBinding.audioArtistAlbumTv.text = "${audio.artist}-${audio.album}"
            } else {
                viewBinding.audioTitleTv.text = ""
                viewBinding.audioArtistAlbumTv.text = ""

            }
        }

        // Audio like
        observePlayingLikeStateChanged {
            viewBinding.likeIv.setImageResource(if (it) R.drawable.icon_favorite_fill else R.drawable.icon_favorite_unfill)
        }

        // Audio image
        observePlayingImageModelChanged {
            if (it != null) {
                Glide.with(this@AudioPlayerActivity)
                    .load(it)
                    .error(R.drawable.icon_audio)
                    .into(viewBinding.audioIv)
            } else {
                viewBinding.likeIv.setImageResource(R.color.white)
            }
        }

        // Audio play type
        observeSelectedAudioListPlayTypeChanged {
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
        observePlayingtMediaPlayerStateChanged { state ->
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
        observePlayingProgressAndDurationChanged { progress, duration ->
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

        observePlayingNextPlayAudio { nextPlayAudio ->
            if (nextPlayAudio == null) {
                viewBinding.nextPlayTv.text = getString(R.string.audio_player_act_list_play_end)
            } else {
                viewBinding.nextPlayTv.text = getString(R.string.audio_player_act_next_play, nextPlayAudio.title)
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
                d.showSafe(this@AudioPlayerActivity.supportFragmentManager, "AudioListDialog${System.currentTimeMillis()}")
            }
        }

        viewBinding.playTypeCard.clicks(this, 500L) {
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

        viewBinding.audioInfoCard.clicks(this, 1000L) {
            AudioMediaInfoDialog().showSafe(supportFragmentManager, "AudioMediaInfoDialog#${System.currentTimeMillis()}")
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
    }

    override fun onDestroy() {
        super.onDestroy()
        AudioListDialog.removeCacheContentViewAndTask(this)
    }

}