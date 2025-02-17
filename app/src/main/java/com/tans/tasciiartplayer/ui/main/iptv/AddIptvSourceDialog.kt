package com.tans.tasciiartplayer.ui.main.iptv

import android.os.IBinder
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.widget.addTextChangedListener
import com.tans.tasciiartplayer.R
import com.tans.tasciiartplayer.database.entity.IptvSource
import com.tans.tasciiartplayer.databinding.AddIptvSourceDialogBinding
import com.tans.tasciiartplayer.iptv.IptvManager
import com.tans.tuiutils.dialog.BaseCoroutineStateDialogFragment
import com.tans.tuiutils.view.clicks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AddIptvSourceDialog : BaseCoroutineStateDialogFragment<Unit>(Unit) {
    override val layoutId: Int = R.layout.add_iptv_source_dialog

    private var editWindowToken: IBinder? = null

    override fun firstLaunchInitData() { }

    override fun bindContentView(view: View) {
        val viewBinding = AddIptvSourceDialogBinding.bind(view)

        viewBinding.cancelBt.clicks(this) {
            dismissAllowingStateLoss()
        }

        var isNameInputError = false
        var isLinkInputError = false
        viewBinding.okBt.clicks(this) {
            val name = viewBinding.nameInputEt.text?.toString()
            val link = viewBinding.linkInputEt.text?.toString()
            if (name.isNullOrBlank()) {
                isNameInputError = true
                viewBinding.nameInputLayout.error = ContextCompat.getString(requireContext(), R.string.add_iptv_source_dialog_input_name_error)
                return@clicks
            }
            if (link.isNullOrBlank()) {
                isLinkInputError = true
                viewBinding.linkInputLayout.error = ContextCompat.getString(requireContext(), R.string.add_iptv_source_dialog_input_link_error)
                return@clicks
            }
            withContext(Dispatchers.IO) {
                IptvManager.insertIptvSource(
                    IptvSource(
                        createTime = System.currentTimeMillis(),
                        title = name,
                        sourceUrl = link
                    )
                )
            }
            dismissSafe()
        }

        viewBinding.nameInputEt.addTextChangedListener {
            if (isNameInputError && !it.isNullOrBlank()) {
                isNameInputError = false
                viewBinding.nameInputLayout.error = null
            }
        }
        viewBinding.linkInputEt.addTextChangedListener {
            if (isLinkInputError && !it.isNullOrBlank()) {
                isLinkInputError = false
                viewBinding.linkInputLayout.error = null
            }
        }
        editWindowToken = viewBinding.nameInputEt.windowToken
    }

    override fun onDestroy() {
        super.onDestroy()
        val ctx = context
        val windowToken = this.editWindowToken
        if (ctx != null && windowToken != null) {
            val inputMethodManager = ctx.getSystemService<InputMethodManager>()
            inputMethodManager?.hideSoftInputFromWindow(windowToken, 0)
        }
    }

}