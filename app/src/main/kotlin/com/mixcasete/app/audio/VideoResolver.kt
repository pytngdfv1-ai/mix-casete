package com.mixcasete.app.audio

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class VideoStream(
    val progressiveUrl: String? = null,
    val videoUrl: String? = null,
    val audioUrl: String? = null
) {
    val playable: Boolean get() = progressiveUrl != null || (videoUrl != null && audioUrl != null)
}

object VideoResolver {

    private const val USER_AGENT =
        "Mozilla/5.0 (Linux; Android 13; Pixel 7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Mobile Safari/537.36"

    private val INVIDIOUS_INSTANCES = listOf(
        "https://inv.nadeko.net",
        "https://invidious.nerdvpn.de",
        "https://yewtu.be",
        "https://inv.tux.pizza",
        "https://invidious.f5.si",
        "https://iv.melmac.space",
        "https://invidious.privacyredirect.com"
    )

    private val PIPED_INSTANCES = listOf(
        "https://pipedapi.kavin.rocks",
        "https://pipedapi.adminforge.de",
        "https://api.piped.projectsegfau.lt",
        "https://pipedapi.in.projectsegfau.lt",
        "https://pipedapi.leptons.xyz",
        "https://pipedapi.reallyaweso.me"
    )

    suspend fun resolve(videoId: String): VideoStream? = withContext(Dispatchers.IO) {
        for (inst in INVIDIOUS_INSTANCES) {
            val s = safe { fromInvidious(inst, videoId) }
            if (s != null && s.playable) return@withContext s
        }
        for (inst in PIPED_INSTANCES) {
            val s = safe { fromPiped(inst, videoId) }
            if (s != null && s.playable) return@withContext s
        }
        null
    }

    private inline fun safe(block: () -> VideoStream?): VideoStream? = try { block() } catch (e: Exception) { null }

    private fun get(url: String): JSONObject? {
        val c = URL(url).openConnection() as HttpURLConnection
        c.requestMethod = "GET"
        c.connectTimeout = 6000
        c.readTimeout = 6000
        c.setRequestProperty("User-Agent", USER_AGENT)
        if (c.responseCode !in 200..299) return null
        return JSONObject(c.inputStream.bufferedReader().readText())
    }

    private fun fromInvidious(inst: String, videoId: String): VideoStream? {
        val json = get("$inst/api/v1/videos/$videoId") ?: return null
        val prog = pickProgressive(json.optJSONArray("formatStreams"))
        if (prog != null) return VideoStream(progressiveUrl = prog)
        val adaptive = json.optJSONArray("adaptiveFormats") ?: return null
        val v = pickByMime(adaptive, "video", "url", "bitrate")
        val a = pickByMime(adaptive, "audio", "url", "bitrate")
        return if (v != null && a != null) VideoStream(videoUrl = v, audioUrl = a) else null
    }

    private fun fromPiped(inst: String, videoId: String): VideoStream? {
        val json = get("$inst/streams/$videoId") ?: return null
        val v = pickByMime(json.optJSONArray("videoStreams"), "video", "url", "bitrate")
        val a = pickByMime(json.optJSONArray("audioStreams"), "audio", "url", "bitrate")
        return if (v != null && a != null) VideoStream(videoUrl = v, audioUrl = a) else null
    }

    private fun pickProgressive(formats: JSONArray?): String? {
        if (formats == null) return null
        var best: String? = null
        var bestH = Int.MAX_VALUE
        var fallback: String? = null
        for (i in 0 until formats.length()) {
            val f = formats.optJSONObject(i) ?: continue
            val url = f.optString("url", "")
            if (url.isEmpty() || !url.startsWith("http")) continue
            if (!f.optString("type", "").contains("video/mp4", true)) continue
            val h = f.optString("quality", "").removeSuffix("p").toIntOrNull() ?: 0
            if (fallback == null) fallback = url
            if (h == 360) return url
            if (h in 144..720 && h < bestH) { bestH = h; best = url }
        }
        return best ?: fallback
    }

    private fun pickByMime(array: JSONArray?, kind: String, urlKey: String, bitrateKey: String): String? {
        if (array == null) return null
        var bestUrl: String? = null
        var bestBitrate = -1
        for (i in 0 until array.length()) {
            val item = array.optJSONObject(i) ?: continue
            val url = item.optString(urlKey, "")
            if (url.isEmpty() || !url.startsWith("http")) continue
            val mime = item.optString("mimeType", "").ifEmpty { item.optString("type", "") }
            if (!mime.contains(kind, true)) continue
            val br = item.optInt(bitrateKey, 0)
            if (br > bestBitrate) { bestBitrate = br; bestUrl = url }
        }
        return bestUrl
    }
}
