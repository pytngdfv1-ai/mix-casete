package com.mixcasete.app.audio

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class SearchResult(
    val url: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String?,
    val videoUrl: String? = null,
    val isPreview: Boolean = false,
    val sourceLabel: String = ""
)

data class SearchOutcome(
    val results: List<SearchResult>,
    val error: String?,
    val source: String
)

private data class ScrapedVideo(
    val videoId: String,
    val title: String,
    val author: String,
    val thumbnail: String?
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
                    val code = connection.responseCode
                    if (code !in 200..299) throw IOException("HTTP $code for ${request.url()}")
                    val body = connection.inputStream.bufferedReader().readText()
                    return org.schabi.newpipe.extractor.downloader.Response(
                        code,
                        connection.responseMessage,
                        connection.headerFields.mapValues { it.value },
                        body,
                        request.url()
                    )
                }
            })
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    suspend fun search(query: String): SearchOutcome = withContext(Dispatchers.IO) {
        // 1) Lista de resultados leyendo la pagina web normal (NO usa InnerTube -> no da 400)
        val scraped = scrapeSearch(query)
        if (scraped.isEmpty()) {
            return@withContext SearchOutcome(
                emptyList(),
                "YouTube no devolvio resultados visibles para '$query'",
                "YouTube"
            )
        }

        // 2) Para cada video, pedir el stream de audio a NewPipe (endpoint /player)
        val results = mutableListOf<SearchResult>()
        var streamError: String? = null
        for (video in scraped.take(6)) {
            val watchUrl = "https://www.youtube.com/watch?v=${video.videoId}"
            try {
                val service = ServiceList.YouTube
                val extractor = service.getStreamExtractor(watchUrl)
                extractor.fetchPage()
                val info = StreamInfo.getInfo(extractor)
                val best = info.audioStreams.maxByOrNull { it.bitrate }
                    ?: info.audioStreams.firstOrNull()
                if (best != null && best.content.isNotEmpty()) {
                    results.add(
                        SearchResult(
                            url = best.content,
                            title = video.title,
                            artist = video.author,
                            thumbnailUrl = video.thumbnail,
                            videoUrl = watchUrl,
                            isPreview = false,
                            sourceLabel = "YouTube"
                        )
                    )
                }
            } catch (e: Exception) {
                streamError = e.javaClass.simpleName + ": " + (e.message ?: "")
            }
        }

        if (results.isNotEmpty()) {
            return@withContext SearchOutcome(results, null, "YouTube (${results.size} completos)")
        }
        SearchOutcome(
            emptyList(),
            "Se encontraron videos pero YouTube bloqueo el audio: $streamError",
            "YouTube"
        )
    }

    private fun scrapeSearch(query: String): List<ScrapedVideo> {
        val out = mutableListOf<ScrapedVideo>()
        try {
            val url = "https://www.youtube.com/results?search_query=" + URLEncoder.encode(query, "UTF-8")
            val connection = URL(url).openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 15000
            connection.readTimeout = 15000
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Accept-Language", "en-US,en;q=0.9")
            connection.setRequestProperty("Cookie", "SOCS=CAI; CONSENT=YES+1")
            val html = connection.inputStream.bufferedReader().readText()

            val json = extractInitialData(html) ?: return out
            val root = JSONObject(json)

            val sections = root
                .optJSONObject("contents")
                ?.optJSONObject("twoColumnSearchResultsRenderer")
                ?.optJSONObject("primaryContents")
                ?.optJSONObject("sectionListRenderer")
                ?.optJSONArray("contents") ?: return out

            for (s in 0 until sections.length()) {
                val section = sections.optJSONObject(s) ?: continue
                val items = section
                    .optJSONObject("itemSectionRenderer")
                    ?.optJSONArray("contents") ?: continue
                for (i in 0 until items.length()) {
                    val vr = items.optJSONObject(i)?.optJSONObject("videoRenderer") ?: continue
                    val videoId = vr.optString("videoId")
                    if (videoId.isEmpty()) continue
                    val title = vr.optJSONObject("title")
                        ?.optJSONArray("runs")
                        ?.optJSONObject(0)
                        ?.optString("text") ?: ""
                    val author = vr.optJSONObject("ownerText")
                        ?.optJSONArray("runs")
                        ?.optJSONObject(0)
                        ?.optString("text") ?: ""
                    val thumbs = vr.optJSONObject("thumbnail")?.optJSONArray("thumbnails")
                    val thumb = if (thumbs != null && thumbs.length() > 0)
                        thumbs.getJSONObject(thumbs.length() - 1).optString("url") else null
                    out.add(ScrapedVideo(videoId, title, author, thumb))
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return out
    }

    private fun extractInitialData(html: String): String? {
        val markers = listOf("var ytInitialData = ", "ytInitialData = ", "window[\"ytInitialData\"] = ")
        for (m in markers) {
            val start = html.indexOf(m)
            if (start >= 0) {
                val jsonStart = start + m.length
                val end = html.indexOf(";</script>", jsonStart)
                if (end > jsonStart) return html.substring(jsonStart, end)
            }
        }
        return null
    }
}
