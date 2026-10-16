package com.example.dexlauncher.ui.window

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CropSquare
import androidx.compose.material.icons.filled.HorizontalRule
import androidx.compose.material.icons.filled.VerticalSplit
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.dexlauncher.ui.theme.DeXDarkPalette

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
            // Snap Left
            WindowActionButton(
                onClick = onSnapLeft,
                contentDescription = "Snap Left"
            ) {
                Icon(
                    imageVector = Icons.Default.VerticalSplit,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(14.dp)
                )
            }

            // Snap Right
            WindowActionButton(
                onClick = onSnapRight,
                contentDescription = "Snap Right"
            ) {
                Icon(
                    imageVector = Icons.Default.VerticalSplit,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(14.dp)
                )
            }

            // Minimize
            WindowActionButton(
                onClick = onMinimize,
                contentDescription = "Minimize"
            ) {
                Icon(
                    imageVector = Icons.Default.HorizontalRule,
                    contentDescription = null,
                    tint = DeXDarkPalette.WindowControlMin,
                    modifier = Modifier.size(14.dp)
                )
            }

            // Maximize
            WindowActionButton(
                onClick = onMaximize,
                contentDescription = "Maximize"
            ) {
                Icon(
                    imageVector = Icons.Default.CropSquare,
                    contentDescription = null,
                    tint = DeXDarkPalette.WindowControlMax,
                    modifier = Modifier.size(13.dp)
                )
            }

            // Close
            WindowActionButton(
                onClick = onClose,
                contentDescription = "Close"
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = DeXDarkPalette.WindowControlClose,
                    modifier = Modifier.size(14.dp)
                )
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
