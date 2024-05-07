package com.tans.tasciiartplayer.video

import com.tans.tasciiartplayer.glide.MediaImageModel
import com.tans.tuiutils.mediastore.MediaStoreVideo

data class VideoModel(
    val mediaStoreVideo: MediaStoreVideo,
    val glideLoadModel: MediaImageModel,
    val lastWatch: Long?
)