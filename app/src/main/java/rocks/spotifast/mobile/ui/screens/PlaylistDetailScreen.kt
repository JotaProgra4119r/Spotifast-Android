package rocks.spotifast.mobile.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import rocks.spotifast.mobile.SpotifastApp
import rocks.spotifast.mobile.data.api.SpotifyClient
import rocks.spotifast.mobile.data.api.models.Playlist
import rocks.spotifast.mobile.data.api.models.Track
import rocks.spotifast.mobile.ui.components.TrackListItem
import rocks.spotifast.mobile.ui.theme.*

@Composable
fun PlaylistDetailScreen(
    playlist: Playlist,
    onBackClick: () -> Unit,
    onTrackClick: (Track, List<Track>) -> Unit,
    onPlayAllClick: (Playlist, List<Track>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val cacheManager = SpotifastApp.instance.cacheManager
    val downloadedIds by cacheManager.downloadedTrackIds.collectAsState()

    var tracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgressText by remember { mutableStateOf<String?>(null) }

    val isPlaylistFullyDownloaded = tracks.isNotEmpty() && tracks.all { downloadedIds.contains(it.id) }

    LaunchedEffect(playlist.id) {
        isLoading = true
        errorMessage = null
        try {
            var fetchedTracks: List<Track> = emptyList()

            val response = SpotifyClient.apiService.getPlaylistItems(playlistId = playlist.id, limit = 50)
            if (response.isSuccessful && response.body() != null) {
                fetchedTracks = response.body()!!.items.mapNotNull { it.track }
            } else {
                val fallbackRes = SpotifyClient.apiService.getPlaylistTracksFallback(playlistId = playlist.id, limit = 50)
                if (fallbackRes.isSuccessful && fallbackRes.body() != null) {
                    fetchedTracks = fallbackRes.body()!!.items.mapNotNull { it.track }
                }
            }

            if (fetchedTracks.isEmpty()) {
                val searchKeywords = when {
                    !playlist.description.isNullOrEmpty() -> playlist.description.replace("and more", "").trim()
                    playlist.name.contains("Radio", ignoreCase = true) -> playlist.name.replace("Radio", "").trim()
                    else -> playlist.name
                }

                val searchRes = SpotifyClient.apiService.search(query = searchKeywords, limit = 20)
                if (searchRes.isSuccessful && searchRes.body()?.tracks?.items?.isNotEmpty() == true) {
                    fetchedTracks = searchRes.body()!!.tracks!!.items
                } else {
                    val topRes = SpotifyClient.apiService.getTopTracks(limit = 20)
                    if (topRes.isSuccessful && topRes.body()?.items?.isNotEmpty() == true) {
                        fetchedTracks = topRes.body()!!.items
                    }
                }
            }

            tracks = fetchedTracks
        } catch (e: Exception) {
            errorMessage = "Error de conexión: ${e.localizedMessage}"
        } finally {
            isLoading = false
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(LocalSpotifastColors.current.background)
            .padding(horizontal = 16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Atrás",
                    tint = LocalSpotifastColors.current.textPrimary,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Portada grande de la Playlist
            val coverUrl = playlist.getCoverUrl(640)
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                if (!coverUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = playlist.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(190.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(LocalSpotifastColors.current.card)
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = LocalSpotifastColors.current.card,
                        modifier = Modifier.size(190.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = LocalSpotifastColors.current.textSecondary, modifier = Modifier.size(64.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nombre y detalles
            Text(
                text = playlist.name,
                color = LocalSpotifastColors.current.textPrimary,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            if (!playlist.description.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = playlist.description,
                    color = LocalSpotifastColors.current.textSecondary,
                    fontSize = 13.sp,
                    lineHeight = 17.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Por ${playlist.owner?.displayName ?: "Spotify"} • ${tracks.size.coerceAtLeast(playlist.tracks?.total ?: 0)} canciones",
                    color = LocalSpotifastColors.current.textSecondary,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Barra de acciones: Botón de Descargar Spotifast, Botón Aleatorio y Botón Play
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (downloadProgressText != null) {
                    Text(
                        text = "Descargando: $downloadProgressText",
                        color = LocalSpotifastColors.current.accent,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Botón de descargar canciones de Spotifast para modo offline
                    Surface(
                        onClick = {
                            if (isDownloading) return@Surface
                            if (isPlaylistFullyDownloaded) {
                                cacheManager.removeDownloadedPlaylist(tracks)
                                Toast.makeText(context, "Descargas eliminadas de Spotifast", Toast.LENGTH_SHORT).show()
                            } else {
                                if (tracks.isNotEmpty()) {
                                    isDownloading = true
                                    Toast.makeText(context, "Descargando ${tracks.size} canciones en Spotifast...", Toast.LENGTH_SHORT).show()
                                    coroutineScope.launch {
                                        cacheManager.downloadPlaylist(tracks) { completed, total ->
                                            downloadProgressText = "$completed/$total"
                                        }
                                        isDownloading = false
                                        downloadProgressText = null
                                        Toast.makeText(context, "¡Playlist descargada para modo offline!", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        shape = CircleShape,
                        color = if (isPlaylistFullyDownloaded) LocalSpotifastColors.current.accent.copy(alpha = 0.2f) else LocalSpotifastColors.current.card,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isDownloading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = LocalSpotifastColors.current.accent,
                                    strokeWidth = 2.dp
                                )
                            } else if (isPlaylistFullyDownloaded) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Descargada en Spotifast",
                                    tint = LocalSpotifastColors.current.accent,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.ArrowCircleDown,
                                    contentDescription = "Descargar canciones de la playlist",
                                    tint = LocalSpotifastColors.current.textSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Botón de reproducción aleatoria
                    Surface(
                        onClick = {
                            if (tracks.isNotEmpty()) {
                                SpotifastApp.instance.playbackManager.setShuffle(true)
                                val shuffled = tracks.shuffled()
                                onPlayAllClick(playlist, shuffled)
                            }
                        },
                        shape = CircleShape,
                        color = LocalSpotifastColors.current.card,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Reproducción aleatoria",
                                tint = LocalSpotifastColors.current.accent,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(14.dp))

                    // Botón de reproducción grande
                    Surface(
                        onClick = { onPlayAllClick(playlist, tracks) },
                        shape = CircleShape,
                        color = LocalSpotifastColors.current.accent,
                        modifier = Modifier.size(54.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = "Reproducir todo",
                                tint = LocalSpotifastColors.current.background,
                                modifier = Modifier.size(34.dp)
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = LocalSpotifastColors.current.accent)
                }
            }
        } else if (errorMessage != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = LocalSpotifastColors.current.card,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp),
                        fontSize = 13.sp
                    )
                }
            }
        } else if (tracks.isEmpty()) {
            item {
                Text(
                    text = "Cargando canciones para este mix...",
                    color = LocalSpotifastColors.current.textSecondary,
                    modifier = Modifier.padding(24.dp),
                    fontSize = 14.sp
                )
            }
        } else {
            itemsIndexed(tracks) { _, track ->
                TrackListItem(
                    track = track,
                    isDownloaded = downloadedIds.contains(track.id),
                    onClick = { onTrackClick(track, tracks) },
                    onToggleDownload = {
                        coroutineScope.launch {
                            if (downloadedIds.contains(track.id)) {
                                cacheManager.removeDownloadedTrack(track.id)
                                Toast.makeText(context, "Canción eliminada del almacenamiento", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Descargando ${track.name}...", Toast.LENGTH_SHORT).show()
                                val ok = cacheManager.downloadTrack(track)
                                if (ok) {
                                    Toast.makeText(context, "¡${track.name} descargada!", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
