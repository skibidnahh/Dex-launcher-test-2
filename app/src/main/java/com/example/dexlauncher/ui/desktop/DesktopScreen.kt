package com.example.dexlauncher.ui.desktop

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dexlauncher.data.model.AppInfo
import com.example.dexlauncher.data.repository.AppRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
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
            items(pinnedDesktopApps, key = { it.packageName }) { app ->
                val iconBitmap = remember(app.packageName) {
                    appRepository.getAppIconBitmap(app.packageName)
                }

                Column(
                    modifier = Modifier
                        .width((iconSize + 24).dp)
                        .clip(RoundedCornerShape(iconCornerRadius.dp))
                        .combinedClickable(
                            onClick = {
                                onLaunchApp(app.packageName)
                            },
                            onLongClick = {
                                selectedAppForMenu = app
                            }
                        )
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
