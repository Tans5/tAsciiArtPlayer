package com.tans.tasciiartplayer.ui.main

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.tans.tasciiartplayer.R
import com.tans.tasciiartplayer.audio.audioplayer.AudioPlayerManager
import com.tans.tasciiartplayer.databinding.VideoItemLayoutBinding
import com.tans.tasciiartplayer.databinding.VideosFragmentBinding
import com.tans.tasciiartplayer.formatDuration
import com.tans.tasciiartplayer.ui.videoplayer.VideoPlayerActivity
import com.tans.tasciiartplayer.video.VideoManager
import com.tans.tasciiartplayer.video.VideoModel
import com.tans.tuiutils.adapter.impl.builders.SimpleAdapterBuilderImpl
import com.tans.tuiutils.adapter.impl.databinders.DataBinderImpl
import com.tans.tuiutils.adapter.impl.datasources.FlowDataSourceImpl
import com.tans.tuiutils.adapter.impl.viewcreatators.SingleItemViewCreatorImpl
import com.tans.tuiutils.dialog.dp2px
import com.tans.tuiutils.fragment.BaseCoroutineStateFragment
import com.tans.tuiutils.view.clicks
import com.tans.tuiutils.view.refreshes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class VideosFragment : BaseCoroutineStateFragment<VideosFragment.Companion.State>(State()) {

    override val layoutId: Int = R.layout.videos_fragment

    override fun CoroutineScope.firstLaunchInitDataCoroutine() {

        launch {
            VideoManager.refreshMediaStoreVideos()
        }

        launch {
            VideoManager.stateFlow
                .map { it.videos.sortedByDescending { mv -> mv.mediaStoreVideo.dateModified } }
                .distinctUntilChanged()
                .collect { videos -> updateState { it.copy(videos = videos) } }
        }
    }

    @OptIn(FlowPreview::class)
    override fun CoroutineScope.bindContentViewCoroutine(contentView: View) {
        val viewBinding = VideosFragmentBinding.bind(contentView)
        viewBinding.refreshLayout.refreshes(this, Dispatchers.IO) {
            VideoManager.refreshMediaStoreVideos()
        }
        val glideLoadManager = Glide.with(this@VideosFragment)
//        glideLoadManager.resumeRequests()
        val videoAdapterBuilder = SimpleAdapterBuilderImpl(
            itemViewCreator = SingleItemViewCreatorImpl(R.layout.video_item_layout),
            dataSource = FlowDataSourceImpl(
                dataFlow = stateFlow().map { it.videos },
                getDataItemIdParam = { d, _ -> d.mediaStoreVideo.id },
                areDataItemsTheSameParam = { d1, d2 -> d1.mediaStoreVideo.id == d2.mediaStoreVideo.id },
                getDataItemsChangePayloadParam = { d1, d2 -> if (d1.mediaStoreVideo == d2.mediaStoreVideo && d1.lastWatch != d2.lastWatch) Unit else null }
            ),
            dataBinder = DataBinderImpl<VideoModel> { (video, _, _), view, _ ->
                val itemViewBinding = VideoItemLayoutBinding.bind(view)
                itemViewBinding.videoTitleTv.text = video.displayName
            }.addPayloadDataBinder(Unit) { (video, loadModel, lastWatch), view, _ ->
                val itemViewBinding = VideoItemLayoutBinding.bind(view)
                glideLoadManager
                    .load(loadModel)
                    .error(R.drawable.icon_movie)
                    .into(itemViewBinding.videoIv)
                itemViewBinding.durationTv.text = video.duration.formatDuration()
                if (lastWatch == null) {
                    itemViewBinding.lastWatchPb.visibility = View.GONE
                } else {
                    itemViewBinding.lastWatchPb.visibility = View.VISIBLE
                    itemViewBinding.lastWatchPb.progress = ((lastWatch.toDouble() / video.duration.toDouble()) * 100.0).toInt()
                }
                itemViewBinding.root.clicks(this) {
                    startActivity(
                        VideoPlayerActivity.createIntent(
                            context = requireActivity(),
                            mediaId = video.id,
                            mediaFile = video.file?.canonicalPath ?: "",
                            lastWatch = lastWatch
                        )
                    )
                }
            }
        )

        viewBinding.videosRv.adapter = videoAdapterBuilder.build()

//        viewBinding.videosRv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
//            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
//                super.onScrollStateChanged(recyclerView, newState)
//                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
//                    glideLoadManager.resumeRequests()
//                } else {
//                    glideLoadManager.pauseRequests()
//                }
//            }
//        })
        launch {
            stateFlow()
                .map { it.videos.isEmpty() }
                .debounce(200L)
                .distinctUntilChanged()
                .flowOn(Dispatchers.IO)
                .collect {
                    if (it) {
                        viewBinding.emptyTv.visibility = View.VISIBLE
                        viewBinding.videosRv.visibility = View.INVISIBLE
                    } else {
                        viewBinding.emptyTv.visibility = View.INVISIBLE
                        viewBinding.videosRv.visibility = View.VISIBLE
                    }
                }
        }

        ViewCompat.setOnApplyWindowInsetsListener(viewBinding.videosRv) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(v.paddingLeft, v.paddingTop, v.paddingRight, systemBars.bottom + requireContext().dp2px(8))
            insets
        }
    }

    companion object {

        data class State(
            val videos: List<VideoModel> = emptyList()
        )
    }
}