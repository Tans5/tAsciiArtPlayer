package com.tans.tasciiartplayer

import android.app.Application
import androidx.room.Room
import com.tans.tasciiartplayer.audio.audiolist.AudioListManager
import com.tans.tasciiartplayer.audio.audioplayer.AudioPlayerManager
import com.tans.tasciiartplayer.database.AppDatabase
import com.tans.tasciiartplayer.hwevent.HeadsetObserver
import com.tans.tasciiartplayer.hwevent.MediaKeyObserver
import com.tans.tasciiartplayer.hwevent.PhoneObserver
import com.tans.tasciiartplayer.iptv.IptvManager
import com.tans.tasciiartplayer.video.VideoManager
import com.tans.tuiutils.systembar.AutoApplySystemBarAnnotation

class App : Application() {

    override fun onCreate() {
        super.onCreate()
        AutoApplySystemBarAnnotation.init(this)
        val database = Room.databaseBuilder(
            context = this,
            klass = AppDatabase::class.java,
            name = AppDatabase.DATA_BASE_NAME
        ).fallbackToDestructiveMigration()
            .build()

        AppSettings.init(this)
        VideoManager.init(this, database.videoDao())
        AudioListManager.init(this, database.audioDao())
        IptvManager.init(this, database.iptvDao())
        AudioPlayerManager.init(this)
        HeadsetObserver.init(this)
        PhoneObserver.init(this)
        MediaKeyObserver.init(this)
    }
}