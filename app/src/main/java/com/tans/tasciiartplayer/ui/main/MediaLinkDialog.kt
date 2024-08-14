package com.tans.tasciiartplayer.ui.main

import android.content.Context
import android.os.IBinder
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.content.getSystemService
import androidx.fragment.app.FragmentManager
import com.tans.tasciiartplayer.R
import com.tans.tasciiartplayer.databinding.MediaLinkDialogBinding
import com.tans.tuiutils.dialog.BaseCoroutineStateCancelableResultDialogFragment
import com.tans.tuiutils.dialog.DialogCancelableResultCallback
import com.tans.tuiutils.view.clicks
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

class MediaLinkDialog : BaseCoroutineStateCancelableResultDialogFragment<Unit, String> {


    constructor() : super(Unit , null)

    constructor(callback: DialogCancelableResultCallback<String>) : super(Unit, callback)

    override fun createContentView(context: Context, parent: ViewGroup): View {
        return LayoutInflater.from(context).inflate(R.layout.media_link_dialog, parent, false)
    }

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

suspend fun FragmentManager.showMediaLinkDialogSuspend(): String? {
    return suspendCancellableCoroutine { cont ->
        val d = MediaLinkDialog(
            callback = CoroutineDialogCancelableResultCallback(cont)
        )
        if (!coroutineShowSafe(d, "TextInputDialog#${System.currentTimeMillis()}", cont)) {
            cont.resume(null)
        }
    }
}