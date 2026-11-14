package rikazzz.everythinglauncher.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import rikazzz.everythinglauncher.R
import rikazzz.everythinglauncher.data.LauncherCustomPreferences
import rikazzz.everythinglauncher.data.feed.AVAILABLE_REGIONS
import rikazzz.everythinglauncher.data.feed.NewsTopic
import rikazzz.everythinglauncher.ui.LauncherActions
import rikazzz.everythinglauncher.ui.LauncherUiState
import rikazzz.everythinglauncher.ui.components.LauncherIcon
import rikazzz.everythinglauncher.ui.components.LauncherSymbol

@Composable
internal fun FeedSettingsPage(
    uiState: LauncherUiState,
    actions: LauncherActions,
    customPrefs: LauncherCustomPreferences,
    onBack: () -> Unit
) {
    var feedEnabled by remember { mutableStateOf(customPrefs.feedScreenEnabled) }
    var widgetEnabled by remember { mutableStateOf(uiState.settings.widgetScreen.enabled) }
    var swipeAppRight by remember { mutableStateOf(customPrefs.swipeAppRightToOpenPopup) }
    var showSearch by remember { mutableStateOf(customPrefs.feedShowSearchBar) }
    var showWeather by remember { mutableStateOf(customPrefs.feedShowWeather) }
    var selectedRegionCode by remember { mutableStateOf(customPrefs.feedRegion) }
    var selectedTopicId by remember { mutableStateOf(customPrefs.feedDefaultTopic) }

    var showRegionDialog by remember { mutableStateOf(false) }
    var showCustomRegionDialog by remember { mutableStateOf(false) }
    var customRegionInput by remember { mutableStateOf("") }
    var showTopicDialog by remember { mutableStateOf(false) }

    val currentRegionName = remember(selectedRegionCode) {
        AVAILABLE_REGIONS.firstOrNull { it.code.equals(selectedRegionCode, ignoreCase = true) }?.name ?: "Custom ($selectedRegionCode)"
    }
    val currentTopicName = remember(selectedTopicId) {
        NewsTopic.fromId(selectedTopicId).label
    }

    SettingsScaffold("Feed & Home Pages", "feed_screen_settings", onBack) { padding ->
        SettingsList(padding) {
            // 1. Pages Configuration
            item { SettingsHeading("Home Screen Pages") }
            item {
                SettingsToggleItem(
                    title = "Left Page: Google News Feed",
                    summary = if (feedEnabled) "Accessible by swiping right from Home" else "Left page is hidden",
                    checked = feedEnabled,
                    index = 0,
                    count = 2,
                    tag = "feed_page_toggle",
                    leading = { LauncherIcon(LauncherSymbol.Outbound) }
                ) { enabled ->
                    feedEnabled = enabled
                    customPrefs.feedScreenEnabled = enabled
                }
            }
            item {
                SettingsToggleItem(
                    title = "Right Page: Widget Screen",
                    summary = if (widgetEnabled) "Accessible by swiping left from Home" else "Right page is hidden",
                    checked = widgetEnabled,
                    index = 1,
                    count = 2,
                    tag = "widget_page_toggle",
                    leading = { LauncherIcon(LauncherSymbol.Widgets) }
                ) { enabled ->
                    widgetEnabled = enabled
                    actions.updateSettings { it.copy(widgetScreen = it.widgetScreen.copy(enabled = enabled)) }
                }
            }

            // 2. Gesture Handling
            item { SettingsHeading("Page Swipe Gestures") }
            item {
                SettingsToggleItem(
                    title = "Swipe App Right for Shortcuts",
                    summary = if (swipeAppRight) "Enabled (may intercept swipe-to-feed when starting drag on an app)"
                              else "Disabled (recommended: allows smooth right-swipe across entire home screen to open Feed)",
                    checked = swipeAppRight,
                    index = 0,
                    count = 1,
                    tag = "swipe_app_right_toggle",
                    leading = { LauncherIcon(LauncherSymbol.Gesture) }
                ) { enabled ->
                    swipeAppRight = enabled
                    customPrefs.swipeAppRightToOpenPopup = enabled
                }
            }

            // 3. News Feed Content
            if (feedEnabled) {
                item { SettingsHeading("Google News Feed Options") }
                item {
                    SettingsToggleItem(
                        title = stringResource(R.string.feed_show_search),
                        summary = stringResource(R.string.feed_show_search_summary),
                        checked = showSearch,
                        index = 0,
                        count = 4,
                        tag = "feed_show_search"
                    ) { show ->
                        showSearch = show
                        customPrefs.feedShowSearchBar = show
                    }
                }
                item {
                    SettingsToggleItem(
                        title = stringResource(R.string.feed_show_weather),
                        summary = stringResource(R.string.feed_show_weather_summary),
                        checked = showWeather,
                        index = 1,
                        count = 4,
                        tag = "feed_show_weather"
                    ) { show ->
                        showWeather = show
                        customPrefs.feedShowWeather = show
                    }
                }
                item {
                    SettingsActionItem(
                        title = stringResource(R.string.feed_region),
                        summary = currentRegionName,
                        index = 2,
                        count = 4,
                        tag = "feed_region"
                    ) {
                        showRegionDialog = true
                    }
                }
                item {
                    SettingsActionItem(
                        title = stringResource(R.string.feed_default_topic),
                        summary = currentTopicName,
                        index = 3,
                        count = 4,
                        tag = "feed_default_topic"
                    ) {
                        showTopicDialog = true
                    }
                }
            }
        }
    }

    if (showRegionDialog) {
        AlertDialog(
            onDismissRequest = { showRegionDialog = false },
            title = { Text(stringResource(R.string.feed_region)) },
            text = {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 350.dp)) {
                    item {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    showRegionDialog = false
                                    customRegionInput = selectedRegionCode
                                    showCustomRegionDialog = true
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = AVAILABLE_REGIONS.none { it.code.equals(selectedRegionCode, ignoreCase = true) },
                                onClick = {
                                    showRegionDialog = false
                                    customRegionInput = selectedRegionCode
                                    showCustomRegionDialog = true
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Custom Country / Language...", style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.primary)
                        }
                    }
                    items(AVAILABLE_REGIONS) { region ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedRegionCode = region.code
                                    customPrefs.feedRegion = region.code
                                    showRegionDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedRegionCode.equals(region.code, ignoreCase = true),
                                onClick = {
                                    selectedRegionCode = region.code
                                    customPrefs.feedRegion = region.code
                                    showRegionDialog = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(region.name, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showRegionDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    if (showCustomRegionDialog) {
        AlertDialog(
            onDismissRequest = { showCustomRegionDialog = false },
            title = { Text("Custom Feed Region") },
            text = {
                androidx.compose.foundation.layout.Column {
                    Text(
                        "Enter country and language code format (e.g. US:en, GB:en, PK:ur, NL:nl, KE:en):",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(Modifier.height(12.dp))
                    androidx.compose.material3.OutlinedTextField(
                        value = customRegionInput,
                        onValueChange = { customRegionInput = it },
                        singleLine = true,
                        placeholder = { Text("COUNTRY:lang (e.g. US:en)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val code = customRegionInput.trim()
                    if (code.isNotEmpty()) {
                        selectedRegionCode = code
                        customPrefs.feedRegion = code
                    }
                    showCustomRegionDialog = false
                }) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { showCustomRegionDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }

    if (showTopicDialog) {
        AlertDialog(
            onDismissRequest = { showTopicDialog = false },
            title = { Text(stringResource(R.string.feed_default_topic)) },
            text = {
                LazyColumn(Modifier.fillMaxWidth().heightIn(max = 350.dp)) {
                    items(NewsTopic.entries) { topic ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedTopicId = topic.id
                                    customPrefs.feedDefaultTopic = topic.id
                                    showTopicDialog = false
                                }
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = selectedTopicId == topic.id,
                                onClick = {
                                    selectedTopicId = topic.id
                                    customPrefs.feedDefaultTopic = topic.id
                                    showTopicDialog = false
                                }
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(topic.label, style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showTopicDialog = false }) {
                    Text(stringResource(android.R.string.cancel))
                }
            }
        )
    }
}
