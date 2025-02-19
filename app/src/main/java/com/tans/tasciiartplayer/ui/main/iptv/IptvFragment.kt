package com.tans.tasciiartplayer.ui.main.iptv

import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.bumptech.glide.Glide
import com.tans.tasciiartplayer.R
import com.tans.tasciiartplayer.databinding.EmptyItemLayoutBinding
import com.tans.tasciiartplayer.databinding.IptvFragmentBinding
import com.tans.tasciiartplayer.databinding.IptvItemLayoutBinding
import com.tans.tasciiartplayer.iptv.IptvManager
import com.tans.tasciiartplayer.iptv.m3u8.model.M3u8Extinf
import com.tans.tasciiartplayer.ui.videoplayer.VideoPlayerActivity
import com.tans.tuiutils.adapter.impl.builders.SimpleAdapterBuilderImpl
import com.tans.tuiutils.adapter.impl.builders.plus
import com.tans.tuiutils.adapter.impl.databinders.DataBinderImpl
import com.tans.tuiutils.adapter.impl.datasources.FlowDataSourceImpl
import com.tans.tuiutils.adapter.impl.viewcreatators.SingleItemViewCreatorImpl
import com.tans.tuiutils.dialog.dp2px
import com.tans.tuiutils.fragment.BaseCoroutineStateFragment
import com.tans.tuiutils.view.clicks
import com.tans.tuiutils.view.refreshes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

class IptvFragment : BaseCoroutineStateFragment<Unit>(Unit) {

    override val layoutId: Int = R.layout.iptv_fragment

    override fun CoroutineScope.firstLaunchInitDataCoroutine() {  }

    override fun CoroutineScope.bindContentViewCoroutine(contentView: View) {
        val viewBinding = IptvFragmentBinding.bind(contentView)
        viewBinding.iptvSourceFab.clicks(this) {
            IptvSourceDialog().showSafe(this@IptvFragment.childFragmentManager, "IptvSourceDialog#${System.currentTimeMillis()}")
        }
        viewBinding.swipeRefreshLayout.refreshes(this, Dispatchers.IO) {
            IptvManager.refresh()
        }

        launch {
            IptvManager.stateFlow()
                .map { it.loadIptvSourceStatus }
                .flowOn(Dispatchers.IO)
                .collect { loadStatus ->
                    when (loadStatus) {
                        is IptvManager.LoadIptvSourceStatus.Loading, is IptvManager.LoadIptvSourceStatus.Refreshing -> {
                            if (!viewBinding.swipeRefreshLayout.isRefreshing) {
                                viewBinding.swipeRefreshLayout.isRefreshing = true
                            }
                        }
                        else -> {
                            if (viewBinding.swipeRefreshLayout.isRefreshing) {
                                viewBinding.swipeRefreshLayout.isRefreshing = false
                            }
                        }
                    }
                }
        }

        val iptvAdapterBuilder = SimpleAdapterBuilderImpl<M3u8Extinf>(
            itemViewCreator = SingleItemViewCreatorImpl(
                itemViewLayoutRes = R.layout.iptv_item_layout
            ),
            dataSource = FlowDataSourceImpl(IptvManager.stateFlow()
                .map {
                    when (val loadStatus = it.loadIptvSourceStatus) {
                        is IptvManager.LoadIptvSourceStatus.LoadSuccess -> loadStatus.loaded.extinfs
                        is IptvManager.LoadIptvSourceStatus.RefreshSuccess -> loadStatus.loaded.extinfs
                        is IptvManager.LoadIptvSourceStatus.Refreshing -> loadStatus.lastLoaded.extinfs
                        is IptvManager.LoadIptvSourceStatus.LoadFail,
                        is IptvManager.LoadIptvSourceStatus.Loading,
                        IptvManager.LoadIptvSourceStatus.NoData,
                        is IptvManager.LoadIptvSourceStatus.RefreshFail -> {
                            emptyList()
                        }
                    }
                }),
            dataBinder = DataBinderImpl { data, itemView, _ ->
                val itemViewBinding = IptvItemLayoutBinding.bind(itemView)
                itemViewBinding.tvNameTv.text = data.titleText
                itemViewBinding.tvLinkTv.text = data.sourceUrl
                val tvLogoUrl = data.durationAsKeyValues["tvg-logo"]
                if (tvLogoUrl != null) {
                    Glide.with(this@IptvFragment)
                        .load(tvLogoUrl)
                        .placeholder(R.drawable.icon_movie)
                        .error(R.drawable.icon_movie)
                        .into(itemViewBinding.tvLogoIv)
                } else {
                    itemViewBinding.tvLogoIv.setImageDrawable(ContextCompat.getDrawable(requireContext(), R.drawable.icon_movie))
                }
                itemViewBinding.root.clicks(this) {
                    requireActivity().startActivity(VideoPlayerActivity.createIntent(context = requireContext(), customLink = data.sourceUrl))
                }
            }
        )

        val errorAdapterBuilder = SimpleAdapterBuilderImpl(
            itemViewCreator = SingleItemViewCreatorImpl(
                R.layout.empty_item_layout
            ),
            dataSource = FlowDataSourceImpl(IptvManager.stateFlow()
                .map {
                    when (val loadStatus = it.loadIptvSourceStatus) {
                        is IptvManager.LoadIptvSourceStatus.LoadFail -> listOf(loadStatus.msg)
                        IptvManager.LoadIptvSourceStatus.NoData -> listOf(ContextCompat.getString(requireContext(), R.string.iptv_fgt_no_data))
                        is IptvManager.LoadIptvSourceStatus.RefreshFail -> listOf(loadStatus.msg)
                        is IptvManager.LoadIptvSourceStatus.Loading,
                        is IptvManager.LoadIptvSourceStatus.LoadSuccess,
                        is IptvManager.LoadIptvSourceStatus.RefreshSuccess,
                        is IptvManager.LoadIptvSourceStatus.Refreshing -> emptyList()
                    }
                }),
            dataBinder = DataBinderImpl { data, itemView, _ ->
                val itemViewBinding = EmptyItemLayoutBinding.bind(itemView)
                itemViewBinding.msgTv.text = data
            }
        )
        viewBinding.iptvRv.adapter = (iptvAdapterBuilder + errorAdapterBuilder).build()


        ViewCompat.setOnApplyWindowInsetsListener(viewBinding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val fabLp = viewBinding.iptvSourceFab.layoutParams as MarginLayoutParams
            fabLp.bottomMargin = systemBars.bottom + requireContext().dp2px(20)
            viewBinding.iptvSourceFab.layoutParams = fabLp
            viewBinding.iptvRv.setPadding(0, 0, 0, systemBars.bottom + viewBinding.iptvSourceFab.measuredHeight)
            insets
        }
    }

}