package com.tans.tasciiartplayer

import android.app.Application
import android.content.Context
import androidx.room.Room
import com.tans.tapm.autoinit.tApmAutoInit
import com.tans.tapm.monitors.CpuPowerCostMonitor
import com.tans.tapm.monitors.CpuUsageMonitor
import com.tans.tapm.monitors.ForegroundScreenPowerCostMonitor
import com.tans.tapm.monitors.HttpRequestMonitor
import com.tans.tapm.monitors.MainThreadLagMonitor
import com.tans.tapm.monitors.MemoryUsageMonitor
import com.tans.tasciiartplayer.audio.audiolist.AudioListManager
import com.tans.tasciiartplayer.audio.audioplayer.AudioPlayerManager
import com.tans.tasciiartplayer.database.AppDatabase
import com.tans.tasciiartplayer.hwevent.HeadsetObserver
import com.tans.tasciiartplayer.hwevent.MediaKeyObserver
import com.tans.tasciiartplayer.hwevent.PhoneObserver
import com.tans.tasciiartplayer.iptv.IptvManager
import com.tans.tasciiartplayer.video.VideoManager
import com.tans.tuiutils.systembar.AutoApplySystemBarAnnotation
import okhttp3.OkHttpClient

class App : Application() {

    override fun attachBaseContext(base: Context?) {
        super.attachBaseContext(base)
        tApmAutoInit.addBuilderInterceptor { builder ->
            if (BuildConfig.DEBUG) {
                builder
                    // CpuUsage
                    .addMonitor(CpuUsageMonitor().apply { setMonitorInterval(1000L * 10) })
                    // CpuPowerCost
                    .addMonitor(CpuPowerCostMonitor())
                    // ForegroundScreenPowerCost
                    .addMonitor(ForegroundScreenPowerCostMonitor())
                    // Http
                    .addMonitor(HttpRequestMonitor())
                    // MainThreadLag
                    .addMonitor(MainThreadLagMonitor())
                    // MemoryUsage
                    .addMonitor(MemoryUsageMonitor())
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        AppLog.init(this)
        AutoApplySystemBarAnnotation.init(this)
        val database = Room.databaseBuilder(
            context = this,
            klass = AppDatabase::class.java,
            name = AppDatabase.DATA_BASE_NAME
        )
            .fallbackToDestructiveMigration(false)
            .build()

        AppSettings.init(this)
        VideoManager.init(this, database.videoDao())
        AudioListManager.init(this, database.audioDao())
        IptvManager.init(this, database.iptvDao())
        AudioPlayerManager.init(this)
        HeadsetObserver.init(this)
        PhoneObserver.init(this)
        MediaKeyObserver.init(this)
    }

    companion object {

        val okhttpClient: OkHttpClient by lazy {
            OkHttpClient.Builder()
                .apply {
                    if (BuildConfig.DEBUG) {
                        addInterceptor(HttpRequestMonitor)
                    }
                }
                .build()
        }
    }
}