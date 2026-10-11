package rocks.spotifast.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import rocks.spotifast.mobile.NavigationScreen
import rocks.spotifast.mobile.SpotifastApp
import rocks.spotifast.mobile.data.api.models.Playlist
import rocks.spotifast.mobile.data.api.models.UserProfile
import rocks.spotifast.mobile.data.local.VisualizerType
import rocks.spotifast.mobile.ui.screens.*
import rocks.spotifast.mobile.ui.theme.*

@Composable
fun DesktopTabletShell(
    userProfile: UserProfile?,
    currentScreen: NavigationScreen,
    selectedPlaylist: Playlist?,
    onNavigate: (NavigationScreen) -> Unit,
    onPlaylistSelected: (Playlist) -> Unit,
    onLogoutRequested: () -> Unit
) {
    val playbackManager = SpotifastApp.instance.playbackManager
    val settingsManager = SpotifastApp.instance.settingsManager

    val currentTrack by playbackManager.currentTrack.collectAsState()
    val isPlaying by playbackManager.isPlaying.collectAsState()
    val progressMs by playbackManager.progressMs.collectAsState()
    val durationMs by playbackManager.durationMs.collectAsState()
    val connectedDevice by playbackManager.connectedDeviceName.collectAsState()
    val isShuffle by playbackManager.isShuffle.collectAsState()
    val repeatMode by playbackManager.repeatMode.collectAsState()
    val visualizerType by settingsManager.visualizerType.collectAsState()

    var showDevicesSheet by remember { mutableStateOf(false) }
    var showEqualizerSheet by remember { mutableStateOf(false) }
    var showLyricsSheet by remember { mutableStateOf(false) }
    var showQueueSheet by remember { mutableStateOf(false) }
    var showWinampDialog by remember { mutableStateOf(false) }
    var volume by remember { mutableFloatStateOf(0.85f) }

    if (showDevicesSheet) {
        DevicesSheet(onDismiss = { showDevicesSheet = false })
    }

    if (showEqualizerSheet) {
        EqualizerSheet(onDismiss = { showEqualizerSheet = false })
    }

    if (showLyricsSheet && currentTrack != null) {
        LyricsSheet(track = currentTrack!!, onDismiss = { showLyricsSheet = false })
    }

    if (showQueueSheet) {
        QueueSheet(onDismiss = { showQueueSheet = false })
    }

    if (showWinampDialog) {
        WinampPlayerDialog(
            track = currentTrack,
            isPlaying = isPlaying,
            progressMs = progressMs,
            durationMs = durationMs,
            onDismiss = { showWinampDialog = false },
            onPlayPause = { playbackManager.togglePlayPause() },
            onNext = { playbackManager.next() },
            onPrevious = { playbackManager.previous() },
            onSeek = { playbackManager.seekTo(it) }
        )
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(SpotifyBlack)
    ) {
        // Área principal (Sidebar a la izquierda + Contenido central)
        Row(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // 1. BARRA LATERAL ESCRITORIO (Sidebar)
            Surface(
                color = SpotifyDarkGray,
                modifier = Modifier
                    .width(230.dp)
                    .fillMaxHeight()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(14.dp)
                ) {
                    // Logo de Spotifast
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(bottom = 20.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = SpotifyGreen,
                            modifier = Modifier.size(34.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.GraphicEq, contentDescription = null, tint = SpotifyBlack, modifier = Modifier.size(20.dp))
                            }
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text("Spotifast", color = SpotifyWhite, fontWeight = FontWeight.Bold, fontSize = 17.sp)
                            Text("Versión Escritorio / Tablet", color = SpotifySubtext, fontSize = 10.sp)
                        }
                    }

                    // Enlaces de navegación principales
                    SidebarNavButton(
                        icon = Icons.Default.Home,
                        label = "Inicio",
                        isSelected = currentScreen == NavigationScreen.HOME,
                        onClick = { onNavigate(NavigationScreen.HOME) }
                    )
                    SidebarNavButton(
                        icon = Icons.Default.Search,
                        label = "Buscar",
                        isSelected = currentScreen == NavigationScreen.SEARCH,
                        onClick = { onNavigate(NavigationScreen.SEARCH) }
                    )
                    SidebarNavButton(
                        icon = Icons.Default.LibraryMusic,
                        label = "Tu biblioteca",
                        isSelected = currentScreen == NavigationScreen.LIBRARY,
                        onClick = { onNavigate(NavigationScreen.LIBRARY) }
                    )

                    Spacer(modifier = Modifier.height(14.dp))
                    HorizontalDivider(color = SpotifyCardGray, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(14.dp))

                    Text("UTILIDADES PC", color = SpotifySubtext, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))

                    SidebarNavButton(
                        icon = Icons.Default.Mic,
                        label = "Letras sincronizadas",
                        isSelected = false,
                        onClick = { showLyricsSheet = true }
                    )
                    SidebarNavButton(
                        icon = Icons.Default.QueueMusic,
                        label = "Cola de reproducción",
                        isSelected = false,
                        onClick = { showQueueSheet = true }
                    )
                    SidebarNavButton(
                        icon = Icons.Default.Tune,
                        label = "Ecualizador de audio",
                        isSelected = false,
                        onClick = { showEqualizerSheet = true }
                    )
                    SidebarNavButton(
                        icon = Icons.Default.Radio,
                        label = "Reproductor Winamp",
                        isSelected = false,
                        onClick = { showWinampDialog = true }
                    )
                    SidebarNavButton(
                        icon = Icons.Default.Settings,
                        label = "Configuración",
                        isSelected = currentScreen == NavigationScreen.SETTINGS,
                        onClick = { onNavigate(NavigationScreen.SETTINGS) }
                    )

                    Spacer(modifier = Modifier.weight(1f))

                    // Perfil inferior
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate(NavigationScreen.SETTINGS) }
                            .padding(vertical = 4.dp)
                    ) {
                        Surface(shape = CircleShape, color = SpotifyCardGray, modifier = Modifier.size(32.dp)) {
                            Box(contentAlignment = Alignment.Center) {
                                Text("U", color = SpotifyWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(userProfile?.displayName ?: "Usuario Spotifast", color = SpotifyWhite, fontSize = 12.sp, fontWeight = FontWeight.SemiBold, maxLines = 1)
                            Text("Configuración", color = SpotifySubtext, fontSize = 10.sp)
                        }
                    }
                }
            }

            // 2. CONTENIDO CENTRAL
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .background(SpotifyBlack)
            ) {
                when (currentScreen) {
                    NavigationScreen.HOME -> {
                        HomeScreen(
                            userProfile = userProfile,
                            onProfileClick = { onNavigate(NavigationScreen.SETTINGS) },
                            onLikedSongsClick = { onNavigate(NavigationScreen.LIBRARY) },
                            onPlaylistClick = { onPlaylistSelected(it) },
                            onTrackClick = { track -> playbackManager.playTrack(track) }
                        )
                    }
                    NavigationScreen.SEARCH -> {
                        SearchScreen(
                            userProfile = userProfile,
                            onProfileClick = { onNavigate(NavigationScreen.SETTINGS) },
                            onTrackClick = { track, list -> playbackManager.playTrack(track, list) }
                        )
                    }
                    NavigationScreen.LIBRARY -> {
                        LibraryScreen(
                            onBackClick = { onNavigate(NavigationScreen.HOME) },
                            onTrackClick = { track, list -> playbackManager.playTrack(track, list) }
                        )
                    }
                    NavigationScreen.PLAYLIST_DETAIL -> {
                        selectedPlaylist?.let { pl ->
                            PlaylistDetailScreen(
                                playlist = pl,
                                onBackClick = { onNavigate(NavigationScreen.HOME) },
                                onTrackClick = { track, list -> playbackManager.playTrack(track, list) },
                                onPlayAllClick = { plItem -> playbackManager.playPlaylistContext(plItem.uri) }
                            )
                        } ?: run {
                            onNavigate(NavigationScreen.HOME)
                        }
                    }
                    NavigationScreen.SETTINGS -> {
                        SettingsScreen(
                            userProfile = userProfile,
                            onBackClick = { onNavigate(NavigationScreen.HOME) },
                            onLogoutClick = { onLogoutRequested() }
                        )
                    }
                }
            }
        }

        // 3. BARRA DE REPRODUCCIÓN INFERIOR DE ESCRITORIO (Player Bar)
        Surface(
            color = Color(0xFF181818),
            modifier = Modifier
                .fillMaxWidth()
                .height(86.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Izquierda: Carátula e información de la canción
                Row(
                    modifier = Modifier.width(230.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentTrack != null) {
                        AsyncImage(
                            model = currentTrack!!.getCoverUrl(300),
                            contentDescription = currentTrack!!.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(SpotifyCardGray)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = currentTrack!!.name,
                                color = SpotifyWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = currentTrack!!.getArtistNames(),
                                color = SpotifySubtext,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        Text("No hay reproducción activa", color = SpotifySubtext, fontSize = 12.sp)
                    }
                }

                // Centro: Controles de reproducción y barra de progreso
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Botones de transporte
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        IconButton(onClick = { playbackManager.toggleShuffle() }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Shuffle, contentDescription = "Aleatorio", tint = if (isShuffle) SpotifyGreen else SpotifySubtext, modifier = Modifier.size(18.dp))
                        }
                        IconButton(onClick = { playbackManager.previous() }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.SkipPrevious, contentDescription = "Anterior", tint = SpotifyWhite, modifier = Modifier.size(24.dp))
                        }
                        Surface(
                            onClick = { playbackManager.togglePlayPause() },
                            shape = CircleShape,
                            color = SpotifyWhite,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                                    contentDescription = if (isPlaying) "Pausa" else "Reproducir",
                                    tint = SpotifyBlack,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                        IconButton(onClick = { playbackManager.next() }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.SkipNext, contentDescription = "Siguiente", tint = SpotifyWhite, modifier = Modifier.size(24.dp))
                        }
                        IconButton(onClick = { playbackManager.cycleRepeat() }, modifier = Modifier.size(28.dp)) {
                            Icon(Icons.Default.Repeat, contentDescription = "Repetir", tint = if (repeatMode != "off") SpotifyGreen else SpotifySubtext, modifier = Modifier.size(18.dp))
                        }
                    }

                    // Barra de progreso y tiempos
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(0.9f)
                    ) {
                        val total = if (durationMs > 0) durationMs else (currentTrack?.durationMs ?: 1L)
                        val pos = progressMs.coerceIn(0L, total)
                        Text(formatDesktopTime(pos), color = SpotifySubtext, fontSize = 10.sp)
                        Slider(
                            value = pos.toFloat(),
                            onValueChange = { playbackManager.seekTo(it.toLong()) },
                            valueRange = 0f..total.toFloat(),
                            colors = SliderDefaults.colors(thumbColor = SpotifyWhite, activeTrackColor = SpotifyWhite, inactiveTrackColor = SpotifyCardGray),
                            modifier = Modifier
                                .weight(1f)
                                .height(16.dp)
                                .padding(horizontal = 8.dp)
                        )
                        Text(formatDesktopTime(total), color = SpotifySubtext, fontSize = 10.sp)
                    }
                }

                // Derecha: Letras, Cola, Dispositivos, Visualizador, Volumen
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.width(260.dp)
                ) {
                    IconButton(onClick = { showLyricsSheet = true }, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Mic, contentDescription = "Letras", tint = SpotifySubtext, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { showQueueSheet = true }, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.QueueMusic, contentDescription = "Cola", tint = SpotifySubtext, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { showDevicesSheet = true }, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Devices, contentDescription = "Dispositivos", tint = SpotifyGreen, modifier = Modifier.size(18.dp))
                    }
                    IconButton(onClick = { showWinampDialog = true }, modifier = Modifier.size(30.dp)) {
                        Icon(Icons.Default.Radio, contentDescription = "Winamp", tint = SpotifyWhite, modifier = Modifier.size(18.dp))
                    }

                    // Control de volumen
                    Icon(
                        imageVector = if (volume > 0.5f) Icons.Default.VolumeUp else if (volume > 0f) Icons.Default.VolumeDown else Icons.Default.VolumeMute,
                        contentDescription = "Volumen",
                        tint = SpotifySubtext,
                        modifier = Modifier.size(18.dp)
                    )
                    Slider(
                        value = volume,
                        onValueChange = { volume = it },
                        colors = SliderDefaults.colors(thumbColor = SpotifyWhite, activeTrackColor = SpotifyGreen),
                        modifier = Modifier
                            .width(80.dp)
                            .height(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun SidebarNavButton(
    icon: ImageVector,
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = if (isSelected) SpotifyCardGray else Color.Transparent,
        shape = RoundedCornerShape(6.dp),
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 3.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (isSelected) SpotifyGreen else SpotifySubtext,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = label,
                color = if (isSelected) SpotifyWhite else SpotifySubtext,
                fontSize = 13.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
            )
        }
    }
}

private fun formatDesktopTime(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format("%d:%02d", min, sec)
}
