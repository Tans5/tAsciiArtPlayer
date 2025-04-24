package com.tans.tasciiartplayer.iptv

import android.app.Application
import com.tans.tapm.monitors.HttpRequestMonitor
import com.tans.tasciiartplayer.AppLog
import com.tans.tasciiartplayer.AppSettings
import com.tans.tasciiartplayer.BuildConfig
import com.tans.tasciiartplayer.appGlobalCoroutineScope
import com.tans.tasciiartplayer.database.dao.IptvDao
import com.tans.tasciiartplayer.database.entity.IptvSource
import com.tans.tasciiartplayer.iptv.m3u8.model.M3u8
import com.tans.tasciiartplayer.iptv.m3u8.parseAsM3u8
import com.tans.tuiutils.state.CoroutineState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.Optional
import kotlin.jvm.optionals.getOrNull

object IptvManager : CoroutineState<IptvManager.IptvManagerState> by CoroutineState(IptvManagerState()), CoroutineScope by appGlobalCoroutineScope {

    private var application: Application? = null

    private var dao: IptvDao? = null

    private val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .apply {
                if (BuildConfig.DEBUG) {
                    addInterceptor(HttpRequestMonitor)
                }
            }
            .build()
    }

    fun init(app: Application, dao: IptvDao) {
        this.application = app
        this.dao = dao

        // IptvSource in database changed.
        launch {
            dao.observeAllIpTvSource()
                .distinctUntilChanged()
                .collect { allIptvSources ->
                    AppLog.d(TAG, "Iptv sources changed: $allIptvSources")
                    updateState { oldState ->
                        val selectedIptvSourceIdInSp = oldState.selectedIptvSourceId.getOrNull()
                        val lastSelectedSource = oldState.selectedIptvSource.getOrNull()
                        val new = allIptvSources.find { it.createTime == selectedIptvSourceIdInSp }
                        if (lastSelectedSource != new) {
                            oldState.copy(selectedIptvSource = Optional.ofNullable(new), allIptvSources = allIptvSources)
                        } else {
                            oldState.copy(allIptvSources = allIptvSources)
                        }
                    }
                }
        }

        // Selected IptvSource changed in SP
        launch {
            AppSettings.observeIptvSelectedSourceId()
                .distinctUntilChanged()
                .collect {
                    val newSelectedIptvId = it.getOrNull()
                    AppLog.d(TAG, "Selected Iptv source changed: $newSelectedIptvId")
                    updateState { oldState ->
                        if (oldState.selectedIptvSource.getOrNull()?.createTime != newSelectedIptvId) {
                            if (newSelectedIptvId == null) {
                                oldState.copy(selectedIptvSource = Optional.empty(), selectedIptvSourceId = Optional.empty())
                            } else {
                                val targetSource = oldState.allIptvSources.find { s -> s.createTime == newSelectedIptvId }
                                if (targetSource == null) {
                                    AppLog.e(TAG, "Selected iptv source id $newSelectedIptvId not in ${oldState.allIptvSources}.")
                                    oldState.copy(selectedIptvSource = Optional.empty(), selectedIptvSourceId = it)
                                } else {
                                    oldState.copy(selectedIptvSource = Optional.of(targetSource), selectedIptvSourceId = it)
                                }
                            }
                        } else {
                            oldState.copy(selectedIptvSourceId = it)
                        }
                    }
                }
        }

        // Load selected iptv source.
        launch {
            stateFlow()
                .map { it.selectedIptvSource }
                .distinctUntilChangedBy { it.getOrNull()?.sourceUrl }
                .collect { loadIptvSource(it, false) }
        }
    }


    suspend fun deleteIptvSource(source: IptvSource) {
        getDaoOrError().deleteIptvSource(source.createTime)
    }

    suspend fun insertIptvSource(source: IptvSource) {
        getDaoOrError().insertIptvSource(source)
    }

    suspend fun updateIptvSource(source: IptvSource) {
        getDaoOrError().updateIptvSource(source)
    }

    suspend fun selectIptvSource(id: Long) {
        AppSettings.setIptvSelectedSourceId(id)
    }

    suspend fun refresh() {
        val source = currentState().selectedIptvSource
        loadIptvSource(source, true)
    }

    private fun getDaoOrError(): IptvDao {
        return dao ?: error("Dao is null.")
    }

    private val loadIptvSourceLock = Mutex()

    private suspend fun loadIptvSource(selectedSource: Optional<IptvSource>, isRefreshing: Boolean) {
        loadIptvSourceLock.withLock {
            val source = selectedSource.getOrNull()
            if (source == null) {
                updateState { it.copy(loadIptvSourceStatus = LoadIptvSourceStatus.NoData) }
                return@withLock
            }
            val currentLoadingStatus = currentState().loadIptvSourceStatus
            val startStatus = when (currentLoadingStatus) {
                is LoadIptvSourceStatus.Loading, is LoadIptvSourceStatus.Refreshing -> {
                    AppLog.e(TAG, "Wrong loading status $currentLoadingStatus, ignore load iptv source.")
                    return@withLock
                }
                is LoadIptvSourceStatus.LoadFail, is LoadIptvSourceStatus.NoData -> {
                    LoadIptvSourceStatus.Loading(source)
                }
                is LoadIptvSourceStatus.LoadSuccess -> {
                    if (isRefreshing) {
                        LoadIptvSourceStatus.Refreshing(
                            source = source,
                            lastLoaded = currentLoadingStatus.loaded,
                            lastSource = currentLoadingStatus.source
                        )
                    } else {
                        LoadIptvSourceStatus.Loading(source)
                    }
                }
                is LoadIptvSourceStatus.RefreshFail -> {
                    if (isRefreshing) {
                        LoadIptvSourceStatus.Refreshing(
                            source = source,
                            lastLoaded = currentLoadingStatus.lastLoaded,
                            lastSource = currentLoadingStatus.lastSource
                        )
                    } else {
                        LoadIptvSourceStatus.Loading(source)
                    }
                }
                is LoadIptvSourceStatus.RefreshSuccess -> {
                    if (isRefreshing) {
                        LoadIptvSourceStatus.Refreshing(
                            source = source,
                            lastLoaded = currentLoadingStatus.loaded,
                            lastSource = currentLoadingStatus.source
                        )
                    } else {
                        LoadIptvSourceStatus.Loading(source)
                    }
                }
            }
            updateState { it.copy(loadIptvSourceStatus = startStatus) }
            AppLog.d(TAG, "Start load ${source.sourceUrl}, status: $startStatus")
            runCatching {
                val url = source.sourceUrl.toHttpUrlOrNull() ?: error("Wrong iptv source url: ${source.sourceUrl}.")
                val request = Request.Builder()
                    .get()
                    .url(url)
                    .build()
                val call = okHttpClient.newCall(request)
                val response = call.execute()
                val body = response.body?.string() ?: error("Empty response body: ${source.sourceUrl}")
                body.parseAsM3u8()
            }.onFailure {
                AppLog.e(TAG, "Load iptv source url ${source.sourceUrl} fail: ${it.message}", it)
                val failStatus = when (startStatus) {
                    is LoadIptvSourceStatus.Loading -> {
                        LoadIptvSourceStatus.LoadFail(source = source, msg = it.message ?: "")
                    }
                    is LoadIptvSourceStatus.Refreshing -> {
                        LoadIptvSourceStatus.RefreshFail(source = source, msg = it.message ?: "", lastSource = startStatus.lastSource, lastLoaded = startStatus.lastLoaded)
                    }
                    else -> {
                        // Can't be here.
                        error("Internal error.")
                    }
                }
                updateState { s -> s.copy(loadIptvSourceStatus = failStatus) }
            }.onSuccess {
                AppLog.d(TAG, "Load iptv source url ${source.sourceUrl} success.")
                val successStatus = when (startStatus) {
                    is LoadIptvSourceStatus.Loading -> {
                        LoadIptvSourceStatus.LoadSuccess(source = source, loaded = it)
                    }
                    is LoadIptvSourceStatus.Refreshing -> {
                        LoadIptvSourceStatus.RefreshSuccess(source = source, loaded = it, lastSource = startStatus.lastSource, lastLoaded = startStatus.lastLoaded)
                    }
                    else -> {
                        // Can't be here.
                        error("Internal error.")
                    }
                }
                updateState { s -> s.copy(loadIptvSourceStatus = successStatus) }
            }
        }
    }

    sealed class LoadIptvSourceStatus {

        data object NoData : LoadIptvSourceStatus()

        data class Loading(val source: IptvSource) : LoadIptvSourceStatus()

        data class LoadSuccess(val source: IptvSource, val loaded: M3u8) : LoadIptvSourceStatus()

        data class LoadFail(val source: IptvSource, val msg: String) : LoadIptvSourceStatus()

        data class Refreshing(val source: IptvSource, val lastSource: IptvSource, val lastLoaded: M3u8) : LoadIptvSourceStatus()

        data class RefreshSuccess(val source: IptvSource, val loaded: M3u8, val lastSource: IptvSource, val lastLoaded: M3u8) : LoadIptvSourceStatus()

        data class RefreshFail(val source: IptvSource, val msg: String, val lastSource: IptvSource, val lastLoaded: M3u8) : LoadIptvSourceStatus()
    }

    data class IptvManagerState(
        val loadIptvSourceStatus: LoadIptvSourceStatus = LoadIptvSourceStatus.NoData,
        val selectedIptvSource: Optional<IptvSource> = Optional.empty(),
        val allIptvSources: List<IptvSource> = emptyList(),
        val selectedIptvSourceId: Optional<Long> = Optional.empty()
    )

    const val TAG = "IptvManager"
}