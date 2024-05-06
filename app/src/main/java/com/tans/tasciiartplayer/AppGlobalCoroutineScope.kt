package com.tans.tasciiartplayer

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers

/**
 * never cancel.
 */
val appGlobalCoroutineScope = CoroutineScope(Dispatchers.IO)