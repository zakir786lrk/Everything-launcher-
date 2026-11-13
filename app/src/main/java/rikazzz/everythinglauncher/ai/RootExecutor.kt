package rikazzz.everythinglauncher.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.DataOutputStream
import java.io.InputStreamReader

object RootExecutor {
    private var rootAvailable: Boolean? = null

    suspend fun isRootAvailable(): Boolean = withContext(Dispatchers.IO) {
        rootAvailable?.let { return@withContext it }
        val available = try {
            val process = Runtime.getRuntime().exec(arrayOf("su", "-c", "id"))
            val output = process.inputStream.bufferedReader().readText()
            process.waitFor()
            output.contains("uid=0") || process.exitValue() == 0
        } catch (e: Exception) {
            false
        }
        rootAvailable = available
        available
    }

    suspend fun execute(command: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val process = Runtime.getRuntime().exec("su")
            val outputStream = DataOutputStream(process.outputStream)
            outputStream.writeBytes("$command\nexit\n")
            outputStream.flush()

            val stdoutReader = BufferedReader(InputStreamReader(process.inputStream))
            val stderrReader = BufferedReader(InputStreamReader(process.errorStream))

            val stdout = stdoutReader.readText()
            val stderr = stderrReader.readText()
            val exitCode = process.waitFor()

            if (exitCode == 0) {
                if (stdout.isBlank()) "Command executed successfully." else stdout.trim()
            } else {
                val errorMsg = if (stderr.isNotBlank()) stderr.trim() else "Exit code: $exitCode"
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
