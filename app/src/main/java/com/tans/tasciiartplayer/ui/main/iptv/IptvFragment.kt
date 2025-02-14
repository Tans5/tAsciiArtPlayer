package com.tans.tasciiartplayer.ui.main.iptv

import android.view.View
import android.view.ViewGroup.MarginLayoutParams
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.tans.tasciiartplayer.R
import com.tans.tasciiartplayer.databinding.IptvFragmentBinding
import com.tans.tuiutils.dialog.dp2px
import com.tans.tuiutils.fragment.BaseCoroutineStateFragment
import com.tans.tuiutils.view.clicks
import kotlinx.coroutines.CoroutineScope

class IptvFragment : BaseCoroutineStateFragment<Unit>(Unit) {

    override val layoutId: Int = R.layout.iptv_fragment

    override fun CoroutineScope.firstLaunchInitDataCoroutine() {
        // TODO:
    }

    override fun CoroutineScope.bindContentViewCoroutine(contentView: View) {
        val viewBinding = IptvFragmentBinding.bind(contentView)
        viewBinding.iptvSourceFab.clicks(this) {
            IptvSourceDialog().showSafe(this@IptvFragment.childFragmentManager, "IptvSourceDialog#${System.currentTimeMillis()}")
        }
        // TODO:

        ViewCompat.setOnApplyWindowInsetsListener(viewBinding.root) { _, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val fabLp = viewBinding.iptvSourceFab.layoutParams as MarginLayoutParams
            fabLp.bottomMargin = systemBars.bottom + requireContext().dp2px(20)
            viewBinding.iptvSourceFab.layoutParams = fabLp
            insets
        }
    }

}