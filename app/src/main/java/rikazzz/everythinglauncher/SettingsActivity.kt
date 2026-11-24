package rikazzz.everythinglauncher

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import rikazzz.everythinglauncher.ui.LauncherAppTheme
import rikazzz.everythinglauncher.ui.LauncherRoute
import rikazzz.everythinglauncher.ui.LauncherViewModel

/** The only MAIN/LAUNCHER entry. HOME remains a separate system-owned task. */
class SettingsActivity : ComponentActivity() {
    private val launcherViewModel: LauncherViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge(navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT))
        setContent {
            LauncherAppTheme(launcherViewModel) {
                LauncherRoute(launcherViewModel, settingsOnly = true, onCloseSettings = ::finish)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        launcherViewModel.refreshOnResume()
    }
}
