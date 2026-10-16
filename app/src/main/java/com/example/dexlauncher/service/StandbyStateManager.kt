package com.example.dexlauncher.service

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object StandbyStateManager {
    private val _isStandbyActive = MutableStateFlow(true)
    val isStandbyActive: StateFlow<Boolean> = _isStandbyActive.asStateFlow()

    var isInStandby: Boolean
        get() = _isStandbyActive.value
        set(value) {
            _isStandbyActive.value = value
        }
}
