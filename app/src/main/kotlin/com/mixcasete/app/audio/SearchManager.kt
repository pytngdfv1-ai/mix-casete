package com.mixcasete.app.audio

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class SearchResult(
    val url: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String?
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

    init {
        try {
            NewPipe.init(object : Downloader() {
                override fun execute(request: org.schabi.newpipe.extractor.downloader.Request): org.schabi.newpipe.extractor.downloader.Response {
                    val connection = URL(request.url()).openConnection() as HttpURLConnection
                    connection.requestMethod = request.httpMethod()
                    connection.connectTimeout = 15000
                    connection.readTimeout = 15000
                    connection.setRequestProperty("User-Agent", USER_AGENT)
                    connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
                    request.headers().forEach { (key, values) ->
                        values.forEach { value -> connection.setRequestProperty(key, value) }
                    }
                    val responseCode = connection.responseCode
                    if (responseCode !in 200..299) {
                        throw IOException("HTTP $responseCode for ${request.url()}")
                    }
                    val responseBody = connection.inputStream.bufferedReader().readText()
                    val responseHeaders = connection.headerFields.mapValues { it.value }
                    return org.schabi.newpipe.extractor.downloader.Response(
                        responseCode,
                        connection.responseMessage,
                        responseHeaders,
                        responseBody,
                        request.url()
                    )
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun search(query: String): SearchOutcome = withContext(Dispatchers.IO) {
        var youTubeError: String? = null
        val youTubeResults = mutableListOf<SearchResult>()

        // Intento 1: YouTube
        try {
            val service = ServiceList.YouTube
            val searchHandler = service.searchQHFactory.fromQuery(query)
            val searchInfo = SearchInfo.getInfo(service, searchHandler)
            for (item in searchInfo.relatedItems.take(20)) {
                if (item is StreamInfoItem) {
                    youTubeResults.add(
                        SearchResult(
                            url = item.url,
                            title = item.name,
                            artist = item.uploaderName,
                            thumbnailUrl = null
                        )
                    )
                }
            }
        } catch (e: Exception) {
            youTubeError = e.javaClass.simpleName + ": " + (e.message ?: "sin detalle")
        }

        if (youTubeResults.isNotEmpty()) {
            return@withContext SearchOutcome(youTubeResults, null, "YouTube")
        }

        // Intento 2: iTunes (previews de 30s con carátula)
        var itunesError: String? = null
        val itunesResults = mutableListOf<SearchResult>()
        try {
            val encoded = URLEncoder.encode(query, "UTF-8")
            val url = "https://itunes.apple.com/search?term=$encoded&media=music&limit=20"
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty("User-Agent", USER_AGENT)
            val text = connection.inputStream.bufferedReader().readText()
            val json = JSONObject(text)
            val arr = json.getJSONArray("results")
            for (i in 0 until arr.length()) {
                val track = arr.getJSONObject(i)
                val preview = track.optString("previewUrl", "")
                if (preview.isNotEmpty()) {
                    itunesResults.add(
                        SearchResult(
                            url = preview,
                            title = track.optString("trackName", "Sin título"),
                            artist = track.optString("artistName", "Desconocido"),
                            thumbnailUrl = track.optString("artworkUrl100", "").ifEmpty { null }
                        )
                    )
                }
            }
        } catch (e: Exception) {
            itunesError = e.javaClass.simpleName + ": " + (e.message ?: "sin detalle")
        }

        if (itunesResults.isNotEmpty()) {
            return@withContext SearchOutcome(itunesResults, null, "iTunes (preview 30s)")
        }

        val combined = "YouTube: ${youTubeError ?: "sin resultados"} | iTunes: ${itunesError ?: "sin resultados"}"
        SearchOutcome(emptyList(), combined, "ninguna")
    }
}
