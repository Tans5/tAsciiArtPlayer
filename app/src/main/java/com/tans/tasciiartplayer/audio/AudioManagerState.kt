package com.tans.tasciiartplayer.audio

data class AudioManagerState(
    val audioIdToAudioMap: Map<Long, AudioModel> = emptyMap(),
    val allAudioList: AudioList = AudioList(AudioListType.AllAudios, emptyList()),
    val likeAudioList: AudioList = AudioList(AudioListType.LikeAudios, emptyList()),
    val albumAudioLists: List<AudioList> = emptyList(),
    val artistAudioLists: List<AudioList> = emptyList(),
    val customAudioLists: List<AudioList> = emptyList()
)