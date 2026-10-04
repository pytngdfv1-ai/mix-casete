package com.mixcasete.app.audio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Obtiene la URL de un stream de audio para un videoId de YouTube usando Piped.
 * Piped es un servicio open-source con multiples instancias: si una falla, se prueba la siguiente.
 */
object PipedResolver {

    private val INSTANCES = listOf(
        "https://pipedapi.kavin.rocks",
        "https://pipedapi.adminforge.de",
        "https://api.piped.projectsegfau.lt",
        "https://pipedapi.in.projectsegfau.lt",
        "https://watchapi.whatever.social"
    )

    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    /**
     * Devuelve una URL de audio reproducible, o null si ninguna instancia pudo resolverlo.
     */
    suspend fun resolveAudioUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        val errors = mutableListOf<String>()
        for (instance in INSTANCES) {
            val url = "$instance/streams/$videoId"
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 10000
                connection.readTimeout = 10000
                connection.setRequestProperty("User-Agent", USER_AGENT)
                connection.setRequestProperty("Accept", "application/json")

                val code = connection.responseCode
                if (code !in 200..299) {
                    errors.add("$instance: HTTP $code")
                    continue
                }

                val text = connection.inputStream.bufferedReader().readText()
                val json = JSONObject(text)

                val audioStreams = json.optJSONArray("audioStreams") ?: continue
                var bestUrl: String? = null
                var bestBitrate = -1

                for (i in 0 until audioStreams.length()) {
                    val stream = audioStreams.optJSONObject(i) ?: continue
                    val streamUrl = stream.optString("url", "")
                    if (streamUrl.isEmpty()) continue
                    val mime = stream.optString("mimeType", "")
                    val bitrate = stream.optInt("bitrate", 0)
                    if (mime.contains("audio") || mime.contains("mp4a") || mime.contains("opus") || mime.contains("webm")) {
                        if (bitrate > bestBitrate) {
                            bestBitrate = bitrate
                            bestUrl = streamUrl
                        }
                    }
                }

                if (bestUrl != null && bestUrl.isNotEmpty()) {
                    return@withContext bestUrl
                } else {
                    errors.add("$instance: sin audioStreams")
                }
            } catch (e: Exception) {
                errors.add("$instance: ${e.javaClass.simpleName}")
            }
        }
        println("PipedResolver: ninguna instancia resolvio $videoId: $errors")
        null
    }
}
