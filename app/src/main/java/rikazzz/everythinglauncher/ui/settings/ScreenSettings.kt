package rikazzz.everythinglauncher.ui.settings

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import rikazzz.everythinglauncher.R
import rikazzz.everythinglauncher.ui.LauncherActions
import rikazzz.everythinglauncher.ui.LauncherUiState

@Composable
internal fun ScreenSettings(uiState: LauncherUiState, actions: LauncherActions, onBack: () -> Unit) {
    SettingsScaffold(stringResource(R.string.settings_screens), "settings_screens", onBack) { padding ->
        SettingsList(padding) {
            item { Spacer(Modifier.height(24.dp)) }
            item {
                SettingsToggleItem(stringResource(R.string.settings_allow_screen_rotation),
                    stringResource(R.string.settings_allow_screen_rotation_summary), uiState.settings.allowScreenRotation,
                    0, 2, "allow_screen_rotation") { enabled ->
                    actions.updateSettings { it.copy(allowScreenRotation = enabled) }
                }
            }
            item {
                SettingsToggleItem(stringResource(R.string.settings_remember_screen_position),
                    stringResource(R.string.settings_remember_screen_position_summary), uiState.settings.rememberScreenPosition,
                    1, 2, "remember_screen_position") { enabled ->
                    actions.updateSettings { it.copy(rememberScreenPosition = enabled) }
                }
            }
        }
    }
}
