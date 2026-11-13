package rikazzz.everythinglauncher.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.File
import java.io.InputStreamReader

object RootExecutor {
    private var rootAvailable: Boolean? = null
    var lastCheckMessage: String = "Not checked yet"
        private set

    suspend fun isRootAvailable(forceRecheck: Boolean = false): Boolean = withContext(Dispatchers.IO) {
        if (!forceRecheck && rootAvailable != null) {
            return@withContext rootAvailable!!
        }

        val suBinaryLocations = listOf(
            "/system/bin/su", "/system/xbin/su", "/sbin/su",
            "/data/adb/ksu/bin/su", "/data/adb/ap/bin/su", "/data/adb/magisk/su"
        )
        val binaryExists = suBinaryLocations.any { File(it).exists() }

        var granted = false
        var details = ""

        // Attempt 1: direct `su -c id`
        try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val line = reader.readLine()
            process.waitFor()
            if (process.exitValue() == 0 || (line != null && line.contains("uid=0"))) {
                granted = true
                details = line ?: "Success"
            }
        } catch (e: Exception) {
            details = e.localizedMessage ?: "su -c execution failed"
        }

        // Attempt 2: interactive `su`
        if (!granted) {
            try {
                val p = Runtime.getRuntime().exec("su")
                val os = DataOutputStream(p.outputStream)
                os.writeBytes("id\nexit\n")
                os.flush()
                val r = BufferedReader(InputStreamReader(p.inputStream))
                val l = r.readLine()
                p.waitFor()
                if (l != null && l.contains("uid=0")) {
                    granted = true
                    details = l
                }
            } catch (e: Exception) {
                // ignore
            }
        }

        rootAvailable = granted
        lastCheckMessage = if (granted) {
            "Root Active ($details)"
        } else if (binaryExists) {
            "Root binary found, but permission was not granted by Magisk/KernelSU yet. Please grant Everything Launcher in your Magisk/KernelSU/APatch app."
        } else {
            "Root not detected. If your device is rooted, grant Everything Launcher in Magisk/KernelSU or toggle 'Force Root Execution' below."
        }

        granted
    }

    suspend fun execute(command: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            var stdout = ""
            var stderr = ""
            var exitCode = -1

            // Try su -c first
            try {
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", command))
                stdout = process.inputStream.bufferedReader().readText().trim()
                stderr = process.errorStream.bufferedReader().readText().trim()
                exitCode = process.waitFor()
            } catch (e: Exception) {
                // Fallback to piped su
                val p = Runtime.getRuntime().exec("su")
                val os = DataOutputStream(p.outputStream)
                os.writeBytes("$command\nexit\n")
                os.flush()
                stdout = p.inputStream.bufferedReader().readText().trim()
                stderr = p.errorStream.bufferedReader().readText().trim()
                exitCode = p.waitFor()
            }

            if (exitCode == 0 || stdout.isNotBlank()) {
                if (stdout.isBlank()) "Command executed successfully." else stdout
            } else {
                val errorMsg = if (stderr.isNotBlank()) stderr else "Exit code: $exitCode"
                throw RuntimeException("Command failed: $errorMsg")
            }
        }
    }

    suspend fun setWifi(enable: Boolean): Result<String> {
        val cmd = if (enable) "svc wifi enable" else "svc wifi disable"
        return execute(cmd)
    }

    suspend fun setBluetooth(enable: Boolean): Result<String> {
        val cmd = if (enable) "svc bluetooth enable" else "svc bluetooth disable"
        return execute(cmd)
    }

    suspend fun setMobileData(enable: Boolean): Result<String> {
        val cmd = if (enable) "svc data enable" else "svc data disable"
        return execute(cmd)
    }

    suspend fun setAirplaneMode(enable: Boolean): Result<String> {
        val state = if (enable) "enable" else "disable"
        return execute("cmd connectivity airplane-mode $state")
    }

    suspend fun setVolume(stream: Int = 3, level: Int): Result<String> {
        val clamped = level.coerceIn(0, 100)
        return execute("media volume --stream $stream --set $clamped")
    }

    suspend fun takeScreenshot(savePath: String = "/sdcard/Pictures/screenshot.png"): Result<String> {
        return execute("screencap -p $savePath")
    }
}
