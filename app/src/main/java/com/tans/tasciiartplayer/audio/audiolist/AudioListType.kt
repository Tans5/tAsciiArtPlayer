package com.tans.tasciiartplayer.audio.audiolist

sealed class AudioListType {
    data object AllAudios : AudioListType()

    data object LikeAudios : AudioListType()

    data class AlbumAudios(
        val albumId: Long,
        val albumName: String
    ) : AudioListType()

    data class ArtistAudios(
        val artistId: Long,
        val artistName: String
    ) : AudioListType()

    data class CustomAudioList(
        val listId: Long,
        val listName: String,
        val listCreateTime: Long
    ) : AudioListType()
}
