package com.tans.tasciiartplayer.ui.main

import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.widget.PopupWindowCompat
import com.tans.tasciiartplayer.AppSettings
import com.tans.tasciiartplayer.R
import com.tans.tasciiartplayer.databinding.AppSettingsDialogBinding
import com.tans.tasciiartplayer.databinding.AppSettingsPopupWindowItemLayoutBinding
import com.tans.tasciiartplayer.databinding.AppSettingsPopupWindowLayoutBinding
import com.tans.tmediaplayer.player.model.AudioChannel
import com.tans.tmediaplayer.player.model.AudioSampleBitDepth
import com.tans.tmediaplayer.player.model.AudioSampleRate
import com.tans.tuiutils.adapter.impl.builders.SimpleAdapterBuilderImpl
import com.tans.tuiutils.adapter.impl.databinders.DataBinderImpl
import com.tans.tuiutils.adapter.impl.datasources.FlowDataSourceImpl
import com.tans.tuiutils.adapter.impl.viewcreatators.SingleItemViewCreatorImpl
import com.tans.tuiutils.dialog.BaseCoroutineStateDialogFragment
import com.tans.tuiutils.view.clicks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class AppSettingsDialog : BaseCoroutineStateDialogFragment<Unit>(Unit) {

    override val layoutId: Int = R.layout.app_settings_dialog

    override fun firstLaunchInitData() {  }

    override fun bindContentView(view: View) {
        val viewBinding = AppSettingsDialogBinding.bind(view)
        launch {
            viewBinding.videoHwDecoderSw.isChecked = AppSettings.isVideoDecodeHardware()
            viewBinding.outputChannelTv.updateOutputChannel(AppSettings.getAudioOutputChannels())
            viewBinding.outputRateTv.updateOutputRate(AppSettings.getAudioOutputSampleRate())
            viewBinding.outputFormatTv.updateOutputFormat(AppSettings.getAudioOutputSampleFormat())

            viewBinding.videoHwDecoderSw.setOnCheckedChangeListener { _, isChecked ->
                launch(Dispatchers.IO) {
                    AppSettings.setVideoDecodeHardware(isChecked)
                }
            }

            val channelSelectWindow = createMenuPopupWindow(
                items = AudioChannel.entries.toList(),
                renderText = { textView, audioChannel ->  textView.updateOutputChannel(audioChannel) },
                itemClicks = { window, data ->
                    withContext(Dispatchers.IO) {
                        AppSettings.setAudioOutputChannels(data)
                    }
                    window.dismiss()
                    viewBinding.outputChannelTv.updateOutputChannel(data)
                }
            )
            channelSelectWindow.setOnDismissListener {
                viewBinding.outputChannelIv.showUpOrDownArrow(false)
            }
            viewBinding.outputChannelLayout.clicks(this) {
                channelSelectWindow.showAsDropDown(viewBinding.outputChannelLayout)
                viewBinding.outputChannelIv.showUpOrDownArrow(true)
            }

            val rateSelectWindow = createMenuPopupWindow(
                items = AudioSampleRate.entries.toList(),
                renderText = { textView, audioRate ->  textView.updateOutputRate(audioRate) },
                itemClicks = { window, data ->
                    withContext(Dispatchers.IO) {
                        AppSettings.setAudioOutputSampleRate(data)
                    }
                    window.dismiss()
                    viewBinding.outputRateTv.updateOutputRate(data)
                }
            )
            rateSelectWindow.setOnDismissListener {
                viewBinding.outputRateIv.showUpOrDownArrow(false)
            }
            viewBinding.outputRateLayout.clicks(this) {
                rateSelectWindow.showAsDropDown(viewBinding.outputRateLayout)
                viewBinding.outputRateIv.showUpOrDownArrow(true)
            }

            val formatSelectWindow = createMenuPopupWindow(
                items = AudioSampleBitDepth.entries.toList(),
                renderText = { textView, format ->  textView.updateOutputFormat(format) },
                itemClicks = { window, data ->
                    withContext(Dispatchers.IO) {
                        AppSettings.setAudioOutputSampleFormat(data)
                    }
                    window.dismiss()
                    viewBinding.outputFormatTv.updateOutputFormat(data)
                }
            )
            formatSelectWindow.setOnDismissListener {
                viewBinding.outputFormatIv.showUpOrDownArrow(false)
            }
            viewBinding.outputFormatLayout.clicks(this) {
                formatSelectWindow.showAsDropDown(viewBinding.outputFormatLayout)
                viewBinding.outputFormatIv.showUpOrDownArrow(true)
            }
        }
    }

    private fun TextView.updateOutputChannel(channel: AudioChannel) {
        text = when (channel) {
            AudioChannel.Mono -> "Mono"
            AudioChannel.Stereo -> "Stereo"
        }
    }

    private fun TextView.updateOutputRate(rate: AudioSampleRate) {
        text = when (rate) {
            AudioSampleRate.Rate44100 -> "44100 Hz"
            AudioSampleRate.Rate48000 -> "48000 Hz"
            AudioSampleRate.Rate96000 -> "96000 Hz"
            AudioSampleRate.Rate192000 -> "192000 Hz"
        }
    }

    private fun TextView.updateOutputFormat(format: AudioSampleBitDepth) {
        text = when (format) {
            AudioSampleBitDepth.EightBits -> "Unsigned 8"
            AudioSampleBitDepth.SixteenBits -> "Signed 16"
            AudioSampleBitDepth.ThreeTwoBits -> "Signed 32"
        }
    }

    private fun ImageView.showUpOrDownArrow(showPopupWindow: Boolean) {
        setImageResource(if (showPopupWindow) R.drawable.icon_arrow_drop_up else R.drawable.icon_arrow_drop_down)
    }

    private fun <T : Any> createMenuPopupWindow(
        items: List<T>,
        renderText: (textView: TextView, T) -> Unit,
        itemClicks: suspend (window: PopupWindow, data: T) -> Unit
    ): PopupWindow {
        val viewBinding = AppSettingsPopupWindowLayoutBinding.inflate(LayoutInflater.from(requireContext()), requireActivity().window.decorView as? ViewGroup, false)
        val window = PopupWindow(requireActivity())
        viewBinding.recyclerView.adapter = SimpleAdapterBuilderImpl<T>(
            itemViewCreator = SingleItemViewCreatorImpl(R.layout.app_settings_popup_window_item_layout),
            dataSource = FlowDataSourceImpl(flow { emit(items) }),
            dataBinder = DataBinderImpl { data, view, _ ->
                val itemViewBinding = AppSettingsPopupWindowItemLayoutBinding.bind(view)
                renderText(itemViewBinding.itemTv, data)
                itemViewBinding.root.clicks(this) {
                    itemClicks(window, data)
                }
            }
        ).build()
        window.contentView = viewBinding.root
        window.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        window.isFocusable = true
        window.isTouchable = true
        PopupWindowCompat.setOverlapAnchor(window, false)
        return window
    }
}