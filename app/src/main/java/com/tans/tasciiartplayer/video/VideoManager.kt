package com.tans.tasciiartplayer.video

import android.app.Application
import androidx.annotation.WorkerThread
import com.tans.tasciiartplayer.AppLog
import com.tans.tasciiartplayer.appGlobalCoroutineScope
import com.tans.tasciiartplayer.database.dao.VideoDao
import com.tans.tasciiartplayer.glide.MediaImageModel
import com.tans.tuiutils.mediastore.queryVideoFromMediaStore
import com.tans.tuiutils.state.CoroutineState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

object VideoManager : CoroutineState<VideoManagerState> by CoroutineState(VideoManagerState()),
    CoroutineScope by appGlobalCoroutineScope {

    private var application: Application? = null

    private var dao: VideoDao? = null

    fun init(application: Application, videoDao: VideoDao) {
        VideoManager.application = application
        dao = videoDao
        // Observe watch history change.
        launch {
            val flow = videoDao.observeAllVideoWatchHistories()
            flow
                .distinctUntilChanged()
                .collect { watchHistories ->
                val videoIdToLastWatch = watchHistories.associate { it.videoId to it.lastWatch }
                val newVideos = stateFlow.value.videos
                    .map { v ->
                        val newWatch = videoIdToLastWatch[v.mediaStoreVideo.id]
                        if (newWatch == v.lastWatch) {
                            v
                        } else {
                            val newGlideLoadModel = v.glideLoadModel.copy(targetPosition = newWatch ?: (v.mediaStoreVideo.duration / 10L))
                            v.copy(glideLoadModel = newGlideLoadModel, lastWatch = newWatch)
                        }
                    }
                updateState { it.copy(videos = newVideos) }
            }
        }
    }

    fun updateOrInsertWatchHistory(videoId: Long, watchHistory: Long) {
        val dao = getDaoOrError()
        launch {
            runCatching {
                dao.upsertVideoWatchHistory(videoId, watchHistory)
            }.onSuccess {
                AppLog.d(
                    TAG,
                    "Update or insert watch history success: videoId=$videoId, watchHistory=$watchHistory"
                )
            }.onFailure {
                AppLog.e(
                    TAG,
                    "Update or insert watch history fail: videoId=$videoId, watchHistory=$watchHistory, errorMessage=${it.message}",
                    it
                )
            }
        }
    }

    @WorkerThread
    suspend fun refreshMediaStoreVideos() {
        val context = getApplicationOrError()
        val mediaStoreVideos = context.queryVideoFromMediaStore()
        val activeVideoIds = mediaStoreVideos.map { it.id }.toTypedArray()
        val dao = getDaoOrError()
        val videoIdToLastWatch = dao.getAllVideoWatchHistories().associate { it.videoId to it.lastWatch }
        updateState { oldState ->
            val newVideos = mediaStoreVideos
                .mapNotNull { mv ->
                    if (mv.file?.canRead() == true) {
                        val lastWatch = videoIdToLastWatch[mv.id]
                        VideoModel(
                            mediaStoreVideo = mv,
                            glideLoadModel = MediaImageModel(mediaFilePath = mv.file?.canonicalPath ?: "", targetPosition = lastWatch ?: 0L, keyId = mv.id),
                            lastWatch = lastWatch
                        )
                    } else {
                        null
                    }
                }
            oldState.copy(videos = newVideos)
        }
        dao.deleteNotActiveHistories(activeVideoIds)
    }

    private fun getDaoOrError(): VideoDao {
        return dao ?: error("Video dao is null.")
    }

    private fun getApplicationOrError(): Application {
        return application ?: error("Application is null.")
    }

    private const val TAG = "VideosManager"
}