package rikazzz.everythinglauncher.ui.home

import android.text.format.DateFormat
import androidx.activity.compose.BackHandler
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.spring
import androidx.compose.foundation.gestures.detectDragGesturesAfterLongPress
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.withFrameNanos
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.Alignment
import androidx.compose.ui.zIndex
import rikazzz.everythinglauncher.data.HomeLayout
import rikazzz.everythinglauncher.data.GraceWidget
import rikazzz.everythinglauncher.data.AppStackInset
import rikazzz.everythinglauncher.data.homeContentOwner
import rikazzz.everythinglauncher.ui.LauncherActions
import rikazzz.everythinglauncher.ui.overlays.FavoritesReorderState
import rikazzz.everythinglauncher.ui.overlays.rememberReorderTranslation
import rikazzz.everythinglauncher.ui.overlays.PopupWidgetItem
import rikazzz.everythinglauncher.ui.overlays.PopupWidgetControls
import kotlinx.coroutines.isActive
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.indication
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Surface
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import rikazzz.everythinglauncher.R
import rikazzz.everythinglauncher.data.LauncherApp
import rikazzz.everythinglauncher.data.CategoryIcon
import rikazzz.everythinglauncher.data.LauncherFolder
import rikazzz.everythinglauncher.data.ScheduleEvent
import rikazzz.everythinglauncher.data.ClockStyle
import rikazzz.everythinglauncher.data.ClockColorPart
import rikazzz.everythinglauncher.data.GraceButtonGesture
import rikazzz.everythinglauncher.data.LauncherCustomPreferences
import rikazzz.everythinglauncher.data.icons.GraceButtonIcon
import rikazzz.everythinglauncher.ui.components.GraceButton
import rikazzz.everythinglauncher.ui.components.GraceButtonOrigin
import rikazzz.everythinglauncher.ui.components.AppIcon
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp
import rikazzz.everythinglauncher.data.FolderPlacement
import rikazzz.everythinglauncher.data.PopupItem
import rikazzz.everythinglauncher.data.nextVisibleEvent
import rikazzz.everythinglauncher.data.media.MediaCommand
import rikazzz.everythinglauncher.data.IconDesign
import rikazzz.everythinglauncher.data.media.NowPlaying
import rikazzz.everythinglauncher.data.media.IdleMediaSessionId
import rikazzz.everythinglauncher.data.weather.WeatherCurrent
import rikazzz.everythinglauncher.ui.LauncherUiState
import rikazzz.everythinglauncher.ui.components.observeBlankDoubleTaps
import rikazzz.everythinglauncher.ui.components.observeListGestures
import rikazzz.everythinglauncher.ui.components.rememberLauncherListGestureConnection
import rikazzz.everythinglauncher.ui.components.AppRowGestures
import rikazzz.everythinglauncher.ui.components.LauncherAppRow
import rikazzz.everythinglauncher.ui.components.favoriteUsageLabel
import rikazzz.everythinglauncher.ui.components.FolderRow
import rikazzz.everythinglauncher.ui.components.AppStack
import rikazzz.everythinglauncher.ui.components.LauncherLayout
import rikazzz.everythinglauncher.ui.components.startPadding
import rikazzz.everythinglauncher.ui.components.endPadding
import rikazzz.everythinglauncher.ui.components.stableStatusBarInset
import rikazzz.everythinglauncher.ui.components.eventRemainingText
import rikazzz.everythinglauncher.ui.theme.LocalLauncherAppearance
import rikazzz.everythinglauncher.ui.theme.rememberBatteryPercent
import rikazzz.everythinglauncher.ui.theme.resolvedFontColor
import rikazzz.everythinglauncher.ui.weather.HomeWeather
import rikazzz.everythinglauncher.ui.widgets.HomeWidget
import rikazzz.everythinglauncher.ui.widgets.HomeEditHandle
import rikazzz.everythinglauncher.ui.widgets.rememberHostedWidget
import java.time.Instant
import kotlin.math.roundToInt

@Composable
internal fun HomeScreen(
    uiState: LauncherUiState,
    topSpace: Dp,
    onLaunchApp: (LauncherApp) -> Unit,
    onAppDetails: (LauncherApp) -> Unit,
    onAppShortcuts: (LauncherApp, Rect) -> Unit,
    onDateClick: () -> Unit,
    onClockClick: () -> Unit,
    modifier: Modifier = Modifier,
    rowGestures: AppRowGestures = AppRowGestures(),
    highlightedAppKey: String? = null,
    onOpenFolder: (LauncherFolder, Rect) -> Unit = { _, _ -> },
    onEditFolder: (LauncherFolder) -> Unit = {},
    onFolderDrag: (LauncherFolder, Rect, Boolean) -> Unit = { _, _, _ -> },
    onFolderDragEnd: (Boolean) -> Unit = {},
    onMediaCommand: (String, MediaCommand) -> Unit = { _, _ -> },
    onDismissMedia: (String, Long) -> Boolean = { _, _ -> false },
    onWidgetMenu: () -> Unit = {},
    onCustomWidgetMenu: () -> Unit = {},
    editingLayout: Boolean = false,
    widgetInputEnabled: Boolean = true,
    onTopOffsetChange: (Float) -> Unit = {},
    onWidgetHeightChange: (Int) -> Unit = {},
    onHomeGesture: (GraceButtonGesture, Offset) -> Unit = { _, _ -> },
    actions: LauncherActions = LauncherActions(),
    onWidgetEditingChange: (Boolean) -> Unit = {},
    onWeatherClick: () -> Unit = {},
    onGraceButtonGesture: (GraceButtonGesture, GraceButtonOrigin) -> Unit = { _, _ -> },
) {
    val now by rememberHomeTime()
    val headphones by rememberHeadphonesConnected()
    val localTime = now.atZone(java.time.ZoneId.systemDefault()).toLocalTime()
    val homeContent = uiState.visibleHomeContent(headphones, localTime.hour * 60 + localTime.minute)
    val event = if (uiState.settings.calendarAgenda) nextVisibleEvent(uiState.events, now,
        deprioritizeAllDay = uiState.settings.calendar.deprioritizeAllDay) else null
    val entries = homeContent.items
    val categoryIcons = uiState.appCategories.associate { it.key to it.icon }
    val entriesByKey = entries.associateBy { it.key }
    val favoriteFolders = (uiState.folders + uiState.favoriteFolders).associateBy { it.id }
    var idleMediaDismissed by rememberSaveable(uiState.settings.mediaAlwaysVisible, uiState.settings.mediaPlayer) { mutableStateOf(false) }
    LaunchedEffect(uiState.homeMedia?.sessionId, uiState.homeMedia?.revision) {
        if (uiState.homeMedia != null) idleMediaDismissed = false
    }
    val media = uiState.homeMedia ?: if (uiState.settings.mediaPlayer && uiState.settings.mediaAlwaysVisible &&
        !idleMediaDismissed && !uiState.isLoadingSettings && !uiState.settingsLoadFailed) NowPlaying(
        sessionId = IdleMediaSessionId, playerName = stringResource(R.string.settings_media_player),
        title = stringResource(R.string.media_idle_title), artist = stringResource(R.string.media_idle_summary),
        playing = false, canToggle = true, canPrevious = false, canNext = false,
    ) else null
    val listState = rememberLazyListState()
    val homeLayout = uiState.settings.homeLayout
    var topOffset by remember(homeLayout.topOffsetDp) { mutableFloatStateOf(homeLayout.topOffsetDp) }
    var capturedBaseline by rememberSaveable { mutableStateOf<Float?>(null) }
    var baselinePinned by rememberSaveable { mutableStateOf(true) }
    val itemHeights = remember { mutableStateMapOf<String, Int>() }
    val density = LocalDensity.current
    val context = LocalContext.current
    val customPrefs = remember(context) { rikazzz.everythinglauncher.data.LauncherCustomPreferences(context) }
    val fixedDockStack = remember(uiState.folders, customPrefs.fixedDockStackId) {
        uiState.folders.firstOrNull { it.id == customPrefs.fixedDockStackId && it.isAppStack }
    }
    val haptics by rememberUpdatedState(LocalHapticFeedback.current)
    val saveOrder by rememberUpdatedState<(List<String>) -> Unit>({ keys ->
        // A category remains one sortable homepage item even though it renders several rows.
        actions.reorderFavorites(keys.map { homeContent.owners[it] ?: it }.distinct())
    })
    val reorder = remember(listState) {
        FavoritesReorderState(listState, entries.map { it.key }, { saveOrder(it) }) {
            haptics.performHapticFeedback(HapticFeedbackType.TextHandleMove)
        }
    }
    var selectedWidget by rememberSaveable { mutableStateOf<String?>(null) }
    var resizing by remember { mutableStateOf(false) }
    var dragged by remember { mutableStateOf(false) }
    val drafts = remember { mutableStateMapOf<String, HomeLayout>() }
    val updateWidget: (String, HomeLayout, Boolean) -> Unit = { key, layout, save ->
        drafts[key] = layout
        if (save) actions.updatePopup(homeContentOwner(), emptyList()) { current ->
            current.map { if (it.key == key && it.widget != null) it.copy(widget = layout) else it }
        }
    }
    val saveWidget by rememberUpdatedState(updateWidget)
    val finishEditing = {
        selectedWidget?.let { key -> drafts[key]?.let { saveWidget(key, it, true) } }
        selectedWidget = null
    }
    BackHandler(enabled = selectedWidget != null) { finishEditing() }
    LaunchedEffect(entries) {
        reorder.synchronize(entries.map { it.key })
        drafts.keys.toList().forEach { key ->
            if (entries.none { it.key == key } || entriesByKey[key]?.widget == drafts[key]) drafts.remove(key)
        }
        if (entriesByKey[selectedWidget]?.widget == null) selectedWidget = null
    }
    val widgetEditing = selectedWidget != null || reorder.draggingKey != null
    val editingCallback by rememberUpdatedState(onWidgetEditingChange)
    LaunchedEffect(widgetEditing) { editingCallback(widgetEditing) }
    DisposableEffect(Unit) { onDispose { editingCallback(false) } }
    LaunchedEffect(widgetInputEnabled, editingLayout, homeLayout.editingLocked) {
        if (!widgetInputEnabled || editingLayout || homeLayout.editingLocked) {
            reorder.finish(true)
            finishEditing()
        }
    }
    LaunchedEffect(reorder.draggingKey, density) {
        if (reorder.draggingKey == null) return@LaunchedEffect
        val edge = with(density) { 64.dp.toPx() }
        val speed = with(density) { 640.dp.toPx() }
        var previous = withFrameNanos { it }
        while (isActive && reorder.draggingKey != null) {
            val frame = withFrameNanos { it }
            if (dragged) reorder.autoScroll(edge, speed * ((frame - previous) / 1_000_000_000f).coerceIn(0f, .032f))
            previous = frame
        }
    }
    val startDrag: (String) -> Unit = { key ->
        if (!homeLayout.editingLocked) { dragged = false; reorder.start(key) }
    }
    val drag: (Float) -> Unit = { delta -> if (delta != 0f) dragged = true; reorder.drag(delta) }
    val doubleTapSlop = remember(context, density) { android.view.ViewConfiguration.get(context).scaledDoubleTapSlop.toFloat() }
    val minimumTop = stableStatusBarInset() + 8.dp
    val bottomInset = WindowInsets.safeDrawing.asPaddingValues().calculateBottomPadding()
    LaunchedEffect(editingLayout, homeLayout.homeOrder) {
        baselinePinned = true
        if (editingLayout && !homeLayout.baselineEnabled) listState.scrollToItem(0)
    }
    val userIsEditing by rememberUpdatedState(widgetEditing || resizing || editingLayout)
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect {
            if (it && !userIsEditing) baselinePinned = false
        }
    }
    val overscroll = rememberOverscrollEffect()
    val gesturesEnabled = widgetInputEnabled && !editingLayout && !widgetEditing && uiState.settings.homeGestures.enabled &&
        !uiState.isLoadingSettings && !uiState.settingsLoadFailed
    val doubleTapEnabled = gesturesEnabled && uiState.settings.homeGestures.doubleTap.active
    val latestGestures by rememberUpdatedState(uiState.settings.homeGestures)
    val latestEnabled by rememberUpdatedState(gesturesEnabled)
    val latestOnGesture by rememberUpdatedState(onHomeGesture)
    var windowOffset by remember { mutableStateOf(Offset.Zero) }
    var pointerPosition by remember { mutableStateOf(Offset.Zero) }
    val scrollConnection = rememberLauncherListGestureConnection(
        overscroll, listState, uiState.settings.homeGestures.swipeSensitivity,
        canTrigger = { latestEnabled && latestGestures.target(it).active },
        onGesture = { latestOnGesture(it, windowOffset + pointerPosition) },
        protectSwipeUp = uiState.settings.homeGestures.protectSwipeUp,
        protectSwipeDown = uiState.settings.homeGestures.protectSwipeDown,
        onPullReady = { haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate) },
    )
    LaunchedEffect(gesturesEnabled) { if (!gesturesEnabled) scrollConnection.cancelGesture() }
    BoxWithConstraints(modifier.fillMaxSize()
        .onGloballyPositioned { windowOffset = it.positionInWindow() }
        .observeListGestures(scrollConnection) { pointerPosition = it }
        .pointerInput(doubleTapEnabled, doubleTapSlop, latestGestures.doubleTapIntervalMs) {
            if (!doubleTapEnabled) return@pointerInput
            observeBlankDoubleTaps(doubleTapSlop, latestGestures.doubleTapIntervalMs) { position ->
                if (latestEnabled && latestGestures.doubleTap.active) latestOnGesture(GraceButtonGesture.DoubleTap, windowOffset + position)
            }
        }) {
        val availableHeight = (maxHeight - bottomInset).coerceAtLeast(0.dp)
        val maximumTop = (availableHeight - 56.dp).coerceAtLeast(minimumTop)
        val resolvedTop = (topSpace + topOffset.dp).coerceIn(minimumTop, maximumTop)
        val baselineOffset = homeLayout.baselineOffsetDp ?: capturedBaseline
        val baselineTop = (topSpace + topOffset.dp + (baselineOffset ?: 0f).dp).coerceIn(minimumTop, maximumTop)
        val baselineIndex = reorder.keys.indexOf(GraceWidget.Baseline.key)
        val aboveHeights = reorder.keys.take(baselineIndex.coerceAtLeast(0)).map { itemHeights[it] }
        val prefixHeight = if (aboveHeights.all { it != null }) with(density) { aboveHeights.filterNotNull().sum().toDp() } else null
        // Once measured, use real leading space, not scrollable virtual padding.
        // A short home list must still stretch immediately at its top boundary.
        val anchoredOrigin = if (baselineIndex >= 0 && baselineOffset != null && prefixHeight != null) baselineTop - prefixHeight else null
        val selectedLayout = selectedWidget?.let { drafts[it] ?: entriesByKey[it]?.widget }
        val toolbarHeight = if (selectedLayout != null) minimumTop + 104.dp else 0.dp
        val inputEnabled = widgetInputEnabled && !editingLayout && !uiState.isLoadingSettings && !uiState.settingsLoadFailed
        // When an off-screen prefix has not been measured yet, align by the
        // marker's key. Measured prefixes use ordinary padding and scroll bounds.
        LaunchedEffect(reorder.keys, aboveHeights, baselineTop, baselineOffset, toolbarHeight, baselinePinned, reorder.draggingKey) {
            if (baselinePinned && baselineIndex >= 0 && baselineOffset != null && reorder.draggingKey == null) {
                if (anchoredOrigin != null) listState.requestScrollToItem(0,
                    with(density) { (toolbarHeight - anchoredOrigin).coerceAtLeast(0.dp).roundToPx() })
                else listState.requestScrollToItem(baselineIndex, 0)
            }
        }
        val allOrderedEntries = reorder.keys.mapNotNull(entriesByKey::get)
        val isFolderMode = !editingLayout && !widgetEditing && customPrefs.favoritesOverflowMode == "folder"
        val maxVisibleApps = customPrefs.favoritesMaxVisibleCount

        val renderedEntries = remember(allOrderedEntries, isFolderMode, maxVisibleApps, uiState, fixedDockStack) {
            if (!isFolderMode) allOrderedEntries
            else {
                var appCount = 0
                val visible = mutableListOf<PopupItem>()
                for (entry in allOrderedEntries) {
                    val app = uiState.findItem(entry.key)
                    if (app != null) {
                        val folder = app.folderId?.let(favoriteFolders::get)
                        if (folder != null && folder.id == fixedDockStack?.id) {
                            visible.add(entry)
                        } else {
                            if (appCount < maxVisibleApps) {
                                visible.add(entry)
                                appCount++
                            }
                        }
                    } else {
                        visible.add(entry)
                    }
                }
                visible
            }
        }

        val overflowApps = remember(allOrderedEntries, isFolderMode, maxVisibleApps, uiState, fixedDockStack) {
            if (!isFolderMode) emptyList<LauncherApp>()
            else {
                var appCount = 0
                val overflow = mutableListOf<LauncherApp>()
                for (entry in allOrderedEntries) {
                    val app = uiState.findItem(entry.key)
                    if (app != null) {
                        val folder = app.folderId?.let(favoriteFolders::get)
                        if (folder != null && folder.id == fixedDockStack?.id) {
                            // Ignored (dock)
                        } else {
                            if (appCount < maxVisibleApps) {
                                appCount++
                            } else {
                                overflow.add(app)
                            }
                        }
                    }
                }
                overflow
            }
        }

        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().padding(top = toolbarHeight)
                .overscroll(overscroll)
                .nestedScroll(scrollConnection).testTag("home_content"),
            contentPadding = PaddingValues(start = uiState.settings.listAppearance.startPadding,
                end = uiState.settings.listAppearance.endPadding,
                top = ((anchoredOrigin ?: if (baselineIndex >= 0) baselineTop else resolvedTop) - toolbarHeight).coerceAtLeast(0.dp),
                bottom = if (baselineIndex >= 0 && (anchoredOrigin == null || anchoredOrigin < toolbarHeight))
                    (availableHeight - baselineTop).coerceAtLeast(100.dp + bottomInset) else 100.dp + bottomInset),
            userScrollEnabled = !editingLayout && reorder.draggingKey == null && !resizing,
            overscrollEffect = null,
        ) {
            items(renderedEntries, key = { FavoritesReorderState.itemKey(it.key) }) { entry ->
                val grace = GraceWidget.fromKey(entry.key)
                val widget = drafts[entry.key] ?: entry.widget
                val app = uiState.findItem(entry.key)
                val dragging = reorder.draggingKey == entry.key
                val offset by rememberReorderTranslation(reorder, entry.key)
                var coordinates by remember { mutableStateOf<LayoutCoordinates?>(null) }
                val currentStart by rememberUpdatedState(startDrag)
                val currentDrag by rememberUpdatedState(drag)
                Box(Modifier.fillMaxWidth()
                    .then(if (dragging) Modifier else Modifier.animateItem())
                    .zIndex(if (dragging || reorder.drop?.key == entry.key) 1f else 0f)
                    .graphicsLayer { translationY = offset }
                    .onGloballyPositioned { coordinates = it }
                    .onSizeChanged { itemHeights[entry.key] = it.height }
                    .testTag("home_item:${entry.key}")
                    .pointerInput(grace, inputEnabled, homeLayout.editingLocked) {
                        if (grace != null && grace != GraceWidget.Baseline && inputEnabled && !homeLayout.editingLocked) {
                            var previous = Offset.Zero
                            detectDragGesturesAfterLongPress(
                                onDragStart = {
                                    previous = coordinates?.localToRoot(it) ?: it
                                    currentStart(entry.key)
                                },
                                onDragEnd = { reorder.finish(false) },
                                onDragCancel = { reorder.finish(true) },
                            ) { change, _ ->
                                val root = coordinates?.localToRoot(change.position) ?: change.position
                                change.consume(); currentDrag(root.y - previous.y); previous = root
                            }
                        }
                    }) {
                    when {
                        grace == GraceWidget.Baseline -> Spacer(Modifier.fillMaxWidth().height(2.dp)
                            .testTag("home_baseline").onGloballyPositioned {
                                if (baselineOffset == null && baselinePinned && !listState.isScrollInProgress && inputEnabled) {
                                    val measured = ((it.positionInWindow().y - windowOffset.y) / density.density - resolvedTop.value).coerceIn(0f, 1600f)
                                    capturedBaseline = measured
                                    actions.updateSettings { current ->
                                        if (current.homeLayout.baselineOffsetDp != null) current else
                                            current.copy(homeLayout = current.homeLayout.copy(baselineOffsetDp = measured))
                                    }
                                }
                            })
                        grace == GraceWidget.ClockCalendar -> {
                            HomeClockHeader(now, event, onDateClick, onClockClick,
                                showBattery = true,
                                clockStyle = uiState.settings.clockStyle,
                                weather = uiState.weather.snapshot?.current.takeIf { uiState.settings.weatherEnabled &&
                                    !uiState.isLoadingSettings && !uiState.settingsLoadFailed },
                                weatherSnapshot = uiState.weather.snapshot.takeIf { uiState.settings.weatherEnabled &&
                                    !uiState.isLoadingSettings && !uiState.settingsLoadFailed },
                                showClock = uiState.settings.clockEnabled, showCalendar = uiState.settings.calendarAgenda,
                                agendaEvents = if (uiState.settings.calendarAgenda) uiState.events else emptyList(),
                                tasks = uiState.tasks.tasks, showTasks = uiState.settings.calendar.tasksOrg.enabled,
                                taskPreferences = uiState.settings.calendar.tasksOrg,
                                onTaskClick = { onDateClick() },
                                calendarAboveClock = uiState.settings.calendarAboveClock,
                                deprioritizeAllDay = uiState.settings.calendar.deprioritizeAllDay,
                                interactive = inputEnabled, onLongClick = {}, onWeatherClick = onWeatherClick,
                                clockActive = inputEnabled)
                            }
                        // Keep the slot while empty so its height springs to zero instead of
                        // disappearing in one measure pass. Following rows flow into the gap.
                        grace == GraceWidget.Media -> Box(Modifier.fillMaxWidth().animateContentSize(
                            animationSpec = spring(dampingRatio = 1f, stiffness = 400f),
                        )) {
                            if (media != null) HomeMediaPlayer(media, onMediaCommand,
                                style = uiState.settings.mediaStyle,
                                iconDesign = uiState.settings.iconDesign?.design ?: IconDesign(),
                                albumContainerColor = rememberMediaAlbumContainerColor(media.artworkSeedColor, uiState.settings),
                                keepWhilePlaying = uiState.settings.mediaKeepWhilePlaying,
                                dismissResistance = uiState.settings.dismissResistance,
                                dismissThresholdDp = uiState.settings.dismissThresholdDp,
                                onDismiss = { id, revision ->
                                    if (id == IdleMediaSessionId) { idleMediaDismissed = true; true }
                                    else onDismissMedia(id, revision).also { if (it) idleMediaDismissed = true }
                                })
                        }
                        widget != null -> PopupWidgetItem(widget, selected = selectedWidget == entry.key,
                            enabled = inputEnabled, editable = !homeLayout.editingLocked, haptics = uiState.settings.allowHapticFeedback,
                            horizontalInset = 8.dp,
                            maxHeight = 1600,
                            onChange = { layout, save -> updateWidget(entry.key, layout, save) },
                            onResizing = { resizing = it }, onDragStart = { startDrag(entry.key) },
                            onDrag = drag, onDragEnd = { cancelled ->
                                reorder.finish(cancelled)
                                if (!cancelled && !homeLayout.editingLocked) selectedWidget = entry.key
                            })
                        app != null -> {
                            val categoryIcon = categoryIcons[homeContent.owners[entry.key]] ?: CategoryIcon.None
                            val folder = app.folderId?.let(favoriteFolders::get)
                            if (folder != null && folder.id == fixedDockStack?.id) {
                                // Pinned at bottom as dock, hide from scrollable entries
                            } else if (folder?.isAppStack == true) AppStack(folder, uiState,
                                onLaunch = { item, bounds -> rowGestures.onLaunchAt?.invoke(item, bounds) ?: onLaunchApp(item) },
                                onDetails = onAppDetails, onEdit = { onEditFolder(folder) },
                                modifier = Modifier.padding(horizontal = LauncherLayout.ContentInset - AppStackInset.dp, vertical = 4.dp))
                            else if (folder != null) FolderRow(folder, app = app,
                                highlighted = highlightedAppKey == folder.key,
                                notifications = uiState.notificationPreviewsFor(app),
                                onOpen = { onOpenFolder(folder, it) }, onLongClick = { onEditFolder(folder) },
                                onDrag = { bounds, expanded -> onFolderDrag(folder, bounds, expanded) },
                                onDragEnd = onFolderDragEnd,
                                onSwipeLeft = rowGestures.onOpenFirst?.let { open -> { bounds -> open(app, bounds) } },
                                showLabel = uiState.settings.listAppearance.showNames(favorites = true),
                                categoryIcon = categoryIcon)
                            else LauncherAppRow(app, onClick = { onLaunchApp(app) }, onLongClick = { onAppDetails(app) },
                                usageText = favoriteUsageLabel(app, uiState),
                                onSwipeRight = if (customPrefs.swipeAppRightToOpenPopup) ({ onAppShortcuts(app, it) }) else ({}),
                                gestures = if (customPrefs.swipeAppRightToOpenPopup) rowGestures else rowGestures.copy(popupsEnabled = false),
                                highlighted = highlightedAppKey == app.key,
                                notifications = uiState.notificationPreviewsFor(app),
                                showLabel = uiState.settings.listAppearance.showNames(favorites = true),
                                categoryIcon = categoryIcon)
                        }
                    }
                }
            }
            if (overflowApps.isNotEmpty()) {
                item(key = "favorites_overflow_folder") {
                    val overflowFolder = remember(overflowApps, customPrefs.favoritesOverflowFolderName) {
                        LauncherFolder(
                            id = "favorites_overflow",
                            name = customPrefs.favoritesOverflowFolderName.ifBlank { "More" },
                            appKeys = overflowApps.map { it.key },
                            placement = FolderPlacement.Both,
                        )
                    }
                    FolderRow(
                        folder = overflowFolder,
                        app = overflowFolder.asApp(),
                        onOpen = { bounds -> onOpenFolder(overflowFolder, bounds) },
                        onLongClick = null,
                        onDrag = { bounds, expanded -> onFolderDrag(overflowFolder, bounds, expanded) },
                        onDragEnd = onFolderDragEnd,
                        showLabel = uiState.settings.listAppearance.showNames(favorites = true),
                    )
                }
            }
        }
        if (selectedLayout != null) Surface(Modifier.fillMaxWidth()
            .padding(horizontal = 16.dp).offset(y = minimumTop).zIndex(4f),
            shape = MaterialTheme.shapes.large, color = MaterialTheme.colorScheme.surface) {
            PopupWidgetControls(selectedLayout,
                onChange = { layout, save -> selectedWidget?.let { updateWidget(it, layout, save) } }, onDone = finishEditing)
        }
        if (editingLayout) HomeEditHandle(
            label = stringResource(R.string.homepage_adjust_height), changed = topOffset != 0f, tag = "home_position_handle",
            onReset = { topOffset = 0f; onTopOffsetChange(0f) },
            onDelta = { delta ->
                val anchorOffset = if (baselineIndex >= 0) baselineOffset ?: 0f else 0f
                val current = (topSpace.value + topOffset + anchorOffset).coerceIn(minimumTop.value, maximumTop.value)
                topOffset = (current + delta).coerceIn(minimumTop.value, maximumTop.value) - topSpace.value - anchorOffset
            }, onFinished = { onTopOffsetChange(topOffset) },
            modifier = Modifier.padding(start = uiState.settings.listAppearance.startPadding, end = uiState.settings.listAppearance.endPadding)
                .offset(y = (if (baselineIndex >= 0) baselineTop else resolvedTop) - 20.dp))

        // Fixed bottom dock with apps + Grace button (up to 5 grids, center-aligned)
        if (!editingLayout && fixedDockStack != null) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = bottomInset + 8.dp)
                    .zIndex(3f),
                contentAlignment = Alignment.Center
            ) {
                FixedDock(
                    stack = fixedDockStack,
                    uiState = uiState,
                    onLaunchApp = { item, bounds -> rowGestures.onLaunchAt?.invoke(item, bounds) ?: onLaunchApp(item) },
                    onAppDetails = onAppDetails,
                    onEditFolder = { onEditFolder(fixedDockStack) },
                    onGraceGesture = onGraceButtonGesture,
                )
            }
        }
    }
}

@Composable
internal fun LegacyHomeClockHeader(
    now: Instant,
    event: ScheduleEvent?,
    onDateClick: () -> Unit,
    onClockClick: () -> Unit,
    showBattery: Boolean,
    clockStyle: ClockStyle,
    weather: WeatherCurrent?,
    modifier: Modifier = Modifier,
    interactive: Boolean = true,
    clockTag: String = "home_clock",
    dateTag: String = "home_date",
    onLongClick: () -> Unit = {},
    onAnchorOffsetChange: (Int) -> Unit = {},
    showClock: Boolean = true,
    showCalendar: Boolean = true,
    calendarAboveClock: Boolean = false,
    onWeatherClick: () -> Unit = {},
) {
    val appearance = LocalLauncherAppearance.current
    val visibility = clockStyle.effectiveVisibility
    val week = clockStyle.layout.week
    val clockVisible = showClock && visibility.showClock
    val weekdayVisible = showCalendar && visibility.showWeekday
    val dateVisible = showCalendar && visibility.showDate
    val battery = if (showCalendar && showBattery && visibility.showBattery) rememberBatteryPercent() else null
    val visibleWeather = weather?.takeIf { visibility.showWeather && it.temperature != null }
    val colors = ClockColorPart.entries.associateWith { resolvedFontColor(clockStyle.colors[it], onWallpaper = true) }
    val digitColors = listOf(ClockColorPart.HourTens, ClockColorPart.HourOnes, ClockColorPart.MinuteTens, ClockColorPart.MinuteOnes,
        ClockColorPart.SecondTens, ClockColorPart.SecondOnes)
        .map(colors::getValue)
    val context = LocalContext.current
    val locale = LocalConfiguration.current.locales[0]
    val clockText = formatHomeClock(now, DateFormat.is24HourFormat(context), locale, leadingZero = clockStyle.face.showLeadingZero)
    val weekdayLocale = remember(locale, week, clockStyle.face.useLatinLetters) {
        clockCalendarLocale(locale, week && clockStyle.face.useLatinLetters)
    }
    val weekday = clockCalendarDate(now, weekdayLocale, true, false,
        colors.getValue(ClockColorPart.Weekday), colors.getValue(ClockColorPart.Month), colors.getValue(ClockColorPart.Day)).text
    val dateText = clockCalendarDate(now, locale, weekdayVisible && !week, dateVisible,
        colors.getValue(ClockColorPart.Weekday), colors.getValue(ClockColorPart.Month), colors.getValue(ClockColorPart.Day))
    val calendarText = buildAnnotatedString {
        append(dateText)
        if (battery != null) {
            if (length > 0) append("  ")
            withStyle(SpanStyle(color = colors.getValue(ClockColorPart.Battery))) { append("$battery%") }
        }
    }
    val completeDate = clockCalendarDate(now, locale, true, true,
        colors.getValue(ClockColorPart.Weekday), colors.getValue(ClockColorPart.Month), colors.getValue(ClockColorPart.Day)).text
    val dateDescription = stringResource(R.string.date_calendar_action, completeDate)
    val clockDescription = stringResource(R.string.clock_action, clockText)
    val largeFaceVisible = if (week) weekdayVisible else clockVisible
    val smallClockVisible = week && clockVisible
    val agendaVisible = showCalendar && event != null
    val rowVisible = smallClockVisible || calendarText.isNotEmpty() || visibleWeather != null
    val calendarContentVisible = rowVisible || agendaVisible
    val calendarAction = showCalendar && (calendarText.isNotEmpty() || agendaVisible)

    val clockContent: @Composable () -> Unit = {
        if (largeFaceVisible) ClockFace(
            time = if (week) weekday else clockText,
            style = clockStyle,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(clockTag)
                .semantics { contentDescription = if (week) dateDescription else clockDescription }
                .then(if (interactive) Modifier.combinedClickable(
                    interactionSource = remember { MutableInteractionSource() }, indication = null,
                    onClick = if (week) onDateClick else onClockClick,
                    onLongClick = onLongClick,
                ) else Modifier)
                .padding(horizontal = LauncherLayout.ContentInset),
            color = if (week) colors.getValue(ClockColorPart.Weekday) else appearance.text,
            digitColors = digitColors.takeUnless { week },
        )
    }
    val calendarContent: @Composable () -> Unit = {
        val shape = LauncherLayout.RowShape
        val interactions = remember { MutableInteractionSource() }
        if (calendarContentVisible) Box(
            modifier = Modifier
                .fillMaxWidth()
                .testTag(dateTag)
                .then(if (calendarAction) Modifier.semantics { contentDescription = dateDescription } else Modifier)
                .then(if (interactive && calendarAction) Modifier.combinedClickable(
                    interactionSource = interactions,
                    indication = null,
                    role = Role.Button,
                    onClick = onDateClick,
                    onLongClick = onLongClick,
                ) else Modifier),
            propagateMinConstraints = true,
        ) {
            val dateStyle = MaterialTheme.typography.bodyLarge.copy(
                fontWeight = FontWeight.Normal,
                shadow = appearance.textShadow,
            )
            CompositionLocalProvider(LocalContentColor provides appearance.text) {
                Column(Modifier.padding(vertical = LauncherLayout.ContentInset)) {
                    if (rowVisible) Row(Modifier.padding(horizontal = LauncherLayout.ContentInset, vertical = 2.dp)) {
                        if (smallClockVisible) {
                            ClockTimeLabel(
                                time = clockText, style = clockStyle, textStyle = dateStyle,
                                modifier = Modifier.alignByBaseline().testTag("${clockTag}_time")
                                    .semantics { contentDescription = clockDescription }
                                    .then(if (interactive) Modifier.combinedClickable(
                                        interactionSource = remember { MutableInteractionSource() }, indication = null,
                                        onClick = onClockClick,
                                        onLongClick = onLongClick,
                                    ) else Modifier),
                                color = appearance.text,
                                digitColors = digitColors,
                            )
                            if (calendarText.isNotEmpty()) Text(" · ", modifier = Modifier.alignByBaseline(), color = appearance.text, style = dateStyle)
                        }
                        if (calendarText.isNotEmpty()) Text(
                            text = calendarText,
                            modifier = Modifier.weight(1f, fill = false).alignByBaseline().testTag("${dateTag}_text"),
                            color = appearance.text,
                            style = dateStyle,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                        if (visibleWeather != null) {
                            if (smallClockVisible || calendarText.isNotEmpty()) Spacer(Modifier.width(10.dp))
                            HomeWeather(visibleWeather, colors.getValue(ClockColorPart.Weather), dateStyle, Modifier.alignByBaseline(),
                                onClick = onWeatherClick.takeIf { interactive }, onLongClick = onLongClick.takeIf { interactive },
                                iconColor = colors.getValue(ClockColorPart.WeatherIcon))
                        }
                    }
                    if (agendaVisible) {
                        ScheduleLine(event = event, now = now, color = colors.getValue(ClockColorPart.Agenda))
                    }
                }
            }
            if (interactive && calendarAction) {
                // Keep the legacy calendar's rounded ripple separate from its text and icons.
                Box(Modifier.matchParentSize().clip(shape)
                    .indication(interactions, ripple(color = appearance.text)))
            }
        }
    }
    Column(modifier) {
        if (calendarAboveClock) calendarContent() else clockContent()
        Spacer(Modifier.height(if (largeFaceVisible && calendarContentVisible) 2.dp else 0.dp)
            .onGloballyPositioned { onAnchorOffsetChange(it.positionInParent().y.roundToInt()) })
        if (calendarAboveClock) clockContent() else calendarContent()
    }
}

@Composable
internal fun ScheduleLine(event: ScheduleEvent, now: Instant, color: Color = LocalLauncherAppearance.current.text) {
    val appearance = LocalLauncherAppearance.current
    val remaining = eventRemainingText(event, now)
    val style = MaterialTheme.typography.bodyLarge.copy(
        fontWeight = FontWeight.Normal,
        shadow = appearance.textShadow,
    )
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("schedule_line")
            .padding(horizontal = LauncherLayout.ContentInset, vertical = 2.dp),
    ) {
        Text(
            text = event.title,
            // CJK fallback and Latin fonts can have different ascents at the same size.
            modifier = Modifier.weight(1f, fill = false).alignByBaseline().testTag("schedule_title"),
            style = style,
            color = color,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(5.dp))
        Text(
            text = "· $remaining",
            modifier = Modifier.alignByBaseline().testTag("schedule_remaining"),
            style = style,
            color = color,
            maxLines = 1,
        )
    }
}

private sealed class FixedDockSlot {
    data class AppSlot(val app: LauncherApp) : FixedDockSlot()
    object GraceSlot : FixedDockSlot()
}

@Composable
private fun FixedDock(
    stack: LauncherFolder,
    uiState: LauncherUiState,
    onLaunchApp: (LauncherApp, Rect) -> Unit,
    onAppDetails: (LauncherApp) -> Unit,
    onEditFolder: () -> Unit,
    onGraceGesture: (GraceButtonGesture, GraceButtonOrigin) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val customPrefs = remember(context) { LauncherCustomPreferences(context) }
    val showLabels = customPrefs.stackLabelsEnabled
    val appearance = LocalLauncherAppearance.current
    val displayedIconSize = 40.dp * (appearance.iconSize / 100f)
    val symbolColor = appearance.text

    // Up to 4 apps from the stack so total slots with Grace button <= 5
    val rawApps = remember(stack, uiState.apps, uiState.folders) {
        uiState.popupItems(uiState.folderItem(stack), emptyList()).mapNotNull { entry ->
            if (entry.widget != null) null else uiState.findItem(entry.key)?.takeIf { it.folderId == null }
        }.take(4)
    }

    // Build slot list with Grace button placed in the center
    val slots = remember(rawApps) {
        val list = mutableListOf<FixedDockSlot>()
        val centerIndex = rawApps.size / 2
        rawApps.forEachIndexed { index, app ->
            if (index == centerIndex) {
                list.add(FixedDockSlot.GraceSlot)
            }
            list.add(FixedDockSlot.AppSlot(app))
        }
        if (centerIndex >= rawApps.size) {
            list.add(FixedDockSlot.GraceSlot)
        }
        list
    }

    val dockBgModifier = if (customPrefs.dockBackgroundEnabled) {
        Modifier
            .clip(RoundedCornerShape(displayedIconSize / 2 + 12.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.25f))
            .padding(horizontal = 10.dp, vertical = 6.dp)
    } else {
        Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
    }

    Box(
        modifier = modifier
            .wrapContentWidth()
            .then(dockBgModifier)
            .combinedClickable(
                onClick = {},
                onLongClick = onEditFolder
            ),
        contentAlignment = Alignment.Center
    ) {
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            slots.forEach { slot ->
                when (slot) {
                    is FixedDockSlot.AppSlot -> {
                        val app = slot.app
                        var bounds by remember { mutableStateOf(Rect.Zero) }
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(displayedIconSize + 16.dp)
                                .padding(horizontal = 2.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(displayedIconSize)
                                    .combinedClickable(
                                        onClick = { onLaunchApp(app, bounds) },
                                        onLongClick = { onAppDetails(app) }
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                AppIcon(app, Modifier.onGloballyPositioned { bounds = it.boundsInWindow() }, size = 40.dp)
                            }
                            if (showLabels) {
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
                    }
                    FixedDockSlot.GraceSlot -> {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(displayedIconSize + 16.dp)
                                .padding(horizontal = 2.dp)
                        ) {
                            GraceButton(
                                settings = uiState.settings.graceButton,
                                editing = false,
                                enabled = true,
                                size = displayedIconSize,
                                artwork = uiState.graceButtonApp?.icon.takeIf { GraceButtonIcon.key in uiState.itemIcons },
                                design = GraceButtonIcon.choice(uiState.itemIcons[GraceButtonIcon.key]).design ?: GraceButtonIcon.defaults,
                                onGesture = onGraceGesture,
                                onFinishEditing = {},
                                modifier = Modifier.size(displayedIconSize)
                            )
                            if (showLabels) {
                                Text(
                                    text = stringResource(R.string.settings_grace_button),
                                    color = symbolColor,
                                    style = MaterialTheme.typography.labelSmall.copy(fontSize = 10.sp),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(top = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
