package com.tans.tasciiartplayer.database

import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.RoomDatabase
import com.tans.tasciiartplayer.database.dao.AudioDao
import com.tans.tasciiartplayer.database.dao.IptvDao
import com.tans.tasciiartplayer.database.dao.VideoDao
import com.tans.tasciiartplayer.database.entity.AudioPlaylist
import com.tans.tasciiartplayer.database.entity.AudioPlaylistCrossRef
import com.tans.tasciiartplayer.database.entity.IptvSource
import com.tans.tasciiartplayer.database.entity.LikeAudio
import com.tans.tasciiartplayer.database.entity.VideoWatchHistory

@Database(
    entities = [VideoWatchHistory::class, AudioPlaylist::class, AudioPlaylistCrossRef::class, LikeAudio::class, IptvSource::class],
    version = 5,
    autoMigrations = [
        AutoMigration(from = 4, to = 5)
    ]
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun videoDao(): VideoDao

    abstract fun audioDao(): AudioDao

    abstract fun iptvDao(): IptvDao

    companion object {
        const val DATA_BASE_NAME = "tAsciiArtPlayer"
    }
}