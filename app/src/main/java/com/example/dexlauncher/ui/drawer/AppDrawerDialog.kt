package com.example.dexlauncher.ui.drawer

import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dexlauncher.data.model.AppInfo
import com.example.dexlauncher.data.repository.AppRepository
import kotlinx.coroutines.launch

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppDrawerOverlay(
    visible: Boolean,
    appRepository: AppRepository,
    onDismiss: () -> Unit,
    onStartDragIcon: (AppInfo, androidx.compose.ui.geometry.Offset) -> Unit,
    onLaunchApp: (String) -> Unit = {}
) {
    val context = LocalContext.current
    val installedApps by appRepository.installedApps.collectAsState()
    val hiddenPackages by appRepository.hiddenPackages.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    val scope = rememberCoroutineScope()

    var contextMenuApp by remember { mutableStateOf<AppInfo?>(null) }

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
                .background(Color.Black.copy(alpha = 0.4f))
                .combinedClickable(onClick = onDismiss, onLongClick = {}),
            contentAlignment = Alignment.BottomStart
        ) {
            Surface(
                modifier = Modifier
                    .padding(start = 16.dp, bottom = 64.dp)
                    .width(480.dp)
                    .height(600.dp)
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
                                    .pointerInput(app) {
                                        detectDragGesturesAfterLongPress(
                                            onDragStart = { offset ->
                                                onDismiss()
                                                onStartDragIcon(app, offset)
                                            },
                                            onDrag = { _, _ -> },
                                            onDragEnd = {},
                                            onDragCancel = {}
                                        )
                                    }
                                    .combinedClickable(
                                        onClick = {
                                            onDismiss()
                                            onLaunchApp(app.packageName)
                                        },
                                        onLongClick = {
                                            contextMenuApp = app
                                        }
                                    )
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

            // Context Menu Dialog for Long Press on App
            if (contextMenuApp != null) {
                val app = contextMenuApp!!
                AlertDialog(
                    onDismissRequest = { contextMenuApp = null },
                    title = { Text(app.label, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                    text = { Text("Tùy chọn ứng dụng", maxLines = 2, overflow = TextOverflow.Ellipsis) },
                    confirmButton = {
                        TextButton(onClick = {
                            scope.launch {
                                appRepository.pinToDesktop(app.packageName)
                            }
                            contextMenuApp = null
                        }) {
                            Text("Thêm Desktop", maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    },
                    dismissButton = {
                        Row {
                            TextButton(onClick = {
                                scope.launch {
                                    appRepository.pinToTaskbar(app.packageName)
                                }
                                contextMenuApp = null
                            }) {
                                Text("Ghim Taskbar", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                            TextButton(onClick = {
                                try {
                                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                        data = Uri.parse("package:${app.packageName}")
                                    }
                                    context.startActivity(intent)
                                } catch (_: Exception) {}
                                contextMenuApp = null
                            }) {
                                Text("Thông tin", maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                )
            }
        }
    }
}
