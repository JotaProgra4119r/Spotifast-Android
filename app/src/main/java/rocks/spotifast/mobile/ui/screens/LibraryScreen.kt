package rocks.spotifast.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import rocks.spotifast.mobile.SpotifastApp
import rocks.spotifast.mobile.data.api.SpotifyClient
import rocks.spotifast.mobile.data.api.models.Track
import rocks.spotifast.mobile.ui.components.TrackListItem
import rocks.spotifast.mobile.ui.theme.*

@Composable
fun LibraryScreen(
    onBackClick: () -> Unit,
    onTrackClick: (Track, List<Track>) -> Unit,
    modifier: Modifier = Modifier
) {
    val cacheManager = SpotifastApp.instance.cacheManager
    val localMediaManager = SpotifastApp.instance.localMediaManager
    val downloadedIds by cacheManager.downloadedTrackIds.collectAsState()
    val localTracks by localMediaManager.localTracks.collectAsState()
    val scope = rememberCoroutineScope()

    var selectedTab by remember { mutableIntStateOf(0) } // 0: Me gusta, 1: Descargadas / Teléfono
    var likedTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var totalLikedCount by remember { mutableIntStateOf(0) }
    var isLoadingLiked by remember { mutableStateOf(true) }
    var isScanningLocal by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    fun loadLikedTracks() {
        scope.launch {
            isLoadingLiked = true
            errorMessage = null
            try {
                val response = SpotifyClient.apiService.getSavedTracks(limit = 50, offset = 0)
                if (response.isSuccessful && response.body() != null) {
                    val body = response.body()!!
                    val list = body.items.map { it.track }
                    likedTracks = list
                    totalLikedCount = body.total
                } else {
                    errorMessage = "Error al obtener canciones de Spotify (${response.code()})"
                }
            } catch (e: Exception) {
                errorMessage = "Sin conexión a Spotify: ${e.localizedMessage}"
            } finally {
                isLoadingLiked = false
            }
        }
    }

    fun scanDeviceMusic() {
        scope.launch {
            isScanningLocal = true
            localMediaManager.scanLocalTracks()
            isScanningLocal = false
        }
    }

    LaunchedEffect(Unit) {
        loadLikedTracks()
        scanDeviceMusic()
    }

    val displayedTracks = remember(selectedTab, likedTracks, localTracks, downloadedIds) {
        if (selectedTab == 1) {
            // Combinar archivos locales del almacenamiento del teléfono + descargas en caché
            val cached = cacheManager.getDownloadedTracks()
            (localTracks + cached).distinctBy { it.id }
        } else {
            likedTracks
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Atrás",
                        tint = SpotifyWhite,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Row {
                    if (selectedTab == 1) {
                        IconButton(onClick = { scanDeviceMusic() }) {
                            Icon(
                                imageVector = Icons.Default.FolderOpen,
                                contentDescription = "Escanear música local",
                                tint = SpotifyGreen
                            )
                        }
                    }
                    IconButton(onClick = { loadLikedTracks() }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refrescar",
                            tint = SpotifySubtext
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (selectedTab == 0) "Tus me gusta" else "Descargas y Locales",
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Bold,
                        color = SpotifyWhite
                    )
                    Text(
                        text = if (selectedTab == 0) {
                            if (isLoadingLiked) "Cargando canciones..." else "$totalLikedCount canciones guardadas"
                        } else {
                            if (isScanningLocal) "Escaneando almacenamiento..." else "${displayedTracks.size} canciones en este teléfono"
                        },
                        fontSize = 13.sp,
                        color = SpotifySubtext
                    )
                }

                // Botón verde flotante de reproducción
                if (displayedTracks.isNotEmpty()) {
                    Surface(
                        shape = CircleShape,
                        color = SpotifyGreen,
                        modifier = Modifier
                            .size(54.dp)
                            .clickable { onTrackClick(displayedTracks.first(), displayedTracks) }
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
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Selector de pestañas
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    label = { Text("Tus me gusta (${likedTracks.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SpotifyGreen,
                        selectedLabelColor = SpotifyBlack,
                        containerColor = SpotifyCardGray,
                        labelColor = SpotifyWhite
                    )
                )

                FilterChip(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    label = { Text("Descargadas / Locales (${localTracks.size + downloadedIds.size})") },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = SpotifyGreen,
                        selectedLabelColor = SpotifyBlack,
                        containerColor = SpotifyCardGray,
                        labelColor = SpotifyWhite
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        if (selectedTab == 0 && isLoadingLiked) {
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
        } else if (selectedTab == 1 && isScanningLocal) {
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
        } else if (selectedTab == 0 && errorMessage != null && displayedTracks.isEmpty()) {
            item {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SpotifyDarkGray,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(text = errorMessage ?: "", color = MaterialTheme.colorScheme.error, fontSize = 14.sp)
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(onClick = { loadLikedTracks() }) {
                            Text("Reintentar")
                        }
                    }
                }
            }
        } else if (displayedTracks.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = if (selectedTab == 1) Icons.Default.FolderOpen else Icons.Default.FavoriteBorder,
                        contentDescription = null,
                        tint = SpotifySubtext,
                        modifier = Modifier.size(48.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = if (selectedTab == 1)
                            "No se encontraron canciones en el almacenamiento de este teléfono."
                        else
                            "No tienes canciones marcadas con 'Me gusta' en Spotify.",
                        color = SpotifySubtext,
                        fontSize = 14.sp
                    )
                    if (selectedTab == 1) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = { scanDeviceMusic() },
                            colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen)
                        ) {
                            Text("Escanear almacenamiento", color = SpotifyBlack, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else {
            itemsIndexed(displayedTracks) { _, track ->
                val isDownloaded = track.isLocal || downloadedIds.contains(track.id)
                TrackListItem(
                    track = track,
                    isDownloaded = isDownloaded,
                    onClick = { onTrackClick(track, displayedTracks) },
                    onToggleDownload = {
                        if (isDownloaded) {
                            cacheManager.removeDownloadedTrack(track.id)
                        } else {
                            cacheManager.markTrackDownloaded(track)
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
