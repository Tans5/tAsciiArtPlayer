package com.tans.tasciiartplayer.ui.videoplayer

import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.view.MotionEvent
import android.view.View
import android.view.animation.LinearInterpolator
import android.widget.SeekBar
import android.widget.Toast
import com.tans.tasciiartplayer.AppLog
import com.tans.tasciiartplayer.AppSettings
import com.tans.tasciiartplayer.R
import com.tans.tasciiartplayer.audio.audioplayer.AudioPlayerManager
import com.tans.tasciiartplayer.video.VideoManager
import com.tans.tasciiartplayer.databinding.VideoPlayerActivityBinding
import com.tans.tasciiartplayer.formatDuration
import com.tans.tasciiartplayer.hwevent.HeadsetObserver
import com.tans.tasciiartplayer.hwevent.MediaKeyObserver
import com.tans.tasciiartplayer.hwevent.PhoneObserver
import com.tans.tmediaplayer.player.model.OptResult
import com.tans.tmediaplayer.player.tMediaPlayer
import com.tans.tmediaplayer.player.tMediaPlayerListener
import com.tans.tmediaplayer.player.tMediaPlayerState
import com.tans.tuiutils.activity.BaseCoroutineStateActivity
import com.tans.tuiutils.systembar.annotation.FullScreenStyle
import com.tans.tuiutils.view.clicks
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asExecutor
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.math.pow
import kotlin.math.sqrt

@FullScreenStyle
class VideoPlayerActivity : BaseCoroutineStateActivity<VideoPlayerActivity.Companion.State>(State()) {

    override val layoutId: Int = R.layout.video_player_activity

    private val mediaPlayer by lazyViewModelField {
        tMediaPlayer(
            audioOutputChannel = AppSettings.getAudioOutputChannelsBlocking(),
            audioOutputSampleRate = AppSettings.getAudioOutputSampleRateBlocking(),
            audioOutputSampleBitDepth = AppSettings.getAudioOutputSampleFormatBlocking(),
            enableVideoHardwareDecoder = AppSettings.isVideoDecodeHardwareBlocking()
        )
    }

    override fun CoroutineScope.firstLaunchInitDataCoroutine() {
        launch(Dispatchers.IO) {
            AudioPlayerManager.removeAudioList()

            mediaPlayer.setListener(object : tMediaPlayerListener {
                override fun onPlayerState(state: tMediaPlayerState) {
                    this@firstLaunchInitDataCoroutine.launch {
                        updateState { it.copy(playerState = state) }
                    }
                }

                override fun onProgressUpdate(progress: Long, duration: Long) {
                    this@firstLaunchInitDataCoroutine.launch {
                        updateState { it.copy(progress = Progress(progress = progress, duration = duration)) }
                    }
                }
            })

            val inputMediaType = intent.getInputMediaType()
            val loadResult = when (inputMediaType) {
                InputMediaType.MediaStore -> {
                    val file = intent.getMediaFileExtra()
                    AppLog.d(TAG, "Start load media file: $file")
                    mediaPlayer.prepare(file)
                }
                InputMediaType.CustomLink -> {
                    val customLink = intent.getMediaCustomLink()
                    AppLog.d(TAG, "Start load custom link: $customLink")
                    mediaPlayer.prepare(customLink)
                }
            }

            when (loadResult) {
                OptResult.Success -> {
                    // Observe headset
                    this@firstLaunchInitDataCoroutine.launch {
                        merge(
                            HeadsetObserver.observeEvent()
                                .map { it == HeadsetObserver.HeadsetEvent.WireHeadsetDisconnected || it == HeadsetObserver.HeadsetEvent.BluetoothHeadsetDisconnected },
                            PhoneObserver.observeEvent()
                                .map { it == PhoneObserver.PhoneEvent.PhoneRinging }
                        ).collect {
                            if (it) {
                                val playerState = mediaPlayer.getState()
                                if (playerState is tMediaPlayerState.Playing && mediaPlayer.getMediaInfo()?.isSeekable == true) {
                                    mediaPlayer.pause()
                                }
                            }
                        }
                    }

                    // Observe MediaKeyEvent
                    this@firstLaunchInitDataCoroutine.launch {
                        MediaKeyObserver.sessionActive(true)
                        MediaKeyObserver.observeEvent()
                            .collect { keyEvent ->
                                val playerState = mediaPlayer.getState()
                                if (playerState is tMediaPlayerState.Playing ||
                                    playerState is tMediaPlayerState.Paused ||
                                    playerState is tMediaPlayerState.PlayEnd ||
                                    playerState is tMediaPlayerState.Stopped) {
                                    when (keyEvent) {
                                        MediaKeyObserver.MediaKeyEvent.Play -> {
                                            if (playerState !is tMediaPlayerState.Playing) {
                                                mediaPlayer.play()
                                            }
                                        }

                                        MediaKeyObserver.MediaKeyEvent.Pause -> {
                                            if (playerState is tMediaPlayerState.Playing && mediaPlayer.getMediaInfo()?.isSeekable == true) {
                                                mediaPlayer.pause()
                                            }
                                        }

                                        MediaKeyObserver.MediaKeyEvent.PlayOrPause -> {
                                            if (playerState is tMediaPlayerState.Playing) {
                                                if (mediaPlayer.getMediaInfo()?.isSeekable == true ) {
                                                    mediaPlayer.pause()
                                                }
                                            } else {
                                                mediaPlayer.play()
                                            }
                                        }

                                        MediaKeyObserver.MediaKeyEvent.Stop -> {
                                            if (playerState !is tMediaPlayerState.Stopped &&
                                                playerState !is tMediaPlayerState.PlayEnd &&
                                                mediaPlayer.getMediaInfo()?.isSeekable == true
                                            ) {
                                                mediaPlayer.stop()
                                            }
                                        }
                                        else -> {
                                            // DO nothing
                                        }
                                    }
                                }
                            }
                    }
                    AppLog.d(TAG, "Load media file success.")
                }

                OptResult.Fail -> {
                    AppLog.e(TAG, "Load media file fail.")
                }
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility", "Recycle")
    override fun CoroutineScope.bindContentViewCoroutine(contentView: View) {
        val viewBinding = VideoPlayerActivityBinding.bind(contentView)

        // Error State observe.
        renderStateNewCoroutine({ it.playerState }) {
            if (it is tMediaPlayerState.Error) {
                Toast.makeText(this@VideoPlayerActivity, R.string.video_player_act_error, Toast.LENGTH_SHORT).show()
                finish()
            }
        }

        launch {
            viewBinding.seekingLoadingPb.show()
            // Waiting player active.
            stateFlow.map { it.playerState }.filterIsInstance<tMediaPlayerState.Prepared>().first()
            viewBinding.seekingLoadingPb.hide()
            mediaPlayer.attachPlayerView(viewBinding.playerView)
            mediaPlayer.attachSubtitleView(viewBinding.subtitleTv)
            if (mediaPlayer.getState() is tMediaPlayerState.Prepared) {
                mediaPlayer.play()
            }
            val mediaInfo = mediaPlayer.getMediaInfo()
            val isSeekable = mediaInfo?.isSeekable ?: false
            if (isSeekable && mediaInfo != null) {
                // Seekable
                viewBinding.durationTv.show()
                viewBinding.playerSb.show()
                viewBinding.playPauseLayout.show()

                // Render durationText
                this@bindContentViewCoroutine.renderStateNewCoroutine({ it.progress.duration }) { duration ->
                    viewBinding.durationTv.text = duration.formatDuration()
                }

                // Update Seekbar progress.
                var isPlayerSbInTouching = false
                this@bindContentViewCoroutine.renderStateNewCoroutine({ it.progress }) { (progress, duration) ->
                    if (!isPlayerSbInTouching && mediaPlayer.getState() !is tMediaPlayerState.Seeking) {
                        val progressInPercent = ((progress - mediaInfo.startTime).toFloat() * 100.0 / duration.toFloat() + 0.5f).toInt()
                        viewBinding.playerSb.progress = progressInPercent
                    }
                }

                // Do user seek request.
                viewBinding.playerSb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {}

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {
                        isPlayerSbInTouching = true
                    }

                    override fun onStopTrackingTouch(seekBar: SeekBar?) {
                        isPlayerSbInTouching = false
                        if (seekBar != null) {
                            val progressF = seekBar.progress.toFloat() / seekBar.max.toFloat()
                            val requestMediaProgress = (progressF * mediaInfo.duration.toDouble()).toLong() + mediaInfo.startTime
                            mediaPlayer.seekTo(requestMediaProgress)
                        }
                    }
                })

                // Observe Player State and update UI
                this@bindContentViewCoroutine.renderStateNewCoroutine({ it.playerState }) { playerState ->

                    // In Seeking.
                    if (playerState is tMediaPlayerState.Seeking) {
                        viewBinding.seekingLoadingPb.show()
                    } else {
                        viewBinding.seekingLoadingPb.hide()
                    }

                    val fixedState = when (playerState) {
                        is tMediaPlayerState.Seeking -> playerState.lastState
                        else -> playerState
                    }

                    // Playing
                    if (fixedState is tMediaPlayerState.Playing) {
                        viewBinding.pauseIv.show()
                    } else {
                        viewBinding.pauseIv.hide()
                    }

                    // Paused
                    if (fixedState is tMediaPlayerState.Prepared ||
                        fixedState is tMediaPlayerState.Paused ||
                        fixedState is tMediaPlayerState.Stopped
                    ) {
                        viewBinding.playIv.show()
                    } else {
                        viewBinding.playIv.hide()
                    }

                    // Play end.
                    if (fixedState is tMediaPlayerState.PlayEnd) {
                        viewBinding.replayIv.show()
                        viewBinding.actionLayout.show()
                    } else {
                        viewBinding.replayIv.hide()
                    }
                }


                // Player Opt clicks.
                viewBinding.playIv.clicks(this@bindContentViewCoroutine) {
                    mediaPlayer.play()
                }

                viewBinding.pauseIv.clicks(this@bindContentViewCoroutine) {
                    mediaPlayer.pause()
                }

                viewBinding.replayIv.clicks(this@bindContentViewCoroutine) {
                    mediaPlayer.play()
                }
            } else {
                // Not seekable
                viewBinding.durationTv.hide()
                viewBinding.playerSb.hide()
                viewBinding.playPauseLayout.hide()
            }

            // Render progress text.
            this@bindContentViewCoroutine.renderStateNewCoroutine({ it.progress.progress }) { progress ->
                viewBinding.progressTv.text = progress.formatDuration()
            }

            // MediaInfo
            viewBinding.infoIv.clicks(this@bindContentViewCoroutine) {
                val info = mediaPlayer.getMediaInfo()
                if (info != null) {
                    viewBinding.actionLayout.hide()
                    val d = VideoMediaInfoDialog(info)
                    d.showSafe(supportFragmentManager, "MediaInfoDialog#${System.currentTimeMillis()}")
                }

            }

            // Settings.
            viewBinding.settingsIv.clicks(this@bindContentViewCoroutine) {
                viewBinding.actionLayout.hide()
                val d = VideoPlayerSettingsDialog(playerView = viewBinding.playerView, player = mediaPlayer)
                d.showSafe(supportFragmentManager, "PlayerSettingsDialog#${System.currentTimeMillis()}}")
            }

            // Subtitles
            val subtitleStreams = mediaPlayer.getMediaInfo()?.subtitleStreams ?: emptyList()
            if (subtitleStreams.isNotEmpty()) {
                viewBinding.subtitleIv.show()
                viewBinding.subtitleIv.clicks(this@bindContentViewCoroutine) {
                    viewBinding.actionLayout.hide()
                    val d = SubtitleSelectDialog(player = mediaPlayer)
                    d.showSafe(supportFragmentManager, "SubtitleSelectDialog#${System.currentTimeMillis()}")
                }
            } else {
                viewBinding.subtitleIv.hide()
            }

            // Player action
            viewBinding.playerView.setOnTouchListener(PlayerClickTouchListener())
            viewBinding.playerView.clicks(this@bindContentViewCoroutine) {
                viewBinding.actionLayout.show()
            }
            viewBinding.actionLayout.setOnTouchListener(PlayerClickTouchListener())
            viewBinding.actionLayout.clicks(this@bindContentViewCoroutine) {
                viewBinding.actionLayout.hide()
            }

            // Change screen orientation
            viewBinding.changeScreenOrientationIv.clicks(this@bindContentViewCoroutine) {
                requestedOrientation = if (this@VideoPlayerActivity.resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
                } else {
                    ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
                }
            }

            // Show last watch history
            this@bindContentViewCoroutine.launch {
                if (mediaInfo != null && intent.getInputMediaType() == InputMediaType.MediaStore) {
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

                        viewBinding.lastWatchLayout.clicks(this@bindContentViewCoroutine) {
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
        if (mediaPlayer.getState() is tMediaPlayerState.Playing && mediaPlayer.getMediaInfo()?.isSeekable == true) {
            mediaPlayer.pause()
        }
    }

    override fun onViewModelCleared() {
        super.onViewModelCleared()
        // Update play history when finished.
        val info = mediaPlayer.getMediaInfo()
        if (info != null && intent.getInputMediaType() == InputMediaType.MediaStore) {
            val mediaId = intent.getMediaIdExtra()
            val state = mediaPlayer.getState()
            if (state is tMediaPlayerState.Stopped || state is tMediaPlayerState.PlayEnd) {
                VideoManager.updateOrInsertWatchHistory(videoId = mediaId, watchHistory = info.duration)
            } else {
                val progress = mediaPlayer.getProgress()
                VideoManager.updateOrInsertWatchHistory(videoId = mediaId, watchHistory = progress)
            }
        }
        Dispatchers.IO.asExecutor().execute {
            mediaPlayer.release()
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

        /**
         * 0: MediaStore
         * 1: CustomLink.
         */
        private const val INPUT_MEDIA_TYPE_EXTRA = "input_media_type_type_extra"

        /**
         * MediaStore
         */
        private const val MEDIA_FILE_EXTRA = "media_file_extra"
        private const val MEDIA_ID_EXTRA = "media_id_extra"
        private const val MEDIA_LAST_WATCH_EXTRA = "media_last_watch_extra"

        /**
         * CustomLink
         */
        private const val MEDIA_CUSTOM_LINK_EXTRA = "media_custom_link_extra"

        fun createIntent(context: Context, mediaId: Long, mediaFile: String, lastWatch: Long?): Intent {
            val intent = Intent(context, VideoPlayerActivity::class.java)
            intent.putExtra(INPUT_MEDIA_TYPE_EXTRA, InputMediaType.MediaStore.ordinal)
            intent.putExtra(MEDIA_ID_EXTRA, mediaId)
            intent.putExtra(MEDIA_FILE_EXTRA, mediaFile)
            intent.putExtra(MEDIA_LAST_WATCH_EXTRA, lastWatch ?: 0L)
            return intent
        }

        fun createIntent(context: Context, customLink: String): Intent {
            val intent = Intent(context, VideoPlayerActivity::class.java)
            intent.putExtra(INPUT_MEDIA_TYPE_EXTRA, InputMediaType.CustomLink.ordinal)
            intent.putExtra(MEDIA_CUSTOM_LINK_EXTRA, customLink)
            return intent
        }

        private fun Intent.getInputMediaType(): InputMediaType {
            val inputTypeInt = this.getIntExtra(INPUT_MEDIA_TYPE_EXTRA, 0)
            return InputMediaType.entries.find { it.ordinal == inputTypeInt } ?: InputMediaType.MediaStore
        }

        private fun Intent.getMediaFileExtra(): String = this.getStringExtra(MEDIA_FILE_EXTRA) ?: ""

        private fun Intent.getMediaIdExtra(): Long = this.getLongExtra(MEDIA_ID_EXTRA, 0L)

        private fun Intent.getMediaLastWatch(): Long = this.getLongExtra(MEDIA_LAST_WATCH_EXTRA, 0L)

        private fun Intent.getMediaCustomLink(): String = this.getStringExtra(MEDIA_CUSTOM_LINK_EXTRA) ?: ""

        data class Progress(
            val progress: Long = 0L,
            val duration: Long = 0L
        )

        data class State(
            val playerState: tMediaPlayerState = tMediaPlayerState.NoInit,
            val progress: Progress = Progress(),
        )

        private enum class InputMediaType {
            MediaStore,
            CustomLink
        }

        const val TAG = "VideoPlayerActivity"
    }
}