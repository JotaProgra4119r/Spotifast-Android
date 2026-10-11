package rocks.spotifast.mobile.data.local

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import rocks.spotifast.mobile.data.api.models.Track
import java.io.File

class OfflineCacheManager(private val context: Context) {

    private val prefs = context.getSharedPreferences("spotifast_cache", Context.MODE_PRIVATE)
    private val gson = Gson()
    private val downloadedTracksDir = File(context.filesDir, "audio_offline").apply { mkdirs() }

    private val _downloadedTrackIds = MutableStateFlow<Set<String>>(emptySet())
    val downloadedTrackIds: StateFlow<Set<String>> = _downloadedTrackIds.asStateFlow()

    init {
        loadDownloadedTrackIds()
    }

    private fun loadDownloadedTrackIds() {
        val set = prefs.getStringSet("downloaded_ids", emptySet()) ?: emptySet()
        _downloadedTrackIds.value = set
    }

    fun isTrackDownloaded(trackId: String): Boolean {
        return _downloadedTrackIds.value.contains(trackId)
    }

    fun markTrackDownloaded(track: Track) {
        val current = _downloadedTrackIds.value.toMutableSet()
        current.add(track.id)
        _downloadedTrackIds.value = current
        prefs.edit().putStringSet("downloaded_ids", current).apply()

        // Guardar metadatos en disco
        val metaFile = File(downloadedTracksDir, "${track.id}.json")
        metaFile.writeText(gson.toJson(track))
    }

    fun removeDownloadedTrack(trackId: String) {
        val current = _downloadedTrackIds.value.toMutableSet()
        current.remove(trackId)
        _downloadedTrackIds.value = current
        prefs.edit().putStringSet("downloaded_ids", current).apply()

        val metaFile = File(downloadedTracksDir, "$trackId.json")
        if (metaFile.exists()) metaFile.delete()

        val audioFile = File(downloadedTracksDir, "$trackId.cache")
        if (audioFile.exists()) audioFile.delete()
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
