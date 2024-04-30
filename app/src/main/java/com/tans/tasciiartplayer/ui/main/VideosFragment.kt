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
import com.tans.tasciiartplayer.glide.MediaImageModel
import com.tans.tasciiartplayer.ui.videoplayer.VideoPlayerActivity
import com.tans.tuiutils.adapter.impl.builders.SimpleAdapterBuilderImpl
import com.tans.tuiutils.adapter.impl.databinders.DataBinderImpl
import com.tans.tuiutils.adapter.impl.datasources.FlowDataSourceImpl
import com.tans.tuiutils.adapter.impl.viewcreatators.SingleItemViewCreatorImpl
import com.tans.tuiutils.dialog.dp2px
import com.tans.tuiutils.fragment.BaseCoroutineStateFragment
import com.tans.tuiutils.mediastore.MediaStoreVideo
import com.tans.tuiutils.mediastore.queryVideoFromMediaStore
import com.tans.tuiutils.view.clicks
import com.tans.tuiutils.view.refreshes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class VideosFragment : BaseCoroutineStateFragment<VideosFragment.Companion.State>(State()) {

    override val layoutId: Int = R.layout.videos_fragment

    override fun CoroutineScope.firstLaunchInitDataCoroutine() {
        launch { refreshVideos() }
    }

    override fun CoroutineScope.bindContentViewCoroutine(contentView: View) {
        val viewBinding = VideosFragmentBinding.bind(contentView)
        viewBinding.refreshLayout.refreshes(this, Dispatchers.IO) {
            refreshVideos()
        }
        val glideLoadManager = Glide.with(this@VideosFragment)
        glideLoadManager.resumeRequests()
        val adapter = SimpleAdapterBuilderImpl<VideoAndLoadModel>(
            itemViewCreator = SingleItemViewCreatorImpl(R.layout.video_item_layout),
            dataSource = FlowDataSourceImpl(stateFlow().map { it.videos }),
            dataBinder = DataBinderImpl { (video, loadModel), view, _ ->
                val itemViewBinding = VideoItemLayoutBinding.bind(view)
                itemViewBinding.videoTitleTv.text = video.displayName
                itemViewBinding.videoDurationTv.text = video.duration.formatDuration()
                glideLoadManager
                    .load(loadModel)
                    .error(R.drawable.ic_movie)
                    .placeholder(R.drawable.ic_movie)
                    .into(itemViewBinding.videoIv)

                itemViewBinding.root.clicks(this) {
                    startActivity(VideoPlayerActivity.createIntent(requireActivity(), video.file?.canonicalPath ?: ""))
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

    private fun refreshVideos() {
        val videos = queryVideoFromMediaStore()
            .sortedByDescending { it.dateModified }
            .filter { it.file != null }
            .map {
                VideoAndLoadModel(
                    video = it,
                    loadModel = MediaImageModel(
                        mediaFilePath = it.file?.canonicalPath ?: "",
                        targetPosition = 10000L,
                        keyId = it.id
                    )
                )
            }
        updateState {
            it.copy(videos = videos)
        }
    }

    companion object {

        data class VideoAndLoadModel(
            val video: MediaStoreVideo,
            val loadModel: MediaImageModel
        )

        data class State(
            val videos: List<VideoAndLoadModel> = emptyList()
        )
    }
}