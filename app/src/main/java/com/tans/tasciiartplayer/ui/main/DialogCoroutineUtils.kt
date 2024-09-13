package com.tans.tasciiartplayer.ui.main

import androidx.fragment.app.FragmentManager
import com.tans.tuiutils.dialog.BaseDialogFragment
import com.tans.tuiutils.dialog.DialogCancelableResultCallback
import com.tans.tuiutils.dialog.DialogForceResultCallback
import kotlinx.coroutines.CancellableContinuation
import java.lang.ref.WeakReference
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class CoroutineDialogCancelableResultCallback<T : Any>(
    private val cont: CancellableContinuation<T?>
) : DialogCancelableResultCallback<T> {
    override fun onCancel() {
        if (cont.isActive) {
            cont.resume(null)
        }
    }

    override fun onResult(t: T) {
        if (cont.isActive) {
            cont.resume(t)
        }
    }

}

class CoroutineDialogForceResultCallback<T : Any>(
    private val cont: CancellableContinuation<T>
) : DialogForceResultCallback<T> {

    override fun onResult(t: T) {
        if (cont.isActive) {
            cont.resume(t)
        }
    }

    override fun onError(e: String) {
        if (cont.isActive) {
            cont.resumeWithException(Throwable(e))
        }
    }

}

fun FragmentManager.coroutineShowSafe(dialog: BaseDialogFragment, tag: String, cont: CancellableContinuation<*>): Boolean {
    return if (!isDestroyed) {
        dialog.show(this, tag)
        val wd = WeakReference(dialog)
        cont.invokeOnCancellation {
            if (!isDestroyed) {
                wd.get()?.dismissSafe()
            }
        }
        true
    } else {
        false
    }
}