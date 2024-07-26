package com.tans.tasciiartplayer.ui.videoplayer

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tans.tasciiartplayer.R
import com.tans.tasciiartplayer.databinding.VideoMediaInfoDialogBinding
import com.tans.tasciiartplayer.databinding.VideoMediaInfoItemLayoutBinding
import com.tans.tasciiartplayer.databinding.VideoMediaInfoTitleLayoutBinding
import com.tans.tasciiartplayer.toSizeString
import com.tans.tmediaplayer.player.model.AudioStreamInfo
import com.tans.tmediaplayer.player.model.MediaInfo
import com.tans.tmediaplayer.player.model.SubtitleStreamInfo
import com.tans.tmediaplayer.player.model.VideoStreamInfo
import com.tans.tuiutils.adapter.impl.builders.SimpleAdapterBuilderImpl
import com.tans.tuiutils.adapter.impl.builders.plus
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

    override fun bindContentView(view: View) {
        val mediaInfo = this.mediaInfo ?: return
        val filePath = this.filePath ?: return
        val viewBinding = VideoMediaInfoDialogBinding.bind(view)
        val ctx = requireContext()

        // File
        var adapterBuilder = createTitleAdapterBuilder(ctx.getString(R.string.media_info_dialog_file_title)) + createKeyValueAdapterBuilder(mediaInfo.getFileInfoStrings(ctx, filePath))

        // Video Stream
        val videoStreamInfo = mediaInfo.videoStreamInfo
        if (videoStreamInfo != null) {
            adapterBuilder += createLineAdapterBuilder()
            adapterBuilder += createTitleAdapterBuilder(ctx.getString(R.string.media_info_dialog_video_title))
            adapterBuilder += createKeyValueAdapterBuilder(videoStreamInfo.getVideoStreamInfoStrings(ctx))
        }

        // Audio Stream
        val audioStreamInfo = mediaInfo.audioStreamInfo
        if (audioStreamInfo != null) {
            adapterBuilder += createLineAdapterBuilder()
            adapterBuilder += createTitleAdapterBuilder(ctx.getString(R.string.media_info_dialog_audio_title))
            adapterBuilder += createKeyValueAdapterBuilder(audioStreamInfo.getAudioStreamInfoStrings(ctx))
        }

        // Subtitle streams
        val subtitleStreams = mediaInfo.subtitleStreams
        if (subtitleStreams.isNotEmpty()) {
            adapterBuilder += createLineAdapterBuilder()
            adapterBuilder += createTitleAdapterBuilder(ctx.getString(R.string.media_info_dialog_subtitle_title))
            adapterBuilder += createKeyValueAdapterBuilder(subtitleStreams.getSubtitlesStreamInfoStrings(ctx))
        }
        viewBinding.mediaInfoRv.adapter = adapterBuilder.build()
    }

    private fun createTitleAdapterBuilder(title: String): SimpleAdapterBuilderImpl<*> {
        return SimpleAdapterBuilderImpl<String>(
            itemViewCreator = SingleItemViewCreatorImpl(R.layout.video_media_info_title_layout),
            dataSource = FlowDataSourceImpl<String>(flow {
                emit(listOf(title))
            }),
            dataBinder = DataBinderImpl { data, itemView, _ ->
                val itemViewBinding = VideoMediaInfoTitleLayoutBinding.bind(itemView)
                itemViewBinding.titleTv.text = data
            }
        )
    }

    private fun createLineAdapterBuilder(): SimpleAdapterBuilderImpl<*> {
        return SimpleAdapterBuilderImpl<Unit>(
            itemViewCreator = SingleItemViewCreatorImpl(R.layout.video_media_info_line_layout),
            dataSource = FlowDataSourceImpl<Unit>(flow {
                emit(listOf(Unit))
            }),
            dataBinder = DataBinderImpl { _, _, _ -> }
        )
    }

    private fun createKeyValueAdapterBuilder(keyAndValues: List<String>): SimpleAdapterBuilderImpl<*> {
        return SimpleAdapterBuilderImpl<String>(
            itemViewCreator = SingleItemViewCreatorImpl(R.layout.video_media_info_item_layout),
            dataSource = FlowDataSourceImpl<String>(flow {
                emit(keyAndValues)
            }),
            dataBinder = DataBinderImpl { data, itemView, _ ->
                val itemViewBinding = VideoMediaInfoItemLayoutBinding.bind(itemView)
                itemViewBinding.keyValueTv.text = data
            }
        )
    }
}

fun MediaInfo.getFileInfoStrings(ctx: Context, filePath: String): List<String> {
    val fileKeyValue = mutableListOf<String>()
    fileKeyValue.add(ctx.getString(R.string.media_info_dialog_file_path, filePath))
    val fileSizeStr = File(filePath).length().toSizeString()
    fileKeyValue.add(ctx.getString(R.string.media_info_dialog_file_size, fileSizeStr))
    fileKeyValue.add(ctx.getString(R.string.media_info_dialog_file_format, containerName))
    if (metadata.isNotEmpty()) {
        fileKeyValue.add("")
        fileKeyValue.add(ctx.getString(R.string.media_info_dialog_metadata))
        for ((key, value) in metadata) {
            fileKeyValue.add(" $key: $value")
        }
    }
    return fileKeyValue
}

fun VideoStreamInfo.getVideoStreamInfoStrings(ctx: Context): List<String> {
    val videoKeyValue = mutableListOf<String>()
    videoKeyValue.add(ctx.getString(R.string.media_info_dialog_decoder, videoDecoderName))
    videoKeyValue.add(ctx.getString(R.string.media_info_dialog_codec, videoCodec.toString()))
    videoKeyValue.add(ctx.getString(R.string.media_info_dialog_resolution, "${videoWidth}x${videoHeight}"))
    videoKeyValue.add(ctx.getString(R.string.media_info_dialog_fps, videoFps))
    if (videoBitrate > 0) {
        videoKeyValue.add(ctx.getString(R.string.media_info_dialog_bitrate, videoBitrate / 1024))
    }
    if (videoPixelBitDepth > 0) {
        videoKeyValue.add(ctx.getString(R.string.media_info_dialog_pixel_depth, videoPixelBitDepth))
    }
    videoKeyValue.add(ctx.getString(R.string.media_info_dialog_pixel_format, videoPixelFormat.name))

    if (videoStreamMetadata.isNotEmpty()) {
        videoKeyValue.add("")
        videoKeyValue.add(ctx.getString(R.string.media_info_dialog_metadata))
        for ((key, value) in videoStreamMetadata) {
            videoKeyValue.add(" $key: $value")
        }
    }
    return videoKeyValue
}

fun AudioStreamInfo.getAudioStreamInfoStrings(ctx: Context): List<String> {
    val audioKeyValue = mutableListOf<String>()
    audioKeyValue.add(ctx.getString(R.string.media_info_dialog_decoder, audioDecoderName))
    audioKeyValue.add(ctx.getString(R.string.media_info_dialog_codec, audioCodec.toString()))
    audioKeyValue.add(ctx.getString(R.string.media_info_dialog_channels, audioChannels))
    audioKeyValue.add(ctx.getString(R.string.media_info_dialog_simple_rate, audioSimpleRate))
    if (audioBitrate > 0) {
        audioKeyValue.add(ctx.getString(R.string.media_info_dialog_bitrate, audioBitrate / 1024))
    }
    if (audioSampleBitDepth > 0) {
        audioKeyValue.add(ctx.getString(R.string.media_info_dialog_simple_depth, audioSampleBitDepth))
    }
    audioKeyValue.add(ctx.getString(R.string.media_info_dialog_simple_format, audioSampleFormat.name))

    if (audioStreamMetadata.isNotEmpty()) {
        audioKeyValue.add("")
        audioKeyValue.add(ctx.getString(R.string.media_info_dialog_metadata))
        for ((key, value) in audioStreamMetadata) {
            audioKeyValue.add(" $key: $value")
        }
    }
    return audioKeyValue
}

fun List<SubtitleStreamInfo>.getSubtitlesStreamInfoStrings(ctx: Context): List<String> {
    val subtitlesKeyValue = mutableListOf<String>()
    for (subtitle in this) {
        for ((key, value) in subtitle.metadata) {
            subtitlesKeyValue.add("$key: $value")
        }
        subtitlesKeyValue.add("")
    }
    return subtitlesKeyValue
}