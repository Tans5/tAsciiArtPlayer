package com.tans.tasciiartplayer

import android.app.Application
import com.tans.tlog.LogLevel
import com.tans.tlog.tLog
import java.io.File

object AppLog {

    @Volatile private var log: tLog? = null

    fun init(app: Application) {
        log = tLog.Companion.Builder(File(app.getExternalFilesDir(null), "AppLog"))
            .setMaxSize(1024L * 1024L * 30L) // 30MB
            .setLogFilterLevel(if (BuildConfig.DEBUG) LogLevel.Debug else LogLevel.Error)
            .build()
    }

    fun d(tag: String, msg: String) {
        log?.d(tag, msg)
    }

    fun w(tag: String, msg: String) {
        log?.w(tag, msg)
    }

    fun e(tag: String, msg: String, e: Throwable? = null) {
        log?.e(tag, msg, e)
    }
}