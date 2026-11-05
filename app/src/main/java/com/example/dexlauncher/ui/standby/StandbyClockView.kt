package com.example.dexlauncher.ui.standby

import android.graphics.Typeface
import androidx.compose.foundation.Canvas
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dexlauncher.data.repository.LauncherSettings
import kotlinx.coroutines.delay
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun StandbyClockView(
    settings: LauncherSettings,
    onClick: () -> Unit = {},
    isEditing: Boolean = false,
    onPositionChanged: ((Float, Float) -> Unit)? = null,
    onScaleChanged: ((Float) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    var currentTime by remember { mutableLongStateOf(System.currentTimeMillis()) }

    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000L)
        }
    }

    val cal = remember(currentTime) {
        Calendar.getInstance().apply { timeInMillis = currentTime }
    }

    val hourStr = remember(currentTime, settings.clockIs24H) {
        val pattern = if (settings.clockIs24H) "HH" else "hh"
        SimpleDateFormat(pattern, Locale.getDefault()).format(Date(currentTime))
    }

    val minuteStr = remember(currentTime) {
        SimpleDateFormat("mm", Locale.getDefault()).format(Date(currentTime))
    }

    val amPmStr = remember(currentTime, settings.clockIs24H) {
        if (!settings.clockIs24H) {
            SimpleDateFormat("a", Locale.getDefault()).format(Date(currentTime))
        } else ""
    }

    val timeStr = remember(currentTime, settings.clockIs24H) {
        if (settings.clockIs24H) "$hourStr:$minuteStr" else "$hourStr:$minuteStr $amPmStr"
    }

    val dateStr = remember(currentTime) {
        SimpleDateFormat("EEEE, dd 'tháng' MM", Locale("vi", "VN")).format(Date(currentTime))
            .replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("vi", "VN")) else it.toString() }
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
                "monospace", "digital", "mono" -> FontFamily.Monospace
                "cursive", "script" -> FontFamily.Cursive
                else -> FontFamily.Default
            }
        }
    }

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

    val isAutoGradient = settings.clockColorMode.equals("auto_gradient", ignoreCase = true)
    val autoGradientBrush = remember {
        Brush.verticalGradient(
            colors = listOf(
                Color(0xFFF3B9C4), // Hồng phấn pastel
                Color(0xFFF7E1C9), // Kem ấm áp
                Color(0xFFE9EFE4)  // Xanh ngọc nhẹ
            )
        )
    }

    val baseClockColor = remember(settings.clockColor) { Color(settings.clockColor) }

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

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val containerWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val containerHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val targetCenterX = (settings.clockOffsetX.coerceIn(0.05f, 0.95f) * containerWidthPx)
        val targetCenterY = (settings.clockOffsetY.coerceIn(0.05f, 0.95f) * containerHeightPx)

        var clockSize by remember { mutableStateOf(IntSize.Zero) }

        val responsiveBaseSp = (minDimension * 0.17f).coerceIn(40f, 130f)
        val scale = settings.clockScale.coerceIn(0.4f, 3.2f)
        val timeFontSize = (responsiveBaseSp * scale).sp
        val dateFontSize = (timeFontSize.value * 0.22f).coerceIn(12f, 32f).sp
        val swipeFontSize = (timeFontSize.value * 0.18f).coerceIn(11f, 24f).sp

        // Đường gióng căn giữa khi đang chỉnh sửa
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
                                val newX = (settings.clockOffsetX + dragAmount.x / containerWidthPx).coerceIn(0.05f, 0.95f)
                                val newY = (settings.clockOffsetY + dragAmount.y / containerHeightPx).coerceIn(0.05f, 0.95f)
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
            // Khung viền chọn One UI 8 khi đang chỉnh sửa
            Box(
                modifier = Modifier
                    .then(
                        if (isEditing) {
                            Modifier
                                .border(
                                    width = 1.8.dp,
                                    color = Color(0xFF38BDF8).copy(alpha = 0.9f),
                                    shape = RoundedCornerShape(16.dp)
                                )
                                .background(Color.White.copy(alpha = 0.06f), RoundedCornerShape(16.dp))
                                .padding(14.dp)
                        } else {
                            Modifier.padding(8.dp)
                        }
                    )
            ) {
                Column(
                    horizontalAlignment = horizontalAlign,
                    modifier = Modifier
                        .alpha(settings.clockOpacity.coerceIn(0.1f, 1.0f))
                        .let { if (settings.clockBlur > 0f) it.blur(settings.clockBlur.dp) else it }
                ) {
                    // Dòng ngày tháng phía trên (One UI / iOS 18 style)
                    if (settings.clockShowDate) {
                        Text(
                            text = dateStr,
                            color = if (isAutoGradient) Color(0xFFF7E1C9) else baseClockColor.copy(alpha = 0.9f),
                            fontSize = dateFontSize,
                            fontFamily = customFontFamily,
                            fontWeight = FontWeight.Medium,
                            letterSpacing = (settings.clockLetterSpacing * 0.3f).sp,
                            textAlign = textAlign,
                            style = TextStyle(shadow = shadow),
                            softWrap = false,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }

                    // Thân đồng hồ chính tùy theo kiểu dáng (clockStyle)
                    when (settings.clockStyle.lowercase()) {
                        "stacked", "stack" -> {
                            // Kiểu 2 tầng lớn (Giờ trên, Phút dưới)
                            Column(
                                horizontalAlignment = horizontalAlign,
                                verticalArrangement = Arrangement.spacedBy((-16 * scale).dp)
                            ) {
                                val textStyle = if (isAutoGradient) {
                                    TextStyle(
                                        brush = autoGradientBrush,
                                        shadow = shadow,
                                        lineHeight = (timeFontSize.value * 0.88f).sp
                                    )
                                } else {
                                    TextStyle(
                                        color = baseClockColor,
                                        shadow = shadow,
                                        lineHeight = (timeFontSize.value * 0.88f).sp
                                    )
                                }

                                Text(
                                    text = hourStr,
                                    fontSize = (timeFontSize.value * 1.15f).sp,
                                    fontFamily = customFontFamily,
                                    fontWeight = FontWeight(settings.clockWeight.coerceIn(100, 900)),
                                    letterSpacing = (settings.clockLetterSpacing - 1.5f).sp,
                                    textAlign = textAlign,
                                    style = textStyle,
                                    softWrap = false,
                                    maxLines = 1
                                )
                                Text(
                                    text = minuteStr,
                                    fontSize = (timeFontSize.value * 1.15f).sp,
                                    fontFamily = customFontFamily,
                                    fontWeight = FontWeight(settings.clockWeight.coerceIn(100, 900)),
                                    letterSpacing = (settings.clockLetterSpacing - 1.5f).sp,
                                    textAlign = textAlign,
                                    style = textStyle,
                                    softWrap = false,
                                    maxLines = 1
                                )
                            }
                        }

                        "small", "compact" -> {
                            // Kiểu nhỏ gọn tối giản
                            val compactSize = (timeFontSize.value * 0.65f).sp
                            val textStyle = if (isAutoGradient) {
                                TextStyle(brush = autoGradientBrush, shadow = shadow)
                            } else {
                                TextStyle(color = baseClockColor, shadow = shadow)
                            }

                            Row(
                                verticalAlignment = Alignment.Bottom,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = timeStr,
                                    fontSize = compactSize,
                                    fontFamily = customFontFamily,
                                    fontWeight = FontWeight(settings.clockWeight.coerceIn(100, 900)),
                                    letterSpacing = settings.clockLetterSpacing.sp,
                                    textAlign = textAlign,
                                    style = textStyle,
                                    softWrap = false,
                                    maxLines = 1
                                )
                            }
                        }

                        "analog" -> {
                            // Mặt đồng hồ kim tròn phong cách sang trọng
                            val analogSizeDp = (160f * scale).coerceIn(100f, 320f).dp
                            val dialColor = if (isAutoGradient) Color(0xFFF7E1C9) else baseClockColor

                            Box(
                                modifier = Modifier
                                    .size(analogSizeDp)
                                    .padding(4.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val center = Offset(size.width / 2f, size.height / 2f)
                                    val radius = size.minDimension / 2f - 6.dp.toPx()

                                    // Vòng viền ngoài
                                    drawCircle(
                                        color = dialColor.copy(alpha = 0.25f),
                                        radius = radius,
                                        center = center,
                                        style = Stroke(width = 2.5.dp.toPx())
                                    )

                                    // 12 Vạch chia giờ
                                    for (i in 0 until 12) {
                                        val angleRad = Math.toRadians((i * 30.0) - 90.0)
                                        val isMajor = (i % 3 == 0)
                                        val tickLen = if (isMajor) 10.dp.toPx() else 5.dp.toPx()
                                        val startR = radius - tickLen
                                        val endR = radius

                                        val startX = center.x + startR * cos(angleRad).toFloat()
                                        val startY = center.y + startR * sin(angleRad).toFloat()
                                        val endX = center.x + endR * cos(angleRad).toFloat()
                                        val endY = center.y + endR * sin(angleRad).toFloat()

                                        drawLine(
                                            color = if (isMajor) dialColor else dialColor.copy(alpha = 0.5f),
                                            start = Offset(startX, startY),
                                            end = Offset(endX, endY),
                                            strokeWidth = if (isMajor) 2.5.dp.toPx() else 1.2.dp.toPx(),
                                            cap = StrokeCap.Round
                                        )
                                    }

                                    // Tính góc cho các kim
                                    val hours = cal.get(Calendar.HOUR)
                                    val minutes = cal.get(Calendar.MINUTE)
                                    val seconds = cal.get(Calendar.SECOND)

                                    val hourAngle = Math.toRadians(((hours + minutes / 60.0) * 30.0) - 90.0)
                                    val minuteAngle = Math.toRadians(((minutes + seconds / 60.0) * 6.0) - 90.0)
                                    val secondAngle = Math.toRadians((seconds * 6.0) - 90.0)

                                    // Kim giờ
                                    val hourLen = radius * 0.52f
                                    drawLine(
                                        color = dialColor,
                                        start = center,
                                        end = Offset(
                                            center.x + hourLen * cos(hourAngle).toFloat(),
                                            center.y + hourLen * sin(hourAngle).toFloat()
                                        ),
                                        strokeWidth = 4.5.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )

                                    // Kim phút
                                    val minLen = radius * 0.76f
                                    drawLine(
                                        color = dialColor.copy(alpha = 0.9f),
                                        start = center,
                                        end = Offset(
                                            center.x + minLen * cos(minuteAngle).toFloat(),
                                            center.y + minLen * sin(minuteAngle).toFloat()
                                        ),
                                        strokeWidth = 3.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )

                                    // Kim giây (Màu nhấn san hô rực rỡ)
                                    val secLen = radius * 0.85f
                                    drawLine(
                                        color = Color(0xFFFF5252),
                                        start = center,
                                        end = Offset(
                                            center.x + secLen * cos(secondAngle).toFloat(),
                                            center.y + secLen * sin(secondAngle).toFloat()
                                        ),
                                        strokeWidth = 1.5.dp.toPx(),
                                        cap = StrokeCap.Round
                                    )

                                    // Chốt trung tâm
                                    drawCircle(color = dialColor, radius = 5.dp.toPx(), center = center)
                                    drawCircle(color = Color(0xFFFF5252), radius = 2.5.dp.toPx(), center = center)
                                }
                            }
                        }

                        else -> {
                            // Kiểu tiêu chuẩn 1 dòng ("12:45")
                            val textStyle = if (isAutoGradient) {
                                TextStyle(brush = autoGradientBrush, shadow = shadow)
                            } else {
                                TextStyle(color = baseClockColor, shadow = shadow)
                            }

                            Text(
                                text = timeStr,
                                fontSize = timeFontSize,
                                fontFamily = customFontFamily,
                                fontWeight = FontWeight(settings.clockWeight.coerceIn(100, 900)),
                                letterSpacing = settings.clockLetterSpacing.sp,
                                textAlign = textAlign,
                                style = textStyle,
                                softWrap = false,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    // Dòng gợi ý vuốt mở khóa
                    if (settings.clockShowSwipeUp) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Vuốt lên để mở khóa",
                            color = (if (isAutoGradient) Color(0xFFE9EFE4) else baseClockColor).copy(alpha = 0.7f),
                            fontSize = swipeFontSize,
                            fontFamily = customFontFamily,
                            fontWeight = FontWeight.Normal,
                            textAlign = textAlign,
                            style = TextStyle(shadow = shadow),
                            softWrap = false,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                // Tay nắm điều chỉnh kích cỡ 4 góc khi đang chỉnh sửa
                if (isEditing && onScaleChanged != null) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.BottomEnd)
                            .offset(x = 8.dp, y = 8.dp)
                            .background(Color.White, CircleShape)
                            .border(2.dp, Color(0xFF38BDF8), CircleShape)
                            .pointerInput(Unit) {
                                detectDragGestures { change, dragAmount ->
                                    change.consume()
                                    val delta = (dragAmount.x + dragAmount.y) / 100f
                                    val newScale = (settings.clockScale + delta).coerceIn(0.4f, 3.2f)
                                    onScaleChanged(newScale)
                                }
                            }
                    )
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.TopStart)
                            .offset(x = (-8).dp, y = (-8).dp)
                            .background(Color.White, CircleShape)
                            .border(2.dp, Color(0xFF38BDF8), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.TopEnd)
                            .offset(x = 8.dp, y = (-8).dp)
                            .background(Color.White, CircleShape)
                            .border(2.dp, Color(0xFF38BDF8), CircleShape)
                    )
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .align(Alignment.BottomStart)
                            .offset(x = (-8).dp, y = 8.dp)
                            .background(Color.White, CircleShape)
                            .border(2.dp, Color(0xFF38BDF8), CircleShape)
                    )
                }
            }
        }
    }
}
