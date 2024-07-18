package com.tans.tasciiartplayer.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = LikeAudio.TABLE_NAME)
data class LikeAudio(
    @PrimaryKey
    @ColumnInfo(AUDIO_ID_COLUMN)
    val audioId: Long,
    @ColumnInfo(AUDIO_LIKE_TIME_COLUMN)
    val likeTime: Long
) {
    companion object {
        const val TABLE_NAME = "like_audio"
        const val AUDIO_ID_COLUMN = "audio_id"
        const val AUDIO_LIKE_TIME_COLUMN = "like_time"
    }
}