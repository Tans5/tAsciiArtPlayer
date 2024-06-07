package com.tans.tasciiartplayer.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = AudioPlaylist.TABLE_NAME)
data class AudioPlaylist(
    @PrimaryKey
    @ColumnInfo(name = PLAYLIST_ID_COLUMN)
    val playlistId: Long,
    @ColumnInfo(name = PLAYLIST_NAME_COLUMN)
    val playlistName: String,
    @ColumnInfo(name = PLAYLIST_CREATE_TIME_COLUMN)
    val playlistCreateTime: Long
) {
    companion object {
        const val TABLE_NAME = "audio_playlist"
        const val PLAYLIST_ID_COLUMN = "playlist_id"
        const val PLAYLIST_NAME_COLUMN = "playlist_name"
        const val PLAYLIST_CREATE_TIME_COLUMN = "playlist_create_time"
    }
}