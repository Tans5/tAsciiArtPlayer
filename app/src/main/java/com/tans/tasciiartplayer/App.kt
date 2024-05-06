package com.tans.tasciiartplayer

import android.app.Application
import androidx.room.Room
import com.tans.tasciiartplayer.database.AppDatabase
import com.tans.tasciiartplayer.database.AppDatabaseMigration
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

        VideosManager.init(this, database.videoDao())
    }
}