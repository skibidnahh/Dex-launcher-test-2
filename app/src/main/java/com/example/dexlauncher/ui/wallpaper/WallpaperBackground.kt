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
import java.io.File

@OptIn(UnstableApi::class)
@Composable
fun WallpaperBackground(
    wallpaperUri: String,
    wallpaperType: String,
    dim: Float,
    blur: Float
) {
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
                    DefaultPresetWallpaper()
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
                    DefaultPresetWallpaper()
                }
            }
            else -> {
                DefaultPresetWallpaper()
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
fun DefaultPresetWallpaper() {
    Canvas(modifier = Modifier.fillMaxSize()) {
        val brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF0F2027), Color(0xFF203A43), Color(0xFF2C5364))
        )
        drawRect(brush = brush)
    }
}
