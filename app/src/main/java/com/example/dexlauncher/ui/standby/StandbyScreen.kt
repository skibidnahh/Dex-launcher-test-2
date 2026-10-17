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
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectVerticalDragGestures
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dexlauncher.data.repository.LauncherSettings
import com.example.dexlauncher.data.repository.SettingsRepository
import com.example.dexlauncher.service.StandbyStateManager
import com.example.dexlauncher.ui.settings.ComposeColorPicker
import com.example.dexlauncher.ui.wallpaper.WallpaperBackground
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream

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
    var activeTab by remember { mutableIntStateOf(0) } // 0: Font và màu, 1: Bố cục & Vị trí

    // Local editor states (One UI 8-8.5 style matching IMG_7774)
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

    var isColorPickerOpen by remember { mutableStateOf(false) }

    // Screen and DPI information
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.toFloat()
    val screenHeightDp = configuration.screenHeightDp.toFloat()
    val minDimension = screenWidthDp.coerceAtMost(screenHeightDp)

    // Font picker for external fonts (.ttf/.otf)
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

    // Wallpaper picker launcher
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
                    alignment = localAlignment
                )
            }
        }
        isEditMode = false
    }

    LaunchedEffect(Unit) {
        offsetYAnimatable.snapTo(0f)
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
        clockAlignment = localAlignment
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
                            .pointerInput(screenHeightPx) {
                                detectTapGestures(
                                    onLongPress = {
                                        isEditMode = true
                                    }
                                )
                            }
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
            WallpaperBackground(
                wallpaperUri = settings.standbyWallpaperUri,
                wallpaperType = settings.standbyWallpaperType,
                dim = settings.wallpaperDim,
                blur = settings.wallpaperBlur
            )

            // Live Interactive Clock (Full screen freeform movement and resizing)
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
                        isEditMode = true
                    }
                }
            )

            // ONE UI 8-8.5 LIVE EDITOR OVERLAY (When isEditMode == true, matching IMG_7774)
            if (isEditMode) {
                // Top Action Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 20.dp, vertical = 14.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { wallpaperPickerLauncher.launch("image/*") },
                        modifier = Modifier
                            .size(42.dp)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                    ) {
                        Icon(Icons.Default.Wallpaper, contentDescription = "Đổi hình nền", tint = Color.White)
                    }

                    Text(
                        text = "Màn hình chờ (One UI Editor)",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )

                    Button(
                        onClick = { saveChanges() },
                        shape = RoundedCornerShape(20.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFA5))
                    ) {
                        Text("Hoàn tất", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                    }
                }

                // One UI 8 Bottom Floating Customizer Sheet
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)),
                    color = Color(0xF21C1C1E),
                    tonalElevation = 16.dp
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Tab Selector (Font và màu | Bố cục & Vị trí)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center
                        ) {
                            TextButton(
                                onClick = { activeTab = 0 },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = if (activeTab == 0) Color.White else Color.Gray
                                )
                            ) {
                                Text(
                                    "Font và màu",
                                    fontSize = 15.sp,
                                    fontWeight = if (activeTab == 0) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                            Spacer(modifier = Modifier.width(24.dp))
                            TextButton(
                                onClick = { activeTab = 1 },
                                colors = ButtonDefaults.textButtonColors(
                                    contentColor = if (activeTab == 1) Color.White else Color.Gray
                                )
                            ) {
                                Text(
                                    "Bố cục & Vị trí",
                                    fontSize = 15.sp,
                                    fontWeight = if (activeTab == 1) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }

                        if (activeTab == 0) {
                            // Font Selection Row (One UI circular font badges)
                            val fontOptions = listOf(
                                "Default" to "12",
                                "Serif" to "12",
                                "SansSerif" to "12",
                                "Monospace" to "12",
                                "Cursive" to "12",
                                "Digital" to "12",
                                "Bold" to "12"
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
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(if (isSelected) Color(0xFF00BFA5) else Color(0xFF2C2C2E))
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
                                            color = Color.White,
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

                                // Custom External Font Button (+ Font ngoài .ttf/.otf)
                                item {
                                    val isCustomSelected = localFont == "Custom" || localFontPath.isNotBlank()
                                    Box(
                                        modifier = Modifier
                                            .size(46.dp)
                                            .clip(CircleShape)
                                            .background(if (isCustomSelected) Color(0xFF00BFA5) else Color(0xFF2C2C2E))
                                            .border(
                                                width = if (isCustomSelected) 2.dp else 1.dp,
                                                color = if (isCustomSelected) Color.White else Color.Gray,
                                                shape = CircleShape
                                            )
                                            .clickable {
                                                fontPickerLauncher.launch("*/*")
                                            },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Add, contentDescription = "Thêm font ngoài", tint = Color.White)
                                    }
                                }
                            }

                            // Weight Slider & Size Scaling
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Cỡ chữ (${String.format("%.1f", localScale)}x)", color = Color.LightGray, fontSize = 12.sp)
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
                                }
                            }

                            Slider(
                                value = localScale,
                                onValueChange = { localScale = it },
                                valueRange = 0.4f..3.0f,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color.White,
                                    activeTrackColor = Color(0xFF00BFA5),
                                    inactiveTrackColor = Color(0xFF3A3A3C)
                                )
                            )

                            // Weight Slider (Mỏng <----> In đậm)
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text("Độ đậm phông chữ", color = Color.LightGray, fontSize = 12.sp)
                                    Text("Weight: $localWeight", color = Color(0xFF00BFA5), fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                }
                                Slider(
                                    value = localWeight.toFloat(),
                                    onValueChange = { localWeight = it.toInt() },
                                    valueRange = 100f..900f,
                                    steps = 7,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color.White,
                                        activeTrackColor = Color(0xFF00BFA5),
                                        inactiveTrackColor = Color(0xFF3A3A3C)
                                    )
                                )
                            }

                            // Color Palette Swatches Row
                            val presetColors = listOf(
                                0xFFFFFFFF.toInt(),
                                0xFF00E5FF.toInt(),
                                0xFF2196F3.toInt(),
                                0xFFFFD700.toInt(),
                                0xFFFF9800.toInt(),
                                0xFFFF5252.toInt(),
                                0xFFE040FB.toInt(),
                                0xFF69F0AE.toInt(),
                                0xFFB0BEC5.toInt(),
                                0xFF212121.toInt()
                            )

                            LazyRow(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Custom Color Picker Icon
                                item {
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Color(0xFF2C2C2E))
                                            .border(1.dp, Color.Gray, CircleShape)
                                            .clickable { isColorPickerOpen = true },
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(Icons.Default.Palette, contentDescription = "Tùy chỉnh màu", tint = Color.White, modifier = Modifier.size(16.dp))
                                    }
                                }

                                items(presetColors) { colorInt ->
                                    val isColorSelected = localColor == colorInt
                                    Box(
                                        modifier = Modifier
                                            .size(34.dp)
                                            .clip(CircleShape)
                                            .background(Color(colorInt))
                                            .border(
                                                width = if (isColorSelected) 3.dp else 1.dp,
                                                color = if (isColorSelected) Color(0xFF00E5FF) else Color.DarkGray,
                                                shape = CircleShape
                                            )
                                            .clickable { localColor = colorInt }
                                    )
                                }
                            }
                        } else {
                            // Tab 1: Bố cục & Vị trí (Điều chỉnh khắp màn hình + Lưới căn nhanh)
                            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text("Vị trí nhanh trên màn hình:", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)

                                // Quick 9-point grid alignment buttons
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    OutlinedButton(
                                        onClick = { localOffsetX = 0.18f; localOffsetY = 0.22f; localAlignment = "left" },
                                        modifier = Modifier.weight(1f).padding(end = 4.dp),
                                        contentPadding = PaddingValues(4.dp)
                                    ) {
                                        Text("Trên - Trái", fontSize = 11.sp, maxLines = 1)
                                    }
                                    OutlinedButton(
                                        onClick = { localOffsetX = 0.5f; localOffsetY = 0.22f; localAlignment = "center" },
                                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                                        contentPadding = PaddingValues(4.dp)
                                    ) {
                                        Text("Trên - Giữa", fontSize = 11.sp, maxLines = 1)
                                    }
                                    OutlinedButton(
                                        onClick = { localOffsetX = 0.82f; localOffsetY = 0.22f; localAlignment = "right" },
                                        modifier = Modifier.weight(1f).padding(start = 4.dp),
                                        contentPadding = PaddingValues(4.dp)
                                    ) {
                                        Text("Trên - Phải", fontSize = 11.sp, maxLines = 1)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    OutlinedButton(
                                        onClick = { localOffsetX = 0.18f; localOffsetY = 0.45f; localAlignment = "left" },
                                        modifier = Modifier.weight(1f).padding(end = 4.dp),
                                        contentPadding = PaddingValues(4.dp)
                                    ) {
                                        Text("Giữa - Trái", fontSize = 11.sp, maxLines = 1)
                                    }
                                    Button(
                                        onClick = { localOffsetX = 0.5f; localOffsetY = 0.45f; localAlignment = "center" },
                                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                                        contentPadding = PaddingValues(4.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFA5))
                                    ) {
                                        Text("Trung tâm", fontSize = 11.sp, maxLines = 1, fontWeight = FontWeight.Bold)
                                    }
                                    OutlinedButton(
                                        onClick = { localOffsetX = 0.82f; localOffsetY = 0.45f; localAlignment = "right" },
                                        modifier = Modifier.weight(1f).padding(start = 4.dp),
                                        contentPadding = PaddingValues(4.dp)
                                    ) {
                                        Text("Giữa - Phải", fontSize = 11.sp, maxLines = 1)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    OutlinedButton(
                                        onClick = { localOffsetX = 0.18f; localOffsetY = 0.70f; localAlignment = "left" },
                                        modifier = Modifier.weight(1f).padding(end = 4.dp),
                                        contentPadding = PaddingValues(4.dp)
                                    ) {
                                        Text("Dưới - Trái", fontSize = 11.sp, maxLines = 1)
                                    }
                                    OutlinedButton(
                                        onClick = { localOffsetX = 0.5f; localOffsetY = 0.70f; localAlignment = "center" },
                                        modifier = Modifier.weight(1f).padding(horizontal = 2.dp),
                                        contentPadding = PaddingValues(4.dp)
                                    ) {
                                        Text("Dưới - Giữa", fontSize = 11.sp, maxLines = 1)
                                    }
                                    OutlinedButton(
                                        onClick = { localOffsetX = 0.82f; localOffsetY = 0.70f; localAlignment = "right" },
                                        modifier = Modifier.weight(1f).padding(start = 4.dp),
                                        contentPadding = PaddingValues(4.dp)
                                    ) {
                                        Text("Dưới - Phải", fontSize = 11.sp, maxLines = 1)
                                    }
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Căn lề chữ", color = Color.White, fontSize = 13.sp)
                                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Định dạng 24 giờ", color = Color.White, fontSize = 13.sp)
                                    Switch(checked = local24H, onCheckedChange = { local24H = it })
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Hiển thị ngày tháng", color = Color.White, fontSize = 13.sp)
                                    Switch(checked = localShowDate, onCheckedChange = { localShowDate = it })
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Dòng chữ 'Vuốt lên để mở'", color = Color.White, fontSize = 13.sp)
                                    Switch(checked = localShowSwipe, onCheckedChange = { localShowSwipe = it })
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Hiệu ứng bóng đổ chữ (Shadow)", color = Color.White, fontSize = 13.sp)
                                    Switch(checked = localShadow, onCheckedChange = { localShadow = it })
                                }
                            }
                        }
                    }
                }

                // Color Picker Dialog
                if (isColorPickerOpen) {
                    AlertDialog(
                        onDismissRequest = { isColorPickerOpen = false },
                        title = { Text("Chọn màu sắc đồng hồ", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        text = {
                            ComposeColorPicker(
                                selectedColor = localColor,
                                onColorChanged = { localColor = it }
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
