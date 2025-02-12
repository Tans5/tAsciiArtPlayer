package com.tans.tasciiartplayer.ui.main

import android.view.View
import com.tans.tasciiartplayer.R
import com.tans.tuiutils.fragment.BaseCoroutineStateFragment
import kotlinx.coroutines.CoroutineScope

class IptvFragment : BaseCoroutineStateFragment<Unit>(Unit) {

    override val layoutId: Int = R.layout.iptv_fragment

    override fun CoroutineScope.firstLaunchInitDataCoroutine() {
        // TODO:
    }

    override fun CoroutineScope.bindContentViewCoroutine(contentView: View) {
        // TODO:
    }

}