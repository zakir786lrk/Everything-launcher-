package rikazzz.everythinglauncher.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import rikazzz.everythinglauncher.ui.LauncherActions
import rikazzz.everythinglauncher.ui.LauncherUiState
import rikazzz.everythinglauncher.ui.components.LauncherIcon
import rikazzz.everythinglauncher.ui.components.LauncherSymbol
import kotlinx.coroutines.launch

private enum class WizardPageType {
    Welcome,
    Permissions,
    Gestures,
    CleanHome,
    Ready
}

private data class WizardPage(
    val type: WizardPageType,
    val title: String,
    val subtitle: String,
    val symbol: LauncherSymbol,
    val bulletPoints: List<String> = emptyList()
)

@Composable
fun SetupWizardScreen(
    onDone: () -> Unit,
    actions: LauncherActions? = null,
    uiState: LauncherUiState? = null,
    modifier: Modifier = Modifier
) {
    val pages = listOf(
        WizardPage(
            type = WizardPageType.Welcome,
            title = "Welcome to Everything",
            subtitle = "A minimal, ultra-fast Android launcher designed for focus, speed, and seamless everyday productivity.",
            symbol = LauncherSymbol.Star,
            bulletPoints = listOf(
                "Pure minimalism with zero clutter",
                "Lightning fast search and app launching",
                "Designed for one-handed operation"
            )
        ),
        WizardPage(
            type = WizardPageType.Permissions,
            title = "Permissions & Setup",
            subtitle = "Grant core permissions so Everything Launcher can deliver instant search, notifications, and dynamic widgets.",
            symbol = LauncherSymbol.Lock
        ),
        WizardPage(
            type = WizardPageType.Gestures,
            title = "Intuitive Navigation",
            subtitle = "Everything you need is just one gesture away from your home screen.",
            symbol = LauncherSymbol.Gesture,
            bulletPoints = listOf(
                "Swipe Left: Personalized Google News Feed",
                "Swipe Right: Dedicated widgets screen",
                "Swipe Up: Fast searchable app drawer",
                "Everything Button: Customizable floating shortcut"
            )
        ),
        WizardPage(
            type = WizardPageType.CleanHome,
            title = "Clean Home & Dynamic Apps",
            subtitle = "Keep your home screen minimal and beautiful without losing access to your apps.",
            symbol = LauncherSymbol.Folder,
            bulletPoints = listOf(
                "Favorite apps fit neatly without awkward scrolling",
                "Extra apps automatically group into 'More' Star folder",
                "Dynamic apps: Recently installed, opened & frequently used",
                "Preloaded quick settings shortcuts for Wi-Fi, Bluetooth & more"
            )
        ),
        WizardPage(
            type = WizardPageType.Ready,
            title = "Ready to Personalize!",
            subtitle = "Configure themes, web suggestions, and back up your settings anytime.",
            symbol = LauncherSymbol.Palette,
            bulletPoints = listOf(
                "Material You dynamic colors and AMOLED dark theme",
                "Instant settings backup and restore with timestamp",
                "Customize gestures, search engines, and fonts"
            )
        )
    )

    val pagerState = rememberPagerState { pages.size }
    val scope = rememberCoroutineScope()

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top Bar with Skip Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (pagerState.currentPage < pages.size - 1) {
                    TextButton(onClick = onDone) {
                        Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Spacer(Modifier.height(48.dp))
                }
            }

            Spacer(Modifier.height(8.dp))

            // Main Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { pageIndex ->
                val page = pages[pageIndex]

                if (page.type == WizardPageType.Permissions) {
                    PermissionsSetupStep(actions = actions, uiState = uiState)
                } else {
                    StandardIntroStep(page = page)
                }
            }

            Spacer(Modifier.height(20.dp))

            // Bottom Navigation & Dots
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Page Indicator Dots
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pages.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        val width = if (isSelected) 24.dp else 8.dp
                        val color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
                        Box(
                            modifier = Modifier
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(color)
                        )
                    }
                }

                // Next or Get Started Button
                if (pagerState.currentPage == pages.size - 1) {
                    Button(
                        onClick = onDone,
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Text("Get Started", fontWeight = FontWeight.Bold)
                    }
                } else {
                    FilledTonalButton(
                        onClick = {
                            scope.launch {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                            }
                        },
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Text("Next")
                    }
                }
            }
        }
    }
}

@Composable
private fun StandardIntroStep(page: WizardPage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 8.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(90.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            LauncherIcon(
                symbol = page.symbol,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(44.dp)
            )
        }

        Spacer(Modifier.height(24.dp))

        Text(
            text = page.title,
            style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(12.dp))

        Text(
            text = page.subtitle,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 22.sp
        )

        if (page.bulletPoints.isNotEmpty()) {
            Spacer(Modifier.height(24.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                page.bulletPoints.forEach { point ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 12.dp)
                    ) {
                        Box(
                            Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            text = point,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun PermissionsSetupStep(
    actions: LauncherActions?,
    uiState: LauncherUiState?
) {
    val isDefaultHome = uiState?.isDefaultHome == true
    val hasMediaAccess = uiState?.media?.hasAccess == true
    val hasUsageAccess = uiState?.usage?.hasAccess == true

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 4.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            LauncherIcon(
                symbol = LauncherSymbol.Lock,
                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                modifier = Modifier.size(40.dp)
            )
        }

        Spacer(Modifier.height(16.dp))

        Text(
            text = "Permissions & Setup",
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(8.dp))

        Text(
            text = "Enable permissions to unlock launcher gestures, media controls, and smart dynamic app recommendations.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )

        Spacer(Modifier.height(20.dp))

        // Card 1: Default Launcher
        PermissionCard(
            title = "Default Launcher",
            description = "Enables smooth navigation gestures, home return, and deep app shortcuts.",
            symbol = LauncherSymbol.Home,
            isGranted = isDefaultHome,
            actionLabel = "Set Default",
            onAction = { actions?.requestDefaultHome?.invoke() }
        )

        Spacer(Modifier.height(12.dp))

        // Card 2: Notification & Media
        PermissionCard(
            title = "Notification & Media Access",
            description = "Shows live now-playing player controls and unread notification preview badges.",
            symbol = LauncherSymbol.Notifications,
            isGranted = hasMediaAccess,
            actionLabel = "Grant Access",
            onAction = { actions?.requestMediaAccess?.invoke() }
        )

        Spacer(Modifier.height(12.dp))

        // Card 3: Usage Stats Access
        PermissionCard(
            title = "Usage Access",
            description = "Enables dynamic app folders (frequently opened and recently used app sorting).",
            symbol = LauncherSymbol.History2,
            isGranted = hasUsageAccess,
            actionLabel = "Grant Access",
            onAction = { actions?.requestUsageAccess?.invoke() }
        )
    }
}

@Composable
private fun PermissionCard(
    title: String,
    description: String,
    symbol: LauncherSymbol,
    isGranted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(CircleShape)
                    .background(
                        if (isGranted) MaterialTheme.colorScheme.primaryContainer
                        else MaterialTheme.colorScheme.surfaceContainerHighest
                    ),
                contentAlignment = Alignment.Center
            ) {
                LauncherIcon(
                    symbol = if (isGranted) LauncherSymbol.Check else symbol,
                    tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(2.dp))
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
            }

            Spacer(Modifier.width(10.dp))

            if (isGranted) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                ) {
                    Text(
                        text = "Active",
                        color = MaterialTheme.colorScheme.primary,
                        style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            } else {
                Button(
                    onClick = onAction,
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                ) {
                    Text(actionLabel, style = MaterialTheme.typography.labelMedium)
                }
            }
        }
    }
}
