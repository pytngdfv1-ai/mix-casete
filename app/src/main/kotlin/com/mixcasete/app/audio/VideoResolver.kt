package com.mixcasete.app.audio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Resuelve una URL de VIDEO PROGRESIVO (video+audio juntos, mp4) para ExoPlayer.
 * Usa Invidious formatStreams (itag 18/22 = mp4 progresivo con audio).
 * Prefiere 360p para reducir bitrate y evitar entrecorte en Miracast.
 */
object VideoResolver {

    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    private val INVIDIOUS_INSTANCES = listOf(
        "https://inv.nadeko.net",
        "https://invidious.nerdvpn.de",
        "https://yewtu.be",
        "https://inv.tux.pizza",
        "https://invidious.f5.si"
    )

    suspend fun resolveProgressiveVideoUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        for (instance in INVIDIOUS_INSTANCES) {
            try {
                val connection = URL("$instance/api/v1/videos/$videoId").openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 6000
                connection.readTimeout = 6000
                connection.setRequestProperty("User-Agent", USER_AGENT)
                if (connection.responseCode !in 200..299) continue

                val json = JSONObject(connection.inputStream.bufferedReader().readText())
                val formats = json.optJSONArray("formatStreams") ?: continue

                // Buscar mp4 progresivo; preferir 360p, si no el de menor altura (menos bitrate)
                var bestUrl: String? = null
                var bestHeight = Int.MAX_VALUE
                var fallbackUrl: String? = null

                for (i in 0 until formats.length()) {
                    val f = formats.optJSONObject(i) ?: continue
                    val url = f.optString("url", "")
                    if (url.isEmpty() || !url.startsWith("http")) continue
                    val type = f.optString("type", "")
                    if (!type.contains("video/mp4", ignoreCase = true)) continue
                    val quality = f.optString("quality", "")
                    val height = quality.removeSuffix("p").toIntOrNull() ?: 0

                    if (fallbackUrl == null) fallbackUrl = url
                    if (height == 360) {
                        return@withContext url
                    }
                    if (height in 144..480 && height < bestHeight) {
                        bestHeight = height
                        bestUrl = url
                    }
                }

                val result = bestUrl ?: fallbackUrl
                if (result != null) return@withContext result
            } catch (e: Exception) {
                continue
            }
        }
        null
    }
}
