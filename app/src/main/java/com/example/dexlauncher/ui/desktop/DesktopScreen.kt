package com.example.dexlauncher.ui.desktop

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.dexlauncher.data.model.AppInfo
import com.example.dexlauncher.data.repository.AppRepository
import kotlinx.coroutines.launch

@Composable
fun DesktopScreen(
    appRepository: AppRepository,
    iconSize: Int,
    iconLabelSize: Int,
    iconCornerRadius: Int,
    showLabels: Boolean,
    showShadow: Boolean,
    layoutMode: String,
    onLaunchApp: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val pinnedDesktopApps by appRepository.pinnedDesktopApps.collectAsState()
    val pinnedTaskbarApps by appRepository.pinnedTaskbarApps.collectAsState()
    val scope = rememberCoroutineScope()

    var selectedAppForMenu by remember { mutableStateOf<AppInfo?>(null) }
    var localApps by remember(pinnedDesktopApps) { mutableStateOf(pinnedDesktopApps) }

    var draggedItemPackage by remember { mutableStateOf<String?>(null) }
    var draggedItemIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }
    val itemBounds = remember { mutableStateMapOf<Int, Rect>() }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = (iconSize + 24).dp),
            modifier = Modifier.fillMaxSize(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            itemsIndexed(localApps, key = { _, app -> app.packageName }) { index, app ->
                val isBeingDragged = draggedItemPackage == app.packageName
                val iconBitmap = remember(app.packageName) {
                    appRepository.getAppIconBitmap(app.packageName)
                }

                Column(
                    modifier = Modifier
                        .width((iconSize + 24).dp)
                        .onGloballyPositioned { coords ->
                            itemBounds[index] = coords.boundsInParent()
                        }
                        .zIndex(if (isBeingDragged) 10f else 1f)
                        .graphicsLayer {
                            if (isBeingDragged) {
                                translationX = dragOffset.x
                                translationY = dragOffset.y
                                scaleX = 1.15f
                                scaleY = 1.15f
                                alpha = 0.92f
                                shadowElevation = 16.dp.toPx()
                            }
                        }
                        .clip(RoundedCornerShape(iconCornerRadius.dp))
                        .pointerInput(app.packageName) {
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    draggedItemPackage = app.packageName
                                    draggedItemIndex = index
                                    dragOffset = Offset.Zero
                                    totalDragDistance = 0f
                                },
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    dragOffset += dragAmount
                                    totalDragDistance += dragAmount.getDistance()

                                    if (totalDragDistance > 20f && draggedItemIndex in localApps.indices) {
                                        val currentSlot = itemBounds[draggedItemIndex]
                                        if (currentSlot != null) {
                                            val currentCenter = currentSlot.center + dragOffset
                                            var targetSlotIndex = -1
                                            for ((slotIdx, rect) in itemBounds) {
                                                if (rect.contains(currentCenter)) {
                                                    targetSlotIndex = slotIdx
                                                    break
                                                }
                                            }

                                            if (targetSlotIndex != -1 && targetSlotIndex != draggedItemIndex && targetSlotIndex in localApps.indices) {
                                                val mutable = localApps.toMutableList()
                                                val moved = mutable.removeAt(draggedItemIndex)
                                                mutable.add(targetSlotIndex, moved)
                                                localApps = mutable

                                                val newSlot = itemBounds[targetSlotIndex]
                                                if (newSlot != null) {
                                                    dragOffset = currentCenter - newSlot.center
                                                }
                                                draggedItemIndex = targetSlotIndex
                                            }
                                        }
                                    }
                                },
                                onDragEnd = {
                                    if (totalDragDistance < 20f) {
                                        selectedAppForMenu = app
                                    } else {
                                        scope.launch {
                                            appRepository.saveDesktopAppOrder(localApps.map { it.packageName })
                                        }
                                    }
                                    draggedItemPackage = null
                                    draggedItemIndex = -1
                                    dragOffset = Offset.Zero
                                    totalDragDistance = 0f
                                },
                                onDragCancel = {
                                    draggedItemPackage = null
                                    draggedItemIndex = -1
                                    dragOffset = Offset.Zero
                                    totalDragDistance = 0f
                                }
                            )
                        }
                        .clickable {
                            if (draggedItemPackage == null) {
                                onLaunchApp(app.packageName)
                            }
                        }
                        .padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (iconBitmap != null) {
                        Image(
                            bitmap = iconBitmap,
                            contentDescription = app.label,
                            modifier = Modifier.size(iconSize.dp)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(iconSize.dp)
                                .background(Color.Gray, RoundedCornerShape(iconCornerRadius.dp))
                        )
                    }

                    if (showLabels) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = app.label,
                            color = Color.White,
                            fontSize = iconLabelSize.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // Long-press Context Menu Dialog
        if (selectedAppForMenu != null) {
            val app = selectedAppForMenu!!
            val isPinnedTaskbar = pinnedTaskbarApps.any { it.packageName == app.packageName }

            AlertDialog(
                onDismissRequest = { selectedAppForMenu = null },
                title = { Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Bỏ ghim khỏi Desktop
                        TextButton(
                            onClick = {
                                scope.launch {
                                    appRepository.unpinFromDesktop(app.packageName)
                                }
                                selectedAppForMenu = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Bỏ ghim khỏi Desktop", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        // Ghim / Bỏ ghim Taskbar
                        TextButton(
                            onClick = {
                                scope.launch {
                                    if (isPinnedTaskbar) {
                                        appRepository.unpinFromTaskbar(app.packageName)
                                    } else {
                                        appRepository.pinToTaskbar(app.packageName)
                                    }
                                }
                                selectedAppForMenu = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (isPinnedTaskbar) "Bỏ ghim khỏi Taskbar" else "Ghim vào Taskbar",
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }

                        // Ẩn ứng dụng
                        TextButton(
                            onClick = {
                                appRepository.hideApp(app.packageName)
                                selectedAppForMenu = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Ẩn ứng dụng", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        // Gỡ cài đặt
                        TextButton(
                            onClick = {
                                try {
                                    val uninstallIntent = Intent(Intent.ACTION_DELETE).apply {
                                        data = Uri.parse("package:${app.packageName}")
                                    }
                                    context.startActivity(uninstallIntent)
                                } catch (_: Exception) {}
                                selectedAppForMenu = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Gỡ cài đặt", color = Color(0xFFFF5252), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }

                        // Thông tin ứng dụng
                        TextButton(
                            onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.parse("package:${app.packageName}")
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                                selectedAppForMenu = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Thông tin ứng dụng", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                },
                confirmButton = {},
                dismissButton = {
                    TextButton(onClick = { selectedAppForMenu = null }) {
                        Text("Đóng", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            )
        }
    }
}
