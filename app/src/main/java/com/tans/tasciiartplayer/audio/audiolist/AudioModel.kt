package com.tans.tasciiartplayer.audio.audiolist

import com.tans.tasciiartplayer.glide.MediaImageModel
import com.tans.tuiutils.mediastore.MediaStoreAudio

data class AudioModel(
    val mediaStoreAudio: MediaStoreAudio,
    val glideLoadModel: MediaImageModel,
    val isLike: Boolean
)
