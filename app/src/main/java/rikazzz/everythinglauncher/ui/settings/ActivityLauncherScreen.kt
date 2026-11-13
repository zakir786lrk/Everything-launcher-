package rikazzz.everythinglauncher.ui.settings

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import rikazzz.everythinglauncher.R
import rikazzz.everythinglauncher.customshortcuts.ActivityLauncherHelper
import rikazzz.everythinglauncher.customshortcuts.ActivityScanner
import rikazzz.everythinglauncher.customshortcuts.ScannedActivity
import rikazzz.everythinglauncher.customshortcuts.ScannedApp
import rikazzz.everythinglauncher.data.CustomActivityShortcut
import rikazzz.everythinglauncher.data.LauncherCustomPreferences
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActivityLauncherScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val prefs = remember { LauncherCustomPreferences(context) }

    var shortcuts by remember { mutableStateOf(prefs.getCustomShortcuts()) }
    var isBrowsingActivities by remember { mutableStateOf(false) }
    var isLoadingApps by remember { mutableStateOf(false) }
    var scannedApps by remember { mutableStateOf<List<ScannedApp>>(emptyList()) }
    var appFilterQuery by remember { mutableStateOf("") }
    var selectedActivityForDialog by remember { mutableStateOf<ScannedActivity?>(null) }
    var selectedPackageForDialog by remember { mutableStateOf<String>("") }

    fun refreshShortcuts() {
        shortcuts = prefs.getCustomShortcuts()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isBrowsingActivities) "Select Activity" else "Custom Shortcuts") },
                navigationIcon = {
                    IconButton(onClick = {
                        if (isBrowsingActivities) isBrowsingActivities = false else onBack()
                    }) {
                        Icon(painterResource(R.drawable.ms_arrow_back), contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        if (isBrowsingActivities) {
            // Scanner view
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp)
            ) {
                OutlinedTextField(
                    value = appFilterQuery,
                    onValueChange = { appFilterQuery = it },
                    label = { Text("Filter apps & activities...") },
                    modifier = Modifier.fillMaxWidth(),
                    trailingIcon = {
                        if (appFilterQuery.isNotBlank()) {
                            IconButton(onClick = { appFilterQuery = "" }) {
                                Icon(painterResource(R.drawable.ms_close), contentDescription = "Clear")
                            }
                        }
                    }
                )

                Spacer(Modifier.height(12.dp))

                if (isLoadingApps) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator()
                            Spacer(Modifier.height(8.dp))
                            Text("Scanning installed activities...", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                } else {
                    val filteredApps = remember(scannedApps, appFilterQuery) {
                        if (appFilterQuery.isBlank()) scannedApps else {
                            val q = appFilterQuery.lowercase()
                            scannedApps.mapNotNull { app ->
                                val appMatches = app.appName.lowercase().contains(q) || app.packageName.lowercase().contains(q)
                                val matchingActs = app.activities.filter {
                                    it.label.lowercase().contains(q) || it.componentName.className.lowercase().contains(q)
                                }
                                if (appMatches || matchingActs.isNotEmpty()) {
                                    app.copy(activities = if (appMatches) app.activities else matchingActs)
                                } else null
                            }
                        }
                    }

                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(filteredApps, key = { it.packageName }) { app ->
                            AppActivityCard(
                                app = app,
                                onSelectActivity = { act ->
                                    selectedPackageForDialog = app.packageName
                                    selectedActivityForDialog = act
                                }
                            )
                        }
                    }
                }
            }
        } else {
            // Manage Created Shortcuts
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                item {
                    Button(
                        onClick = {
                            isBrowsingActivities = true
                            if (scannedApps.isEmpty()) {
                                scope.launch {
                                    isLoadingApps = true
                                    scannedApps = ActivityScanner.scanInstalledApps(context)
                                    isLoadingApps = false
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(painterResource(R.drawable.ms_add), contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Add New Custom Activity Shortcut")
                    }
                }

                if (shortcuts.isEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                        ) {
                            Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("No Custom Shortcuts", fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(4.dp))
                                Text(
                                    "Tap '+ Add New Custom Activity Shortcut' to browse installed apps and create direct shortcuts to hidden settings, sub-screens, or app activities.",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                } else {
                    item {
                        Text("Configured Shortcuts (${shortcuts.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }

                    items(shortcuts, key = { it.id }) { shortcut ->
                        ShortcutManageCard(
                            shortcut = shortcut,
                            onUpdate = { updated ->
                                prefs.updateCustomShortcut(updated)
                                refreshShortcuts()
                            },
                            onDelete = {
                                prefs.removeCustomShortcut(shortcut.id)
                                refreshShortcuts()
                            },
                            onLaunch = {
                                ActivityLauncherHelper.launchShortcut(context, shortcut)
                            }
                        )
                    }
                }
            }
        }
    }

    // Add dialog
    selectedActivityForDialog?.let { act ->
        var customName by remember { mutableStateOf(act.label) }
        var showInSearch by remember { mutableStateOf(true) }
        var showInDrawer by remember { mutableStateOf(true) }
        var assignQuickTile by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { selectedActivityForDialog = null },
            title = { Text("Configure Shortcut") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = customName,
                        onValueChange = { customName = it },
                        label = { Text("Shortcut Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Text(
                        "${selectedPackageForDialog}/${act.componentName.className.substringAfterLast('.')}",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Show in Search", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = showInSearch, onCheckedChange = { showInSearch = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Show in App Drawer", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = showInDrawer, onCheckedChange = { showInDrawer = it })
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text("Assign to Quick Settings Tile", style = MaterialTheme.typography.bodyMedium)
                        Switch(checked = assignQuickTile, onCheckedChange = { assignQuickTile = it })
                    }
                }
            },
            confirmButton = {
                Button(onClick = {
                    val newShortcut = CustomActivityShortcut(
                        id = UUID.randomUUID().toString(),
                        name = customName.ifBlank { act.label },
                        packageName = selectedPackageForDialog,
                        activityName = act.componentName.className,
                        showInSearch = showInSearch,
                        showInDrawer = showInDrawer,
                        assignToQuickTile = assignQuickTile
                    )
                    prefs.addCustomShortcut(newShortcut)
                    refreshShortcuts()
                    selectedActivityForDialog = null
                    isBrowsingActivities = false
                    Toast.makeText(context, "Shortcut added!", Toast.LENGTH_SHORT).show()
                }) {
                    Text("Add Shortcut")
                }
            },
            dismissButton = {
                TextButton(onClick = { selectedActivityForDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
private fun AppActivityCard(
    app: ScannedApp,
    onSelectActivity: (ScannedActivity) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
    ) {
        Column(Modifier.fillMaxWidth().padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(app.appName, fontWeight = FontWeight.Bold)
                    Text(
                        "${app.activities.size} activities • ${app.packageName}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    painter = painterResource(if (expanded) R.drawable.ms_expand_more else R.drawable.ms_chevron_forward),
                    contentDescription = null
                )
            }

            AnimatedVisibility(visible = expanded) {
                Column(
                    modifier = Modifier.padding(top = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    app.activities.forEach { act ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { onSelectActivity(act) }
                                .padding(vertical = 6.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(Modifier.weight(1f)) {
                                Text(act.label, fontWeight = FontWeight.Medium, fontSize = 13.sp)
                                Text(
                                    act.componentName.className.substringAfterLast('.'),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            if (!act.isExported) {
                                Surface(
                                    shape = RoundedCornerShape(4.dp),
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Text(
                                        "Root",
                                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                        fontSize = 10.sp,
                                        color = MaterialTheme.colorScheme.onErrorContainer
                                    )
                                }
                            }
                            Spacer(Modifier.width(8.dp))
                            Icon(painterResource(R.drawable.ms_add), contentDescription = "Add", modifier = Modifier.size(18.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShortcutManageCard(
    shortcut: CustomActivityShortcut,
    onUpdate: (CustomActivityShortcut) -> Unit,
    onDelete: () -> Unit,
    onLaunch: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(Modifier.weight(1f)) {
                    Text(shortcut.name, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(
                        "${shortcut.packageName}/${shortcut.activityName.substringAfterLast('.')}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row {
                    IconButton(onClick = onLaunch) {
                        Icon(painterResource(R.drawable.ms_play_arrow), contentDescription = "Test Launch")
                    }
                    IconButton(onClick = onDelete) {
                        Icon(painterResource(R.drawable.ms_delete), contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                    }
                }
            }

            Spacer(Modifier.height(8.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            Spacer(Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Show in Search", style = MaterialTheme.typography.bodySmall)
                Checkbox(
                    checked = shortcut.showInSearch,
                    onCheckedChange = { onUpdate(shortcut.copy(showInSearch = it)) }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Show in App Drawer", style = MaterialTheme.typography.bodySmall)
                Checkbox(
                    checked = shortcut.showInDrawer,
                    onCheckedChange = { onUpdate(shortcut.copy(showInDrawer = it)) }
                )
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Quick Settings Tile", style = MaterialTheme.typography.bodySmall)
                Checkbox(
                    checked = shortcut.assignToQuickTile,
                    onCheckedChange = { onUpdate(shortcut.copy(assignToQuickTile = it)) }
                )
            }
        }
    }
}
