package com.example.dexlauncher.ui.drawer

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
            isDraggingOutside -> 0.18f
            else -> 0.88f
        },
        animationSpec = tween(durationMillis = 200),
        label = "drawerAlpha"
    )

    val animatedScrimAlpha by animateFloatAsState(
        targetValue = when {
            draggingApp == null -> 0.45f
            isDraggingOutside -> 0.08f
            else -> 0.30f
        },
        animationSpec = tween(durationMillis = 200),
        label = "scrimAlpha"
    )

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.9f),
        exit = fadeOut() + scaleOut(targetScale = 0.9f)
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
            Surface(
                modifier = Modifier
                    .padding(start = 16.dp, bottom = 64.dp)
                    .width(480.dp)
                    .height(600.dp)
                    .graphicsLayer {
                        alpha = animatedDrawerAlpha
                    }
                    .clip(RoundedCornerShape(16.dp)),
                color = Color(0xEE212121),
                tonalElevation = 16.dp
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
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.LightGray) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color.Gray
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

                            Column(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
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
                                                onStartDragIcon(app, offset)
                                            },
                                            onDrag = { change, dragAmount ->
                                                change.consume()
                                                dragPosition += dragAmount
                                                totalDragDistance += dragAmount.getDistance()
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
                                                    onDragIconEnd()
                                                    onDismiss()
                                                } else {
                                                    // Đã kéo -> Ghim ra Desktop tại vị trí thả ngón tay (Tính năng số 14)
                                                    scope.launch {
                                                        appRepository.pinToDesktop(app.packageName)
                                                        if (screenWidthPx > 0 && screenHeightPx > 0) {
                                                            val dropX = (dragPosition.x / screenWidthPx).coerceIn(0.05f, 0.92f)
                                                            val dropY = (dragPosition.y / screenHeightPx).coerceIn(0.05f, 0.88f)
                                                            appRepository.saveDesktopAppPosition(app.packageName, dropX, dropY)
                                                        }
                                                    }
                                                    onDragIconEnd()
                                                    onDismiss()
                                                }
                                                draggingApp = null
                                                totalDragDistance = 0f
                                            },
                                            onDragCancel = {
                                                draggingApp = null
                                                totalDragDistance = 0f
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
                                val isItemBeingDragged = draggingApp?.packageName == app.packageName
                                val itemAlpha = if (isItemBeingDragged) 0.32f else 1f

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
                                            .background(Color.Gray.copy(alpha = itemAlpha), RoundedCornerShape(8.dp))
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = app.label,
                                    color = Color.White.copy(alpha = itemAlpha),
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Preview icon nổi bám theo ngón tay khi đang kéo thả
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
                        .graphicsLayer {
                            scaleX = 1.15f
                            scaleY = 1.15f
                            shadowElevation = 24.dp.toPx()
                        }
                        .clip(RoundedCornerShape(16.dp)),
                    color = Color(0xEE1E1E1E),
                    shadowElevation = 24.dp,
                    border = BorderStroke(
                        2.dp,
                        if (isOverTaskbar) Color(0xFF00FF88) else Color(0xFF00E5FF)
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
                                    .background(Color.Gray, RoundedCornerShape(10.dp))
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
                                tint = if (isOverTaskbar) Color(0xFF00FF88) else Color(0xFF00E5FF),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = when {
                                    isOverTaskbar -> "Thả vào Taskbar để ghim"
                                    isDraggingOutside -> "Thả vào Desktop để ghim"
                                    else -> "Kéo ra Desktop hoặc Taskbar"
                                },
                                color = if (isOverTaskbar) Color(0xFF00FF88) else Color(0xFF00E5FF),
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
