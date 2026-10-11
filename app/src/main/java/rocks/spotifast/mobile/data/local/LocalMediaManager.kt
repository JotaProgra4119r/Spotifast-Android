package rocks.spotifast.mobile.data.local

import android.content.ContentUris
import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import rocks.spotifast.mobile.data.api.models.Album
import rocks.spotifast.mobile.data.api.models.Artist
import rocks.spotifast.mobile.data.api.models.SpotifyImage
import rocks.spotifast.mobile.data.api.models.Track
import java.io.File

class LocalMediaManager(private val context: Context) {

    private val _localTracks = MutableStateFlow<List<Track>>(emptyList())
    val localTracks: StateFlow<List<Track>> = _localTracks.asStateFlow()

    suspend fun scanLocalTracks(): List<Track> = withContext(Dispatchers.IO) {
        val tracksList = mutableListOf<Track>()
        val seenPaths = mutableSetOf<String>()

        // 1. Escaneo mediante MediaStore (sin filtro restrictivo IS_MUSIC para compatibilidad con Android 14/15 ColorOS)
        try {
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.ALBUM_ID,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.DATA
            )

            val selection = "${MediaStore.Audio.Media.DURATION} > 3000"
            val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

            val cursor = context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )

            cursor?.use { c ->
                val idCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val albumIdCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)
                val durationCol = c.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val dataCol = c.getColumnIndex(MediaStore.Audio.Media.DATA)

                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val title = c.getString(titleCol) ?: "Pista desconocida"
                    val artistName = c.getString(artistCol) ?: "Artista desconocido"
                    val albumName = c.getString(albumCol) ?: "Álbum local"
                    val albumId = c.getLong(albumIdCol)
                    val duration = c.getLong(durationCol)
                    val filePath = if (dataCol >= 0) c.getString(dataCol) else null

                    if (filePath != null) {
                        seenPaths.add(filePath)
                    }

                    val contentUri: Uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val albumArtUri = "content://media/external/audio/albumart/$albumId"

                    val track = Track(
                        id = "local_$id",
                        name = title,
                        uri = contentUri.toString(),
                        durationMs = duration,
                        previewUrl = contentUri.toString(),
                        artists = listOf(Artist(id = "local_artist_$id", name = artistName, uri = null)),
                        album = Album(
                            id = "local_album_$albumId",
                            name = albumName,
                            uri = null,
                            images = listOf(SpotifyImage(url = albumArtUri, height = 300, width = 300)),
                            releaseDate = null
                        ),
                        isPlayable = true,
                        isLocal = true,
                        isDownloaded = true
                    )
                    tracksList.add(track)
                }
            }
        } catch (e: Exception) {
            Log.e("LocalMediaManager", "Error al consultar MediaStore", e)
        }

        // 2. Escaneo directo de carpetas Descargas y Música en almacenamiento externo
        try {
            val audioExtensions = setOf("mp3", "m4a", "aac", "flac", "wav", "ogg", "opus")
            val directoriesToScan = listOfNotNull(
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC),
                Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                File("/sdcard/Music"),
                File("/sdcard/Download")
            ).filter { it.exists() && it.isDirectory }

            val retriever = MediaMetadataRetriever()

            for (dir in directoriesToScan) {
                dir.walkTopDown().maxDepth(3).forEach { file ->
                    if (file.isFile && audioExtensions.contains(file.extension.lowercase())) {
                        val path = file.absolutePath
                        if (!seenPaths.contains(path)) {
                            seenPaths.add(path)
                            try {
                                retriever.setDataSource(path)
                                val title = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
                                    ?: file.nameWithoutExtension
                                val artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)
                                    ?: "Artista local"
                                val album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)
                                    ?: "Descargas"
                                val durationStr = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)
                                val duration = durationStr?.toLongOrNull() ?: 180000L

                                val fileUri = Uri.fromFile(file).toString()

                                val track = Track(
                                    id = "file_${file.hashCode()}",
                                    name = title,
                                    uri = fileUri,
                                    durationMs = duration,
                                    previewUrl = fileUri,
                                    artists = listOf(Artist(id = "artist_${file.hashCode()}", name = artist, uri = null)),
                                    album = Album(
                                        id = "album_${file.hashCode()}",
                                        name = album,
                                        uri = null,
                                        images = null,
                                        releaseDate = null
                                    ),
                                    isPlayable = true,
                                    isLocal = true,
                                    isDownloaded = true
                                )
                                tracksList.add(track)
                            } catch (e: Exception) {
                                Log.w("LocalMediaManager", "Error leyendo metadatos de ${file.name}: ${e.message}")
                            }
                        }
                    }
                }
            }
            try { retriever.release() } catch (_: Exception) {}
        } catch (e: Exception) {
            Log.e("LocalMediaManager", "Error en escaneo directo de carpetas", e)
        }

        _localTracks.value = tracksList
        tracksList
    }
}
