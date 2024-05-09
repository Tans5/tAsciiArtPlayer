package com.tans.tasciiartplayer

import android.app.Application
import androidx.room.Room
import com.tans.tasciiartplayer.database.AppDatabase
import com.tans.tasciiartplayer.database.AppDatabaseMigration
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
        ).addMigrations(AppDatabaseMigration)
            .build()

        VideoManager.init(this, database.videoDao())

        AppSettings.init(this)
    }
}