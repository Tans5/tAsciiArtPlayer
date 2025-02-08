package com.tans.tasciiartplayer.ui.videoplayer

import android.app.Dialog
import android.view.View
import android.widget.SeekBar
import com.tans.tasciiartplayer.R
import com.tans.tasciiartplayer.databinding.VideoPlayerSettingsDialogBinding
import com.tans.tmediaplayer.player.playerview.filter.AsciiArtImageFilter
import com.tans.tmediaplayer.player.playerview.tMediaPlayerView
import com.tans.tmediaplayer.player.tMediaPlayer
import com.tans.tmediaplayer.player.tMediaPlayerState
import com.tans.tuiutils.dialog.BaseCoroutineStateDialogFragment
import com.tans.tuiutils.dialog.createDefaultDialog

class VideoPlayerSettingsDialog : BaseCoroutineStateDialogFragment<Unit> {

    private val playerView: tMediaPlayerView?

    private val player: tMediaPlayer?

    constructor() : super(Unit) {
        this.playerView = null
        this.player = null
    }

    constructor(playerView: tMediaPlayerView, player: tMediaPlayer) : super(Unit) {
        this.playerView = playerView
        this.player = player
    }

    override val contentViewWidthInScreenRatio: Float = 0.5f

    override val layoutId: Int = R.layout.video_player_settings_dialog

    override fun createDialog(contentView: View): Dialog {
        return requireActivity().createDefaultDialog(contentView = contentView, dimAmount = 0.0f)
    }

    override fun firstLaunchInitData() {}

    override fun bindContentView(view: View) {
        val playerView = this.playerView ?: return
        val player = this.player ?: return
        val viewBinding = VideoPlayerSettingsDialogBinding.bind(view)
        val ctx = requireContext()
        fun requestRender() {
            val info = player.getMediaInfo()
            val state = player.getState()
            if (info?.videoStreamInfo?.isAttachment == true
                || state is tMediaPlayerState.Paused
                || state is tMediaPlayerState.PlayEnd
                || state is tMediaPlayerState.Stopped
            ) {
                playerView.requestRender()
            }
        }
        viewBinding.cropImageSw.isChecked = playerView.getScaleType() == tMediaPlayerView.Companion.ScaleType.CenterCrop
        viewBinding.cropImageSw.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                playerView.setScaleType(tMediaPlayerView.Companion.ScaleType.CenterCrop)
            } else {
                playerView.setScaleType(tMediaPlayerView.Companion.ScaleType.CenterFit)
            }
            requestRender()
        }

        val asciiArtFilter = playerView.getAsciiArtImageFilter()
        viewBinding.asciiFilterSw.isChecked = asciiArtFilter.isEnable()
        viewBinding.asciiFilterSw.setOnCheckedChangeListener { _, isChecked ->
            playerView.enableAsciiArtFilter(isChecked)
            requestRender()
        }


        viewBinding.charReverseSw.isChecked = asciiArtFilter.isReverseChar()
        viewBinding.charReverseSw.setOnCheckedChangeListener { _, isChecked ->
            asciiArtFilter.reverseChar(isChecked)
            requestRender()
        }

        viewBinding.colorReverseSw.isChecked = asciiArtFilter.isReverseColor()
        viewBinding.colorReverseSw.setOnCheckedChangeListener { _, isChecked ->
            asciiArtFilter.reverseColor(isChecked)
            requestRender()
        }


        viewBinding.charWidthSb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                val requestWidth = (progress.toFloat() / 100.0f * (AsciiArtImageFilter.MAX_CHAR_LINE_WIDTH - AsciiArtImageFilter.MIN_CHAR_LINE_WIDTH).toFloat() + AsciiArtImageFilter.MIN_CHAR_LINE_WIDTH.toFloat() + 0.5f).toInt()
                if (fromUser) {
                    asciiArtFilter.setCharLineWidth(requestWidth)
                    requestRender()
                    viewBinding.charWidthTv.text = ctx.getString(R.string.player_setting_dialog_ascii_char_width, requestWidth)
                }
            }
        })
        val charWidthProgress = ((asciiArtFilter.getCharLineWith().toFloat() - AsciiArtImageFilter.MIN_CHAR_LINE_WIDTH.toFloat()) / (AsciiArtImageFilter.MAX_CHAR_LINE_WIDTH - AsciiArtImageFilter.MIN_CHAR_LINE_WIDTH).toFloat() * 100.0f + 0.5f).toInt()
        val charWidth = asciiArtFilter.getCharLineWith()
        viewBinding.charWidthSb.progress = charWidthProgress
        viewBinding.charWidthTv.text = ctx.getString(R.string.player_setting_dialog_ascii_char_width, charWidth)

        viewBinding.imageColorFillRateSb.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onStartTrackingTouch(seekBar: SeekBar?) { }
            override fun onStopTrackingTouch(seekBar: SeekBar?) { }

            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val requestRate = progress.toFloat() / 100.0f
                    asciiArtFilter.colorFillRate(requestRate)
                    requestRender()
                    viewBinding.imageColorFillRateTv.text = ctx.getString(R.string.player_setting_dialog_ascii_image_color_fill_rate, progress)
                }
            }
        })
        val colorFillProgress = (asciiArtFilter.getColorFillRate() * 100.0f + 0.5f).toInt()
        viewBinding.imageColorFillRateTv.text = ctx.getString(R.string.player_setting_dialog_ascii_image_color_fill_rate, colorFillProgress)
        viewBinding.imageColorFillRateSb.progress = colorFillProgress
    }
}