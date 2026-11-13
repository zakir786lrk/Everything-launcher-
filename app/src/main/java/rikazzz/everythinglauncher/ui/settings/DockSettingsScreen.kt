package rikazzz.everythinglauncher.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import rikazzz.everythinglauncher.R
import rikazzz.everythinglauncher.ai.AiPreferences
import rikazzz.everythinglauncher.ui.LauncherActions
import rikazzz.everythinglauncher.ui.LauncherUiState
import rikazzz.everythinglauncher.ui.components.LauncherIcon
import rikazzz.everythinglauncher.ui.components.LauncherSymbol

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DockSettingsScreen(
    uiState: LauncherUiState,
    actions: LauncherActions,
    onBack: () -> Unit,
    onManageStacks: () -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { AiPreferences(context) }

    var showDock by remember { mutableStateOf(prefs.showDock) }
    var dockLabels by remember { mutableStateOf(prefs.dockLabelsEnabled) }
    var stackLabels by remember { mutableStateOf(prefs.stackLabelsEnabled) }
    var bgStyle by remember { mutableStateOf(prefs.dockBackgroundStyle) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Bottom Dock & App Stacks") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(painterResource(R.drawable.ms_arrow_back), contentDescription = "Back")
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Text(
                    text = "Dock Configuration",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            // Enable Dock Switch
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Enable Fixed Bottom Dock", fontWeight = FontWeight.Bold)
                            Text(
                                "Fixes favorite apps and stackable folders at the bottom of the home screen",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
                            checked = showDock,
                            onCheckedChange = {
                                showDock = it
                                prefs.showDock = it
                            }
                        )
                    }
                }
            }

            // Show Labels in Dock
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Show App Names in Dock", fontWeight = FontWeight.Bold)
                            Text(
                                "Displays the app title underneath each icon in the dock",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
                            checked = dockLabels,
                            onCheckedChange = {
                                dockLabels = it
                                prefs.dockLabelsEnabled = it
                            }
                        )
                    }
                }
            }

            // Show Labels in Stackable Apps
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f))
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Show App Names in App Stacks", fontWeight = FontWeight.Bold)
                            Text(
                                "Displays the app title underneath each icon inside stackable folders",
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
                            checked = stackLabels,
                            onCheckedChange = {
                                stackLabels = it
                                prefs.stackLabelsEnabled = it
                            }
                        )
                    }
                }
            }

            // Dock Background Style
            item {
                Text("Dock Container Background Style", style = MaterialTheme.typography.labelLarge)
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = bgStyle == "glass",
                        onClick = {
                            bgStyle = "glass"
                            prefs.dockBackgroundStyle = "glass"
                        },
                        label = { Text("Translucent Glass") }
                    )
                    FilterChip(
                        selected = bgStyle == "solid",
                        onClick = {
                            bgStyle = "solid"
                            prefs.dockBackgroundStyle = "solid"
                        },
                        label = { Text("Solid Surface") }
                    )
                    FilterChip(
                        selected = bgStyle == "transparent",
                        onClick = {
                            bgStyle = "transparent"
                            prefs.dockBackgroundStyle = "transparent"
                        },
                        label = { Text("Transparent") }
                    )
                }
            }

            // Manage App Stacks button
            item {
                Spacer(Modifier.height(8.dp))
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onManageStacks() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            LauncherIcon(LauncherSymbol.Steppers, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                            Spacer(Modifier.width(12.dp))
                            Column {
                                Text("Manage Stackable Apps", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
                                Text("Create and organize apps into stacks", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer)
                            }
                        }
                        Icon(painterResource(R.drawable.ms_chevron_forward), contentDescription = null, tint = MaterialTheme.colorScheme.onPrimaryContainer)
                    }
                }
            }
        }
    }
}
