package com.example.dexlauncher.ui.settings

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dexlauncher.data.model.DeXWallpaperPreset
import com.example.dexlauncher.data.repository.LauncherSettings
import com.example.dexlauncher.data.repository.SettingsRepository
import com.example.dexlauncher.ui.standby.StandbyClockView
import com.example.dexlauncher.ui.wallpaper.WallpaperBackground
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.io.FileOutputStream
import kotlin.math.abs

sealed class SettingsPage {
    object Main : SettingsPage()
    object Standby : SettingsPage()
    object Desktop : SettingsPage()
    object Wallpaper : SettingsPage()
    object Taskbar : SettingsPage()
    object Icons : SettingsPage()
    object Typography : SettingsPage()
    object Animations : SettingsPage()
    object HiddenApps : SettingsPage()
    object ShizukuBeta : SettingsPage()
    object Permissions : SettingsPage()
    object About : SettingsPage()
}

@Composable
fun SettingsScreen(
    settingsRepository: SettingsRepository,
    settings: LauncherSettings,
    initialPage: SettingsPage = SettingsPage.Main,
    onDismiss: () -> Unit
) {
    var currentPage by remember { mutableStateOf(initialPage) }
    val scope = rememberCoroutineScope()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.72f))
            .pointerInput(Unit) {
                detectTapGestures { onDismiss() }
            },
        contentAlignment = Alignment.Center
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .fillMaxHeight(0.92f)
                .widthIn(max = 750.dp)
                .clip(RoundedCornerShape(24.dp))
                .pointerInput(Unit) {
                    detectTapGestures { /* Chặn chạm vào trong modal */ }
                },
            color = Color(0xFF181818),
            tonalElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp)
            ) {
                // Top App Bar
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f, fill = false)
                    ) {
                        if (currentPage !is SettingsPage.Main) {
                            IconButton(onClick = { currentPage = SettingsPage.Main }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "Quay lại",
                                    tint = Color.White
                                )
                            }
                            Spacer(modifier = Modifier.width(6.dp))
                        }
                        Text(
                            text = when (currentPage) {
                                is SettingsPage.Main -> "Cài đặt DeX"
                                is SettingsPage.Standby -> "Màn hình chờ (Standby)"
                                is SettingsPage.Desktop -> "Màn hình chính"
                                is SettingsPage.Wallpaper -> "Hình nền"
                                is SettingsPage.Taskbar -> "Thanh tác vụ (Taskbar)"
                                is SettingsPage.Icons -> "Biểu tượng ứng dụng"
                                is SettingsPage.Typography -> "Phông chữ & Đồng hồ"
                                is SettingsPage.Animations -> "Hiệu ứng chuyển động"
                                is SettingsPage.HiddenApps -> "Ứng dụng đã ẩn"
                                is SettingsPage.ShizukuBeta -> "Shizuku (BETA)"
                                is SettingsPage.Permissions -> "Quyền & Tương thích"
                                is SettingsPage.About -> "Giới thiệu"
                            },
                            color = Color.White,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (currentPage is SettingsPage.Main) {
                            OutlinedButton(
                                onClick = {
                                    scope.launch { settingsRepository.resetToDefaults() }
                                },
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = Color(0xFFFF5252))
                            ) {
                                Text("Khôi phục mặc định", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                        Button(
                            onClick = onDismiss,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B))
                        ) {
                            Text("Xong", color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Content Pages
                Box(modifier = Modifier.weight(1f)) {
                    when (currentPage) {
                        is SettingsPage.Main -> MainSettingsList(
                            settings = settings,
                            onNavigate = { currentPage = it }
                        )
                        is SettingsPage.Standby -> StandbySettingsPage(settings = settings, settingsRepository = settingsRepository)
                        is SettingsPage.Desktop -> DesktopSettingsPage(settings = settings, settingsRepository = settingsRepository)
                        is SettingsPage.Wallpaper -> WallpaperSettingsPage(settings = settings, settingsRepository = settingsRepository)
                        is SettingsPage.Taskbar -> TaskbarSettingsPage(settings = settings, settingsRepository = settingsRepository)
                        is SettingsPage.Icons -> IconsSettingsPage(settings = settings, settingsRepository = settingsRepository)
                        is SettingsPage.Typography -> TypographySettingsPage(settings = settings, settingsRepository = settingsRepository)
                        is SettingsPage.Animations -> AnimationsSettingsPage(settings = settings, settingsRepository = settingsRepository)
                        is SettingsPage.HiddenApps -> HiddenAppsSettingsPage(settings = settings, settingsRepository = settingsRepository)
                        is SettingsPage.ShizukuBeta -> ShizukuSettingsPage()
                        is SettingsPage.Permissions -> PermissionsSettingsPage()
                        is SettingsPage.About -> AboutSettingsPage()
                    }
                }
            }
        }
    }
}

@Composable
fun MainSettingsList(
    settings: LauncherSettings,
    onNavigate: (SettingsPage) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            SettingsCard(title = "Hiển thị & Giao diện") {
                SettingsItem(icon = Icons.Default.Lock, iconBg = Color(0xFFE91E63), title = "Màn hình chờ (Standby)", desc = "Đồng hồ, vị trí kéo thả, màu sắc, hiệu ứng") { onNavigate(SettingsPage.Standby) }
                SettingsItem(icon = Icons.Default.Home, iconBg = Color(0xFF4CAF50), title = "Màn hình chính", desc = "Bố cục lưới, kích thước icon, lề desktop") { onNavigate(SettingsPage.Desktop) }
                SettingsItem(icon = Icons.Default.Wallpaper, iconBg = Color(0xFF2196F3), title = "Hình nền", desc = "Ảnh/Video màn hình chính và standby, độ mờ") { onNavigate(SettingsPage.Wallpaper) }
                SettingsItem(icon = Icons.Default.Star, iconBg = Color(0xFFFF9800), title = "Biểu tượng", desc = "Độ bo góc, nhãn icon, kích thước") { onNavigate(SettingsPage.Icons) }
            }
        }

        item {
            SettingsCard(title = "Hệ thống & Tác vụ") {
                SettingsItem(icon = Icons.Default.Menu, iconBg = Color(0xFF9C27B0), title = "Thanh tác vụ (Taskbar)", desc = "Tự động ẩn, ghim ứng dụng") { onNavigate(SettingsPage.Taskbar) }
                SettingsItem(icon = Icons.Default.Edit, iconBg = Color(0xFF00BCD4), title = "Phông chữ & Đồng hồ", desc = "Tùy chỉnh font .ttf/.otf, kiểu dáng đồng hồ") { onNavigate(SettingsPage.Typography) }
                SettingsItem(icon = Icons.Default.PlayArrow, iconBg = Color(0xFF795548), title = "Hiệu ứng chuyển động", desc = "Tốc độ animation, chế độ toàn màn hình ngang") { onNavigate(SettingsPage.Animations) }
                SettingsItem(icon = Icons.Default.Delete, iconBg = Color(0xFF607D8B), title = "Ứng dụng đã ẩn", desc = "Quản lý các app ẩn trong app drawer") { onNavigate(SettingsPage.HiddenApps) }
            }
        }

        item {
            SettingsCard(title = "Nâng cao & Tương thích") {
                SettingsItem(icon = Icons.Default.Build, iconBg = Color(0xFFE65100), title = "Shizuku (BETA)", desc = "Kết nối Shizuku để chạy freeform nâng cao") { onNavigate(SettingsPage.ShizukuBeta) }
                SettingsItem(icon = Icons.Default.Settings, iconBg = Color(0xFF3F51B5), title = "Quyền & Tương thích", desc = "Usage stats, overlay, lệnh ADB") { onNavigate(SettingsPage.Permissions) }
                SettingsItem(icon = Icons.Default.Info, iconBg = Color(0xFF009688), title = "Giới thiệu", desc = "DeX Launcher v2.0 tối ưu Android 10+") { onNavigate(SettingsPage.About) }
            }
        }
    }
}

@Composable
fun SettingsCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color(0xFF242424),
        tonalElevation = 4.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                color = Color(0xFF80CBC4),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            content()
        }
    }
}

@Composable
fun SettingsItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconBg: Color,
    title: String,
    desc: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(iconBg, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(14.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = desc,
                color = Color.Gray,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Icon(Icons.Default.KeyboardArrowRight, contentDescription = null, tint = Color.Gray)
    }
}

@Composable
fun StandbySettingsPage(
    settings: LauncherSettings,
    settingsRepository: SettingsRepository
) {
    val scope = rememberCoroutineScope()
    var clockScale by remember(settings.clockScale) { mutableFloatStateOf(settings.clockScale) }
    var clockOffsetX by remember(settings.clockOffsetX) { mutableFloatStateOf(settings.clockOffsetX) }
    var clockOffsetY by remember(settings.clockOffsetY) { mutableFloatStateOf(settings.clockOffsetY) }
    var clockColor by remember(settings.clockColor) { mutableIntStateOf(settings.clockColor) }
    var clockOpacity by remember(settings.clockOpacity) { mutableFloatStateOf(settings.clockOpacity) }
    var clockBlur by remember(settings.clockBlur) { mutableFloatStateOf(settings.clockBlur) }
    var clockWeight by remember(settings.clockWeight) { mutableIntStateOf(settings.clockWeight) }
    var clockLetterSpacing by remember(settings.clockLetterSpacing) { mutableFloatStateOf(settings.clockLetterSpacing) }
    var clockShadow by remember(settings.clockShadow) { mutableStateOf(settings.clockShadow) }
    var is24H by remember(settings.clockIs24H) { mutableStateOf(settings.clockIs24H) }
    var showDate by remember(settings.clockShowDate) { mutableStateOf(settings.clockShowDate) }
    var showSwipeUp by remember(settings.clockShowSwipeUp) { mutableStateOf(settings.clockShowSwipeUp) }
    var clockAlignment by remember(settings.clockAlignment) { mutableStateOf(settings.clockAlignment) }

    var saveTrigger by remember { mutableIntStateOf(0) }

    fun scheduleSave() {
        saveTrigger++
    }

    fun saveImmediately() {
        scope.launch {
            settingsRepository.updateClockCustomization(
                settings.clockFont,
                settings.clockFontPath,
                clockColor,
                clockScale,
                clockOffsetX,
                clockOffsetY,
                clockOpacity,
                clockBlur,
                clockWeight,
                clockLetterSpacing,
                clockShadow,
                is24H,
                showDate,
                showSwipeUp,
                clockAlignment
            )
        }
    }

    // Debounce DataStore save by 300ms for continuous gestures
    LaunchedEffect(saveTrigger) {
        if (saveTrigger > 0) {
            delay(300L)
            settingsRepository.updateClockCustomization(
                settings.clockFont,
                settings.clockFontPath,
                clockColor,
                clockScale,
                clockOffsetX,
                clockOffsetY,
                clockOpacity,
                clockBlur,
                clockWeight,
                clockLetterSpacing,
                clockShadow,
                is24H,
                showDate,
                showSwipeUp,
                clockAlignment
            )
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val maxPreviewHeight = maxHeight * 0.55f

        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Preview Box 16:9 with Drag and Pinch-to-Zoom, max 55% height
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Xem trước màn hình chờ (Kéo 1 ngón di chuyển • Chụm 2 ngón đổi kích cỡ)",
                        color = Color.LightGray,
                        fontSize = 13.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = maxPreviewHeight)
                            .aspectRatio(16f / 9f, matchHeightConstraintsFirst = false)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Black)
                            .border(1.dp, Color(0xFF333333), RoundedCornerShape(16.dp))
                            .pointerInput(Unit) {
                                detectTransformGestures(panZoomLock = false) { _, pan, zoom, _ ->
                                    if (zoom != 1f) {
                                        clockScale = (clockScale * zoom).coerceIn(0.4f, 3.0f)
                                    }
                                    if (pan != Offset.Zero) {
                                        val previewW = size.width.toFloat()
                                        val previewH = size.height.toFloat()
                                        if (previewW > 0 && previewH > 0) {
                                            val rawX = (clockOffsetX + (pan.x / previewW)).coerceIn(0.05f, 0.95f)
                                            val rawY = (clockOffsetY + (pan.y / previewH)).coerceIn(0.05f, 0.95f)
                                            clockOffsetX = if (abs(rawX - 0.5f) < 0.035f) 0.5f else rawX
                                            clockOffsetY = if (abs(rawY - 0.5f) < 0.035f) 0.5f else rawY
                                        }
                                    }
                                    scheduleSave()
                                }
                            }
                    ) {
                        val previewSettings = settings.copy(
                            clockScale = clockScale,
                            clockOffsetX = clockOffsetX,
                            clockOffsetY = clockOffsetY,
                            clockColor = clockColor,
                            clockOpacity = clockOpacity,
                            clockBlur = clockBlur,
                            clockWeight = clockWeight,
                            clockLetterSpacing = clockLetterSpacing,
                            clockShadow = clockShadow,
                            clockIs24H = is24H,
                            clockShowDate = showDate,
                            clockShowSwipeUp = showSwipeUp,
                            clockAlignment = clockAlignment
                        )

                        WallpaperBackground(
                            wallpaperUri = settings.standbyWallpaperUri,
                            wallpaperType = settings.standbyWallpaperType,
                            dim = settings.wallpaperDim,
                            blur = settings.wallpaperBlur
                        )

                        // Alignment guidelines
                        val isCenterHoriz = abs(clockOffsetX - 0.5f) < 0.01f
                        val isCenterVert = abs(clockOffsetY - 0.5f) < 0.01f

                        Canvas(modifier = Modifier.fillMaxSize()) {
                            if (isCenterHoriz) {
                                drawLine(
                                    color = Color(0x9900E5FF),
                                    start = Offset(size.width * 0.5f, 0f),
                                    end = Offset(size.width * 0.5f, size.height),
                                    strokeWidth = 2.dp.toPx()
                                )
                            }
                            if (isCenterVert) {
                                drawLine(
                                    color = Color(0x9900E5FF),
                                    start = Offset(0f, size.height * 0.5f),
                                    end = Offset(size.width, size.height * 0.5f),
                                    strokeWidth = 2.dp.toPx()
                                )
                            }
                        }

                        StandbyClockView(
                            settings = previewSettings,
                            onClick = {}
                        )
                    }

                    // Quick Reset & Alignment Actions
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedButton(
                            onClick = {
                                clockOffsetX = 0.5f
                                clockOffsetY = 0.4f
                                clockScale = 1.0f
                                saveImmediately()
                            }
                        ) {
                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Đặt lại vị trí và kích cỡ", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            FilterChip(
                                selected = clockAlignment == "left",
                                onClick = {
                                    clockAlignment = "left"
                                    saveImmediately()
                                },
                                label = { Text("Trái", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                            FilterChip(
                                selected = clockAlignment == "center",
                                onClick = {
                                    clockAlignment = "center"
                                    saveImmediately()
                                },
                                label = { Text("Giữa", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                            FilterChip(
                                selected = clockAlignment == "right",
                                onClick = {
                                    clockAlignment = "right"
                                    saveImmediately()
                                },
                                label = { Text("Phải", maxLines = 1, overflow = TextOverflow.Ellipsis) }
                            )
                        }
                    }
                }
            }

            // Color Picker Card
            item {
                SettingsCard(title = "Màu sắc đồng hồ") {
                    ComposeColorPicker(
                        selectedColor = clockColor,
                        onColorChanged = { newColor ->
                            clockColor = newColor
                            scheduleSave()
                        }
                    )
                }
            }

            // Clock Display & Layout Card
            item {
                SettingsCard(title = "Tùy chỉnh hiển thị") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Định dạng 24 giờ", color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Switch(
                                checked = is24H,
                                onCheckedChange = {
                                    is24H = it
                                    saveImmediately()
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Hiển thị ngày tháng", color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Switch(
                                checked = showDate,
                                onCheckedChange = {
                                    showDate = it
                                    saveImmediately()
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Hiển thị dòng chữ 'Vuốt lên để mở'", color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Switch(
                                checked = showSwipeUp,
                                onCheckedChange = {
                                    showSwipeUp = it
                                    saveImmediately()
                                }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Hiệu ứng bóng đổ chữ (Shadow)", color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            Switch(
                                checked = clockShadow,
                                onCheckedChange = {
                                    clockShadow = it
                                    saveImmediately()
                                }
                            )
                        }

                        Text("Kích thước đồng hồ (${String.format("%.2f", clockScale)}x)", color = Color.LightGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Slider(
                            value = clockScale,
                            onValueChange = { clockScale = it },
                            onValueChangeFinished = { saveImmediately() },
                            valueRange = 0.4f..3.0f
                        )

                        Text("Độ đậm phông chữ (Weight: $clockWeight)", color = Color.LightGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Slider(
                            value = clockWeight.toFloat(),
                            onValueChange = { clockWeight = it.toInt() },
                            onValueChangeFinished = { saveImmediately() },
                            valueRange = 100f..900f,
                            steps = 7
                        )

                        Text("Khoảng cách chữ (${String.format("%.1f", clockLetterSpacing)}sp)", color = Color.LightGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Slider(
                            value = clockLetterSpacing,
                            onValueChange = { clockLetterSpacing = it },
                            onValueChangeFinished = { saveImmediately() },
                            valueRange = -2f..10f
                        )

                        Text("Độ trong suốt (${(clockOpacity * 100).toInt()}%)", color = Color.LightGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Slider(
                            value = clockOpacity,
                            onValueChange = { clockOpacity = it },
                            onValueChangeFinished = { saveImmediately() },
                            valueRange = 0.1f..1.0f
                        )

                        Text("Độ mờ chữ Blur (${String.format("%.1f", clockBlur)}dp)", color = Color.LightGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Slider(
                            value = clockBlur,
                            onValueChange = { clockBlur = it },
                            onValueChangeFinished = { saveImmediately() },
                            valueRange = 0f..15f
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ComposeColorPicker(
    selectedColor: Int,
    onColorChanged: (Int) -> Unit
) {
    val initialHsv = remember {
        val array = FloatArray(3)
        android.graphics.Color.colorToHSV(selectedColor, array)
        array
    }

    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var sat by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }
    var alpha by remember {
        mutableFloatStateOf(((selectedColor ushr 24) and 0xFF) / 255f)
    }

    var hexText by remember {
        mutableStateOf(String.format("#%08X", selectedColor))
    }

    var lastEmittedColor by remember { mutableIntStateOf(selectedColor) }
    val currentOnColorChanged by rememberUpdatedState(onColorChanged)

    LaunchedEffect(selectedColor) {
        if (selectedColor != lastEmittedColor) {
            val hsv = FloatArray(3)
            android.graphics.Color.colorToHSV(selectedColor, hsv)
            // Giữ hue khi sat hoặc value bằng 0
            if (hsv[1] > 0.01f && hsv[2] > 0.01f) {
                hue = hsv[0]
            }
            sat = hsv[1]
            value = hsv[2]
            alpha = ((selectedColor ushr 24) and 0xFF) / 255f
            hexText = String.format("#%08X", selectedColor)
            lastEmittedColor = selectedColor
        }
    }

    fun applyColor(newHue: Float, newSat: Float, newVal: Float, newAlpha: Float) {
        hue = newHue
        sat = newSat
        value = newVal
        alpha = newAlpha
        val rgb = android.graphics.Color.HSVToColor((newAlpha * 255).toInt(), floatArrayOf(newHue, newSat, newVal))
        hexText = String.format("#%08X", rgb)
        lastEmittedColor = rgb
        currentOnColorChanged(rgb)
    }

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        // Preset Palette Row
        val presetColors = remember {
            listOf(
                0xFFFFFFFF.toInt(),
                0xFF00E5FF.toInt(),
                0xFF00B0FF.toInt(),
                0xFF00897B.toInt(),
                0xFF69F0AE.toInt(),
                0xFFFFD700.toInt(),
                0xFFFF9800.toInt(),
                0xFFFF5252.toInt(),
                0xFFE040FB.toInt(),
                0xFFB0BEC5.toInt(),
                0xFF212121.toInt()
            )
        }

        Text("Bảng màu gợi ý", color = Color.LightGray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        LazyRow(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(presetColors) { colorInt ->
                val isSelected = selectedColor == colorInt
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(Color(colorInt))
                        .border(
                            width = if (isSelected) 3.dp else 1.dp,
                            color = if (isSelected) Color(0xFF00E5FF) else Color.DarkGray,
                            shape = CircleShape
                        )
                        .clickable {
                            val newHsv = FloatArray(3)
                            android.graphics.Color.colorToHSV(colorInt, newHsv)
                            val a = ((colorInt ushr 24) and 0xFF) / 255f
                            val chosenHue = if (newHsv[1] > 0.01f && newHsv[2] > 0.01f) newHsv[0] else hue
                            applyColor(chosenHue, newHsv[1], newHsv[2], a)
                        }
                )
            }
        }

        // Saturation & Value 2D Touch Area
        Text("Độ bão hòa & Độ sáng (Saturation / Value)", color = Color.LightGray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        val pureHueColor = remember(hue) {
            Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, 1f, 1f)))
        }

        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .height(130.dp)
                .clip(RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val s = (offset.x / size.width).coerceIn(0f, 1f)
                            val v = (1f - (offset.y / size.height)).coerceIn(0f, 1f)
                            applyColor(hue, s, v, alpha)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val s = (change.position.x / size.width).coerceIn(0f, 1f)
                            val v = (1f - (change.position.y / size.height)).coerceIn(0f, 1f)
                            applyColor(hue, s, v, alpha)
                        }
                    )
                }
        ) {
            val widthPx = constraints.maxWidth.toFloat()
            val heightPx = constraints.maxHeight.toFloat()

            Canvas(modifier = Modifier.fillMaxSize()) {
                // Hue base background
                drawRect(color = pureHueColor)
                // White to transparent horizontal gradient for Saturation
                drawRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(Color.White, Color.Transparent)
                    )
                )
                // Transparent to black vertical gradient for Value
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(Color.Transparent, Color.Black)
                    )
                )
                // Selector circle
                val thumbX = sat * widthPx
                val thumbY = (1f - value) * heightPx
                drawCircle(
                    color = Color.White,
                    radius = 8.dp.toPx(),
                    center = Offset(thumbX, thumbY)
                )
                drawCircle(
                    color = Color.Black,
                    radius = 6.dp.toPx(),
                    center = Offset(thumbX, thumbY)
                )
            }
        }

        // Hue Rainbow Slider
        Text("Tông màu (Hue: ${hue.toInt()}°)", color = Color.LightGray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            Color.Red, Color.Yellow, Color.Green,
                            Color.Cyan, Color.Blue, Color.Magenta, Color.Red
                        )
                    )
                )
                .border(1.dp, Color(0xFF444444), RoundedCornerShape(14.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val h = ((offset.x / size.width).coerceIn(0f, 1f) * 360f)
                            applyColor(h, sat, value, alpha)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val h = ((change.position.x / size.width).coerceIn(0f, 1f) * 360f)
                            applyColor(h, sat, value, alpha)
                        }
                    )
                }
        )

        // Alpha Slider
        Text("Độ đậm trong suốt (Alpha: ${(alpha * 100).toInt()}%)", color = Color.LightGray, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
        val opaqueColor = remember(hue, sat, value) {
            Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, sat, value)))
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(24.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(
                    Brush.horizontalGradient(
                        colors = listOf(
                            opaqueColor.copy(alpha = 0f),
                            opaqueColor.copy(alpha = 1f)
                        )
                    )
                )
                .border(1.dp, Color(0xFF444444), RoundedCornerShape(12.dp))
                .pointerInput(Unit) {
                    detectDragGestures(
                        onDragStart = { offset ->
                            val a = (offset.x / size.width).coerceIn(0.05f, 1f)
                            applyColor(hue, sat, value, a)
                        },
                        onDrag = { change, _ ->
                            change.consume()
                            val a = (change.position.x / size.width).coerceIn(0.05f, 1f)
                            applyColor(hue, sat, value, a)
                        }
                    )
                }
        )

        // Hex Code Input & Preview Swatch
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(selectedColor))
                    .border(2.dp, Color.White, RoundedCornerShape(8.dp))
            )

            OutlinedTextField(
                value = hexText,
                onValueChange = { hexText = it },
                label = { Text("Mã HEX (#AARRGGBB)", maxLines = 1, overflow = TextOverflow.Ellipsis) },
                singleLine = true,
                modifier = Modifier.weight(1f),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF00E5FF),
                    unfocusedBorderColor = Color.DarkGray
                )
            )

            Button(
                onClick = {
                    try {
                        val parsed = android.graphics.Color.parseColor(hexText.trim())
                        val newHsv = FloatArray(3)
                        android.graphics.Color.colorToHSV(parsed, newHsv)
                        val a = ((parsed ushr 24) and 0xFF) / 255f
                        val chosenHue = if (newHsv[1] > 0.01f && newHsv[2] > 0.01f) newHsv[0] else hue
                        applyColor(chosenHue, newHsv[1], newHsv[2], a)
                    } catch (_: Exception) {}
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00897B))
            ) {
                Text("Áp dụng", maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}

@Composable
fun DesktopSettingsPage(
    settings: LauncherSettings,
    settingsRepository: SettingsRepository
) {
    val scope = rememberCoroutineScope()
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            SettingsCard(title = "Bố cục Màn hình chính") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { scope.launch { settingsRepository.updateDesktopLayoutMode("grid") } },
                        colors = ButtonDefaults.buttonColors(containerColor = if (settings.desktopLayoutMode == "grid") Color(0xFF00897B) else Color.DarkGray)
                    ) {
                        Text("Lưới (Grid)", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    Button(
                        onClick = { scope.launch { settingsRepository.updateDesktopLayoutMode("freeform") } },
                        colors = ButtonDefaults.buttonColors(containerColor = if (settings.desktopLayoutMode == "freeform") Color(0xFF00897B) else Color.DarkGray)
                    ) {
                        Text("Tự do (Freeform)", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun WallpaperSettingsPage(
    settings: LauncherSettings,
    settingsRepository: SettingsRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val desktopWallpaperLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val type = if (context.contentResolver.getType(it)?.startsWith("video") == true) "video" else "image"
                scope.launch { settingsRepository.updateDesktopWallpaper(it.toString(), type) }
            } catch (_: Exception) {}
        }
    }

    val standbyWallpaperLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val type = if (context.contentResolver.getType(it)?.startsWith("video") == true) "video" else "image"
                scope.launch { settingsRepository.updateStandbyWallpaper(it.toString(), type) }
            } catch (_: Exception) {}
        }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Chế độ Đổi hình nền theo thời gian trong ngày (Tính năng số 7)
        item {
            SettingsCard(title = "Chế độ Hình nền Động thông minh") {
                val isDynamic = settings.desktopWallpaperType == "dynamic_time"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Tự động đổi theo giờ trong ngày",
                            color = Color.White,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "Bình minh (Sáng) • Biển ngọc (Trưa) • Hoàng hôn (Chiều) • Cyber (Đêm)",
                            color = Color.LightGray,
                            fontSize = 11.sp
                        )
                    }
                    Switch(
                        checked = isDynamic,
                        onCheckedChange = { checked ->
                            scope.launch {
                                if (checked) {
                                    settingsRepository.updateDesktopWallpaper("", "dynamic_time")
                                } else {
                                    settingsRepository.updateDesktopWallpaper(DeXWallpaperPreset.NEBULA_PURPLE.id, "preset")
                                }
                            }
                        }
                    )
                }
            }
        }

        // Bộ sưu tập Hình nền Preset DeX Galaxy
        item {
            SettingsCard(title = "Bộ sưu tập Hình nền DeX Preset") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    items(DeXWallpaperPreset.entries) { preset ->
                        val isSelected = settings.desktopWallpaperType == "preset" && settings.desktopWallpaperUri == preset.id
                        Card(
                            modifier = Modifier
                                .width(130.dp)
                                .height(90.dp)
                                .border(
                                    if (isSelected) 2.dp else 1.dp,
                                    if (isSelected) Color(0xFF00E5FF) else Color(0x33FFFFFF),
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    scope.launch {
                                        settingsRepository.updateDesktopWallpaper(preset.id, "preset")
                                    }
                                },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Box(modifier = Modifier.fillMaxSize()) {
                                Canvas(modifier = Modifier.fillMaxSize()) {
                                    val brush = Brush.linearGradient(
                                        listOf(preset.topColor, preset.midColor, preset.bottomColor)
                                    )
                                    drawRect(brush = brush)
                                }
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .align(Alignment.BottomCenter)
                                        .background(Color(0xAA000000))
                                        .padding(4.dp)
                                ) {
                                    Text(
                                        text = preset.title,
                                        color = Color.White,
                                        fontSize = 10.sp,
                                        maxLines = 1,
                                        textAlign = TextAlign.Center,
                                        modifier = Modifier.fillMaxWidth(),
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Tùy chọn Hình nền riêng cho Màn hình chính
        item {
            SettingsCard(title = "Hình nền Tùy chỉnh Desktop (Ảnh / Video lặp lại)") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { desktopWallpaperLauncher.launch(arrayOf("image/*", "video/*")) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFA5))
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Chọn Ảnh / Video Desktop", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(onClick = { scope.launch { settingsRepository.updateDesktopWallpaper(DeXWallpaperPreset.NEBULA_PURPLE.id, "preset") } }) {
                        Text("Khôi phục mặc định", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
                if (settings.desktopWallpaperType == "video") {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Đang dùng hình nền video lặp lại liên tục (ExoPlayer Loop)", color = Color(0xFF00FF88), fontSize = 11.sp)
                }
            }
        }

        // Tùy chọn Hình nền riêng cho Màn hình chờ
        item {
            SettingsCard(title = "Hình nền Màn hình chờ (Standby)") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { standbyWallpaperLauncher.launch(arrayOf("image/*", "video/*")) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF7C4DFF))
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Chọn Ảnh / Video Standby", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    OutlinedButton(onClick = { scope.launch { settingsRepository.updateStandbyWallpaper("", "preset") } }) {
                        Text("Mặc định", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }

        // Hiệu ứng nền
        item {
            SettingsCard(title = "Hiệu ứng nền (Làm tối & Làm mờ Blur)") {
                Text("Độ tối nền (${(settings.wallpaperDim * 100).toInt()}%)", color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Slider(value = settings.wallpaperDim, onValueChange = { scope.launch { settingsRepository.updateWallpaperDim(it) } }, valueRange = 0f..0.6f)

                Text("Độ mờ Blur nền (${settings.wallpaperBlur.toInt()}dp)", color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Slider(value = settings.wallpaperBlur, onValueChange = { scope.launch { settingsRepository.updateWallpaperBlur(it) } }, valueRange = 0f..25f)
            }
        }
    }
}

@Composable
fun TaskbarSettingsPage(
    settings: LauncherSettings,
    settingsRepository: SettingsRepository
) {
    val scope = rememberCoroutineScope()
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            SettingsCard(title = "Tác vụ & Taskbar") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Tự động ẩn Thanh tác vụ", color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Switch(checked = settings.taskbarAutoHide, onCheckedChange = {
                        scope.launch { settingsRepository.updateTaskbarAutoHide(it) }
                    })
                }
            }
        }
    }
}

@Composable
fun IconsSettingsPage(
    settings: LauncherSettings,
    settingsRepository: SettingsRepository
) {
    val scope = rememberCoroutineScope()
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            SettingsCard(title = "Bố cục Màn hình chính (Desktop)") {
                Text("Chế độ bố cục:", color = Color.LightGray, fontSize = 13.sp)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    FilterChip(
                        selected = settings.desktopLayoutMode != "freeform",
                        onClick = {
                            scope.launch { settingsRepository.updateDesktopLayoutMode("grid") }
                        },
                        label = { Text("Lưới chia ô (Grid)", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                    FilterChip(
                        selected = settings.desktopLayoutMode == "freeform",
                        onClick = {
                            scope.launch { settingsRepository.updateDesktopLayoutMode("freeform") }
                        },
                        label = { Text("Tự do (Freeform)", fontSize = 12.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (settings.desktopLayoutMode == "freeform") 
                        "Chế độ tự do cho phép kéo thả biểu tượng đặt ở bất kỳ tọa độ nào trên toàn màn hình."
                        else "Chế độ lưới chia đều các ô trên dưới trái phải, tự động gióng hàng ngay ngắn.",
                    color = Color.Gray,
                    fontSize = 12.sp
                )
            }
        }

        item {
            SettingsCard(title = "Kích thước & Kiểu dáng Icon") {
                Text("Kích thước Icon (${settings.iconSize}dp)", color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Slider(
                    value = settings.iconSize.toFloat(),
                    onValueChange = {
                        scope.launch { settingsRepository.updateIconSettings(it.toInt(), settings.iconLabelSize, settings.iconCornerRadius, settings.showLabels, settings.showShadow) }
                    },
                    valueRange = 40f..112f
                )

                Text("Kích thước chữ nhãn (${settings.iconLabelSize}sp)", color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Slider(
                    value = settings.iconLabelSize.toFloat(),
                    onValueChange = {
                        scope.launch { settingsRepository.updateIconSettings(settings.iconSize, it.toInt(), settings.iconCornerRadius, settings.showLabels, settings.showShadow) }
                    },
                    valueRange = 10f..18f
                )

                Text("Độ bo góc Icon (${settings.iconCornerRadius}dp)", color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Slider(
                    value = settings.iconCornerRadius.toFloat(),
                    onValueChange = {
                        scope.launch { settingsRepository.updateIconSettings(settings.iconSize, settings.iconLabelSize, it.toInt(), settings.showLabels, settings.showShadow) }
                    },
                    valueRange = 0f..28f
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Hiển thị nhãn ứng dụng", color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Switch(checked = settings.showLabels, onCheckedChange = {
                        scope.launch { settingsRepository.updateIconSettings(settings.iconSize, settings.iconLabelSize, settings.iconCornerRadius, it, settings.showShadow) }
                    })
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Bóng đổ chữ nhãn (Shadow)", color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Switch(checked = settings.showShadow, onCheckedChange = {
                        scope.launch { settingsRepository.updateIconSettings(settings.iconSize, settings.iconLabelSize, settings.iconCornerRadius, settings.showLabels, it) }
                    })
                }
            }
        }
    }
}

@Composable
fun TypographySettingsPage(
    settings: LauncherSettings,
    settingsRepository: SettingsRepository
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var fontName by remember { mutableStateOf(settings.clockFont) }
    var fontPath by remember { mutableStateOf(settings.clockFontPath) }

    val fontPickerLauncher = rememberLauncherForActivityResult(contract = ActivityResultContracts.OpenDocument()) { uri ->
        uri?.let {
            try {
                context.contentResolver.takePersistableUriPermission(it, Intent.FLAG_GRANT_READ_URI_PERMISSION)
                val fontsDir = File(context.filesDir, "fonts").apply { mkdirs() }
                val destFile = File(fontsDir, "custom_font_${System.currentTimeMillis()}.ttf")
                context.contentResolver.openInputStream(it)?.use { input ->
                    FileOutputStream(destFile).use { output -> input.copyTo(output) }
                }
                fontPath = destFile.absolutePath
                fontName = destFile.name
                scope.launch {
                    settingsRepository.updateClockCustomization(fontName, fontPath, settings.clockColor, settings.clockScale, settings.clockOffsetX, settings.clockOffsetY, settings.clockOpacity, settings.clockBlur, settings.clockWeight, settings.clockLetterSpacing, settings.clockShadow, settings.clockIs24H, settings.clockShowDate, settings.clockShowSwipeUp, settings.clockAlignment)
                }
            } catch (_: Exception) {}
        }
    }

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            SettingsCard(title = "Phông chữ đồng hồ tùy chỉnh") {
                Text("Font hiện tại: $fontName", color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { fontPickerLauncher.launch(arrayOf("font/ttf", "font/otf", "*/*")) }) {
                        Text("Nhập font .ttf / .otf", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                    if (fontPath.isNotEmpty()) {
                        OutlinedButton(onClick = {
                            fontPath = ""
                            fontName = "Default"
                            scope.launch {
                                settingsRepository.updateClockCustomization(fontName, fontPath, settings.clockColor, settings.clockScale, settings.clockOffsetX, settings.clockOffsetY, settings.clockOpacity, settings.clockBlur, settings.clockWeight, settings.clockLetterSpacing, settings.clockShadow, settings.clockIs24H, settings.clockShowDate, settings.clockShowSwipeUp, settings.clockAlignment)
                            }
                        }) {
                            Text("Xóa font tùy chỉnh", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AnimationsSettingsPage(
    settings: LauncherSettings,
    settingsRepository: SettingsRepository
) {
    val scope = rememberCoroutineScope()
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            SettingsCard(title = "Hiệu ứng & Fullscreen") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Fullscreen Ngang (Ẩn Status/Nav Bar)", color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Switch(checked = settings.fullscreenEnabled, onCheckedChange = {
                        scope.launch { settingsRepository.updateFullscreenEnabled(it) }
                    })
                }
            }
        }
    }
}

@Composable
fun HiddenAppsSettingsPage(
    settings: LauncherSettings,
    settingsRepository: SettingsRepository
) {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        item {
            SettingsCard(title = "Danh sách ứng dụng đã ẩn") {
                if (settings.hiddenApps.isEmpty()) {
                    Text("Không có ứng dụng nào bị ẩn.", color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                } else {
                    settings.hiddenApps.forEach { pkg ->
                        Text(pkg, color = Color.White, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
        }
    }
}

@Composable
fun ShizukuSettingsPage() {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            SettingsCard(title = "Trạng thái Shizuku (BETA)") {
                Text(
                    text = "Shizuku cho phép khởi chạy ứng dụng dạng cửa sổ nổi freeform chuẩn xác mà không cần root.",
                    color = Color.LightGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Button(onClick = {}) {
                    Text("Cài đặt / Kiểm tra Shizuku", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
    }
}

@Composable
fun PermissionsSettingsPage() {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val adbCommand = "adb shell pm grant com.example.dexlauncher android.permission.WRITE_SECURE_SETTINGS"

    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            SettingsCard(title = "Quyền hệ thống & ADB") {
                Button(onClick = {
                    try { context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) } catch (_: Exception) {}
                }) {
                    Text("Cấp quyền Usage Access", maxLines = 1, overflow = TextOverflow.Ellipsis)
                }

                Spacer(modifier = Modifier.height(8.dp))

                Text("Lệnh ADB cấp quyền bảo mật:", color = Color.LightGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(adbCommand, color = Color.Green, fontSize = 12.sp, modifier = Modifier.weight(1f), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Button(onClick = { clipboardManager.setText(AnnotatedString(adbCommand)) }) {
                            Text("Sao chép", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun AboutSettingsPage() {
    LazyColumn(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            SettingsCard(title = "Thông tin phiên bản") {
                Text("DeX Launcher Pro", color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Phiên bản: 2.0.0 (Android 10+ Optimized)", color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("Giao diện Samsung DeX & One UI kết hợp hoàn hảo.", color = Color.LightGray, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
        }
    }
}
