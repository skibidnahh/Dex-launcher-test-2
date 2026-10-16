package com.example.dexlauncher.ui.quickpanel

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun QuickPanelOverlay(
    onDismiss: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val context = LocalContext.current
    var volume by remember { mutableStateOf(0.5f) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Transparent)
            .clickable(onClick = onDismiss),
        contentAlignment = Alignment.BottomEnd
    ) {
        Surface(
            modifier = Modifier
                .padding(end = 16.dp, bottom = 64.dp)
                .width(360.dp)
                .clip(RoundedCornerShape(24.dp)),
            color = Color(0xEE2A2A2A),
            tonalElevation = 16.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Row 1: Wi-Fi & Bluetooth
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    QuickPanelTile(
                        icon = Icons.Default.Wifi,
                        title = "Wi-Fi",
                        subtitle = "Bật",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            try {
                                context.startActivity(Intent(Settings.ACTION_WIFI_SETTINGS))
                            } catch (_: Exception) {}
                        }
                    )
                    QuickPanelTile(
                        icon = Icons.Default.Bluetooth,
                        title = "Bluetooth",
                        subtitle = "Bật",
                        modifier = Modifier.weight(1f),
                        onClick = {
                            try {
                                context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS))
                            } catch (_: Exception) {}
                        }
                    )
                }

                // Row 2: Grid of toggles
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    QuickPanelSmallTile(icon = Icons.Default.VolumeUp, label = "Âm thanh") {}
                    QuickPanelSmallTile(icon = Icons.Default.NetworkCell, label = "Dữ liệu") {}
                    QuickPanelSmallTile(icon = Icons.Default.DarkMode, label = "Chế độ tối") {}
                    QuickPanelSmallTile(icon = Icons.Default.ScreenRotation, label = "Xoay") {}
                }

                // Volume slider
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.VolumeDown, contentDescription = null, tint = Color.White)
                    Slider(
                        value = volume,
                        onValueChange = { volume = it },
                        modifier = Modifier.weight(1f)
                    )
                    Icon(Icons.Default.VolumeUp, contentDescription = null, tint = Color.White)
                }

                // Settings shortcut
                Button(
                    onClick = onOpenSettings,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Settings, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cài đặt Launcher")
                }
            }
        }
    }
}

@Composable
fun QuickPanelTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .height(64.dp)
            .clip(RoundedCornerShape(32.dp))
            .clickable(onClick = onClick),
        color = Color(0xFF3A3A3A)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = Color.Cyan)
            Column {
                Text(text = title, color = Color.White, fontSize = 14.sp)
                Text(text = subtitle, color = Color.Gray, fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun QuickPanelSmallTile(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(48.dp)
                .background(Color(0xFF3A3A3A), RoundedCornerShape(24.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = Color.White)
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = label, color = Color.White, fontSize = 11.sp)
    }
}
