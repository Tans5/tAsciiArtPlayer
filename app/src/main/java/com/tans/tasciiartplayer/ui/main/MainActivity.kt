package com.tans.tasciiartplayer.ui.main

import android.Manifest
import android.os.Build
import android.view.View
import androidx.collection.LongSparseArray
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.viewpager2.adapter.FragmentStateAdapter
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import com.tans.tasciiartplayer.R
import com.tans.tasciiartplayer.audio.audioplayer.AudioPlayerManager
import com.tans.tasciiartplayer.databinding.MainActivityBinding
import com.tans.tasciiartplayer.ui.audioplayer.AlbumsDialog
import com.tans.tasciiartplayer.ui.audioplayer.ArtistsDialog
import com.tans.tasciiartplayer.ui.audioplayer.AudioListDialog
import com.tans.tasciiartplayer.ui.videoplayer.VideoPlayerActivity
import com.tans.tuiutils.activity.BaseCoroutineStateActivity
import com.tans.tuiutils.permission.permissionsRequestSuspend
import com.tans.tuiutils.systembar.annotation.SystemBarStyle
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch


@SystemBarStyle(statusBarThemeStyle = 1, navigationBarThemeStyle = 1)
class MainActivity : BaseCoroutineStateActivity<MainActivity.Companion.State>(State()) {

    override val layoutId: Int = R.layout.main_activity

    private val fragments: Map<TabType, Fragment> by lazyViewModelField("fragments") {
        mapOf(
            TabType.Videos to VideosFragment(),
            TabType.Audios to AudiosFragment()
        )
    }

    override fun CoroutineScope.firstLaunchInitDataCoroutine() {  }

    override fun CoroutineScope.bindContentViewCoroutine(contentView: View) {
        val permissionsNeed = mutableListOf<String>()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsNeed.add(Manifest.permission.READ_MEDIA_AUDIO)
            permissionsNeed.add(Manifest.permission.READ_MEDIA_VIDEO)
            permissionsNeed.add(Manifest.permission.POST_NOTIFICATIONS)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                permissionsNeed.add(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED)
            }
        } else {
            permissionsNeed.add(Manifest.permission.READ_EXTERNAL_STORAGE)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            permissionsNeed.add(Manifest.permission.BLUETOOTH_CONNECT)
        }
        permissionsNeed.add(Manifest.permission.READ_PHONE_STATE)
        launch {
            runCatching {
                permissionsRequestSuspend(*permissionsNeed.toTypedArray())
            }
            val viewBinding = MainActivityBinding.bind(contentView)

            val fragmentsAdapter = object : FragmentStateAdapter(this@MainActivity) {
                override fun getItemCount(): Int = fragments.size
                override fun createFragment(position: Int): Fragment = fragments[TabType.entries[position]]!!
            }
            // To fix act restart cause crash.
            try {
                val fragmentsField = FragmentStateAdapter::class.java.getDeclaredField("mFragments")
                fragmentsField.isAccessible = true
                val fragments = fragmentsField.get(fragmentsAdapter) as LongSparseArray<Fragment>
                fragments.clear()
                for ((i, f) in this@MainActivity.fragments.map { it.value }.withIndex()) {
                    fragments.put(i.toLong(), f)
                }
            } catch (e: Throwable) {
                e.printStackTrace()
            }
            viewBinding.viewPager.adapter = fragmentsAdapter
            viewBinding.viewPager.isSaveEnabled = false
            viewBinding.viewPager.offscreenPageLimit = fragments.size
            viewBinding.tabLayout.addOnTabSelectedListener(object :
                TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab?) {
                    when (tab?.position) {
                        TabType.Videos.ordinal -> updateState { it.copy(selectedTab = TabType.Videos) }
                        TabType.Audios.ordinal -> updateState { it.copy(selectedTab = TabType.Audios) }
                    }
                }
                override fun onTabUnselected(tab: TabLayout.Tab?) {}
                override fun onTabReselected(tab: TabLayout.Tab?) {}
            })
            TabLayoutMediator(viewBinding.tabLayout, viewBinding.viewPager) { tab, position ->
                tab.text = when (TabType.entries[position]) {
                    TabType.Videos -> getString(R.string.main_act_videos_tab)
                    TabType.Audios -> getString(R.string.main_act_audios_tab)
                }
            }.attach()

            viewBinding.toolBar.menu.findItem(R.id.app_settings).setOnMenuItemClickListener {
                val settingsDialog = AppSettingsDialog()
                settingsDialog.showSafe(supportFragmentManager, "AppSettingsDialog#${System.currentTimeMillis()}")
                true
            }

            // Remove search media files for google play check.
//            viewBinding.toolBar.menu.findItem(R.id.video_audio_search).setOnMenuItemClickListener {
//                this@bindContentViewCoroutine.launch {
//                    try {
//                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && !Environment.isExternalStorageManager()) {
//                            val grant = this@MainActivity.supportFragmentManager.showOptionalDialogSuspend(
//                                title = getString(R.string.main_act_storage_permission_request_title),
//                                message = getString(R.string.main_act_storage_permission_request_body),
//                                positiveButtonText = getString(R.string.main_act_storage_permission_request_accept),
//                                negativeButtonText = getString(R.string.main_act_storage_permission_request_deny)
//                            )
//                            if (grant == true) {
//                                val i = Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
//                                i.data = Uri.fromParts("package", packageName, null)
//                                startActivity(i)
//                            }
//                        } else {
//                            val scanResult = this@MainActivity.supportFragmentManager.showVideoAudioSearchDialogSuspend()
//                            if (scanResult != null && (scanResult.first > 0 || scanResult.second > 0)) {
//                                Toast.makeText(this@MainActivity, getString(R.string.main_act_found_new_videos_audios, scanResult.first, scanResult.second), Toast.LENGTH_SHORT).show()
//                            }
//                        }
//                    } catch (e: Throwable) {
//                        e.printStackTrace()
//                    }
//                }
//                true
//            }

            viewBinding.toolBar.menu.findItem(R.id.media_link).setOnMenuItemClickListener {
                this@bindContentViewCoroutine.launch {
                    val mediaLink = supportFragmentManager.showMediaLinkDialogSuspend()
                    if (!mediaLink.isNullOrBlank()) {
                        AudioPlayerManager.removeAudioList()
                        startActivity(VideoPlayerActivity.createIntent(this@MainActivity, mediaLink))
                    }
                }
                true
            }

            ViewCompat.setOnApplyWindowInsetsListener(viewBinding.root) { v, insets ->
                val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
                v.setPadding(0, systemBars.top, 0, 0)
                insets
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        ArtistsDialog.removeCacheContentViewAndTask(this)
        AudioListDialog.removeCacheContentViewAndTask(this)
        AlbumsDialog.removeCacheContentViewAndTask(this)
    }

    companion object {

        enum class TabType { Videos, Audios }

        data class State(
            val selectedTab: TabType = TabType.Videos
        )
    }
}