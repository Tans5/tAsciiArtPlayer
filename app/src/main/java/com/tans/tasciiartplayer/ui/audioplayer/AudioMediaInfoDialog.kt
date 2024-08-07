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
import com.tans.tasciiartplayer.ui.videoplayer.getAudioStreamInfoStrings
import com.tans.tasciiartplayer.ui.videoplayer.getFileInfoStrings
import com.tans.tasciiartplayer.ui.videoplayer.getVideoStreamInfoStrings
import com.tans.tuiutils.adapter.AdapterBuilder
import com.tans.tuiutils.adapter.impl.builders.CombinedAdapterBuilderImpl
import com.tans.tuiutils.adapter.impl.builders.SimpleAdapterBuilderImpl
import com.tans.tuiutils.adapter.impl.builders.plus
import com.tans.tuiutils.adapter.impl.databinders.DataBinderImpl
import com.tans.tuiutils.adapter.impl.datasources.DataSourceImpl
import com.tans.tuiutils.adapter.impl.viewcreatators.SingleItemViewCreatorImpl
import com.tans.tuiutils.dialog.BaseCoroutineStateDialogFragment
import com.tans.tuiutils.dialog.createBottomSheetDialog
import com.tans.tuiutils.systembar.SystemBarThemeStyle

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
            val dataSourceRunnable = mutableListOf<Runnable>()
            // File
            var adapterBuilder = createTitleAdapterBuilder(ctx.getString(R.string.media_info_dialog_file_title)).let { dataSourceRunnable.add(it.second);it.first }+ createKeyValueAdapterBuilder(audioMediaInfo.getFileInfoStrings(ctx, audioFile.canonicalPath)).let { dataSourceRunnable.add(it.second);it.first }

            // Video Stream
            val videoStreamInfo = audioMediaInfo.videoStreamInfo
            if (videoStreamInfo != null) {
                adapterBuilder = createLineAdapterBuilder().combineAdapterBuilder(adapterBuilder, dataSourceRunnable)
                adapterBuilder = createTitleAdapterBuilder(ctx.getString(R.string.media_info_dialog_video_title)).combineAdapterBuilder(adapterBuilder, dataSourceRunnable)
                adapterBuilder = createKeyValueAdapterBuilder(videoStreamInfo.getVideoStreamInfoStrings(ctx)).combineAdapterBuilder(adapterBuilder, dataSourceRunnable)
            }

            // Audio Stream
            val audioStreamInfo = audioMediaInfo.audioStreamInfo
            if (audioStreamInfo != null) {
                adapterBuilder = createLineAdapterBuilder().combineAdapterBuilder(adapterBuilder, dataSourceRunnable)
                adapterBuilder = createTitleAdapterBuilder(ctx.getString(R.string.media_info_dialog_audio_title)).combineAdapterBuilder(adapterBuilder, dataSourceRunnable)
                adapterBuilder = createKeyValueAdapterBuilder(audioStreamInfo.getAudioStreamInfoStrings(ctx)).combineAdapterBuilder(adapterBuilder, dataSourceRunnable)
            }
            viewBinding.mediaInfoRv.adapter = adapterBuilder.build()
            for (r in dataSourceRunnable) {
                r.run()
            }
        }
    }

    private fun createTitleAdapterBuilder(title: String): Pair<AdapterBuilder<*>, Runnable> {
        val dataSource = DataSourceImpl<String>()
        val dataSourceUpdater = Runnable {
            dataSource.submitDataList(listOf(title))
        }
        return SimpleAdapterBuilderImpl(
            itemViewCreator = SingleItemViewCreatorImpl(R.layout.audio_media_info_title_layout),
            dataSource = dataSource,
            dataBinder = DataBinderImpl { data, itemView, _ ->
                val itemViewBinding = AudioMediaInfoTitleLayoutBinding.bind(itemView)
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
            itemViewCreator = SingleItemViewCreatorImpl(R.layout.audio_media_info_line_layout),
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
            itemViewCreator = SingleItemViewCreatorImpl(R.layout.audio_media_info_item_layout),
            dataSource = dataSource,
            dataBinder = DataBinderImpl { data, itemView, _ ->
                val itemViewBinding = AudioMediaInfoItemLayoutBinding.bind(itemView)
                itemViewBinding.keyValueTv.text = data
            }
        ) to dataSourceUpdater
    }

    private fun Pair<AdapterBuilder<*>, Runnable>.combineAdapterBuilder(combined: CombinedAdapterBuilderImpl, updaters: MutableList<Runnable>): CombinedAdapterBuilderImpl {
        updaters.add(this.second)
        return combined + first
    }
}