package rikazzz.everythinglauncher.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
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

@Composable
internal fun FeedSettingsPage(
    customPrefs: LauncherCustomPreferences,
    onBack: () -> Unit
) {
    var feedEnabled by remember { mutableStateOf(customPrefs.feedScreenEnabled) }
    var showSearch by remember { mutableStateOf(customPrefs.feedShowSearchBar) }
    var showWeather by remember { mutableStateOf(customPrefs.feedShowWeather) }
    var selectedRegionCode by remember { mutableStateOf(customPrefs.feedRegion) }
    var selectedTopicId by remember { mutableStateOf(customPrefs.feedDefaultTopic) }

    var showRegionDialog by remember { mutableStateOf(false) }
    var showTopicDialog by remember { mutableStateOf(false) }

    val currentRegionName = remember(selectedRegionCode) {
        AVAILABLE_REGIONS.firstOrNull { it.code == selectedRegionCode }?.name ?: "United States (English)"
    }
    val currentTopicName = remember(selectedTopicId) {
        NewsTopic.fromId(selectedTopicId).label
    }

    SettingsScaffold(stringResource(R.string.settings_feed_screen), "feed_screen_settings", onBack) { padding ->
        SettingsList(padding) {
            item {
                SettingsFeatureBanner(
                    title = stringResource(R.string.feed_screen_enable),
                    checked = feedEnabled,
                    tag = "feed_screen_enabled"
                ) { enabled ->
                    feedEnabled = enabled
                    customPrefs.feedScreenEnabled = enabled
                }
            }
            item {
                SettingsToggleItem(
                    title = stringResource(R.string.feed_screen_swipe),
                    summary = if (feedEnabled) null else stringResource(R.string.feed_screen_swipe_disabled),
                    checked = feedEnabled,
                    index = 0,
                    count = 5,
                    tag = "feed_screen_swipe"
                ) { enabled ->
                    feedEnabled = enabled
                    customPrefs.feedScreenEnabled = enabled
                }
            }
            item {
                SettingsToggleItem(
                    title = stringResource(R.string.feed_show_search),
                    summary = stringResource(R.string.feed_show_search_summary),
                    checked = showSearch,
                    index = 1,
                    count = 5,
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
                    index = 2,
                    count = 5,
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
                    index = 3,
                    count = 5,
                    tag = "feed_region"
                ) {
                    showRegionDialog = true
                }
            }
            item {
                SettingsActionItem(
                    title = stringResource(R.string.feed_default_topic),
                    summary = currentTopicName,
                    index = 4,
                    count = 5,
                    tag = "feed_default_topic"
                ) {
                    showTopicDialog = true
                }
            }
        }
    }

    if (showRegionDialog) {
        AlertDialog(
            onDismissRequest = { showRegionDialog = false },
            title = { Text(stringResource(R.string.feed_region)) },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                ) {
                    items(AVAILABLE_REGIONS) { region ->
                        val isSelected = region.code == selectedRegionCode
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedRegionCode = region.code
                                    customPrefs.feedRegion = region.code
                                    showRegionDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    selectedRegionCode = region.code
                                    customPrefs.feedRegion = region.code
                                    showRegionDialog = false
                                }
                            )
                            Spacer(Modifier.width(12.dp))
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

    if (showTopicDialog) {
        AlertDialog(
            onDismissRequest = { showTopicDialog = false },
            title = { Text(stringResource(R.string.feed_default_topic)) },
            text = {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 360.dp)
                ) {
                    items(NewsTopic.entries) { topic ->
                        val isSelected = topic.id == selectedTopicId
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    selectedTopicId = topic.id
                                    customPrefs.feedDefaultTopic = topic.id
                                    showTopicDialog = false
                                }
                                .padding(vertical = 10.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = {
                                    selectedTopicId = topic.id
                                    customPrefs.feedDefaultTopic = topic.id
                                    showTopicDialog = false
                                }
                            )
                            Spacer(Modifier.width(12.dp))
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
