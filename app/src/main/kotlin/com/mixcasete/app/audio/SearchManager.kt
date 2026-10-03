package com.mixcasete.app.audio

import android.content.Context
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.search.SearchInfo
import org.schabi.newpipe.extractor.stream.StreamInfoItem

data class SearchResult(
    val url: String,
    val title: String,
    val artist: String,
    val thumbnailUrl: String?
)

class SearchManager(private val context: Context) {
    
    init {
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

    suspend fun search(query: String): List<SearchResult> = withContext(Dispatchers.IO) {
        val results = mutableListOf<SearchResult>()
        try {
            val service = ServiceList.YouTube
            val searchHandler = service.searchQHFactory.fromQuery(query)
            val searchInfo = SearchInfo.getInfo(service, searchHandler)
            val items = searchInfo.relatedItems

            for (item in items.take(20)) {
                if (item is StreamInfoItem) {
                    // NOTA: En v0.22.7 no todas las propiedades de thumbnail están disponibles
                    // en StreamInfoItem directamente. Usamos null y el fallback de RemoteImage.
                    results.add(
                        SearchResult(
                            url = item.url,
                            title = item.name,
                            artist = item.uploaderName,
                            thumbnailUrl = null  // Simplificado: usamos icono de nota musical como fallback
                        )
                    )
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        results
    }
}
