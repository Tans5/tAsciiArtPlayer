package com.tans.tasciiartplayer.hwevent

import android.app.Application
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.telephony.TelephonyManager
import com.tans.tasciiartplayer.AppLog
import com.tans.tasciiartplayer.appGlobalCoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

object PhoneObserver {

    private val eventSubject: MutableSharedFlow<PhoneEvent> by lazy {
        MutableSharedFlow()
    }

    private val receiver: BroadcastReceiver by lazy {
        object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                if (intent?.action == TelephonyManager.ACTION_PHONE_STATE_CHANGED) {
                    val state = intent.getStringExtra(TelephonyManager.EXTRA_STATE)
                    when (state) {
                        TelephonyManager.EXTRA_STATE_RINGING -> {
                            AppLog.d(TAG, "Phone ringing.")
                            appGlobalCoroutineScope.launch {
                                eventSubject.emit(PhoneEvent.PhoneRinging)
                            }
                        }
                        TelephonyManager.EXTRA_STATE_IDLE -> {
                            AppLog.d(TAG, "Phone idle.")
                            appGlobalCoroutineScope.launch {
                                eventSubject.emit(PhoneEvent.PhoneIdle)
                            }
                        }
                        TelephonyManager.EXTRA_STATE_OFFHOOK -> {
                            AppLog.d(TAG, "Phone off hook.")
                            appGlobalCoroutineScope.launch {
                                eventSubject.emit(PhoneEvent.PhoneOffHook)
                            }
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