package com.tans.tasciiartplayer.database

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

object AppDatabaseMigration : Migration(1, Int.MAX_VALUE) {
    override fun migrate(db: SupportSQLiteDatabase) {
        // Drop data base.
        db.execSQL("DROP DATABASE ${AppDatabase.DATA_BASE_NAME}")
    }

}