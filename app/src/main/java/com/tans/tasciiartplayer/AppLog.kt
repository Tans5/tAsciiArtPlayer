package com.tans.tasciiartplayer

import android.util.Log

object AppLog {
    fun d(tag: String, msg: String) {
        Log.d(tag, msg)
    }

    fun e(tag: String, msg: String, e: Throwable? = null) {
        Log.e(tag, msg, e)
    }
}