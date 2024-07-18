package com.tans.tasciiartplayer.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.tans.tasciiartplayer.database.entity.VideoWatchHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface VideoDao {

    @Query("SELECT * FROM ${VideoWatchHistory.TABLE_NAME}")
    suspend fun getAllVideoWatchHistories(): List<VideoWatchHistory>

    @Query("SELECT * FROM ${VideoWatchHistory.TABLE_NAME}")
    fun observeAllVideoWatchHistories(): Flow<List<VideoWatchHistory>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVideoWatchHistory(history: VideoWatchHistory)

    @Query("DELETE FROM ${VideoWatchHistory.TABLE_NAME} WHERE ${VideoWatchHistory.VIDEO_ID_COLUMN} NOT IN (:activeVideoIds)")
    suspend fun deleteNotActiveHistories(activeVideoIds: Array<Long>)
}