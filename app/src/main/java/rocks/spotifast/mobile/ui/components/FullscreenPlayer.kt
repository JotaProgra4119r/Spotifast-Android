package rocks.spotifast.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import rocks.spotifast.mobile.SpotifastApp
import rocks.spotifast.mobile.data.api.models.Track
import rocks.spotifast.mobile.data.local.VisualizerType
import rocks.spotifast.mobile.ui.theme.*

@Composable
fun FullscreenPlayer(
    track: Track?,
    isPlaying: Boolean,
    progressMs: Long,
    durationMs: Long,
    connectedDevice: String?,
    isShuffle: Boolean = false,
    repeatMode: String = "off",
    onDismiss: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit,
    onToggleShuffle: () -> Unit = {},
    onCycleRepeat: () -> Unit = {},
    onDevicesClick: () -> Unit = {}
) {
    if (track == null) return

    val playbackManager = SpotifastApp.instance.playbackManager
    val settingsManager = SpotifastApp.instance.settingsManager
    val visualizerType by settingsManager.visualizerType.collectAsState()

    var showEqualizer by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showWinamp by remember { mutableStateOf(false) }

    if (showEqualizer) {
        EqualizerSheet(onDismiss = { showEqualizer = false })
    }

    if (showLyrics) {
        LyricsSheet(track = track, onDismiss = { showLyrics = false })
    }

    if (showQueue) {
        QueueSheet(onDismiss = { showQueue = false })
    }

    if (showWinamp) {
        WinampPlayerDialog(
            track = track,
            isPlaying = isPlaying,
            progressMs = progressMs,
            durationMs = durationMs,
            onDismiss = { showWinamp = false },
            onPlayPause = onPlayPause,
            onNext = onNext,
            onPrevious = onPrevious,
            onSeek = onSeek
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(SpotifyBlack)
            .padding(horizontal = 24.dp, vertical = 12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Cabecera superior
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onDismiss) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Cerrar",
                        tint = SpotifyWhite,
                        modifier = Modifier.size(32.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "REPRODUCIENDO AHORA",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = SpotifySubtext
                    )
                    Text(
                        text = track.album?.name ?: "Spotifast Mobile",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SpotifyWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row {
                    // Botón para alternar Visualizador (Bars -> Scope -> MilkDrop -> Off)
                    IconButton(onClick = {
                        val next = when (visualizerType) {
                            VisualizerType.OFF -> VisualizerType.SPECTRUM_BARS
                            VisualizerType.SPECTRUM_BARS -> VisualizerType.OSCILLOSCOPE
                            VisualizerType.OSCILLOSCOPE -> VisualizerType.MILKDROP_WAVES
                            VisualizerType.MILKDROP_WAVES -> VisualizerType.OFF
                        }
                        settingsManager.setVisualizerType(next)
                    }) {
                        Icon(
                            imageVector = Icons.Default.GraphicEq,
                            contentDescription = "Visualizador",
                            tint = if (visualizerType != VisualizerType.OFF) SpotifyGreen else SpotifySubtext
                        )
                    }

                    // Botón de Ecualizador de 10 bandas
                    IconButton(onClick = { showEqualizer = true }) {
                        Icon(
                            imageVector = Icons.Default.Tune,
                            contentDescription = "Ecualizador",
                            tint = SpotifyWhite
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Carátula grande
            AsyncImage(
                model = track.getCoverUrl(640),
                contentDescription = track.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(SpotifyCardGray)
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Visualizador reactivo post-equalizer pre-volume (Spotifast standard)
            if (visualizerType != VisualizerType.OFF) {
                VisualizerView(
                    type = visualizerType,
                    isPlaying = isPlaying,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }

            // Título y artista
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.name,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = SpotifyWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = track.getArtistNames(),
                        fontSize = 15.sp,
                        color = SpotifySubtext,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Guardado",
                    tint = SpotifyGreen,
                    modifier = Modifier.size(28.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Barra de progreso y tiempos
            Column(modifier = Modifier.fillMaxWidth()) {
                val total = if (durationMs > 0) durationMs else track.durationMs
                var sliderPosition by remember(progressMs) { mutableFloatStateOf(progressMs.toFloat()) }

                Slider(
                    value = sliderPosition,
                    onValueChange = { sliderPosition = it },
                    onValueChangeFinished = { onSeek(sliderPosition.toLong()) },
                    valueRange = 0f..total.toFloat().coerceAtLeast(1f),
                    colors = SliderDefaults.colors(
                        thumbColor = SpotifyWhite,
                        activeTrackColor = SpotifyWhite,
                        inactiveTrackColor = SpotifyCardGray
                    )
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = formatTime(sliderPosition.toLong()), fontSize = 12.sp, color = SpotifySubtext)
                    Text(text = formatTime(total), fontSize = 12.sp, color = SpotifySubtext)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Controles de reproducción
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = "Aleatorio",
                        tint = if (isShuffle) SpotifyGreen else SpotifySubtext
                    )
                }
                IconButton(onClick = onPrevious) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Anterior",
                        tint = SpotifyWhite,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Surface(
                    onClick = onPlayPause,
                    shape = CircleShape,
                    color = SpotifyWhite,
                    modifier = Modifier.size(64.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pausa" else "Reproducir",
                            tint = SpotifyBlack,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
                IconButton(onClick = onNext) {
                    Icon(
                        Icons.Default.SkipNext,
                        contentDescription = "Siguiente",
                        tint = SpotifyWhite,
                        modifier = Modifier.size(36.dp)
                    )
                }
                IconButton(onClick = onCycleRepeat) {
                    Icon(
                        Icons.Default.Repeat,
                        contentDescription = "Repetir",
                        tint = if (repeatMode != "off") SpotifyGreen else SpotifySubtext
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Fila de utilidades rápidas: Letras, Cola de reproducción, Modo Winamp
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { showLyrics = true }) {
                    Icon(Icons.Default.Mic, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Letras", color = SpotifyWhite, fontSize = 12.sp)
                }

                TextButton(onClick = { showQueue = true }) {
                    Icon(Icons.Default.QueueMusic, contentDescription = null, tint = SpotifyWhite, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Cola", color = SpotifyWhite, fontSize = 12.sp)
                }

                TextButton(onClick = { showWinamp = true }) {
                    Icon(Icons.Default.Radio, contentDescription = null, tint = SpotifyWhite, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Winamp", color = SpotifyWhite, fontSize = 12.sp)
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Barra de dispositivo Spotify Connect
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDevicesClick() }
                    .padding(bottom = 12.dp, top = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Devices,
                        contentDescription = null,
                        tint = SpotifyGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = connectedDevice ?: "Buscar dispositivos Connect...",
                        color = SpotifyGreen,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = "Cambiar",
                    color = SpotifySubtext,
                    fontSize = 12.sp
                )
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format("%d:%02d", min, sec)
}
