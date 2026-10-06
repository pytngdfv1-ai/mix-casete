package com.mixcasete.app.audio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Resuelve una URL de VIDEO PROGRESIVO (video+audio juntos, mp4) para ExoPlayer.
 * Usa Invidious formatStreams (itag 18/22 = mp4 progresivo con audio).
 * Prefiere 720p para mejor calidad. Fallback: 1080p, 480p, 360p.
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

                // Agrupar streams por calidad
                val byHeight = mutableMapOf<Int, String>()  // height -> url
                var fallbackUrl: String? = null

                for (i in 0 until formats.length()) {
                    val f = formats.optJSONObject(i) ?: continue
                    val url = f.optString("url", "")
                    if (url.isEmpty() || !url.startsWith("http")) continue
                    val type = f.optString("type", "")
                    if (!type.contains("video/mp4", ignoreCase = true)) continue
                    val quality = f.optString("quality", "")
                    val height = quality.removeSuffix("p").toIntOrNull() ?: continue

                    if (fallbackUrl == null) fallbackUrl = url
                    byHeight[height] = url
                }

                // Preferencia: 720p > 1080p > 480p > 360p > cualquiera disponible
                val preferred = listOf(720, 1080, 480, 360)
                for (h in preferred) {
                    byHeight[h]?.let { return@withContext it }
                }

                // Fallback: el de mayor altura disponible (hasta 1080p para no saturar Miracast)
                val under1080 = byHeight.filterKeys { it <= 1080 }
                val best = under1080.maxByOrNull { it.key }?.value
                if (best != null) return@withContext best

                if (fallbackUrl != null) return@withContext fallbackUrl
            } catch (e: Exception) {
                continue
            }
        }
        null
    }
}
