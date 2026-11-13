package rikazzz.everythinglauncher.quicksearch

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object WebSuggestionsUtils {
    suspend fun getSuggestions(query: String): List<String> = withContext(Dispatchers.IO) {
        val trimmed = query.trim()
        if (trimmed.length < 2) return@withContext emptyList()

        try {
            val encoded = URLEncoder.encode(trimmed, "UTF-8")
            val url = URL("https://suggestqueries.google.com/complete/search?client=firefox&q=$encoded")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "GET"
            conn.connectTimeout = 3000
            conn.readTimeout = 3000

            if (conn.responseCode in 200..299) {
                val responseText = BufferedReader(InputStreamReader(conn.inputStream)).use { it.readText() }
                val jsonArray = JSONArray(responseText)
                if (jsonArray.length() >= 2) {
                    val suggestionsArray = jsonArray.getJSONArray(1)
                    val result = mutableListOf<String>()
                    val max = minOf(5, suggestionsArray.length())
                    for (i in 0 until max) {
                        val item = suggestionsArray.optString(i, "")
                        if (item.isNotBlank()) {
                            result.add(item)
                        }
                    }
                    return@withContext result
                }
            }
            emptyList()
        } catch (e: Exception) {
            emptyList()
        }
    }
}
