package com.mixcasete.app.audio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
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
    private const val GLOBAL_TIMEOUT_MS = 15000L
    private val cache = mutableMapOf<String, Pair<Long, String>>()

    private val PIPED_INSTANCES = listOf(
        "https://pipedapi.kavin.rocks",
        "https://pipedapi.adminforge.de",
        "https://api.piped.projectsegfau.lt",
        "https://pipedapi.in.projectsegfau.lt",
        "https://pipedapi.reallyaweso.me"
    )

    private val INVIDIOUS_INSTANCES = listOf(
        "https://inv.nadeko.net",
        "https://invidious.nerdvpn.de",
        "https://yewtu.be",
        "https://inv.tux.pizza",
        "https://invidious.f5.si"
    )

    suspend fun resolveAudioUrl(videoId: String): String? {
        cache[videoId]?.let { (ts, url) ->
            if (System.currentTimeMillis() - ts < CACHE_TTL_MS) return url
        }

        val resolved = withContext(Dispatchers.IO) {
            coroutineScope {
                val channel = Channel<String?>(Channel.UNLIMITED)
                val jobs = mutableListOf<Job>()

                jobs.add(launch { channel.send(safe { viaNewPipe(videoId) }) })
                jobs.add(launch { channel.send(safe { viaPiped(videoId) }) })
                jobs.add(launch { channel.send(safe { viaInvidious(videoId) }) })

                var result: String? = null
                var terminadas = 0
                val total = jobs.size

                withTimeoutOrNull(GLOBAL_TIMEOUT_MS) {
                    while (terminadas < total && result == null) {
                        val r = channel.receive()
                        terminadas++
                        if (r != null) result = r
                    }
                    result
                }.also {
                    jobs.forEach { j -> j.cancel() }
                }
            }
        }

        if (resolved != null) cache[videoId] = System.currentTimeMillis() to resolved
        return resolved
    }

    private inline fun safe(block: () -> String?): String? {
        return try { block() } catch (e: Exception) { null }
    }

    private fun viaNewPipe(videoId: String): String? {
        val watchUrl = "https://www.youtube.com/watch?v=$videoId"
        val extractor = ServiceList.YouTube.getStreamExtractor(watchUrl)
        extractor.fetchPage()
        val info = StreamInfo.getInfo(extractor)
        return (info.audioStreams.maxByOrNull { it.bitrate } ?: info.audioStreams.firstOrNull())?.content
    }

    private fun viaPiped(videoId: String): String? {
        for (instance in PIPED_INSTANCES) {
            val url = safe {
                val connection = URL("$instance/streams/$videoId").openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.setRequestProperty("User-Agent", USER_AGENT)
                if (connection.responseCode !in 200..299) null
                else {
                    val json = JSONObject(connection.inputStream.bufferedReader().readText())
                    pickBest(json.optJSONArray("audioStreams") ?: return@safe null, "url", "bitrate", "mimeType")
                }
            }
            if (url != null) return url
        }
        return null
    }

    private fun viaInvidious(videoId: String): String? {
        for (instance in INVIDIOUS_INSTANCES) {
            val url = safe {
                val connection = URL("$instance/api/v1/videos/$videoId").openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.setRequestProperty("User-Agent", USER_AGENT)
                if (connection.responseCode !in 200..299) null
                else {
                    val json = JSONObject(connection.inputStream.bufferedReader().readText())
                    pickBest(json.optJSONArray("adaptiveFormats") ?: return@safe null, "url", "bitrate", "type")
                }
            }
            if (url != null) return url
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
