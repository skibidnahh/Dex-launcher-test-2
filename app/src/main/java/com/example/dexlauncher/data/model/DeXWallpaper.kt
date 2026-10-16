package com.example.dexlauncher.data.model

import androidx.compose.ui.graphics.Color

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
    )
}
