package com.tans.tasciiartplayer.ui.main

import android.view.View
import com.tans.tasciiartplayer.R
import com.tans.tasciiartplayer.audio.AudioListType
import com.tans.tasciiartplayer.audio.AudioManager
import com.tans.tasciiartplayer.databinding.AudiosFragmentBinding
import com.tans.tasciiartplayer.ui.audioplayer.ArtistsDialog
import com.tans.tasciiartplayer.ui.audioplayer.AudioListDialog
import com.tans.tuiutils.fragment.BaseCoroutineStateFragment
import com.tans.tuiutils.view.clicks
import com.tans.tuiutils.view.refreshes
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class AudiosFragment : BaseCoroutineStateFragment<Unit>(Unit) {

    override val layoutId: Int = R.layout.audios_fragment

    override fun CoroutineScope.firstLaunchInitDataCoroutine() {
        launch {
            AudioManager.refreshMediaStoreAudios()
        }
    }

    override fun CoroutineScope.bindContentViewCoroutine(contentView: View) {
        val viewBinding = AudiosFragmentBinding.bind(contentView)

        viewBinding.swipeRefreshLayout.refreshes(this, Dispatchers.IO) {
            AudioManager.refreshMediaStoreAudios()
        }

        viewBinding.allAudiosLayout.clicks(this) {
            val d = AudioListDialog(AudioListType.AllAudios)
            d.show(requireActivity().supportFragmentManager, "AudioListDialog#${System.currentTimeMillis()}")
        }

        viewBinding.myFavoritesLayout.clicks(this) {
            val d = AudioListDialog(AudioListType.LikeAudios)
            d.show(requireActivity().supportFragmentManager, "AudioListDialog#${System.currentTimeMillis()}")
        }

        viewBinding.albumsLayout.clicks(this) {
            // TODO:
        }

        viewBinding.artistsLayout.clicks(this) {
            val d = ArtistsDialog()
            d.show(requireActivity().supportFragmentManager, "ArtistsDialog#${System.currentTimeMillis()}")
        }

        viewBinding.customPlaylistsLayout.clicks(this) {
            // TODO:
        }
    }
}