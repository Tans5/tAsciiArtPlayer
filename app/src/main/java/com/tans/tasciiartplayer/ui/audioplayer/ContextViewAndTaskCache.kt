package com.tans.tasciiartplayer.ui.audioplayer

import android.content.Context
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.ConcurrentHashMap

class ContextViewAndTaskCache(
    private val createNew: (context: FragmentActivity, viewGroup: ViewGroup?) -> ContentViewAndTask
) {

    private val caches: ConcurrentHashMap<FragmentActivity, ContentViewAndTask> = ConcurrentHashMap()

    fun getFromCacheOrCreateNew(context: Context, viewGroup: ViewGroup?): ContentViewAndTask? {
        return if (context is FragmentActivity && !context.isDestroyed && !context.isFinishing) {
            val cache = caches[context]
            if (cache != null) {
                cache
            } else {
                val new = createNew(context, viewGroup)
                caches.putIfAbsent(context, new).let {
                    if (it != null) {
                        new.task.cancel()
                        it
                    } else {
                        new
                    }
                }
            }
        } else {
            null
        }
    }

    fun removeCache(context: Context) {
        if (context is FragmentActivity) {
            val toRemove = caches.remove(context)
            toRemove?.task?.cancel()
        }
    }
}