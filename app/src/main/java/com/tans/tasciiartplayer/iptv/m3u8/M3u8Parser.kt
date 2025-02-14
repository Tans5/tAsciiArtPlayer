package com.tans.tasciiartplayer.iptv.m3u8

import com.tans.tasciiartplayer.AppLog
import com.tans.tasciiartplayer.iptv.m3u8.model.M3u8
import com.tans.tasciiartplayer.iptv.m3u8.model.M3u8Extinf

private const val START_TAG = "#EXTM3U"

private const val LOG_TAG = "M3u8Parser"

fun String.parseAsM3u8(): M3u8 {
    val lines = this.lines().map { it.trim() }.filter { it.isNotBlank() }
    if (lines.isEmpty()) {
        error("Empty m3u8 text.")
    }
    val firstLine = lines[0]
    if (!firstLine.startsWith(START_TAG)) {
        error("$this is not m3u8 format text.")
    }
    var readIndex = 1
    val extifs: MutableList<M3u8Extinf> = mutableListOf()
    var waitingSourceUrlExtinf: String? = null
    while (readIndex < lines.size) {
        val l = lines[readIndex]
        if (l[0] == '#') {
            waitingSourceUrlExtinf = null
            if (l.startsWith(M3u8Extinf.TAG)) {
                val startIndex = l.indexOf(':')
                if (startIndex == -1) {
                    error("Wrong EXTINF line: $l")
                } else {
                    waitingSourceUrlExtinf = l.substring(startIndex + 1)
                }
            } else {
                AppLog.d(LOG_TAG, "Unknown line: $l")
            }
        } else {
            val waitingExtinf = waitingSourceUrlExtinf
            if (waitingExtinf != null) {
                waitingSourceUrlExtinf = null
                val splitIndex = waitingExtinf.indexOfLast { it == ',' }
                if (splitIndex == -1) {
                    error("Wrong EXTINF params: $splitIndex")
                }
                val durationText = waitingExtinf.substring(0, splitIndex).trim()
                val titleText = waitingExtinf.substring(splitIndex + 1).trim()
                val durationAsKeyValues: MutableMap<String, String> = mutableMapOf()
                for (segment in durationText.split(' ')) {
                    if (segment.contains('=')) {
                        val keyAndValue = segment.split('=')
                        val key = keyAndValue[0]
                        val value = keyAndValue.getOrNull(1)?.let {
                            if (it.startsWith('"') && it.endsWith('"')) {
                                it.substring(1, it.length - 1)
                            } else {
                                it
                            }
                        } ?: ""
                        durationAsKeyValues[key] = value
                    }
                }
                extifs.add(M3u8Extinf(
                    durationText = durationText,
                    titleText = titleText,
                    durationAsKeyValues = durationAsKeyValues,
                    sourceUrl = l
                ))
            } else {
                AppLog.d(LOG_TAG, "Unknown line: $l")
            }
        }

        readIndex ++
    }
    return M3u8(extinfs = extifs)
}