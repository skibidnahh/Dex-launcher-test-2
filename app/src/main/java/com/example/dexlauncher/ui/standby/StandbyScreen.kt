package com.example.dexlauncher.ui.standby

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dexlauncher.data.repository.LauncherSettings
import com.example.dexlauncher.data.repository.SettingsRepository
import com.example.dexlauncher.service.StandbyStateManager
import com.example.dexlauncher.ui.settings.ComposeColorPicker
import com.example.dexlauncher.ui.wallpaper.WallpaperBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs
import kotlin.math.roundToInt

@Composable
fun StandbyScreen(
    settings: LauncherSettings,
    settingsRepository: SettingsRepository? = null,
    onOpenSettings: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val offsetYAnimatable = remember { Animatable(0f) }

    var isEditMode by remember { mutableStateOf(false) }
    var activeTab by remember { mutableIntStateOf(0) } // 0: Kiểu & Font, 1: Màu sắc, 2: Bố cục & Kích cỡ

    // Trạng thái giữ 5 giây để mở chế độ chỉnh sửa
    var isHoldingToEdit by remember { mutableStateOf(false) }
    var holdProgress by remember { mutableFloatStateOf(0f) }
    var holdTouchPos by remember { mutableStateOf(Offset.Zero) }
    var showQuickHint by remember { mutableStateOf(false) }

    // Trạng thái cục bộ khi chỉnh sửa (One UI 8 / Mẫu HTML)
    var localOffsetX by remember(settings.clockOffsetX) { mutableFloatStateOf(settings.clockOffsetX) }
    var localOffsetY by remember(settings.clockOffsetY) { mutableFloatStateOf(settings.clockOffsetY) }
    var localScale by remember(settings.clockScale) { mutableFloatStateOf(settings.clockScale) }
    var localFont by remember(settings.clockFont) { mutableStateOf(settings.clockFont) }
    var localFontPath by remember(settings.clockFontPath) { mutableStateOf(settings.clockFontPath) }
    var localColor by remember(settings.clockColor) { mutableIntStateOf(settings.clockColor) }
    var localWeight by remember(settings.clockWeight) { mutableIntStateOf(settings.clockWeight) }
    var local24H by remember(settings.clockIs24H) { mutableStateOf(settings.clockIs24H) }
    var localShowDate by remember(settings.clockShowDate) { mutableStateOf(settings.clockShowDate) }
    var localShowSwipe by remember(settings.clockShowSwipeUp) { mutableStateOf(settings.clockShowSwipeUp) }
    var localShadow by remember(settings.clockShadow) { mutableStateOf(settings.clockShadow) }
    var localAlignment by remember(settings.clockAlignment) { mutableStateOf(settings.clockAlignment) }
    var localClockStyle by remember(settings.clockStyle) { mutableStateOf(settings.clockStyle) }
    var localColorMode by remember(settings.clockColorMode) { mutableStateOf(settings.clockColorMode) }

    var isColorPickerOpen by remember { mutableStateOf(false) }

    // Bộ chọn font chữ tùy chỉnh (.ttf / .otf)
    val fontPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            coroutineScope.launch(Dispatchers.IO) {
                try {
                    val input = context.contentResolver.openInputStream(uri)
                    if (input != null) {
                        val dir = File(context.filesDir, "custom_fonts")
                        if (!dir.exists()) dir.mkdirs()
                        val fontFile = File(dir, "user_font_${System.currentTimeMillis()}.ttf")
                        FileOutputStream(fontFile).use { out -> input.copyTo(out) }
                        localFontPath = fontFile.absolutePath
                        localFont = "Custom"
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
        }
    }

    // Bộ chọn hình nền màn hình chờ
    val wallpaperPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null && settingsRepository != null) {
            coroutineScope.launch {
                settingsRepository.updateStandbyWallpaper(uri.toString(), "image")
            }
        }
    }

    fun saveChanges() {
        if (settingsRepository != null) {
            coroutineScope.launch {
                settingsRepository.updateClockCustomization(
                    font = localFont,
                    fontPath = localFontPath,
                    color = localColor,
                    scale = localScale,
                    offsetX = localOffsetX,
                    offsetY = localOffsetY,
                    opacity = settings.clockOpacity,
                    blur = settings.clockBlur,
                    weight = localWeight,
                    letterSpacing = settings.clockLetterSpacing,
                    shadow = localShadow,
                    is24H = local24H,
                    showDate = localShowDate,
                    showSwipeUp = localShowSwipe,
                    alignment = localAlignment,
                    clockStyle = localClockStyle,
                    clockColorMode = localColorMode
                )
            }
        }
        isEditMode = false
    }

    LaunchedEffect(Unit) {
        offsetYAnimatable.snapTo(0f)
    }

    // Tự động tắt gợi ý sau 2.5s
    LaunchedEffect(showQuickHint) {
        if (showQuickHint) {
            delay(2500L)
            showQuickHint = false
        }
    }

    val previewSettings = settings.copy(
        clockOffsetX = localOffsetX,
        clockOffsetY = localOffsetY,
        clockScale = localScale,
        clockFont = localFont,
        clockFontPath = localFontPath,
        clockColor = localColor,
        clockWeight = localWeight,
        clockIs24H = local24H,
        clockShowDate = localShowDate,
        clockShowSwipeUp = localShowSwipe,
        clockShadow = localShadow,
        clockAlignment = localAlignment,
        clockStyle = localClockStyle,
        clockColorMode = localColorMode
    )

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
                .then(
                    if (!isEditMode) {
                        Modifier
                            // Nhận diện thao tác giữ khoảng 5 giây để mở chỉnh sửa
                            .pointerInput(Unit) {
                                awaitEachGesture {
                                    val down = awaitFirstDown(requireUnconsumed = false)
                                    holdTouchPos = down.position
                                    isHoldingToEdit = true
                                    holdProgress = 0f

                                    val holdStartTime = System.currentTimeMillis()
                                    val holdTargetMs = 5000L

                                    val progressJob = coroutineScope.launch {
                                        while (isHoldingToEdit) {
                                            val elapsed = System.currentTimeMillis() - holdStartTime
                                            val currentProg = (elapsed.toFloat() / holdTargetMs).coerceIn(0f, 1f)
                                            holdProgress = currentProg
                                            if (elapsed >= holdTargetMs) {
                                                isEditMode = true
                                                isHoldingToEdit = false
                                                holdProgress = 0f
                                                break
                                            }
                                            delay(16L)
                                        }
                                    }

                                    var dragDistance = 0f
                                    var isPointerUp = false

                                    try {
                                        do {
                                            val event = awaitPointerEvent()
                                            val pointer = event.changes.firstOrNull { it.id == down.id }
                                            if (pointer == null || !pointer.pressed) {
                                                isPointerUp = true
                                                break
                                            }
                                            val diff = pointer.position - down.position
                                            dragDistance = abs(diff.y) + abs(diff.x)
                                            // Nếu ngón tay di chuyển nhiều (> 25px) thì hủy đếm giữ 5s (chuyển sang cử chỉ vuốt)
                                            if (dragDistance > 25f) {
                                                isHoldingToEdit = false
                                                progressJob.cancel()
                                                holdProgress = 0f
                                                break
                                            }
                                        } while (!isPointerUp && !isEditMode)
                                    } finally {
                                        if (isHoldingToEdit && holdProgress < 0.95f) {
                                            showQuickHint = true
                                        }
                                        isHoldingToEdit = false
                                        progressJob.cancel()
                                        holdProgress = 0f
                                    }
                                }
                            }
                            // Nhận diện vuốt dọc mở khóa
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
                    } else {
                        Modifier
                    }
                )
        ) {
            // Hình nền màn hình chờ
            WallpaperBackground(
                wallpaperUri = settings.standbyWallpaperUri,
                wallpaperType = settings.standbyWallpaperType,
                dim = settings.wallpaperDim,
                blur = settings.wallpaperBlur
            )

            // Đồng hồ trực quan tương tác toàn màn hình
            StandbyClockView(
                settings = previewSettings,
                isEditing = isEditMode,
                onPositionChanged = { newX, newY ->
                    localOffsetX = newX
                    localOffsetY = newY
                },
                onScaleChanged = { newScale ->
                    localScale = newScale
                },
                onClick = {
                    if (!isEditMode) {
                        showQuickHint = true
                    }
                }
            )

            // Vòng tròn hiển thị tiến trình khi đang giữ 5 giây để chỉnh sửa
            if (isHoldingToEdit && holdProgress > 0.05f) {
                val secondsLeft = ((1f - holdProgress) * 5f + 0.9f).toInt().coerceIn(1, 5)

                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.35f * holdProgress)),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier
                            .clip(RoundedCornerShape(24.dp))
                            .background(Color(0xCC1A1A1E))
                            .border(1.5.dp, Color(0xFF38BDF8), RoundedCornerShape(24.dp))
                            .padding(horizontal = 28.dp, vertical = 22.dp)
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier.size(76.dp)
                        ) {
                            CircularProgressIndicator(
                                progress = { holdProgress },
                                modifier = Modifier.fillMaxSize(),
                                color = Color(0xFF38BDF8),
                                strokeWidth = 5.dp,
                                trackColor = Color.White.copy(alpha = 0.15f)
                            )
                            Text(
                                text = "${secondsLeft}s",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        Text(
                            text = "Đang giữ để chỉnh sửa...",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.SemiBold
                        )

                        Text(
                            text = "Tiếp tục giữ ngón tay trên màn hình",
                            color = Color.LightGray,
                            fontSize = 12.sp
                        )
                    }
                }
            }

            // Gợi ý nhẹ khi chạm nhanh trên màn hình
            AnimatedVisibility(
                visible = showQuickHint && !isEditMode && !isHoldingToEdit,
                enter = fadeIn() + slideInVertically(initialOffsetY = { -30 }),
                exit = fadeOut() + slideOutVertically(targetOffsetY = { -30 }),
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .statusBarsPadding()
                    .padding(top = 20.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = Color(0xD91E1E22),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.18f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.TouchApp,
                            contentDescription = null,
                            tint = Color(0xFF38BDF8),
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = "Nhấn giữ 5 giây để chỉnh sửa màn hình chờ",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Nút mở khóa vân tay & thanh vuốt ở đáy màn hình khi không chỉnh sửa (như mẫu HTML)
            if (!isEditMode) {
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .navigationBarsPadding()
                        .padding(bottom = 20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Biểu tượng vân tay phát sáng (Fingerprint ring từ mẫu HTML)
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f))
                            .border(1.5.dp, Color.White.copy(alpha = 0.65f), CircleShape)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = {
                                    coroutineScope.launch {
                                        offsetYAnimatable.animateTo(
                                            targetValue = -screenHeightPx,
                                            animationSpec = tween(durationMillis = 240, easing = FastOutSlowInEasing)
                                        )
                                        StandbyStateManager.isInStandby = false
                                    }
                                }
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Fingerprint,
                            contentDescription = "Chạm để mở khóa",
                            tint = Color.White.copy(alpha = 0.85f),
                            modifier = Modifier.size(28.dp)
                        )
                    }

                    Text(
                        text = "Chạm hoặc vuốt lên để mở khóa",
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Normal
                    )
                }
            }

            // GIAO DIỆN CHỈNH SỬA MÀN HÌNH CHỜ (Khi isEditMode == true)
            if (isEditMode) {
                // Thanh điều khiển trên cùng (Top Bar)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Nút đổi ảnh nền (Photo Picker như #btnPhoto)
                    IconButton(
                        onClick = { wallpaperPickerLauncher.launch("image/*") },
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color.Black.copy(alpha = 0.55f), CircleShape)
                            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Đổi hình nền",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "Màn hình chờ",
                            color = Color.White,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Kéo để di chuyển đồng hồ",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }

                    // Nút hoàn tất (#btnDone)
                    Button(
                        onClick = { saveChanges() },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFA5)),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Text("Hoàn tất", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                // Tấm tùy biến dạng nổi One UI 8 ở phía dưới màn hình
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)),
                    color = Color(0xF21C1C1E),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
                    tonalElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 18.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Thanh Tab lựa chọn 3 mục (Kiểu & Font | Màu sắc | Bố cục)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(16.dp))
                                .background(Color.Black.copy(alpha = 0.4f))
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            val tabs = listOf(
                                0 to "Kiểu & Font",
                                1 to "Màu sắc",
                                2 to "Bố cục & Kích cỡ"
                            )
                            tabs.forEach { (index, title) ->
                                val isSelected = activeTab == index
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) Color(0xFF38BDF8) else Color.Transparent)
                                        .clickable { activeTab = index }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = title,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) Color.Black else Color.LightGray
                                    )
                                }
                            }
                        }

                        when (activeTab) {
                            0 -> {
                                // TAB 0: KIỂU ĐỒNG HỒ & PHÔNG CHỮ
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    // 1. Chọn Kiểu Đồng Hồ (One UI 8 / HTML Style)
                                    Text("Kiểu hiển thị đồng hồ:", color = Color.LightGray, fontSize = 12.sp)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val styles = listOf(
                                            "standard" to "Cổ điển (1 dòng)",
                                            "stacked" to "Xếp tầng (2 dòng)",
                                            "small" to "Nhỏ gọn",
                                            "analog" to "Kim tròn"
                                        )
                                        styles.forEach { (key, label) ->
                                            val isStyleSelected = localClockStyle.equals(key, ignoreCase = true)
                                            Box(
                                                modifier = Modifier
                                                    .weight(1f)
                                                    .clip(RoundedCornerShape(12.dp))
                                                    .background(if (isStyleSelected) Color(0xFF00BFA5) else Color(0xFF2C2C2E))
                                                    .border(
                                                        width = if (isStyleSelected) 1.5.dp else 1.dp,
                                                        color = if (isStyleSelected) Color.White else Color.Transparent,
                                                        shape = RoundedCornerShape(12.dp)
                                                    )
                                                    .clickable { localClockStyle = key }
                                                    .padding(vertical = 10.dp, horizontal = 4.dp),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = label,
                                                    color = Color.White,
                                                    fontSize = 10.sp,
                                                    textAlign = TextAlign.Center,
                                                    fontWeight = if (isStyleSelected) FontWeight.Bold else FontWeight.Normal,
                                                    maxLines = 2
                                                )
                                            }
                                        }
                                    }

                                    // 2. Chọn Phông Chữ (Các vòng tròn One UI font badges)
                                    Text("Phông chữ:", color = Color.LightGray, fontSize = 12.sp)
                                    val fontOptions = listOf(
                                        "Default" to "Aa",
                                        "Serif" to "Aa",
                                        "SansSerif" to "Aa",
                                        "Monospace" to "Aa",
                                        "Cursive" to "Aa",
                                        "Bold" to "Aa",
                                        "Digital" to "Aa"
                                    )

                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        items(fontOptions) { (fontKey, fontLabel) ->
                                            val isSelected = localFont.equals(fontKey, ignoreCase = true) && localFontPath.isBlank()
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isSelected) Color(0xFF38BDF8) else Color(0xFF2C2C2E))
                                                    .border(
                                                        width = if (isSelected) 2.dp else 1.dp,
                                                        color = if (isSelected) Color.White else Color.Transparent,
                                                        shape = CircleShape
                                                    )
                                                    .clickable {
                                                        localFont = fontKey
                                                        localFontPath = ""
                                                    },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Text(
                                                    text = fontLabel,
                                                    color = if (isSelected) Color.Black else Color.White,
                                                    fontSize = 15.sp,
                                                    fontWeight = if (fontKey == "Bold") FontWeight.ExtraBold else FontWeight.Bold,
                                                    fontFamily = when (fontKey.lowercase()) {
                                                        "serif" -> FontFamily.Serif
                                                        "sansserif" -> FontFamily.SansSerif
                                                        "monospace", "digital" -> FontFamily.Monospace
                                                        "cursive" -> FontFamily.Cursive
                                                        else -> FontFamily.Default
                                                    }
                                                )
                                            }
                                        }

                                        // Nút Thêm Font ngoài (+ TTF/OTF)
                                        item {
                                            val isCustomSelected = localFont == "Custom" || localFontPath.isNotBlank()
                                            Box(
                                                modifier = Modifier
                                                    .size(44.dp)
                                                    .clip(CircleShape)
                                                    .background(if (isCustomSelected) Color(0xFF38BDF8) else Color(0xFF2C2C2E))
                                                    .border(
                                                        width = if (isCustomSelected) 2.dp else 1.dp,
                                                        color = if (isCustomSelected) Color.White else Color.Gray,
                                                        shape = CircleShape
                                                    )
                                                    .clickable { fontPickerLauncher.launch("*/*") },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = "Thêm phông ngoài",
                                                    tint = if (isCustomSelected) Color.Black else Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }
                                    }

                                    // 3. Thanh trượt độ đậm phông chữ (Weight Slider)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Độ đậm: $localWeight", color = Color.LightGray, fontSize = 12.sp)
                                        Slider(
                                            value = localWeight.toFloat(),
                                            onValueChange = { localWeight = it.toInt() },
                                            valueRange = 100f..900f,
                                            steps = 7,
                                            modifier = Modifier.weight(1f).padding(start = 12.dp),
                                            colors = SliderDefaults.colors(
                                                thumbColor = Color.White,
                                                activeTrackColor = Color(0xFF38BDF8),
                                                inactiveTrackColor = Color(0xFF3A3A3C)
                                            )
                                        )
                                    }
                                }
                            }

                            1 -> {
                                // TAB 1: MÀU SẮC ĐỒNG HỒ (One UI / Mẫu HTML Gradient)
                                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                    Text("Màu chữ đồng hồ:", color = Color.LightGray, fontSize = 12.sp)

                                    // Nút Màu Gradient Tự động theo nền (như trong mẫu HTML: .clock.auto)
                                    val isGradientActive = localColorMode == "auto_gradient"
                                    Surface(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clip(RoundedCornerShape(14.dp))
                                            .clickable {
                                                localColorMode = if (isGradientActive) "solid" else "auto_gradient"
                                            },
                                        color = Color(0xFF2C2C2E),
                                        border = BorderStroke(
                                            width = if (isGradientActive) 2.dp else 1.dp,
                                            color = if (isGradientActive) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.1f)
                                        )
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                                            ) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(28.dp)
                                                        .clip(CircleShape)
                                                        .background(
                                                            Brush.verticalGradient(
                                                                listOf(Color(0xFFF3B9C4), Color(0xFFF7E1C9), Color(0xFFE9EFE4))
                                                            )
                                                        )
                                                )
                                                Column {
                                                    Text(
                                                        text = "Tự động (Gradient Pastel)",
                                                        color = Color.White,
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold
                                                    )
                                                    Text(
                                                        text = "Chữ đổi màu gradient ấm áp tự nhiên",
                                                        color = Color.Gray,
                                                        fontSize = 11.sp
                                                    )
                                                }
                                            }
                                            RadioButton(
                                                selected = isGradientActive,
                                                onClick = { localColorMode = "auto_gradient" },
                                                colors = RadioButtonDefaults.colors(selectedColor = Color(0xFF38BDF8))
                                            )
                                        }
                                    }

                                    Text("Màu sắc đơn sắc:", color = Color.LightGray, fontSize = 12.sp)

                                    // Danh sách màu preset (Trắng, Đen, Vàng hoàng hôn, Hồng san hô, Xanh ngọc...)
                                    val presetColors = listOf(
                                        0xFFFFFFFF.toInt(), // Trắng
                                        0xFF1C1C1E.toInt(), // Đen One UI
                                        0xFFFFD98A.toInt(), // Vàng hoàng hôn
                                        0xFFFF8B8B.toInt(), // Hồng cam san hô
                                        0xFF6EE7B7.toInt(), // Xanh bạc hà
                                        0xFFC084FC.toInt(), // Tím lavender
                                        0xFF38BDF8.toInt(), // Xanh neon
                                        0xFF94A3B8.toInt()  // Xám khói
                                    )

                                    LazyRow(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Nút mở Color Picker tùy chọn màu bất kỳ
                                        item {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF2C2C2E))
                                                    .border(1.dp, Color.Gray, CircleShape)
                                                    .clickable { isColorPickerOpen = true },
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Palette,
                                                    contentDescription = "Bảng chọn màu",
                                                    tint = Color.White,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        items(presetColors) { colorInt ->
                                            val isSelected = !isGradientActive && localColor == colorInt
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(colorInt))
                                                    .border(
                                                        width = if (isSelected) 3.dp else 1.dp,
                                                        color = if (isSelected) Color(0xFF38BDF8) else Color.DarkGray,
                                                        shape = CircleShape
                                                    )
                                                    .clickable {
                                                        localColorMode = "solid"
                                                        localColor = colorInt
                                                    }
                                            )
                                        }
                                    }
                                }
                            }

                            2 -> {
                                // TAB 2: BỐ CỤC, VỊ TRÍ & KÍCH CỠ
                                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                                    // Thanh trượt kích cỡ
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Cỡ đồng hồ: ${String.format("%.1f", localScale)}x", color = Color.LightGray, fontSize = 12.sp)
                                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                            FilterChip(
                                                selected = localScale in 0.75f..0.85f,
                                                onClick = { localScale = 0.8f },
                                                label = { Text("Nhỏ", fontSize = 10.sp) }
                                            )
                                            FilterChip(
                                                selected = localScale in 0.95f..1.05f,
                                                onClick = { localScale = 1.0f },
                                                label = { Text("Chuẩn", fontSize = 10.sp) }
                                            )
                                            FilterChip(
                                                selected = localScale in 1.35f..1.45f,
                                                onClick = { localScale = 1.4f },
                                                label = { Text("Lớn", fontSize = 10.sp) }
                                            )
                                            FilterChip(
                                                selected = localScale in 1.75f..1.85f,
                                                onClick = { localScale = 1.8f },
                                                label = { Text("Cực đại", fontSize = 10.sp) }
                                            )
                                        }
                                    }

                                    Slider(
                                        value = localScale,
                                        onValueChange = { localScale = it },
                                        valueRange = 0.4f..2.5f,
                                        colors = SliderDefaults.colors(
                                            thumbColor = Color.White,
                                            activeTrackColor = Color(0xFF38BDF8),
                                            inactiveTrackColor = Color(0xFF3A3A3C)
                                        )
                                    )

                                    // Lưới căn nhanh vị trí 9 điểm
                                    Text("Căn nhanh vị trí trên màn hình:", color = Color.LightGray, fontSize = 12.sp)
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { localOffsetX = 0.18f; localOffsetY = 0.22f; localAlignment = "left" },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(2.dp)
                                        ) {
                                            Text("Trên-Trái", fontSize = 10.sp)
                                        }
                                        Button(
                                            onClick = { localOffsetX = 0.5f; localOffsetY = 0.22f; localAlignment = "center" },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(2.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8))
                                        ) {
                                            Text("Trên-Giữa", fontSize = 10.sp, color = Color.Black, fontWeight = FontWeight.Bold)
                                        }
                                        OutlinedButton(
                                            onClick = { localOffsetX = 0.82f; localOffsetY = 0.22f; localAlignment = "right" },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(2.dp)
                                        ) {
                                            Text("Trên-Phải", fontSize = 10.sp)
                                        }
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        OutlinedButton(
                                            onClick = { localOffsetX = 0.18f; localOffsetY = 0.45f; localAlignment = "left" },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(2.dp)
                                        ) {
                                            Text("Giữa-Trái", fontSize = 10.sp)
                                        }
                                        Button(
                                            onClick = { localOffsetX = 0.5f; localOffsetY = 0.45f; localAlignment = "center" },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(2.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFA5))
                                        ) {
                                            Text("Trung tâm", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                        OutlinedButton(
                                            onClick = { localOffsetX = 0.82f; localOffsetY = 0.45f; localAlignment = "right" },
                                            modifier = Modifier.weight(1f),
                                            contentPadding = PaddingValues(2.dp)
                                        ) {
                                            Text("Giữa-Phải", fontSize = 10.sp)
                                        }
                                    }

                                    // Căn lề chữ
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Căn lề chữ:", color = Color.White, fontSize = 12.sp)
                                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                            FilterChip(
                                                selected = localAlignment == "left",
                                                onClick = { localAlignment = "left" },
                                                label = { Text("Trái", fontSize = 11.sp) }
                                            )
                                            FilterChip(
                                                selected = localAlignment == "center",
                                                onClick = { localAlignment = "center" },
                                                label = { Text("Giữa", fontSize = 11.sp) }
                                            )
                                            FilterChip(
                                                selected = localAlignment == "right",
                                                onClick = { localAlignment = "right" },
                                                label = { Text("Phải", fontSize = 11.sp) }
                                            )
                                        }
                                    }

                                    // Các công tắc cài đặt tiện ích
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Định dạng 24 giờ", color = Color.White, fontSize = 12.sp)
                                        Switch(checked = local24H, onCheckedChange = { local24H = it })
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Hiển thị ngày tháng", color = Color.White, fontSize = 12.sp)
                                        Switch(checked = localShowDate, onCheckedChange = { localShowDate = it })
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Gợi ý 'Vuốt lên để mở'", color = Color.White, fontSize = 12.sp)
                                        Switch(checked = localShowSwipe, onCheckedChange = { localShowSwipe = it })
                                    }

                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text("Bóng đổ chữ (Shadow)", color = Color.White, fontSize = 12.sp)
                                        Switch(checked = localShadow, onCheckedChange = { localShadow = it })
                                    }
                                }
                            }
                        }
                    }
                }

                // Hộp thoại chọn màu tùy chỉnh RGB
                if (isColorPickerOpen) {
                    AlertDialog(
                        onDismissRequest = { isColorPickerOpen = false },
                        title = { Text("Chọn màu sắc đồng hồ", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        text = {
                            ComposeColorPicker(
                                selectedColor = localColor,
                                onColorChanged = {
                                    localColor = it
                                    localColorMode = "solid"
                                }
                            )
                        },
                        confirmButton = {
                            Button(
                                onClick = { isColorPickerOpen = false },
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFA5))
                            ) {
                                Text("Xác nhận")
                            }
                        }
                    )
                }
            }
        }
    }
}
