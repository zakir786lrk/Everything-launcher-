package rikazzz.everythinglauncher.ui.search

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.overscroll
import androidx.compose.foundation.rememberOverscrollEffect
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.LayoutDirection
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import rikazzz.everythinglauncher.R
import rikazzz.everythinglauncher.customshortcuts.ActivityLauncherHelper
import rikazzz.everythinglauncher.data.*
import rikazzz.everythinglauncher.platform.SearchLauncher
import rikazzz.everythinglauncher.quicksearch.CalculatorUtils
import rikazzz.everythinglauncher.quicksearch.DeviceSettingItem
import rikazzz.everythinglauncher.quicksearch.DeviceSettingsRepository
import rikazzz.everythinglauncher.quicksearch.QuickSearchEngineRegistry
import rikazzz.everythinglauncher.quicksearch.WebSuggestionsUtils
import rikazzz.everythinglauncher.ui.LauncherActions
import rikazzz.everythinglauncher.ui.LauncherUiState
import rikazzz.everythinglauncher.ui.components.AppIcon
import rikazzz.everythinglauncher.ui.components.LauncherLayout
import rikazzz.everythinglauncher.ui.components.stableStatusBarInset
import rikazzz.everythinglauncher.ui.components.LauncherSearchBar
import rikazzz.everythinglauncher.ui.components.GraceButtonTransition
import rikazzz.everythinglauncher.ui.components.observeListGestures
import rikazzz.everythinglauncher.ui.components.rememberLauncherListGestureConnection
import rikazzz.everythinglauncher.ui.theme.LocalLauncherAppearance
import rikazzz.everythinglauncher.ui.theme.effectiveDisplay
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private sealed interface SearchResult {
    val key: String
    data class App(val app: LauncherApp) : SearchResult { override val key = "app:" + app.key }
    data class Shortcut(val app: LauncherApp) : SearchResult { override val key = "shortcut:" + app.key }
    data class CustomShortcut(val shortcut: CustomActivityShortcut) : SearchResult { override val key = "custom_shortcut:" + shortcut.id }
    data class DeviceSetting(val item: DeviceSettingItem) : SearchResult { override val key = "setting:" + item.id }
    data class Calculator(val expression: String, val result: String) : SearchResult { override val key = "calc:" + expression }
    data class WebSuggestion(val suggestion: String) : SearchResult { override val key = "web_sug:" + suggestion }
    data class Contact(val contact: SearchContact) : SearchResult { override val key = "contact:" + contact.id }
    data object Internet : SearchResult { override val key = "search_internet_result" }
}

@Composable
internal fun AppSearchScreen(
    uiState: LauncherUiState,
    actions: LauncherActions,
    onLaunch: (LauncherApp) -> Unit,
    onDetails: (LauncherApp) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    backProgress: Float = 0f,
    enterAlpha: Float = 1f,
    onSettings: () -> Unit = {},
    queryState: TextFieldState = rememberTextFieldState(),
    transition: GraceButtonTransition? = null,
) {
    val settings = uiState.settings.search
    val query = queryState.text.toString().trim()
    val context = LocalContext.current
    val customPrefs = remember(context) { LauncherCustomPreferences(context) }
    val deviceSettingsRepo = remember(context) { DeviceSettingsRepository(context) }
    val keyboard = LocalSoftwareKeyboardController.current
    var requestKeyboard by remember { mutableStateOf(transition == null) }
    LaunchedEffect(transition) {
        if (transition != null) delay(100)
        requestKeyboard = true
    }
    val appearance = LocalLauncherAppearance.current
    val contactsSource = remember(context) { SearchContacts(context) }
    var hasContactsAccess by remember { mutableStateOf(contactsSource.hasAccess()) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle, contactsSource) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) hasContactsAccess = contactsSource.hasAccess()
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer) }
    }
    val contacts by produceState(emptyList<SearchContact>(), settings.enabled, settings.contacts, hasContactsAccess) {
        value = emptyList()
        if (settings.enabled && settings.contacts && hasContactsAccess) contactsSource.observe().collect { value = it }
    }

    var webSuggestions by remember { mutableStateOf<List<String>>(emptyList()) }
    LaunchedEffect(query, customPrefs.searchWebSuggestionsEnabled) {
        if (customPrefs.searchWebSuggestionsEnabled && query.length >= 2) {
            webSuggestions = WebSuggestionsUtils.getSuggestions(query)
        } else {
            webSuggestions = emptyList()
        }
    }

    val apps = remember(uiState.allApps, uiState.effectiveHiddenAppKeys, settings.hiddenApps) {
        uiState.allApps.filter { it.folderId == null && (settings.hiddenApps || it.key !in uiState.effectiveHiddenAppKeys) }
    }
    val names = rememberAppSearchNames(apps)
    val customShortcuts = remember(customPrefs) { customPrefs.getCustomShortcuts() }

    val results: List<SearchResult> = remember(
        apps, names, contacts, query, settings, hasContactsAccess, uiState.shortcutApps, customShortcuts, webSuggestions
    ) {
        if (query.isEmpty()) {
            if (settings.suggestions) {
                val byKey = apps.associateBy { it.key }
                settings.recentAppKeys.mapNotNull(byKey::get).distinctBy { it.key }.take(5).map { SearchResult.App(it) }
            } else emptyList()
        } else {
            val term = SearchQuery(query, settings.fuzzy)

            // 1. Math calculation
            val calcResult = if (customPrefs.searchCalculatorEnabled && CalculatorUtils.isMathExpression(query)) {
                CalculatorUtils.evaluateExpression(query)
            } else null

            // 2. Matching device settings
            val matchedSettings = if (customPrefs.searchDeviceSettingsEnabled) {
                deviceSettingsRepo.searchSettings(query).take(3).map { SearchResult.DeviceSetting(it) }
            } else emptyList()

            // 3. Matching custom activity shortcuts
            val matchedCustomShortcuts = if (customPrefs.searchCustomShortcutsEnabled) {
                customShortcuts.filter {
                    it.showInSearch && (it.name.contains(query, ignoreCase = true) || it.packageName.contains(query, ignoreCase = true))
                }.take(3).map { SearchResult.CustomShortcut(it) }
            } else emptyList()

            // 4. Apps and app shortcuts
            val matchedApps = apps.mapNotNull { app ->
                val score = names?.get(app.key)?.score(term) ?: return@mapNotNull null
                score to app
            }.sortedWith(compareBy<Pair<Int, LauncherApp>> { it.first }.thenComparator { a, b -> LauncherAppOrder.compare(a.second, b.second) })

            val matchedShortcuts = uiState.shortcutApps.filter {
                it.label.contains(query, ignoreCase = true)
            }.take(3).map { SearchResult.Shortcut(it) }

            // 5. Contacts
            val matchedContacts = if (settings.contacts && hasContactsAccess) contacts.mapNotNull { contact ->
                contact.score(term)?.let { it to contact }
            }.sortedBy { it.first } else emptyList()

            buildList {
                if (calcResult != null) {
                    add(SearchResult.Calculator(query, calcResult))
                }
                matchedSettings.forEach { add(it) }
                matchedCustomShortcuts.forEach { add(it) }
                matchedApps.forEach { add(SearchResult.App(it.second)) }
                matchedShortcuts.forEach { add(it) }
                matchedContacts.forEach { add(SearchResult.Contact(it.second)) }
                webSuggestions.forEach { add(SearchResult.WebSuggestion(it)) }
                if (settings.internet) add(SearchResult.Internet)
            }
        }
    }

    fun open(result: SearchResult, bounds: Rect? = null) {
        keyboard?.hide()
        when (result) {
            is SearchResult.App -> {
                val launch = actions.launchSearchApp
                if (launch != null) { onDismiss(); launch(result.app, bounds) } else onLaunch(result.app)
            }
            is SearchResult.Shortcut -> {
                onDismiss()
                val launch = actions.launchAppAt
                if (launch != null && bounds != null) launch(result.app, bounds) else onLaunch(result.app)
            }
            is SearchResult.CustomShortcut -> {
                onDismiss()
                ActivityLauncherHelper.launchShortcut(context, result.shortcut)
            }
            is SearchResult.DeviceSetting -> {
                onDismiss()
                context.startActivity(result.item.toIntent())
            }
            is SearchResult.Calculator -> {
                val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                cm.setPrimaryClip(ClipData.newPlainText("Calculator Result", result.result))
                Toast.makeText(context, "Copied result: ${result.result}", Toast.LENGTH_SHORT).show()
            }
            is SearchResult.WebSuggestion -> {
                QuickSearchEngineRegistry.launch(context, result.suggestion, customPrefs.defaultSearchEngine)
                onDismiss()
            }
            is SearchResult.Contact -> {
                val opened = SearchLauncher.contact(context, result.contact)
                if (opened) onDismiss() else Toast.makeText(context, R.string.app_unavailable, Toast.LENGTH_SHORT).show()
            }
            SearchResult.Internet -> {
                QuickSearchEngineRegistry.launch(context, query, customPrefs.defaultSearchEngine)
                onDismiss()
            }
        }
    }

    val list = rememberLazyListState()
    val overscroll = rememberOverscrollEffect()
    val scrollConnection = rememberLauncherListGestureConnection(
        overscroll, list, uiState.settings.homeGestures.swipeSensitivity,
        canTrigger = { it == GraceButtonGesture.SwipeDown && transition == null && backProgress == 0f },
        onGesture = { keyboard?.hide(); onDismiss() },
    )
    LaunchedEffect(query) { list.requestScrollToItem(0) }
    val density = LocalDensity.current
    var searchBarHeight by remember { mutableStateOf(56.dp) }
    val listTop = 12.dp + searchBarHeight / 2
    val bottomInset = WindowInsets.safeDrawing.exclude(WindowInsets.ime).asPaddingValues().calculateBottomPadding()
    val backDistance = with(density) { 30.dp.toPx() } * if (LocalLayoutDirection.current == LayoutDirection.Ltr) 1 else -1

    Box(modifier.fillMaxSize().testTag("app_search").graphicsLayer {
        alpha = (if (transition != null) 1f else enterAlpha) * (1f - backProgress)
        translationX = backDistance * backProgress
    }.then(if (transition != null) Modifier.clearAndSetSemantics {} else Modifier)
        .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Horizontal))
        .padding(top = stableStatusBarInset())
        .imePadding().padding(horizontal = 16.dp)) {

        LazyColumn(
            Modifier.fillMaxSize().padding(top = listTop)
                .testTag("app_search_results")
                .observeListGestures(scrollConnection)
                .overscroll(overscroll)
                .nestedScroll(scrollConnection)
                .graphicsLayer { alpha = transition?.searchResultsAlpha ?: 1f },
            state = list,
            overscrollEffect = null,
            contentPadding = PaddingValues(top = searchBarHeight / 2 + 16.dp, bottom = 64.dp + bottomInset)
        ) {
            if (query.isNotEmpty() && names != null && results.isEmpty()) item {
                Text(stringResource(R.string.search_no_results), Modifier.padding(20.dp), color = appearance.text)
            }

            itemsIndexed(results, key = { _, result -> result.key }) { index, result ->
                when (result) {
                    is SearchResult.Calculator -> {
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .combinedClickable(onClick = { open(result) }),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                        ) {
                            Row(
                                modifier = Modifier.padding(16.dp).fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text(result.expression, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f))
                                    Text("= ${result.result}", style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold), color = MaterialTheme.colorScheme.onPrimaryContainer)
                                }
                                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primary) {
                                    Icon(painterResource(R.drawable.ms_edit), contentDescription = "Copy", modifier = Modifier.padding(8.dp).size(20.dp), tint = MaterialTheme.colorScheme.onPrimary)
                                }
                            }
                        }
                    }
                    is SearchResult.DeviceSetting -> {
                        SearchResultRow(
                            label = "${result.item.title} (${result.item.category})",
                            key = result.key,
                            first = index == 0,
                            onClick = { open(result, it) },
                            onLongClick = null
                        ) { iconModifier ->
                            Surface(modifier = iconModifier.size(40.dp), shape = CircleShape, color = MaterialTheme.colorScheme.secondaryContainer) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(painterResource(result.item.iconResId), contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                                }
                            }
                        }
                    }
                    is SearchResult.CustomShortcut -> {
                        SearchResultRow(
                            label = result.shortcut.name,
                            key = result.key,
                            first = index == 0,
                            onClick = { open(result, it) },
                            onLongClick = null
                        ) { iconModifier ->
                            Surface(modifier = iconModifier.size(40.dp), shape = CircleShape, color = MaterialTheme.colorScheme.tertiaryContainer) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(painterResource(R.drawable.ms_steppers), contentDescription = null, modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                                }
                            }
                        }
                    }
                    is SearchResult.WebSuggestion -> {
                        SearchResultRow(
                            label = result.suggestion,
                            key = result.key,
                            first = index == 0,
                            onClick = { open(result, it) },
                            onLongClick = null
                        ) { iconModifier ->
                            Surface(modifier = iconModifier.size(40.dp), shape = CircleShape, color = MaterialTheme.colorScheme.surfaceVariant) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(painterResource(R.drawable.ms_search), contentDescription = null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                        }
                    }
                    else -> {
                        val label = when (result) {
                            is SearchResult.App -> result.app.label
                            is SearchResult.Shortcut -> "Shortcut: " + result.app.label
                            is SearchResult.Contact -> result.contact.name
                            SearchResult.Internet -> stringResource(R.string.search_internet_result)
                            else -> ""
                        }
                        val resultDisplay = (result as? SearchResult.App)?.app?.effectiveDisplay(appearance)
                        SearchResultRow(
                            label, result.key, first = index == 0, onClick = { open(result, it) },
                            hideIcon = resultDisplay?.style == IconDisplayStyle.Hidden,
                            onLongClick = (result as? SearchResult.App)?.let { { keyboard?.hide(); onDetails(it.app) } }
                        ) { iconModifier ->
                            when (result) {
                                is SearchResult.App -> AppIcon(result.app, iconModifier, size = 40.dp, display = resultDisplay!!)
                                is SearchResult.Shortcut -> AppIcon(result.app, iconModifier, size = 40.dp)
                                is SearchResult.Contact -> ContactAvatar(result.contact, contactsSource, iconModifier)
                                SearchResult.Internet -> Surface(modifier = iconModifier.size(40.dp), shape = CircleShape,
                                    color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
                                    Box(contentAlignment = Alignment.Center) { Icon(painterResource(R.drawable.ms_travel_explore), null, Modifier.size(24.dp)) }
                                }
                                else -> {}
                            }
                        }
                    }
                }
            }
        }

        // Search engine quick chips at bottom
        if (customPrefs.searchEngineChipsEnabled && query.isNotBlank()) {
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(bottom = bottomInset + 4.dp),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = 0.9f),
                tonalElevation = 6.dp
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp)
                        .horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    QuickSearchEngineRegistry.engines.forEach { engine ->
                        SuggestionChip(
                            onClick = {
                                keyboard?.hide()
                                QuickSearchEngineRegistry.launch(context, query, engine.id)
                                onDismiss()
                            },
                            label = { Text(engine.name, fontSize = 12.sp) }
                        )
                    }
                }
            }
        }

        LauncherSearchBar(
            queryState, stringResource(R.string.settings_search), "app_search_query",
            modifier = Modifier.padding(top = 12.dp).onSizeChanged { searchBarHeight = with(density) { it.height.toDp() } }
                .onGloballyPositioned { transition?.searchBounds = it.boundsInRoot() }
                .graphicsLayer { alpha = transition?.searchBarAlpha ?: 1f },
            autoFocus = requestKeyboard,
            onSearch = { results.firstOrNull()?.let { open(it) } },
            trailingIcon = {
                IconButton(onClick = { keyboard?.hide(); onSettings() }, modifier = Modifier.testTag("app_search_settings")) {
                    Icon(painterResource(R.drawable.ms_more_vert), stringResource(R.string.search_open_settings))
                }
            },
        )
    }
}

@Composable
private fun SearchResultRow(label: String, key: String, first: Boolean, hideIcon: Boolean = false, onClick: (Rect) -> Unit,
    onLongClick: (() -> Unit)?, icon: @Composable (Modifier) -> Unit) {
    val appearance = LocalLauncherAppearance.current
    var bounds by remember { mutableStateOf(Rect.Zero) }
    Row(Modifier.fillMaxWidth().heightIn(min = LauncherLayout.RowMinHeight).testTag(key)
        .clip(LauncherLayout.RowShape).combinedClickable(onClick = { onClick(bounds) }, onLongClick = onLongClick)
        .semantics { selected = first }.padding(LauncherLayout.ContentInset), verticalAlignment = Alignment.CenterVertically) {
        if (!hideIcon) {
            icon(Modifier.onGloballyPositioned { bounds = it.boundsInWindow() })
            Spacer(Modifier.width(LauncherLayout.IconLabelGap))
        }
        Row(Modifier.weight(1f).then(if (hideIcon) Modifier.onGloballyPositioned { bounds = it.boundsInWindow() } else Modifier),
            verticalAlignment = Alignment.CenterVertically) {
            Text(label, Modifier.weight(1f, fill = false).testTag(key + ":label"), color = appearance.text,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (first) FontWeight.Bold else FontWeight.Normal, shadow = appearance.textShadow),
                maxLines = 1, overflow = TextOverflow.Ellipsis)
            if (first) Box(Modifier.padding(start = 8.dp).size(5.dp).background(appearance.text, CircleShape).testTag("search_first_result_dot"))
        }
    }
}

@Composable
private fun ContactAvatar(contact: SearchContact, source: SearchContacts, modifier: Modifier) {
    val photo by produceState<ImageBitmap?>(null, contact.id, contact.photo, source) { value = source.photo(contact) }
    if (photo != null) Image(photo!!, null, modifier.size(40.dp).clip(CircleShape), contentScale = ContentScale.Crop)
    else Surface(modifier = modifier.size(40.dp), shape = CircleShape,
        color = MaterialTheme.colorScheme.primaryContainer, contentColor = MaterialTheme.colorScheme.onPrimaryContainer) {
        Box(contentAlignment = Alignment.Center) {
            val name = contact.name.trim()
            Text(if (name.isEmpty()) "?" else String(Character.toChars(name.codePointAt(0))).uppercase(), style = MaterialTheme.typography.titleLarge)
        }
    }
}
