package com.tans.tasciiartplayer.ui.audioplayer

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.tans.tasciiartplayer.R
import com.tans.tasciiartplayer.appGlobalCoroutineScope
import com.tans.tasciiartplayer.audio.AudioListType
import com.tans.tasciiartplayer.audio.AudioManager
import com.tans.tasciiartplayer.audio.AudioModel
import com.tans.tasciiartplayer.audio.AudioPlayerManager
import com.tans.tasciiartplayer.audio.getAllPlayList
import com.tans.tasciiartplayer.databinding.AudioItemLayoutBinding
import com.tans.tasciiartplayer.databinding.AudioListDialogBinding
import com.tans.tasciiartplayer.formatDuration
import com.tans.tuiutils.adapter.impl.builders.SimpleAdapterBuilderImpl
import com.tans.tuiutils.adapter.impl.databinders.DataBinderImpl
import com.tans.tuiutils.adapter.impl.datasources.DataSourceImpl
import com.tans.tuiutils.adapter.impl.viewcreatators.SingleItemViewCreatorImpl
import com.tans.tuiutils.dialog.BaseCoroutineStateDialogFragment
import com.tans.tuiutils.dialog.createBottomSheetDialog
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap

class AudioListDialog : BaseCoroutineStateDialogFragment<Unit> {

    private val type: AudioListType?

    override val contentViewHeightInScreenRatio: Float = 1.0f

    constructor() : super(Unit) {
        this.type = null
    }

    constructor(type: AudioListType) : super(Unit) {
        this.type = type
    }

    override fun createContentView(context: Context, parent: ViewGroup): View {
        return if (type == null) {
            View(context)
        } else {
            createContentViewOrGetFromCache(type, context, parent)
        }
    }

    override fun createDialog(contentView: View): Dialog {
        ViewCompat.setOnApplyWindowInsetsListener(contentView) { v, insets ->
            val systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemInsets.bottom + systemInsets.top)
            insets
        }
        return requireActivity().createBottomSheetDialog(contentView = contentView) { b ->
            b.isDraggable = true
            b.isHideable = true
        }
    }

    override fun firstLaunchInitData() {  }

    override fun bindContentView(view: View) {
        type ?: dismiss()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (cachedContentViews[type]?.view?.parent as? ViewGroup)?.removeAllViews()
    }

    companion object {

        private data class ContentViewAndTask(
            val view: View,
            val task: Job
        )

        private val cachedContentViews: ConcurrentHashMap<AudioListType, ContentViewAndTask> = ConcurrentHashMap()

        private fun createContentViewOrGetFromCache(
            type: AudioListType,
            context: Context,
            parent: ViewGroup): View {
            val cache = cachedContentViews[type]
            return if (cache != null) {
                cache.view
            } else {
                val view = LayoutInflater.from(context).inflate(R.layout.audio_list_dialog, parent, false)
                val task = appGlobalCoroutineScope.launch(Dispatchers.Main) {
                    val ctx = context.applicationContext
                    val viewBinding = AudioListDialogBinding.bind(view)
                    viewBinding.listTitleTv.text = when (type) {
                        AudioListType.AllAudios -> ctx.getString(R.string.audios_fgt_all_audios)
                        AudioListType.LikeAudios -> ctx.getString(R.string.audios_fgt_my_favorites)
                        is AudioListType.AlbumAudios -> type.albumName
                        is AudioListType.ArtistAudios -> type.artistName
                        is AudioListType.CustomAudioList -> type.listName
                    }
                    val dataSource = DataSourceImpl<AudioModel>()
                    val glideLoadManager = Glide.with(ctx)
                    val adapter = SimpleAdapterBuilderImpl<AudioModel>(
                        itemViewCreator = SingleItemViewCreatorImpl(R.layout.audio_item_layout),
                        dataSource = dataSource,
                        dataBinder = DataBinderImpl { (audio, loadModel), view, _ ->
                            val itemViewBinding = AudioItemLayoutBinding.bind(view)
                            itemViewBinding.titleTv.text = audio.title
                            itemViewBinding.artistAlbumTv.text = "${audio.artist}-${audio.album}"
                            itemViewBinding.durationTv.text = audio.duration.formatDuration()
                            glideLoadManager
                                .load(loadModel)
                                .error(R.drawable.icon_audio)
                                .into(itemViewBinding.audioImgIv)
                            itemViewBinding.root.setOnClickListener {
                                // TODO:
                            }
                        }
                    ).build()
                    viewBinding.audioListRv.adapter = adapter
                    AudioManager.stateFlow()
                        .map { it.getAllPlayList()[type]?.audios ?: emptyList() }
                        .distinctUntilChanged()
                        .flowOn(Dispatchers.IO)
                        .collect {
                            dataSource.submitDataList(it)
                        }
                }
                return cachedContentViews.putIfAbsent(type, ContentViewAndTask(view, task)).let {
                    if (it != null) {
                        task.cancel()
                        it.view
                    } else {
                        view
                    }
                }
            }
        }
    }
}