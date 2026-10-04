package com.mixcasete.app.audio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.stream.StreamInfo
import java.net.HttpURLConnection
import java.net.URL

/**
 * Resuelve la URL de audio de un videoId de YouTube probando varias vias en orden:
 * 1) NewPipe Extractor (endpoint /player)
 * 2) Instancias Piped
 * 3) Instancias Invidious
 * Devuelve la primera URL util, o null si todas fallan.
 */
object PipedResolver {

    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

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

    suspend fun resolveAudioUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        val watchUrl = "https://www.youtube.com/watch?v=$videoId"

        // VIA 1: NewPipe Extractor (player)
        try {
            val service = ServiceList.YouTube
            val extractor = service.getStreamExtractor(watchUrl)
            extractor.fetchPage()
            val info = StreamInfo.getInfo(extractor)
            val best = info.audioStreams.maxByOrNull { it.bitrate }
                ?: info.audioStreams.firstOrNull()
            val url = best?.content
            if (!url.isNullOrEmpty()) return@withContext url
        } catch (e: Exception) {
            // seguir a la siguiente via
        }

        // VIA 2: Piped
        for (instance in PIPED_INSTANCES) {
            val url = tryPiped(instance, videoId)
            if (url != null) return@withContext url
        }

        // VIA 3: Invidious
        for (instance in INVIDIOUS_INSTANCES) {
            val url = tryInvidious(instance, videoId)
            if (url != null) return@withContext url
        }

        null
    }

    private fun tryPiped(instance: String, videoId: String): String? {
        return try {
            val connection = URL("$instance/streams/$videoId").openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Accept", "application/json")
            if (connection.responseCode !in 200..299) return null
            val json = JSONObject(connection.inputStream.bufferedReader().readText())
            val streams = json.optJSONArray("audioStreams") ?: return null
            pickBest(streams, "url", "bitrate", "mimeType")
        } catch (e: Exception) {
            null
        }
    }

    private fun tryInvidious(instance: String, videoId: String): String? {
        return try {
            val connection = URL("$instance/api/v1/videos/$videoId").openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 8000
            connection.readTimeout = 8000
            connection.setRequestProperty("User-Agent", USER_AGENT)
            connection.setRequestProperty("Accept", "application/json")
            if (connection.responseCode !in 200..299) return null
            val json = JSONObject(connection.inputStream.bufferedReader().readText())
            val formats = json.optJSONArray("adaptiveFormats") ?: return null
            // En Invidious los campos son: url, bitrate, type (ej "audio/mp4")
            pickBest(formats, "url", "bitrate", "type")
        } catch (e: Exception) {
            null
        }
    }

    private fun pickBest(
        array: org.json.JSONArray,
        urlKey: String,
        bitrateKey: String,
        mimeKey: String
    ): String? {
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
