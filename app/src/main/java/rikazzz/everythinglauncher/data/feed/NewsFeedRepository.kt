package rikazzz.everythinglauncher.data.feed

import android.content.Context
import android.text.Html
import android.util.Xml
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import org.xmlpull.v1.XmlPullParser
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.io.StringReader
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.security.MessageDigest
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

class NewsFeedRepository(private val context: Context) {

    private val memoryCache = ConcurrentHashMap<String, List<NewsArticle>>()
    private val _feedState = MutableStateFlow<Map<String, List<NewsArticle>>>(emptyMap())
    val feedState: StateFlow<Map<String, List<NewsArticle>>> = _feedState.asStateFlow()

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    private val _lastRefreshTime = MutableStateFlow<Map<String, Long>>(emptyMap())
    val lastRefreshTime: StateFlow<Map<String, Long>> = _lastRefreshTime.asStateFlow()

    companion object {
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 14; SM-S911B) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/141.0.0.0 Mobile Safari/537.36"
        private const val CACHE_EXPIRY_MS = 15 * 60 * 1000L // 15 minutes

        @Volatile
        private var instance: NewsFeedRepository? = null

        fun getInstance(context: Context): NewsFeedRepository =
            instance ?: synchronized(this) {
                instance ?: NewsFeedRepository(context.applicationContext).also { instance = it }
            }
    }

    suspend fun getArticles(topic: NewsTopic, region: String, forceRefresh: Boolean = false): List<NewsArticle> =
        withContext(Dispatchers.IO) {
            val cacheKey = "${topic.id}_${region}"
            val cached = memoryCache[cacheKey] ?: loadFromDisk(topic, region)

            if (cached.isNotEmpty()) {
                memoryCache[cacheKey] = cached
                _feedState.value = _feedState.value + (cacheKey to cached)
            }

            val lastTime = _lastRefreshTime.value[cacheKey] ?: 0L
            val isExpired = System.currentTimeMillis() - lastTime > CACHE_EXPIRY_MS

            if (forceRefresh || cached.isEmpty() || isExpired) {
                refreshFeed(topic, region)
            } else {
                cached
            }
        }

    suspend fun refreshFeed(topic: NewsTopic, region: String): List<NewsArticle> = withContext(Dispatchers.IO) {
        val cacheKey = "${topic.id}_${region}"
        _isRefreshing.value = true
        try {
            val urlString = topic.getUrl(region)
            val articles = fetchAndParse(urlString, topic.id)
            if (articles.isNotEmpty()) {
                memoryCache[cacheKey] = articles
                saveToDisk(topic, region, articles)
                _feedState.value = _feedState.value + (cacheKey to articles)
                _lastRefreshTime.value = _lastRefreshTime.value + (cacheKey to System.currentTimeMillis())
                articles
            } else {
                memoryCache[cacheKey] ?: emptyList()
            }
        } catch (e: Exception) {
            memoryCache[cacheKey] ?: emptyList()
        } finally {
            _isRefreshing.value = false
        }
    }

    private fun fetchAndParse(urlString: String, topicId: String): List<NewsArticle> {
        var connection: HttpURLConnection? = null
        return try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 10000
            connection.readTimeout = 15000
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Accept", "application/rss+xml, application/xml, text/xml, */*")
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9")

            if (connection.responseCode in 200..299) {
                val responseXml = BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { it.readText() }
                parseRssXml(responseXml, topicId)
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        } finally {
            connection?.disconnect()
        }
    }

    private fun parseRssXml(xml: String, topicId: String): List<NewsArticle> {
        val articles = mutableListOf<NewsArticle>()
        val parser = Xml.newPullParser()
        parser.setInput(StringReader(xml))

        var eventType = parser.eventType
        var insideItem = false

        var currentTitle: String? = null
        var currentLink: String? = null
        var currentPubDate: String? = null
        var currentSource: String? = null
        var currentSourceUrl: String? = null
        var currentDescription: String? = null

        while (eventType != XmlPullParser.END_DOCUMENT) {
            val tagName = parser.name
            when (eventType) {
                XmlPullParser.START_TAG -> {
                    if (tagName.equals("item", ignoreCase = true)) {
                        insideItem = true
                        currentTitle = null
                        currentLink = null
                        currentPubDate = null
                        currentSource = null
                        currentSourceUrl = null
                        currentDescription = null
                    } else if (insideItem) {
                        when {
                            tagName.equals("title", ignoreCase = true) -> {
                                currentTitle = parser.nextText()
                            }
                            tagName.equals("link", ignoreCase = true) -> {
                                currentLink = parser.nextText()
                            }
                            tagName.equals("pubDate", ignoreCase = true) -> {
                                currentPubDate = parser.nextText()
                            }
                            tagName.equals("source", ignoreCase = true) -> {
                                currentSourceUrl = parser.getAttributeValue(null, "url")
                                currentSource = parser.nextText()
                            }
                            tagName.equals("description", ignoreCase = true) -> {
                                currentDescription = parser.nextText()
                            }
                        }
                    }
                }
                XmlPullParser.END_TAG -> {
                    if (tagName.equals("item", ignoreCase = true)) {
                        insideItem = false
                        val link = currentLink?.trim().orEmpty()
                        var rawTitle = currentTitle?.trim().orEmpty()
                        if (link.isNotBlank() && rawTitle.isNotBlank()) {
                            val publisher = currentSource?.trim()?.ifBlank { null }
                                ?: extractPublisherFromTitle(rawTitle)
                                ?: hostOf(currentSourceUrl ?: link)

                            // Strip " - Publisher" from end of title
                            var cleanTitle = cleanHtml(rawTitle)
                            if (publisher.isNotBlank()) {
                                cleanTitle = cleanTitle.removeSuffix(" - $publisher").trim()
                            }

                            val publisherUrl = currentSourceUrl?.trim()?.ifBlank { null } ?: domainToUrl(link)
                            val domain = hostOf(publisherUrl ?: link)
                            val publishedMs = parseDate(currentPubDate) ?: System.currentTimeMillis()
                            val relativeTime = formatRelativeTime(publishedMs)

                            val article = NewsArticle(
                                id = generateArticleId(link),
                                title = cleanTitle,
                                link = link,
                                publisher = publisher,
                                publisherUrl = publisherUrl,
                                domain = domain,
                                publishedAt = publishedMs,
                                publishedRelative = relativeTime,
                                topicId = topicId,
                                snippet = currentDescription?.let { cleanHtml(it).take(200) }
                            )
                            articles.add(article)
                        }
                    }
                }
            }
            eventType = parser.next()
        }

        return articles
    }

    private fun extractPublisherFromTitle(title: String): String? {
        val lastDash = title.lastIndexOf(" - ")
        return if (lastDash != -1 && lastDash < title.length - 3) {
            title.substring(lastDash + 3).trim().ifBlank { null }
        } else null
    }

    private fun cleanHtml(html: String): String {
        return try {
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY).toString().trim()
        } catch (e: Exception) {
            html.replace(Regex("<[^>]*>"), " ").replace(Regex("\\s+"), " ").trim()
        }
    }

    private fun hostOf(url: String): String = runCatching {
        URI(url.trim().replace(" ", "%20")).host.orEmpty()
            .lowercase()
            .removePrefix("www.")
            .removePrefix("m.")
            .removePrefix("amp.")
    }.getOrDefault("")

    private fun domainToUrl(url: String): String? {
        val host = hostOf(url)
        return if (host.isNotBlank() && !host.contains("google.com")) "https://$host" else null
    }

    private fun generateArticleId(link: String): String = runCatching {
        val md = MessageDigest.getInstance("SHA-1").digest(link.toByteArray())
        md.take(8).joinToString("") { "%02x".format(it) }
    }.getOrDefault(link.hashCode().toString())

    private val rfcPatterns = listOf(
        "EEE, d MMM yyyy HH:mm:ss Z",
        "EEE, d MMM yyyy HH:mm:ss zzz",
        "EEE, d MMM yyyy HH:mm Z",
        "d MMM yyyy HH:mm:ss Z",
        "yyyy-MM-dd'T'HH:mm:ss'Z'",
        "yyyy-MM-dd HH:mm:ss",
    )

    private fun parseDate(dateStr: String?): Long? {
        if (dateStr.isNullOrBlank()) return null
        for (pattern in rfcPatterns) {
            try {
                val format = SimpleDateFormat(pattern, Locale.US)
                val parsed = format.parse(dateStr.trim())
                if (parsed != null) return parsed.time
            } catch (_: Exception) { }
        }
        return null
    }

    private fun formatRelativeTime(ms: Long): String {
        val now = System.currentTimeMillis()
        val diffMinutes = (now - ms) / 60000L
        return when {
            diffMinutes < 1 -> "now"
            diffMinutes < 60 -> "${diffMinutes}m"
            diffMinutes < 24 * 60 -> "${diffMinutes / 60}h"
            diffMinutes < 7 * 24 * 60 -> "${diffMinutes / (24 * 60)}d"
            else -> SimpleDateFormat("d MMM", Locale.getDefault()).format(Date(ms))
        }
    }

    private fun getCacheFile(topic: NewsTopic, region: String): File {
        val safeRegion = region.replace(":", "_")
        return File(context.cacheDir, "news_feed_${topic.id}_${safeRegion}.json")
    }

    private fun loadFromDisk(topic: NewsTopic, region: String): List<NewsArticle> {
        return try {
            val file = getCacheFile(topic, region)
            if (!file.exists()) return emptyList()
            val jsonText = file.readText(Charsets.UTF_8)
            val jsonArray = JSONArray(jsonText)
            val list = mutableListOf<NewsArticle>()
            for (i in 0 until jsonArray.length()) {
                val obj = jsonArray.getJSONObject(i)
                val publishedAt = obj.optLong("publishedAt", System.currentTimeMillis())
                list.add(
                    NewsArticle(
                        id = obj.optString("id", ""),
                        title = obj.optString("title", ""),
                        link = obj.optString("link", ""),
                        publisher = obj.optString("publisher", ""),
                        publisherUrl = obj.optString("publisherUrl", "").ifBlank { null },
                        domain = obj.optString("domain", ""),
                        publishedAt = publishedAt,
                        publishedRelative = formatRelativeTime(publishedAt),
                        topicId = obj.optString("topicId", topic.id),
                        snippet = obj.optString("snippet", "").ifBlank { null }
                    )
                )
            }
            list
        } catch (e: Exception) {
            emptyList()
        }
    }

    private fun saveToDisk(topic: NewsTopic, region: String, articles: List<NewsArticle>) {
        try {
            val file = getCacheFile(topic, region)
            val jsonArray = JSONArray()
            for (article in articles) {
                val obj = JSONObject().apply {
                    put("id", article.id)
                    put("title", article.title)
                    put("link", article.link)
                    put("publisher", article.publisher)
                    put("publisherUrl", article.publisherUrl.orEmpty())
                    put("domain", article.domain)
                    put("publishedAt", article.publishedAt)
                    put("topicId", article.topicId)
                    put("snippet", article.snippet.orEmpty())
                }
                jsonArray.put(obj)
            }
            file.writeText(jsonArray.toString(), Charsets.UTF_8)
        } catch (_: Exception) { }
    }
}
