package rikazzz.everythinglauncher.ui.feed

import android.app.SearchManager
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.speech.RecognizerIntent
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import rikazzz.everythinglauncher.R
import rikazzz.everythinglauncher.data.LauncherCustomPreferences
import rikazzz.everythinglauncher.data.feed.NewsArticle
import rikazzz.everythinglauncher.data.feed.NewsFeedRepository
import rikazzz.everythinglauncher.data.feed.NewsTopic
import rikazzz.everythinglauncher.ui.LauncherUiState
import rikazzz.everythinglauncher.ui.components.LauncherIcon
import rikazzz.everythinglauncher.ui.components.LauncherSymbol

@Composable
internal fun FeedScreen(
    uiState: LauncherUiState,
    customPrefs: LauncherCustomPreferences,
    statusBarHeight: Dp,
    bottomInset: Dp,
    onOpenSearch: () -> Unit,
    onVoiceSearch: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onWeatherClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val repository = remember(context) { NewsFeedRepository.getInstance(context) }

    var selectedTopic by rememberSaveable { mutableStateOf(NewsTopic.fromId(customPrefs.feedDefaultTopic)) }
    val region = customPrefs.feedRegion
    val feedState by repository.feedState.collectAsState()
    val isRefreshing by repository.isRefreshing.collectAsState()
    val listState = rememberLazyListState()

    val cacheKey = "${selectedTopic.id}_${region}"
    val articles = feedState[cacheKey] ?: emptyList()

    LaunchedEffect(selectedTopic, region) {
        repository.getArticles(selectedTopic, region)
    }

    val voiceLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { res ->
        val query = res.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        if (!query.isNullOrBlank()) {
            onVoiceSearch(query)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(top = statusBarHeight)
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(bottom = bottomInset + 32.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Feed Header
            item("header") {
                FeedHeader(
                    uiState = uiState,
                    customPrefs = customPrefs,
                    isRefreshing = isRefreshing,
                    onRefresh = { scope.launch { repository.refreshFeed(selectedTopic, region) } },
                    onOpenSettings = onOpenSettings,
                    onOpenSearch = onOpenSearch,
                    onVoice = {
                        try {
                            voiceLauncher.launch(
                                Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                                    putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                                    putExtra(RecognizerIntent.EXTRA_PROMPT, "Search Google")
                                }
                            )
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(context, "Voice input not available", Toast.LENGTH_SHORT).show()
                        }
                    },
                    onLens = { launchGoogleLens(context) },
                    onWeather = onWeatherClick
                )
            }

            // Topic Filter Chips
            item("chips") {
                TopicChipsRow(
                    selected = selectedTopic,
                    onSelect = { selectedTopic = it }
                )
            }

            // Feed Content
            if (articles.isEmpty()) {
                item("empty_or_loading") {
                    if (isRefreshing) {
                        SkeletonLoadingCards()
                    } else {
                        EmptyFeedState(
                            onRefresh = { scope.launch { repository.refreshFeed(selectedTopic, region) } }
                        )
                    }
                }
            } else {
                items(articles, key = { it.id }) { article ->
                    NewsArticleCard(
                        article = article,
                        onOpen = { openArticleLink(context, article.link) },
                        onShare = { shareArticle(context, article.title, article.link) },
                        onCopy = { copyArticleLink(context, article.link) }
                    )
                }

                item("footer") {
                    FeedFooter(
                        count = articles.size,
                        onRefresh = {
                            scope.launch {
                                repository.refreshFeed(selectedTopic, region)
                                listState.animateScrollToItem(0)
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun FeedHeader(
    uiState: LauncherUiState,
    customPrefs: LauncherCustomPreferences,
    isRefreshing: Boolean,
    onRefresh: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSearch: () -> Unit,
    onVoice: () -> Unit,
    onLens: () -> Unit,
    onWeather: () -> Unit,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "refresh_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp)
    ) {
        // Top row: Weather (if enabled) + Actions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val weatherSnapshot = uiState.weather.snapshot
            if (customPrefs.feedShowWeather && weatherSnapshot?.current != null) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .clickable(onClick = onWeather)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LauncherIcon(
                            symbol = LauncherSymbol.Weather,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        val tempText = weatherSnapshot.current.temperature?.format() ?: ""
                        if (tempText.isNotBlank()) {
                            Text(
                                text = tempText,
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.weight(1f))

            IconButton(onClick = onRefresh) {
                Icon(
                    painter = painterResource(R.drawable.ic_refresh),
                    contentDescription = stringResource(R.string.feed_refresh),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .size(20.dp)
                        .then(if (isRefreshing) Modifier.rotate(rotation) else Modifier)
                )
            }

            IconButton(onClick = onOpenSettings) {
                LauncherIcon(
                    symbol = LauncherSymbol.Settings,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Google Wordmark
        GoogleWordmark(
            modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .padding(top = 4.dp, bottom = 14.dp)
        )

        // Google Search Pill (if enabled)
        if (customPrefs.feedShowSearchBar) {
            GoogleSearchPill(
                onSearch = onOpenSearch,
                onVoice = onVoice,
                onLens = onLens
            )
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun GoogleWordmark(modifier: Modifier = Modifier) {
    val blue = Color(0xFF4285F4)
    val red = Color(0xFFEA4335)
    val yellow = Color(0xFFFBBC05)
    val green = Color(0xFF34A853)

    val wordmark = buildAnnotatedString {
        appendLetter("G", blue)
        appendLetter("o", red)
        appendLetter("o", yellow)
        appendLetter("g", blue)
        appendLetter("l", green)
        appendLetter("e", red)
    }

    Text(
        text = wordmark,
        fontWeight = FontWeight.Medium,
        fontSize = 42.sp,
        letterSpacing = (-1.2).sp,
        modifier = modifier
    )
}

private fun androidx.compose.ui.text.AnnotatedString.Builder.appendLetter(char: String, color: Color) {
    withStyle(SpanStyle(color = color)) {
        append(char)
    }
}

@Composable
private fun GoogleSearchPill(
    onSearch: () -> Unit,
    onVoice: () -> Unit,
    onLens: () -> Unit,
) {
    Surface(
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .clip(RoundedCornerShape(28.dp))
            .clickable(onClick = onSearch)
    ) {
        Row(
            modifier = Modifier.padding(start = 16.dp, end = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_google_g),
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.width(12.dp))
            Text(
                text = "Search",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            IconButton(onClick = onVoice) {
                Icon(
                    painter = painterResource(R.drawable.ic_mic),
                    contentDescription = "Voice search",
                    tint = Color.Unspecified,
                    modifier = Modifier.size(22.dp)
                )
            }
            IconButton(onClick = onLens) {
                Icon(
                    painter = painterResource(R.drawable.ic_camera),
                    contentDescription = "Google Lens",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
private fun TopicChipsRow(
    selected: NewsTopic,
    onSelect: (NewsTopic) -> Unit,
) {
    LazyRow(
        contentPadding = PaddingValues(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(vertical = 4.dp)
    ) {
        items(NewsTopic.entries, key = { it.id }) { topic ->
            val isSelected = selected == topic
            FilterChip(
                selected = isSelected,
                onClick = { onSelect(topic) },
                label = {
                    Text(
                        text = topic.label,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                    )
                },
                shape = RoundedCornerShape(50),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    containerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                    labelColor = MaterialTheme.colorScheme.onSurfaceVariant
                )
            )
        }
    }
}

@Composable
private fun NewsArticleCard(
    article: NewsArticle,
    onOpen: () -> Unit,
    onShare: () -> Unit,
    onCopy: () -> Unit,
) {
    var menuOpen by remember { mutableStateOf(false) }

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        ),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onOpen)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            // Source Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                PublisherFavicon(
                    faviconUrl = article.faviconUrl,
                    publisher = article.publisher,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = article.publisher,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f, fill = false)
                )
                Text(
                    text = " • ${article.publishedRelative}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(Modifier.weight(1f))

                Box {
                    IconButton(
                        onClick = { menuOpen = true },
                        modifier = Modifier.size(24.dp)
                    ) {
                        LauncherIcon(
                            symbol = LauncherSymbol.Outbound,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = menuOpen,
                        onDismissRequest = { menuOpen = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.feed_open_browser)) },
                            onClick = { menuOpen = false; onOpen() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.feed_share)) },
                            onClick = { menuOpen = false; onShare() }
                        )
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.feed_copy_link)) },
                            onClick = { menuOpen = false; onCopy() }
                        )
                    }
                }
            }

            Spacer(Modifier.height(8.dp))

            // Article Headline
            Text(
                text = article.title,
                style = MaterialTheme.typography.titleMedium.copy(
                    lineHeight = 22.sp,
                    fontWeight = FontWeight.Medium
                ),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis
            )

            // Snippet preview (if present)
            if (!article.snippet.isNullOrBlank()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    text = article.snippet,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
private fun SkeletonLoadingCards() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        repeat(4) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceContainer,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(18.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .width(90.dp)
                                .height(12.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                        )
                    }
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.92f)
                            .height(16.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(0.65f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyFeedState(onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("📰", fontSize = 40.sp)
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.feed_no_stories),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Spacer(Modifier.height(16.dp))
        OutlinedButton(onClick = onRefresh) {
            Text(stringResource(R.string.feed_retry))
        }
    }
}

@Composable
private fun FeedFooter(count: Int, onRefresh: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "✓",
            style = MaterialTheme.typography.headlineMedium,
            color = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = stringResource(R.string.feed_caught_up),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = "$count stories",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(12.dp))
        OutlinedButton(onClick = onRefresh) {
            Text(stringResource(R.string.feed_refresh))
        }
    }
}

private fun openArticleLink(context: Context, url: String) {
    try {
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } catch (_: Exception) {
        Toast.makeText(context, "No app available to open link", Toast.LENGTH_SHORT).show()
    }
}

private fun shareArticle(context: Context, title: String, url: String) {
    try {
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, title)
            putExtra(Intent.EXTRA_TEXT, "$title\n$url")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(Intent.createChooser(send, "Share story").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: Exception) {
        Toast.makeText(context, "Unable to share", Toast.LENGTH_SHORT).show()
    }
}

private fun copyArticleLink(context: Context, url: String) {
    try {
        val cm = context.getSystemService(ClipboardManager::class.java)
        cm.setPrimaryClip(ClipData.newPlainText("link", url))
        Toast.makeText(context, "Link copied", Toast.LENGTH_SHORT).show()
    } catch (_: Exception) { }
}

private fun launchGoogleLens(context: Context) {
    val tries = listOf(
        { Intent(Intent.ACTION_VIEW, Uri.parse("googleapp://lens")).setPackage("com.google.android.googlequicksearchbox") },
        { context.packageManager.getLaunchIntentForPackage("com.google.ar.lens") ?: throw ActivityNotFoundException() },
        { Intent(Intent.ACTION_VIEW, Uri.parse("https://lens.google.com/")) }
    )
    for (t in tries) {
        try {
            context.startActivity(t().addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        } catch (_: Exception) { }
    }
}
