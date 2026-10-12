package rocks.spotifast.mobile.data.local

import android.content.Context
import android.net.Uri
import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import rocks.spotifast.mobile.data.api.models.Track
import rocks.spotifast.mobile.playback.AudioStreamResolver
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class OfflineCacheManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("spotifast_cache", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val downloadedTracksDir = File(context.filesDir, "audio_offline").apply { mkdirs() }

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    private val _downloadedTrackIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadedTrackIds: StateFlow<Set<String>> = _downloadedTrackIds.asStateFlow()

    init {
        loadDownloadedTrackIds()
    }

    private fun loadDownloadedTrackIds() {
        val set = prefs.getStringSet("downloaded_ids", emptySet()) ?: emptySet()
        val validSet = set.filter { id ->
            val audioFile = File(downloadedTracksDir, "$id.mp3")
            audioFile.exists() && audioFile.length() > 1000
        }.toSet()

        _downloadedTrackIds.value = validSet
        if (validSet.size != set.size) {
            prefs.edit().putStringSet("downloaded_ids", validSet).apply()
        }
    }

    fun isTrackDownloaded(trackId: String): Boolean {
        return _downloadedTrackIds.value.contains(trackId)
    }

    fun getTrackAudioUri(trackId: String): String? {
        val audioFile = File(downloadedTracksDir, "$trackId.mp3")
        return if (audioFile.exists() && audioFile.length() > 1000) {
            Uri.fromFile(audioFile).toString()
        } else {
            null
        }
    }

    suspend fun downloadTrack(track: Track): Boolean = withContext(Dispatchers.IO) {
        if (isTrackDownloaded(track.id)) return@withContext true

        val streamUrl = AudioStreamResolver.resolveStreamUrl(
            title = track.name,
            artist = track.getArtistNames(),
            previewUrl = track.previewUrl
        ) ?: track.previewUrl

        if (streamUrl.isNullOrEmpty()) {
            Log.w("OfflineCacheManager", "No se pudo resolver URL de audio para ${track.name}")
            return@withContext false
        }

        val tmpFile = File(downloadedTracksDir, "${track.id}.tmp")
        val finalFile = File(downloadedTracksDir, "${track.id}.mp3")
        val metaFile = File(downloadedTracksDir, "${track.id}.json")

        try {
            val request = Request.Builder()
                .url(streamUrl)
                .header("User-Agent", "Spotifast/1.0 (Android)")
                .build()

            val response = client.newCall(request).execute()
            if (!response.isSuccessful || response.body == null) {
                Log.w("OfflineCacheManager", "Error HTTP al descargar ${track.name}: ${response.code}")
                return@withContext false
            }

            response.body!!.byteStream().use { input ->
                FileOutputStream(tmpFile).use { output ->
                    input.copyTo(output)
                }
            }

            if (tmpFile.length() > 5000) {
                if (finalFile.exists()) finalFile.delete()
                tmpFile.renameTo(finalFile)

                val offlineTrack = track.copy(
                    uri = Uri.fromFile(finalFile).toString(),
                    isDownloaded = true,
                    isLocal = true
                )
                metaFile.writeText(gson.toJson(offlineTrack))

                val current = _downloadedTrackIds.value.toMutableSet()
                current.add(track.id)
                _downloadedTrackIds.value = current
                prefs.edit().putStringSet("downloaded_ids", current).apply()

                Log.i("OfflineCacheManager", "Pista descargada con éxito: ${track.name} (${finalFile.length() / 1024} KB)")
                return@withContext true
            } else {
                tmpFile.delete()
                return@withContext false
            }
        } catch (e: Exception) {
            Log.e("OfflineCacheManager", "Error al descargar pista ${track.name}", e)
            if (tmpFile.exists()) tmpFile.delete()
            return@withContext false
        }
    }

    suspend fun downloadPlaylist(
        tracks: List<Track>,
        onProgress: (completed: Int, total: Int) -> Unit
    ): Int = withContext(Dispatchers.IO) {
        var completed = 0
        val total = tracks.size

        for (track in tracks) {
            if (isTrackDownloaded(track.id)) {
                completed++
                onProgress(completed, total)
                continue
            }

            val success = downloadTrack(track)
            if (success) {
                completed++
            }
            onProgress(completed, total)
        }
        return@withContext completed
    }

    fun removeDownloadedTrack(trackId: String) {
        val current = _downloadedTrackIds.value.toMutableSet()
        current.remove(trackId)
        _downloadedTrackIds.value = current
        prefs.edit().putStringSet("downloaded_ids", current).apply()

        val metaFile = File(downloadedTracksDir, "$trackId.json")
        if (metaFile.exists()) metaFile.delete()

        val audioFile = File(downloadedTracksDir, "$trackId.mp3")
        if (audioFile.exists()) audioFile.delete()
    }

    fun removeDownloadedPlaylist(tracks: List<Track>) {
        tracks.forEach { removeDownloadedTrack(it.id) }
    }

    fun isPlaylistDownloaded(tracks: List<Track>): Boolean {
        if (tracks.isEmpty()) return false
        val current = _downloadedTrackIds.value
        return tracks.all { current.contains(it.id) }
    }

    fun getDownloadedTracks(): List<Track> {
        val files = downloadedTracksDir.listFiles { _, name -> name.endsWith(".json") } ?: return emptyList()
        return files.mapNotNull { file ->
            try {
                gson.fromJson(file.readText(), Track::class.java).apply {
                    isDownloaded = true
                }
            } catch (e: Exception) {
                null
            }
        }
    }

    fun getCacheSizeMb(): Long {
        var totalBytes: Long = 0
        downloadedTracksDir.listFiles()?.forEach { totalBytes += it.length() }
        context.cacheDir.listFiles()?.forEach { totalBytes += it.length() }
        return totalBytes / (1024 * 1024)
    }

    fun clearCache() {
        downloadedTracksDir.deleteRecursively()
        downloadedTracksDir.mkdirs()
        _downloadedTrackIds.value = emptySet()
        prefs.edit().remove("downloaded_ids").apply()
    }
}
