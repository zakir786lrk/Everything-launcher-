package rikazzz.everythinglauncher.quicksearch

import android.content.Context
import android.content.Intent
import android.net.Uri
import java.net.URLEncoder

data class SearchEngineItem(
    val id: String,
    val name: String,
    val urlTemplate: String,
    val appPackage: String? = null,
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
        SearchEngineItem("chatgpt", "ChatGPT", "https://chatgpt.com/?prompt={query}", "com.openai.chatgpt"),
        SearchEngineItem("gemini", "Gemini", "https://gemini.google.com/app?text={query}", "com.google.android.apps.bard"),
        SearchEngineItem("perplexity", "Perplexity", "https://www.perplexity.ai/search?q={query}", "ai.perplexity.app.android"),
        SearchEngineItem("claude", "Claude", "https://claude.ai/new?q={query}", "com.anthropic.claude"),
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
        val trimmed = query.trim()

        // 1. Specialized handling for Gemini app (com.google.android.apps.bard)
        // Gemini's deep link URL handler does not parse URL query parameters, but
        // its Android app accepts prompt text directly into the input box via ACTION_SEND.
        if (engine.id == "gemini") {
            val isInstalled = try {
                context.packageManager.getPackageInfo("com.google.android.apps.bard", 0) != null
            } catch (e: Exception) {
                false
            }

            if (isInstalled) {
                if (trimmed.isNotEmpty()) {
                    val sendIntent = Intent(Intent.ACTION_SEND).apply {
                        type = "text/plain"
                        putExtra(Intent.EXTRA_TEXT, trimmed)
                        putExtra(Intent.EXTRA_PROCESS_TEXT, trimmed)
                        putExtra("query", trimmed)
                        setPackage("com.google.android.apps.bard")
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    try {
                        context.startActivity(sendIntent)
                        return
                    } catch (e: Exception) {
                        // Fall back to web URL below
                    }
                } else {
                    val launchIntent = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.bard")
                    if (launchIntent != null) {
                        try {
                            context.startActivity(launchIntent)
                            return
                        } catch (e: Exception) {
                            // Fall back to web URL below
                        }
                    }
                }
            }
        }

        // 2. Specialized handling for Claude app (com.anthropic.claude) if installed
        if (engine.id == "claude" && trimmed.isNotEmpty()) {
            val isInstalled = try {
                context.packageManager.getPackageInfo("com.anthropic.claude", 0) != null
            } catch (e: Exception) {
                false
            }
            if (isInstalled) {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_TEXT, trimmed)
                    setPackage("com.anthropic.claude")
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                try {
                    context.startActivity(sendIntent)
                    return
                } catch (e: Exception) {
                    // Fall back to web URL below
                }
            }
        }

        // 3. Default ACTION_VIEW with URL
        val url = engine.buildSearchUrl(query)
        try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(intent)
        } catch (e: Exception) {
            // Fallback to browser
            try {
                val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(browserIntent)
            } catch (ignored: Exception) {
                android.widget.Toast.makeText(context, rikazzz.everythinglauncher.R.string.app_unavailable, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}
