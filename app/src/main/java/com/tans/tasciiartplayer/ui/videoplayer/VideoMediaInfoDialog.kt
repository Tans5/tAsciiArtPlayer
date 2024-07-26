package com.tans.tasciiartplayer.ui.videoplayer

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tans.tasciiartplayer.R
import com.tans.tasciiartplayer.databinding.VideoMediaInfoDialogBinding
import com.tans.tasciiartplayer.databinding.VideoMediaInfoItemLayoutBinding
import com.tans.tasciiartplayer.toSizeString
import com.tans.tmediaplayer.player.model.MediaInfo
import com.tans.tuiutils.adapter.impl.builders.SimpleAdapterBuilderImpl
import com.tans.tuiutils.adapter.impl.databinders.DataBinderImpl
import com.tans.tuiutils.adapter.impl.datasources.FlowDataSourceImpl
import com.tans.tuiutils.adapter.impl.viewcreatators.SingleItemViewCreatorImpl
import com.tans.tuiutils.dialog.BaseCoroutineStateDialogFragment
import com.tans.tuiutils.dialog.createDefaultDialog
import kotlinx.coroutines.flow.flow
import java.io.File

class VideoMediaInfoDialog : BaseCoroutineStateDialogFragment<Unit> {

    private val mediaInfo: MediaInfo?

    private val filePath: String?

    constructor() : super(Unit) {
        this.mediaInfo = null
        this.filePath = null
    }

    constructor(mediaInfo: MediaInfo, filePath: String) : super(Unit) {
        this.mediaInfo = mediaInfo
        this.filePath = filePath
    }

    override val contentViewWidthInScreenRatio: Float = 0.5f

    override fun createContentView(context: Context, parent: ViewGroup): View {
       return LayoutInflater.from(context).inflate(R.layout.video_media_info_dialog, parent, false)
    }

    override fun createDialog(contentView: View): Dialog {
        return requireActivity().createDefaultDialog(contentView = contentView, dimAmount = 0.0f)
    }

    override fun firstLaunchInitData() {

    }

    @SuppressLint("ClickableViewAccessibility")
    override fun bindContentView(view: View) {
        val mediaInfo = this.mediaInfo ?: return
        val filePath = this.filePath ?: return
        val viewBinding = VideoMediaInfoDialogBinding.bind(view)
        val ctx = requireContext()

        viewBinding.fileRv.adapter = SimpleAdapterBuilderImpl<String>(
            itemViewCreator = SingleItemViewCreatorImpl(R.layout.video_media_info_item_layout),
            dataSource = FlowDataSourceImpl(flow {
                val result = mutableListOf<String>()
                result.add(ctx.getString(R.string.media_info_dialog_file_path, filePath))
                val fileSizeStr = File(filePath).length().toSizeString()
                result.add(ctx.getString(R.string.media_info_dialog_file_size, fileSizeStr))
                result.add(ctx.getString(R.string.media_info_dialog_file_format, mediaInfo.containerName))
                if (mediaInfo.metadata.isNotEmpty()) {
                    result.add("")
                    result.add(ctx.getString(R.string.media_info_dialog_metadata))
                    for ((key, value) in mediaInfo.metadata) {
                        result.add(" $key: $value")
                    }
                }
                emit(result)
            }),
            dataBinder = DataBinderImpl { data, itemView, _ ->
                val itemViewBinding = VideoMediaInfoItemLayoutBinding.bind(itemView)
                itemViewBinding.keyValueTv.text = data
            }
        ).build()
        viewBinding.fileRv.setOnTouchListener { _, _ -> true }

        val videoStreamInfo = mediaInfo.videoStreamInfo
        if (videoStreamInfo != null) {
            viewBinding.videoGroup.visibility = View.VISIBLE
            viewBinding.videoRv.adapter = SimpleAdapterBuilderImpl<String>(
                itemViewCreator = SingleItemViewCreatorImpl(R.layout.video_media_info_item_layout),
                dataSource = FlowDataSourceImpl(flow {
                    val result = mutableListOf<String>()
                    result.add(ctx.getString(R.string.media_info_dialog_decoder, videoStreamInfo.videoDecoderName))
                    result.add(ctx.getString(R.string.media_info_dialog_codec, videoStreamInfo.videoCodec.toString()))
                    result.add(ctx.getString(R.string.media_info_dialog_resolution, "${videoStreamInfo.videoWidth}x${videoStreamInfo.videoHeight}"))
                    result.add(ctx.getString(R.string.media_info_dialog_fps, videoStreamInfo.videoFps))
                    if (videoStreamInfo.videoBitrate > 0) {
                        result.add(ctx.getString(R.string.media_info_dialog_bitrate, videoStreamInfo.videoBitrate / 1024))
                    }
                    if (videoStreamInfo.videoPixelBitDepth > 0) {
                        result.add(ctx.getString(R.string.media_info_dialog_pixel_depth, videoStreamInfo.videoPixelBitDepth))
                    }
                    result.add(ctx.getString(R.string.media_info_dialog_pixel_format, videoStreamInfo.videoPixelFormat.name))

                    if (videoStreamInfo.videoStreamMetadata.isNotEmpty()) {
                        result.add("")
                        result.add(ctx.getString(R.string.media_info_dialog_metadata))
                        for ((key, value) in videoStreamInfo.videoStreamMetadata) {
                            result.add(" $key: $value")
                        }
                    }
                    emit(result)
                }),
                dataBinder = DataBinderImpl { data, itemView, _ ->
                    val itemViewBinding = VideoMediaInfoItemLayoutBinding.bind(itemView)
                    itemViewBinding.keyValueTv.text = data
                }
            ).build()
        } else {
            viewBinding.videoGroup.visibility = View.GONE
        }
        viewBinding.videoRv.setOnTouchListener { _, _ -> true }

        val audioStreamInfo = mediaInfo.audioStreamInfo
        if (audioStreamInfo != null) {
            viewBinding.audioGroup.visibility = View.VISIBLE
            viewBinding.audioRv.adapter = SimpleAdapterBuilderImpl<String>(
                itemViewCreator = SingleItemViewCreatorImpl(R.layout.video_media_info_item_layout),
                dataSource = FlowDataSourceImpl(flow {
                    val result = mutableListOf<String>()
                    result.add(ctx.getString(R.string.media_info_dialog_decoder, audioStreamInfo.audioDecoderName))
                    result.add(ctx.getString(R.string.media_info_dialog_codec, audioStreamInfo.audioCodec.toString()))
                    result.add(ctx.getString(R.string.media_info_dialog_channels, audioStreamInfo.audioChannels))
                    result.add(ctx.getString(R.string.media_info_dialog_simple_rate, audioStreamInfo.audioSimpleRate))
                    if (audioStreamInfo.audioBitrate > 0) {
                        result.add(ctx.getString(R.string.media_info_dialog_bitrate, audioStreamInfo.audioBitrate / 1024))
                    }
                    if (audioStreamInfo.audioSampleBitDepth > 0) {
                        result.add(ctx.getString(R.string.media_info_dialog_simple_depth, audioStreamInfo.audioSampleBitDepth))
                    }
                    result.add(ctx.getString(R.string.media_info_dialog_simple_format, audioStreamInfo.audioSampleFormat.name))

                    if (audioStreamInfo.audioStreamMetadata.isNotEmpty()) {
                        result.add("")
                        result.add(ctx.getString(R.string.media_info_dialog_metadata))
                        for ((key, value) in audioStreamInfo.audioStreamMetadata) {
                            result.add(" $key: $value")
                        }
                    }
                    emit(result)
                }),
                dataBinder = DataBinderImpl { data, itemView, _ ->
                    val itemViewBinding = VideoMediaInfoItemLayoutBinding.bind(itemView)
                    itemViewBinding.keyValueTv.text = data
                }
            ).build()
        } else {
            viewBinding.audioGroup.visibility = View.GONE
        }
        viewBinding.audioRv.setOnTouchListener { _, _ -> true }

        val subtitleStreams = mediaInfo.subtitleStreams
        if (subtitleStreams.isNotEmpty()) {
            viewBinding.subtitleGroup.visibility = View.VISIBLE
            viewBinding.subtitleRv.adapter = SimpleAdapterBuilderImpl<String>(
                itemViewCreator = SingleItemViewCreatorImpl(R.layout.video_media_info_item_layout),
                dataSource = FlowDataSourceImpl(flow {
                    val result = mutableListOf<String>()
                    for (subtitle in mediaInfo.subtitleStreams) {
                        for ((key, value) in subtitle.metadata) {
                            result.add("$key: $value")
                        }
                        result.add("")
                    }
                    emit(result)
                }),
                dataBinder = DataBinderImpl { data, itemView, _ ->
                    val itemViewBinding = VideoMediaInfoItemLayoutBinding.bind(itemView)
                    itemViewBinding.keyValueTv.text = data
                }
            ).build()
        } else {
            viewBinding.subtitleGroup.visibility = View.GONE
        }
        viewBinding.subtitleRv.setOnTouchListener { _, _ -> true }
    }


}