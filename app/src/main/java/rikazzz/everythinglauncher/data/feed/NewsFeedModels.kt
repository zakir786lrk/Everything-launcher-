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
    // Americas
    NewsRegion("US:en", "United States (English)"),
    NewsRegion("CA:en", "Canada (English)"),
    NewsRegion("CA:fr", "Canada (Français)"),
    NewsRegion("MX:es-419", "Mexico (Español)"),
    NewsRegion("BR:pt-BR", "Brazil (Português)"),
    NewsRegion("AR:es-419", "Argentina (Español)"),
    NewsRegion("CO:es-419", "Colombia (Español)"),
    NewsRegion("CL:es-419", "Chile (Español)"),
    NewsRegion("PE:es-419", "Peru (Español)"),

    // Europe
    NewsRegion("GB:en", "United Kingdom (English)"),
    NewsRegion("IE:en", "Ireland (English)"),
    NewsRegion("DE:de", "Germany (Deutsch)"),
    NewsRegion("AT:de", "Austria (Deutsch)"),
    NewsRegion("CH:de", "Switzerland (Deutsch)"),
    NewsRegion("FR:fr", "France (Français)"),
    NewsRegion("BE:fr", "Belgium (Français)"),
    NewsRegion("NL:nl", "Netherlands (Nederlands)"),
    NewsRegion("ES:es", "Spain (Español)"),
    NewsRegion("IT:it", "Italy (Italiano)"),
    NewsRegion("PT:pt-PT", "Portugal (Português)"),
    NewsRegion("SE:sv", "Sweden (Svenska)"),
    NewsRegion("NO:no", "Norway (Norsk)"),
    NewsRegion("DK:da", "Denmark (Dansk)"),
    NewsRegion("FI:fi", "Finland (Suomi)"),
    NewsRegion("PL:pl", "Poland (Polski)"),
    NewsRegion("CZ:cs", "Czechia (Čeština)"),
    NewsRegion("HU:hu", "Hungary (Magyar)"),
    NewsRegion("RO:ro", "Romania (Română)"),
    NewsRegion("GR:el", "Greece (Ελληνικά)"),
    NewsRegion("TR:tr", "Turkey (Türkçe)"),
    NewsRegion("UA:uk", "Ukraine (Українська)"),
    NewsRegion("RU:ru", "Russia (Русский)"),

    // Asia & Pacific
    NewsRegion("IN:en", "India (English)"),
    NewsRegion("IN:hi", "India (हिन्दी)"),
    NewsRegion("PK:en", "Pakistan (English)"),
    NewsRegion("PK:ur", "Pakistan (اردو)"),
    NewsRegion("BD:bn", "Bangladesh (বাংলা)"),
    NewsRegion("AU:en", "Australia (English)"),
    NewsRegion("NZ:en", "New Zealand (English)"),
    NewsRegion("SG:en", "Singapore (English)"),
    NewsRegion("MY:en", "Malaysia (English)"),
    NewsRegion("MY:ms", "Malaysia (Melayu)"),
    NewsRegion("PH:en", "Philippines (English)"),
    NewsRegion("ID:id", "Indonesia (Bahasa Indonesia)"),
    NewsRegion("VN:vi", "Vietnam (Tiếng Việt)"),
    NewsRegion("TH:th", "Thailand (ไทย)"),
    NewsRegion("JP:ja", "Japan (日本語)"),
    NewsRegion("KR:ko", "South Korea (한국어)"),
    NewsRegion("TW:zh-TW", "Taiwan (繁體中文)"),
    NewsRegion("HK:zh-HK", "Hong Kong (繁體中文)"),

    // Middle East & Africa
    NewsRegion("AE:en", "United Arab Emirates (English)"),
    NewsRegion("AE:ar", "United Arab Emirates (العربية)"),
    NewsRegion("SA:ar", "Saudi Arabia (العربية)"),
    NewsRegion("EG:ar", "Egypt (العربية)"),
    NewsRegion("IL:he", "Israel (עברית)"),
    NewsRegion("ZA:en", "South Africa (English)"),
    NewsRegion("NG:en", "Nigeria (English)"),
    NewsRegion("KE:en", "Kenya (English)"),
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
