package rikazzz.everythinglauncher.ui.settings

import android.content.Context
import android.os.Environment
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import rikazzz.everythinglauncher.data.backup.BackupManager
import rikazzz.everythinglauncher.ui.LauncherActions
import rikazzz.everythinglauncher.ui.LauncherUiState
import rikazzz.everythinglauncher.ui.components.LauncherIcon
import rikazzz.everythinglauncher.ui.components.LauncherSymbol
import kotlinx.coroutines.launch
import java.io.File

@Composable
internal fun BackupSettingsScreen(
    uiState: LauncherUiState,
    actions: LauncherActions,
    onBack: () -> Unit,
    onShowApplying: () -> Unit = {},
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isExporting by remember { mutableStateOf(false) }
    var isImporting by remember { mutableStateOf(false) }

    fun findLatestBackup(): File? {
        val downloadDir = File("/sdcard/Download").takeIf { it.exists() && it.canRead() }
            ?: context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)
            ?: context.filesDir
        return downloadDir?.listFiles { file ->
            file.name.startsWith("EverythingLauncher_Backup_") && file.name.endsWith(".json")
        }?.maxByOrNull { it.lastModified() }
    }

    var latestBackupFile by remember { mutableStateOf(findLatestBackup()) }

    val restorePicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) {
            isImporting = true
            scope.launch {
                val result = BackupManager.importBackup(context, uri, actions)
                isImporting = false
                result.onSuccess { time ->
                    Toast.makeText(context, "Settings restored successfully ($time)", Toast.LENGTH_LONG).show()
                    onShowApplying()
                }.onFailure { e ->
                    Toast.makeText(context, "Failed to restore backup: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                }
            }
        }
    }

    SettingsScaffold("Backup & Restore", "settings_backup", onBack) { padding ->
        SettingsList(padding) {
            item {
                SettingsActionItem(
                    "Create Backup Now",
                    "Export all launcher settings, favorites, shortcuts & folders to a timestamped JSON file in Downloads",
                    0, 3, "backup_create",
                    leading = { LauncherIcon(LauncherSymbol.Launch) },
                    enabled = !isExporting && !isImporting,
                ) {
                    isExporting = true
                    scope.launch {
                        val result = BackupManager.exportBackup(context, uiState)
                        isExporting = false
                        result.onSuccess { file ->
                            latestBackupFile = file
                            Toast.makeText(context, "Backup saved: ${file.name}", Toast.LENGTH_LONG).show()
                        }.onFailure { e ->
                            Toast.makeText(context, "Failed to create backup: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
                        }
                    }
                }
            }
            item {
                SettingsActionItem(
                    "Restore Backup",
                    "Choose an exported JSON backup file to restore settings and layout",
                    1, 3, "backup_restore",
                    leading = { LauncherIcon(LauncherSymbol.Folder) },
                    enabled = !isExporting && !isImporting,
                ) {
                    restorePicker.launch(arrayOf("application/json", "text/*", "*/*"))
                }
            }
            item {
                val latest = latestBackupFile
                SettingsActionItem(
                    "Share Latest Backup",
                    if (latest != null) "Share ${latest.name}" else "No backup file found in Downloads",
                    2, 3, "backup_share",
                    leading = { LauncherIcon(LauncherSymbol.Outbound) },
                    enabled = latest != null && !isExporting && !isImporting,
                ) {
                    if (latest != null) {
                        BackupManager.shareBackup(context, latest)
                    }
                }
            }
        }
    }
}
