package com.tans.tasciiartplayer.ui.main

import android.view.View
import com.tans.tasciiartplayer.R
import com.tans.tasciiartplayer.databinding.AudiosFragmentBinding
import com.tans.tuiutils.fragment.BaseCoroutineStateFragment
import kotlinx.coroutines.CoroutineScope

class AudiosFragment : BaseCoroutineStateFragment<Unit>(Unit) {

    override val layoutId: Int = R.layout.audios_fragment

    override fun CoroutineScope.firstLaunchInitDataCoroutine() {
        // TODO:
    }

    override fun CoroutineScope.bindContentViewCoroutine(contentView: View) {
        val viewBinding = AudiosFragmentBinding.bind(contentView)
        // TODO:
    }
}