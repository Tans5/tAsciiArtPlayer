package com.tans.tasciiartplayer.ui.main

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentManager
import com.tans.tasciiartplayer.AppLog
import com.tans.tasciiartplayer.R
import com.tans.tasciiartplayer.audio.audiolist.AudioListManager
import com.tans.tasciiartplayer.databinding.VideoAudioSearchDialogBinding
import com.tans.tasciiartplayer.video.VideoManager
import com.tans.tuiutils.dialog.BaseCoroutineStateCancelableResultDialogFragment
import com.tans.tuiutils.dialog.DialogCancelableResultCallback
import com.tans.tuiutils.view.clicks
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume
import kotlin.math.max
import kotlin.system.measureTimeMillis

class VideoAudioSearchDialog : BaseCoroutineStateCancelableResultDialogFragment<Unit, Pair<Int, Int>> {

    constructor() : super(Unit, null)

    constructor(callback: DialogCancelableResultCallback<Pair<Int, Int>>) : super(Unit, callback)

    override fun createContentView(context: Context, parent: ViewGroup): View {
        return LayoutInflater.from(context).inflate(R.layout.video_audio_search_dialog, parent, false)
    }

    override fun firstLaunchInitData() {
        launch(Dispatchers.IO) {
            if (VideoManager.stateFlow.value.videos.isEmpty()) {
                kotlin.runCatching {
                    VideoManager.refreshMediaStoreVideos()
                }
            }
            if (AudioListManager.stateFlow.value.audioIdToAudioMap.isEmpty()) {
                kotlin.runCatching {
                    AudioListManager.refreshMediaStoreAudios()
                }
            }
            val beforeUpdateVideos = VideoManager.stateFlow.value.videos.mapNotNull { it.mediaStoreVideo.file?.canonicalPath }.toHashSet()
            val beforeUpdateAudios = AudioListManager.stateFlow.value.audioIdToAudioMap.mapNotNull { it.value.mediaStoreAudio.file?.canonicalPath }.toHashSet()
            val foundNewVideos = mutableListOf<Pair<String, String>>()
            val foundNewAudios = mutableListOf<Pair<String, String>>()
            val scanFileCost = measureTimeMillis {
                scanFiles(Environment.getExternalStorageDirectory()) { f ->
                    if (!beforeUpdateVideos.contains(f.canonicalPath) && !beforeUpdateAudios.contains(f.canonicalPath)) {
                        val mimeTypeAndMediaType = getMediaMimeTypeWithFileName(f.name)
                        if (mimeTypeAndMediaType != null) {
                            when (mimeTypeAndMediaType.second) {
                                MediaType.Video -> foundNewVideos.add(f.canonicalPath to mimeTypeAndMediaType.first)
                                MediaType.Audio -> foundNewAudios.add(f.canonicalPath to mimeTypeAndMediaType.first)
                            }
                        }
                    }
                }
            }
            AppLog.d(TAG, "Scan files cost: $scanFileCost ms, found ${foundNewVideos.size} new video files and ${foundNewAudios.size} new audio files")
            if ((foundNewVideos.isNotEmpty() || foundNewAudios.isNotEmpty()) && isActive) {
                measureTimeMillis {
                    kotlin.runCatching {
                        withTimeout(5000L) {
                            insertToMediaStore(foundNewVideos + foundNewAudios)
                        }
                    }.onFailure {
                        AppLog.e(TAG, "Insert to media store fail: ${it.message}", it)
                    }
                }.let {
                    AppLog.d(TAG, "Insert to media store cost: $it ms.")
                }
                kotlin.runCatching {
                    if (foundNewVideos.isNotEmpty()) {
                        VideoManager.refreshMediaStoreVideos()
                    }
                    if (foundNewAudios.isNotEmpty()) {
                        AudioListManager.refreshMediaStoreAudios()
                    }
                }
                val insertVideoSize = VideoManager.currentState().videos.size - beforeUpdateVideos.size
                val insertAudioSize = AudioListManager.currentState().audioIdToAudioMap.size - beforeUpdateAudios.size
                onResult(max(0, insertVideoSize) to max(0, insertAudioSize))
            } else {
                AppLog.d(TAG, "Do not find new video and audio.")
                onResult(0 to 0)
            }

        }
    }

    override fun bindContentView(view: View) {
        val viewBinding = VideoAudioSearchDialogBinding.bind(view)
        viewBinding.cancelButton.clicks(this) {
            onCancel()
        }
    }

    private fun scanFiles(file: File, handle: (f: File) -> Unit) {
        if (file.canRead() && this.isActive) {
            if (file.isFile) {
                handle(file.canonicalFile)
            } else {
                val children = file.listFiles() ?: emptyArray<File>()
                for (c in children) {
                    scanFiles(c, handle)
                }
            }
        }
    }

    private suspend fun insertToMediaStore(fileAndMimeType: List<Pair<String, String>>) {
        return suspendCancellableCoroutine { cont ->
            val insertCount = AtomicInteger(0)
            MediaScannerConnection.scanFile(
                requireContext(),
                fileAndMimeType.map { it.first }.toTypedArray(),
                fileAndMimeType.map { it.second }.toTypedArray()
            ) { file, _ ->
                val size = insertCount.addAndGet(1)
                AppLog.d(TAG, "Save file to media store, file=$file, size=$size")
                if (size >= fileAndMimeType.size) {
                    AppLog.d(TAG, "Save file to media store finished.")
                    if (cont.isActive) {
                        cont.resume(Unit)
                    }
                }
            }
        }
    }

    companion object {

        private const val TAG = "VideoAudioSearchDialog"

        private val mediaFileSuffixAndMimeType: Map<String, String> by lazy {
            mapOf(
                "aac" to "audio/aac",
                "avi" to "video/x-msvideo",
                "dv" to "video/x-dv",
                "mid" to "audio/midi",
                "midi" to "audio/midi",
                "mp3" to "audio/mpeg",
                "mp4" to "video/mp4",
                "mp4a" to "audio/mp4",
                "mpeg" to "audio/mpeg",
                "mpg" to "audio/mpeg",
                "mov" to "video/quicktime",
                "mpeg" to "video/mpeg",
                "oga" to "audio/ogg",
                "ogv" to "video/ogg",
                "wav" to "audio/wav",
                "wama" to "audio/x-ms-wma",
                "weba" to "audio/webm",
                "webm" to "video/webm",
                "wm" to "video/x-ms-wmv",
                "flv" to "video/x-flv",
                "mkv" to "video/x-matroska",
                "3gp" to "video/3gp",
                "3g2" to "video/3g2",
                "flac" to "audio/flac"
            )
        }

        private enum class MediaType {
            Audio,
            Video,
        }

        private val fileSuffixRegex by lazy {
            ".*\\.(.+)$".toRegex()
        }

        /**
         * @return Media file Mimetype and MediaType
         */
        private fun getMediaMimeTypeWithFileName(fileName: String): Pair<String, MediaType>? {
            val matchResult = fileSuffixRegex.find(fileName)
            return matchResult?.groupValues?.get(1)?.lowercase(Locale.US).let { suffix ->
                val mimeType = mediaFileSuffixAndMimeType[suffix]
                when {
                    mimeType?.startsWith("audio") == true -> mimeType to MediaType.Audio
                    mimeType?.startsWith("video") == true -> mimeType to MediaType.Video
                    else -> null
                }
            }
        }
    }
}

suspend fun FragmentManager.showVideoAudioSearchDialogSuspend(): Pair<Int, Int>? {
    return suspendCancellableCoroutine<Pair<Int, Int>?> { cont ->
        val d = VideoAudioSearchDialog(CoroutineDialogCancelableResultCallback(cont))
        coroutineShowSafe(d, "VideoAudioSearchDialog#${System.currentTimeMillis()}", cont)
    }
}