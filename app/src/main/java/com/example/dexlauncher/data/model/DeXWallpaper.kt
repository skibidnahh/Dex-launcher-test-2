package com.example.dexlauncher.data.model

import androidx.compose.ui.graphics.Color
import java.util.Calendar

enum class DeXWallpaperPreset(
    val id: String,
    val title: String,
    val topColor: Color,
    val midColor: Color,
    val bottomColor: Color,
    val accentWaveColor: Color
) {
    NEBULA_PURPLE(
        id = "nebula_purple",
        title = "Galaxy DeX Nebula",
        topColor = Color(0xFF090D1A),
        midColor = Color(0xFF1E284A),
        bottomColor = Color(0xFFC7B199),
        accentWaveColor = Color(0x33A6B4E8)
    ),
    VIOLET_NIGHT(
        id = "violet_night",
        title = "Violet Dunes",
        topColor = Color(0xFF070B16),
        midColor = Color(0xFF18233C),
        bottomColor = Color(0xFFBCA697),
        accentWaveColor = Color(0x286377A8)
    ),
    DEEP_SPACE(
        id = "deep_space",
        title = "Deep Space Cosmos",
        topColor = Color(0xFF030712),
        midColor = Color(0xFF0F172A),
        bottomColor = Color(0xFF1E293B),
        accentWaveColor = Color(0x3338BDF8)
    ),
    ONYX_MINIMAL(
        id = "onyx_minimal",
        title = "Onyx Dark DeX",
        topColor = Color(0xFF0B0E14),
        midColor = Color(0xFF131722),
        bottomColor = Color(0xFF1E2433),
        accentWaveColor = Color(0x282B66FF)
    ),
    SUNRISE_GLOW(
        id = "sunrise_glow",
        title = "Bình minh Rực rỡ",
        topColor = Color(0xFF1A1C29),
        midColor = Color(0xFF6B3E4E),
        bottomColor = Color(0xFFF68E5F),
        accentWaveColor = Color(0x44FFC371)
    ),
    DAY_SKY(
        id = "day_sky",
        title = "Ban ngày Xanh ngọc",
        topColor = Color(0xFF0A2240),
        midColor = Color(0xFF1565C0),
        bottomColor = Color(0xFF42A5F5),
        accentWaveColor = Color(0x3380D8FF)
    ),
    SUNSET_HORIZON(
        id = "sunset_horizon",
        title = "Hoàng hôn Tím vàng",
        topColor = Color(0xFF180B26),
        midColor = Color(0xFF511845),
        bottomColor = Color(0xFFC70039),
        accentWaveColor = Color(0x44FF5733)
    ),
    MIDNIGHT_CYBER(
        id = "midnight_cyber",
        title = "Đêm Huyền bí Cyber",
        topColor = Color(0xFF02040A),
        midColor = Color(0xFF0B192C),
        bottomColor = Color(0xFF1E3E62),
        accentWaveColor = Color(0x4400E5FF)
    );

    companion object {
        fun fromId(id: String): DeXWallpaperPreset {
            return entries.find { it.id == id } ?: NEBULA_PURPLE
        }

        fun getTimeOfDayPreset(hour: Int = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)): DeXWallpaperPreset {
            return when (hour) {
                in 6..10 -> SUNRISE_GLOW      // 06:00 - 10:59 Sáng bình minh
                in 11..16 -> DAY_SKY          // 11:00 - 16:59 Ban ngày xanh trong
                in 17..19 -> SUNSET_HORIZON   // 17:00 - 19:59 Hoàng hôn tím
                else -> MIDNIGHT_CYBER        // 20:00 - 05:59 Đêm sao huyền ảo
            }
        }
    }
}
