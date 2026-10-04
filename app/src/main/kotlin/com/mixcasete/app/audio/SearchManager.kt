package com.mixcasete.app.audio

import android.content.Context
import com.mixcasete.app.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class SearchResult(
    val videoId: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
    val sourceLabel: String = "YouTube"
)

data class SearchOutcome(
    val results: List<SearchResult>,
    val error: String?,
    val source: String
)

class SearchManager(private val context: Context) {

    companion object {
        private const val USER_AGENT =
            "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"
    }

    suspend fun search(query: String): SearchOutcome = withContext(Dispatchers.IO) {
        val apiKey = BuildConfig.YOUTUBE_API_KEY
        if (apiKey.isBlank()) {
            return@withContext SearchOutcome(
                emptyList(),
                "YOUTUBE_API_KEY no configurada. Cargala como secreto en GitHub y vuelve a compilar.",
                "YouTube Data API"
            )
        }

        val results = mutableListOf<SearchResult>()
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://www.googleapis.com/youtube/v3/search?part=snippet&type=video&q=$encoded&maxResults=20&key=$apiKey"
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Accept", "application/json")

            val code = connection.responseCode
            if (code !in 200..299) {
                val err = try { connection.errorStream?.bufferedReader()?.readText() ?: "" } catch (e: Exception) { "" }
                return@withContext SearchOutcome(
                    emptyList(),
                    "YouTube Data API HTTP $code: ${err.take(150)}",
                    "YouTube Data API"
                )
            }

            val text = connection.inputStream.bufferedReader().readText()
            val json = JSONObject(text)
            val items = json.optJSONArray("items")
                ?: return@withContext SearchOutcome(emptyList(), "Respuesta sin items", "YouTube Data API")

            for (i in 0 until items.length()) {
                val item = items.getJSONObject(i)
                val id = item.optJSONObject("id")?.optString("videoId") ?: continue
                val snippet = item.optJSONObject("snippet") ?: continue
                val title = snippet.optString("title", "Sin titulo")
                val channel = snippet.optString("channelTitle", "Desconocido")
                val thumbs = snippet.optJSONObject("thumbnails")
                val thumb = thumbs?.optJSONObject("high")?.optString("url")
                    ?: thumbs?.optJSONObject("medium")?.optString("url")
                    ?: thumbs?.optJSONObject("default")?.optString("url")
                results.add(
                    SearchResult(
                        videoId = id,
                        title = title,
                        artist = channel,
                        thumbnailUrl = thumb,
                        sourceLabel = "YouTube"
                    )
                )
            }
        } catch (e: Exception) {
            return@withContext SearchOutcome(
                emptyList(),
                "Error consultando YouTube Data API: ${e.javaClass.simpleName}: ${e.message}",
                "YouTube Data API"
            )
        }

        if (results.isEmpty()) {
            return@withContext SearchOutcome(emptyList(), "Sin resultados para '$query'", "YouTube Data API")
        }
        SearchOutcome(results, null, "YouTube Data API (${results.size})")
    }
}
