package com.tans.tasciiartplayer.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.tans.tasciiartplayer.database.dao.VideoDao
import com.tans.tasciiartplayer.database.entity.VideoWatchHistory

@Database(entities = [VideoWatchHistory::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun videoDao(): VideoDao

    companion object {
        const val DATA_BASE_NAME = "tAsciiArtPlayer"
    }
}