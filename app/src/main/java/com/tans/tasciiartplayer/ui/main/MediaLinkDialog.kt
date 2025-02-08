package com.tans.tasciiartplayer.ui.main

import android.os.IBinder
import android.view.View
import android.view.inputmethod.InputMethodManager
import androidx.core.content.getSystemService
import com.tans.tasciiartplayer.R
import com.tans.tasciiartplayer.databinding.MediaLinkDialogBinding
import com.tans.tuiutils.dialog.BaseSimpleCoroutineResultCancelableDialogFragment
import com.tans.tuiutils.view.clicks

class MediaLinkDialog : BaseSimpleCoroutineResultCancelableDialogFragment<Unit, String>(Unit) {

    override val layoutId: Int = R.layout.media_link_dialog

    override fun firstLaunchInitData() {

    }

    private var editWindowToken: IBinder? = null
    override fun bindContentView(view: View) {
        val viewBinding = MediaLinkDialogBinding.bind(view)
        viewBinding.cancelBt.clicks(this) {
            onCancel()
            viewBinding.textEt.clearFocus()
        }
        viewBinding.okBt.clicks(this) {
            val text = viewBinding.textEt.text?.toString()
            if (!text.isNullOrBlank()) {
                onResult(text)
            } else {
                onCancel()
            }
            viewBinding.textEt.clearFocus()
        }
        editWindowToken = viewBinding.textEt.windowToken
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