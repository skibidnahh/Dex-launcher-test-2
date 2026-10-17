package com.example.dexlauncher.data.repository

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.content.pm.ResolveInfo
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.os.Build
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.example.dexlauncher.data.model.AppInfo
import com.example.dexlauncher.shizuku.ShizukuManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.util.UUID

data class DesktopFolder(
    val id: String,
    val name: String,
    val packageNames: List<String>,
    val isLarge: Boolean = false,
    val xPercent: Float = 0.1f,
    val yPercent: Float = 0.1f
)

enum class DesktopWidgetType(val title: String) {
    CLOCK_CALENDAR("Đồng hồ & Lịch"),
    WEATHER("Thời tiết"),
    SYSTEM_STATS("Tài nguyên Hệ thống"),
    STICKY_NOTE("Ghi chú Nhanh"),
    MEDIA_PLAYER("Trình phát Media"),
    SEARCH("Tìm kiếm Nhanh")
}

data class DesktopWidgetItem(
    val id: String,
    val type: DesktopWidgetType,
    val xPercent: Float,
    val yPercent: Float,
    val widthDp: Int,
    val heightDp: Int
)

class AppRepository(private val context: Context) {

    private val prefs = context.getSharedPreferences("dex_apps_pref", Context.MODE_PRIVATE)

    private val iconCache = LruCache<String, ImageBitmap>(120)

    private val _installedApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val installedApps: StateFlow<List<AppInfo>> = _installedApps.asStateFlow()

    private val _pinnedDesktopApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val pinnedDesktopApps: StateFlow<List<AppInfo>> = _pinnedDesktopApps.asStateFlow()

    private val _pinnedTaskbarApps = MutableStateFlow<List<AppInfo>>(emptyList())
    val pinnedTaskbarApps: StateFlow<List<AppInfo>> = _pinnedTaskbarApps.asStateFlow()

    private val _runningPackages = MutableStateFlow<Set<String>>(emptySet())
    val runningPackages: StateFlow<Set<String>> = _runningPackages.asStateFlow()

    private val _focusedPackage = MutableStateFlow<String?>(null)
    val focusedPackage: StateFlow<String?> = _focusedPackage.asStateFlow()

    private val _hiddenPackages = MutableStateFlow<Set<String>>(loadHiddenPackages())
    val hiddenPackages: StateFlow<Set<String>> = _hiddenPackages.asStateFlow()

    private val _folders = MutableStateFlow<List<DesktopFolder>>(loadFolders())
    val folders: StateFlow<List<DesktopFolder>> = _folders.asStateFlow()

    private val _widgets = MutableStateFlow<List<DesktopWidgetItem>>(loadWidgets())
    val widgets: StateFlow<List<DesktopWidgetItem>> = _widgets.asStateFlow()

    fun getAppIconBitmap(packageName: String): ImageBitmap? {
        iconCache.get(packageName)?.let { return it }
        try {
            val pm = context.packageManager
            val drawable = pm.getApplicationIcon(packageName)
            val bitmap = drawableToBitmap(drawable)
            val imageBitmap = bitmap.asImageBitmap()
            iconCache.put(packageName, imageBitmap)
            return imageBitmap
        } catch (e: Exception) {
            return null
        }
    }

    private fun drawableToBitmap(drawable: Drawable): Bitmap {
        if (drawable is android.graphics.drawable.BitmapDrawable && drawable.bitmap != null) {
            return drawable.bitmap
        }
        val width = if (drawable.intrinsicWidth > 0) drawable.intrinsicWidth else 96
        val height = if (drawable.intrinsicHeight > 0) drawable.intrinsicHeight else 96
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)
        return bitmap
    }

    suspend fun refreshApps() = withContext(Dispatchers.IO) {
        val pm = context.packageManager
        val intent = Intent(Intent.ACTION_MAIN, null).apply {
            addCategory(Intent.CATEGORY_LAUNCHER)
        }

        val resolveInfos: List<ResolveInfo> = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            pm.queryIntentActivities(intent, PackageManager.ResolveInfoFlags.of(0L))
        } else {
            @Suppress("DEPRECATION")
            pm.queryIntentActivities(intent, 0)
        }

        val desktopPinnedSet = getPinnedDesktopPackages()
        val taskbarPinnedSet = getPinnedTaskbarPackages()
        val runningSet = _runningPackages.value
        val hiddenSet = _hiddenPackages.value
        val myPackageName = context.packageName

        val apps = resolveInfos
            .filter { it.activityInfo.packageName != myPackageName }
            .map { resolveInfo ->
                val pkg = resolveInfo.activityInfo.packageName
                val label = resolveInfo.loadLabel(pm).toString()
                val activity = resolveInfo.activityInfo.name
                val drawable = resolveInfo.loadIcon(pm)
                try {
                    val bmp = drawableToBitmap(drawable)
                    iconCache.put(pkg, bmp.asImageBitmap())
                } catch (_: Exception) {}

                AppInfo(
                    label = label,
                    packageName = pkg,
                    activityName = activity,
                    isPinnedToDesktop = desktopPinnedSet.contains(pkg),
                    isPinnedToTaskbar = taskbarPinnedSet.contains(pkg),
                    isRunning = runningSet.contains(pkg)
                )
            }
            .sortedBy { it.label.lowercase() }

        _installedApps.value = apps

        val desktopOrder = getDesktopAppOrder()
        val pinned = apps.filter { it.isPinnedToDesktop && !hiddenSet.contains(it.packageName) }
        _pinnedDesktopApps.value = if (desktopOrder.isNotEmpty()) {
            val orderMap = desktopOrder.withIndex().associate { it.value to it.index }
            pinned.sortedBy { orderMap[it.packageName] ?: 999 }
        } else {
            pinned
        }

        _pinnedTaskbarApps.value = apps.filter { it.isPinnedToTaskbar && !hiddenSet.contains(it.packageName) }
    }

    private fun loadHiddenPackages(): Set<String> {
        return prefs.getStringSet("hidden_packages", emptySet()) ?: emptySet()
    }

    fun hideApp(packageName: String) {
        val current = _hiddenPackages.value.toMutableSet()
        current.add(packageName)
        prefs.edit().putStringSet("hidden_packages", current).apply()
        _hiddenPackages.value = current
        _pinnedDesktopApps.value = _pinnedDesktopApps.value.filter { it.packageName != packageName }
        _pinnedTaskbarApps.value = _pinnedTaskbarApps.value.filter { it.packageName != packageName }
    }

    fun unhideApp(packageName: String) {
        val current = _hiddenPackages.value.toMutableSet()
        current.remove(packageName)
        prefs.edit().putStringSet("hidden_packages", current).apply()
        _hiddenPackages.value = current
    }

    private fun getPinnedDesktopPackages(): Set<String> {
        val defaultPinned = setOf(
            "com.sec.android.app.myfiles",
            "com.android.settings",
            "com.sec.android.app.sbrowser",
            "com.android.chrome",
            "com.google.android.apps.photos",
            "com.google.android.gm"
        )
        return prefs.getStringSet("pinned_desktop_packages", null) ?: run {
            prefs.edit().putStringSet("pinned_desktop_packages", defaultPinned).apply()
            defaultPinned
        }
    }

    private fun getPinnedTaskbarPackages(): Set<String> {
        val defaultPinned = setOf(
            "com.google.android.gm",
            "com.android.settings",
            "com.google.android.apps.docs",
            "com.android.chrome"
        )
        return prefs.getStringSet("pinned_taskbar_packages", null) ?: run {
            prefs.edit().putStringSet("pinned_taskbar_packages", defaultPinned).apply()
            defaultPinned
        }
    }

    fun getDesktopAppPositions(): Map<String, Pair<Float, Float>> {
        val raw = prefs.getString("desktop_app_positions", "") ?: ""
        if (raw.isBlank()) return emptyMap()
        val result = mutableMapOf<String, Pair<Float, Float>>()
        raw.split(";").forEach { entry ->
            val parts = entry.split(":")
            if (parts.size == 2) {
                val coords = parts[1].split(",")
                if (coords.size == 2) {
                    val x = coords[0].toFloatOrNull()
                    val y = coords[1].toFloatOrNull()
                    if (x != null && y != null) {
                        result[parts[0]] = Pair(x, y)
                    }
                }
            }
        }
        return result
    }

    fun saveDesktopAppPosition(packageName: String, x: Float, y: Float) {
        val current = getDesktopAppPositions().toMutableMap()
        current[packageName] = Pair(x, y)
        val serialized = current.entries.joinToString(";") { "${it.key}:${it.value.first},${it.value.second}" }
        prefs.edit().putString("desktop_app_positions", serialized).apply()
    }

    fun getDesktopAppOrder(): List<String> {
        val raw = prefs.getString("desktop_app_order", "") ?: ""
        return if (raw.isBlank()) emptyList() else raw.split(",")
    }

    fun saveDesktopAppOrder(order: List<String>) {
        prefs.edit().putString("desktop_app_order", order.joinToString(",")).apply()
        val orderMap = order.withIndex().associate { it.value to it.index }
        _pinnedDesktopApps.value = _pinnedDesktopApps.value.sortedBy { orderMap[it.packageName] ?: 999 }
    }

    suspend fun pinToDesktop(packageName: String) {
        val currentSet = getPinnedDesktopPackages().toMutableSet()
        currentSet.add(packageName)
        prefs.edit().putStringSet("pinned_desktop_packages", currentSet).apply()

        val currentOrder = getDesktopAppOrder().toMutableList()
        if (!currentOrder.contains(packageName)) {
            currentOrder.add(packageName)
            prefs.edit().putString("desktop_app_order", currentOrder.joinToString(",")).apply()
        }
        refreshApps()
    }

    suspend fun unpinFromDesktop(packageName: String) {
        val currentSet = getPinnedDesktopPackages().toMutableSet()
        currentSet.remove(packageName)
        prefs.edit().putStringSet("pinned_desktop_packages", currentSet).apply()

        val currentOrder = getDesktopAppOrder().toMutableList()
        if (currentOrder.contains(packageName)) {
            currentOrder.remove(packageName)
            prefs.edit().putString("desktop_app_order", currentOrder.joinToString(",")).apply()
        }
        refreshApps()
    }

    suspend fun pinToTaskbar(packageName: String) {
        val currentSet = getPinnedTaskbarPackages().toMutableSet()
        currentSet.add(packageName)
        prefs.edit().putStringSet("pinned_taskbar_packages", currentSet).apply()
        refreshApps()
    }

    suspend fun unpinFromTaskbar(packageName: String) {
        val currentSet = getPinnedTaskbarPackages().toMutableSet()
        currentSet.remove(packageName)
        prefs.edit().putStringSet("pinned_taskbar_packages", currentSet).apply()
        refreshApps()
    }

    fun markAppStarted(packageName: String) {
        val current = _runningPackages.value.toMutableSet()
        current.add(packageName)
        _runningPackages.value = current
        _focusedPackage.value = packageName
    }

    fun setFocusedApp(packageName: String?) {
        _focusedPackage.value = packageName
    }

    fun markAppStopped(packageName: String) {
        val current = _runningPackages.value.toMutableSet()
        current.remove(packageName)
        _runningPackages.value = current
        if (_focusedPackage.value == packageName) {
            _focusedPackage.value = current.lastOrNull()
        }
    }

    fun syncRunningTasksWithShizuku(shizukuManager: ShizukuManager) {
        if (!shizukuManager.isConnected()) return
        try {
            val output = shizukuManager.executeShell("dumpsys activity recents | grep -E 'realActivity=' | head -n 15")
            val regex = Regex("realActivity=([^/]+)/")
            val matches = regex.findAll(output).map { it.groupValues[1] }.toSet()
            if (matches.isNotEmpty()) {
                val current = _runningPackages.value.toMutableSet()
                current.addAll(matches.filter { it != context.packageName })
                _runningPackages.value = current
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun syncRunningTasksWithUsageStats() {
        try {
            val usm = context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager ?: return
            val endTime = System.currentTimeMillis()
            val startTime = endTime - 1000 * 60 * 30
            val usageEvents = usm.queryEvents(startTime, endTime)
            val event = UsageEvents.Event()
            val recentPkgs = mutableSetOf<String>()
            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED && event.packageName != context.packageName) {
                    recentPkgs.add(event.packageName)
                }
            }
            if (recentPkgs.isNotEmpty()) {
                val current = _runningPackages.value.toMutableSet()
                current.addAll(recentPkgs)
                _runningPackages.value = current
            }
        } catch (_: Exception) {}
    }

    // --- Quản lý Thư mục Màn hình chính (Desktop Folders) ---
    private fun loadFolders(): List<DesktopFolder> {
        val raw = prefs.getString("desktop_folders", "") ?: ""
        if (raw.isBlank()) return emptyList()
        val list = mutableListOf<DesktopFolder>()
        raw.split(";;").forEach { item ->
            val parts = item.split("::")
            if (parts.size >= 6) {
                val id = parts[0]
                val name = parts[1]
                val pkgs = if (parts[2].isBlank()) emptyList() else parts[2].split(",")
                val isLarge = parts[3].toBoolean()
                val x = parts[4].toFloatOrNull() ?: 0.1f
                val y = parts[5].toFloatOrNull() ?: 0.1f
                list.add(DesktopFolder(id, name, pkgs, isLarge, x, y))
            }
        }
        return list
    }

    private fun persistFolders(folders: List<DesktopFolder>) {
        val serialized = folders.joinToString(";;") { f ->
            "${f.id}::${f.name}::${f.packageNames.joinToString(",")}::${f.isLarge}::${f.xPercent}::${f.yPercent}"
        }
        prefs.edit().putString("desktop_folders", serialized).apply()
        _folders.value = folders
    }

    fun createFolder(app1: String, app2: String, x: Float, y: Float): DesktopFolder {
        val newFolder = DesktopFolder(
            id = "folder_" + UUID.randomUUID().toString().take(8),
            name = "Thư mục mới",
            packageNames = listOf(app1, app2),
            isLarge = false,
            xPercent = x,
            yPercent = y
        )
        val updated = _folders.value + newFolder
        persistFolders(updated)
        return newFolder
    }

    fun addAppToFolder(folderId: String, packageName: String) {
        val updated = _folders.value.map { folder ->
            if (folder.id == folderId && !folder.packageNames.contains(packageName)) {
                folder.copy(packageNames = folder.packageNames + packageName)
            } else {
                folder
            }
        }
        persistFolders(updated)
    }

    fun removeAppFromFolder(folderId: String, packageName: String) {
        val updated = _folders.value.mapNotNull { folder ->
            if (folder.id == folderId) {
                val newPkgs = folder.packageNames - packageName
                if (newPkgs.isEmpty()) null else folder.copy(packageNames = newPkgs)
            } else {
                folder
            }
        }
        persistFolders(updated)
    }

    fun renameFolder(folderId: String, newName: String) {
        val updated = _folders.value.map { folder ->
            if (folder.id == folderId) folder.copy(name = newName) else folder
        }
        persistFolders(updated)
    }

    fun toggleFolderSize(folderId: String) {
        val updated = _folders.value.map { folder ->
            if (folder.id == folderId) folder.copy(isLarge = !folder.isLarge) else folder
        }
        persistFolders(updated)
    }

    fun dissolveFolder(folderId: String) {
        val updated = _folders.value.filter { it.id != folderId }
        persistFolders(updated)
    }

    fun updateFolderPosition(folderId: String, x: Float, y: Float) {
        val updated = _folders.value.map { folder ->
            if (folder.id == folderId) folder.copy(xPercent = x, yPercent = y) else folder
        }
        persistFolders(updated)
    }

    // --- Quản lý Widget Màn hình chính (Resizable Widgets) ---
    private fun loadWidgets(): List<DesktopWidgetItem> {
        val raw = prefs.getString("desktop_widgets", "") ?: ""
        if (raw.isBlank()) return emptyList()
        val list = mutableListOf<DesktopWidgetItem>()
        raw.split(";;").forEach { item ->
            val parts = item.split("::")
            if (parts.size >= 6) {
                val id = parts[0]
                val type = try { DesktopWidgetType.valueOf(parts[1]) } catch (_: Exception) { DesktopWidgetType.CLOCK_CALENDAR }
                val x = parts[2].toFloatOrNull() ?: 0.5f
                val y = parts[3].toFloatOrNull() ?: 0.1f
                val w = parts[4].toIntOrNull() ?: 280
                val h = parts[5].toIntOrNull() ?: 150
                list.add(DesktopWidgetItem(id, type, x, y, w, h))
            }
        }
        return list
    }

    private fun persistWidgets(widgets: List<DesktopWidgetItem>) {
        val serialized = widgets.joinToString(";;") { w ->
            "${w.id}::${w.type.name}::${w.xPercent}::${w.yPercent}::${w.widthDp}::${w.heightDp}"
        }
        prefs.edit().putString("desktop_widgets", serialized).apply()
        _widgets.value = widgets
    }

    fun addWidget(type: DesktopWidgetType, x: Float = 0.5f, y: Float = 0.15f): DesktopWidgetItem {
        val (defaultW, defaultH) = when (type) {
            DesktopWidgetType.CLOCK_CALENDAR -> Pair(280, 140)
            DesktopWidgetType.WEATHER -> Pair(260, 140)
            DesktopWidgetType.SYSTEM_STATS -> Pair(270, 160)
            DesktopWidgetType.STICKY_NOTE -> Pair(240, 180)
            DesktopWidgetType.MEDIA_PLAYER -> Pair(280, 130)
            DesktopWidgetType.SEARCH -> Pair(320, 70)
        }
        val widget = DesktopWidgetItem(
            id = "widget_" + UUID.randomUUID().toString().take(8),
            type = type,
            xPercent = x.coerceIn(0.05f, 0.85f),
            yPercent = y.coerceIn(0.05f, 0.80f),
            widthDp = defaultW,
            heightDp = defaultH
        )
        persistWidgets(_widgets.value + widget)
        return widget
    }

    fun updateWidgetBounds(id: String, x: Float, y: Float, widthDp: Int, heightDp: Int) {
        val updated = _widgets.value.map { w ->
            if (w.id == id) w.copy(xPercent = x, yPercent = y, widthDp = widthDp, heightDp = heightDp) else w
        }
        persistWidgets(updated)
    }

    fun removeWidget(id: String) {
        val updated = _widgets.value.filter { it.id != id }
        persistWidgets(updated)
    }

    fun getStickyNoteText(): String {
        return prefs.getString("sticky_note_content", "Ghi chú nhanh trên Desktop DeX...") ?: ""
    }

    fun saveStickyNoteText(text: String) {
        prefs.edit().putString("sticky_note_content", text).apply()
    }
}
