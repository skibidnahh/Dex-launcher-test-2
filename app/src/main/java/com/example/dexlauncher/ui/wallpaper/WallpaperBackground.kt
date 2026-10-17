package com.example.dexlauncher.ui.wallpaper

import android.net.Uri
import androidx.annotation.OptIn
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.ui.PlayerView
import coil.compose.AsyncImage
import com.example.dexlauncher.data.model.DeXWallpaperPreset
import kotlinx.coroutines.delay
import java.io.File
import java.util.Calendar

@OptIn(UnstableApi::class)
@Composable
fun WallpaperBackground(
    wallpaperUri: String,
    wallpaperType: String,
    dim: Float,
    blur: Float
) {
    // Theo dõi giờ để tự động chuyển hình nền khi đặt chế độ 'dynamic_time'
    var currentHour by remember { mutableIntStateOf(Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) }

    LaunchedEffect(wallpaperType) {
        if (wallpaperType == "dynamic_time") {
            while (true) {
                currentHour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)
                delay(60000L) // Cập nhật mỗi phút
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        when (wallpaperType) {
            "image" -> {
                if (wallpaperUri.isNotBlank()) {
                    AsyncImage(
                        model = if (wallpaperUri.startsWith("content://") || wallpaperUri.startsWith("file://")) Uri.parse(wallpaperUri) else File(wallpaperUri),
                        contentDescription = "Wallpaper Image",
                        modifier = Modifier
                            .fillMaxSize()
                            .let { if (blur > 0f) it.blur(blur.dp) else it }
                    )
                } else {
                    PresetWallpaperCanvas(DeXWallpaperPreset.NEBULA_PURPLE)
                }
            }
            "video" -> {
                if (wallpaperUri.isNotBlank()) {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    val exoPlayer = remember(wallpaperUri) {
                        ExoPlayer.Builder(context).build().apply {
                            repeatMode = Player.REPEAT_MODE_ALL
                            volume = 0f
                            val mediaItem = MediaItem.fromUri(Uri.parse(wallpaperUri))
                            setMediaItem(mediaItem)
                            prepare()
                            playWhenReady = true
                            addListener(object : Player.Listener {
                                override fun onPlaybackStateChanged(playbackState: Int) {
                                    if (playbackState == Player.STATE_ENDED) {
                                        seekTo(0)
                                        play()
                                    }
                                }
                            })
                        }
                    }

                    DisposableEffect(wallpaperUri) {
                        onDispose {
                            exoPlayer.release()
                        }
                    }

                    AndroidView(
                        factory = { ctx ->
                            PlayerView(ctx).apply {
                                player = exoPlayer
                                useController = false
                                layoutParams = android.view.ViewGroup.LayoutParams(
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                                )
                            }
                        },
                        modifier = Modifier
                            .fillMaxSize()
                            .let { if (blur > 0f) it.blur(blur.dp) else it }
                    )
                } else {
                    PresetWallpaperCanvas(DeXWallpaperPreset.NEBULA_PURPLE)
                }
            }
            "dynamic_time" -> {
                val timePreset = remember(currentHour) {
                    DeXWallpaperPreset.getTimeOfDayPreset(currentHour)
                }
                PresetWallpaperCanvas(timePreset, blur = blur)
            }
            "preset" -> {
                val preset = remember(wallpaperUri) {
                    DeXWallpaperPreset.fromId(wallpaperUri)
                }
                PresetWallpaperCanvas(preset, blur = blur)
            }
            else -> {
                PresetWallpaperCanvas(DeXWallpaperPreset.NEBULA_PURPLE, blur = blur)
            }
        }

        if (dim > 0f) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = dim))
            )
        }
    }
}

@Composable
fun PresetWallpaperCanvas(
    preset: DeXWallpaperPreset,
    blur: Float = 0f
) {
    Canvas(
        modifier = Modifier
            .fillMaxSize()
            .let { if (blur > 0f) it.blur(blur.dp) else it }
    ) {
        val w = size.width
        val h = size.height

        // Gradient nền chính theo góc chéo hiện đại
        val mainBrush = Brush.linearGradient(
            colors = listOf(preset.topColor, preset.midColor, preset.bottomColor),
            start = Offset(0f, 0f),
            end = Offset(w, h)
        )
        drawRect(brush = mainBrush)

        // Lớp sóng ánh sáng nhấn (Accent atmospheric glow) phong cách Samsung DeX
        val glowBrush = Brush.radialGradient(
            colors = listOf(preset.accentWaveColor, Color.Transparent),
            center = Offset(w * 0.72f, h * 0.35f),
            radius = w * 0.65f
        )
        drawCircle(
            brush = glowBrush,
            radius = w * 0.65f,
            center = Offset(w * 0.72f, h * 0.35f)
        )

        val bottomGlowBrush = Brush.radialGradient(
            colors = listOf(preset.accentWaveColor.copy(alpha = 0.25f), Color.Transparent),
            center = Offset(w * 0.25f, h * 0.85f),
            radius = w * 0.5f
        )
        drawCircle(
            brush = bottomGlowBrush,
            radius = w * 0.5f,
            center = Offset(w * 0.25f, h * 0.85f)
        )
    }
}
