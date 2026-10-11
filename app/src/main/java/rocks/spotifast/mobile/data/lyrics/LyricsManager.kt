package rocks.spotifast.mobile.data.lyrics

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.net.URLEncoder
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

data class LyricLine(
    val timeMs: Long,
    val text: String
)

data class TrackLyrics(
    val lines: List<LyricLine>,
    val isSynced: Boolean,
    val isInstrumental: Boolean
)

object LyricsManager {

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    private val timePattern = Pattern.compile("\\[(\\d{2}):(\\d{2})\\.(\\d{2,3})\\](.*)")

    suspend fun fetchLyrics(artist: String, title: String, album: String? = null, durationMs: Long? = null): TrackLyrics? = withContext(Dispatchers.IO) {
        val cleanTitle = cleanSongTitle(title)
        val cleanArtist = artist.trim()

        // 1. Intentar get exacto en LRCLIB (idéntico a Spotifast desktop src/lyrics.rs)
        val exactLyrics = tryGetExact(cleanArtist, cleanTitle, album, durationMs)
        if (exactLyrics != null) return@withContext exactLyrics

        // 2. Fallback a búsqueda general en LRCLIB
        return@withContext trySearch(cleanArtist, cleanTitle)
    }

    private fun tryGetExact(artist: String, title: String, album: String?, durationMs: Long?): TrackLyrics? {
        try {
            val urlBuilder = StringBuilder("https://lrclib.net/api/get?")
            urlBuilder.append("artist_name=").append(URLEncoder.encode(artist, "UTF-8"))
            urlBuilder.append("&track_name=").append(URLEncoder.encode(title, "UTF-8"))
            if (!album.isNullOrEmpty()) {
                urlBuilder.append("&album_name=").append(URLEncoder.encode(album, "UTF-8"))
            }
            if (durationMs != null && durationMs > 0) {
                urlBuilder.append("&duration=").append(durationMs / 1000)
            }

            val request = Request.Builder()
                .url(urlBuilder.toString())
                .header("User-Agent", "Spotifast/0.36.0 (https://spotifast.rocks)")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyStr = response.body?.string() ?: return null
                val json = JSONObject(bodyStr)
                return parseLyricsJson(json)
            }
        } catch (e: Exception) {
            Log.d("LyricsManager", "Error en exact lyrics: ${e.message}")
        }
        return null
    }

    private fun trySearch(artist: String, title: String): TrackLyrics? {
        try {
            val query = "$artist $title"
            val url = "https://lrclib.net/api/search?q=" + URLEncoder.encode(query, "UTF-8")
            val request = Request.Builder()
                .url(url)
                .header("User-Agent", "Spotifast/0.36.0 (https://spotifast.rocks)")
                .build()

            val response = client.newCall(request).execute()
            if (response.isSuccessful) {
                val bodyStr = response.body?.string() ?: return null
                val array = JSONArray(bodyStr)
                if (array.length() > 0) {
                    val first = array.getJSONObject(0)
                    return parseLyricsJson(first)
                }
            }
        } catch (e: Exception) {
            Log.d("LyricsManager", "Error en search lyrics: ${e.message}")
        }
        return null
    }

    private fun parseLyricsJson(json: JSONObject): TrackLyrics {
        val instrumental = json.optBoolean("instrumental", false)
        if (instrumental) {
            return TrackLyrics(emptyList(), isSynced = true, isInstrumental = true)
        }

        val synced = json.optString("syncedLyrics", "")
        if (synced.isNotEmpty()) {
            val lines = parseSyncedLyrics(synced)
            if (lines.isNotEmpty()) {
                return TrackLyrics(lines, isSynced = true, isInstrumental = false)
            }
        }

        val plain = json.optString("plainLyrics", "")
        if (plain.isNotEmpty()) {
            val lines = plain.lines().mapIndexed { index, line ->
                LyricLine(timeMs = index * 3000L, text = line.trim())
            }.filter { it.text.isNotEmpty() }
            return TrackLyrics(lines, isSynced = false, isInstrumental = false)
        }

        return TrackLyrics(emptyList(), isSynced = false, isInstrumental = false)
    }

    private fun parseSyncedLyrics(lrcText: String): List<LyricLine> {
        val list = mutableListOf<LyricLine>()
        for (line in lrcText.lines()) {
            val matcher = timePattern.matcher(line.trim())
            if (matcher.matches()) {
                val min = matcher.group(1)?.toLongOrNull() ?: 0L
                val sec = matcher.group(2)?.toLongOrNull() ?: 0L
                val msStr = matcher.group(3) ?: "0"
                val ms = if (msStr.length == 2) msStr.toLong() * 10 else msStr.toLong()
                val totalMs = (min * 60 + sec) * 1000 + ms
                val text = matcher.group(4)?.trim() ?: ""
                list.add(LyricLine(totalMs, text))
            }
        }
        return list.sortedBy { it.timeMs }
    }

    private fun cleanSongTitle(title: String): String {
        return title
            .replace(Regex(" - [0-9]{4} Remaster.*"), "")
            .replace(Regex(" - Remastered.*"), "")
            .replace(Regex("\\(feat\\..*?\\)"), "")
            .replace(Regex("\\[feat\\..*?\\]"), "")
            .trim()
    }
}
