package com.tans.tasciiartplayer

import android.app.Application
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.tans.tmediaplayer.player.model.AudioChannel
import com.tans.tmediaplayer.player.model.AudioSampleBitDepth
import com.tans.tmediaplayer.player.model.AudioSampleRate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import java.util.Optional

object AppSettings {

    private val dataStoreProvider = preferencesDataStore(
        name = "settings",
        scope = appGlobalCoroutineScope
    )

    private val VIDEO_DECODE_HARDWARE_KEY = booleanPreferencesKey("video_decode_hardware")

    private val AUDIO_OUTPUT_CHANNELS_KEY = intPreferencesKey("audio_output_channels")

    private val AUDIO_OUTPUT_SAMPLE_RATE_KEY = intPreferencesKey("audio_output_sample_rate")

    private val AUDIO_OUTPUT_SAMPLE_FMT_KEY = intPreferencesKey("audio_output_sample_fmt")

    private val IPTV_SELECTED_SOURCE_ID_KEY = longPreferencesKey("iptv_selected_source_id")

    private var application: Application? = null

    private val dataStore: DataStore<Preferences> by lazy {
        dataStoreProvider.getValue(application ?: error("No application to init data store."), ::dataStore)
    }

    fun init(application: Application) {
        this.application = application
        // Trigger to init.
        this.dataStore
    }

    suspend fun isVideoDecodeHardware(): Boolean {
        return dataStore.data.firstOrNull()?.get(VIDEO_DECODE_HARDWARE_KEY) ?: true
    }

    suspend fun setVideoDecodeHardware(videoDecodeHardware: Boolean) {
        dataStore.edit { it[VIDEO_DECODE_HARDWARE_KEY] = videoDecodeHardware }
    }

    suspend fun getAudioOutputChannels(): AudioChannel {
        val channel = dataStore.data.firstOrNull()?.get(AUDIO_OUTPUT_CHANNELS_KEY)
        return AudioChannel.entries.find { it.channel == channel } ?: AudioChannel.Stereo
    }

    suspend fun setAudioOutputChannels(channel: AudioChannel) {
        dataStore.edit { it[AUDIO_OUTPUT_CHANNELS_KEY] = channel.channel }
    }

    suspend fun getAudioOutputSampleRate(): AudioSampleRate {
        val sampleRate = dataStore.data.firstOrNull()?.get(AUDIO_OUTPUT_SAMPLE_RATE_KEY)
        return AudioSampleRate.entries.find { it.rate == sampleRate } ?: AudioSampleRate.Rate48000
    }

    suspend fun setAudioOutputSampleRate(simpleRate: AudioSampleRate) {
        dataStore.edit { it[AUDIO_OUTPUT_SAMPLE_RATE_KEY] = simpleRate.rate }
    }

    suspend fun getAudioOutputSampleFormat(): AudioSampleBitDepth {
        val simpleDepth = dataStore.data.firstOrNull()?.get(AUDIO_OUTPUT_SAMPLE_FMT_KEY)
        return AudioSampleBitDepth.entries.find { it.depth == simpleDepth } ?: AudioSampleBitDepth.SixteenBits
    }

    suspend fun setAudioOutputSampleFormat(simpleFormat: AudioSampleBitDepth) {
        dataStore.edit { it[AUDIO_OUTPUT_SAMPLE_FMT_KEY] = simpleFormat.depth }
    }


    suspend fun getIptvSelectedSourceId(): Long? {
        return dataStore.data.firstOrNull()?.get(IPTV_SELECTED_SOURCE_ID_KEY)
    }

    fun observeIptvSelectedSourceId(): Flow<Optional<Long>> {
        return dataStore.data
            .map {
                val id = it[IPTV_SELECTED_SOURCE_ID_KEY]
                Optional.ofNullable(id)
            }
            .distinctUntilChanged()
    }

    /**
     * create time as id
     */
    suspend fun setIptvSelectedSourceId(id: Long?) {
        dataStore.edit {
            if (id == null) {
                it.remove(IPTV_SELECTED_SOURCE_ID_KEY)
            } else {
                it[IPTV_SELECTED_SOURCE_ID_KEY] = id
            }
        }
    }
}