package com.tans.tasciiartplayer.ui.audioplayer

import android.content.Context
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import java.util.concurrent.ConcurrentHashMap

class ContextMultiViewAndTaskCache<Type : Any>(private val createNew: (type: Type, context: FragmentActivity, viewGroup: ViewGroup?) -> ContentViewAndTask) {

    private val caches: ConcurrentHashMap<FragmentActivity, ConcurrentHashMap<Type, ContentViewAndTask>> = ConcurrentHashMap()

    fun getFromCacheOrCreateNew(type: Type, context: Context, viewGroup: ViewGroup?): ContentViewAndTask? {
        return if (context is FragmentActivity && !context.isDestroyed && !context.isFinishing) {
            val cacheMap = caches.getOrPut(context) { ConcurrentHashMap() }
            val cache = cacheMap[type]
            if (cache != null) {
                cache
            } else {
                val new = createNew(type, context, viewGroup)
                cacheMap.putIfAbsent(type, new).let {
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
            if (toRemove != null) {
                for ((_, v) in toRemove) {
                    v.task.cancel()
                }
            }
        }
    }
}