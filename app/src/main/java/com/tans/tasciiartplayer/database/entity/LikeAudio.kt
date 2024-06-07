package com.tans.tasciiartplayer.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = LikeAudio.TABLE_NAME)
data class LikeAudio(
    @PrimaryKey
    @ColumnInfo(AUDIO_ID_COLUMN)
    val audioId: Long
) {
    companion object {
        const val TABLE_NAME = "like_audio"
        const val AUDIO_ID_COLUMN = "audio_id"
    }
}