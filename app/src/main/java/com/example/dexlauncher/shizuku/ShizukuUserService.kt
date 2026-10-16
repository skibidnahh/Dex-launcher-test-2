package com.example.dexlauncher.shizuku

import android.os.Process
import com.example.dexlauncher.IShizukuService
import java.io.BufferedReader
import java.io.InputStreamReader
import kotlin.system.exitProcess

class ShizukuUserService : IShizukuService.Stub() {

    override fun executeCommand(command: String?): Int {
        if (command.isNullOrBlank()) return -1
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            process.waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
            -1
        }
    }

    override fun executeCommandWithOutput(command: String?): String {
        if (command.isNullOrBlank()) return ""
        return try {
            val process = Runtime.getRuntime().exec(arrayOf("sh", "-c", command))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val output = StringBuilder()
            var line: String?
            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            process.waitFor()
            output.toString()
        } catch (e: Exception) {
            e.printStackTrace()
            "Error: ${e.message}"
        }
    }

    override fun launchWindow(packageName: String?, left: Int, top: Int, right: Int, bottom: Int) {
        if (packageName.isNullOrBlank()) return
        try {
            // Windowing mode 5 = WINDOWING_MODE_FREEFORM
            val cmd = "am start --windowingMode 5 \$(pm resolve-activity --brief -c android.intent.category.LAUNCHER -a android.intent.action.MAIN $packageName | tail -n 1)"
            Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd)).waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun resizeTask(taskId: Int, left: Int, top: Int, right: Int, bottom: Int) {
        if (taskId <= 0) return
        try {
            val cmd = "am task resize $taskId $left $top $right $bottom"
            Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd)).waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun setTaskWindowingMode(taskId: Int, windowingMode: Int) {
        if (taskId <= 0) return
        try {
            val cmd = "am task set-windowing-mode $taskId $windowingMode"
            Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd)).waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun closeTask(taskId: Int) {
        if (taskId <= 0) return
        try {
            val cmd = "am task lock stop $taskId ; am stack remove $taskId"
            Runtime.getRuntime().exec(arrayOf("sh", "-c", cmd)).waitFor()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    override fun getRecentTasks(): String {
        return executeCommandWithOutput("dumpsys activity recents | grep -E 'taskId=|realActivity=' | head -n 30")
    }

    override fun destroy() {
        exitProcess(0)
    }
}
