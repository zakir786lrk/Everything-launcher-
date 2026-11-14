package rikazzz.everythinglauncher.ui.onboarding

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import rikazzz.everythinglauncher.ui.components.LauncherIcon
import rikazzz.everythinglauncher.ui.components.LauncherSymbol
import kotlinx.coroutines.launch

private data class WizardStep(
    val title: String,
    val subtitle: String,
    val symbol: LauncherSymbol,
    val bulletPoints: List<String> = emptyList()
)

@Composable
fun SetupWizardScreen(
    onDone: () -> Unit,
    modifier: Modifier = Modifier
) {
    val steps = listOf(
        WizardStep(
            title = "Welcome to Everything",
            subtitle = "A minimal, ultra-fast Android launcher designed for focus, speed, and seamless everyday productivity.",
            symbol = LauncherSymbol.Star,
            bulletPoints = listOf(
                "Pure minimalism with zero clutter",
                "Lightning fast search and app launching",
                "Designed for one-handed operation"
            )
        ),
        WizardStep(
            title = "Intuitive Navigation",
            subtitle = "Everything you need is just one gesture away from your home screen.",
            symbol = LauncherSymbol.Gesture,
            bulletPoints = listOf(
                "Swipe Left: Browse personalized Google News Feed",
                "Swipe Right: Access your widgets screen",
                "Swipe Up: Fast searchable app drawer",
                "Everything Button: Customizable floating shortcut"
            )
        ),
        WizardStep(
            title = "Clean Home & Overflow",
            subtitle = "Keep your home screen minimal and beautiful without losing access to your apps.",
            symbol = LauncherSymbol.Folder,
            bulletPoints = listOf(
                "Favorite apps fit neatly without awkward scrolling",
                "Extra apps automatically group into a 'More' folder",
                "Custom shortcuts for sub-settings & hidden activities"
            )
        ),
        WizardStep(
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

    val pagerState = rememberPagerState { steps.size }
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
                if (pagerState.currentPage < steps.size - 1) {
                    TextButton(onClick = onDone) {
                        Text("Skip", color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                } else {
                    Spacer(Modifier.height(48.dp))
                }
            }

            Spacer(Modifier.height(16.dp))

            // Main Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) { page ->
                val step = steps[page]
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        LauncherIcon(
                            symbol = step.symbol,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.size(48.dp)
                        )
                    }

                    Spacer(Modifier.height(32.dp))

                    Text(
                        text = step.title,
                        style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )

                    Spacer(Modifier.height(16.dp))

                    Text(
                        text = step.subtitle,
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        lineHeight = 24.sp
                    )

                    if (step.bulletPoints.isNotEmpty()) {
                        Spacer(Modifier.height(28.dp))
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            step.bulletPoints.forEach { point ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 16.dp)
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

            Spacer(Modifier.height(24.dp))

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
                    repeat(steps.size) { index ->
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
                if (pagerState.currentPage == steps.size - 1) {
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
