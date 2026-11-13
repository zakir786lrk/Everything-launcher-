package rikazzz.everythinglauncher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import rikazzz.everythinglauncher.data.LauncherApp
import rikazzz.everythinglauncher.data.LauncherFolder
import rikazzz.everythinglauncher.ui.LauncherUiState
import rikazzz.everythinglauncher.ui.components.AppIcon
import rikazzz.everythinglauncher.ui.components.AppStack
import rikazzz.everythinglauncher.ui.theme.LocalLauncherAppearance

@Composable
fun HomeDock(
    apps: List<LauncherApp>,
    folders: List<LauncherFolder>,
    uiState: LauncherUiState,
    onLaunchApp: (LauncherApp, Rect) -> Unit,
    onAppDetails: (LauncherApp) -> Unit,
    onOpenFolder: (LauncherFolder, Rect) -> Unit,
    onEditFolder: (LauncherFolder) -> Unit,
    modifier: Modifier = Modifier,
    dockLabelsEnabled: Boolean = true,
    backgroundStyle: String = "glass"
) {
    if (apps.isEmpty() && folders.isEmpty()) return

    val appearance = LocalLauncherAppearance.current
    val iconSize = 42.dp * (appearance.iconSize / 100f)
    val containerColor = when (backgroundStyle) {
        "solid" -> MaterialTheme.colorScheme.surfaceContainerHigh
        "transparent" -> androidx.compose.ui.graphics.Color.Transparent
        else -> MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.35f)
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        shape = RoundedCornerShape(28.dp),
        color = containerColor,
        tonalElevation = if (backgroundStyle == "transparent") 0.dp else 4.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 6.dp)
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            folders.filter { it.isAppStack }.take(2).forEach { folder ->
                AppStack(
                    stack = folder,
                    uiState = uiState,
                    onLaunch = { app, rect -> onLaunchApp(app, rect) },
                    onDetails = onAppDetails,
                    onEdit = { onEditFolder(folder) },
                    modifier = Modifier.widthIn(max = 160.dp),
                    iconSize = 36.dp
                )
            }

            apps.take(5).forEach { app ->
                var bounds by remember { mutableStateOf(Rect.Zero) }
                Column(
                    modifier = Modifier
                        .padding(horizontal = 6.dp)
                        .widthIn(min = 52.dp, max = 64.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .combinedClickable(
                            onClick = { onLaunchApp(app, bounds) },
                            onLongClick = { onAppDetails(app) }
                        )
                        .padding(vertical = 4.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(iconSize)
                            .onGloballyPositioned { bounds = it.boundsInWindow() },
                        contentAlignment = Alignment.Center
                    ) {
                        AppIcon(app, Modifier.fillMaxSize(), size = iconSize)
                    }
                    if (dockLabelsEnabled) {
                        Text(
                            text = app.label,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            color = appearance.text,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
