package com.mixcasete.app.audio

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.exceptions.ExtractionException
import org.schabi.newpipe.extractor.services.youtube.YoutubeService
import org.schabi.newpipe.extractor.stream.StreamInfo
import org.schabi.newpipe.extractor.stream.AudioStream
import java.io.IOException

data class AudioSource(
    val url: String,
    val type: SourceType,
    val title: String,
    val artist: String
)

enum class SourceType {
    YOUTUBE, LOCAL, PREVIEW
}

class SourceManager(private val context: Context) {
    
    init {
        // Inicializar NewPipe Extractor
        try {
            NewPipe.init(object : Downloader() {
                override fun execute(request: org.schabi.newpipe.extractor.downloader.Request): org.schabi.newpipe.extractor.downloader.Response {
                    val connection = java.net.URL(request.url()).openConnection() as java.net.HttpURLConnection
                    connection.requestMethod = request.httpMethod()
                    request.headers().forEach { (key, values) ->
                        values.forEach { value -> connection.setRequestProperty(key, value) }
                    }
                    val responseCode = connection.responseCode
                    val responseBody = try {
                        connection.inputStream.bufferedReader().readText()
                    } catch (e: Exception) {
                        ""
                    }
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

    suspend fun getNextSource(
        currentSource: AudioSource?,
        localUri: Uri?,
        searchQuery: String = "lofi hip hop"
    ): AudioSource? = withContext(Dispatchers.IO) {
        val sources = mutableListOf<AudioSource>()
        
        // Fuente 1: YouTube via NewPipe Extractor
        try {
            val youtubeSource = getYouTubeSource(searchQuery)
            if (youtubeSource != null) {
                sources.add(youtubeSource)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Fuente 2: Archivo local
        if (localUri != null) {
            sources.add(
                AudioSource(
                    url = localUri.toString(),
                    type = SourceType.LOCAL,
                    title = "Local File",
                    artist = "Unknown"
                )
            )
        }
        
        // Fuente 3: Preview Deezer/iTunes (30 segundos)
        try {
            val previewSource = getPreviewSource(searchQuery)
            if (previewSource != null) {
                sources.add(previewSource)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        
        // Si hay fuente actual, encontrar la siguiente
        if (currentSource != null) {
            val currentIndex = sources.indexOfFirst { it.url == currentSource.url }
            if (currentIndex >= 0 && currentIndex < sources.size - 1) {
                return@withContext sources[currentIndex + 1]
            }
        }
        
        // Devolver la primera disponible
        return@withContext sources.firstOrNull()
    }

    private suspend fun getYouTubeSource(query: String): AudioSource? = withContext(Dispatchers.IO) {
        try {
            val service = ServiceList.YouTube
            val searchQuery = "https://www.youtube.com/results?search_query=${query.replace(" ", "+")}"
            
            val searchExtractor = service.searchExtractorFactory.fromQuery(searchQuery)
            searchExtractor.fetchPage()
            
            val items = searchExtractor.initialPage.items
            if (items.isNotEmpty()) {
                val firstVideo = items[0]
                val streamUrl = firstVideo.url
                
                val streamExtractor = service.streamExtractorFactory.fromUrl(streamUrl)
                streamExtractor.fetchPage()
                
                val streamInfo = StreamInfo.getInfo(streamExtractor)
                val audioStreams = streamInfo.audioStreams
                
                if (audioStreams.isNotEmpty()) {
                    val bestAudio = audioStreams.maxByOrNull { it.bitrate }
                    if (bestAudio != null) {
                        return@withContext AudioSource(
                            url = bestAudio.content,
                            type = SourceType.YOUTUBE,
                            title = streamInfo.name,
                            artist = streamInfo.uploaderName
                        )
                    }
                }
            }
        } catch (e: ExtractionException) {
            e.printStackTrace()
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return@withContext null
    }

    private suspend fun getPreviewSource(query: String): AudioSource? = withContext(Dispatchers.IO) {
        try {
            // iTunes Search API para previews de 30 segundos
            val searchUrl = "https://itunes.apple.com/search?term=${query.replace(" ", "+")}&media=music&limit=1"
            val connection = java.net.URL(searchUrl).openConnection() as java.net.HttpURLConnection
            connection.requestMethod = "GET"
            
            val response = connection.inputStream.bufferedReader().readText()
            val json = org.json.JSONObject(response)
            val results = json.getJSONArray("results")
            
            if (results.length() > 0) {
                val track = results.getJSONObject(0)
                val previewUrl = track.optString("previewUrl", "")
                if (previewUrl.isNotEmpty()) {
                    return@withContext AudioSource(
                        url = previewUrl,
                        type = SourceType.PREVIEW,
                        title = track.getString("trackName"),
                        artist = track.getString("artistName")
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return@withContext null
    }
}
