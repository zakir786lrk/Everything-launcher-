package rikazzz.everythinglauncher.data

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import rikazzz.everythinglauncher.ui.LauncherUiState

enum class DynamicAppType {
    RecentlyInstalled,
    RecentlyOpened,
    FrequentlyOpened;

    fun label(): String = when (this) {
        RecentlyInstalled -> "Recently Installed"
        RecentlyOpened -> "Recently Opened"
        FrequentlyOpened -> "Frequently Opened"
    }

    fun description(): String = when (this) {
        RecentlyInstalled -> "Apps newly installed or updated on your device"
        RecentlyOpened -> "Apps you have launched most recently"
        FrequentlyOpened -> "Apps you launch most frequently based on usage"
    }
}

object DynamicAppsHelper {
    fun getApps(type: DynamicAppType, uiState: LauncherUiState, limit: Int = 5): List<LauncherApp> {
        return when (type) {
            DynamicAppType.RecentlyInstalled -> {
                uiState.recentlyInstalledApps.take(limit)
            }
            DynamicAppType.RecentlyOpened -> {
                val recentKeys = uiState.settings.recentAppKeys
                val found = recentKeys.mapNotNull { key ->
                    uiState.findItem(key) ?: uiState.allApps.firstOrNull { it.key == key }
                }.distinctBy { it.key }
                if (found.isNotEmpty()) found.take(limit)
                else uiState.allApps.take(limit)
            }
            DynamicAppType.FrequentlyOpened -> {
                val counts = uiState.usage.openCounts.ifEmpty { uiState.usage.ranking }
                if (counts.isNotEmpty()) {
                    val sorted = uiState.allApps
                        .filter { it.key in counts && (counts[it.key] ?: 0L) > 0L }
                        .sortedByDescending { counts[it.key] ?: 0L }
                    if (sorted.isNotEmpty()) sorted.take(limit)
                    else uiState.allApps.take(limit)
                } else {
                    uiState.allApps.take(limit)
                }
            }
        }
    }
}

@Composable
fun DynamicAppsDialog(
    onDismiss: () -> Unit,
    onSelect: (DynamicAppType) -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Include Dynamic Apps", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    "Choose a dynamic group to quickly add apps based on your device usage:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(4.dp))
                DynamicAppType.entries.forEach { type ->
                    Surface(
                        onClick = {
                            onSelect(type)
                            onDismiss()
                        },
                        shape = RoundedCornerShape(12.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(12.dp)) {
                            Text(type.label(), fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                            Spacer(Modifier.height(2.dp))
                            Text(
                                type.description(),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
