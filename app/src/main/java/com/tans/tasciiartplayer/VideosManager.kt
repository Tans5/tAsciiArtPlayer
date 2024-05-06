package com.tans.tasciiartplayer

import android.app.Application
import com.tans.tasciiartplayer.database.dao.VideoDao
import com.tans.tuiutils.state.CoroutineState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

object VideosManager : CoroutineState<Unit> by CoroutineState<Unit>(Unit),
    CoroutineScope by appGlobalCoroutineScope {

    private var application: Application? = null

    private var dao: VideoDao? = null

    fun init(application: Application, videoDao: VideoDao) {
        this.application = application
        this.dao = videoDao
    }

    fun updateOrInsertWatchHistory(videoId: Long, watchHistory: Long) {
        val dao = getDaoOrError()
        launch {
            runCatching {
                dao.upsertVideoWatchHistory(videoId, watchHistory)
            }.onSuccess {
                AppLog.d(TAG, "Update or insert watch history success: videoId=$videoId, watchHistory=$watchHistory")
            }.onFailure {
                AppLog.e(TAG, "Update or insert watch history fail: videoId=$videoId, watchHistory=$watchHistory, errorMessage=${it.message}", it)
            }
        }
    }

    private fun getDaoOrError(): VideoDao {
        return dao ?: error("Video dao is null.")
    }

    private fun getApplicationOrError(): Application {
        return application ?: error("Application is null.")
    }

    private const val TAG = "VideosManager"
}