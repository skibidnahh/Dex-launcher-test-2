package com.example.dexlauncher.ui.taskbar

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
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
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TaskbarView(
    appRepository: AppRepository,
    onToggleAppDrawer: () -> Unit,
    onToggleQuickPanel: () -> Unit,
    onOpenSettings: () -> Unit,
    onToggleStandby: () -> Unit,
    onLaunchApp: (String) -> Unit = {}
) {
    val pinnedTaskbarApps by appRepository.pinnedTaskbarApps.collectAsState()
    val installedApps by appRepository.installedApps.collectAsState()
    val runningPackages by appRepository.runningPackages.collectAsState()
    val focusedPackage by appRepository.focusedPackage.collectAsState()

    val runningUnpinnedApps = remember(runningPackages, pinnedTaskbarApps) {
        installedApps.filter { app -> runningPackages.contains(app.packageName) && pinnedTaskbarApps.none { it.packageName == app.packageName } }
    }

    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delayToNextMinute()
        }
    }

    val timeStr = remember(currentTime) {
        SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(currentTime))
    }
    val dateStr = remember(currentTime) {
        SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()).format(Date(currentTime))
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp),
        color = Color(0xDD1E1E1E),
        tonalElevation = 8.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onToggleAppDrawer,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(Icons.Default.Apps, contentDescription = "Ngăn ứng dụng", tint = Color.White)
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = {},
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Menu, contentDescription = "Ứng dụng gần đây", tint = Color.LightGray)
            }
            IconButton(
                onClick = {},
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Home, contentDescription = "Trang chủ", tint = Color.LightGray)
            }
            IconButton(
                onClick = {},
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.ArrowBack, contentDescription = "Quay lại", tint = Color.LightGray)
            }

            VerticalDivider(
                modifier = Modifier
                    .height(24.dp)
                    .padding(horizontal = 8.dp),
                color = Color.Gray.copy(alpha = 0.5f)
            )

            LazyRow(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                items(pinnedTaskbarApps, key = { it.packageName }) { app ->
                    val isRunning = runningPackages.contains(app.packageName)
                    val isFocused = focusedPackage == app.packageName
                    TaskbarAppItem(
                        app = app,
                        appRepository = appRepository,
                        isRunning = isRunning,
                        isFocused = isFocused,
                        onClick = {
                            onLaunchApp(app.packageName)
                        }
                    )
                }

                if (runningUnpinnedApps.isNotEmpty()) {
                    item {
                        VerticalDivider(
                            modifier = Modifier
                                .height(20.dp)
                                .padding(horizontal = 4.dp),
                            color = Color.White.copy(alpha = 0.3f)
                        )
                    }
                    items(runningUnpinnedApps, key = { "run_${it.packageName}" }) { app ->
                        val isFocused = focusedPackage == app.packageName
                        TaskbarAppItem(
                            app = app,
                            appRepository = appRepository,
                            isRunning = true,
                            isFocused = isFocused,
                            onClick = {
                                onLaunchApp(app.packageName)
                            }
                        )
                    }
                }
            }

            Row(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onToggleQuickPanel() }
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Wifi, contentDescription = "Wi-Fi", tint = Color.White, modifier = Modifier.size(16.dp))
                Icon(Icons.Default.VolumeUp, contentDescription = "Âm lượng", tint = Color.White, modifier = Modifier.size(16.dp))
                Icon(Icons.Default.BatteryFull, contentDescription = "Pin", tint = Color.White, modifier = Modifier.size(16.dp))

                Column(horizontalAlignment = Alignment.End) {
                    Text(text = timeStr, color = Color.White, fontSize = 12.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = dateStr, color = Color.LightGray, fontSize = 10.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = onToggleStandby,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(Icons.Default.Lock, contentDescription = "Khóa màn hình chờ", tint = Color.LightGray)
            }
        }
    }
}

@Composable
fun TaskbarAppItem(
    app: AppInfo,
    appRepository: AppRepository,
    isRunning: Boolean,
    isFocused: Boolean,
    onClick: () -> Unit
) {
    val iconBitmap = remember(app.packageName) {
        appRepository.getAppIconBitmap(app.packageName)
    }

    Box(
        modifier = Modifier
            .size(44.dp)
            .clip(RoundedCornerShape(6.dp))
            .background(if (isFocused) Color.White.copy(alpha = 0.2f) else Color.Transparent)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            if (iconBitmap != null) {
                Image(
                    bitmap = iconBitmap,
                    contentDescription = app.label,
                    modifier = Modifier.size(28.dp)
                )
            } else {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .background(Color.DarkGray, RoundedCornerShape(4.dp))
                )
            }

            if (isRunning) {
                Spacer(modifier = Modifier.height(2.dp))
                Box(
                    modifier = Modifier
                        .width(if (isFocused) 20.dp else 10.dp)
                        .height(2.dp)
                        .background(if (isFocused) Color.Cyan else Color.White, RoundedCornerShape(1.dp))
                )
            }
        }
    }
}

private suspend fun delayToNextMinute() {
    val cal = Calendar.getInstance()
    val millisToNextMinute = (60 - cal.get(Calendar.SECOND)) * 1000L - cal.get(Calendar.MILLISECOND)
    kotlinx.coroutines.delay(if (millisToNextMinute > 0) millisToNextMinute else 1000L)
}
