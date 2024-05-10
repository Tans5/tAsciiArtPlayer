package com.tans.tasciiartplayer.ui.videoplayer

import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.SeekBar
import android.widget.Toast
import com.tans.tasciiartplayer.AppSettings
import com.tans.tasciiartplayer.R
import com.tans.tasciiartplayer.video.VideoManager
import com.tans.tasciiartplayer.databinding.VideoPlayerActivityBinding
import com.tans.tasciiartplayer.formatDuration
import com.tans.tasciiartplayer.ui.common.MediaInfoDialog
import com.tans.tasciiartplayer.ui.common.PlayerSettingsDialog
import com.tans.tmediaplayer.player.OptResult
import com.tans.tmediaplayer.frameloader.tMediaFrameLoader
import com.tans.tmediaplayer.player.tMediaPlayer
import com.tans.tmediaplayer.player.tMediaPlayerListener
import com.tans.tmediaplayer.player.tMediaPlayerState
import com.tans.tuiutils.activity.BaseCoroutineStateActivity
import com.tans.tuiutils.systembar.annotation.FullScreenStyle
import com.tans.tuiutils.view.clicks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.launch
import java.util.Optional
import kotlin.jvm.optionals.getOrNull
import kotlin.math.abs

@FullScreenStyle
class VideoPlayerActivity : BaseCoroutineStateActivity<VideoPlayerActivity.Companion.State>(State()) {

    override val layoutId: Int = R.layout.video_player_activity

    override fun CoroutineScope.firstLaunchInitDataCoroutine() {
        launch(Dispatchers.IO) {

            val mediaPlayer = tMediaPlayer(
                audioOutputChannel = AppSettings.getAudioOutputChannels(),
                audioOutputSampleRate = AppSettings.getAudioOutputSampleRate(),
                audioOutputSampleBitDepth = AppSettings.getAudioOutputSampleFormat(),
                enableVideoHardwareDecoder = AppSettings.isVideoDecodeHardware()
            )
            if (isFinishing || isDestroyed) {
                mediaPlayer.release()
                return@launch
            }

            updateState { it.copy(player = Optional.of(mediaPlayer)) }

            mediaPlayer.setListener(object : tMediaPlayerListener {
                override fun onPlayerState(state: tMediaPlayerState) {
                    updateState { it.copy(playerState = state) }
                }

                override fun onProgressUpdate(progress: Long, duration: Long) {
                    updateState { it.copy(progress = Progress(progress = progress, duration = duration)) }
                }
            })

            val loadResult = mediaPlayer.prepare(intent.getMediaFileExtra())
            when (loadResult) {
                OptResult.Success -> {
                    mediaPlayer.play()
                    Log.d(TAG, "Load media file success.")
                }

                OptResult.Fail -> {
                    Log.e(TAG, "Load media file fail.")
                }
            }
        }
    }

    override fun CoroutineScope.bindContentViewCoroutine(contentView: View) {
        tMediaFrameLoader
        val viewBinding = VideoPlayerActivityBinding.bind(contentView)

        launch {
            // Waiting player is ok.
            val mediaPlayer = stateFlow.mapNotNull { it.player.getOrNull() }.first()

            mediaPlayer.attachPlayerView(viewBinding.playerView)

            renderStateNewCoroutine({ it.progress.duration }) { duration ->
                viewBinding.durationTv.text = duration.formatDuration()
            }

            var isPlayerSbInTouching = false
            renderStateNewCoroutine({ it.progress }) { (progress, duration) ->
                viewBinding.progressTv.text = progress.formatDuration()
                if (!isPlayerSbInTouching && mediaPlayer.getState() !is tMediaPlayerState.Seeking) {
                    val progressInPercent = (progress.toFloat() * 100.0 / duration.toFloat() + 0.5f).toInt()
                    viewBinding.playerSb.progress = progressInPercent
                }
            }

            renderStateNewCoroutine({ it.playerState }) { playerState ->

                if (playerState is tMediaPlayerState.Error) {
                    Toast.makeText(this@VideoPlayerActivity, R.string.video_player_error, Toast.LENGTH_SHORT).show()
                    finish()
                }

                if (playerState is tMediaPlayerState.Seeking) {
                    viewBinding.seekingLoadingPb.show()
                } else {
                    viewBinding.seekingLoadingPb.hide()
                }

                val fixedState = when (playerState) {
                    is tMediaPlayerState.Seeking -> playerState.lastState
                    else -> playerState
                }
                if (fixedState is tMediaPlayerState.Playing) {
                    viewBinding.pauseIv.show()
                } else {
                    viewBinding.pauseIv.hide()
                }

                if (fixedState is tMediaPlayerState.Prepared ||
                    fixedState is tMediaPlayerState.Paused ||
                    fixedState is tMediaPlayerState.Stopped
                ) {
                    viewBinding.playIv.show()
                } else {
                    viewBinding.playIv.hide()
                }

                if (fixedState is tMediaPlayerState.PlayEnd) {
                    viewBinding.replayIv.show()
                    viewBinding.actionLayout.show()
                } else {
                    viewBinding.replayIv.hide()
                }
            }

            viewBinding.playIv.clicks(this) {
                mediaPlayer.play()
            }

            viewBinding.pauseIv.clicks(this) {
                mediaPlayer.pause()
            }

            viewBinding.replayIv.clicks(this) {
                mediaPlayer.play()
            }

            viewBinding.playerSb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}

                override fun onStartTrackingTouch(seekBar: SeekBar?) {
                    isPlayerSbInTouching = true
                }

                override fun onStopTrackingTouch(seekBar: SeekBar?) {
                    isPlayerSbInTouching = false
                    val mediaInfo = mediaPlayer.getMediaInfo()
                    if (seekBar != null && mediaInfo != null) {
                        val progressF = seekBar.progress.toFloat() / seekBar.max.toFloat()
                        val requestMediaProgress = (progressF * mediaInfo.duration.toDouble()).toLong()
                        mediaPlayer.seekTo(requestMediaProgress)
                    }
                }
            })

            viewBinding.infoIv.clicks(this) {
                val info = mediaPlayer.getMediaInfo()
                if (info != null) {
                    viewBinding.actionLayout.hide()
                    val d = MediaInfoDialog(info)
                    d.show(supportFragmentManager, "MediaInfoDialog#${System.currentTimeMillis()}")
                }

            }

            viewBinding.settingsIv.clicks(this) {
                viewBinding.actionLayout.hide()
                val d = PlayerSettingsDialog(playerView = viewBinding.playerView)
                d.show(supportFragmentManager, "PlayerSettingsDialog#${System.currentTimeMillis()}}")
            }

            viewBinding.playerView.clicks(this) {
                viewBinding.actionLayout.show()
            }

            viewBinding.actionLayout.clicks(this) {
                viewBinding.actionLayout.hide()
            }
            launch {
                val mediaInfo = stateFlow.mapNotNull { it.player.getOrNull()?.getMediaInfo() }.first()
                val lastWatch = intent.getMediaLastWatch()
                if ((lastWatch > 5000L && (mediaInfo.duration - lastWatch) > 5000L)) {
                    // Show 5s
                    viewBinding.lastWatchLayout.show()
                    viewBinding.lastWatchTv.text = lastWatch.formatDuration()
                    viewBinding.lastWatchDismissCircularPb.setProgressWithAnimation(progress = 0.0f, duration = 5000L, interpolator = LinearInterpolator())
                    viewBinding.lastWatchDismissCircularPb.onProgressChangeListener = {
                        if (abs(it - 0.0f) < 0.001f) {
                            viewBinding.lastWatchLayout.hide()
                        }
                    }
                    viewBinding.lastWatchLayout.clicks(this) {
                        mediaPlayer.seekTo(lastWatch)
                        viewBinding.lastWatchLayout.hide()
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        val player = stateFlow.value.player.getOrNull()
        if (player != null && player.getState() is tMediaPlayerState.Playing) {
            player.pause()
        }
    }

    override fun onViewModelCleared() {
        super.onViewModelCleared()
        val player = stateFlow.value.player.getOrNull()
        if (player != null) {
            val info = player.getMediaInfo()
            if (info != null) {
                val mediaId = intent.getMediaIdExtra()
                val state = player.getState()
                if (state is tMediaPlayerState.Stopped || state is tMediaPlayerState.PlayEnd) {
                    VideoManager.updateOrInsertWatchHistory(videoId = mediaId, watchHistory = info.duration)
                } else {
                    val progress = player.getProgress()
                    VideoManager.updateOrInsertWatchHistory(videoId = mediaId, watchHistory = progress)
                }
            }
            player.release()
        }
    }

    private fun View.isVisible(): Boolean = this.visibility == View.VISIBLE

    private fun View.isInvisible(): Boolean = !isVisible()

    private fun View.hide() {
        if (isVisible()) {
            this.visibility = View.GONE
        }
    }

    private fun View.show() {
        if (isInvisible()) {
            this.visibility = View.VISIBLE
        }
    }

    companion object {

        private const val MEDIA_FILE_EXTRA = "media_file_extra"
        private const val MEDIA_ID_EXTRA = "media_id_extra"
        private const val MEDIA_LAST_WATCH_EXTRA = "media_last_watch_extra"

        fun createIntent(context: Context, mediaId: Long, mediaFile: String, lastWatch: Long?): Intent {
            val intent = Intent(context, VideoPlayerActivity::class.java)
            intent.putExtra(MEDIA_ID_EXTRA, mediaId)
            intent.putExtra(MEDIA_FILE_EXTRA, mediaFile)
            intent.putExtra(MEDIA_LAST_WATCH_EXTRA, lastWatch ?: 0L)
            return intent
        }

        private fun Intent.getMediaFileExtra(): String = this.getStringExtra(MEDIA_FILE_EXTRA) ?: ""

        private fun Intent.getMediaIdExtra(): Long = this.getLongExtra(MEDIA_ID_EXTRA, 0L)

        private fun Intent.getMediaLastWatch(): Long = this.getLongExtra(MEDIA_LAST_WATCH_EXTRA, 0L)

        data class Progress(
            val progress: Long = 0L,
            val duration: Long = 0L
        )

        data class State(
            val playerState: tMediaPlayerState = tMediaPlayerState.NoInit,
            val progress: Progress = Progress(),
            val player: Optional<tMediaPlayer> = Optional.empty()
        )

        const val TAG = "MainActivity"
    }
}