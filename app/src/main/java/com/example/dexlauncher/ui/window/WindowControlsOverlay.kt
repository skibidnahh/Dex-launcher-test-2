package com.example.dexlauncher.ui.window

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.dexlauncher.data.repository.AppRepository
import com.example.dexlauncher.shizuku.ShizukuManager
import com.example.dexlauncher.ui.theme.DeXDarkPalette
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

data class FreeformWindowState(
    val id: String,
    val packageName: String,
    val title: String,
    var x: Float,
    var y: Float,
    var width: Float,
    var height: Float,
    var isMinimized: Boolean = false,
    var isMaximized: Boolean = false,
    var isAlwaysOnTop: Boolean = false,
    var opacity: Float = 1.0f,
    var zIndex: Float = 1.0f,
    var preMaxX: Float = 100f,
    var preMaxY: Float = 100f,
    var preMaxW: Float = 600f,
    var preMaxH: Float = 420f
)

object FreeformWindowManager {
    private val _windows = MutableStateFlow<List<FreeformWindowState>>(emptyList())
    val windows: StateFlow<List<FreeformWindowState>> = _windows.asStateFlow()

    private var currentZ = 10f

    fun openOrFocusWindow(
        packageName: String,
        title: String,
        screenWidth: Float,
        screenHeight: Float
    ): FreeformWindowState {
        val existing = _windows.value.find { it.packageName == packageName }
        if (existing != null) {
            existing.isMinimized = false
            currentZ += 1f
            existing.zIndex = if (existing.isAlwaysOnTop) currentZ + 100f else currentZ
            _windows.value = ArrayList(_windows.value)
            return existing
        }

        val cascadeIndex = _windows.value.size % 5
        val initW = (screenWidth * 0.55f).coerceIn(400f, screenWidth - 60f)
        val initH = (screenHeight * 0.55f).coerceIn(280f, screenHeight - 100f)
        val initX = (60f + cascadeIndex * 36f).coerceIn(20f, (screenWidth - initW - 20f).coerceAtLeast(20f))
        val initY = (50f + cascadeIndex * 36f).coerceIn(40f, (screenHeight - initH - 60f).coerceAtLeast(40f))

        currentZ += 1f
        val newWin = FreeformWindowState(
            id = "win_" + UUID.randomUUID().toString().take(8),
            packageName = packageName,
            title = title,
            x = initX,
            y = initY,
            width = initW,
            height = initH,
            zIndex = currentZ
        )
        _windows.value = _windows.value + newWin
        return newWin
    }

    fun closeWindow(id: String) {
        _windows.value = _windows.value.filter { it.id != id }
    }

    fun minimizeWindow(id: String) {
        _windows.value = _windows.value.map {
            if (it.id == id) it.copy(isMinimized = true) else it
        }
    }

    fun restoreWindow(id: String) {
        currentZ += 1f
        _windows.value = _windows.value.map {
            if (it.id == id) it.copy(isMinimized = false, zIndex = currentZ) else it
        }
    }

    fun toggleMaximize(id: String, screenWidth: Float, screenHeight: Float, taskbarHeight: Float = 56f) {
        _windows.value = _windows.value.map { win ->
            if (win.id == id) {
                if (win.isMaximized) {
                    win.copy(
                        x = win.preMaxX,
                        y = win.preMaxY,
                        width = win.preMaxW,
                        height = win.preMaxH,
                        isMaximized = false
                    )
                } else {
                    win.copy(
                        preMaxX = win.x,
                        preMaxY = win.y,
                        preMaxW = win.width,
                        preMaxH = win.height,
                        x = 0f,
                        y = 0f,
                        width = screenWidth,
                        height = screenHeight - taskbarHeight,
                        isMaximized = true
                    )
                }
            } else win
        }
    }

    fun snapLeft(id: String, screenWidth: Float, screenHeight: Float, taskbarHeight: Float = 56f) {
        _windows.value = _windows.value.map { win ->
            if (win.id == id) {
                win.copy(
                    x = 0f,
                    y = 0f,
                    width = screenWidth / 2f,
                    height = screenHeight - taskbarHeight,
                    isMaximized = false
                )
            } else win
        }
    }

    fun snapRight(id: String, screenWidth: Float, screenHeight: Float, taskbarHeight: Float = 56f) {
        _windows.value = _windows.value.map { win ->
            if (win.id == id) {
                win.copy(
                    x = screenWidth / 2f,
                    y = 0f,
                    width = screenWidth / 2f,
                    height = screenHeight - taskbarHeight,
                    isMaximized = false
                )
            } else win
        }
    }

    fun toggleAlwaysOnTop(id: String) {
        _windows.value = _windows.value.map { win ->
            if (win.id == id) {
                val nextOnTop = !win.isAlwaysOnTop
                win.copy(
                    isAlwaysOnTop = nextOnTop,
                    zIndex = if (nextOnTop) currentZ + 100f else currentZ
                )
            } else win
        }
    }

    fun updateOpacity(id: String, opacity: Float) {
        _windows.value = _windows.value.map { win ->
            if (win.id == id) win.copy(opacity = opacity.coerceIn(0.25f, 1.0f)) else win
        }
    }

    fun bringToFront(id: String) {
        currentZ += 1f
        _windows.value = _windows.value.map { win ->
            if (win.id == id) {
                win.copy(zIndex = if (win.isAlwaysOnTop) currentZ + 100f else currentZ)
            } else win
        }
    }

    fun updateBounds(id: String, x: Float, y: Float, width: Float, height: Float) {
        _windows.value = _windows.value.map { win ->
            if (win.id == id) {
                win.copy(x = x, y = y, width = width, height = height)
            } else win
        }
    }
}

// --- FREEFORM WINDOW MANAGER OVERLAY (FEATURE 9, 10, 11, 12, 13) ---
@Composable
fun FreeformWindowOverlay(
    appRepository: AppRepository,
    shizukuManager: ShizukuManager? = null,
    onLaunchNativeApp: (String) -> Unit = {}
) {
    val windows by FreeformWindowManager.windows.collectAsState()

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        val screenW = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val screenH = constraints.maxHeight.toFloat().coerceAtLeast(1f)
        val taskbarHeightPx = 56f

        // Hiển thị từng cửa sổ tự do
        windows.filter { !it.isMinimized }.forEach { windowState ->
            key(windowState.id) {
                FreeformWindowItem(
                    win = windowState,
                    screenWidth = screenW,
                    screenHeight = screenH,
                    taskbarHeight = taskbarHeightPx,
                    appRepository = appRepository,
                    onLaunchNativeApp = onLaunchNativeApp
                )
            }
        }
    }
}

@Composable
fun FreeformWindowItem(
    win: FreeformWindowState,
    screenWidth: Float,
    screenHeight: Float,
    taskbarHeight: Float,
    appRepository: AppRepository,
    onLaunchNativeApp: (String) -> Unit
) {
    var posX by remember(win.x) { mutableFloatStateOf(win.x) }
    var posY by remember(win.y) { mutableFloatStateOf(win.y) }
    var widthPx by remember(win.width) { mutableFloatStateOf(win.width) }
    var heightPx by remember(win.height) { mutableFloatStateOf(win.height) }
    var snapPreviewRegion by remember { mutableStateOf<String?>(null) } // "left", "right", "max"
    var showOpacitySlider by remember { mutableStateOf(false) }

    val iconBitmap = remember(win.packageName) {
        appRepository.getAppIconBitmap(win.packageName)
    }

    // Vùng xem trước Snap Cửa Sổ (Snap Preview)
    if (snapPreviewRegion != null) {
        val snapBounds = when (snapPreviewRegion) {
            "left" -> IntOffset(0, 0) to (screenWidth / 2).toInt() to (screenHeight - taskbarHeight).toInt()
            "right" -> IntOffset((screenWidth / 2).toInt(), 0) to (screenWidth / 2).toInt() to (screenHeight - taskbarHeight).toInt()
            else -> IntOffset(0, 0) to screenWidth.toInt() to (screenHeight - taskbarHeight).toInt()
        }

        Box(
            modifier = Modifier
                .offset { snapBounds.first.first }
                .width(with(LocalDensity.current) { snapBounds.first.second.toDp() })
                .height(with(LocalDensity.current) { snapBounds.second.toDp() })
                .zIndex(50f)
                .background(Color(0x3300E5FF), RoundedCornerShape(12.dp))
                .border(2.dp, Color(0xFF00E5FF), RoundedCornerShape(12.dp))
        )
    }

    Box(
        modifier = Modifier
            .offset { IntOffset(posX.toInt(), posY.toInt()) }
            .width(with(LocalDensity.current) { widthPx.toDp() })
            .height(with(LocalDensity.current) { heightPx.toDp() })
            .zIndex(win.zIndex)
            .shadow(16.dp, RoundedCornerShape(12.dp))
            .clip(RoundedCornerShape(12.dp))
            .background(Color(0xFF1E1E1E).copy(alpha = win.opacity))
            .border(
                1.5.dp,
                if (win.isAlwaysOnTop) Color(0xFFFFD700) else Color(0x55FFFFFF),
                RoundedCornerShape(12.dp)
            )
            .pointerInput(win.id) {
                detectTapGestures {
                    FreeformWindowManager.bringToFront(win.id)
                }
            }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // THANH TIÊU ĐỀ CỬA SỔ (WINDOW HEADER BAR) - Kéo để di chuyển & Snap
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .background(Color(0xFF262626).copy(alpha = win.opacity))
                    .pointerInput(win.id) {
                        detectDragGestures(
                            onDragStart = {
                                FreeformWindowManager.bringToFront(win.id)
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                posX = (posX + dragAmount.x).coerceIn(0f, screenWidth - 100f)
                                posY = (posY + dragAmount.y).coerceIn(0f, screenHeight - 60f)

                                // Phát hiện chạm viền màn hình để Snap (Feature 11)
                                snapPreviewRegion = when {
                                    posX < 24f -> "left"
                                    posX > (screenWidth - widthPx * 0.6f) -> "right"
                                    posY < 16f -> "max"
                                    else -> null
                                }
                            },
                            onDragEnd = {
                                when (snapPreviewRegion) {
                                    "left" -> FreeformWindowManager.snapLeft(win.id, screenWidth, screenHeight, taskbarHeight)
                                    "right" -> FreeformWindowManager.snapRight(win.id, screenWidth, screenHeight, taskbarHeight)
                                    "max" -> FreeformWindowManager.toggleMaximize(win.id, screenWidth, screenHeight, taskbarHeight)
                                    else -> FreeformWindowManager.updateBounds(win.id, posX, posY, widthPx, heightPx)
                                }
                                snapPreviewRegion = null
                            },
                            onDragCancel = {
                                snapPreviewRegion = null
                            }
                        )
                    }
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Tên app và Icon
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f, fill = false)
                ) {
                    if (iconBitmap != null) {
                        Image(bitmap = iconBitmap, contentDescription = null, modifier = Modifier.size(20.dp))
                    } else {
                        Icon(Icons.Default.Apps, contentDescription = null, tint = Color.Cyan, modifier = Modifier.size(18.dp))
                    }
                    Text(
                        text = win.title,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Cụm nút điều khiển cửa sổ
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Nút điều chỉnh độ mờ (Window Opacity - Feature 12)
                    IconButton(
                        onClick = { showOpacitySlider = !showOpacitySlider },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            Icons.Default.Opacity,
                            contentDescription = "Độ trong suốt",
                            tint = if (win.opacity < 1f) Color(0xFF00E5FF) else Color.LightGray,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Nút ghim trên cùng (Always on Top - Feature 13)
                    IconButton(
                        onClick = { FreeformWindowManager.toggleAlwaysOnTop(win.id) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            Icons.Default.PushPin,
                            contentDescription = "Ghim trên cùng",
                            tint = if (win.isAlwaysOnTop) Color(0xFFFFD700) else Color.LightGray,
                            modifier = Modifier.size(14.dp)
                        )
                    }

                    // Snap Trái (Split Screen Left - Feature 11)
                    IconButton(
                        onClick = { FreeformWindowManager.snapLeft(win.id, screenWidth, screenHeight, taskbarHeight) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(Icons.Default.VerticalSplit, contentDescription = "Snap Trái", tint = Color.LightGray, modifier = Modifier.size(14.dp))
                    }

                    // Snap Phải (Split Screen Right - Feature 11)
                    IconButton(
                        onClick = { FreeformWindowManager.snapRight(win.id, screenWidth, screenHeight, taskbarHeight) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(Icons.Default.VerticalSplit, contentDescription = "Snap Phải", tint = Color.LightGray, modifier = Modifier.size(14.dp))
                    }

                    // Thu nhỏ (Minimize - Feature 10)
                    IconButton(
                        onClick = { FreeformWindowManager.minimizeWindow(win.id) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(Icons.Default.HorizontalRule, contentDescription = "Thu nhỏ", tint = DeXDarkPalette.WindowControlMin, modifier = Modifier.size(14.dp))
                    }

                    // Phóng to / Khôi phục (Maximize - Feature 10)
                    IconButton(
                        onClick = { FreeformWindowManager.toggleMaximize(win.id, screenWidth, screenHeight, taskbarHeight) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            if (win.isMaximized) Icons.Default.FilterNone else Icons.Default.CropSquare,
                            contentDescription = "Phóng to",
                            tint = DeXDarkPalette.WindowControlMax,
                            modifier = Modifier.size(13.dp)
                        )
                    }

                    // Đóng cửa sổ (Close - Feature 10)
                    IconButton(
                        onClick = { FreeformWindowManager.closeWindow(win.id) },
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Đóng", tint = DeXDarkPalette.WindowControlClose, modifier = Modifier.size(14.dp))
                    }
                }
            }

            // Thanh trượt chỉnh Opacity khi mở
            if (showOpacitySlider) {
                Surface(
                    color = Color(0xEE1E1E1E),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text("Độ mờ: ${(win.opacity * 100).toInt()}%", fontSize = 11.sp, color = Color.White)
                        Slider(
                            value = win.opacity,
                            onValueChange = { FreeformWindowManager.updateOpacity(win.id, it) },
                            valueRange = 0.3f..1.0f,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // NỘI DUNG CỬA SỔ
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(Color(0xFF121212).copy(alpha = win.opacity))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (iconBitmap != null) {
                        Image(bitmap = iconBitmap, contentDescription = null, modifier = Modifier.size(64.dp))
                    }
                    Text(win.title, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Text(
                        "Cửa sổ đa nhiệm tự do DeX",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = { onLaunchNativeApp(win.packageName) },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00E5FF))
                        ) {
                            Icon(Icons.Default.Launch, contentDescription = null, tint = Color.Black, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Chạy ở chế độ thực", color = Color.Black, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        // --- CÁC CẠNH VÀ GÓC RESIZE CỬA SỔ (FEATURE 9: RESIZE BẰNG CÁCH KÉO CÁC CẠNH VIỀN) ---
        // 1. Cạnh phải
        Box(
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .fillMaxHeight()
                .width(10.dp)
                .pointerInput(win.id) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        widthPx = (widthPx + dragAmount.x).coerceIn(280f, screenWidth - posX)
                        FreeformWindowManager.updateBounds(win.id, posX, posY, widthPx, heightPx)
                    }
                }
        )

        // 2. Cạnh dưới
        Box(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .height(10.dp)
                .pointerInput(win.id) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        heightPx = (heightPx + dragAmount.y).coerceIn(200f, screenHeight - posY - taskbarHeight)
                        FreeformWindowManager.updateBounds(win.id, posX, posY, widthPx, heightPx)
                    }
                }
        )

        // 3. Cạnh trái
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .fillMaxHeight()
                .width(10.dp)
                .pointerInput(win.id) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        val newW = (widthPx - dragAmount.x).coerceIn(280f, posX + widthPx)
                        val deltaX = widthPx - newW
                        posX += deltaX
                        widthPx = newW
                        FreeformWindowManager.updateBounds(win.id, posX, posY, widthPx, heightPx)
                    }
                }
        )

        // 4. Góc dưới - phải (Resize cả 2 chiều)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(18.dp)
                .pointerInput(win.id) {
                    detectDragGestures { change, dragAmount ->
                        change.consume()
                        widthPx = (widthPx + dragAmount.x).coerceIn(280f, screenWidth - posX)
                        heightPx = (heightPx + dragAmount.y).coerceIn(200f, screenHeight - posY - taskbarHeight)
                        FreeformWindowManager.updateBounds(win.id, posX, posY, widthPx, heightPx)
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.SouthEast, contentDescription = null, tint = Color.Gray.copy(alpha = 0.5f), modifier = Modifier.size(12.dp))
        }
    }
}

// Giữ nguyên WindowControlsBar cũ để không phá vỡ bất kỳ tham chiếu nào
@Composable
fun WindowControlsBar(
    taskTitle: String,
    onMinimize: () -> Unit,
    onMaximize: () -> Unit,
    onSnapLeft: () -> Unit,
    onSnapRight: () -> Unit,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(DeXDarkPalette.SurfaceFloating)
            .border(1.dp, DeXDarkPalette.Border, RoundedCornerShape(8.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = taskTitle,
            color = Color.White,
            fontSize = 12.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.weight(1f, fill = false)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            WindowActionButton(onClick = onSnapLeft, contentDescription = "Snap Left") {
                Icon(Icons.Default.VerticalSplit, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
            }
            WindowActionButton(onClick = onSnapRight, contentDescription = "Snap Right") {
                Icon(Icons.Default.VerticalSplit, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(14.dp))
            }
            WindowActionButton(onClick = onMinimize, contentDescription = "Minimize") {
                Icon(Icons.Default.HorizontalRule, contentDescription = null, tint = DeXDarkPalette.WindowControlMin, modifier = Modifier.size(14.dp))
            }
            WindowActionButton(onClick = onMaximize, contentDescription = "Maximize") {
                Icon(Icons.Default.CropSquare, contentDescription = null, tint = DeXDarkPalette.WindowControlMax, modifier = Modifier.size(13.dp))
            }
            WindowActionButton(onClick = onClose, contentDescription = "Close") {
                Icon(Icons.Default.Close, contentDescription = null, tint = DeXDarkPalette.WindowControlClose, modifier = Modifier.size(14.dp))
            }
        }
    }
}

@Composable
fun WindowActionButton(
    onClick: () -> Unit,
    contentDescription: String,
    content: @Composable () -> Unit
) {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.08f))
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
