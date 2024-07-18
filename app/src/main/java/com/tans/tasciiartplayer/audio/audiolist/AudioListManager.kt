package com.tans.tasciiartplayer.audio.audiolist

import android.app.Application
import androidx.annotation.WorkerThread
import com.tans.tasciiartplayer.AppLog
import com.tans.tasciiartplayer.appGlobalCoroutineScope
import com.tans.tasciiartplayer.database.dao.AudioDao
import com.tans.tasciiartplayer.database.entity.AudioPlaylist
import com.tans.tasciiartplayer.database.entity.AudioPlaylistCrossRef
import com.tans.tasciiartplayer.database.entity.LikeAudio
import com.tans.tasciiartplayer.glide.MediaImageModel
import com.tans.tuiutils.mediastore.queryAudioFromMediaStore
import com.tans.tuiutils.state.CoroutineState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

object AudioListManager : CoroutineState<AudioManagerState> by CoroutineState(AudioManagerState()), CoroutineScope by appGlobalCoroutineScope {

    private var application: Application? = null

    private var dao: AudioDao? = null

    fun init(application: Application, audioDao: AudioDao) {
        AudioListManager.application = application
        dao = audioDao


        // Observe database like state changes
        launch {
            audioDao.observeAllLikeAudios()
                .distinctUntilChanged()
                .collectLatest { likeAudios ->
                    runCatching {
                        val likeAudioIds = likeAudios.map { it.audioId }.toHashSet()
                        val newAudioIdToAudioMap = stateFlow.value.audioIdToAudioMap
                            .map {
                                val isLike = likeAudioIds.contains(it.key)
                                if (it.value.isLike != isLike) {
                                    it.key to it.value.copy(isLike = isLike)
                                } else {
                                    it.key to it.value
                                }
                            }.toMap()
                        fun updateListLikeState(oldList: AudioList): AudioList {
                            return oldList.copy(audios = oldList.audios.map { it.copy(isLike = likeAudioIds.contains(it.mediaStoreAudio.id)) })
                        }
                        updateState { oldState ->
                            AudioManagerState(
                                audioIdToAudioMap = newAudioIdToAudioMap,
                                allAudioList = oldState.allAudioList.copy(audios = newAudioIdToAudioMap.values.toList()),
                                likeAudioList = oldState.likeAudioList.copy(audios = likeAudios.mapNotNull { newAudioIdToAudioMap[it.audioId] }),
                                albumAudioLists = oldState.albumAudioLists.map(::updateListLikeState),
                                artistAudioLists = oldState.artistAudioLists.map(::updateListLikeState),
                                customAudioLists = oldState.customAudioLists.map(::updateListLikeState)
                            )
                        }
                    }.onSuccess {
                        AppLog.d(TAG, "Audios database like state change handle success.")
                    }.onFailure {
                        AppLog.e(TAG, "Audios database like state change handle fail: ${it.message}", it)
                    }
                }
        }

        // Observe database custom playlist change
        launch {
            audioDao.observeAllAudioPlaylist()
                .distinctUntilChanged()
                .collectLatest { playlists ->
                    runCatching {
                        updateState { oldState ->
                            oldState.copy(
                                customAudioLists = playlists
                                    .map { (playlist, refs) ->
                                        val audios =
                                            refs.mapNotNull { oldState.audioIdToAudioMap[it.audioId] }
                                        val type = AudioListType.CustomAudioList(
                                            listId = playlist.playlistId,
                                            listName = playlist.playlistName,
                                            listCreateTime = playlist.playlistCreateTime
                                        )
                                        AudioList(type, audios)
                                    }
                            )
                        }
                    }.onSuccess {
                        AppLog.d(TAG, "Audios database custom playlist change handle success.")
                    }.onFailure {
                        AppLog.e(TAG, "Audio database custom playlist change handle fail: ${it.message}", it)
                    }
                }
        }
    }



    @WorkerThread
    suspend fun refreshMediaStoreAudios() {
        val app = getApplicationOrError()
        val dao = getDaoOrError()
        val allMediaStoreAudios = app.queryAudioFromMediaStore()
        val likeAudioIds = dao.queryAllLikeAudios().map { it.audioId }.toHashSet()
        val allAudios = allMediaStoreAudios.map { mediaStoreAudio ->
            AudioModel(
                mediaStoreAudio = mediaStoreAudio,
                glideLoadModel = MediaImageModel(mediaFilePath = mediaStoreAudio.file?.canonicalPath ?: "", targetPosition = 0L, keyId = mediaStoreAudio.albumId),
                isLike = likeAudioIds.contains(mediaStoreAudio.id)
            )
        }
        val audioIdToAudioMap = allAudios.associateBy { it.mediaStoreAudio.id }

        // All
        val allAudioList = AudioList(audioListType = AudioListType.AllAudios, audios = allAudios)

        // Like
        val likeAudioList = AudioList(audioListType = AudioListType.LikeAudios, audios = allAudios.filter { it.isLike })

        // Album
        val albumAudioLists = allAudios.groupBy {
            AudioListType.AlbumAudios(
                it.mediaStoreAudio.albumId,
                it.mediaStoreAudio.album
            )
        }
            .map { AudioList(it.key, it.value) }

        // Artist
        val artistAudioLists = allAudios.groupBy {
            AudioListType.ArtistAudios(
                it.mediaStoreAudio.artistId,
                it.mediaStoreAudio.artist
            )
        }
            .map { AudioList(it.key, it.value) }

        // Custom
        val customPlaylist = dao.queryAllAudioPlaylist()
            .map { (playlist, refs) ->
                val audios = refs.mapNotNull { audioIdToAudioMap[it.audioId] }
                val type = AudioListType.CustomAudioList(
                    listId = playlist.playlistId,
                    listName = playlist.playlistName,
                    listCreateTime = playlist.playlistCreateTime
                )
                AudioList(type, audios)
            }

        updateState { state ->
            state.copy(
                audioIdToAudioMap = audioIdToAudioMap,
                allAudioList = allAudioList,
                likeAudioList = likeAudioList,
                albumAudioLists = albumAudioLists,
                artistAudioLists = artistAudioLists,
                customAudioLists = customPlaylist
            )
        }
        val activeMediaIds = allMediaStoreAudios.map { it.id }
        dao.deleteNotActiveLikeAudios(activeMediaIds)
        dao.deleteNotActivePlaylistRef(activeMediaIds)
    }

    @WorkerThread
    suspend fun likeAudio(audioId: Long) {
        val dao = getDaoOrError()
        dao.insertLikeAudio(LikeAudio(audioId = audioId, likeTime = System.currentTimeMillis()))
    }

    @WorkerThread
    suspend fun unlikeAudio(audioId: Long) {
        val dao = getDaoOrError()
        dao.deleteLikeAudio(audioId)
    }

    @WorkerThread
    suspend fun createCustomAudioList(playlistName: String) {
        val dao = getDaoOrError()
        val time = System.currentTimeMillis()
        dao.insertAudioPlaylist(
            AudioPlaylist(
                playlistId = time,
                playlistName = playlistName,
                playlistCreateTime = time
            )
        )
    }

    @WorkerThread
    suspend fun deleteCustomAudioList(playlistId: Long) {
        val dao = getDaoOrError()
        dao.deleteAudioPlaylistAndRefs(playlistId)
    }

    @WorkerThread
    suspend fun addAudioToCustomAudioList(audioId: Long, playlistId: Long) {
        val dao = getDaoOrError()
        dao.insertAudioPlaylistRef(
            AudioPlaylistCrossRef(
                audioId = audioId,
                playlistId = playlistId,
                createTime = System.currentTimeMillis()
            )
        )
    }

    @WorkerThread
    suspend fun deleteAudioFromCustomAudioList(audioId: Long, playlistId: Long) {
        val dao = getDaoOrError()
        dao.deleteAudioPlaylistRef(audioId = audioId, playlistId = playlistId)
    }

    private fun getDaoOrError(): AudioDao {
        return dao ?: error("Video dao is null.")
    }

    private fun getApplicationOrError(): Application {
        return application ?: error("Application is null.")
    }

    private const val TAG = "AudioManager"
}