package rikazzz.everythinglauncher.data.feed

enum class NewsTopic(val id: String, val label: String, val urlPath: String) {
    TOP("top", "Top Stories", "https://news.google.com/rss"),
    WORLD("world", "World", "https://news.google.com/rss/headlines/section/topic/WORLD"),
    TECH("tech", "Technology", "https://news.google.com/rss/headlines/section/topic/TECHNOLOGY"),
    BUSINESS("business", "Business", "https://news.google.com/rss/headlines/section/topic/BUSINESS"),
    SCIENCE("science", "Science", "https://news.google.com/rss/headlines/section/topic/SCIENCE"),
    ENTERTAINMENT("entertainment", "Entertainment", "https://news.google.com/rss/headlines/section/topic/ENTERTAINMENT"),
    SPORTS("sports", "Sports", "https://news.google.com/rss/headlines/section/topic/SPORTS"),
    HEALTH("health", "Health", "https://news.google.com/rss/headlines/section/topic/HEALTH");

    fun getUrl(region: String): String {
        val parts = region.split(":")
        val gl = parts.getOrNull(0)?.ifBlank { "US" } ?: "US"
        val hl = parts.getOrNull(1)?.ifBlank { "en" } ?: "en"
        val separator = if (urlPath.contains("?")) "&" else "?"
        return "$urlPath${separator}hl=$hl-$gl&gl=$gl&ceid=$gl:$hl"
    }

    companion object {
        fun fromId(id: String): NewsTopic = entries.firstOrNull { it.id.equals(id, ignoreCase = true) } ?: TOP
    }
}

data class NewsRegion(val code: String, val name: String)

val AVAILABLE_REGIONS = listOf(
    NewsRegion("US:en", "United States (English)"),
    NewsRegion("GB:en", "United Kingdom (English)"),
    NewsRegion("IN:en", "India (English)"),
    NewsRegion("AU:en", "Australia (English)"),
    NewsRegion("CA:en", "Canada (English)"),
    NewsRegion("PK:en", "Pakistan (English)"),
    NewsRegion("DE:de", "Germany (Deutsch)"),
    NewsRegion("FR:fr", "France (Français)"),
    NewsRegion("ES:es", "Spain (Español)"),
    NewsRegion("IT:it", "Italy (Italiano)"),
    NewsRegion("JP:ja", "Japan (日本語)"),
    NewsRegion("BR:pt-BR", "Brazil (Português)"),
)

data class NewsArticle(
    val id: String,
    val title: String,
    val link: String,
    val publisher: String,
    val publisherUrl: String?,
    val domain: String,
    val publishedAt: Long,
    val publishedRelative: String,
    val topicId: String,
    val snippet: String? = null,
) {
    val faviconUrl: String
        get() = if (domain.isNotBlank()) "https://www.google.com/s2/favicons?domain=$domain&sz=64"
                else "https://www.google.com/s2/favicons?domain=google.com&sz=64"
}
