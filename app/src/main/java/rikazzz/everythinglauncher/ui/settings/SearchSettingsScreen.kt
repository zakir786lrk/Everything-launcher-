package rikazzz.everythinglauncher.ui.settings

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import rikazzz.everythinglauncher.R
import rikazzz.everythinglauncher.data.SearchContacts
import rikazzz.everythinglauncher.data.SearchSettings
import rikazzz.everythinglauncher.ui.LauncherActions
import rikazzz.everythinglauncher.ui.LauncherUiState

@Composable
internal fun SearchSettingsScreen(uiState: LauncherUiState, actions: LauncherActions, onBack: () -> Unit) {
    val settings = uiState.settings.search
    val context = LocalContext.current
    val customPrefs = remember(context) { rikazzz.everythinglauncher.data.LauncherCustomPreferences(context) }
    var calcEnabled by remember { mutableStateOf(customPrefs.searchCalculatorEnabled) }
    var devSettingsEnabled by remember { mutableStateOf(customPrefs.searchDeviceSettingsEnabled) }
    var webSugEnabled by remember { mutableStateOf(customPrefs.searchWebSuggestionsEnabled) }
    var chipsEnabled by remember { mutableStateOf(customPrefs.searchEngineChipsEnabled) }
    var customShortcutsEnabled by remember { mutableStateOf(customPrefs.searchCustomShortcutsEnabled) }
    var recentInstalledEnabled by remember { mutableStateOf(customPrefs.searchShowRecentlyInstalled) }
    var recentOpenedEnabled by remember { mutableStateOf(customPrefs.searchShowRecentlyOpened) }
    var freqOpenedEnabled by remember { mutableStateOf(customPrefs.searchShowFrequentlyOpened) }
    var hiddenChips by remember { mutableStateOf(customPrefs.hiddenEngineChips) }
    var showChipsDialog by remember { mutableStateOf(false) }

    val contacts = remember(context) { SearchContacts(context) }
    var hasAccess by remember { mutableStateOf(contacts.hasAccess()) }
    var permissionRequested by rememberSaveable { mutableStateOf(false) }

    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, contacts) {
        val observer = LifecycleEventObserver { _, event -> if (event == Lifecycle.Event.ON_RESUME) hasAccess = contacts.hasAccess() }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }

    fun update(change: (SearchSettings) -> SearchSettings) = actions.updateSettings { it.copy(search = change(it.search)) }

    fun permissionResult(granted: Boolean) {
        hasAccess = granted
        update { it.copy(contacts = granted) }
        if (!granted) Toast.makeText(context, R.string.search_contacts_permission, Toast.LENGTH_LONG).show()
    }

    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission(), ::permissionResult)
    val systemSettings = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {
        permissionResult(contacts.hasAccess())
    }

    SettingsScaffold(stringResource(R.string.settings_search), "settings_search", onBack) { padding ->
        SettingsList(padding) {
            item {
                SettingsFeatureBanner(stringResource(R.string.search_enable), settings.enabled, "search_enabled") { value ->
                    update { it.copy(enabled = value) }
                }
            }

            // 1. Search Content & Sources
            item { SettingsHeading("Search Content & Sources") }
            item {
                SettingsToggleItem(stringResource(R.string.search_contacts), "Search contacts with direct Call and SMS actions",
                    settings.contacts && hasAccess, 0, 6, "search_contacts", enabled = settings.enabled) { value ->
                    if (!value || contacts.hasAccess()) update { it.copy(contacts = value) }
                    else if (permissionRequested && (context as? Activity)?.let {
                        ActivityCompat.shouldShowRequestPermissionRationale(it, Manifest.permission.READ_CONTACTS)
                    } == false) {
                        systemSettings.launch(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null)))
                    } else {
                        permissionRequested = true
                        permission.launch(Manifest.permission.READ_CONTACTS)
                    }
                }
            }
            item {
                SettingsToggleItem("Device Settings Search", "Search and open system settings (Wi-Fi, Bluetooth, Battery, etc.)",
                    devSettingsEnabled, 1, 6, "search_dev_settings", enabled = settings.enabled) {
                    devSettingsEnabled = it
                    customPrefs.searchDeviceSettingsEnabled = it
                }
            }
            item {
                SettingsToggleItem("Math Calculator", "Evaluate math expressions directly in search results",
                    calcEnabled, 2, 6, "search_calc", enabled = settings.enabled) {
                    calcEnabled = it
                    customPrefs.searchCalculatorEnabled = it
                }
            }
            item {
                SettingsToggleItem("Custom Activity Shortcuts", "Include user-created custom activity shortcuts in search",
                    customShortcutsEnabled, 3, 6, "search_custom_shortcuts", enabled = settings.enabled) {
                    customShortcutsEnabled = it
                    customPrefs.searchCustomShortcutsEnabled = it
                }
            }
            item {
                SettingsToggleItem(stringResource(R.string.search_hidden_apps), stringResource(R.string.search_hidden_apps_summary),
                    settings.hiddenApps, 4, 6, "search_hidden_apps", enabled = settings.enabled) { value -> update { it.copy(hiddenApps = value) } }
            }
            item {
                SettingsToggleItem(stringResource(R.string.search_fuzzy), stringResource(R.string.search_fuzzy_summary),
                    settings.fuzzy, 5, 6, "search_fuzzy", enabled = settings.enabled) { value -> update { it.copy(fuzzy = value) } }
            }

            // 2. App Suggestions in Search
            item { SettingsHeading("App Suggestions in Search") }
            item {
                SettingsToggleItem("Recently Opened Apps", "Show recently launched apps when search query is empty",
                    recentOpenedEnabled, 0, 3, "search_recent_opened", enabled = settings.enabled) {
                    recentOpenedEnabled = it
                    customPrefs.searchShowRecentlyOpened = it
                }
            }
            item {
                SettingsToggleItem("Frequently Opened Apps", "Show most frequently used apps when search query is empty",
                    freqOpenedEnabled, 1, 3, "search_freq_opened", enabled = settings.enabled) {
                    freqOpenedEnabled = it
                    customPrefs.searchShowFrequentlyOpened = it
                }
            }
            item {
                SettingsToggleItem("Recently Installed Apps", "Show newly installed apps when search query is empty",
                    recentInstalledEnabled, 2, 3, "search_recent_installed", enabled = settings.enabled) {
                    recentInstalledEnabled = it
                    customPrefs.searchShowRecentlyInstalled = it
                }
            }

            // 3. Web & AI Search
            item { SettingsHeading("Web & AI Search") }
            item {
                SettingsToggleItem("Web Search Suggestions", "Instant autocomplete search suggestions from Google",
                    webSugEnabled, 0, if (chipsEnabled) 3 else 2, "search_web_sug", enabled = settings.enabled) {
                    webSugEnabled = it
                    customPrefs.searchWebSuggestionsEnabled = it
                }
            }
            item {
                SettingsToggleItem("Search Engine Chips", "Show quick engine chips (ChatGPT, Gemini, Google, YouTube, Reddit, etc.)",
                    chipsEnabled, 1, if (chipsEnabled) 3 else 2, "search_engine_chips", enabled = settings.enabled) {
                    chipsEnabled = it
                    customPrefs.searchEngineChipsEnabled = it
                }
            }
            if (chipsEnabled) {
                item {
                    SettingsActionItem("Customize Chips", "Show or hide specific search engine chips",
                        2, 3, "search_chips_customize", enabled = settings.enabled) {
                        showChipsDialog = true
                    }
                }
            }
        }
    }

    if (showChipsDialog) {
        AlertDialog(
            onDismissRequest = { showChipsDialog = false },
            title = { Text("Search Engine Chips") },
            text = {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 350.dp)) {
                    items(rikazzz.everythinglauncher.quicksearch.QuickSearchEngineRegistry.engines) { engine ->
                        val isVisible = engine.id !in hiddenChips
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    val updated = if (isVisible) hiddenChips + engine.id else hiddenChips - engine.id
                                    hiddenChips = updated
                                    customPrefs.hiddenEngineChips = updated
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = isVisible,
                                onCheckedChange = { checked ->
                                    val updated = if (!checked) hiddenChips + engine.id else hiddenChips - engine.id
                                    hiddenChips = updated
                                    customPrefs.hiddenEngineChips = updated
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(engine.name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showChipsDialog = false }) {
                    Text("Done")
                }
            }
        )
    }
}
