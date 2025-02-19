package com.tans.tasciiartplayer.ui.main.iptv

import android.view.View
import com.tans.tasciiartplayer.R
import com.tans.tasciiartplayer.database.entity.IptvSource
import com.tans.tasciiartplayer.databinding.IptvSourceAddLayoutBinding
import com.tans.tasciiartplayer.databinding.IptvSourceDialogBinding
import com.tans.tasciiartplayer.databinding.IptvSourceItemLayoutBinding
import com.tans.tasciiartplayer.iptv.IptvManager
import com.tans.tuiutils.adapter.impl.builders.SimpleAdapterBuilderImpl
import com.tans.tuiutils.adapter.impl.builders.plus
import com.tans.tuiutils.adapter.impl.databinders.DataBinderImpl
import com.tans.tuiutils.adapter.impl.datasources.FlowDataSourceImpl
import com.tans.tuiutils.adapter.impl.viewcreatators.SingleItemViewCreatorImpl
import com.tans.tuiutils.dialog.BaseCoroutineStateDialogFragment
import com.tans.tuiutils.view.clicks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

class IptvSourceDialog : BaseCoroutineStateDialogFragment<IptvSourceDialog.Companion.State>(State()) {

    override val layoutId: Int = R.layout.iptv_source_dialog

    override fun firstLaunchInitData() {
        launch(Dispatchers.IO) {
            IptvManager.stateFlow()
                .map { it.selectedIptvSource to it.allIptvSources }
                .distinctUntilChanged()
                .collect { (selected, allSources) ->
                    updateState { s ->
                        val managerSelected = selected.getOrNull()
                        if (managerSelected == null) {
                            s.copy(managerSelected = selected, allSources = allSources.map { false to it })
                        } else {
                            val requestSelected = s.allSources.find { it.first }
                            var containRequestSelected = false
                            val newAllSources =  allSources.map {
                                if (it.createTime == requestSelected?.second?.createTime) {
                                    containRequestSelected = true
                                    true to it
                                } else {
                                    false to it
                                }
                            }
                            if (containRequestSelected) {
                                s.copy(managerSelected = selected, allSources = newAllSources)
                            } else {
                                s.copy(managerSelected = selected, allSources = allSources.map { (managerSelected.createTime == it.createTime) to it })
                            }
                        }
                    }
                }
        }
    }

    override fun bindContentView(view: View) {
        val viewBinding = IptvSourceDialogBinding.bind(view)
        val sourceAdapterBuilder = SimpleAdapterBuilderImpl<Pair<Boolean, IptvSource>>(
            itemViewCreator = SingleItemViewCreatorImpl(
                itemViewLayoutRes = R.layout.iptv_source_item_layout
            ),
            dataSource = FlowDataSourceImpl(
                dataFlow = stateFlow().map { it.allSources },
                areDataItemsTheSameParam = { d1, d2 -> d1.second == d2.second },
                getDataItemsChangePayloadParam = { d1, d2 ->
                    if (d1.second == d2.second && d1.first != d2.first) {
                        Unit
                    } else {
                        null
                    }
                }
            ),
            dataBinder = DataBinderImpl<Pair<Boolean, IptvSource>> { data, itemView, _ ->
                val itemViewBinding = IptvSourceItemLayoutBinding.bind(itemView)
                itemViewBinding.titleTv.text = data.second.title
                itemViewBinding.linkTv.text = data.second.sourceUrl
                itemViewBinding.modifyIv.clicks(this@IptvSourceDialog) {
                    ModifyIptvSourceDialog(data.second).showSafe(this@IptvSourceDialog.childFragmentManager, "ModifyIptvSourceDialog#${System.currentTimeMillis()}")
                }
                itemViewBinding.deleteIv.clicks(this@IptvSourceDialog) {
                    IptvManager.deleteIptvSource(data.second)
                }
            }.addPayloadDataBinder(Unit) { data, itemView, _ ->
                val itemViewBinding = IptvSourceItemLayoutBinding.bind(itemView)
                itemViewBinding.selectRb.isChecked = data.first
                itemViewBinding.root.clicks(this@IptvSourceDialog) {
                    if (!data.first) {
                        updateState { s ->
                            s.copy(
                                allSources = s.allSources
                                    .map {
                                        when {
                                            it.second == data.second -> true to data.second
                                            it.first -> false to it.second
                                            else -> it
                                        }
                                    }
                            )
                        }
                    }
                }
            }
        )
        val footerAdapterBuilder = SimpleAdapterBuilderImpl<Unit>(
            itemViewCreator = SingleItemViewCreatorImpl(itemViewLayoutRes = R.layout.iptv_source_add_layout),
            dataSource = FlowDataSourceImpl(dataFlow = flow { emit(listOf(Unit)) }),
            dataBinder = DataBinderImpl { _, itemView, _ ->
                val itemViewBinding = IptvSourceAddLayoutBinding.bind(itemView)
                itemViewBinding.root.clicks(this@IptvSourceDialog) {
                    AddIptvSourceDialog().showSafe(this.childFragmentManager, "AddIptvSourceDialog#${System.currentTimeMillis()}")
                }
            }
        )
        viewBinding.iptvSourceRv.adapter = (sourceAdapterBuilder + footerAdapterBuilder).build()

        viewBinding.cancelBt.clicks(this) {
            dismissSafe()
        }
        viewBinding.okBt.clicks(this) {
            val s = currentState()
            val managerSelected = s.managerSelected.getOrNull()
            val requestSelect = s.allSources.find { it.first }?.second
            if (requestSelect != managerSelected && requestSelect != null) {
                withContext(Dispatchers.IO) {
                    IptvManager.selectIptvSource(requestSelect.createTime)
                }
            }
            dismissSafe()
        }
    }

    companion object {
        data class State(
            val managerSelected: Optional<IptvSource> = Optional.empty(),
            val allSources: List<Pair<Boolean, IptvSource>> = emptyList()
        )
    }
}