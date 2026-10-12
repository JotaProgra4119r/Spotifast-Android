package rocks.spotifast.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import rocks.spotifast.mobile.SpotifastApp
import rocks.spotifast.mobile.data.api.SpotifyClient
import rocks.spotifast.mobile.data.api.models.Track
import rocks.spotifast.mobile.data.api.models.UserProfile
import rocks.spotifast.mobile.ui.components.TrackListItem
import rocks.spotifast.mobile.ui.theme.*

@Composable
fun LikedSongsScreen(
    userProfile: UserProfile?,
    onBackClick: () -> Unit,
    onTrackClick: (Track, List<Track>) -> Unit,
    onPlayAllClick: (List<Track>) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val context = androidx.compose.ui.platform.LocalContext.current
    val cacheManager = SpotifastApp.instance.cacheManager
    val downloadedIds by cacheManager.downloadedTrackIds.collectAsState()

    var tracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var totalCount by remember { mutableIntStateOf(0) }
    var isLoadingFirstPage by remember { mutableStateOf(true) }
    var isLoadingMore by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var filterQuery by remember { mutableStateOf("") }
    var isSearchVisible by remember { mutableStateOf(false) }
    var isDownloading by remember { mutableStateOf(false) }
    var downloadProgressText by remember { mutableStateOf<String?>(null) }

    suspend fun loadPage(offset: Int) {
        try {
            val response = SpotifyClient.apiService.getSavedTracks(limit = 50, offset = offset)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()!!
                val newTracks = body.items.map { it.track }
                totalCount = body.total
                tracks = (tracks + newTracks).distinctBy { it.id }
            } else {
                if (tracks.isEmpty()) {
                    errorMessage = "Error al cargar canciones guardadas (${response.code()})"
                }
            }
        } catch (e: Exception) {
            if (tracks.isEmpty()) {
                errorMessage = "Error de red: ${e.localizedMessage}"
            }
        }
    }

    // Cargar primera página y continuar cargando progresivamente todas las canciones
    LaunchedEffect(Unit) {
        isLoadingFirstPage = true
        errorMessage = null
        loadPage(0)
        isLoadingFirstPage = false

        // Continuar cargando en segundo plano hasta tener todas las canciones
        scope.launch {
            var currentOffset = 50
            while (tracks.size < totalCount) {
                isLoadingMore = true
                loadPage(currentOffset)
                currentOffset += 50
                isLoadingMore = false
            }
        }
    }

    val filteredTracks = remember(tracks, filterQuery) {
        if (filterQuery.isBlank()) {
            tracks
        } else {
            val q = filterQuery.trim().lowercase()
            tracks.filter {
                it.name.lowercase().contains(q) || it.getArtistNames().lowercase().contains(q)
            }
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .background(LocalSpotifastColors.current.background)
            .padding(horizontal = 16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            // Barra superior: botón Atrás y botón buscar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.Default.ArrowBack,
                        contentDescription = "Atrás",
                        tint = LocalSpotifastColors.current.textPrimary,
                        modifier = Modifier.size(28.dp)
                    )
                }

                IconButton(onClick = { isSearchVisible = !isSearchVisible }) {
                    Icon(
                        imageVector = if (isSearchVisible) Icons.Default.Close else Icons.Default.Search,
                        contentDescription = "Buscar en tus me gusta",
                        tint = LocalSpotifastColors.current.textPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Cabecera grande con gradiente violeta/esmeralda idéntica a Spotifast Desktop / Spotify
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF450AF5),
                                Color(0xFF1B0764)
                            )
                        )
                    )
                    .padding(20.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Cuadro de icono con corazón blanco
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = Color(0xFF5324D6),
                        modifier = Modifier.size(90.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Favorite,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = "Tus me gusta",
                            color = Color.White,
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "${userProfile?.displayName ?: "Tus canciones"} • ${totalCount.coerceAtLeast(tracks.size)} canciones",
                            color = Color(0xFFD0C4F7),
                            fontSize = 13.sp
                        )
                        if (isLoadingMore) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Sincronizando biblioteca completa (${tracks.size}/$totalCount)...",
                                color = LocalSpotifastColors.current.accent,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Campo de búsqueda en vivo dentro de Tus me gusta
            if (isSearchVisible) {
                OutlinedTextField(
                    value = filterQuery,
                    onValueChange = { filterQuery = it },
                    placeholder = { Text("Filtrar en Tus me gusta...", fontSize = 13.sp) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = LocalSpotifastColors.current.accent,
                        focusedTextColor = LocalSpotifastColors.current.textPrimary,
                        unfocusedTextColor = LocalSpotifastColors.current.textPrimary,
                        unfocusedBorderColor = LocalSpotifastColors.current.border
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(14.dp))
            }

            // Barra de controles: botón de reproducción grande y botón de aleatorio
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "${filteredTracks.size} canciones listadas",
                    color = LocalSpotifastColors.current.textSecondary,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isLikedFullyDownloaded = tracks.isNotEmpty() && tracks.all { downloadedIds.contains(it.id) }
                    Surface(
                        onClick = {
                            if (isDownloading) return@Surface
                            if (isLikedFullyDownloaded) {
                                cacheManager.removeDownloadedPlaylist(tracks)
                                android.widget.Toast.makeText(context, "Descargas eliminadas de Spotifast", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                if (tracks.isNotEmpty()) {
                                    isDownloading = true
                                    android.widget.Toast.makeText(context, "Descargando canciones en Spotifast...", android.widget.Toast.LENGTH_SHORT).show()
                                    scope.launch {
                                        cacheManager.downloadPlaylist(tracks) { completed, total ->
                                            downloadProgressText = "$completed/$total"
                                        }
                                        isDownloading = false
                                        downloadProgressText = null
                                        android.widget.Toast.makeText(context, "¡Canciones descargadas para modo offline!", android.widget.Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                        },
                        shape = CircleShape,
                        color = if (isLikedFullyDownloaded) LocalSpotifastColors.current.accent.copy(alpha = 0.2f) else LocalSpotifastColors.current.card,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isDownloading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(22.dp),
                                    color = LocalSpotifastColors.current.accent,
                                    strokeWidth = 2.dp
                                )
                            } else if (isLikedFullyDownloaded) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Descargadas en Spotifast",
                                    tint = LocalSpotifastColors.current.accent,
                                    modifier = Modifier.size(24.dp)
                                )
                            } else {
                                Icon(
                                    imageVector = Icons.Default.ArrowCircleDown,
                                    contentDescription = "Descargar canciones",
                                    tint = LocalSpotifastColors.current.textSecondary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Surface(
                        onClick = {
                            if (tracks.isNotEmpty()) {
                                SpotifastApp.instance.playbackManager.setShuffle(true)
                                onPlayAllClick(tracks.shuffled())
                            }
                        },
                        shape = CircleShape,
                        color = LocalSpotifastColors.current.card,
                        modifier = Modifier.size(46.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Shuffle,
                                contentDescription = "Aleatorio",
                                tint = LocalSpotifastColors.current.accent,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Surface(
                        onClick = {
                            if (tracks.isNotEmpty()) {
                                onPlayAllClick(tracks)
                            }
                        },
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

            Spacer(modifier = Modifier.height(14.dp))
        }

        if (isLoadingFirstPage) {
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
                    modifier = Modifier.fillMaxWidth().padding(16.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp),
                        fontSize = 13.sp
                    )
                }
            }
        } else if (filteredTracks.isEmpty()) {
            item {
                Text(
                    text = if (filterQuery.isNotEmpty()) "No se encontraron canciones que coincidan con \"$filterQuery\"" else "Aún no tienes canciones en Tus me gusta.",
                    color = LocalSpotifastColors.current.textSecondary,
                    modifier = Modifier.padding(24.dp),
                    fontSize = 14.sp
                )
            }
        } else {
            itemsIndexed(filteredTracks) { _, track ->
                TrackListItem(
                    track = track,
                    isDownloaded = downloadedIds.contains(track.id),
                    onClick = { onTrackClick(track, filteredTracks) },
                    onToggleDownload = {
                        scope.launch {
                            if (downloadedIds.contains(track.id)) {
                                cacheManager.removeDownloadedTrack(track.id)
                                android.widget.Toast.makeText(context, "Canción eliminada del almacenamiento", android.widget.Toast.LENGTH_SHORT).show()
                            } else {
                                android.widget.Toast.makeText(context, "Descargando ${track.name}...", android.widget.Toast.LENGTH_SHORT).show()
                                val ok = cacheManager.downloadTrack(track)
                                if (ok) {
                                    android.widget.Toast.makeText(context, "¡${track.name} descargada!", android.widget.Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                    }
                )
            }

            if (isLoadingMore) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            color = LocalSpotifastColors.current.accent,
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Cargando más canciones (${tracks.size}/$totalCount)...",
                            color = LocalSpotifastColors.current.textSecondary,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
