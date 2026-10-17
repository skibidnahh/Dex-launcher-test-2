package com.example.dexlauncher.ui.desktop

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.BatteryManager
import android.os.Environment
import android.os.StatFs
import android.provider.Settings
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.boundsInParent
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import com.example.dexlauncher.data.model.AppInfo
import com.example.dexlauncher.data.repository.AppRepository
import com.example.dexlauncher.data.repository.DesktopFolder
import com.example.dexlauncher.data.repository.DesktopWidgetItem
import com.example.dexlauncher.data.repository.DesktopWidgetType
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.roundToInt

@Composable
fun DesktopScreen(
    appRepository: AppRepository,
    iconSize: Int,
    iconLabelSize: Int,
    iconCornerRadius: Int,
    showLabels: Boolean,
    showShadow: Boolean,
    layoutMode: String,
    onLaunchApp: (String) -> Unit = {},
    onToggleLayoutMode: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val pinnedDesktopApps by appRepository.pinnedDesktopApps.collectAsState()
    val pinnedTaskbarApps by appRepository.pinnedTaskbarApps.collectAsState()
    val installedApps by appRepository.installedApps.collectAsState()
    val folders by appRepository.folders.collectAsState()
    val widgets by appRepository.widgets.collectAsState()
    val scope = rememberCoroutineScope()

    var currentLayoutMode by remember(layoutMode) { mutableStateOf(layoutMode) }
    var selectedAppForMenu by remember { mutableStateOf<AppInfo?>(null) }
    var selectedFolderForDetail by remember { mutableStateOf<DesktopFolder?>(null) }
    var selectedFolderForMenu by remember { mutableStateOf<DesktopFolder?>(null) }
    var isDesktopBackgroundMenuOpen by remember { mutableStateOf(false) }
    var isAddWidgetDialogOpen by remember { mutableStateOf(false) }

    // Danh sách ứng dụng ngoài desktop (loại bỏ những app đã được gom vào thư mục)
    val folderPackageSet = remember(folders) {
        folders.flatMap { it.packageNames }.toSet()
    }
    val standaloneApps = remember(pinnedDesktopApps, folderPackageSet) {
        pinnedDesktopApps.filter { !folderPackageSet.contains(it.packageName) }
    }
    var localApps by remember(standaloneApps) { mutableStateOf(standaloneApps) }

    // Tự động căn chỉnh kích thước biểu tượng và phông chữ theo độ phân giải & DPI màn hình
    val configuration = LocalConfiguration.current
    val screenWidthDp = configuration.screenWidthDp.toFloat()
    val scaleFactor = (screenWidthDp / 800f).coerceIn(0.85f, 1.40f)
    val effectiveIconSize = (iconSize * scaleFactor).toInt().coerceIn(42, 100)
    val effectiveLabelSize = (iconLabelSize * scaleFactor).toInt().coerceIn(10, 16)

    var draggedItemPackage by remember { mutableStateOf<String?>(null) }
    var draggedItemIndex by remember { mutableIntStateOf(-1) }
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var totalDragDistance by remember { mutableFloatStateOf(0f) }
    var hoverTargetAppPkg by remember { mutableStateOf<String?>(null) }
    var hoverTargetFolderId by remember { mutableStateOf<String?>(null) }
    val itemBounds = remember { mutableStateMapOf<Int, Rect>() }
    val folderBounds = remember { mutableStateMapOf<String, Rect>() }

    // Tọa độ tự do (Freeform desktop positions map)
    var freeformPositions by remember {
        mutableStateOf(appRepository.getDesktopAppPositions())
    }

    val textShadow = remember(showShadow) {
        if (showShadow) {
            Shadow(
                color = Color.Black.copy(alpha = 0.9f),
                offset = Offset(2f, 2f),
                blurRadius = 6f
            )
        } else {
            Shadow.None
        }
    }

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .pointerInput(Unit) {
                detectTapGestures(
                    onLongPress = {
                        isDesktopBackgroundMenuOpen = true
                    }
                )
            }
    ) {
        val containerWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
        val containerHeightPx = constraints.maxHeight.toFloat().coerceAtLeast(1f)

        val density = LocalDensity.current
        val cellWidthPx = with(density) { (effectiveIconSize + 36).dp.toPx() }
        val cellHeightPx = with(density) { (effectiveIconSize + 52).dp.toPx() }

        // --- 1. RENDER CÁC WIDGET MÀN HÌNH CHÍNH (FEATURE 18) ---
        widgets.forEach { widgetItem ->
            DesktopWidgetContainer(
                widgetItem = widgetItem,
                containerWidthPx = containerWidthPx,
                containerHeightPx = containerHeightPx,
                appRepository = appRepository,
                onLaunchApp = onLaunchApp
            )
        }

        // --- 2. RENDER THƯ MỤC VÀ BIỂU TƯỢNG TRONG CHẾ ĐỘ FREEFORM HOẶC GRID ---
        if (currentLayoutMode == "freeform") {
            // Hiển thị Thư mục (Freeform)
            folders.forEach { folder ->
                val isHovered = hoverTargetFolderId == folder.id
                val folderW = if (folder.isLarge) (effectiveIconSize * 2 + 36).dp else (effectiveIconSize + 24).dp
                val folderH = if (folder.isLarge) (effectiveIconSize * 2 + 56).dp else (effectiveIconSize + 36).dp

                Box(
                    modifier = Modifier
                        .offset {
                            IntOffset(
                                (folder.xPercent * containerWidthPx).toInt(),
                                (folder.yPercent * containerHeightPx).toInt()
                            )
                        }
                        .onGloballyPositioned { coords ->
                            folderBounds[folder.id] = coords.boundsInParent()
                        }
                        .pointerInput(folder.id) {
                            detectDragGestures(
                                onDrag = { change, dragAmount ->
                                    change.consume()
                                    val newX = (folder.xPercent + dragAmount.x / containerWidthPx).coerceIn(0.02f, 0.90f)
                                    val newY = (folder.yPercent + dragAmount.y / containerHeightPx).coerceIn(0.02f, 0.86f)
                                    appRepository.updateFolderPosition(folder.id, newX, newY)
                                }
                            )
                        }
                        .pointerInput(folder.id) {
                            detectTapGestures(
                                onLongPress = { selectedFolderForMenu = folder },
                                onTap = { selectedFolderForDetail = folder }
                            )
                        }
                        .width(folderW)
                        .height(folderH)
                        .zIndex(2f)
                ) {
                    DesktopFolderItem(
                        folder = folder,
                        appRepository = appRepository,
                        isHovered = isHovered,
                        effectiveIconSize = effectiveIconSize,
                        iconCornerRadius = iconCornerRadius,
                        showLabels = showLabels,
                        textShadow = textShadow,
                        onAppClick = { onLaunchApp(it) },
                        onFolderClick = { selectedFolderForDetail = folder },
                        onToggleSize = { appRepository.toggleFolderSize(folder.id) }
                    )
                }
            }

            // Hiển thị Ứng dụng Desktop tự do
            localApps.forEachIndexed { index, app ->
                val isBeingDragged = draggedItemPackage == app.packageName
                val isHoverTarget = hoverTargetAppPkg == app.packageName
                val iconBitmap = remember(app.packageName) {
                    appRepository.getAppIconBitmap(app.packageName)
                }

                val defaultCols = (containerWidthPx / cellWidthPx).toInt().coerceAtLeast(1)
                val defaultX = ((index % defaultCols) * cellWidthPx + 20f) / containerWidthPx
                val defaultY = ((index / defaultCols) * cellHeightPx + 20f) / containerHeightPx

                val pos = freeformPositions[app.packageName] ?: Pair(defaultX.coerceIn(0.02f, 0.9f), defaultY.coerceIn(0.02f, 0.85f))

                Box(
                    modifier = Modifier
                        .offset {
                            val basePxX = (pos.first * containerWidthPx).toInt()
                            val basePxY = (pos.second * containerHeightPx).toInt()
                            if (isBeingDragged) {
                                IntOffset(
                                    (basePxX + dragOffset.x).toInt(),
                                    (basePxY + dragOffset.y).toInt()
                                )
                            } else {
                                IntOffset(basePxX, basePxY)
                            }
                        }
                        .onGloballyPositioned { coords ->
                            itemBounds[index] = coords.boundsInParent()
                        }
                        .zIndex(if (isBeingDragged) 20f else 1f)
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

                                    // Phát hiện va chạm với app khác để tạo thư mục (Feature 16)
                                    val currentSlot = itemBounds[draggedItemIndex]
                                    if (currentSlot != null) {
                                        val fingerCenter = currentSlot.center + dragOffset
                                        var targetApp: String? = null
                                        for ((idx, rect) in itemBounds) {
                                            if (idx != draggedItemIndex && rect.contains(fingerCenter) && idx in localApps.indices) {
                                                targetApp = localApps[idx].packageName
                                                break
                                            }
                                        }
                                        hoverTargetAppPkg = targetApp

                                        // Kiểm tra va chạm với Thư mục hiện có
                                        var targetFolder: String? = null
                                        for ((fId, rect) in folderBounds) {
                                            if (rect.contains(fingerCenter)) {
                                                targetFolder = fId
                                                break
                                            }
                                        }
                                        hoverTargetFolderId = targetFolder
                                    }
                                },
                                onDragEnd = {
                                    if (totalDragDistance < 20f) {
                                        selectedAppForMenu = app
                                    } else if (hoverTargetFolderId != null) {
                                        // Thả vào thư mục hiện có
                                        appRepository.addAppToFolder(hoverTargetFolderId!!, app.packageName)
                                        scope.launch { appRepository.unpinFromDesktop(app.packageName) }
                                    } else if (hoverTargetAppPkg != null) {
                                        // Kéo app đè lên app -> Tạo thư mục mới (Feature 16)
                                        val dropPos = freeformPositions[hoverTargetAppPkg!!] ?: Pair(0.2f, 0.2f)
                                        appRepository.createFolder(
                                            app1 = hoverTargetAppPkg!!,
                                            app2 = app.packageName,
                                            x = dropPos.first,
                                            y = dropPos.second
                                        )
                                    } else {
                                        val curPos = freeformPositions[app.packageName] ?: Pair(defaultX, defaultY)
                                        val newX = (curPos.first + dragOffset.x / containerWidthPx).coerceIn(0.02f, 0.92f)
                                        val newY = (curPos.second + dragOffset.y / containerHeightPx).coerceIn(0.02f, 0.88f)
                                        appRepository.saveDesktopAppPosition(app.packageName, newX, newY)
                                        freeformPositions = appRepository.getDesktopAppPositions()
                                    }
                                    draggedItemPackage = null
                                    draggedItemIndex = -1
                                    dragOffset = Offset.Zero
                                    totalDragDistance = 0f
                                    hoverTargetAppPkg = null
                                    hoverTargetFolderId = null
                                },
                                onDragCancel = {
                                    draggedItemPackage = null
                                    draggedItemIndex = -1
                                    dragOffset = Offset.Zero
                                    totalDragDistance = 0f
                                    hoverTargetAppPkg = null
                                    hoverTargetFolderId = null
                                }
                            )
                        }
                        .clickable {
                            if (draggedItemPackage == null) {
                                onLaunchApp(app.packageName)
                            }
                        }
                        .graphicsLayer {
                            if (isBeingDragged) {
                                scaleX = 1.15f
                                scaleY = 1.15f
                                shadowElevation = 18.dp.toPx()
                                alpha = 0.95f
                            }
                        }
                        .padding(6.dp)
                ) {
                    DesktopAppIcon(
                        app = app,
                        iconBitmap = iconBitmap,
                        effectiveIconSize = effectiveIconSize,
                        effectiveLabelSize = effectiveLabelSize,
                        iconCornerRadius = iconCornerRadius,
                        showLabels = showLabels,
                        textShadow = textShadow,
                        isHoverTarget = isHoverTarget
                    )
                }
            }
        } else {
            // Chế độ bố cục lưới ô (Grid Mode)
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = (effectiveIconSize + 36).dp),
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Hiển thị các thư mục ở đầu lưới
                items(folders.size, key = { "folder_${folders[it].id}" }) { fIndex ->
                    val folder = folders[fIndex]
                    val isHovered = hoverTargetFolderId == folder.id
                    Box(
                        modifier = Modifier
                            .onGloballyPositioned { coords ->
                                folderBounds[folder.id] = coords.boundsInParent()
                            }
                            .pointerInput(folder.id) {
                                detectTapGestures(
                                    onLongPress = { selectedFolderForMenu = folder },
                                    onTap = { selectedFolderForDetail = folder }
                                )
                            }
                    ) {
                        DesktopFolderItem(
                            folder = folder,
                            appRepository = appRepository,
                            isHovered = isHovered,
                            effectiveIconSize = effectiveIconSize,
                            iconCornerRadius = iconCornerRadius,
                            showLabels = showLabels,
                            textShadow = textShadow,
                            onAppClick = { onLaunchApp(it) },
                            onFolderClick = { selectedFolderForDetail = folder },
                            onToggleSize = { appRepository.toggleFolderSize(folder.id) }
                        )
                    }
                }

                // Hiển thị các app độc lập
                itemsIndexed(localApps, key = { _, app -> app.packageName }) { index, app ->
                    val isBeingDragged = draggedItemPackage == app.packageName
                    val isHoverTarget = hoverTargetAppPkg == app.packageName
                    val iconBitmap = remember(app.packageName) {
                        appRepository.getAppIconBitmap(app.packageName)
                    }

                    Column(
                        modifier = Modifier
                            .width((effectiveIconSize + 32).dp)
                            .onGloballyPositioned { coords ->
                                itemBounds[index] = coords.boundsInParent()
                            }
                            .zIndex(if (isBeingDragged) 20f else 1f)
                            .graphicsLayer {
                                if (isBeingDragged) {
                                    translationX = dragOffset.x
                                    translationY = dragOffset.y
                                    scaleX = 1.15f
                                    scaleY = 1.15f
                                    alpha = 0.95f
                                    shadowElevation = 18.dp.toPx()
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

                                                // Phát hiện kéo đè lên thư mục
                                                var targetFolder: String? = null
                                                for ((fId, rect) in folderBounds) {
                                                    if (rect.contains(currentCenter)) {
                                                        targetFolder = fId
                                                        break
                                                    }
                                                }
                                                hoverTargetFolderId = targetFolder

                                                // Phát hiện kéo đè lên app khác để tạo thư mục
                                                var targetSlotIndex = -1
                                                for ((slotIdx, rect) in itemBounds) {
                                                    if (slotIdx != draggedItemIndex && rect.contains(currentCenter)) {
                                                        targetSlotIndex = slotIdx
                                                        break
                                                    }
                                                }
                                                if (targetSlotIndex != -1 && targetSlotIndex in localApps.indices) {
                                                    hoverTargetAppPkg = localApps[targetSlotIndex].packageName
                                                } else {
                                                    hoverTargetAppPkg = null
                                                }
                                            }
                                        }
                                    },
                                    onDragEnd = {
                                        if (totalDragDistance < 20f) {
                                            selectedAppForMenu = app
                                        } else if (hoverTargetFolderId != null) {
                                            appRepository.addAppToFolder(hoverTargetFolderId!!, app.packageName)
                                            scope.launch { appRepository.unpinFromDesktop(app.packageName) }
                                        } else if (hoverTargetAppPkg != null) {
                                            // Tạo thư mục mới khi kéo đè app
                                            appRepository.createFolder(
                                                app1 = hoverTargetAppPkg!!,
                                                app2 = app.packageName,
                                                x = 0.3f,
                                                y = 0.3f
                                            )
                                        } else {
                                            scope.launch {
                                                appRepository.saveDesktopAppOrder(localApps.map { it.packageName })
                                            }
                                        }
                                        draggedItemPackage = null
                                        draggedItemIndex = -1
                                        dragOffset = Offset.Zero
                                        totalDragDistance = 0f
                                        hoverTargetAppPkg = null
                                        hoverTargetFolderId = null
                                    },
                                    onDragCancel = {
                                        draggedItemPackage = null
                                        draggedItemIndex = -1
                                        dragOffset = Offset.Zero
                                        totalDragDistance = 0f
                                        hoverTargetAppPkg = null
                                        hoverTargetFolderId = null
                                    }
                                )
                            }
                            .clickable {
                                if (draggedItemPackage == null) {
                                    onLaunchApp(app.packageName)
                                }
                            }
                            .padding(6.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        DesktopAppIcon(
                            app = app,
                            iconBitmap = iconBitmap,
                            effectiveIconSize = effectiveIconSize,
                            effectiveLabelSize = effectiveLabelSize,
                            iconCornerRadius = iconCornerRadius,
                            showLabels = showLabels,
                            textShadow = textShadow,
                            isHoverTarget = isHoverTarget
                        )
                    }
                }
            }
        }
    }

    // --- DIALOG CHI TIẾT THƯ MỤC KHI MỞ ---
    if (selectedFolderForDetail != null) {
        val folder = selectedFolderForDetail!!
        FolderDetailDialog(
            folder = folder,
            appRepository = appRepository,
            onDismiss = { selectedFolderForDetail = null },
            onLaunchApp = {
                selectedFolderForDetail = null
                onLaunchApp(it)
            }
        )
    }

    // --- CONTEXT MENU CHO THƯ MỤC (ONE UI 8 LARGE FOLDER MENU) ---
    if (selectedFolderForMenu != null) {
        val folder = selectedFolderForMenu!!
        var isRenaming by remember { mutableStateOf(false) }
        var tempName by remember { mutableStateOf(folder.name) }

        AlertDialog(
            onDismissRequest = { selectedFolderForMenu = null },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Folder, contentDescription = null, tint = Color(0xFF00E5FF))
                    Text(text = folder.name, fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (isRenaming) {
                        OutlinedTextField(
                            value = tempName,
                            onValueChange = { tempName = it },
                            label = { Text("Tên thư mục") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                appRepository.renameFolder(folder.id, tempName)
                                isRenaming = false
                                selectedFolderForMenu = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Lưu tên mới")
                        }
                    } else {
                        TextButton(
                            onClick = {
                                appRepository.toggleFolderSize(folder.id)
                                selectedFolderForMenu = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                if (folder.isLarge) "Thu nhỏ thành Thư mục chuẩn (1x1)" else "Phóng to thành Thư mục lớn (2x2)",
                                modifier = Modifier.fillMaxWidth()
                            )
                        }

                        TextButton(
                            onClick = { isRenaming = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Đổi tên thư mục", modifier = Modifier.fillMaxWidth())
                        }

                        TextButton(
                            onClick = {
                                // Giải tán thư mục: đưa các app trở lại desktop
                                scope.launch {
                                    folder.packageNames.forEach { pkg ->
                                        appRepository.pinToDesktop(pkg)
                                    }
                                    appRepository.dissolveFolder(folder.id)
                                }
                                selectedFolderForMenu = null
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Giải tán thư mục (Đưa ứng dụng ra ngoài)", color = MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth())
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedFolderForMenu = null }) {
                    Text("Đóng")
                }
            }
        )
    }

    // --- CONTEXT MENU KHI NHẤN GIỮ ICON DESKTOP ---
    if (selectedAppForMenu != null) {
        val app = selectedAppForMenu!!
        val isPinnedTaskbar = pinnedTaskbarApps.any { it.packageName == app.packageName }

        AlertDialog(
            onDismissRequest = { selectedAppForMenu = null },
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
                            scope.launch { appRepository.unpinFromDesktop(app.packageName) }
                            selectedAppForMenu = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Bỏ ghim khỏi Desktop", modifier = Modifier.fillMaxWidth(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    TextButton(
                        onClick = {
                            scope.launch {
                                if (isPinnedTaskbar) appRepository.unpinFromTaskbar(app.packageName)
                                else appRepository.pinToTaskbar(app.packageName)
                            }
                            selectedAppForMenu = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (isPinnedTaskbar) "Bỏ ghim khỏi Taskbar" else "Ghim vào Taskbar",
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    TextButton(
                        onClick = {
                            currentLayoutMode = if (currentLayoutMode == "freeform") "grid" else "freeform"
                            selectedAppForMenu = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            if (currentLayoutMode == "freeform") "Chuyển sang Bố cục Lưới ô (Grid)" else "Chuyển sang Bố cục Tự do (Freeform)",
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    TextButton(
                        onClick = {
                            try {
                                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                                    data = Uri.fromParts("package", app.packageName, null)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                            selectedAppForMenu = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Thông tin ứng dụng", modifier = Modifier.fillMaxWidth(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }

                    TextButton(
                        onClick = {
                            try {
                                val intent = Intent(Intent.ACTION_DELETE).apply {
                                    data = Uri.fromParts("package", app.packageName, null)
                                }
                                context.startActivity(intent)
                            } catch (_: Exception) {}
                            selectedAppForMenu = null
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Gỡ cài đặt", color = MaterialTheme.colorScheme.error, modifier = Modifier.fillMaxWidth(), maxLines = 1, overflow = TextOverflow.Ellipsis)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { selectedAppForMenu = null }) {
                    Text("Đóng")
                }
            }
        )
    }

    // --- MENU TÙY CHỌN NỀN DESKTOP (LONG PRESS DESKTOP) ---
    if (isDesktopBackgroundMenuOpen) {
        AlertDialog(
            onDismissRequest = { isDesktopBackgroundMenuOpen = false },
            title = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(Icons.Default.Dashboard, contentDescription = null, tint = Color(0xFF00E5FF))
                    Text("Tùy chọn Màn hình chính", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Button(
                        onClick = {
                            isDesktopBackgroundMenuOpen = false
                            isAddWidgetDialogOpen = true
                        },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2979FF))
                    ) {
                        Icon(Icons.Default.Widgets, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("➕ Thêm Widget vào Desktop", fontWeight = FontWeight.SemiBold)
                    }

                    Text("Chế độ sắp xếp biểu tượng hiện tại:", color = Color.LightGray, fontSize = 13.sp)

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = currentLayoutMode == "grid",
                            onClick = {
                                currentLayoutMode = "grid"
                                isDesktopBackgroundMenuOpen = false
                            },
                            label = { Text("Lưới chia ô (Grid)", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                        FilterChip(
                            selected = currentLayoutMode == "freeform",
                            onClick = {
                                currentLayoutMode = "freeform"
                                isDesktopBackgroundMenuOpen = false
                            },
                            label = { Text("Tự do (Freeform)", fontSize = 12.sp) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    if (currentLayoutMode == "freeform") {
                        Button(
                            onClick = {
                                freeformPositions = emptyMap()
                                localApps.forEachIndexed { i, app ->
                                    val cols = 6
                                    val defaultX = ((i % cols) * 0.14f + 0.04f).coerceIn(0.04f, 0.90f)
                                    val defaultY = ((i / cols) * 0.20f + 0.06f).coerceIn(0.06f, 0.86f)
                                    appRepository.saveDesktopAppPosition(app.packageName, defaultX, defaultY)
                                }
                                freeformPositions = appRepository.getDesktopAppPositions()
                                isDesktopBackgroundMenuOpen = false
                            },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BFA5))
                        ) {
                            Icon(Icons.Default.GridView, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Tự động gióng đều hàng & cột", fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { isDesktopBackgroundMenuOpen = false }) {
                    Text("Đóng")
                }
            }
        )
    }

    // --- DIALOG CHỌN WIDGET ĐỂ THÊM VÀO DESKTOP (FEATURE 18) ---
    if (isAddWidgetDialogOpen) {
        AlertDialog(
            onDismissRequest = { isAddWidgetDialogOpen = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Widgets, contentDescription = null, tint = Color(0xFF00E5FF))
                    Text("Chọn Widget Desktop để thêm", fontWeight = FontWeight.Bold)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    DesktopWidgetType.entries.forEach { type ->
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    appRepository.addWidget(type)
                                    isAddWidgetDialogOpen = false
                                },
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF242424))
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                val icon = when (type) {
                                    DesktopWidgetType.CLOCK_CALENDAR -> Icons.Default.AccessTime
                                    DesktopWidgetType.WEATHER -> Icons.Default.WbSunny
                                    DesktopWidgetType.SYSTEM_STATS -> Icons.Default.Memory
                                    DesktopWidgetType.STICKY_NOTE -> Icons.Default.StickyNote2
                                    DesktopWidgetType.MEDIA_PLAYER -> Icons.Default.PlayCircle
                                    DesktopWidgetType.SEARCH -> Icons.Default.Search
                                }
                                Icon(icon, contentDescription = null, tint = Color(0xFF00E5FF), modifier = Modifier.size(24.dp))
                                Column {
                                    Text(type.title, color = Color.White, fontWeight = FontWeight.SemiBold)
                                    Text(
                                        when (type) {
                                            DesktopWidgetType.CLOCK_CALENDAR -> "Đồng hồ số và thông tin ngày tháng"
                                            DesktopWidgetType.WEATHER -> "Nhiệt độ và dự báo thời tiết"
                                            DesktopWidgetType.SYSTEM_STATS -> "Mức sử dụng RAM, Pin và Bộ nhớ"
                                            DesktopWidgetType.STICKY_NOTE -> "Ghi chú nhanh có thể gõ chữ trực tiếp"
                                            DesktopWidgetType.MEDIA_PLAYER -> "Điều khiển nhạc và đa phương tiện"
                                            DesktopWidgetType.SEARCH -> "Thanh tìm kiếm nhanh giao diện DeX"
                                        },
                                        color = Color.Gray,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { isAddWidgetDialogOpen = false }) {
                    Text("Hủy")
                }
            }
        )
    }
}

// --- COMPOSABLE VẼ ICON APP ---
@Composable
fun DesktopAppIcon(
    app: AppInfo,
    iconBitmap: androidx.compose.ui.graphics.ImageBitmap?,
    effectiveIconSize: Int,
    effectiveLabelSize: Int,
    iconCornerRadius: Int,
    showLabels: Boolean,
    textShadow: Shadow,
    isHoverTarget: Boolean
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width((effectiveIconSize + 24).dp)
            .let {
                if (isHoverTarget) it.border(2.dp, Color(0xFF00FF88), RoundedCornerShape(iconCornerRadius.dp))
                else it
            }
    ) {
        if (iconBitmap != null) {
            Image(
                bitmap = iconBitmap,
                contentDescription = app.label,
                modifier = Modifier.size(effectiveIconSize.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(effectiveIconSize.dp)
                    .background(Color.Gray, RoundedCornerShape(iconCornerRadius.dp))
            )
        }

        if (showLabels) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = app.label,
                color = Color.White,
                fontSize = effectiveLabelSize.sp,
                maxLines = 2,
                textAlign = TextAlign.Center,
                style = androidx.compose.ui.text.TextStyle(shadow = textShadow),
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

// --- COMPOSABLE HIỂN THỊ THƯ MỤC (STANDARD HOẶC LARGE FOLDER) ---
@Composable
fun DesktopFolderItem(
    folder: DesktopFolder,
    appRepository: AppRepository,
    isHovered: Boolean,
    effectiveIconSize: Int,
    iconCornerRadius: Int,
    showLabels: Boolean,
    textShadow: Shadow,
    onAppClick: (String) -> Unit,
    onFolderClick: () -> Unit,
    onToggleSize: () -> Unit
) {
    if (folder.isLarge) {
        // ONE UI 8 LARGE FOLDER (2x2): Cho phép bấm trực tiếp vào 4 app bên trong từ màn hình chính!
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .border(
                    if (isHovered) 2.dp else 1.dp,
                    if (isHovered) Color(0xFF00FF88) else Color.White.copy(alpha = 0.25f),
                    RoundedCornerShape(20.dp)
                ),
            shape = RoundedCornerShape(20.dp),
            color = Color(0x661A1A1A),
            shadowElevation = 8.dp
        ) {
            Column(modifier = Modifier.padding(10.dp)) {
                // Header thư mục lớn
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = folder.name,
                        color = Color.White,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).clickable { onFolderClick() }
                    )
                    IconButton(
                        onClick = onFolderClick,
                        modifier = Modifier.size(20.dp)
                    ) {
                        Icon(Icons.Default.OpenInNew, contentDescription = "Mở rộng", tint = Color.LightGray, modifier = Modifier.size(14.dp))
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                // 2x2 Grid các icon bấm trực tiếp
                val displayPkgs = folder.packageNames.take(4)
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.SpaceEvenly
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        displayPkgs.getOrNull(0)?.let { pkg ->
                            MiniLargeFolderApp(pkg, appRepository, effectiveIconSize, onAppClick)
                        }
                        displayPkgs.getOrNull(1)?.let { pkg ->
                            MiniLargeFolderApp(pkg, appRepository, effectiveIconSize, onAppClick)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        displayPkgs.getOrNull(2)?.let { pkg ->
                            MiniLargeFolderApp(pkg, appRepository, effectiveIconSize, onAppClick)
                        }
                        displayPkgs.getOrNull(3)?.let { pkg ->
                            MiniLargeFolderApp(pkg, appRepository, effectiveIconSize, onAppClick)
                        }
                    }
                }
            }
        }
    } else {
        // STANDARD FOLDER (1x1): Biểu tượng gọn với 4 mini app preview
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.clickable { onFolderClick() }
        ) {
            Box(
                modifier = Modifier
                    .size(effectiveIconSize.dp)
                    .clip(RoundedCornerShape(iconCornerRadius.dp))
                    .background(Color(0x55282828))
                    .border(
                        if (isHovered) 2.dp else 1.dp,
                        if (isHovered) Color(0xFF00FF88) else Color.White.copy(alpha = 0.2f),
                        RoundedCornerShape(iconCornerRadius.dp)
                    )
                    .padding(4.dp),
                contentAlignment = Alignment.Center
            ) {
                // Mini 2x2 icon preview
                val previewPkgs = folder.packageNames.take(4)
                Column(
                    verticalArrangement = Arrangement.SpaceEvenly,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.fillMaxSize()
                ) {
                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        previewPkgs.getOrNull(0)?.let { pkg ->
                            val bmp = appRepository.getAppIconBitmap(pkg)
                            if (bmp != null) Image(bitmap = bmp, contentDescription = null, modifier = Modifier.size((effectiveIconSize * 0.38f).dp))
                        }
                        previewPkgs.getOrNull(1)?.let { pkg ->
                            val bmp = appRepository.getAppIconBitmap(pkg)
                            if (bmp != null) Image(bitmap = bmp, contentDescription = null, modifier = Modifier.size((effectiveIconSize * 0.38f).dp))
                        }
                    }
                    Row(horizontalArrangement = Arrangement.SpaceEvenly, modifier = Modifier.fillMaxWidth()) {
                        previewPkgs.getOrNull(2)?.let { pkg ->
                            val bmp = appRepository.getAppIconBitmap(pkg)
                            if (bmp != null) Image(bitmap = bmp, contentDescription = null, modifier = Modifier.size((effectiveIconSize * 0.38f).dp))
                        }
                        previewPkgs.getOrNull(3)?.let { pkg ->
                            val bmp = appRepository.getAppIconBitmap(pkg)
                            if (bmp != null) Image(bitmap = bmp, contentDescription = null, modifier = Modifier.size((effectiveIconSize * 0.38f).dp))
                        }
                    }
                }
            }

            if (showLabels) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = folder.name,
                    color = Color.White,
                    fontSize = 11.sp,
                    maxLines = 1,
                    textAlign = TextAlign.Center,
                    style = androidx.compose.ui.text.TextStyle(shadow = textShadow),
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun MiniLargeFolderApp(
    packageName: String,
    appRepository: AppRepository,
    effectiveIconSize: Int,
    onClick: (String) -> Unit
) {
    val bitmap = remember(packageName) { appRepository.getAppIconBitmap(packageName) }
    Box(
        modifier = Modifier
            .size((effectiveIconSize * 0.72f).dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable { onClick(packageName) },
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(bitmap = bitmap, contentDescription = null, modifier = Modifier.fillMaxSize())
        } else {
            Box(modifier = Modifier.fillMaxSize().background(Color.Gray, RoundedCornerShape(10.dp)))
        }
    }
}

// --- DIALOG XEM TOÀN BỘ ỨNG DỤNG TRONG THƯ MỤC ---
@Composable
fun FolderDetailDialog(
    folder: DesktopFolder,
    appRepository: AppRepository,
    onDismiss: () -> Unit,
    onLaunchApp: (String) -> Unit
) {
    val installedApps by appRepository.installedApps.collectAsState()
    val folderApps = remember(folder.packageNames, installedApps) {
        folder.packageNames.mapNotNull { pkg -> installedApps.find { it.packageName == pkg } }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Color(0xFF00E5FF))
                    Text(text = folder.name, fontWeight = FontWeight.Bold, color = Color.White)
                }
                IconButton(onClick = { appRepository.toggleFolderSize(folder.id) }) {
                    Icon(
                        if (folder.isLarge) Icons.Default.CloseFullscreen else Icons.Default.OpenInFull,
                        contentDescription = "Chuyển kích thước thư mục",
                        tint = Color.LightGray
                    )
                }
            }
        },
        text = {
            Box(modifier = Modifier.width(360.dp).heightIn(max = 400.dp)) {
                if (folderApps.isEmpty()) {
                    Text("Thư mục trống", color = Color.Gray, modifier = Modifier.align(Alignment.Center))
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(3),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(folderApps.size, key = { folderApps[it].packageName }) { idx ->
                            val app = folderApps[idx]
                            val bmp = remember(app.packageName) { appRepository.getAppIconBitmap(app.packageName) }
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onLaunchApp(app.packageName) }
                                    .padding(6.dp)
                            ) {
                                if (bmp != null) {
                                    Image(bitmap = bmp, contentDescription = app.label, modifier = Modifier.size(46.dp))
                                } else {
                                    Box(modifier = Modifier.size(46.dp).background(Color.Gray, RoundedCornerShape(8.dp)))
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
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Đóng")
            }
        }
    )
}

// --- CONTAINER WIDGET MÀN HÌNH CHÍNH (FEATURE 18: RESIZABLE WIDGETS) ---
@Composable
fun DesktopWidgetContainer(
    widgetItem: DesktopWidgetItem,
    containerWidthPx: Float,
    containerHeightPx: Float,
    appRepository: AppRepository,
    onLaunchApp: (String) -> Unit
) {
    var curWidthDp by remember(widgetItem.widthDp) { mutableIntStateOf(widgetItem.widthDp) }
    var curHeightDp by remember(widgetItem.heightDp) { mutableIntStateOf(widgetItem.heightDp) }
    var curXPct by remember(widgetItem.xPercent) { mutableFloatStateOf(widgetItem.xPercent) }
    var curYPct by remember(widgetItem.yPercent) { mutableFloatStateOf(widgetItem.yPercent) }

    val density = LocalDensity.current

    Box(
        modifier = Modifier
            .offset {
                IntOffset(
                    (curXPct * containerWidthPx).toInt(),
                    (curYPct * containerHeightPx).toInt()
                )
            }
            .width(curWidthDp.dp)
            .height(curHeightDp.dp)
            .zIndex(1.5f)
            .shadow(12.dp, RoundedCornerShape(16.dp))
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xDD1E1E1E))
            .border(1.dp, Color(0x44FFFFFF), RoundedCornerShape(16.dp))
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Thanh tiêu đề Widget (kéo để di chuyển)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(30.dp)
                    .background(Color(0x33000000))
                    .pointerInput(widgetItem.id) {
                        detectDragGestures(
                            onDrag = { change, dragAmount ->
                                change.consume()
                                curXPct = (curXPct + dragAmount.x / containerWidthPx).coerceIn(0.01f, 0.95f)
                                curYPct = (curYPct + dragAmount.y / containerHeightPx).coerceIn(0.01f, 0.92f)
                            },
                            onDragEnd = {
                                appRepository.updateWidgetBounds(widgetItem.id, curXPct, curYPct, curWidthDp, curHeightDp)
                            }
                        )
                    }
                    .padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(
                        Icons.Default.DragIndicator,
                        contentDescription = "Di chuyển",
                        tint = Color.Gray,
                        modifier = Modifier.size(14.dp)
                    )
                    Text(
                        text = widgetItem.type.title,
                        color = Color.LightGray,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                IconButton(
                    onClick = { appRepository.removeWidget(widgetItem.id) },
                    modifier = Modifier.size(20.dp)
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Xóa", tint = Color.Gray, modifier = Modifier.size(12.dp))
                }
            }

            // Thân widget theo từng loại
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                when (widgetItem.type) {
                    DesktopWidgetType.CLOCK_CALENDAR -> ClockCalendarWidgetContent()
                    DesktopWidgetType.WEATHER -> WeatherWidgetContent()
                    DesktopWidgetType.SYSTEM_STATS -> SystemStatsWidgetContent()
                    DesktopWidgetType.STICKY_NOTE -> StickyNoteWidgetContent(appRepository)
                    DesktopWidgetType.MEDIA_PLAYER -> MediaPlayerWidgetContent()
                    DesktopWidgetType.SEARCH -> QuickSearchWidgetContent(onLaunchApp)
                }
            }
        }

        // Nút kéo góc dưới-phải để thay đổi kích thước widget tự do (Resizable Handle)
        Box(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .size(24.dp)
                .pointerInput(widgetItem.id) {
                    detectDragGestures(
                        onDrag = { change, dragAmount ->
                            change.consume()
                            with(density) {
                                val deltaW = dragAmount.x.toDp().value.toInt()
                                val deltaH = dragAmount.y.toDp().value.toInt()
                                curWidthDp = (curWidthDp + deltaW).coerceIn(160, 600)
                                curHeightDp = (curHeightDp + deltaH).coerceIn(100, 450)
                            }
                        },
                        onDragEnd = {
                            appRepository.updateWidgetBounds(widgetItem.id, curXPct, curYPct, curWidthDp, curHeightDp)
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.OpenInFull,
                contentDescription = "Đổi kích thước",
                tint = Color.Gray.copy(alpha = 0.6f),
                modifier = Modifier.size(12.dp)
            )
        }
    }
}

// --- WIDGET CONTENT 1: ĐỒNG HỒ & LỊCH ---
@Composable
fun ClockCalendarWidgetContent() {
    var currentTime by remember { mutableStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            currentTime = System.currentTimeMillis()
            delay(1000L)
        }
    }
    val timeFormat = remember { SimpleDateFormat("HH:mm:ss", Locale.getDefault()) }
    val dateFormat = remember { SimpleDateFormat("EEEE, dd MMMM yyyy", Locale("vi", "VN")) }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = timeFormat.format(Date(currentTime)),
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = dateFormat.format(Date(currentTime)),
            fontSize = 12.sp,
            color = Color(0xFF00E5FF),
            fontWeight = FontWeight.Medium
        )
    }
}

// --- WIDGET CONTENT 2: THỜI TIẾT ---
@Composable
fun WeatherWidgetContent() {
    Row(
        modifier = Modifier.fillMaxSize().padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceAround
    ) {
        Icon(
            Icons.Default.WbSunny,
            contentDescription = null,
            tint = Color(0xFFFFB300),
            modifier = Modifier.size(46.dp)
        )
        Column {
            Text("29°C", fontSize = 28.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Text("Nắng nhẹ • TP. Hồ Chí Minh", fontSize = 11.sp, color = Color.LightGray)
            Text("Độ ẩm: 68% • Gió: 12 km/h", fontSize = 10.sp, color = Color.Gray)
        }
    }
}

// --- WIDGET CONTENT 3: TÀI NGUYÊN HỆ THỐNG ---
@Composable
fun SystemStatsWidgetContent() {
    val context = LocalContext.current
    var memPct by remember { mutableFloatStateOf(0.45f) }
    var storagePct by remember { mutableFloatStateOf(0.60f) }
    var batteryPct by remember { mutableIntStateOf(85) }

    LaunchedEffect(Unit) {
        try {
            val actManager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val memInfo = ActivityManager.MemoryInfo()
            actManager.getMemoryInfo(memInfo)
            memPct = 1f - (memInfo.availMem.toFloat() / memInfo.totalMem.toFloat())

            val stat = StatFs(Environment.getDataDirectory().path)
            val bytesAvailable = stat.availableBlocksLong * stat.blockSizeLong
            val bytesTotal = stat.blockCountLong * stat.blockSizeLong
            storagePct = 1f - (bytesAvailable.toFloat() / bytesTotal.toFloat())

            val bm = context.getSystemService(Context.BATTERY_SERVICE) as BatteryManager
            batteryPct = bm.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY).coerceIn(0, 100)
        } catch (_: Exception) {}
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.SpaceEvenly
    ) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("RAM: ${(memPct * 100).toInt()}%", fontSize = 11.sp, color = Color.White)
            Text("Bộ nhớ: ${(storagePct * 100).toInt()}%", fontSize = 11.sp, color = Color.LightGray)
            Text("Pin: $batteryPct%", fontSize = 11.sp, color = Color(0xFF00FF88))
        }

        LinearProgressIndicator(
            progress = { memPct },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = Color(0xFF2979FF),
            trackColor = Color(0x33FFFFFF)
        )

        LinearProgressIndicator(
            progress = { storagePct },
            modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
            color = Color(0xFFFF9100),
            trackColor = Color(0x33FFFFFF)
        )
    }
}

// --- WIDGET CONTENT 4: GHI CHÚ NHANH (STICKY NOTE) ---
@Composable
fun StickyNoteWidgetContent(appRepository: AppRepository) {
    var noteText by remember { mutableStateOf(appRepository.getStickyNoteText()) }
    val scope = rememberCoroutineScope()

    TextField(
        value = noteText,
        onValueChange = {
            noteText = it
            scope.launch { appRepository.saveStickyNoteText(it) }
        },
        modifier = Modifier.fillMaxSize(),
        placeholder = { Text("Nhập ghi chú tại đây...", fontSize = 12.sp, color = Color.Gray) },
        colors = TextFieldDefaults.colors(
            focusedContainerColor = Color.Transparent,
            unfocusedContainerColor = Color.Transparent,
            focusedTextColor = Color(0xFFFFF9C4),
            unfocusedTextColor = Color(0xFFFFF9C4),
            focusedIndicatorColor = Color.Transparent,
            unfocusedIndicatorColor = Color.Transparent
        ),
        textStyle = androidx.compose.ui.text.TextStyle(fontSize = 12.sp)
    )
}

// --- WIDGET CONTENT 5: TRÌNH PHÁT MEDIA ---
@Composable
fun MediaPlayerWidgetContent() {
    var isPlaying by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier.fillMaxSize(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Box(
            modifier = Modifier
                .size(54.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Brush.linearGradient(listOf(Color(0xFF8E2DE2), Color(0xFF4A00E0)))),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Default.MusicNote, contentDescription = null, tint = Color.White, modifier = Modifier.size(28.dp))
        }

        Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
            Text("Samsung Music", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 12.sp, maxLines = 1)
            Text("Galaxy DeX Ambient Lounge", color = Color.Gray, fontSize = 10.sp, maxLines = 1)
        }

        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = {}, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.SkipPrevious, contentDescription = "Trước", tint = Color.White, modifier = Modifier.size(18.dp))
            }
            IconButton(onClick = { isPlaying = !isPlaying }, modifier = Modifier.size(36.dp)) {
                Icon(
                    if (isPlaying) Icons.Default.PauseCircle else Icons.Default.PlayCircle,
                    contentDescription = "Phát/Tạm dừng",
                    tint = Color(0xFF00E5FF),
                    modifier = Modifier.size(30.dp)
                )
            }
            IconButton(onClick = {}, modifier = Modifier.size(28.dp)) {
                Icon(Icons.Default.SkipNext, contentDescription = "Sau", tint = Color.White, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// --- WIDGET CONTENT 6: THANH TÌM KIẾM NHANH ---
@Composable
fun QuickSearchWidgetContent(onLaunchApp: (String) -> Unit) {
    val context = LocalContext.current
    var query by remember { mutableStateOf("") }

    Row(
        modifier = Modifier
            .fillMaxSize()
            .clip(RoundedCornerShape(24.dp))
            .background(Color(0x44FFFFFF))
            .padding(horizontal = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Default.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        TextField(
            value = query,
            onValueChange = { query = it },
            placeholder = { Text("Tìm kiếm ứng dụng hoặc web...", fontSize = 12.sp, color = Color.LightGray) },
            singleLine = true,
            modifier = Modifier.weight(1f),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                focusedTextColor = Color.White,
                unfocusedTextColor = Color.White
            )
        )
        if (query.isNotBlank()) {
            IconButton(
                onClick = {
                    try {
                        val searchIntent = Intent(Intent.ACTION_WEB_SEARCH).apply {
                            putExtra(android.app.SearchManager.QUERY, query)
                        }
                        context.startActivity(searchIntent)
                    } catch (_: Exception) {}
                },
                modifier = Modifier.size(28.dp)
            ) {
                Icon(Icons.Default.ArrowForward, contentDescription = "Tìm", tint = Color(0xFF00E5FF), modifier = Modifier.size(18.dp))
            }
        }
    }
}
