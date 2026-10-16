package com.example.dexlauncher.shizuku

import android.app.Activity
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.graphics.Rect
import android.net.Uri
import android.os.IBinder
import com.example.dexlauncher.BuildConfig
import com.example.dexlauncher.IShizukuService
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rikka.shizuku.Shizuku

sealed class ShizukuStatus {
    object NotInstalled : ShizukuStatus()
    object NotRunning : ShizukuStatus()
    object PermissionNeeded : ShizukuStatus()
    data class Connected(val isRoot: Boolean, val uid: Int) : ShizukuStatus()
}

class ShizukuManager(private val context: Context) {

    private val _status = MutableStateFlow<ShizukuStatus>(ShizukuStatus.NotRunning)
    val status: StateFlow<ShizukuStatus> = _status.asStateFlow()

    private var shizukuService: IShizukuService? = null

    private val userServiceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            shizukuService = IShizukuService.Stub.asInterface(service)
            val isRoot = Shizuku.isPreV11() || Shizuku.getUid() == 0
            _status.value = ShizukuStatus.Connected(isRoot = isRoot, uid = Shizuku.getUid())
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            shizukuService = null
            checkStatus()
        }
    }

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        checkStatus()
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        shizukuService = null
        checkStatus()
    }

    private val requestPermissionResultListener =
        Shizuku.OnRequestPermissionResultListener { requestCode, grantResult ->
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                bindUserService()
            } else {
                _status.value = ShizukuStatus.PermissionNeeded
            }
        }

    init {
        try {
            Shizuku.addBinderReceivedListener(binderReceivedListener)
            Shizuku.addBinderDeadListener(binderDeadListener)
            Shizuku.addRequestPermissionResultListener(requestPermissionResultListener)
            checkStatus()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun checkStatus() {
        try {
            if (!isShizukuInstalled()) {
                _status.value = ShizukuStatus.NotInstalled
                return
            }

            if (!Shizuku.pingBinder()) {
                _status.value = ShizukuStatus.NotRunning
                return
            }

            if (Shizuku.isPreV11()) {
                _status.value = ShizukuStatus.Connected(isRoot = false, uid = 0)
                return
            }

            if (Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                bindUserService()
            } else if (Shizuku.shouldShowRequestPermissionRationale()) {
                _status.value = ShizukuStatus.PermissionNeeded
            } else {
                _status.value = ShizukuStatus.PermissionNeeded
            }
        } catch (e: Exception) {
            _status.value = ShizukuStatus.NotRunning
        }
    }

    private fun isShizukuInstalled(): Boolean {
        return try {
            context.packageManager.getPackageInfo("moe.shizuku.privileged.api", 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun requestPermission(activity: Activity, requestCode: Int = 1001) {
        try {
            if (Shizuku.pingBinder()) {
                Shizuku.requestPermission(requestCode)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun openShizukuApp() {
        val intent = context.packageManager.getLaunchIntentForPackage("moe.shizuku.privileged.api")
        if (intent != null) {
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } else {
            val marketIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app"))
            marketIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(marketIntent)
        }
    }

    private fun bindUserService() {
        if (shizukuService != null) return

        try {
            val userServiceArgs = Shizuku.UserServiceArgs(
                ComponentName(BuildConfig.APPLICATION_ID, ShizukuUserService::class.java.name)
            )
                .daemon(false)
                .processNameSuffix("dex_shell")
                .debuggable(BuildConfig.DEBUG)
                .version(BuildConfig.VERSION_CODE)

            Shizuku.bindUserService(userServiceArgs, userServiceConnection)
        } catch (e: Exception) {
            val isRoot = Shizuku.isPreV11() || Shizuku.getUid() == 0
            _status.value = ShizukuStatus.Connected(isRoot = isRoot, uid = Shizuku.getUid())
        }
    }

    fun isConnected(): Boolean {
        return _status.value is ShizukuStatus.Connected
    }

    fun launchFreeformWindow(packageName: String, bounds: Rect): Boolean {
        val service = shizukuService ?: return false
        return try {
            service.launchWindow(packageName, bounds.left, bounds.top, bounds.right, bounds.bottom)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }

    fun resizeTask(taskId: Int, bounds: Rect) {
        try {
            shizukuService?.resizeTask(taskId, bounds.left, bounds.top, bounds.right, bounds.bottom)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun maximizeTask(taskId: Int, screenWidth: Int, screenHeight: Int) {
        try {
            val topBarHeight = 60
            val taskbarHeight = 70
            shizukuService?.resizeTask(taskId, 0, topBarHeight, screenWidth, screenHeight - taskbarHeight)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun snapLeft(taskId: Int, screenWidth: Int, screenHeight: Int) {
        try {
            val topBarHeight = 60
            val taskbarHeight = 70
            shizukuService?.resizeTask(taskId, 0, topBarHeight, screenWidth / 2, screenHeight - taskbarHeight)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun snapRight(taskId: Int, screenWidth: Int, screenHeight: Int) {
        try {
            val topBarHeight = 60
            val taskbarHeight = 70
            shizukuService?.resizeTask(taskId, screenWidth / 2, topBarHeight, screenWidth, screenHeight - taskbarHeight)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun closeTask(taskId: Int) {
        try {
            shizukuService?.closeTask(taskId)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun forceStopApp(packageName: String): Boolean {
        return try {
            executeShell("am force-stop $packageName")
            true
        } catch (e: Exception) {
            false
        }
    }

    fun executeShell(command: String): String {
        return try {
            shizukuService?.executeCommandWithOutput(command) ?: "Shizuku UserService not bound"
        } catch (e: Exception) {
            "Error: ${e.message}"
        }
    }
}
