package com.tans.tasciiartplayer.ui.main.iptv

import android.os.IBinder
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.core.content.getSystemService
import androidx.core.widget.addTextChangedListener
import com.tans.tasciiartplayer.R
import com.tans.tasciiartplayer.database.entity.IptvSource
import com.tans.tasciiartplayer.databinding.ModifyIptvSourceDialogBinding
import com.tans.tasciiartplayer.iptv.IptvManager
import com.tans.tuiutils.dialog.BaseCoroutineStateDialogFragment
import com.tans.tuiutils.view.clicks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class ModifyIptvSourceDialog(private val toModify: IptvSource) : BaseCoroutineStateDialogFragment<Unit>(Unit) {

    override val layoutId: Int = R.layout.modify_iptv_source_dialog

    private var editWindowToken: IBinder? = null

    override fun firstLaunchInitData() { }

    override fun bindContentView(view: View) {
        val viewBinding = ModifyIptvSourceDialogBinding.bind(view)

        viewBinding.cancelBt.clicks(this) {
            dismissAllowingStateLoss()
        }

        viewBinding.nameInputEt.setText(toModify.title)
        viewBinding.linkInputEt.setText(toModify.sourceUrl)

        var isNameInputError = false
        var isLinkInputError = false
        viewBinding.okBt.clicks(this) {
            val name = viewBinding.nameInputEt.text?.toString()
            val link = viewBinding.linkInputEt.text?.toString()
            if (name.isNullOrBlank()) {
                isNameInputError = true
                viewBinding.nameInputLayout.error = ContextCompat.getString(requireContext(), R.string.modify_iptv_source_dialog_input_name_error)
                return@clicks
            }
            if (link.isNullOrBlank()) {
                isLinkInputError = true
                viewBinding.linkInputLayout.error = ContextCompat.getString(requireContext(), R.string.modify_iptv_source_dialog_input_link_error)
                return@clicks
            }
            if (name != toModify.title || link != toModify.sourceUrl) {
                withContext(Dispatchers.IO) {
                    IptvManager.updateIptvSource(
                        IptvSource(
                            createTime = toModify.createTime,
                            title = name,
                            sourceUrl = link
                        )
                    )
                }
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