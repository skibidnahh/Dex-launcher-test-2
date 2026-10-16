package com.example.dexlauncher.ui.desktop

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
    val pinnedDesktopApps by appRepository.pinnedDesktopApps.collectAsState()
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

        // Context Menu
        if (selectedAppForMenu != null) {
            val app = selectedAppForMenu!!
            AlertDialog(
                onDismissRequest = { selectedAppForMenu = null },
                title = { Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                text = { Text("Tùy chọn trên màn hình chính", maxLines = 2, overflow = TextOverflow.Ellipsis) },
                confirmButton = {
                    TextButton(onClick = {
                        scope.launch {
                            appRepository.unpinFromDesktop(app.packageName)
                        }
                        selectedAppForMenu = null
                    }) {
                        Text("Xóa khỏi Desktop", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                },
                dismissButton = {
                    TextButton(onClick = {
                        selectedAppForMenu = null
                    }) {
                        Text("Đóng", maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            )
        }
    }
}
