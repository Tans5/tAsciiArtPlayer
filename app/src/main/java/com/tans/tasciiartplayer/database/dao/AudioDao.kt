package com.tans.tasciiartplayer.database.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.tans.tasciiartplayer.database.entity.AudioPlaylist
import com.tans.tasciiartplayer.database.entity.AudioPlaylistCrossRef
import com.tans.tasciiartplayer.database.entity.LikeAudio
import kotlinx.coroutines.flow.Flow

@Dao
interface AudioDao {

    @Query("SELECT * FROM ${AudioPlaylist.TABLE_NAME} LEFT JOIN ${AudioPlaylistCrossRef.TABLE_NAME} ON ${AudioPlaylist.TABLE_NAME}.${AudioPlaylist.PLAYLIST_ID_COLUMN} = ${AudioPlaylistCrossRef.TABLE_NAME}.${AudioPlaylistCrossRef.PLAYLIST_ID_COLUMN} ORDER BY ${AudioPlaylist.PLAYLIST_CREATE_TIME_COLUMN}, ${AudioPlaylistCrossRef.CREATE_TIME_COLUMN}")
    suspend fun queryAllAudioPlaylist(): Map<AudioPlaylist, List<AudioPlaylistCrossRef>>

    @Query("SELECT * FROM ${AudioPlaylist.TABLE_NAME} LEFT JOIN ${AudioPlaylistCrossRef.TABLE_NAME} ON ${AudioPlaylist.TABLE_NAME}.${AudioPlaylist.PLAYLIST_ID_COLUMN} = ${AudioPlaylistCrossRef.TABLE_NAME}.${AudioPlaylistCrossRef.PLAYLIST_ID_COLUMN} ORDER BY ${AudioPlaylist.PLAYLIST_CREATE_TIME_COLUMN}, ${AudioPlaylistCrossRef.CREATE_TIME_COLUMN}")
    fun observeAllAudioPlaylist(): Flow<Map<AudioPlaylist, List<AudioPlaylistCrossRef>>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAudioPlaylist(playlist: AudioPlaylist)

    @Query("DELETE FROM ${AudioPlaylist.TABLE_NAME} WHERE ${AudioPlaylist.PLAYLIST_ID_COLUMN} = :playlistId")
    suspend fun deleteAudioPlaylist(playlistId: Long)

    @Query("DELETE FROM ${AudioPlaylistCrossRef.TABLE_NAME} WHERE ${AudioPlaylistCrossRef.PLAYLIST_ID_COLUMN} = :playlistId")
    suspend fun deleteAudioPlaylistInRef(playlistId: Long)

    @Transaction
    suspend fun deleteAudioPlaylistAndRefs(playlistId: Long) {
        deleteAudioPlaylist(playlistId)
        deleteAudioPlaylistInRef(playlistId)
    }

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAudioPlaylistRef(ref: AudioPlaylistCrossRef)

    @Query("DELETE FROM ${AudioPlaylistCrossRef.TABLE_NAME} WHERE ${AudioPlaylistCrossRef.PLAYLIST_ID_COLUMN} = :playlistId AND ${AudioPlaylistCrossRef.AUDIO_ID_COLUMN} = :audioId")
    suspend fun deleteAudioPlaylistRef(playlistId: Long, audioId: Long)

    @Query("DELETE FROM ${AudioPlaylistCrossRef.TABLE_NAME} WHERE ${AudioPlaylistCrossRef.AUDIO_ID_COLUMN} NOT IN (:activeAudioIds)")
    suspend fun deleteNotActivePlaylistRef(activeAudioIds: List<Long>)

    @Query("SELECT * FROM ${LikeAudio.TABLE_NAME}")
    suspend fun queryAllLikeAudios(): List<LikeAudio>

    @Query("SELECT * FROM ${LikeAudio.TABLE_NAME}")
    fun observeAllLikeAudios(): Flow<List<LikeAudio>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertLikeAudio(likeAudio: LikeAudio)

    @Query("DELETE FROM ${LikeAudio.TABLE_NAME} WHERE ${LikeAudio.AUDIO_ID_COLUMN} = :audioId")
    suspend fun deleteLikeAudio(audioId: Long)

    @Query("DELETE FROM ${LikeAudio.TABLE_NAME} WHERE ${LikeAudio.AUDIO_ID_COLUMN} NOT IN (:activeAudioIds)")
    suspend fun deleteNotActiveLikeAudios(activeAudioIds: List<Long>)

}