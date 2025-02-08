package com.tans.tasciiartplayer.ui.videoplayer

import android.app.Dialog
import android.content.Context
import android.view.View
import com.tans.tasciiartplayer.R
import com.tans.tasciiartplayer.databinding.VideoMediaInfoDialogBinding
import com.tans.tasciiartplayer.databinding.VideoMediaInfoItemLayoutBinding
import com.tans.tasciiartplayer.databinding.VideoMediaInfoTitleLayoutBinding
import com.tans.tasciiartplayer.toSizeString
import com.tans.tmediaplayer.player.model.AudioStreamInfo
import com.tans.tmediaplayer.player.model.MediaInfo
import com.tans.tmediaplayer.player.model.SubtitleStreamInfo
import com.tans.tmediaplayer.player.model.VideoStreamInfo
import com.tans.tuiutils.adapter.AdapterBuilder
import com.tans.tuiutils.adapter.impl.builders.CombinedAdapterBuilderImpl
import com.tans.tuiutils.adapter.impl.builders.SimpleAdapterBuilderImpl
import com.tans.tuiutils.adapter.impl.builders.plus
import com.tans.tuiutils.adapter.impl.databinders.DataBinderImpl
import com.tans.tuiutils.adapter.impl.datasources.DataSourceImpl
import com.tans.tuiutils.adapter.impl.viewcreatators.SingleItemViewCreatorImpl
import com.tans.tuiutils.dialog.BaseCoroutineStateDialogFragment
import com.tans.tuiutils.dialog.createDefaultDialog
import java.io.File

class VideoMediaInfoDialog : BaseCoroutineStateDialogFragment<Unit> {

    private val mediaInfo: MediaInfo?

    constructor() : super(Unit) {
        this.mediaInfo = null
    }

    constructor(mediaInfo: MediaInfo) : super(Unit) {
        this.mediaInfo = mediaInfo
    }

    override val contentViewWidthInScreenRatio: Float = 0.5f

    override val layoutId: Int = R.layout.video_media_info_dialog

    override fun createDialog(contentView: View): Dialog {
        return requireActivity().createDefaultDialog(contentView = contentView, dimAmount = 0.0f)
    }

    override fun firstLaunchInitData() {

    }

    override fun bindContentView(view: View) {
        val mediaInfo = this.mediaInfo ?: return
        val viewBinding = VideoMediaInfoDialogBinding.bind(view)
        val ctx = requireContext()

        val dataSourceRunnable = mutableListOf<Runnable>()
        // File
        var adapterBuilder = createTitleAdapterBuilder(ctx.getString(R.string.media_info_dialog_file_title)).let { dataSourceRunnable.add(it.second);it.first }+ createKeyValueAdapterBuilder(mediaInfo.getFileInfoStrings(ctx)).let { dataSourceRunnable.add(it.second);it.first }

        // Video Stream
        val videoStreamInfo = mediaInfo.videoStreamInfo
        if (videoStreamInfo != null) {
            adapterBuilder = createLineAdapterBuilder().combineAdapterBuilder(adapterBuilder, dataSourceRunnable)
            adapterBuilder = createTitleAdapterBuilder(ctx.getString(R.string.media_info_dialog_video_title)).combineAdapterBuilder(adapterBuilder, dataSourceRunnable)
            adapterBuilder = createKeyValueAdapterBuilder(videoStreamInfo.getVideoStreamInfoStrings(ctx)).combineAdapterBuilder(adapterBuilder, dataSourceRunnable)
        }

        // Audio Stream
        val audioStreamInfo = mediaInfo.audioStreamInfo
        if (audioStreamInfo != null) {
            adapterBuilder = createLineAdapterBuilder().combineAdapterBuilder(adapterBuilder, dataSourceRunnable)
            adapterBuilder = createTitleAdapterBuilder(ctx.getString(R.string.media_info_dialog_audio_title)).combineAdapterBuilder(adapterBuilder, dataSourceRunnable)
            adapterBuilder = createKeyValueAdapterBuilder(audioStreamInfo.getAudioStreamInfoStrings(ctx)).combineAdapterBuilder(adapterBuilder, dataSourceRunnable)
        }

        // Subtitle streams
        val subtitleStreams = mediaInfo.subtitleStreams
        if (subtitleStreams.isNotEmpty()) {
            adapterBuilder = createLineAdapterBuilder().combineAdapterBuilder(adapterBuilder, dataSourceRunnable)
            adapterBuilder = createTitleAdapterBuilder(ctx.getString(R.string.media_info_dialog_subtitle_title)).combineAdapterBuilder(adapterBuilder, dataSourceRunnable)
            adapterBuilder = createKeyValueAdapterBuilder(subtitleStreams.getSubtitlesStreamInfoStrings()).combineAdapterBuilder(adapterBuilder, dataSourceRunnable)
        }
        viewBinding.mediaInfoRv.adapter = adapterBuilder.build()
        for (r in dataSourceRunnable) {
            r.run()
        }
    }

    private fun createTitleAdapterBuilder(title: String): Pair<AdapterBuilder<*>, Runnable> {
        val dataSource = DataSourceImpl<String>()
        val dataSourceUpdater = Runnable {
            dataSource.submitDataList(listOf(title))
        }
        return SimpleAdapterBuilderImpl(
            itemViewCreator = SingleItemViewCreatorImpl(R.layout.video_media_info_item_layout),
            dataSource = dataSource,
            dataBinder = DataBinderImpl { data, itemView, _ ->
                val itemViewBinding = VideoMediaInfoTitleLayoutBinding.bind(itemView)
                itemViewBinding.titleTv.text = data
            }
        ) to dataSourceUpdater
    }

    private fun createLineAdapterBuilder(): Pair<AdapterBuilder<*>, Runnable> {
        val dataSource = DataSourceImpl<Unit>()
        val dataSourceUpdater = Runnable {
            dataSource.submitDataList(listOf(Unit))
        }
        return SimpleAdapterBuilderImpl(
            itemViewCreator = SingleItemViewCreatorImpl(R.layout.video_media_info_line_layout),
            dataSource = dataSource,
            dataBinder = DataBinderImpl { _, _, _ -> }
        ) to dataSourceUpdater
    }

    private fun createKeyValueAdapterBuilder(keyAndValues: List<String>): Pair<AdapterBuilder<*>, Runnable> {
        val dataSource = DataSourceImpl<String>()
        val dataSourceUpdater = Runnable {
            dataSource.submitDataList(keyAndValues)
        }
        return SimpleAdapterBuilderImpl(
            itemViewCreator = SingleItemViewCreatorImpl(R.layout.video_media_info_item_layout),
            dataSource = dataSource,
            dataBinder = DataBinderImpl { data, itemView, _ ->
                val itemViewBinding = VideoMediaInfoItemLayoutBinding.bind(itemView)
                itemViewBinding.keyValueTv.text = data
            }
        ) to dataSourceUpdater
    }

    private fun Pair<AdapterBuilder<*>, Runnable>.combineAdapterBuilder(combined: CombinedAdapterBuilderImpl, updaters: MutableList<Runnable>): CombinedAdapterBuilderImpl {
        updaters.add(this.second)
        return combined + first
    }
}

fun MediaInfo.getFileInfoStrings(ctx: Context): List<String> {
    val fileKeyValue = mutableListOf<String>()
    fileKeyValue.add(ctx.getString(R.string.media_info_dialog_file_path, file))
    val f = File(file)
    if (f.isFile && f.canRead()) {
        fileKeyValue.add(ctx.getString(R.string.media_info_dialog_file_size, f.length().toSizeString()))
    }
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

fun List<SubtitleStreamInfo>.getSubtitlesStreamInfoStrings(): List<String> {
    val subtitlesKeyValue = mutableListOf<String>()
    for (subtitle in this) {
        for ((key, value) in subtitle.metadata) {
            subtitlesKeyValue.add("$key: $value")
        }
        subtitlesKeyValue.add("")
    }
    return subtitlesKeyValue
}