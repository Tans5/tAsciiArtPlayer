package com.tans.tasciiartplayer.ui.audioplayer

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.tans.tasciiartplayer.R
import com.tans.tasciiartplayer.audio.audiolist.AudioListType
import com.tans.tasciiartplayer.audio.audiolist.AudioListManager
import com.tans.tasciiartplayer.audio.audiolist.AudioModel
import com.tans.tasciiartplayer.audio.audioplayer.AudioPlayerManager
import com.tans.tasciiartplayer.audio.audioplayer.PlayListState
import com.tans.tasciiartplayer.audio.audiolist.getAllPlayList
import com.tans.tasciiartplayer.databinding.AudioItemLayoutBinding
import com.tans.tasciiartplayer.databinding.AudioListDialogBinding
import com.tans.tasciiartplayer.databinding.EmptyItemLayoutBinding
import com.tans.tasciiartplayer.formatDuration
import com.tans.tuiutils.adapter.impl.builders.SimpleAdapterBuilderImpl
import com.tans.tuiutils.adapter.impl.builders.plus
import com.tans.tuiutils.adapter.impl.databinders.DataBinderImpl
import com.tans.tuiutils.adapter.impl.datasources.DataSourceImpl
import com.tans.tuiutils.adapter.impl.viewcreatators.SingleItemViewCreatorImpl
import com.tans.tuiutils.dialog.BaseCoroutineStateDialogFragment
import com.tans.tuiutils.view.clicks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

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
        return requireActivity().createAudioBottomSheetDialog(contentView)
    }

    override fun firstLaunchInitData() {  }

    override fun bindContentView(view: View) {
        type ?: dismiss()
    }

    companion object {

        private enum class ChangePayload {
            PlayingStateChange,
            LikeStateChange
        }

        private data class AudioWithPlaying(
            val audioModel: AudioModel,
            val isPlaying: Boolean,
        )

        @SuppressLint("SetTextI18n")
        private val cache = ContextMultiViewAndTaskCache<AudioListType> { type, ctx, viewGroup ->
            val view = LayoutInflater.from(ctx).inflate(R.layout.audio_list_dialog, viewGroup, false)
            val coroutineScope = ctx.lifecycleScope
            val task = coroutineScope.launch {
                val viewBinding = AudioListDialogBinding.bind(view)
                viewBinding.listTitleTv.text = when (type) {
                    AudioListType.AllAudios -> ctx.getString(R.string.audios_fgt_all_audios)
                    AudioListType.LikeAudios -> ctx.getString(R.string.audios_fgt_my_favorites)
                    is AudioListType.AlbumAudios -> type.albumName
                    is AudioListType.ArtistAudios -> type.artistName
                    is AudioListType.CustomAudioList -> type.listName
                }
                launch {
                    AudioPlayerManager.stateFlow()
                        .map {
                            if (it.playListState is PlayListState.SelectedPlayList) {
                                it.playListState.audioList.audioListType == type
                            } else {
                                false
                            }
                        }
                        .distinctUntilChanged()
                        .flowOn(Dispatchers.IO)
                        .collect {
                            viewBinding.listTitleTv.setTextColor(ContextCompat.getColor(ctx, if (it) R.color.cyan_400 else R.color.gray_900))
                        }
                }

                val dataSource = DataSourceImpl<AudioWithPlaying>(
                    areDataItemsTheSameParam = { d1, d2 -> d1.audioModel.mediaStoreAudio.id == d2.audioModel.mediaStoreAudio.id},
                    areDataItemsContentTheSameParam = { d1, d2 -> d1 == d2 },
                    getDataItemIdParam = { d, _ -> d.audioModel.mediaStoreAudio.id },
                    getDataItemsChangePayloadParam = { d1, d2 ->
                        when {
                            d1.isPlaying != d2.isPlaying -> ChangePayload.PlayingStateChange
                            d1.audioModel.isLike != d2.audioModel.isLike -> ChangePayload.LikeStateChange
                            else -> null
                        }
                    }
                )
                val glideLoadManager = Glide.with(ctx)
                val audioAdapterBuilder = SimpleAdapterBuilderImpl<AudioWithPlaying>(
                    itemViewCreator = SingleItemViewCreatorImpl(R.layout.audio_item_layout),
                    dataSource = dataSource,
                    dataBinder = DataBinderImpl<AudioWithPlaying> { data, view, _ ->
                        val audio = data.audioModel.mediaStoreAudio
                        val loadModel = data.audioModel.glideLoadModel
                        val itemViewBinding = AudioItemLayoutBinding.bind(view)
                        itemViewBinding.titleTv.text = audio.title
                        itemViewBinding.artistAlbumTv.text = "${audio.artist}-${audio.album}"
                        itemViewBinding.durationTv.text = audio.duration.formatDuration()
                        glideLoadManager
                            .load(loadModel)
                            .error(R.drawable.icon_audio)
                            .into(itemViewBinding.audioImgIv)
                    }.addPayloadDataBinder(ChangePayload.PlayingStateChange) { data, view, _ ->
                        val itemViewBinding = AudioItemLayoutBinding.bind(view)
                        itemViewBinding.titleTv.setTextColor(ContextCompat.getColor(ctx, if (data.isPlaying) R.color.cyan_400 else R.color.gray_900))
                        itemViewBinding.artistAlbumTv.setTextColor(ContextCompat.getColor(ctx, if (data.isPlaying) R.color.cyan_400 else R.color.gray_900))
                        itemViewBinding.durationTv.setTextColor(ContextCompat.getColor(ctx, if (data.isPlaying) R.color.cyan_200 else R.color.gray_400))
                    }.addPayloadDataBinder(ChangePayload.LikeStateChange) { data, view, _ ->
                        val itemViewBinding = AudioItemLayoutBinding.bind(view)
                        itemViewBinding.root.clicks(coroutineScope, 1000L, Dispatchers.IO) {
                            val audioList = AudioListManager.stateFlow.value.getAllPlayList()[type]
                            if (audioList != null) {
                                AudioPlayerManager.playAudioList(list = audioList, startIndex = audioList.audios.indexOf(data.audioModel))
                            }
                        }
                        itemViewBinding.likeIv.setImageResource(if (data.audioModel.isLike) R.drawable.icon_favorite_fill else R.drawable.icon_favorite_unfill)
                        itemViewBinding.likeCard.clicks(coroutineScope, 1000L, Dispatchers.IO) {
                            try {
                                if (data.audioModel.isLike) {
                                    AudioListManager.unlikeAudio(data.audioModel.mediaStoreAudio.id)
                                } else {
                                    AudioListManager.likeAudio(data.audioModel.mediaStoreAudio.id)
                                }
                            } catch (e: Throwable) {
                                e.printStackTrace()
                            }
                        }
                    }
                )
                val emptyDataSource = DataSourceImpl<Unit>()
                val emptyAdapterBuilder = SimpleAdapterBuilderImpl<Unit>(
                    itemViewCreator = SingleItemViewCreatorImpl(R.layout.empty_item_layout),
                    dataSource = emptyDataSource,
                    dataBinder = DataBinderImpl{ _, itemView, _ ->
                        val itemViewBinding = EmptyItemLayoutBinding.bind(itemView)
                        itemViewBinding.msgTv.text = itemView.context.getString(R.string.audios_fgt_no_audio)
                    }
                )
                viewBinding.audioListRv.adapter = (audioAdapterBuilder + emptyAdapterBuilder).build()
                combine(
                    AudioListManager.stateFlow(),
                    AudioPlayerManager.stateFlow()
                        .map { apms ->
                            (apms.playListState as? PlayListState.SelectedPlayList)?.currentPlayIndex?.let {
                                apms.playListState.audioList.audios.getOrNull(it)?.mediaStoreAudio?.id
                            }
                        }
                        .distinctUntilChanged()
                ) { ams, playingAudioId ->
                    val audioList = ams.getAllPlayList()[type]?.audios ?: emptyList()
                    audioList.map { AudioWithPlaying(audioModel = it, isPlaying = it.mediaStoreAudio.id == playingAudioId) }
                }
                    .distinctUntilChanged()
                    .flowOn(Dispatchers.IO)
                    .collect {
                        dataSource.submitDataList(it)
                        if (it.isEmpty()) {
                            emptyDataSource.submitDataList(listOf(Unit))
                        } else {
                            emptyDataSource.submitDataList(emptyList())
                        }
                    }
            }
            ContentViewAndTask(view, task)
        }


        private fun createContentViewOrGetFromCache(
            type: AudioListType,
            context: Context,
            parent: ViewGroup): View {
            return cache.getFromCacheOrCreateNew(type, context, parent)?.view ?: View(context)
        }

        fun removeCacheContentViewAndTask(context: Context) {
            cache.removeCache(context)
        }
    }
}