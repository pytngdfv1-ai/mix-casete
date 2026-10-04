package com.mixcasete.app.audio

import android.content.Context
import android.net.Uri
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

data class AudioSource(
    val url: String,
    val type: SourceType,
    val title: String,
    val artist: String
)

enum class SourceType {
    YOUTUBE, LOCAL
}

class SourceManager(private val context: Context) {

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

    // SOLO YouTube (+ archivo local opcional). Sin iTunes.
    suspend fun getNextSource(
        currentSource: AudioSource?,
        localUri: Uri?,
        searchQuery: String = "lofi hip hop"
    ): AudioSource? = withContext(Dispatchers.IO) {
        val sources = mutableListOf<AudioSource>()

        try {
            val service = ServiceList.YouTube
            val searchHandler = service.searchQHFactory.fromQuery(searchQuery)
            val searchInfo = SearchInfo.getInfo(service, searchHandler)
            for (item in searchInfo.relatedItems.take(5)) {
                if (item is StreamInfoItem) {
                    try {
                        val extractor = service.getStreamExtractor(item.url)
                        extractor.fetchPage()
                        val streamInfo = StreamInfo.getInfo(extractor)
                        val bestAudio = streamInfo.audioStreams.maxByOrNull { it.bitrate }
                        if (bestAudio != null && bestAudio.content.isNotEmpty()) {
                            sources.add(
                                AudioSource(
                                    url = bestAudio.content,
                                    type = SourceType.YOUTUBE,
                                    title = streamInfo.name,
                                    artist = streamInfo.uploaderName
                                )
                            )
                        }
                    } catch (e: Exception) {
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        if (localUri != null) {
            sources.add(
                AudioSource(
                    url = localUri.toString(),
                    type = SourceType.LOCAL,
                    title = "Archivo local",
                    artist = "Desconocido"
                )
            )
        }

        if (currentSource != null) {
            val idx = sources.indexOfFirst { it.url == currentSource.url }
            if (idx >= 0 && idx < sources.size - 1) {
                return@withContext sources[idx + 1]
            }
        }
        sources.firstOrNull()
    }
}
