package com.mixcasete.app.audio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Cliente InnerTube directo (API interna de YouTube) con clientes ANDROID y
 * TVHTML5_SIMPLY_EMBEDDED_PLAYER. Habla directo con YouTube, sin terceros.
 */
object InnerTubeClient {

    private const val API_KEY = "AIzaSyA8eiZmM1FaDVjRy-df2KTyQ_vz_yYM39w"
    private const val UA_ANDROID = "com.google.android.youtube/19.09.37 (Linux; U; Android 11; en_US) gzip"

    suspend fun resolveUrl(videoId: String): String? = withContext(Dispatchers.IO) {
        tryClient(androidClient(), videoId, null)
            ?: tryClient(embeddedClient(), videoId, "https://www.youtube.com/embed/$videoId")
    }

    private fun androidClient() = JSONObject().apply {
        put("clientName", "ANDROID")
        put("clientVersion", "19.09.37")
        put("androidSdkVersion", 30)
        put("hl", "en")
        put("gl", "US")
    }

    private fun embeddedClient() = JSONObject().apply {
        put("clientName", "TVHTML5_SIMPLY_EMBEDDED_PLAYER")
        put("clientVersion", "2.20240102.00.00")
        put("hl", "en")
        put("gl", "US")
    }

    private fun tryClient(client: JSONObject, videoId: String, thirdParty: String?): String? {
        return try {
            val conn = URL("https://www.youtube.com/youtubei/v1/player?key=$API_KEY&prettyPrint=false")
                .openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.connectTimeout = 8000
            conn.readTimeout = 8000
            conn.doOutput = true
            conn.setRequestProperty("Content-Type", "application/json")
            conn.setRequestProperty("User-Agent", UA_ANDROID)

            val context = JSONObject().apply { put("client", client) }
            if (thirdParty != null) {
                context.put("thirdParty", JSONObject().apply { put("embedUrl", thirdParty) })
            }
            val payload = JSONObject().apply {
                put("context", context)
                put("videoId", videoId)
            }
            conn.outputStream.use { it.write(payload.toString().toByteArray(Charsets.UTF_8)) }
            if (conn.responseCode !in 200..299) return null
            pickUrl(JSONObject(conn.inputStream.bufferedReader().readText()))
        } catch (e: Exception) {
            null
        }
    }

    private fun pickUrl(json: JSONObject): String? {
        val status = json.optJSONObject("playabilityStatus")?.optString("status")
        if (status != "OK") return null
        val streaming = json.optJSONObject("streamingData") ?: return null

        // 1) Progresivo (video+audio juntos)
        val formats = streaming.optJSONArray("formats")
        if (formats != null) {
            for (i in 0 until formats.length()) {
                val f = formats.optJSONObject(i) ?: continue
                val url = f.optString("url", "")
                if (url.startsWith("http")) return url
            }
        }
        // 2) Audio adaptativo de mayor bitrate con URL directa
        val adaptive = streaming.optJSONArray("adaptiveFormats") ?: return null
        var bestUrl: String? = null
        var bestBitrate = -1
        for (i in 0 until adaptive.length()) {
            val f = adaptive.optJSONObject(i) ?: continue
            if (!f.optString("mimeType", "").startsWith("audio/")) continue
            val url = f.optString("url", "")
            if (!url.startsWith("http")) continue
            val br = f.optInt("bitrate", 0)
            if (br > bestBitrate) { bestBitrate = br; bestUrl = url }
        }
        return bestUrl
    }
}
