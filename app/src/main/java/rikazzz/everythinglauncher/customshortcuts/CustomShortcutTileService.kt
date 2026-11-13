package rikazzz.everythinglauncher.customshortcuts

import android.app.PendingIntent
import android.content.Intent
import android.os.Build
import android.service.quicksettings.Tile
import android.service.quicksettings.TileService
import androidx.annotation.RequiresApi
import rikazzz.everythinglauncher.data.LauncherCustomPreferences

@RequiresApi(Build.VERSION_CODES.N)
class CustomShortcutTileService : TileService() {

    override fun onStartListening() {
        super.onStartListening()
        val prefs = LauncherCustomPreferences(this)
        val shortcuts = prefs.getCustomShortcuts()
        val tileShortcut = shortcuts.firstOrNull { it.assignToQuickTile } ?: shortcuts.firstOrNull()

        qsTile?.apply {
            state = if (tileShortcut != null) Tile.STATE_ACTIVE else Tile.STATE_INACTIVE
            label = tileShortcut?.name ?: "Custom Shortcut"
            updateTile()
        }
    }

    override fun onClick() {
        super.onClick()
        val prefs = LauncherCustomPreferences(this)
        val shortcuts = prefs.getCustomShortcuts()
        val tileShortcut = shortcuts.firstOrNull { it.assignToQuickTile } ?: shortcuts.firstOrNull()

        if (tileShortcut != null) {
            val intent = Intent().apply {
                component = android.content.ComponentName(tileShortcut.packageName, tileShortcut.activityName)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }

            unlockAndRun {
                @Suppress("DEPRECATION")
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startActivityAndCollapse(intent)
                } else {
                    val pending = PendingIntent.getActivity(
                        this,
                        0,
                        intent,
                        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
                    )
                    startActivityAndCollapse(pending)
                }
            }
        } else {
            // Open launcher home
            val homeIntent = Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            unlockAndRun {
                @Suppress("DEPRECATION")
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    startActivityAndCollapse(homeIntent)
                } else {
                    val pending = PendingIntent.getActivity(
                        this,
                        0,
                        homeIntent,
                        PendingIntent.FLAG_IMMUTABLE
                    )
                    startActivityAndCollapse(pending)
                }
            }
        }
    }
}
