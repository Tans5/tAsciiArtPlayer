package com.tans.tasciiartplayer.audio.audiolist

data class AudioList(
    val audioListType: AudioListType,
    val audios: List<AudioModel>
)
