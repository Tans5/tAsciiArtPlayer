package com.tans.tasciiartplayer.ui.audioplayer

import android.app.Dialog
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.shape.MaterialShapeDrawable
import com.tans.tasciiartplayer.R
import com.tans.tasciiartplayer.audio.audioplayer.AudioPlayerManager
import com.tans.tasciiartplayer.audio.audioplayer.PlayListState
import com.tans.tasciiartplayer.audio.audioplayer.getCurrentPlayAudio
import com.tans.tasciiartplayer.databinding.AudioMediaInfoDialogBinding
import com.tans.tasciiartplayer.databinding.AudioMediaInfoItemLayoutBinding
import com.tans.tasciiartplayer.databinding.AudioMediaInfoTitleLayoutBinding
import com.tans.tasciiartplayer.toSizeString
import com.tans.tuiutils.adapter.impl.builders.SimpleAdapterBuilderImpl
import com.tans.tuiutils.adapter.impl.builders.plus
import com.tans.tuiutils.adapter.impl.databinders.DataBinderImpl
import com.tans.tuiutils.adapter.impl.datasources.FlowDataSourceImpl
import com.tans.tuiutils.adapter.impl.viewcreatators.SingleItemViewCreatorImpl
import com.tans.tuiutils.dialog.BaseCoroutineStateDialogFragment
import com.tans.tuiutils.dialog.createBottomSheetDialog
import com.tans.tuiutils.systembar.SystemBarThemeStyle
import kotlinx.coroutines.flow.flow

class AudioMediaInfoDialog : BaseCoroutineStateDialogFragment<Unit>(Unit) {

    override val contentViewHeightInScreenRatio: Float = 0.55f

    override fun createContentView(context: Context, parent: ViewGroup): View {
        return LayoutInflater.from(context).inflate(R.layout.audio_media_info_dialog, parent, false)
    }

    override fun createDialog(contentView: View): Dialog {
        return requireActivity().createBottomSheetDialog(
            contentView = contentView,
            navigationThemeStyle = SystemBarThemeStyle.Light,
            statusBarThemeStyle = SystemBarThemeStyle.Light,
            dimAmount = 0.05f
        ) { b ->
            b.isDraggable = true
            b.isHideable = true
            b.isFitToContents = true
            b.state = BottomSheetBehavior.STATE_EXPANDED
            b.setPeekHeight(0, false)
            b.addBottomSheetCallback(object : BottomSheetBehavior.BottomSheetCallback() {
                override fun onStateChanged(bottomSheet: View, newState: Int) {
                    if (newState == BottomSheetBehavior.STATE_COLLAPSED) {
                        b.state = BottomSheetBehavior.STATE_HIDDEN
                    }
                }

                override fun onSlide(bottomSheet: View, slideOffset: Float) {
                }
            })
            try {
                val method = BottomSheetBehavior::class.java.getDeclaredMethod("getMaterialShapeDrawable")
                method.isAccessible = true
                val d = method.invoke(b) as MaterialShapeDrawable
                d.fillColor = ColorStateList.valueOf(Color.WHITE)
            } catch (e: Throwable) {
                e.printStackTrace()
            }
        }
    }

    override fun firstLaunchInitData() {

    }

    override fun bindContentView(view: View) {
        val viewBinding = AudioMediaInfoDialogBinding.bind(view)
        val selectedPlayList = (AudioPlayerManager.stateFlow.value.playListState as? PlayListState.SelectedPlayList)
        val audioFile = selectedPlayList?.getCurrentPlayAudio()?.mediaStoreAudio?.file
        val audioMediaInfo = selectedPlayList?.playerMediaInfo
        if (audioFile == null || audioMediaInfo == null) {
            dismissSafe()
        } else {
            val ctx = requireContext()
            // File
            val fileKeyValue = mutableListOf<String>()
            fileKeyValue.add(ctx.getString(R.string.media_info_dialog_file_path, audioFile.canonicalPath))
            val fileSizeStr = audioFile.length().toSizeString()
            fileKeyValue.add(ctx.getString(R.string.media_info_dialog_file_size, fileSizeStr))
            fileKeyValue.add(ctx.getString(R.string.media_info_dialog_file_format, audioMediaInfo.containerName))
            if (audioMediaInfo.metadata.isNotEmpty()) {
                fileKeyValue.add("")
                fileKeyValue.add(ctx.getString(R.string.media_info_dialog_metadata))
                for ((key, value) in audioMediaInfo.metadata) {
                    fileKeyValue.add(" $key: $value")
                }
            }
            var adapterBuilder = createTitleAdapterBuilder(ctx.getString(R.string.media_info_dialog_file_title)) + createKeyValueAdapterBuilder(fileKeyValue)


            // Video Stream
            val videoStreamInfo = audioMediaInfo.videoStreamInfo
            if (videoStreamInfo != null) {
                val videoKeyValue = mutableListOf<String>()
                videoKeyValue.add(ctx.getString(R.string.media_info_dialog_decoder, videoStreamInfo.videoDecoderName))
                videoKeyValue.add(ctx.getString(R.string.media_info_dialog_codec, videoStreamInfo.videoCodec.toString()))
                videoKeyValue.add(ctx.getString(R.string.media_info_dialog_resolution, "${videoStreamInfo.videoWidth}x${videoStreamInfo.videoHeight}"))
                videoKeyValue.add(ctx.getString(R.string.media_info_dialog_fps, videoStreamInfo.videoFps))
                if (videoStreamInfo.videoBitrate > 0) {
                    videoKeyValue.add(ctx.getString(R.string.media_info_dialog_bitrate, videoStreamInfo.videoBitrate / 1024))
                }
                if (videoStreamInfo.videoPixelBitDepth > 0) {
                    videoKeyValue.add(ctx.getString(R.string.media_info_dialog_pixel_depth, videoStreamInfo.videoPixelBitDepth))
                }
                videoKeyValue.add(ctx.getString(R.string.media_info_dialog_pixel_format, videoStreamInfo.videoPixelFormat.name))

                if (videoStreamInfo.videoStreamMetadata.isNotEmpty()) {
                    videoKeyValue.add("")
                    videoKeyValue.add(ctx.getString(R.string.media_info_dialog_metadata))
                    for ((key, value) in videoStreamInfo.videoStreamMetadata) {
                        videoKeyValue.add(" $key: $value")
                    }
                }
                adapterBuilder += createLineAdapterBuilder()
                adapterBuilder += createTitleAdapterBuilder(ctx.getString(R.string.media_info_dialog_video_title))
                adapterBuilder += createKeyValueAdapterBuilder(videoKeyValue)
            }

            // Audio Stream
            val audioStreamInfo = audioMediaInfo.audioStreamInfo
            if (audioStreamInfo != null) {
                val audioKeyValue = mutableListOf<String>()
                audioKeyValue.add(ctx.getString(R.string.media_info_dialog_decoder, audioStreamInfo.audioDecoderName))
                audioKeyValue.add(ctx.getString(R.string.media_info_dialog_codec, audioStreamInfo.audioCodec.toString()))
                audioKeyValue.add(ctx.getString(R.string.media_info_dialog_channels, audioStreamInfo.audioChannels))
                audioKeyValue.add(ctx.getString(R.string.media_info_dialog_simple_rate, audioStreamInfo.audioSimpleRate))
                if (audioStreamInfo.audioBitrate > 0) {
                    audioKeyValue.add(ctx.getString(R.string.media_info_dialog_bitrate, audioStreamInfo.audioBitrate / 1024))
                }
                if (audioStreamInfo.audioSampleBitDepth > 0) {
                    audioKeyValue.add(ctx.getString(R.string.media_info_dialog_simple_depth, audioStreamInfo.audioSampleBitDepth))
                }
                audioKeyValue.add(ctx.getString(R.string.media_info_dialog_simple_format, audioStreamInfo.audioSampleFormat.name))

                if (audioStreamInfo.audioStreamMetadata.isNotEmpty()) {
                    audioKeyValue.add("")
                    audioKeyValue.add(ctx.getString(R.string.media_info_dialog_metadata))
                    for ((key, value) in audioStreamInfo.audioStreamMetadata) {
                        audioKeyValue.add(" $key: $value")
                    }
                }
                adapterBuilder += createLineAdapterBuilder()
                adapterBuilder += createTitleAdapterBuilder(ctx.getString(R.string.media_info_dialog_audio_title))
                adapterBuilder += createKeyValueAdapterBuilder(audioKeyValue)
            }
            viewBinding.mediaInfoRv.adapter = adapterBuilder.build()
        }
    }

    private fun createTitleAdapterBuilder(title: String): SimpleAdapterBuilderImpl<*> {
        return SimpleAdapterBuilderImpl<String>(
            itemViewCreator = SingleItemViewCreatorImpl(R.layout.audio_media_info_title_layout),
            dataSource = FlowDataSourceImpl<String>(flow {
                emit(listOf(title))
            }),
            dataBinder = DataBinderImpl { data, itemView, _ ->
                val itemViewBinding = AudioMediaInfoTitleLayoutBinding.bind(itemView)
                itemViewBinding.titleTv.text = data
            }
        )
    }

    private fun createLineAdapterBuilder(): SimpleAdapterBuilderImpl<*> {
        return SimpleAdapterBuilderImpl<Unit>(
            itemViewCreator = SingleItemViewCreatorImpl(R.layout.audio_media_info_line_layout),
            dataSource = FlowDataSourceImpl<Unit>(flow {
                emit(listOf(Unit))
            }),
            dataBinder = DataBinderImpl { _, _, _ -> }
        )
    }

    private fun createKeyValueAdapterBuilder(keyAndValues: List<String>): SimpleAdapterBuilderImpl<*> {
        return SimpleAdapterBuilderImpl<String>(
            itemViewCreator = SingleItemViewCreatorImpl(R.layout.audio_media_info_item_layout),
            dataSource = FlowDataSourceImpl<String>(flow {
                emit(keyAndValues)
            }),
            dataBinder = DataBinderImpl { data, itemView, _ ->
                val itemViewBinding = AudioMediaInfoItemLayoutBinding.bind(itemView)
                itemViewBinding.keyValueTv.text = data
            }
        )
    }
}