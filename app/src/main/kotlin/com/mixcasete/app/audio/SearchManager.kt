package com.mixcasete.app.audio

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

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

    // SOLO YouTube. Sin iTunes.
    suspend fun search(query: String): SearchOutcome = withContext(Dispatchers.IO) {
        var youTubeError: String? = null
        val youTubeResults = mutableListOf<SearchResult>()

        try {
            val service = ServiceList.YouTube
            val searchHandler = service.searchQHFactory.fromQuery(query)
            val searchInfo = SearchInfo.getInfo(service, searchHandler)

            for (item in searchInfo.relatedItems.take(8)) {
                if (item is StreamInfoItem) {
                    var audioUrl: String? = null
                    try {
                        val extractor = service.getStreamExtractor(item.url)
                        extractor.fetchPage()
                        val streamInfo = StreamInfo.getInfo(extractor)
                        val audioStreams = streamInfo.audioStreams
                        if (audioStreams.isNotEmpty()) {
                            val bestAudio = audioStreams.maxByOrNull { it.bitrate } ?: audioStreams.first()
                            if (bestAudio.content.isNotEmpty()) {
                                audioUrl = bestAudio.content
                            }
                        }
                    } catch (e: Exception) {
                    }

                    if (audioUrl != null) {
                        youTubeResults.add(
                            SearchResult(
                                url = audioUrl,
                                title = item.name,
                                artist = item.uploaderName,
                                thumbnailUrl = null,
                                videoUrl = item.url,
                                isPreview = false,
                                sourceLabel = "YouTube"
                            )
                        )
                    }
                }
            }
        } catch (e: Exception) {
            youTubeError = e.javaClass.simpleName + ": " + (e.message ?: "sin detalle")
        }

        if (youTubeResults.isNotEmpty()) {
            return@withContext SearchOutcome(youTubeResults, null, "YouTube (${youTubeResults.size} temas completos)")
        }

        SearchOutcome(emptyList(), youTubeError ?: "YouTube no devolvio streams de audio", "YouTube")
    }
}
