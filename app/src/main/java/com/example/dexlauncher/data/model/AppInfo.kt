package com.example.dexlauncher.data.model

import androidx.compose.runtime.Immutable

@Immutable
data class AppInfo(
    val label: String,
    val packageName: String,
    val activityName: String,
    val isPinnedToDesktop: Boolean = false,
    val isPinnedToTaskbar: Boolean = false,
    val isRunning: Boolean = false,
    val launchCount: Int = 0
)
