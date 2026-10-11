package rocks.spotifast.mobile.data.local

import android.content.ContentUris
import android.content.Context
import android.net.Uri
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

class LocalMediaManager(private val context: Context) {

    private val _localTracks = MutableStateFlow<List<Track>>(emptyList())
    val localTracks: StateFlow<List<Track>> = _localTracks.asStateFlow()

    suspend fun scanLocalTracks(): List<Track> = withContext(Dispatchers.IO) {
        val tracksList = mutableListOf<Track>()

        val projection = arrayOf(
            MediaStore.Audio.Media._ID,
            MediaStore.Audio.Media.TITLE,
            MediaStore.Audio.Media.ARTIST,
            MediaStore.Audio.Media.ALBUM,
            MediaStore.Audio.Media.ALBUM_ID,
            MediaStore.Audio.Media.DURATION,
            MediaStore.Audio.Media.IS_MUSIC
        )

        val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 10000"
        val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

        try {
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

                while (c.moveToNext()) {
                    val id = c.getLong(idCol)
                    val title = c.getString(titleCol) ?: "Pista desconocida"
                    val artistName = c.getString(artistCol) ?: "Artista desconocido"
                    val albumName = c.getString(albumCol) ?: "Álbum local"
                    val albumId = c.getLong(albumIdCol)
                    val duration = c.getLong(durationCol)

                    val contentUri: Uri = ContentUris.withAppendedId(
                        MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                        id
                    )

                    val albumArtUri = Uri.parse("content://media/external/audio/albumart/$albumId").toString()

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
            Log.e("LocalMediaManager", "Error al escanear música del teléfono", e)
        }

        _localTracks.value = tracksList
        tracksList
    }
}
