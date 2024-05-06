package com.tans.tasciiartplayer.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tans.tasciiartplayer.database.entity.VideoWatchHistory.Companion.TABLE_NAME

@Entity(tableName = TABLE_NAME)
data class VideoWatchHistory(
    @PrimaryKey
    @ColumnInfo(name = VIDEO_ID_COLUMN)
    val videoId: Long,
    @ColumnInfo(name = LAST_WATCH_COLUMN)
    val lastWatch: Long
) {

    companion object {
        const val TABLE_NAME = "video_watch_history"
        const val VIDEO_ID_COLUMN = "video_id"
        const val LAST_WATCH_COLUMN = "last_watch"
    }
}
