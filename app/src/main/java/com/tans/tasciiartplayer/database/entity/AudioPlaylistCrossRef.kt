package com.tans.tasciiartplayer.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(tableName = AudioPlaylistCrossRef.TABLE_NAME, primaryKeys = [AudioPlaylistCrossRef.PLAYLIST_ID_COLUMN, AudioPlaylistCrossRef.AUDIO_ID_COLUMN])
data class AudioPlaylistCrossRef(
    @ColumnInfo(PLAYLIST_ID_COLUMN)
    val playlistId: Long,
    @ColumnInfo(AUDIO_ID_COLUMN)
    val audioId: Long,
    @ColumnInfo(CREATE_TIME_COLUMN)
    val createTime: Long
) {
    companion object {
        const val TABLE_NAME = "audio_playlist_cross_ref"
        const val PLAYLIST_ID_COLUMN = "playlist_id"
        const val AUDIO_ID_COLUMN = "audio_id"
        const val CREATE_TIME_COLUMN = "create_time"
    }
}