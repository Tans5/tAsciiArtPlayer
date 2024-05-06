package com.tans.tasciiartplayer.database.dao

import androidx.room.Dao
import androidx.room.Query
import com.tans.tasciiartplayer.database.entity.VideoWatchHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {

    @Query("SELECT * FROM ${VideoWatchHistory.TABLE_NAME}")
    suspend fun getAllVideoWatchHistories(): List<VideoWatchHistory>

    @Query("SELECT * FROM ${VideoWatchHistory.TABLE_NAME}")
    fun observeAllVideoWatchHistories(): Flow<List<VideoWatchHistory>>

    @Query("INSERT OR REPLACE INTO ${VideoWatchHistory.TABLE_NAME} (${VideoWatchHistory.VIDEO_ID_COLUMN}, ${VideoWatchHistory.LAST_WATCH_COLUMN}) VALUES (:videoId, :watchHistory)")
    suspend fun upsertVideoWatchHistory(videoId: Long, watchHistory: Long)

    @Query("DELETE FROM ${VideoWatchHistory.TABLE_NAME} WHERE ${VideoWatchHistory.VIDEO_ID_COLUMN} NOT IN (:activeVideoIds)")
    suspend fun deleteNotActiveHistories(activeVideoIds: Array<Long>)
}