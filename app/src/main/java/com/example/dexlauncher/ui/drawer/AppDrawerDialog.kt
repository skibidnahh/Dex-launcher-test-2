package com.example.dexlauncher.ui.drawer

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PushPin
import androidx.compose.material.icons.filled.Search
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
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.dexlauncher.data.model.AppInfo
import com.example.dexlauncher.data.repository.AppRepository
import kotlinx.coroutines.launch

@Composable
fun AppDrawerOverlay(
    visible: Boolean,
    appRepository: AppRepository,
    onDismiss: () -> Unit,
    onStartDragIcon: (AppInfo, Offset) -> Unit = { _, _ -> },
    onLaunchApp: (String) -> Unit = {},
    onDragIconMove: (Offset) -> Unit = {},
    onDragIconEnd: () -> Unit = {}
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val installedApps by appRepository.installedApps.collectAsState()
    val hiddenPackages by appRepository.hiddenPackages.collectAsState()
    val pinnedDesktopApps by appRepository.pinnedDesktopApps.collectAsState()
    val pinnedTaskbarApps by appRepository.pinnedTaskbarApps.collectAsState()

    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    var contextMenuApp by remember { mutableStateOf<AppInfo?>(null) }
    var draggingApp by remember { mutableStateOf<AppInfo?>(null) }
    var dragPosition by remember { mutableStateOf(Offset.Zero) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }
    var dragVelocityX by remember { mutableFloatStateOf(0f) }
    val itemCoords = remember { mutableStateMapOf<String, Offset>() }

    val filteredApps = remember(installedApps, hiddenPackages, searchQuery) {
        installedApps.filter { app ->
            !hiddenPackages.contains(app.packageName) &&
            (searchQuery.isBlank() || app.label.contains(searchQuery, ignoreCase = true))
        }
    }

    val screenHeightPx = context.resources.displayMetrics.heightPixels.toFloat()
    val screenWidthPx = context.resources.displayMetrics.widthPixels.toFloat()
    val taskbarHeightPx = with(density) { 64.dp.toPx() }

    // Kiểm tra xem vị trí ngón tay đang nằm trên thanh tác vụ (Taskbar) hay không
    val isOverTaskbar = remember(dragPosition, screenHeightPx, taskbarHeightPx) {
        dragPosition.y >= (screenHeightPx - taskbarHeightPx)
    }

    // Kích thước ô lưới mô phỏng trên màn hình chính
    val cellWidthPx = with(density) { 96.dp.toPx() }
    val cellHeightPx = with(density) { 104.dp.toPx() }
    val padXPx = with(density) { 16.dp.toPx() }
    val padYPx = with(density) { 16.dp.toPx() }

    val cols = remember(screenWidthPx, cellWidthPx, padXPx) {
        ((screenWidthPx - padXPx * 2) / cellWidthPx).toInt().coerceAtLeast(1)
    }
    val rows = remember(screenHeightPx, taskbarHeightPx, cellHeightPx, padYPx) {
        ((screenHeightPx - taskbarHeightPx - padYPx * 2) / cellHeightPx).toInt().coerceAtLeast(1)
    }

    // Tính ô lưới mà ngón tay đang trỏ tới trên màn hình chính
    val targetCell = remember(dragPosition, cols, rows, padXPx, padYPx, cellWidthPx, cellHeightPx, isOverTaskbar) {
        if (isOverTaskbar || dragPosition == Offset.Zero) null
        else {
            val c = ((dragPosition.x - padXPx) / cellWidthPx).toInt()
            val r = ((dragPosition.y - padYPx) / cellHeightPx).toInt()
            if (c in 0 until cols && r in 0 until rows) Pair(c, r) else null
        }
    }

    // Xác định xem ngón tay đã kéo ra khỏi ngăn chứa App Drawer chưa
    val drawerWidthPx = with(density) { 480.dp.toPx() }
    val drawerHeightPx = with(density) { 600.dp.toPx() }
    val isDraggingOutside = remember(dragPosition, draggingApp) {
        if (draggingApp == null) false
        else dragPosition.x > (drawerWidthPx + 30f) || dragPosition.y < (screenHeightPx - drawerHeightPx - 50f)
    }

    val animatedDrawerAlpha by animateFloatAsState(
        targetValue = when {
            draggingApp == null -> 1f
            isDraggingOutside -> 0.08f
            else -> 0.82f
        },
        animationSpec = tween(durationMillis = 200),
        label = "drawerAlpha"
    )

    val animatedScrimAlpha by animateFloatAsState(
        targetValue = when {
            draggingApp == null -> 0.45f
            isDraggingOutside -> 0.02f
            else -> 0.25f
        },
        animationSpec = tween(durationMillis = 200),
        label = "scrimAlpha"
    )

    val animatedTilt by animateFloatAsState(
        targetValue = (dragVelocityX * 0.14f).coerceIn(-14f, 14f),
        animationSpec = spring(stiffness = 500f, dampingRatio = 0.7f),
        label = "drawerGhostTilt"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.92f),
        exit = fadeOut() + scaleOut(targetScale = 0.92f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = animatedScrimAlpha))
                .clickable {
                    if (draggingApp == null) {
                        onDismiss()
                    }
                },
            contentAlignment = Alignment.BottomStart
        ) {
            // Khi đang kéo ra ngoài màn hình chính: hiển thị lưới ô đứt nét & ô định vị (Indicator)
            if (draggingApp != null && isDraggingOutside && !isOverTaskbar) {
                // 1. Lưới ô mờ đứt nét trên desktop
                Box(modifier = Modifier.fillMaxSize()) {
                    for (c in 0 until cols) {
                        for (r in 0 until rows) {
                            Box(
                                modifier = Modifier
                                    .offset {
                                        IntOffset(
                                            (padXPx + c * cellWidthPx + with(density) { 4.dp.toPx() }).toInt(),
                                            (padYPx + r * cellHeightPx + with(density) { 4.dp.toPx() }).toInt()
                                        )
                                    }
                                    .size(88.dp, 96.dp)
                                    .border(
                                        width = 1.dp,
                                        color = Color.White.copy(alpha = 0.16f),
                                        shape = RoundedCornerShape(18.dp)
                                    )
                            )
                        }
                    }

                    // 2. Ô định vị mục tiêu (Indicator)
                    if (targetCell != null) {
                        val indX = padXPx + targetCell.first * cellWidthPx + with(density) { 4.dp.toPx() }
                        val indY = padYPx + targetCell.second * cellHeightPx + with(density) { 4.dp.toPx() }
                        Box(
                            modifier = Modifier
                                .offset { IntOffset(indX.toInt(), indY.toInt()) }
                                .size(88.dp, 96.dp)
                                .background(Color(120, 170, 255).copy(alpha = 0.20f), RoundedCornerShape(18.dp))
                                .border(2.dp, Color(120, 170, 255).copy(alpha = 0.95f), RoundedCornerShape(18.dp))
                        )
                    }
                }
            }

            // Giao diện khung Ngăn kéo ứng dụng (App Drawer)
            Surface(
                modifier = Modifier
                    .padding(start = 16.dp, bottom = 64.dp)
                    .width(480.dp)
                    .height(600.dp)
                    .graphicsLayer {
                        alpha = animatedDrawerAlpha
                    }
                    .clip(RoundedCornerShape(20.dp)),
                color = Color(0xF21C2230),
                tonalElevation = 16.dp,
                border = BorderStroke(1.dp, Color(0x33FFFFFF))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        modifier = Modifier.fillMaxWidth(),
                        placeholder = { Text("Tìm kiếm ứng dụng...", color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color(0xFF00E5FF)) },
                        singleLine = true,
                        shape = RoundedCornerShape(20.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF00E5FF),
                            unfocusedBorderColor = Color(0x33FFFFFF),
                            focusedContainerColor = Color(0x22FFFFFF),
                            unfocusedContainerColor = Color(0x14FFFFFF)
                        )
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    LazyVerticalGrid(
                        columns = GridCells.Fixed(4),
                        modifier = Modifier.weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(filteredApps, key = { it.packageName }) { app ->
                            val iconBitmap = remember(app.packageName) {
                                appRepository.getAppIconBitmap(app.packageName)
                            }
                            val isPinnedDesktop = pinnedDesktopApps.any { it.packageName == app.packageName }
                            val isItemBeingDragged = draggingApp?.packageName == app.packageName
                            val itemAlpha = if (isItemBeingDragged) 0.25f else 1f

                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .onGloballyPositioned { coords ->
                                        itemCoords[app.packageName] = coords.positionInRoot()
                                    }
                                    .pointerInput(app.packageName) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { offset ->
                                                draggingApp = app
                                                val startRoot = itemCoords[app.packageName] ?: Offset.Zero
                                                dragPosition = startRoot + offset
                                                totalDragDistance = 0f
                                                dragVelocityX = 0f
                                                onStartDragIcon(app, offset)
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragPosition += dragAmount
                                                totalDragDistance += dragAmount.getDistance()
                                                dragVelocityX = dragVelocityX * 0.7f + dragAmount.x * 0.3f
                                                onDragIconMove(dragAmount)
                                            },
                                            onDragEnd = {
                                                if (totalDragDistance < 20f) {
                                                    // Nhấn giữ tại chỗ -> mở menu tùy chọn
                                                    contextMenuApp = app
                                                } else if (isOverTaskbar) {
                                                    // Kéo thả thẳng xuống Taskbar để ghim (Tính năng số 15)
                                                    scope.launch {
                                                        appRepository.pinToTaskbar(app.packageName)
                                                    }
                                                    Toast.makeText(context, "Đã ghim ${app.label} vào Taskbar", Toast.LENGTH_SHORT).show()
                                                    onDragIconEnd()
                                                    onDismiss()
                                                } else {
                                                    // Đã kéo -> Ghim ra Desktop tại ô thả ngón tay (Tính năng số 14)
                                                    scope.launch {
                                                        appRepository.pinToDesktop(app.packageName)
                                                        if (targetCell != null && screenWidthPx > 0 && screenHeightPx > 0) {
                                                            val dropX = (padXPx + targetCell.first * cellWidthPx + 10f) / screenWidthPx
                                                            val dropY = (padYPx + targetCell.second * cellHeightPx + 10f) / screenHeightPx
                                                            appRepository.saveDesktopAppPosition(app.packageName, dropX.coerceIn(0.04f, 0.90f), dropY.coerceIn(0.04f, 0.86f))
                                                        } else if (screenWidthPx > 0 && screenHeightPx > 0) {
                                                            val dropX = (dragPosition.x / screenWidthPx).coerceIn(0.05f, 0.92f)
                                                            val dropY = (dragPosition.y / screenHeightPx).coerceIn(0.05f, 0.88f)
                                                            appRepository.saveDesktopAppPosition(app.packageName, dropX, dropY)
                                                        }
                                                    }
                                                    Toast.makeText(context, "Đã đặt ${app.label} vào Màn hình chính", Toast.LENGTH_SHORT).show()
                                                    onDragIconEnd()
                                                    onDismiss()
                                                }
                                                draggingApp = null
                                                totalDragDistance = 0f
                                                dragVelocityX = 0f
                                            },
                                            onDragCancel = {
                                                draggingApp = null
                                                totalDragDistance = 0f
                                                dragVelocityX = 0f
                                                onDragIconEnd()
                                            }
                                        )
                                    }
                                    .clickable {
                                        if (draggingApp == null) {
                                            onDismiss()
                                            onLaunchApp(app.packageName)
                                        }
                                    }
                                    .padding(8.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                // Biểu tượng app kèm chấm xanh nhận diện app đã có trên Desktop
                                Box(contentAlignment = Alignment.TopEnd) {
                                    if (iconBitmap != null) {
                                        Image(
                                            bitmap = iconBitmap,
                                            contentDescription = app.label,
                                            modifier = Modifier
                                                .size(48.dp)
                                                .graphicsLayer { alpha = itemAlpha }
                                        )
                                    } else {
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .background(Color.Gray.copy(alpha = itemAlpha), RoundedCornerShape(12.dp))
                                        )
                                    }

                                    // Chấm xanh biểu thị ứng dụng đã xuất hiện trên Màn hình chính
                                    if (isPinnedDesktop) {
                                        Box(
                                            modifier = Modifier
                                                .size(9.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFF7BE28A))
                                                .border(1.5.dp, Color(0xFF1C2230), CircleShape)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = app.label,
                                    color = Color.White.copy(alpha = itemAlpha),
                                    fontSize = 11.5.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Dòng ghi chú chân ngăn kéo
                    Text(
                        text = "Nhấn giữ một ứng dụng rồi kéo ra màn hình chính. Chấm xanh là ứng dụng đã có trên màn hình.",
                        color = Color.White.copy(alpha = 0.55f),
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            // Preview icon nổi bám theo ngón tay khi đang kéo thả (Ghost icon với độ nghiêng vật lý)
            if (draggingApp != null) {
                val dragIconBitmap = remember(draggingApp!!.packageName) {
                    appRepository.getAppIconBitmap(draggingApp!!.packageName)
                }
                Surface(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (dragPosition.x - 54.dp.toPx()).toInt(),
                                (dragPosition.y - 70.dp.toPx()).toInt()
                            )
                        }
                        .zIndex(100f)
                        .graphicsLayer {
                            scaleX = 1.16f
                            scaleY = 1.16f
                            rotationZ = animatedTilt
                            shadowElevation = 24.dp.toPx()
                        }
                        .clip(RoundedCornerShape(18.dp)),
                    color = Color(0xEE1E2436),
                    shadowElevation = 24.dp,
                    border = BorderStroke(
                        2.dp,
                        if (isOverTaskbar) Color(0xFF00FF88) else Color(0xFF78AAFF)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        if (dragIconBitmap != null) {
                            Image(
                                bitmap = dragIconBitmap,
                                contentDescription = draggingApp!!.label,
                                modifier = Modifier.size(54.dp)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(54.dp)
                                    .background(Color.Gray, RoundedCornerShape(12.dp))
                            )
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = draggingApp!!.label,
                            color = Color.White,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                Icons.Default.PushPin,
                                contentDescription = null,
                                tint = if (isOverTaskbar) Color(0xFF00FF88) else Color(0xFF78AAFF),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = when {
                                    isOverTaskbar -> "Thả vào Taskbar để ghim"
                                    isDraggingOutside -> "Thả vào ô lưới Desktop"
                                    else -> "Kéo ra Desktop hoặc Taskbar"
                                },
                                color = if (isOverTaskbar) Color(0xFF00FF88) else Color(0xFF78AAFF),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }
    }

    // Context Menu khi nhấn giữ app trong Drawer
    if (contextMenuApp != null) {
        val app = contextMenuApp!!
        val isPinnedDesktop = pinnedDesktopApps.any { it.packageName == app.packageName }
        val isPinnedTaskbar = pinnedTaskbarApps.any { it.packageName == app.packageName }

        AlertDialog(
            onDismissRequest = { contextMenuApp = null },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    val iconBitmap = remember(app.packageName) {
                        appRepository.getAppIconBitmap(app.packageName)
                    }
                    if (iconBitmap != null) {
                        Image(bitmap = iconBitmap, contentDescription = null, modifier = Modifier.size(36.dp))
                    }
                    Column {
                        Text(text = app.label, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        Text(text = app.packageName, fontSize = 11.sp, color = Color.Gray, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    TextButton(
                        onClick = {
                            scope.launch {
                                if (isPinnedDesktop) appRepository.unpinFromDesktop(app.packageName)
                                else appRepository.pinToDesktop(app.packageName)
                            }
                            contextMenuApp = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (isPinnedDesktop) "Bỏ ghim khỏi Màn hình chính" else "Ghim ra Màn hình chính (Desktop)",
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    TextButton(
                        onClick = {
                            scope.launch {
                                if (isPinnedTaskbar) appRepository.unpinFromTaskbar(app.packageName)
                                else appRepository.pinToTaskbar(app.packageName)
                            }
                            contextMenuApp = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (isPinnedTaskbar) "Bỏ ghim khỏi Taskbar" else "Ghim vào Thanh tác vụ (Taskbar)",
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    TextButton(
                        onClick = {
                            appRepository.hideApp(app.packageName)
                            contextMenuApp = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Ẩn ứng dụng khỏi Ngăn kéo", modifier = Modifier.fillMaxWidth(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    TextButton(
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", app.packageName, null)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                            contextMenuApp = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Thông tin ứng dụng", modifier = Modifier.fillMaxWidth(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { contextMenuApp = null }) {
                    Text("Đóng")
                }
            }
        )
    }
}
