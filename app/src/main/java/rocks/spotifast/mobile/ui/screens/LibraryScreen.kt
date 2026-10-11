package rocks.spotifast.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import rocks.spotifast.mobile.SpotifastApp
import rocks.spotifast.mobile.data.api.SpotifyClient
import rocks.spotifast.mobile.data.api.models.*
import rocks.spotifast.mobile.ui.components.TrackListItem
import rocks.spotifast.mobile.ui.theme.*

enum class LibraryTab(val label: String) {
    PLAYLISTS("Playlists"),
    ALBUMS("Álbumes"),
    ARTISTS("Artistas"),
    LIKED("Tus me gusta"),
    DOWNLOADS("Descargadas")
}

@Composable
fun LibraryScreen(
    onBackClick: () -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onTrackClick: (Track, List<Track>) -> Unit,
    modifier: Modifier = Modifier
) {
    val cacheManager = SpotifastApp.instance.cacheManager
    val localMediaManager = SpotifastApp.instance.localMediaManager
    val localTracks by localMediaManager.localTracks.collectAsState()
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableStateOf(LibraryTab.PLAYLISTS) }
    var playlists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var albums by remember { mutableStateOf<List<SavedAlbum>>(emptyList()) }
    var artists by remember { mutableStateOf<List<Artist>>(emptyList()) }
    var likedTracks by remember { mutableStateOf<List<Track>>(emptyList()) }

    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadData() {
        scope.launch {
            isLoading = true
            errorMessage = null
            try {
                when (selectedTab) {
                    LibraryTab.PLAYLISTS -> {
                        val res = SpotifyClient.apiService.getUserPlaylists(limit = 50)
                        if (res.isSuccessful && res.body() != null) {
                            playlists = res.body()!!.items
                        }
                    }
                    LibraryTab.ALBUMS -> {
                        val res = SpotifyClient.apiService.getSavedAlbums(limit = 50)
                        if (res.isSuccessful && res.body() != null) {
                            albums = res.body()!!.items
                        }
                    }
                    LibraryTab.ARTISTS -> {
                        val res = SpotifyClient.apiService.getFollowedArtists(limit = 50)
                        if (res.isSuccessful && res.body() != null) {
                            artists = res.body()!!.artists?.items ?: emptyList()
                        }
                    }
                    LibraryTab.LIKED -> {
                        val res = SpotifyClient.apiService.getSavedTracks(limit = 50)
                        if (res.isSuccessful && res.body() != null) {
                            likedTracks = res.body()!!.items.map { it.track }
                        }
                    }
                    LibraryTab.DOWNLOADS -> {
                        localMediaManager.scanLocalTracks()
                    }
                }
            } catch (e: Exception) {
                errorMessage = "Error al cargar: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(selectedTab) {
        loadData()
    }

    val downloadedAllTracks = remember(localTracks) {
        val cached = cacheManager.getDownloadedTracks()
        (localTracks + cached).distinctBy { it.id }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(LocalSpotifastColors.current.background)
            .padding(horizontal = 16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            // Barra superior: Atrás + Título "Tu biblioteca" + Botones buscar / agregar
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = onBackClick) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Atrás",
                            tint = LocalSpotifastColors.current.textPrimary,
                            modifier = Modifier.size(26.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tu biblioteca",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = LocalSpotifastColors.current.textPrimary
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = { loadData() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Recargar",
                            tint = LocalSpotifastColors.current.textSecondary,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Chips de filtrado idénticos a Captura 2 (Playlists, Álbumes, Artistas, Tus me gusta, Descargadas)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LibraryTab.values().forEach { tab ->
                    val isSelected = selectedTab == tab
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedTab = tab },
                        label = {
                            Text(
                                text = tab.label,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = LocalSpotifastColors.current.accent,
                            selectedLabelColor = LocalSpotifastColors.current.background,
                            containerColor = LocalSpotifastColors.current.card,
                            labelColor = LocalSpotifastColors.current.textPrimary
                        ),
                        shape = RoundedCornerShape(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Subtítulo con ordenación (Reproducido recientemente)
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Sort,
                    contentDescription = null,
                    tint = LocalSpotifastColors.current.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Reproducido recientemente",
                    color = LocalSpotifastColors.current.textSecondary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(10.dp))
        }

        // CONTENIDO SEGÚN PESTAÑA
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
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        } else {
            when (selectedTab) {
                LibraryTab.PLAYLISTS -> {
                    if (playlists.isEmpty()) {
                        item {
                            EmptyLibraryMessage("No tienes listas de reproducción guardadas.")
                        }
                    } else {
                        items(playlists) { playlist ->
                            PlaylistLibraryRow(
                                playlist = playlist,
                                onClick = { onPlaylistClick(playlist) }
                            )
                        }
                    }
                }
                LibraryTab.ALBUMS -> {
                    if (albums.isEmpty()) {
                        item {
                            EmptyLibraryMessage("No tienes álbumes guardados.")
                        }
                    } else {
                        items(albums) { savedAlbum ->
                            val album = savedAlbum.album
                            AlbumLibraryRow(
                                album = album,
                                onClick = {
                                    // Crear playlist virtual para reproducir el álbum
                                    val fakePl = Playlist(
                                        id = album.id,
                                        name = album.name,
                                        description = "Álbum de ${album.name}",
                                        uri = album.uri ?: "spotify:album:${album.id}",
                                        images = album.images,
                                        owner = PlaylistOwner(id = "spotify", displayName = "Spotify"),
                                        tracks = PlaylistTracksInfo(total = 20)
                                    )
                                    onPlaylistClick(fakePl)
                                }
                            )
                        }
                    }
                }
                LibraryTab.ARTISTS -> {
                    if (artists.isEmpty()) {
                        item {
                            EmptyLibraryMessage("No sigues a ningún artista aún.")
                        }
                    } else {
                        items(artists) { artist ->
                            ArtistLibraryRow(artist = artist)
                        }
                    }
                }
                LibraryTab.LIKED -> {
                    if (likedTracks.isEmpty()) {
                        item {
                            EmptyLibraryMessage("No tienes canciones con 'Me gusta' aún.")
                        }
                    } else {
                        itemsIndexed(likedTracks) { _, track ->
                            TrackListItem(
                                track = track,
                                onClick = { onTrackClick(track, likedTracks) }
                            )
                        }
                    }
                }
                LibraryTab.DOWNLOADS -> {
                    if (downloadedAllTracks.isEmpty()) {
                        item {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text(
                                    text = "No se encontraron canciones en el almacenamiento del teléfono.",
                                    color = LocalSpotifastColors.current.textSecondary,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Button(
                                    onClick = {
                                        scope.launch { localMediaManager.scanLocalTracks() }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = LocalSpotifastColors.current.accent)
                                ) {
                                    Text("Escanear almacenamiento", color = LocalSpotifastColors.current.background, fontWeight = FontWeight.Bold)
                                }
                            }
                        }
                    } else {
                        itemsIndexed(downloadedAllTracks) { _, track ->
                            TrackListItem(
                                track = track,
                                onClick = { onTrackClick(track, downloadedAllTracks) }
                            )
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun EmptyLibraryMessage(text: String) {
    Text(
        text = text,
        color = LocalSpotifastColors.current.textSecondary,
        fontSize = 14.sp,
        modifier = Modifier.padding(vertical = 32.dp)
    )
}

@Composable
fun PlaylistLibraryRow(
    playlist: Playlist,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val cover = playlist.getCoverUrl(300)
        if (!cover.isNullOrEmpty()) {
            AsyncImage(
                model = cover,
                contentDescription = playlist.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(LocalSpotifastColors.current.card)
            )
        } else {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = LocalSpotifastColors.current.card,
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = LocalSpotifastColors.current.textSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = playlist.name,
                color = LocalSpotifastColors.current.textPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Playlist • ${playlist.owner?.displayName ?: "Spotify"}",
                color = LocalSpotifastColors.current.textSecondary,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun AlbumLibraryRow(
    album: Album,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val cover = album.getCoverUrl(300)
        if (!cover.isNullOrEmpty()) {
            AsyncImage(
                model = cover,
                contentDescription = album.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(54.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(LocalSpotifastColors.current.card)
            )
        } else {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = LocalSpotifastColors.current.card,
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Album, contentDescription = null, tint = LocalSpotifastColors.current.textSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = album.name,
                color = LocalSpotifastColors.current.textPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Álbum • ${album.releaseDate?.take(4) ?: ""}",
                color = LocalSpotifastColors.current.textSecondary,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
fun ArtistLibraryRow(artist: Artist) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val img = artist.getImageUrl()
        if (!img.isNullOrEmpty()) {
            AsyncImage(
                model = img,
                contentDescription = artist.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(54.dp)
                    .clip(CircleShape)
                    .background(LocalSpotifastColors.current.card)
            )
        } else {
            Surface(
                shape = CircleShape,
                color = LocalSpotifastColors.current.card,
                modifier = Modifier.size(54.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.Person, contentDescription = null, tint = LocalSpotifastColors.current.textSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = artist.name,
                color = LocalSpotifastColors.current.textPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "Artista",
                color = LocalSpotifastColors.current.textSecondary,
                fontSize = 13.sp
            )
        }
    }
}
