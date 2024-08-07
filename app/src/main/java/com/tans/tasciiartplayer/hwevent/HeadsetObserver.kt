package com.tans.tasciiartplayer.hwevent

import android.app.Application
import android.bluetooth.BluetoothHeadset
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import com.tans.tasciiartplayer.AppLog
import com.tans.tasciiartplayer.appGlobalCoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

object HeadsetObserver {

    private val eventSubject: MutableSharedFlow<HeadsetEvent> by lazy {
        MutableSharedFlow()
    }

    private val receiver: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent != null) {
                    when (intent.action) {

                        // Wire headset
                        AudioManager.ACTION_HEADSET_PLUG -> {
                            val state = intent.getIntExtra("state", -1)
                            when (state) {
                                0 -> {
                                    AppLog.d(TAG, "Wire headset disconnected.")
                                    appGlobalCoroutineScope.launch {
                                        eventSubject.emit(HeadsetEvent.WireHeadsetDisconnected)
                                    }
                                }
                                1 -> {
                                    AppLog.d(TAG, "Wire headset connected.")
                                    appGlobalCoroutineScope.launch {
                                        eventSubject.emit(HeadsetEvent.WireHeadsetConnected)
                                    }
                                }
                            }
                        }

                        // Bluetooth headset
                        BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED -> {
                            val state = intent.getIntExtra(BluetoothHeadset.EXTRA_STATE, -1)
                            when (state) {
                                BluetoothHeadset.STATE_DISCONNECTED -> {
                                    AppLog.d(TAG, "Bluetooth headset disconnected")
                                    appGlobalCoroutineScope.launch {
                                        eventSubject.emit(HeadsetEvent.BluetoothHeadsetDisconnected)
                                    }
                                }
                                BluetoothHeadset.STATE_CONNECTED -> {
                                    AppLog.d(TAG, "Bluetooth headset connected")
                                    appGlobalCoroutineScope.launch {
                                        eventSubject.emit(HeadsetEvent.BluetoothHeadsetConnected)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun init(application: Application) {
        val filter = IntentFilter()
        filter.addAction(AudioManager.ACTION_HEADSET_PLUG)
        filter.addAction(BluetoothHeadset.ACTION_CONNECTION_STATE_CHANGED)
        application.registerReceiver(receiver, filter)
    }

    fun observeEvent(): Flow<HeadsetEvent> = eventSubject

    private const val TAG = "HeadsetObserver"

    enum class HeadsetEvent {
        WireHeadsetConnected,
        WireHeadsetDisconnected,
        BluetoothHeadsetConnected,
        BluetoothHeadsetDisconnected
    }
}