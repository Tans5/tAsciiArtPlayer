package com.tans.tasciiartplayer.ui.audioplayer

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.tans.tasciiartplayer.R
import com.tans.tasciiartplayer.audio.audiolist.AudioList
import com.tans.tasciiartplayer.audio.audiolist.AudioListType
import com.tans.tasciiartplayer.audio.audiolist.AudioListManager
import com.tans.tasciiartplayer.databinding.AudioAlbumItemLayoutBinding
import com.tans.tasciiartplayer.databinding.AudioAlbumsDialogBinding
import com.tans.tasciiartplayer.databinding.EmptyItemLayoutBinding
import com.tans.tuiutils.adapter.impl.builders.SimpleAdapterBuilderImpl
import com.tans.tuiutils.adapter.impl.builders.plus
import com.tans.tuiutils.adapter.impl.databinders.DataBinderImpl
import com.tans.tuiutils.adapter.impl.datasources.DataSourceImpl
import com.tans.tuiutils.adapter.impl.viewcreatators.SingleItemViewCreatorImpl
import com.tans.tuiutils.dialog.BaseCoroutineStateDialogFragment
import com.tans.tuiutils.view.clicks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class AlbumsDialog : BaseCoroutineStateDialogFragment<Unit>(Unit) {

    override val contentViewHeightInScreenRatio: Float = 1.0f

    override fun createContentView(context: Context, parent: ViewGroup): View {
        return createContentViewOrGetFromCache(context, parent)
    }

    override fun createDialog(contentView: View): Dialog {
        return requireActivity().createAudioBottomSheetDialog(contentView)
    }

    override fun firstLaunchInitData() {  }

    var contentView: View? = null

    override fun bindContentView(view: View) {
        contentView = view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        (contentView?.parent as? ViewGroup)?.removeAllViews()
        contentView = null
    }

    companion object {

        private val cache = ContextViewAndTaskCache { ctx, viewGroup ->
            val contentView = LayoutInflater.from(ctx).inflate(R.layout.audio_albums_dialog, viewGroup, false)
            val coroutineScope = ctx.lifecycleScope
            val task = coroutineScope.launch(Dispatchers.Main) {
                val viewBinding = AudioAlbumsDialogBinding.bind(contentView)

                val glideLoadManager = Glide.with(ctx.applicationContext)

                val dataSource = DataSourceImpl<AudioList>()
                val dataAdapterBuilder = SimpleAdapterBuilderImpl<AudioList>(
                    itemViewCreator = SingleItemViewCreatorImpl<AudioList>(R.layout.audio_album_item_layout),
                    dataSource = dataSource,
                    dataBinder = DataBinderImpl<AudioList> { data, view, _ ->
                        val itemViewBinding = AudioAlbumItemLayoutBinding.bind(view)
                        itemViewBinding.albumTitleTv.text = (data.audioListType as? AudioListType.AlbumAudios)?.albumName
                        itemViewBinding.artistTv.text = data.audios.getOrNull(0)?.mediaStoreAudio?.artist
                        itemViewBinding.songCountTv.text = data.audios.size.toString()
                        glideLoadManager.load(data.audios.getOrNull(0)?.glideLoadModel)
                            .error(R.drawable.icon_album)
                            .into(itemViewBinding.albumIv)

                        itemViewBinding.root.clicks(coroutineScope, 1000L) {
                            val d = AudioListDialog(data.audioListType)
                            d.showSafe(ctx.supportFragmentManager, "AudioListDialog#${System.currentTimeMillis()}")
                        }
                    }
                )

                viewBinding.albumsRv.adapter = dataAdapterBuilder.build()
                AudioListManager.stateFlow()
                    .map { it.albumAudioLists }
                    .distinctUntilChanged()
                    .flowOn(Dispatchers.IO)
                    .collect {
                        dataSource.submitDataList(it)
                        if (it.isEmpty()) {
                            viewBinding.emptyTv.visibility = View.VISIBLE
                            viewBinding.albumsRv.visibility = View.INVISIBLE
                        } else {
                            viewBinding.emptyTv.visibility = View.INVISIBLE
                            viewBinding.albumsRv.visibility = View.VISIBLE
                        }
                    }
            }

            ContentViewAndTask(contentView, task)
        }

        private fun createContentViewOrGetFromCache(
            context: Context,
            parent: ViewGroup): View {
            return cache.getFromCacheOrCreateNew(context, parent)?.view ?: View(context)
        }

        fun removeCacheContentViewAndTask(context: Context) {
            cache.removeCache(context)
        }
    }
}