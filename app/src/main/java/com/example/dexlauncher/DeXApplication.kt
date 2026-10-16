package com.example.dexlauncher

import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.dexlauncher.data.repository.AppRepository
import com.example.dexlauncher.data.repository.SettingsRepository
import com.example.dexlauncher.service.ScreenStateService

class DeXApplication : Application() {

    val appRepository by lazy { AppRepository(this) }
    val settingsRepository by lazy { SettingsRepository(this) }

    override fun onCreate() {
        super.onCreate()
        instance = this
        createNotificationChannel()
        startScreenStateService()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "DeX Launcher Screen Service",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Keeps Standby screen and taskbar overlay responsive"
            }
            val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            manager.createNotificationChannel(channel)
        }
    }

    private fun startScreenStateService() {
        try {
            val intent = Intent(this, ScreenStateService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (_: Exception) {}
    }

    companion object {
        const val CHANNEL_ID = "dex_launcher_channel"
        lateinit var instance: DeXApplication
            private set
    }
}
