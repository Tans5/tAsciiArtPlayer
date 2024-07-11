package com.tans.tasciiartplayer.ui.audioplayer

import android.view.View
import kotlinx.coroutines.Job

data class ContentViewAndTask(
    val view: View,
    val task: Job
)