package rocks.spotifast.mobile.playback

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Resolutor de streaming de audio independiente para Spotifast.
 * Permite reproducir cualquier pista directamente dentro de la aplicación con ExoPlayer
 * sin depender de la app oficial de Spotify ni abrir intents externos.
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

    suspend fun resolveStreamUrl(title: String, artist: String, previewUrl: String?): String? = withContext(Dispatchers.IO) {
        val cacheKey = "$title - $artist".trim().lowercase()

        // 1. Revisar caché en memoria
        streamCache[cacheKey]?.let { return@withContext it }

        // 2. Si la pista ya tiene preview_url válida de Spotify, usarla como base
        if (!previewUrl.isNullOrEmpty() && (previewUrl.startsWith("http://") || previewUrl.startsWith("https://"))) {
            streamCache[cacheKey] = previewUrl
            return@withContext previewUrl
        }

        val cleanTitle = cleanSearchTerm(title)
        val cleanArtist = cleanSearchTerm(artist)
        val query = "$cleanTitle $cleanArtist".trim()

        // 3. Consultar servicio público de audio stream de alta fidelidad (JioSaavn API libre)
        try {
            val encodedQuery = URLEncoder.encode(query, "UTF-8")
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
                            // Obtener la mejor calidad (320kbps o 160kbps)
                            val bestQuality = downloadUrls.getJSONObject(downloadUrls.length() - 1)
                            val url = bestQuality.optString("url")
                            if (url.isNotEmpty()) {
                                Log.i(TAG, "Audio stream resuelto para: $query")
                                streamCache[cacheKey] = url
                                return@withContext url
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Fallo al resolver stream primario: ${e.message}")
        }

        // 4. Fallback secundario: Piped audio stream
        try {
            val encodedQuery = URLEncoder.encode("$query audio", "UTF-8")
            val request = Request.Builder()
                .url("https://pipedapi.kavin.rocks/search?q=$encodedQuery&filter=music_songs")
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
                        val urlPath = firstItem.optString("url") // e.g. "/watch?v=..."
                        val videoId = urlPath.substringAfter("v=").substringBefore("&")
                        if (videoId.isNotEmpty()) {
                            // Consultar stream directo de audio
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
                                            Log.i(TAG, "Piped audio stream resuelto para: $query")
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
            Log.w(TAG, "Fallo al resolver stream secundario: ${e.message}")
        }

        // 5. Si todo falla, retornar previewUrl aunque sea nulo
        return@withContext previewUrl
    }

    private fun cleanSearchTerm(input: String): String {
        return input
            .replace(Regex("\\(feat\\..*?\\)", RegexOption.IGNORE_CASE), "")
            .replace(Regex("\\[.*?\\]"), "")
            .replace(Regex("- Remastered.*", RegexOption.IGNORE_CASE), "")
            .replace(Regex("- Single.*", RegexOption.IGNORE_CASE), "")
            .trim()
    }
}
