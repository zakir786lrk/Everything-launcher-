package rikazzz.everythinglauncher.customshortcuts

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.widget.Toast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rikazzz.everythinglauncher.data.CustomActivityShortcut

object ActivityLauncherHelper {
    fun launchShortcut(context: Context, shortcut: CustomActivityShortcut) {
        val intent = Intent().apply {
            if (!shortcut.action.isNullOrBlank()) {
                action = shortcut.action
            }
            if (shortcut.packageName.isNotBlank() && shortcut.activityName.isNotBlank()) {
                component = ComponentName(shortcut.packageName, shortcut.activityName)
            } else if (shortcut.packageName.isNotBlank()) {
                `package` = shortcut.packageName
            }
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (sec: SecurityException) {
            // Unexported activity - try launching with root shell
            if (shortcut.packageName.isNotBlank() && shortcut.activityName.isNotBlank()) {
                tryLaunchWithRoot(context, shortcut.packageName, shortcut.activityName)
            } else {
                Toast.makeText(context, "Permission denied: ${sec.localizedMessage ?: sec.message}", Toast.LENGTH_SHORT).show()
            }
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot launch: ${e.localizedMessage ?: e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    fun launchActivity(context: Context, packageName: String, activityName: String) {
        val intent = Intent().apply {
            component = ComponentName(packageName, activityName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        try {
            context.startActivity(intent)
        } catch (sec: SecurityException) {
            tryLaunchWithRoot(context, packageName, activityName)
        } catch (e: Exception) {
            Toast.makeText(context, "Cannot launch: ${e.localizedMessage ?: e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun tryLaunchWithRoot(context: Context, packageName: String, activityName: String) {
        Thread {
            try {
                val cmd = "am start -n $packageName/$activityName"
                val process = Runtime.getRuntime().exec(arrayOf("su", "-c", cmd))
                val exitCode = process.waitFor()
                if (exitCode != 0) {
                    val err = process.errorStream.bufferedReader().readText()
                    showToast(context, "Root launch failed: $err")
                }
            } catch (e: Exception) {
                showToast(context, "Permission denied (Root required to launch private activity)")
            }
        }.start()
    }

    private fun showToast(context: Context, msg: String) {
        android.os.Handler(android.os.Looper.getMainLooper()).post {
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }
}
