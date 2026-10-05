package com.mixcasete.app.audio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.json.JSONArray
import org.json.JSONObject
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.net.HttpURLConnection
import java.net.URL

object PipedResolver {

    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    private const val CACHE_TTL_MS = 45 * 60 * 1000L
    private val cache = mutableMapOf<String, Pair<Long, String>>()

    private val PIPED_INSTANCES = listOf(
        "https://pipedapi.kavin.rocks",
        "https://pipedapi.adminforge.de",
        "https://api.piped.projectsegfau.lt"
    )

    private val INVIDIOUS_INSTANCES = listOf(
        "https://inv.nadeko.net",
        "https://invidious.nerdvpn.de",
        "https://yewtu.be"
    )

    suspend fun resolveAudioUrl(videoId: String): String? {
        cache[videoId]?.let { (ts, url) ->
            if (System.currentTimeMillis() - ts < CACHE_TTL_MS) return url
        }

        val resolved = withContext(Dispatchers.IO) {
            coroutineScope {
                val defs = listOf(
                    async { viaNewPipe(videoId) },
                    async { viaPiped(videoId) },
                    async { viaInvidious(videoId) }
                )
                val all = withTimeoutOrNull(6000) { defs.awaitAll() }
                if (all != null) {
                    all.firstOrNull { !it.isNullOrEmpty() }
                } else {
                    defs.firstNotNullOfOrNull { d ->
                        runCatching { if (d.isCompleted) d.getCompleted() else null }.getOrNull()
                    }
                }
            }
        }

        if (resolved != null) cache[videoId] = System.currentTimeMillis() to resolved
        return resolved
    }

    private fun viaNewPipe(videoId: String): String? {
        return try {
            val watchUrl = "https://www.youtube.com/watch?v=$videoId"
            val extractor = ServiceList.YouTube.getStreamExtractor(watchUrl)
            extractor.fetchPage()
            val info = StreamInfo.getInfo(extractor)
            (info.audioStreams.maxByOrNull { it.bitrate } ?: info.audioStreams.firstOrNull())?.content
        } catch (e: Exception) { null }
    }

    private fun viaPiped(videoId: String): String? {
        for (instance in PIPED_INSTANCES) {
            try {
                val connection = URL("$instance/streams/$videoId").openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 4000
                connection.readTimeout = 4000
                connection.setRequestProperty("User-Agent", USER_AGENT)
                if (connection.responseCode !in 200..299) continue
                val json = JSONObject(connection.inputStream.bufferedReader().readText())
                val audioStreams = json.optJSONArray("audioStreams") ?: continue
                val url = pickBest(audioStreams, "url", "bitrate", "mimeType")
                if (url != null) return url
            } catch (e: Exception) {
                continue
            }
        }
        return null
    }

    private fun viaInvidious(videoId: String): String? {
        for (instance in INVIDIOUS_INSTANCES) {
            try {
                val connection = URL("$instance/api/v1/videos/$videoId").openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 4000
                connection.readTimeout = 4000
                connection.setRequestProperty("User-Agent", USER_AGENT)
                if (connection.responseCode !in 200..299) continue
                val json = JSONObject(connection.inputStream.bufferedReader().readText())
                val formats = json.optJSONArray("adaptiveFormats") ?: continue
                val url = pickBest(formats, "url", "bitrate", "type")
                if (url != null) return url
            } catch (e: Exception) {
                continue
            }
        }
        return null
    }

    private fun pickBest(array: JSONArray, urlKey: String, bitrateKey: String, mimeKey: String): String? {
        var bestUrl: String? = null
        var bestBitrate = -1
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val url = item.optString(urlKey, "")
            if (url.isEmpty() || !url.startsWith("http")) continue
            val mime = item.optString(mimeKey, "")
            if (!mime.contains("audio", ignoreCase = true)) continue
            val bitrate = item.optInt(bitrateKey, 0)
            if (bitrate > bestBitrate) {
                bestBitrate = bitrate
                bestUrl = url
            }
        }
        return bestUrl
    }
}
