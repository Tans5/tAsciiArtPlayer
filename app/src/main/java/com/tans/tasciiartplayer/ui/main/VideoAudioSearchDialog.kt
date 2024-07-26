package com.tans.tasciiartplayer.ui.main

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.tans.tasciiartplayer.R
import com.tans.tuiutils.dialog.BaseCoroutineStateDialogFragment

class VideoAudioSearchDialog : BaseCoroutineStateDialogFragment<Unit>(Unit) {

    override fun createContentView(context: Context, parent: ViewGroup): View {
        return LayoutInflater.from(context).inflate(R.layout.video_audio_search_dialog, parent, false)
    }

    override fun firstLaunchInitData() {

    }

    override fun bindContentView(view: View) {
        // TODO
    }
}