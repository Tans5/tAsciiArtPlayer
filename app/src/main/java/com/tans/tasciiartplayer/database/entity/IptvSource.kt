package com.tans.tasciiartplayer.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.tans.tasciiartplayer.database.entity.IptvSource.Companion.TABLE_NAME

@Entity(tableName = TABLE_NAME)
data class IptvSource(
    @PrimaryKey
    @ColumnInfo(CREATE_TIME_COLUMN)
    val createTime: Long,
    @ColumnInfo(TITLE_COLUMN)
    val title: String,
    @ColumnInfo(SOURCE_URL_COLUMN)
    val sourceUrl: String
) {

    companion object {
        const val TABLE_NAME = "iptv_source"
        const val CREATE_TIME_COLUMN = "create_time"
        const val TITLE_COLUMN = "title"
        const val SOURCE_URL_COLUMN = "source_url"
    }
}