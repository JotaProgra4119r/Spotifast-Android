package rocks.spotifast.mobile.playback

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Resolutor de streaming de audio independiente para Spotifast.
 * Resuelve y reproduce pistas COMPLETAS directamente dentro de la aplicación con ExoPlayer
 * sin depender de la app oficial de Spotify ni limitarse a previews de 30 segundos.
 */
object AudioStreamResolver {

    private const val TAG = "AudioStreamResolver"
    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    // Caché en memoria para evitar consultas repetidas de la misma canción
    private val streamCache = ConcurrentHashMap<String, String>()

    private val INVIDIOUS_INSTANCES = listOf(
        "https://invidious.f5.si",
        "https://inv.nadeko.net",
        "https://invidious.nerdvpn.de"
    )

    suspend fun resolveStreamUrl(title: String, artist: String, previewUrl: String?): String? = withContext(Dispatchers.IO) {
        val cacheKey = "$title - $artist".trim().lowercase()

        // 1. Revisar caché en memoria
        streamCache[cacheKey]?.let { return@withContext it }

        val cleanTitle = cleanSearchTerm(title)
        val cleanArtist = cleanSearchTerm(artist)
        val query = "$cleanTitle $cleanArtist".trim()
        val encodedQuery = URLEncoder.encode(query, "UTF-8")

        // 2. Layer 1: Invidious Audio Engine para canciones COMPLETAS (Full Track)
        for (instance in INVIDIOUS_INSTANCES) {
            try {
                val searchReq = Request.Builder()
                    .url("$instance/api/v1/search?q=$encodedQuery&type=video")
                    .header("User-Agent", "Spotifast/1.0 (Android)")
                    .build()

                val searchRes = client.newCall(searchReq).execute()
                if (searchRes.isSuccessful) {
                    val body = searchRes.body?.string()
                    if (!body.isNullOrEmpty() && body.startsWith("[")) {
                        val items = JSONArray(body)
                        if (items.length() > 0) {
                            val videoId = items.getJSONObject(0).optString("videoId")
                            if (videoId.isNotEmpty()) {
                                val vidReq = Request.Builder()
                                    .url("$instance/api/v1/videos/$videoId")
                                    .header("User-Agent", "Spotifast/1.0 (Android)")
                                    .build()
                                val vidRes = client.newCall(vidReq).execute()
                                if (vidRes.isSuccessful) {
                                    val vidBody = vidRes.body?.string()
                                    if (!vidBody.isNullOrEmpty()) {
                                        val vidJson = JSONObject(vidBody)
                                        val adaptive = vidJson.optJSONArray("adaptiveFormats")
                                        if (adaptive != null && adaptive.length() > 0) {
                                            var bestUrl = ""
                                            var bestBitrate = 0
                                            for (i in 0 until adaptive.length()) {
                                                val fmt = adaptive.getJSONObject(i)
                                                val type = fmt.optString("type")
                                                if (type.contains("audio")) {
                                                    val url = fmt.optString("url")
                                                    val bitrate = fmt.optInt("bitrate", 0)
                                                    if (url.isNotEmpty() && bitrate >= bestBitrate) {
                                                        bestBitrate = bitrate
                                                        bestUrl = url
                                                    }
                                                }
                                            }
                                            if (bestUrl.isNotEmpty()) {
                                                Log.i(TAG, "Canción COMPLETA resuelta vía Invidious ($instance): $query")
                                                streamCache[cacheKey] = bestUrl
                                                return@withContext bestUrl
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            } catch (e: Exception) {
                Log.d(TAG, "Intento Invidious falló en $instance: ${e.message}")
            }
        }

        // 3. Layer 2: Fallback Piped audio stream para canciones completas
        try {
            val pipedQuery = URLEncoder.encode("$query audio", "UTF-8")
            val request = Request.Builder()
                .url("https://pipedapi.kavin.rocks/search?q=$pipedQuery&filter=music_songs")
                .header("User-Agent", "Spotifast/1.0 (Android)")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyStr = response.body?.string()
                if (!bodyStr.isNullOrEmpty()) {
                    val json = JSONObject(bodyStr)
                    val items = json.optJSONArray("items")
                    if (items != null && items.length() > 0) {
                        val firstItem = items.getJSONObject(0)
                        val urlPath = firstItem.optString("url")
                        val videoId = urlPath.substringAfter("v=").substringBefore("&")
                        if (videoId.isNotEmpty()) {
                            val streamReq = Request.Builder()
                                .url("https://pipedapi.kavin.rocks/streams/$videoId")
                                .build()
                            val streamRes = client.newCall(streamReq).execute()
                            if (streamRes.isSuccessful) {
                                val streamBody = streamRes.body?.string()
                                if (!streamBody.isNullOrEmpty()) {
                                    val streamJson = JSONObject(streamBody)
                                    val audioStreams = streamJson.optJSONArray("audioStreams")
                                    if (audioStreams != null && audioStreams.length() > 0) {
                                        val audioObj = audioStreams.getJSONObject(0)
                                        val streamUrl = audioObj.optString("url")
                                        if (streamUrl.isNotEmpty()) {
                                            Log.i(TAG, "Piped audio completo resuelto para: $query")
                                            streamCache[cacheKey] = streamUrl
                                            return@withContext streamUrl
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fallo al resolver vía Piped: ${e.message}")
        }

        // 4. Layer 3: JioSaavn API pública
        try {
            val request = Request.Builder()
                .url("https://saavn.dev/api/search/songs?query=$encodedQuery&page=1&limit=3")
                .header("User-Agent", "Spotifast/1.0 (Android)")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyStr = response.body?.string()
                if (!bodyStr.isNullOrEmpty()) {
                    val json = JSONObject(bodyStr)
                    val data = json.optJSONObject("data")
                    val results = data?.optJSONArray("results")
                    if (results != null && results.length() > 0) {
                        val firstMatch = results.getJSONObject(0)
                        val downloadUrls = firstMatch.optJSONArray("downloadUrl")
                        if (downloadUrls != null && downloadUrls.length() > 0) {
                            val bestQuality = downloadUrls.getJSONObject(downloadUrls.length() - 1)
                            val url = bestQuality.optString("url")
                            if (url.isNotEmpty()) {
                                Log.i(TAG, "Audio stream resuelto vía Saavn para: $query")
                                streamCache[cacheKey] = url
                                return@withContext url
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fallo al resolver vía Saavn: ${e.message}")
        }

        // 5. Layer 4: iTunes Global Search API (Fallback rápido si no hay conexión a full streams)
        try {
            val request = Request.Builder()
                .url("https://itunes.apple.com/search?term=$encodedQuery&entity=song&limit=1")
                .header("User-Agent", "Spotifast/1.0 (Android)")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyStr = response.body?.string()
                if (!bodyStr.isNullOrEmpty()) {
                    val json = JSONObject(bodyStr)
                    val results = json.optJSONArray("results")
                    if (results != null && results.length() > 0) {
                        val firstMatch = results.getJSONObject(0)
                        val preview = firstMatch.optString("previewUrl")
                        if (preview.isNotEmpty() && (preview.startsWith("http://") || preview.startsWith("https://"))) {
                            Log.i(TAG, "Audio stream fallback vía iTunes: $query")
                            streamCache[cacheKey] = preview
                            return@withContext preview
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fallo al resolver vía iTunes: ${e.message}")
        }

        // 6. Layer 5: Deezer API
        try {
            val request = Request.Builder()
                .url("https://api.deezer.com/search?q=$encodedQuery&limit=1")
                .header("User-Agent", "Spotifast/1.0 (Android)")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyStr = response.body?.string()
                if (!bodyStr.isNullOrEmpty()) {
                    val json = JSONObject(bodyStr)
                    val data = json.optJSONArray("data")
                    if (data != null && data.length() > 0) {
                        val firstMatch = data.getJSONObject(0)
                        val preview = firstMatch.optString("preview")
                        if (preview.isNotEmpty() && (preview.startsWith("http://") || preview.startsWith("https://"))) {
                            Log.i(TAG, "Audio stream fallback vía Deezer: $query")
                            streamCache[cacheKey] = preview
                            return@withContext preview
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fallo al resolver vía Deezer: ${e.message}")
        }

        // 7. Retornar previewUrl original de Spotify si existiera
        return@withContext previewUrl
    }

    private fun cleanSearchTerm(input: String): String {
        return input
            .replace(Regex("\\(feat\\..*?\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\(ft\\..*?\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\(with.*?\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\[.*?\\]"), "")
            .replace(Regex("- Remastered.*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("- Single.*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("- Radio Edit.*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("- Live.*", RegexOption.IGNORE_CASE), "")
            .trim()
    }
}
