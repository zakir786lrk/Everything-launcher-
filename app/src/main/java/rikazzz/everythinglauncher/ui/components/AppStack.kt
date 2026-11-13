package rikazzz.everythinglauncher.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntOffsetAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import rikazzz.everythinglauncher.R
import rikazzz.everythinglauncher.data.*
import rikazzz.everythinglauncher.ui.LauncherUiState
import rikazzz.everythinglauncher.ui.theme.LocalLauncherAppearance

/** One inline container for home, drawer, folders and the widget grid. No visible name. */
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun AppStack(
    stack: LauncherFolder, uiState: LauncherUiState,
    onLaunch: (LauncherApp, Rect) -> Unit, onDetails: (LauncherApp) -> Unit, onEdit: () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true, gridManaged: Boolean = false,
    expanded: Boolean? = null, onExpandedChange: ((Boolean) -> Unit)? = null,
    iconSize: Dp = 40.dp, onWallpaper: Boolean = true,
) {
    val inputEnabled = enabled && LocalLauncherInputEnabled.current
    val appearance = LocalLauncherAppearance.current
    val displayedIconSize = iconSize * (appearance.iconSize / 100f)
    val symbolColor = if (onWallpaper) appearance.text else LocalContentColor.current
    val apps = uiState.popupItems(uiState.folderItem(stack), emptyList()).mapNotNull { entry ->
        if (entry.widget != null) null else uiState.findItem(entry.key)?.takeIf { it.folderId == null }
    }
    var localExpanded by rememberSaveable(stack.id) { mutableStateOf(false) }
    val open = expanded ?: localExpanded
    val expandLabel = stringResource(if (open) R.string.app_stack_collapse else R.string.app_stack_expand)
    val editLabel = stringResource(R.string.app_stack_edit)
    BoxWithConstraints(modifier.fillMaxWidth().testTag("app_stack:${stack.id}")) {
        val density = LocalDensity.current
        val columns = appStackColumns(maxWidth.value, displayedIconSize.value)
        val inset = AppStackInset.dp
        // Anchor the first icon's LEFT edge, rather than centering it in a wide cell.
        val context = androidx.compose.ui.platform.LocalContext.current
        val showLabels = remember(context) { rikazzz.everythinglauncher.data.LauncherCustomPreferences(context).stackLabelsEnabled }
        val labelExtra = if (showLabels) 14.dp else 0.dp
        val step = (maxWidth - inset * 2 - displayedIconSize) / (columns - 1)
        val rowStep = displayedIconSize + labelExtra + AppStackRowGap.dp
        val overflow = apps.size > columns
        val rows = appStackRows(apps.size, columns, open)
        val height by animateDpAsState(appStackHeight(rows, displayedIconSize.value, showLabels).dp,
            MaterialTheme.motionScheme.defaultSpatialSpec(), label = "stackHeight")
        val radius by animateDpAsState(if (open && overflow) 16.dp else displayedIconSize / 2 + inset,
            MaterialTheme.motionScheme.defaultSpatialSpec(), label = "stackShape")
        Box(Modifier.fillMaxWidth().height(height).clip(RoundedCornerShape(radius))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = if (onWallpaper) 0.25f else 1f))
            .then(if (gridManaged) Modifier else Modifier.combinedClickable(enabled = inputEnabled,
                onClick = {}, onLongClick = onEdit))
            .semantics {
                contentDescription = stack.name
                customActions = listOf(CustomAccessibilityAction(editLabel) { if (inputEnabled) onEdit(); inputEnabled })
            }) {
            apps.forEachIndexed { index, app -> key(app.key) {
                val visible = open || !overflow || index < columns - 1
                val alpha by animateFloatAsState(if (visible) 1f else 0f,
                    MaterialTheme.motionScheme.defaultEffectsSpec(), label = "stackIcon")
                var bounds by remember { mutableStateOf(Rect.Zero) }
                val interactions = remember { MutableInteractionSource() }
                val rippleMask = app.icon.takeUnless { appearance.themedIcons && app.monochromeIcon != null }
                Column(
                    modifier = Modifier
                        .offset(x = inset + step * (index % columns) - 4.dp, y = inset + rowStep * (index / columns))
                        .width(displayedIconSize + 8.dp)
                        .graphicsLayer { this.alpha = alpha }
                        .then(if (!visible) Modifier.clearAndSetSemantics {} else Modifier),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(Modifier.size(displayedIconSize)
                        .combinedClickable(enabled = inputEnabled && visible,
                            interactionSource = interactions, indication = null,
                            onClick = { onLaunch(app, bounds) },
                            onLongClick = if (gridManaged) null else ({ onDetails(app) }))
                        .semantics { if (visible) contentDescription = app.label },
                        contentAlignment = Alignment.Center) {
                        AppIcon(app, Modifier.onGloballyPositioned { bounds = it.boundsInWindow() }, size = iconSize)
                        // Mask only the feedback, not the icon or its profile badges. The rendered
                        // bitmap preserves custom shapes and icon-pack silhouettes as well.
                        Box(Modifier.matchParentSize()
                            .then(if (rippleMask == null) Modifier.clip(CircleShape) else Modifier
                                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                                .drawWithContent {
                                    drawContent()
                                    drawImage(rippleMask, dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                                        blendMode = BlendMode.DstIn)
                                })
                            .indication(interactions, ripple()))
                    }
                    if (showLabels && visible) {
                        Text(
                            text = app.label,
                            color = symbolColor,
                            style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(top = 1.dp)
                        )
                    }
                }
            } }
            if (overflow) {
                val target = with(density) { IntOffset((inset + step * (columns - 1)).roundToPx(),
                    (inset + rowStep * (rows - 1)).roundToPx()) }
                val arrow by animateIntOffsetAsState(target, MaterialTheme.motionScheme.defaultSpatialSpec(), label = "stackArrow")
                val rotation by animateFloatAsState(if (open) 180f else 0f,
                    MaterialTheme.motionScheme.defaultSpatialSpec(), label = "stackChevron")
                IconButton(onClick = { localExpanded = !open; onExpandedChange?.invoke(!open) }, enabled = inputEnabled,
                    modifier = Modifier.offset { arrow }.size(displayedIconSize).testTag("app_stack_toggle:${stack.id}")
                        .semantics { contentDescription = expandLabel }) {
                    LauncherIcon(LauncherSymbol.Chevron, Modifier.graphicsLayer { rotationZ = rotation }, tint = symbolColor)
                }
            }
            if (apps.isEmpty()) IconButton(onClick = onEdit, enabled = inputEnabled,
                modifier = Modifier.align(Alignment.Center).semantics { contentDescription = editLabel }) {
                LauncherIcon(LauncherSymbol.Plus, tint = symbolColor)
            }
        }
    }
}
