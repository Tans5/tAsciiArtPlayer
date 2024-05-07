package com.tans.tasciiartplayer.ui.main

import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.tans.tasciiartplayer.R
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
import kotlinx.coroutines.flow.distinctUntilChanged
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

    override fun CoroutineScope.bindContentViewCoroutine(contentView: View) {
        val viewBinding = VideosFragmentBinding.bind(contentView)
        viewBinding.refreshLayout.refreshes(this, Dispatchers.IO) {
            VideoManager.refreshMediaStoreVideos()
        }
        val ctx = requireContext()
        val glideLoadManager = Glide.with(this@VideosFragment)
        glideLoadManager.resumeRequests()
        val adapter = SimpleAdapterBuilderImpl(
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
                    .error(R.drawable.ic_movie)
                    .placeholder(R.drawable.ic_movie)
                    .into(itemViewBinding.videoIv)
                if (lastWatch == null) {
                    itemViewBinding.videoLastWatchAndDurationTv.text = video.duration.formatDuration()
                } else {
                    itemViewBinding.videoLastWatchAndDurationTv.text = ctx.getString(R.string.video_watch_history_and_duration, lastWatch.formatDuration(), video.duration.formatDuration())
                }
                itemViewBinding.root.clicks(this) {
                    startActivity(VideoPlayerActivity.createIntent(requireActivity(), video.id,  video.file?.canonicalPath ?: ""))
                }
            }
        ).build()
        viewBinding.videosRv.adapter = adapter

        viewBinding.videosRv.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrollStateChanged(recyclerView: RecyclerView, newState: Int) {
                super.onScrollStateChanged(recyclerView, newState)
                if (newState == RecyclerView.SCROLL_STATE_IDLE) {
                    glideLoadManager.resumeRequests()
                } else {
                    glideLoadManager.pauseRequests()
                }
            }
        })

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