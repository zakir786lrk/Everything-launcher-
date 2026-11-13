package rikazzz.everythinglauncher.ui.feed

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

object FeedImageLoader {
    private val memoryCache = object : LruCache<String, Bitmap>(10 * 1024 * 1024) {
        override fun sizeOf(key: String, bitmap: Bitmap): Int = bitmap.byteCount
    }

    suspend fun loadBitmap(context: Context, urlString: String): Bitmap? = withContext(Dispatchers.IO) {
        if (urlString.isBlank()) return@withContext null
        memoryCache.get(urlString)?.let { return@withContext it }

        val diskCacheDir = File(context.cacheDir, "favicons")
        if (!diskCacheDir.exists()) diskCacheDir.mkdirs()

        val cacheKey = hashKey(urlString)
        val diskFile = File(diskCacheDir, cacheKey)

        if (diskFile.exists() && diskFile.length() > 0) {
            try {
                val bitmap = BitmapFactory.decodeFile(diskFile.absolutePath)
                if (bitmap != null) {
                    memoryCache.put(urlString, bitmap)
                    return@withContext bitmap
                }
            } catch (_: Exception) { }
        }

        var connection: HttpURLConnection? = null
        try {
            val url = URL(urlString)
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5000
            connection.readTimeout = 5000
            connection.instanceFollowRedirects = true
            connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Linux; Android 14) Chrome/141.0.0.0")

            if (connection.responseCode in 200..299) {
                val bytes = connection.inputStream.use { it.readBytes() }
                if (bytes.isNotEmpty()) {
                    val bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                    if (bitmap != null) {
                        memoryCache.put(urlString, bitmap)
                        try {
                            diskFile.writeBytes(bytes)
                        } catch (_: Exception) { }
                        return@withContext bitmap
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        } finally {
            connection?.disconnect()
        }
    }

    private fun hashKey(key: String): String = runCatching {
        val md = MessageDigest.getInstance("MD5").digest(key.toByteArray())
        md.joinToString("") { "%02x".format(it) }
    }.getOrDefault(key.hashCode().toString())
}

@Composable
fun PublisherFavicon(
    faviconUrl: String?,
    publisher: String,
    modifier: Modifier = Modifier.size(20.dp),
) {
    val context = LocalContext.current
    var bitmap by remember(faviconUrl) { mutableStateOf<Bitmap?>(null) }

    LaunchedEffect(faviconUrl) {
        if (!faviconUrl.isNullOrBlank()) {
            bitmap = FeedImageLoader.loadBitmap(context, faviconUrl)
        }
    }

    if (bitmap != null) {
        Image(
            bitmap = bitmap!!.asImageBitmap(),
            contentDescription = publisher,
            contentScale = ContentScale.Fit,
            modifier = modifier.clip(CircleShape)
        )
    } else {
        val initial = publisher.trim().firstOrNull()?.uppercaseChar()?.toString() ?: "N"
        Box(
            modifier = modifier
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = initial,
                style = MaterialTheme.typography.labelSmall.copy(
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                ),
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
    }
}
