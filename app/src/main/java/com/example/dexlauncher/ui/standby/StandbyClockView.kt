package com.example.dexlauncher.ui.standby

import android.graphics.Typeface
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.layout.onSizeChanged
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

@Composable
fun StandbyClockView(
    settings: LauncherSettings,
    onClick: () -> Unit,
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

    val customFontFamily = remember(settings.clockFontPath) {
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
        } else null
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

    BoxWithConstraints(
        modifier = modifier.fillMaxSize()
    ) {
        val containerWidthPx = constraints.maxWidth
        val containerHeightPx = constraints.maxHeight
        val targetCenterX = (settings.clockOffsetX.coerceIn(0f, 1f) * containerWidthPx)
        val targetCenterY = (settings.clockOffsetY.coerceIn(0f, 1f) * containerHeightPx)

        var clockSize by remember { mutableStateOf(IntSize.Zero) }

        val containerWidthDp = maxWidth.value
        val scale = settings.clockScale.coerceIn(0.4f, 3.0f)
        val timeFontSize = ((containerWidthDp * 0.082f).coerceIn(28f, 120f) * scale).sp
        val dateFontSize = ((timeFontSize.value * 0.28f).coerceIn(12f, 32f)).sp
        val swipeFontSize = ((timeFontSize.value * 0.22f).coerceIn(11f, 24f)).sp

        Column(
            horizontalAlignment = horizontalAlign,
            modifier = Modifier
                .onSizeChanged { clockSize = it }
                .offset {
                    val rawX = (targetCenterX - clockSize.width / 2f).toInt()
                    val rawY = (targetCenterY - clockSize.height / 2f).toInt()
                    val maxX = (containerWidthPx - clockSize.width).coerceAtLeast(0)
                    val maxY = (containerHeightPx - clockSize.height).coerceAtLeast(0)
                    IntOffset(rawX.coerceIn(0, maxX), rawY.coerceIn(0, maxY))
                }
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick
                )
                .padding(16.dp)
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
                Spacer(modifier = Modifier.height(8.dp))
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
    }
}
