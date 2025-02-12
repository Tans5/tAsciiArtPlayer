package com.tans.tasciiartplayer.m3u8.model

data class M3u8Extinf(
    val durationText: String,
    val durationAsKeyValues: Map<String, String>,
    val titleText: String,
    val sourceUrl: String
) {

    companion object {
        const val TAG = "#EXTINF"
    }
}