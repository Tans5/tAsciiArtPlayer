package com.tans.tasciiartplayer.hwevent

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.telephony.TelephonyManager
import com.tans.tasciiartplayer.AppLog
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

object PhoneObserver {

    private val eventSubject: MutableSharedFlow<PhoneEvent> by lazy {
        MutableSharedFlow(replay = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    }

    private val receiver: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
                    val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                    when (state) {
                        TelephonyManager.EXTRA_STATE_RINGING -> {
                            AppLog.d(TAG, "Phone ringing.")
                            eventSubject.tryEmit(PhoneEvent.PhoneRinging)
                        }
                        TelephonyManager.EXTRA_STATE_IDLE -> {
                            AppLog.d(TAG, "Phone idle.")
                            eventSubject.tryEmit(PhoneEvent.PhoneIdle)
                        }
                        TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                            AppLog.d(TAG, "Phone off hook.")
                            eventSubject.tryEmit(PhoneEvent.PhoneOffHook)
                        }
                    }
                }
            }
        }
    }

    fun init(application: Application) {
        val filter = IntentFilter()
        filter.addAction(TelephonyManager.ACTION_PHONE_STATE_CHANGED)
        application.registerReceiver(receiver, filter)
    }

    fun observeEvent(): Flow<PhoneEvent> = eventSubject

    private const val TAG = "PhoneObserver"

    enum class PhoneEvent {
        PhoneRinging,
        PhoneIdle,
        PhoneOffHook
    }
}