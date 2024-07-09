package com.tans.tasciiartplayer.ui.audioplayer

import android.app.Dialog
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.FragmentActivity
import com.bumptech.glide.Glide
import com.tans.tasciiartplayer.R
import com.tans.tasciiartplayer.appGlobalCoroutineScope
import com.tans.tasciiartplayer.audio.AudioList
import com.tans.tasciiartplayer.audio.AudioListType
import com.tans.tasciiartplayer.audio.AudioManager
import com.tans.tasciiartplayer.databinding.ArtistItemLayoutBinding
import com.tans.tasciiartplayer.databinding.ArtistsDialogBinding
import com.tans.tasciiartplayer.databinding.EmptyItemLayoutBinding
import com.tans.tuiutils.adapter.impl.builders.SimpleAdapterBuilderImpl
import com.tans.tuiutils.adapter.impl.builders.plus
import com.tans.tuiutils.adapter.impl.databinders.DataBinderImpl
import com.tans.tuiutils.adapter.impl.datasources.DataSourceImpl
import com.tans.tuiutils.adapter.impl.viewcreatators.SingleItemViewCreatorImpl
import com.tans.tuiutils.dialog.BaseCoroutineStateDialogFragment
import com.tans.tuiutils.dialog.createBottomSheetDialog
import com.tans.tuiutils.systembar.SystemBarThemeStyle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

class ArtistsDialog : BaseCoroutineStateDialogFragment<Unit>(Unit) {

    override val contentViewHeightInScreenRatio: Float = 1.0f


    override fun createContentView(context: Context, parent: ViewGroup): View {
        return createContentViewOrGetFromCache(context, parent)
    }

    override fun createDialog(contentView: View): Dialog {
        ViewCompat.setOnApplyWindowInsetsListener(contentView) { v, insets ->
            val systemInsets = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(0, 0, 0, systemInsets.bottom + systemInsets.top)
            insets
        }
        return requireActivity().createBottomSheetDialog(
            contentView = contentView,
            navigationThemeStyle = SystemBarThemeStyle.Light,
            statusBarThemeStyle = SystemBarThemeStyle.Light
        ) { b ->
            b.isDraggable = true
            b.isHideable = true
        }
    }

    override fun firstLaunchInitData() {  }

    override fun bindContentView(view: View) {  }

    override fun onDestroyView() {
        super.onDestroyView()
        (cachedContentViewAndTask.get()?.view?.parent as? ViewGroup)?.removeAllViews()
    }

    companion object {

        private data class ContentViewAndTask(
            val view: View,
            val task: Job
        )

        private val cachedContentViewAndTask: AtomicReference<ContentViewAndTask?> = AtomicReference(null)

        private fun createContentViewOrGetFromCache(
            context: Context,
            parent: ViewGroup): View {
            val cache = cachedContentViewAndTask.get()
            return if (cache != null) {
                cache.view
            } else {
                val contentView = LayoutInflater.from(context).inflate(R.layout.artists_dialog, parent, false)
                val task = appGlobalCoroutineScope.launch(Dispatchers.Main) {
                    val viewBinding = ArtistsDialogBinding.bind(contentView)

                    val glideLoadManager = Glide.with(context.applicationContext)

                    val dataSource = DataSourceImpl<AudioList>()
                    val dataAdapterBuilder = SimpleAdapterBuilderImpl<AudioList>(
                        itemViewCreator = SingleItemViewCreatorImpl<AudioList>(R.layout.artist_item_layout),
                        dataSource = dataSource,
                        dataBinder = DataBinderImpl<AudioList> { data, view, _ ->
                            val itemViewBinding = ArtistItemLayoutBinding.bind(view)
                            itemViewBinding.artistTv.text = (data.audioListType as? AudioListType.ArtistAudios)?.artistName
                            itemViewBinding.songCountTv.text = data.audios.size.toString()
                            glideLoadManager.load(data.audios.getOrNull(0)?.glideLoadModel)
                                .error(R.drawable.icon_artist)
                                .into(itemViewBinding.artistAvatarIv)

                            itemViewBinding.root.setOnClickListener { v ->
                                val ctx = v.context as? FragmentActivity
                                if (ctx != null) {
                                    val d = AudioListDialog(data.audioListType)
                                    d.show(ctx.supportFragmentManager, "AudioListDialog#${System.currentTimeMillis()}")
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

                    viewBinding.artistsRv.adapter = (dataAdapterBuilder + emptyAdapterBuilder).build()
                    AudioManager.stateFlow()
                        .map { it.artistAudioLists }
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
                if (cachedContentViewAndTask.compareAndSet(null, ContentViewAndTask(contentView, task))) {
                    contentView
                } else {
                    task.cancel()
                    createContentViewOrGetFromCache(context, parent)
                }
            }
        }
    }
}