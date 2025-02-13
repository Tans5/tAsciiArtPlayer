package com.tans.tasciiartplayer.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.tans.tasciiartplayer.database.entity.IptvSource
import kotlinx.coroutines.flow.Flow

@Dao
interface IptvDao {

    @Query("SELECT * FROM ${IptvSource.TABLE_NAME}")
    suspend fun queryAllIptvSource(): List<IptvSource>

    @Query("SELECT * FROM ${IptvSource.TABLE_NAME}")
    fun observeAllIpTvSource(): Flow<List<IptvSource>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIptvSource(iptvSource: IptvSource)

    @Query("DELETE FROM ${IptvSource.TABLE_NAME} WHERE ${IptvSource.CREATE_TIME_COLUMN} = :createTime")
    suspend fun deleteIptvSource(createTime: Long)

    @Update
    suspend fun updateIptvSource(iptvSource: IptvSource)
}