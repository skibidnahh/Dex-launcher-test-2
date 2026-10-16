package com.example.dexlauncher.data.repository

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "dex_settings")

class SettingsRepository(private val context: Context) {
    object PreferencesKeys {
        val DESKTOP_WALLPAPER_URI = stringPreferencesKey("desktop_wallpaper_uri")
        val DESKTOP_WALLPAPER_TYPE = stringPreferencesKey("desktop_wallpaper_type")
        val STANDBY_WALLPAPER_URI = stringPreferencesKey("standby_wallpaper_uri")
        val STANDBY_WALLPAPER_TYPE = stringPreferencesKey("standby_wallpaper_type")
        val WALLPAPER_DIM = floatPreferencesKey("wallpaper_dim")
        val WALLPAPER_BLUR = floatPreferencesKey("wallpaper_blur")

        val CLOCK_FONT = stringPreferencesKey("clock_font")
        val CLOCK_FONT_PATH = stringPreferencesKey("clock_font_path")
        val CLOCK_COLOR = intPreferencesKey("clock_color")
        val CLOCK_SCALE = floatPreferencesKey("clock_scale")
        val CLOCK_OFFSET_X = floatPreferencesKey("clock_offset_x")
        val CLOCK_OFFSET_Y = floatPreferencesKey("clock_offset_y")
        val CLOCK_OPACITY = floatPreferencesKey("clock_opacity")
        val CLOCK_BLUR = floatPreferencesKey("clock_blur")
        val CLOCK_WEIGHT = intPreferencesKey("clock_weight")
        val CLOCK_LETTER_SPACING = floatPreferencesKey("clock_letter_spacing")
        val CLOCK_SHADOW = booleanPreferencesKey("clock_shadow")
        val CLOCK_IS_24H = booleanPreferencesKey("clock_is_24h")
        val CLOCK_SHOW_DATE = booleanPreferencesKey("clock_show_date")
        val CLOCK_SHOW_SWIPE_UP = booleanPreferencesKey("clock_show_swipe_up")
        val CLOCK_ALIGNMENT = stringPreferencesKey("clock_alignment")

        val ICON_SIZE = intPreferencesKey("icon_size")
        val ICON_LABEL_SIZE = intPreferencesKey("icon_label_size")
        val ICON_CORNER_RADIUS = intPreferencesKey("icon_corner_radius")
        val SHOW_LABELS = booleanPreferencesKey("show_labels")
        val SHOW_SHADOW = booleanPreferencesKey("show_shadow")
        val DESKTOP_LAYOUT_MODE = stringPreferencesKey("desktop_layout_mode")

        val FULLSCREEN_ENABLED = booleanPreferencesKey("fullscreen_enabled")
        val TASKBAR_AUTO_HIDE = booleanPreferencesKey("taskbar_auto_hide")
        val ANIMATION_SPEED = floatPreferencesKey("animation_speed")
        val HIDDEN_APPS = stringSetPreferencesKey("hidden_apps")
    }

    val settingsFlow: Flow<LauncherSettings> = context.dataStore.data
        .map { prefs ->
            LauncherSettings(
                desktopWallpaperUri = prefs[PreferencesKeys.DESKTOP_WALLPAPER_URI] ?: "",
                desktopWallpaperType = prefs[PreferencesKeys.DESKTOP_WALLPAPER_TYPE] ?: "preset",
                standbyWallpaperUri = prefs[PreferencesKeys.STANDBY_WALLPAPER_URI] ?: "",
                standbyWallpaperType = prefs[PreferencesKeys.STANDBY_WALLPAPER_TYPE] ?: "preset",
                wallpaperDim = prefs[PreferencesKeys.WALLPAPER_DIM] ?: 0.2f,
                wallpaperBlur = prefs[PreferencesKeys.WALLPAPER_BLUR] ?: 0f,
                clockFont = prefs[PreferencesKeys.CLOCK_FONT] ?: "Default",
                clockFontPath = prefs[PreferencesKeys.CLOCK_FONT_PATH] ?: "",
                clockColor = prefs[PreferencesKeys.CLOCK_COLOR] ?: android.graphics.Color.WHITE,
                clockScale = prefs[PreferencesKeys.CLOCK_SCALE] ?: 1.0f,
                clockOffsetX = prefs[PreferencesKeys.CLOCK_OFFSET_X] ?: 0.5f,
                clockOffsetY = prefs[PreferencesKeys.CLOCK_OFFSET_Y] ?: 0.4f,
                clockOpacity = prefs[PreferencesKeys.CLOCK_OPACITY] ?: 1.0f,
                clockBlur = prefs[PreferencesKeys.CLOCK_BLUR] ?: 0f,
                clockWeight = prefs[PreferencesKeys.CLOCK_WEIGHT] ?: 400,
                clockLetterSpacing = prefs[PreferencesKeys.CLOCK_LETTER_SPACING] ?: 0f,
                clockShadow = prefs[PreferencesKeys.CLOCK_SHADOW] ?: true,
                clockIs24H = prefs[PreferencesKeys.CLOCK_IS_24H] ?: true,
                clockShowDate = prefs[PreferencesKeys.CLOCK_SHOW_DATE] ?: true,
                clockShowSwipeUp = prefs[PreferencesKeys.CLOCK_SHOW_SWIPE_UP] ?: true,
                clockAlignment = prefs[PreferencesKeys.CLOCK_ALIGNMENT] ?: "center",
                iconSize = prefs[PreferencesKeys.ICON_SIZE] ?: 64,
                iconLabelSize = prefs[PreferencesKeys.ICON_LABEL_SIZE] ?: 12,
                iconCornerRadius = prefs[PreferencesKeys.ICON_CORNER_RADIUS] ?: 16,
                showLabels = prefs[PreferencesKeys.SHOW_LABELS] ?: true,
                showShadow = prefs[PreferencesKeys.SHOW_SHADOW] ?: true,
                desktopLayoutMode = prefs[PreferencesKeys.DESKTOP_LAYOUT_MODE] ?: "grid",
                fullscreenEnabled = prefs[PreferencesKeys.FULLSCREEN_ENABLED] ?: true,
                taskbarAutoHide = prefs[PreferencesKeys.TASKBAR_AUTO_HIDE] ?: false,
                animationSpeed = prefs[PreferencesKeys.ANIMATION_SPEED] ?: 1.0f,
                hiddenApps = prefs[PreferencesKeys.HIDDEN_APPS] ?: emptySet()
            )
        }

    suspend fun updateDesktopWallpaper(uri: String, type: String) {
        context.dataStore.edit {
            it[PreferencesKeys.DESKTOP_WALLPAPER_URI] = uri
            it[PreferencesKeys.DESKTOP_WALLPAPER_TYPE] = type
        }
    }

    suspend fun updateStandbyWallpaper(uri: String, type: String) {
        context.dataStore.edit {
            it[PreferencesKeys.STANDBY_WALLPAPER_URI] = uri
            it[PreferencesKeys.STANDBY_WALLPAPER_TYPE] = type
        }
    }

    suspend fun updateWallpaperDim(dim: Float) {
        context.dataStore.edit { it[PreferencesKeys.WALLPAPER_DIM] = dim }
    }

    suspend fun updateWallpaperBlur(blur: Float) {
        context.dataStore.edit { it[PreferencesKeys.WALLPAPER_BLUR] = blur }
    }

    suspend fun updateClockCustomization(
        font: String,
        fontPath: String,
        color: Int,
        scale: Float,
        offsetX: Float,
        offsetY: Float,
        opacity: Float,
        blur: Float,
        weight: Int,
        letterSpacing: Float,
        shadow: Boolean,
        is24H: Boolean,
        showDate: Boolean,
        showSwipeUp: Boolean,
        alignment: String
    ) {
        context.dataStore.edit {
            it[PreferencesKeys.CLOCK_FONT] = font
            it[PreferencesKeys.CLOCK_FONT_PATH] = fontPath
            it[PreferencesKeys.CLOCK_COLOR] = color
            it[PreferencesKeys.CLOCK_SCALE] = scale
            it[PreferencesKeys.CLOCK_OFFSET_X] = offsetX
            it[PreferencesKeys.CLOCK_OFFSET_Y] = offsetY
            it[PreferencesKeys.CLOCK_OPACITY] = opacity
            it[PreferencesKeys.CLOCK_BLUR] = blur
            it[PreferencesKeys.CLOCK_WEIGHT] = weight
            it[PreferencesKeys.CLOCK_LETTER_SPACING] = letterSpacing
            it[PreferencesKeys.CLOCK_SHADOW] = shadow
            it[PreferencesKeys.CLOCK_IS_24H] = is24H
            it[PreferencesKeys.CLOCK_SHOW_DATE] = showDate
            it[PreferencesKeys.CLOCK_SHOW_SWIPE_UP] = showSwipeUp
            it[PreferencesKeys.CLOCK_ALIGNMENT] = alignment
        }
    }

    suspend fun updateIconSettings(size: Int, labelSize: Int, cornerRadius: Int, showLabels: Boolean, showShadow: Boolean) {
        context.dataStore.edit {
            it[PreferencesKeys.ICON_SIZE] = size
            it[PreferencesKeys.ICON_LABEL_SIZE] = labelSize
            it[PreferencesKeys.ICON_CORNER_RADIUS] = cornerRadius
            it[PreferencesKeys.SHOW_LABELS] = showLabels
            it[PreferencesKeys.SHOW_SHADOW] = showShadow
        }
    }

    suspend fun updateDesktopLayoutMode(mode: String) {
        context.dataStore.edit { it[PreferencesKeys.DESKTOP_LAYOUT_MODE] = mode }
    }

    suspend fun updateFullscreenEnabled(enabled: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.FULLSCREEN_ENABLED] = enabled }
    }

    suspend fun updateTaskbarAutoHide(hide: Boolean) {
        context.dataStore.edit { it[PreferencesKeys.TASKBAR_AUTO_HIDE] = hide }
    }

    suspend fun updateAnimationSpeed(speed: Float) {
        context.dataStore.edit { it[PreferencesKeys.ANIMATION_SPEED] = speed }
    }

    suspend fun setAppHidden(packageName: String, hidden: Boolean) {
        context.dataStore.edit { prefs ->
            val current = prefs[PreferencesKeys.HIDDEN_APPS] ?: emptySet()
            val updated = if (hidden) current + packageName else current - packageName
            prefs[PreferencesKeys.HIDDEN_APPS] = updated
        }
    }

    suspend fun resetToDefaults() {
        context.dataStore.edit { it.clear() }
    }
}

data class LauncherSettings(
    val desktopWallpaperUri: String,
    val desktopWallpaperType: String,
    val standbyWallpaperUri: String,
    val standbyWallpaperType: String,
    val wallpaperDim: Float,
    val wallpaperBlur: Float,
    val clockFont: String,
    val clockFontPath: String,
    val clockColor: Int,
    val clockScale: Float,
    val clockOffsetX: Float,
    val clockOffsetY: Float,
    val clockOpacity: Float,
    val clockBlur: Float,
    val clockWeight: Int,
    val clockLetterSpacing: Float,
    val clockShadow: Boolean,
    val clockIs24H: Boolean,
    val clockShowDate: Boolean,
    val clockShowSwipeUp: Boolean,
    val clockAlignment: String,
    val iconSize: Int,
    val iconLabelSize: Int,
    val iconCornerRadius: Int,
    val showLabels: Boolean,
    val showShadow: Boolean,
    val desktopLayoutMode: String,
    val fullscreenEnabled: Boolean,
    val taskbarAutoHide: Boolean,
    val animationSpeed: Float,
    val hiddenApps: Set<String>
)
