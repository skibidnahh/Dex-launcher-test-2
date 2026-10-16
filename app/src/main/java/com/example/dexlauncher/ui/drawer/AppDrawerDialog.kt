package com.example.dexlauncher.ui.drawer

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.*
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalContext
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

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + scaleIn(initialScale = 0.9f),
        exit = fadeOut() + scaleOut(targetScale = 0.9f)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = if (draggingApp != null) 0.15f else 0.45f))
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
                        alpha = if (draggingApp != null) 0.15f else 1f
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
                                                    contextMenuApp = app
                                                } else {
                                                    scope.launch {
                                                        appRepository.pinToDesktop(app.packageName)
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
                                if (iconBitmap != null) {
                                    Image(
                                        bitmap = iconBitmap,
                                        contentDescription = app.label,
                                        modifier = Modifier.size(48.dp)
                                    )
                                } else {
                                    Box(
                                        modifier = Modifier
                                            .size(48.dp)
                                            .background(Color.Gray, RoundedCornerShape(8.dp))
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = app.label,
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Floating drag preview that follows finger over desktop
            if (draggingApp != null) {
                val dragIconBitmap = remember(draggingApp!!.packageName) {
                    appRepository.getAppIconBitmap(draggingApp!!.packageName)
                }
                Surface(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (dragPosition.x - 48.dp.toPx()).toInt(),
                                (dragPosition.y - 56.dp.toPx()).toInt()
                            )
                        }
                        .clip(RoundedCornerShape(16.dp)),
                    color = Color(0xEE1E1E1E),
                    shadowElevation = 18.dp,
                    border = BorderStroke(2.dp, Color(0xFF00E5FF))
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
                        Text(
                            text = "Thả vào Desktop để ghim",
                            color = Color(0xFF80CBC4),
                            fontSize = 10.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            // Context Menu Dialog for Long Press on App
            if (contextMenuApp != null) {
                val app = contextMenuApp!!
                val isPinnedDesktop = pinnedDesktopApps.any { it.packageName == app.packageName }
                val isPinnedTaskbar = pinnedTaskbarApps.any { it.packageName == app.packageName }

                AlertDialog(
                    onDismissRequest = { contextMenuApp = null },
                    title = { Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Ghim / Bỏ ghim Desktop
                            TextButton(
                                onClick = {
                                    scope.launch {
                                        if (isPinnedDesktop) {
                                            appRepository.unpinFromDesktop(app.packageName)
                                        } else {
                                            appRepository.pinToDesktop(app.packageName)
                                        }
                                    }
                                    contextMenuApp = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    if (isPinnedDesktop) "Bỏ ghim khỏi Desktop" else "Ghim ra Desktop",
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
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
                                    contextMenuApp = null
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
                                    contextMenuApp = null
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
                                    contextMenuApp = null
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
                                    contextMenuApp = null
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Thông tin ứng dụng", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    },
                    confirmButton = {},
                    dismissButton = {
                        TextButton(onClick = { contextMenuApp = null }) {
                            Text("Đóng", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                )
            }
        }
    }
}
