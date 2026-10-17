package com.example.dexlauncher.ui.standby

import android.graphics.Typeface
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dexlauncher.data.repository.LauncherSettings
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs

@Composable
fun StandbyClockView(
    settings: LauncherSettings,
    onClick: () -> Unit = {},
    isEditing: Boolean = false,
    onPositionChanged: ((Float, Float) -> Unit)? = null,
    onScaleChanged: ((Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            val cal = Calendar.getInstance()
            val delay = (60 - cal.get(Calendar.SECOND)) * 1000L - cal.get(Calendar.MILLISECOND)
            kotlinx.coroutines.delay(if (delay > 0) delay else 1000L)
        }
    }

    val timeStr = remember(currentTime, settings.clockIs24H) {
        val pattern = if (settings.clockIs24H) "HH:mm" else "hh:mm a"
        SimpleDateFormat(pattern, Locale.getDefault()).format(Date(currentTime))
    }

    val dateStr = remember(currentTime) {
        SimpleDateFormat("EEEE, dd 'tháng' MM", Locale("vi", "VN")).format(Date(currentTime))
    }

    val customFontFamily = remember(settings.clockFontPath, settings.clockFont) {
        if (settings.clockFontPath.isNotBlank()) {
            try {
                val file = File(settings.clockFontPath)
                if (file.exists() && file.canRead()) {
                    val tf = Typeface.createFromFile(file)
                    FontFamily(tf)
                } else null
            } catch (_: Exception) {
                null
            }
        } else {
            when (settings.clockFont.lowercase()) {
                "serif" -> FontFamily.Serif
                "sansserif", "sans" -> FontFamily.SansSerif
                "monospace", "mono" -> FontFamily.Monospace
                "cursive", "script" -> FontFamily.Cursive
                else -> FontFamily.Default
            }
        }
    }

    val clockColor = remember(settings.clockColor) { Color(settings.clockColor) }
    val shadow = remember(settings.clockShadow) {
        if (settings.clockShadow) {
            Shadow(
                color = Color.Black.copy(alpha = 0.85f),
                offset = Offset(3f, 3f),
                blurRadius = 8f
            )
        } else {
            Shadow.None
        }
    }

    val (horizontalAlign, textAlign) = remember(settings.clockAlignment) {
        when (settings.clockAlignment.lowercase()) {
            "left", "start" -> Alignment.Start to TextAlign.Start
            "right", "end" -> Alignment.End to TextAlign.End
            else -> Alignment.CenterHorizontally to TextAlign.Center
        }
    }

    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.toFloat()
    val screenHeightDp = configuration.screenHeightDp.toFloat()
    val minDimension = screenWidthDp.coerceAtMost(screenHeightDp)

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val containerWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val containerHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val targetCenterX = (settings.clockOffsetX.coerceIn(0f, 1f) * containerWidthPx)
        val targetCenterY = (settings.clockOffsetY.coerceIn(0f, 1f) * containerHeightPx)

        var clockSize by remember { mutableStateOf(IntSize.Zero) }

        // Tự động căn chỉnh kích thước font chữ phù hợp theo kích thước màn hình & DPI
        val responsiveBaseSp = (minDimension * 0.16f).coerceIn(38f, 130f)
        val scale = settings.clockScale.coerceIn(0.4f, 3.5f)
        val timeFontSize = (responsiveBaseSp * scale).sp
        val dateFontSize = (timeFontSize.value * 0.28f).coerceIn(12f, 36f).sp
        val swipeFontSize = (timeFontSize.value * 0.22f).coerceIn(10f, 26f).sp

        // Hiển thị đường gióng căn giữa khi đang chỉnh sửa
        if (isEditing) {
            val isCenterHoriz = abs(settings.clockOffsetX - 0.5f) < 0.02f
            val isCenterVert = abs(settings.clockOffsetY - 0.5f) < 0.02f
            if (isCenterHoriz) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(1.5.dp)
                        .align(Alignment.Center)
                        .background(Color(0xFF00E5FF).copy(alpha = 0.75f))
                )
            }
            if (isCenterVert) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(1.5.dp)
                        .align(Alignment.Center)
                        .background(Color(0xFF00E5FF).copy(alpha = 0.75f))
                )
            }
        }

        Box(
            modifier = Modifier
                .onSizeChanged { clockSize = it }
                .offset {
                    val rawX = (targetCenterX - clockSize.width / 2f).toInt()
                    val rawY = (targetCenterY - clockSize.height / 2f).toInt()
                    val maxX = (containerWidthPx - clockSize.width).toInt().coerceAtLeast(0)
                    val maxY = (containerHeightPx - clockSize.height).toInt().coerceAtLeast(0)
                    IntOffset(rawX.coerceIn(0, maxX), rawY.coerceIn(0, maxY))
                }
                .then(
                    if (isEditing && onPositionChanged != null) {
                        Modifier.pointerInput(containerWidthPx, containerHeightPx) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                val newX = (settings.clockOffsetX + dragAmount.x / containerWidthPx).coerceIn(0.02f, 0.98f)
                                val newY = (settings.clockOffsetY + dragAmount.y / containerHeightPx).coerceIn(0.02f, 0.98f)
                                val snappedX = if (abs(newX - 0.5f) < 0.025f) 0.5f else newX
                                val snappedY = if (abs(newY - 0.5f) < 0.025f) 0.5f else newY
                                onPositionChanged(snappedX, snappedY)
                            }
                        }
                    } else {
                        Modifier.clickable(
                            interactionSource = remember { MutableInteractionSource() },
                            indication = null,
                            onClick = onClick
                        )
                    }
                )
        ) {
            // Khung chọn phong cách One UI khi đang chỉnh sửa
            Box(
                modifier = Modifier
                    .then(
                        if (isEditing) {
                            Modifier
                                .border(1.5.dp, Color(0xFF00E5FF).copy(alpha = 0.8f), RoundedCornerShape(12.dp))
                                .padding(12.dp)
                        } else {
                            Modifier.padding(12.dp)
                        }
                    )
            ) {
                Column(
                    horizontalAlignment = horizontalAlign,
                    modifier = Modifier
                        .alpha(settings.clockOpacity.coerceIn(0.1f, 1.0f))
                        .let { if (settings.clockBlur > 0f) it.blur(settings.clockBlur.dp) else it }
                ) {
                    Text(
                        text = timeStr,
                        color = clockColor,
                        fontSize = timeFontSize,
                        fontFamily = customFontFamily,
                        fontWeight = FontWeight(settings.clockWeight.coerceIn(100, 900)),
                        letterSpacing = settings.clockLetterSpacing.sp,
                        textAlign = textAlign,
                        style = androidx.compose.ui.text.TextStyle(shadow = shadow),
                        softWrap = false,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    if (settings.clockShowDate) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = dateStr,
                            color = clockColor.copy(alpha = 0.85f),
                            fontSize = dateFontSize,
                            fontFamily = customFontFamily,
                            letterSpacing = (settings.clockLetterSpacing * 0.5f).sp,
                            textAlign = textAlign,
                            style = androidx.compose.ui.text.TextStyle(shadow = shadow),
                            softWrap = false,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (settings.clockShowSwipeUp) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Vuốt lên để mở",
                            color = clockColor.copy(alpha = 0.65f),
                            fontSize = swipeFontSize,
                            fontFamily = customFontFamily,
                            textAlign = textAlign,
                            style = androidx.compose.ui.text.TextStyle(shadow = shadow),
                            softWrap = false,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Điểm neo điều chỉnh kích cỡ ở 4 góc khung chọn (One UI 8 style)
                if (isEditing && onScaleChanged != null) {
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 6.dp, y = 6.dp)
                            .background(Color.White, CircleShape)
                            .border(2.dp, Color(0xFF00E5FF), CircleShape)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val delta = (dragAmount.x + dragAmount.y) / 100f
                                    val newScale = (settings.clockScale + delta).coerceIn(0.4f, 3.5f)
                                    onScaleChanged(newScale)
                                }
                            }
                    )
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.TopStart)
                            .offset(x = (-6).dp, y = (-6).dp)
                            .background(Color.White, CircleShape)
                            .border(2.dp, Color(0xFF00E5FF), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 6.dp, y = (-6).dp)
                            .background(Color.White, CircleShape)
                            .border(2.dp, Color(0xFF00E5FF), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(14.dp)
                            .align(Alignment.BottomStart)
                            .offset(x = (-6).dp, y = 6.dp)
                            .background(Color.White, CircleShape)
                            .border(2.dp, Color(0xFF00E5FF), CircleShape)
                    )
                }
            }
        }
    }
}
