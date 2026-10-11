package rocks.spotifast.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
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
    onPlayAllClick: (Playlist) -> Unit,
    modifier: Modifier = Modifier
) {
    var tracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var is403Forbidden by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(playlist.id) {
        isLoading = true
        errorMessage = null
        is403Forbidden = false
        try {
            // Intentar primero con /items
            val response = SpotifyClient.apiService.getPlaylistItems(playlistId = playlist.id, limit = 50)
            if (response.isSuccessful && response.body() != null) {
                tracks = response.body()!!.items.mapNotNull { it.track }
            } else if (response.code() == 403) {
                // Probar fallback /tracks
                val fallbackRes = SpotifyClient.apiService.getPlaylistTracksFallback(playlistId = playlist.id, limit = 50)
                if (fallbackRes.isSuccessful && fallbackRes.body() != null) {
                    tracks = fallbackRes.body()!!.items.mapNotNull { it.track }
                } else {
                    is403Forbidden = true
                }
            } else {
                errorMessage = "Error al cargar canciones (${response.code()})"
            }
        } catch (e: Exception) {
            errorMessage = "Error de conexión: ${e.localizedMessage}"
        } finally {
            isLoading = false
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SpotifyBlack)
            .padding(horizontal = 16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            IconButton(onClick = onBackClick) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Atrás",
                    tint = SpotifyWhite,
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
                            .background(SpotifyCardGray)
                    )
                } else {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SpotifyCardGray,
                        modifier = Modifier.size(190.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.MusicNote, contentDescription = null, tint = SpotifySubtext, modifier = Modifier.size(64.dp))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Nombre y detalles
            Text(
                text = playlist.name,
                color = SpotifyWhite,
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )

            if (!playlist.description.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = playlist.description,
                    color = SpotifySubtext,
                    fontSize = 13.sp,
                    lineHeight = 17.sp
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Por ${playlist.owner?.displayName ?: "Spotify"} • ${playlist.tracks?.total ?: tracks.size} canciones",
                    color = SpotifySubtext,
                    fontSize = 12.sp
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Botón de reproducción grande verde (funciona para reproducir la playlist completa vía Spotify Connect)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Surface(
                    onClick = { onPlayAllClick(playlist) },
                    shape = CircleShape,
                    color = SpotifyGreen,
                    modifier = Modifier.size(54.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = "Reproducir todo",
                            tint = SpotifyBlack,
                            modifier = Modifier.size(34.dp)
                        )
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
                    CircularProgressIndicator(color = SpotifyGreen)
                }
            }
        } else if (is403Forbidden) {
            item {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SpotifyDarkGray,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Info, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "Lista editorial protegida por Spotify",
                                color = SpotifyWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Spotify restringe la lista de pistas en playlists públicas de terceros sin cuota comercial extendida. Puedes reproducirla completa pulsando el botón verde de reproducción.",
                                color = SpotifySubtext,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }
        } else if (errorMessage != null) {
            item {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SpotifyDarkGray,
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
                    text = "Esta lista de reproducción no tiene canciones disponibles.",
                    color = SpotifySubtext,
                    modifier = Modifier.padding(24.dp),
                    fontSize = 14.sp
                )
            }
        } else {
            itemsIndexed(tracks) { _, track ->
                TrackListItem(
                    track = track,
                    onClick = { onTrackClick(track, tracks) }
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
