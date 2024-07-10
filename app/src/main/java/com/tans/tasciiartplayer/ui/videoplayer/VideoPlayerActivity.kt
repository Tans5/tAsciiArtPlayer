package com.tans.tasciiartplayer.ui.videoplayer

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.util.Log
import android.view.MotionEvent
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
import com.tans.tmediaplayer.player.model.OptResult
import com.tans.tmediaplayer.player.tMediaPlayer
import com.tans.tmediaplayer.player.tMediaPlayerListener
import com.tans.tmediaplayer.player.tMediaPlayerState
import com.tans.tuiutils.activity.BaseCoroutineStateActivity
import com.tans.tuiutils.systembar.annotation.FullScreenStyle
import com.tans.tuiutils.view.clicks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

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
                    updateState { it.copy(loadStatus = PlayerLoadStatus.Prepared(mediaPlayer)) }
                    Log.d(TAG, "Load media file success.")
                }

                OptResult.Fail -> {
                    Log.e(TAG, "Load media file fail.")
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility", "Recycle")
    override fun CoroutineScope.bindContentViewCoroutine(contentView: View) {
        val viewBinding = VideoPlayerActivityBinding.bind(contentView)

        launch {
            renderStateNewCoroutine({ it.playerState }) {
                if (it is tMediaPlayerState.Error) {
                    Toast.makeText(this@VideoPlayerActivity, R.string.video_player_error, Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }

        launch {
            // Waiting player prepare.
            val mediaPlayer = stateFlow.map { it.loadStatus }.filterIsInstance<PlayerLoadStatus.Prepared>().first().player
            mediaPlayer.attachPlayerView(viewBinding.playerView)
            mediaPlayer.attachSubtitleView(viewBinding.subtitleTv)
            if (mediaPlayer.getState() is tMediaPlayerState.Prepared) {
                mediaPlayer.play()
            }
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
                    val d = MediaInfoDialog(info, intent.getMediaFileExtra())
                    d.showSafe(supportFragmentManager, "MediaInfoDialog#${System.currentTimeMillis()}")
                }

            }

            viewBinding.settingsIv.clicks(this) {
                viewBinding.actionLayout.hide()
                val d = PlayerSettingsDialog(playerView = viewBinding.playerView, player = mediaPlayer)
                d.showSafe(supportFragmentManager, "PlayerSettingsDialog#${System.currentTimeMillis()}}")
            }
            val subtitleStreams = mediaPlayer.getMediaInfo()?.subtitleStreams ?: emptyList()
            if (subtitleStreams.isNotEmpty()) {
                viewBinding.subtitleIv.show()
                viewBinding.subtitleIv.clicks(this) {
                    viewBinding.actionLayout.hide()
                    val d = SubtitleSelectDialog(player = mediaPlayer)
                    d.showSafe(supportFragmentManager, "SubtitleSelectDialog#${System.currentTimeMillis()}")
                }
            } else {
                viewBinding.subtitleIv.hide()
            }

            viewBinding.playerView.setOnTouchListener(PlayerClickTouchListener())
            viewBinding.playerView.clicks(this) {
                viewBinding.actionLayout.show()
            }

            viewBinding.actionLayout.setOnTouchListener(PlayerClickTouchListener())
            viewBinding.actionLayout.clicks(this) {
                viewBinding.actionLayout.hide()
            }
            launch {
                val mediaInfo = mediaPlayer.getMediaInfo()
                if (mediaInfo != null) {
                    val lastWatch = intent.getMediaLastWatch()
                    if ((lastWatch > 5000L && (mediaInfo.duration - lastWatch) > 5000L)) {
                        val targetSeekTime = lastWatch - 5000L
                        // Show 5s
                        viewBinding.lastWatchLayout.show()
                        viewBinding.lastWatchTv.text = targetSeekTime.formatDuration()
                        viewBinding.lastWatchDismissCircularPb.setVisibilityAfterHide(View.INVISIBLE)

                        val animatorJob = launch {
                            suspendCancellableCoroutine { cont ->
                                val animator = ValueAnimator.ofInt(0, 100)
                                animator.duration = 5000L
                                cont.invokeOnCancellation {
                                    animator.cancel()
                                }
                                animator.interpolator = LinearInterpolator()
                                animator.setEvaluator { fraction, startValue, endValue ->
                                    startValue as Int
                                    endValue as Int
                                    ((endValue - startValue).toFloat() * fraction).toInt()
                                }
                                animator.addUpdateListener { value ->
                                    val progress = value.animatedValue as Int
                                    viewBinding.lastWatchDismissCircularPb.setProgressCompat(progress, false)
                                    if (progress >= 100) {
                                        viewBinding.lastWatchDismissCircularPb.hide()
                                        if (cont.isActive) {
                                            cont.resume(Unit)
                                        }
                                    }
                                }
                                animator.start()
                            }
                            delay(300L)
                            viewBinding.lastWatchLayout.hide()
                        }

                        viewBinding.lastWatchLayout.clicks(this) {
                            mediaPlayer.seekTo(targetSeekTime)
                            viewBinding.lastWatchLayout.hide()
                            animatorJob.cancel()
                        }
                    }
                }
            }
        }
    }

    override fun onPause() {
        super.onPause()
        val player = (stateFlow.value.loadStatus as? PlayerLoadStatus.Prepared)?.player
        if (player != null && player.getState() is tMediaPlayerState.Playing) {
            player.pause()
        }
    }

    override fun onViewModelCleared() {
        super.onViewModelCleared()
        val player = (stateFlow.value.loadStatus as? PlayerLoadStatus.Prepared)?.player
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

    class PlayerClickTouchListener : View.OnTouchListener {
        private var downX: Float? = null
        private var downY: Float? = null

        private val minDownAndUpDistance = 10.0f
        @SuppressLint("ClickableViewAccessibility")
        override fun onTouch(v: View?, event: MotionEvent?): Boolean {
            return if (event != null) {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.x
                        downY = event.y
                        false
                    }
                    MotionEvent.ACTION_UP -> {
                        val dx = downX
                        val dy = downY
                        if (dx != null && dy != null) {
                            val d = sqrt((event.x - dx).pow(2) + (event.y - dy).pow(2))
                            d > minDownAndUpDistance
                        } else {
                            false
                        }
                    }
                    else -> false
                }
            } else {
                false
            }
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

        sealed class PlayerLoadStatus {
            data class Prepared(val player: tMediaPlayer) : PlayerLoadStatus()

            data object None : PlayerLoadStatus()
        }

        data class State(
            val playerState: tMediaPlayerState = tMediaPlayerState.NoInit,
            val progress: Progress = Progress(),
            val loadStatus: PlayerLoadStatus = PlayerLoadStatus.None
        )

        const val TAG = "VideoPlayerActivity"
    }
}