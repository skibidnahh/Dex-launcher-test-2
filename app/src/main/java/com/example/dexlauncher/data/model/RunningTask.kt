package com.example.dexlauncher.data.model

data class RunningTask(
    val packageName: String,
    val title: String,
    val isFocused: Boolean = false,
    val isMinimized: Boolean = false
)
