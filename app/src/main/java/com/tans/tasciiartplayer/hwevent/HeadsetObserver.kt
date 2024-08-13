package com.tans.tasciiartplayer.hwevent

import android.Manifest
import android.annotation.SuppressLint
import android.app.Application
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothHeadset
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.AudioManager
import android.os.Build
import androidx.core.content.getSystemService
import com.tans.tasciiartplayer.AppLog
import com.tans.tasciiartplayer.appGlobalCoroutineScope
import com.tans.tuiutils.permission.permissionCheck
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

object HeadsetObserver {

    private val eventSubject: MutableSharedFlow<HeadsetEvent> by lazy {
        MutableSharedFlow()
    }

    private val receiver: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {

            private val isBluetoothHeadsetConnected = AtomicBoolean(false)

            @SuppressLint("MissingPermission")
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent != null && context != null) {
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

                        // Bluetooth device connected state changed
                        BluetoothDevice.ACTION_ACL_CONNECTED, BluetoothDevice.ACTION_ACL_DISCONNECTED -> {
                            val permissionGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                                context.permissionCheck(Manifest.permission.BLUETOOTH_CONNECT)
                            } else {
                                true
                            }
                            if (permissionGranted) {
                                val bluetoothAdapter = context.getSystemService<BluetoothManager>()!!.adapter
                                if (bluetoothAdapter.getProfileConnectionState(BluetoothHeadset.HEADSET) == BluetoothAdapter.STATE_CONNECTED) {
                                    if (isBluetoothHeadsetConnected.compareAndSet(false, true)) {
                                        appGlobalCoroutineScope.launch {
                                            eventSubject.emit(HeadsetEvent.BluetoothHeadsetConnected)
                                        }
                                        AppLog.d(TAG, "Bluetooth headset connected.")
                                    }
                                } else {
                                    if (isBluetoothHeadsetConnected.compareAndSet(true, false)) {
                                        appGlobalCoroutineScope.launch {
                                            eventSubject.emit(HeadsetEvent.BluetoothHeadsetDisconnected)
                                        }
                                        AppLog.d(TAG, "Bluetooth headset disconnected.")
                                    }
                                }
                            } else {
                                AppLog.e(TAG, "No bluetooth connect permission, can't check bluetooth headset.")
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
        filter.addAction(BluetoothDevice.ACTION_ACL_CONNECTED)
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
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