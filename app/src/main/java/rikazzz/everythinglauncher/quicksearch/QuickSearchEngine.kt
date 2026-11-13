package rikazzz.everythinglauncher.quicksearch

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

data class SearchEngineItem(
    val id: String,
    val name: String,
    val urlTemplate: String,
    val iconName: String = "search"
) {
    fun buildSearchUrl(query: String): String {
        val encoded = URLEncoder.encode(query.trim(), "UTF-8")
        return urlTemplate.replace("{query}", encoded)
    }
}

object QuickSearchEngineRegistry {
    val engines = listOf(
        SearchEngineItem("google", "Google", "https://www.google.com/search?q={query}"),
        SearchEngineItem("duckduckgo", "DuckDuckGo", "https://duckduckgo.com/?q={query}"),
        SearchEngineItem("bing", "Bing", "https://www.bing.com/search?q={query}"),
        SearchEngineItem("youtube", "YouTube", "https://www.youtube.com/results?search_query={query}"),
        SearchEngineItem("wikipedia", "Wikipedia", "https://en.wikipedia.org/wiki/Special:Search?search={query}"),
        SearchEngineItem("reddit", "Reddit", "https://www.reddit.com/search/?q={query}"),
        SearchEngineItem("github", "GitHub", "https://github.com/search?q={query}"),
        SearchEngineItem("playstore", "Play Store", "https://play.google.com/store/search?q={query}&c=apps"),
        SearchEngineItem("maps", "Maps", "https://www.google.com/maps/search/{query}"),
        SearchEngineItem("amazon", "Amazon", "https://www.amazon.com/s?k={query}"),
        SearchEngineItem("brave", "Brave", "https://search.brave.com/search?q={query}")
    )

    fun getEngine(id: String): SearchEngineItem =
        engines.firstOrNull { it.id == id } ?: engines.first()

    fun launch(context: Context, query: String, engineId: String = "google") {
        val engine = getEngine(engineId)
        val url = engine.buildSearchUrl(query)
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }
}
