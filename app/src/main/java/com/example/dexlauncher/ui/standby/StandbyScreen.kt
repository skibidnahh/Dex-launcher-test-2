package com.example.dexlauncher.ui.standby

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import com.example.dexlauncher.data.repository.LauncherSettings
import com.example.dexlauncher.service.StandbyStateManager
import com.example.dexlauncher.ui.wallpaper.WallpaperBackground
import kotlinx.coroutines.launch

@Composable
fun StandbyScreen(
    settings: LauncherSettings,
    onOpenSettings: () -> Unit
) {
    val coroutineScope = rememberCoroutineScope()
    val offsetYAnimatable = remember { Animatable(0f) }

    LaunchedEffect(Unit) {
        offsetYAnimatable.snapTo(0f)
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        val screenHeightPx = constraints.maxHeight.toFloat()
        val dismissThreshold = screenHeightPx * 0.22f

        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(screenHeightPx) {
                    detectVerticalDragGestures(
                        onVerticalDrag = { change, dragAmount ->
                            change.consume()
                            coroutineScope.launch {
                                val newOffset = (offsetYAnimatable.value + dragAmount).coerceAtMost(0f)
                                offsetYAnimatable.snapTo(newOffset)
                            }
                        },
                        onDragEnd = {
                            coroutineScope.launch {
                                val currentOffset = offsetYAnimatable.value
                                if (-currentOffset >= dismissThreshold) {
                                    offsetYAnimatable.animateTo(
                                        targetValue = -screenHeightPx,
                                        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                                    )
                                    StandbyStateManager.isInStandby = false
                                } else {
                                    offsetYAnimatable.animateTo(
                                        targetValue = 0f,
                                        animationSpec = spring(
                                            dampingRatio = Spring.DampingRatioMediumBouncy,
                                            stiffness = Spring.StiffnessLow
                                        )
                                    )
                                }
                            }
                        },
                        onDragCancel = {
                            coroutineScope.launch {
                                offsetYAnimatable.animateTo(
                                    targetValue = 0f,
                                    animationSpec = spring(
                                        dampingRatio = Spring.DampingRatioMediumBouncy,
                                        stiffness = Spring.StiffnessLow
                                    )
                                )
                            }
                        }
                    )
                }
                .graphicsLayer {
                    translationY = offsetYAnimatable.value
                    val progress = ((-offsetYAnimatable.value) / screenHeightPx.coerceAtLeast(1f)).coerceIn(0f, 1f)
                    alpha = 1f - (progress * 0.35f)
                }
        ) {
            WallpaperBackground(
                wallpaperUri = settings.standbyWallpaperUri,
                wallpaperType = settings.standbyWallpaperType,
                dim = settings.wallpaperDim,
                blur = settings.wallpaperBlur
            )

            StandbyClockView(
                settings = settings,
                onClick = onOpenSettings
            )
        }
    }
}
