package com.tans.tasciiartplayer.ui.main

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.tans.tasciiartplayer.R
import com.tans.tasciiartplayer.databinding.OptionalDialogLayoutBinding
import com.tans.tuiutils.dialog.BaseCoroutineStateCancelableResultDialogFragment
import com.tans.tuiutils.dialog.DialogCancelableResultCallback
import com.tans.tuiutils.view.clicks
import kotlinx.coroutines.suspendCancellableCoroutine

class OptionalDialog : BaseCoroutineStateCancelableResultDialogFragment<Unit, Boolean> {

    private val title: String?
    private val message: String?
    private val positiveButtonText: String?
    private val negativeButtonText: String?
    constructor() : super(Unit, null) {
        title = null
        message = null
        positiveButtonText = null
        negativeButtonText = null
    }

    constructor(
        title: String,
        message: String,
        positiveButtonText: String,
        negativeButtonText: String,
        callback: DialogCancelableResultCallback<Boolean>) : super(Unit, callback) {
        this.title = title
        this.message = message
        this.positiveButtonText = positiveButtonText
        this.negativeButtonText = negativeButtonText
    }

    override fun createContentView(context: Context, parent: ViewGroup): View {
        return LayoutInflater.from(context).inflate(R.layout.optional_dialog_layout, parent, false)
    }

    override fun firstLaunchInitData() {

    }

    override fun bindContentView(view: View) {
        val viewBinding = OptionalDialogLayoutBinding.bind(view)
        viewBinding.titleTv.text = title ?: ""
        viewBinding.messageTv.text = message ?: ""
        viewBinding.positiveButton.text = positiveButtonText ?: ""
        viewBinding.negativeButton.text = negativeButtonText ?: ""

        viewBinding.positiveButton.clicks(this) {
            onResult(true)
        }
        viewBinding.negativeButton.clicks(this) {
            onResult(false)
        }
    }
}

suspend fun FragmentManager.showOptionalDialogSuspend(
    title: String,
    message: String,
    positiveButtonText: String,
    negativeButtonText: String
): Boolean? {
    return suspendCancellableCoroutine { cont ->
        val d = OptionalDialog(
            title = title,
            message = message,
            positiveButtonText = positiveButtonText,
            negativeButtonText = negativeButtonText,
            callback = CoroutineDialogCancelableResultCallback(cont)
        )
        coroutineShowSafe(d, "OptionalDialog#${System.currentTimeMillis()}", cont)
    }
}