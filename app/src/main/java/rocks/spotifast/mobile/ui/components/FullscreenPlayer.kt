package rocks.spotifast.mobile.ui.components

import android.content.Intent
import android.graphics.drawable.BitmapDrawable
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.Coil
import coil.compose.AsyncImage
import coil.request.ImageRequest
import coil.request.SuccessResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import rocks.spotifast.mobile.SpotifastApp
import rocks.spotifast.mobile.data.api.models.Track
import rocks.spotifast.mobile.data.local.VisualizerType
import rocks.spotifast.mobile.data.lyrics.LyricsManager
import rocks.spotifast.mobile.data.lyrics.TrackLyrics
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

    val context = LocalContext.current
    val playbackManager = SpotifastApp.instance.playbackManager
    val settingsManager = SpotifastApp.instance.settingsManager
    val visualizerType by settingsManager.visualizerType.collectAsState()

    var showEqualizer by remember { mutableStateOf(false) }
    var showLyrics by remember { mutableStateOf(false) }
    var showQueue by remember { mutableStateOf(false) }
    var showWinamp by remember { mutableStateOf(false) }
    var showOptionsMenu by remember { mutableStateOf(false) }

    // Color dinámico de fondo extraído del arte del álbum
    var targetDominantColor by remember(track.id) { mutableStateOf(Color(0xFF2C2228)) }
    val animatedBgColor by animateColorAsState(
        targetValue = targetDominantColor,
        animationSpec = tween(600),
        label = "bg_color"
    )

    // Letras sincronizadas en segundo plano
    var trackLyrics by remember(track.id) { mutableStateOf<TrackLyrics?>(null) }

    LaunchedEffect(track.id) {
        val lyrics = LyricsManager.fetchLyrics(
            artist = track.getArtistNames(),
            title = track.name,
            album = track.album?.name,
            durationMs = track.durationMs
        )
        trackLyrics = lyrics
    }

    // Extracción de paleta de color dominante
    LaunchedEffect(track.getCoverUrl(300)) {
        val coverUrl = track.getCoverUrl(300) ?: return@LaunchedEffect
        withContext(Dispatchers.IO) {
            try {
                val loader = Coil.imageLoader(context)
                val request = ImageRequest.Builder(context)
                    .data(coverUrl)
                    .allowHardware(false)
                    .size(32, 32)
                    .build()
                val result = (loader.execute(request) as? SuccessResult)?.drawable
                if (result is BitmapDrawable) {
                    val bitmap = result.bitmap
                    var rTotal = 0L
                    var gTotal = 0L
                    var bTotal = 0L
                    var count = 0
                    for (x in 0 until bitmap.width step 2) {
                        for (y in 0 until bitmap.height step 2) {
                            val pixel = bitmap.getPixel(x, y)
                            val r = (pixel shr 16) and 0xFF
                            val g = (pixel shr 8) and 0xFF
                            val b = pixel and 0xFF
                            val lum = (r * 299 + g * 587 + b * 114) / 1000
                            if (lum in 25..230) {
                                rTotal += r
                                gTotal += g
                                bTotal += b
                                count++
                            }
                        }
                    }
                    if (count > 0) {
                        val avgR = ((rTotal / count) * 0.82f).toInt().coerceIn(20, 160)
                        val avgG = ((gTotal / count) * 0.82f).toInt().coerceIn(20, 160)
                        val avgB = ((bTotal / count) * 0.82f).toInt().coerceIn(20, 160)
                        targetDominantColor = Color(avgR, avgG, avgB)
                    }
                }
            } catch (_: Exception) {}
        }
    }

    val lyricsLines = trackLyrics?.lines ?: emptyList()
    val activeIndex = remember(progressMs, lyricsLines) {
        if (lyricsLines.isEmpty() || trackLyrics?.isSynced != true) -1
        else {
            val idx = lyricsLines.indexOfLast { it.timeMs <= progressMs }
            if (idx >= 0) idx else 0
        }
    }

    val activeLyricLine = remember(trackLyrics, progressMs) {
        lyricsLines.lastOrNull { it.timeMs <= progressMs }?.text ?: ""
    }

    val lyricsListState = rememberLazyListState()

    // Auto-scroll suave de las letras conforme corre la música
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0 && lyricsLines.size > activeIndex) {
            lyricsListState.animateScrollToItem((activeIndex - 1).coerceAtLeast(0))
        }
    }

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
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        animatedBgColor,
                        animatedBgColor.copy(alpha = 0.7f),
                        SpotifyBlack,
                        SpotifyBlack
                    )
                )
            )
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Top
        ) {
            // Cabecera superior
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 8.dp),
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

                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                ) {
                    Text(
                        text = "Reproduciendo desde Playlist",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = SpotifySubtext,
                        maxLines = 1
                    )
                    Text(
                        text = track.album?.name ?: "Tus me gusta",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = SpotifyWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Box {
                    IconButton(onClick = { showOptionsMenu = true }) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Opciones",
                            tint = SpotifyWhite,
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    DropdownMenu(
                        expanded = showOptionsMenu,
                        onDismissRequest = { showOptionsMenu = false },
                        modifier = Modifier.background(SpotifyDarkGray)
                    ) {
                        DropdownMenuItem(
                            text = { Text("Ecualizador (10 bandas)", color = SpotifyWhite) },
                            leadingIcon = { Icon(Icons.Default.Tune, contentDescription = null, tint = SpotifyGreen) },
                            onClick = {
                                showOptionsMenu = false
                                showEqualizer = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Alternar Visualizador", color = SpotifyWhite) },
                            leadingIcon = { Icon(Icons.Default.GraphicEq, contentDescription = null, tint = SpotifyGreen) },
                            onClick = {
                                showOptionsMenu = false
                                val next = when (visualizerType) {
                                    VisualizerType.OFF -> VisualizerType.SPECTRUM_BARS
                                    VisualizerType.SPECTRUM_BARS -> VisualizerType.OSCILLOSCOPE
                                    VisualizerType.OSCILLOSCOPE -> VisualizerType.MILKDROP_WAVES
                                    VisualizerType.MILKDROP_WAVES -> VisualizerType.OFF
                                }
                                settingsManager.setVisualizerType(next)
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Reproductor Winamp Retro", color = SpotifyWhite) },
                            leadingIcon = { Icon(Icons.Default.Radio, contentDescription = null, tint = SpotifyGreen) },
                            onClick = {
                                showOptionsMenu = false
                                showWinamp = true
                            }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Carátula cuadrada centrada
            AsyncImage(
                model = track.getCoverUrl(640),
                contentDescription = track.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(SpotifyCardGray)
            )

            // Visualizador reactivo post-equalizer pre-volume
            if (visualizerType != VisualizerType.OFF) {
                Spacer(modifier = Modifier.height(6.dp))
                VisualizerView(
                    type = visualizerType,
                    isPlaying = isPlaying,
                    modifier = Modifier.padding(vertical = 2.dp)
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Teaser de Letra Sincronizada (Screenshot 2 exacto)
            if (activeLyricLine.isNotEmpty()) {
                Text(
                    text = activeLyricLine,
                    color = SpotifyWhite,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showLyrics = true }
                        .padding(vertical = 4.dp)
                )
            } else {
                Spacer(modifier = Modifier.height(4.dp))
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Título, Artista e icono de guardado
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = track.name,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = SpotifyWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(2.dp))
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

            Spacer(modifier = Modifier.height(12.dp))

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
                        inactiveTrackColor = SpotifyWhite.copy(alpha = 0.25f)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )

                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(text = formatTime(sliderPosition.toLong()), fontSize = 12.sp, color = SpotifySubtext)
                    Text(text = formatTime(total), fontSize = 12.sp, color = SpotifySubtext)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Controles principales de reproducción
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(onClick = onToggleShuffle) {
                    Icon(
                        Icons.Default.Shuffle,
                        contentDescription = "Aleatorio",
                        tint = if (isShuffle) SpotifyGreen else SpotifySubtext,
                        modifier = Modifier.size(26.dp)
                    )
                }
                IconButton(onClick = onPrevious) {
                    Icon(
                        Icons.Default.SkipPrevious,
                        contentDescription = "Anterior",
                        tint = SpotifyWhite,
                        modifier = Modifier.size(38.dp)
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
                        modifier = Modifier.size(38.dp)
                    )
                }
                IconButton(onClick = onCycleRepeat) {
                    Icon(
                        Icons.Default.Repeat,
                        contentDescription = "Repetir",
                        tint = if (repeatMode != "off") SpotifyGreen else SpotifySubtext,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Barra inferior con dispositivo Connect, Compartir y Cola (Exacto Screenshot 2)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Selector de dispositivos Connect
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onDevicesClick() }
                        .padding(vertical = 4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Devices,
                        contentDescription = "Dispositivos",
                        tint = if (connectedDevice != null && !connectedDevice.contains("Este teléfono")) SpotifyGreen else SpotifySubtext,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = connectedDevice ?: "Este teléfono",
                        color = if (connectedDevice != null && !connectedDevice.contains("Este teléfono")) SpotifyGreen else SpotifySubtext,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(onClick = {
                        val sendIntent = Intent().apply {
                            action = Intent.ACTION_SEND
                            putExtra(Intent.EXTRA_TEXT, "${track.name} - ${track.getArtistNames()} en Spotifast: ${track.uri}")
                            type = "text/plain"
                        }
                        val shareIntent = Intent.createChooser(sendIntent, "Compartir canción")
                        context.startActivity(shareIntent)
                    }) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = "Compartir",
                            tint = SpotifyWhite,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    IconButton(onClick = { showQueue = true }) {
                        Icon(
                            imageVector = Icons.Default.QueueMusic,
                            contentDescription = "Cola",
                            tint = SpotifyWhite,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Tarjeta interactiva de "Letras" sincronizadas con scroll (Exacto a Spotify)
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = animatedBgColor.copy(alpha = 0.65f),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .padding(bottom = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Letras",
                            color = SpotifyWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        IconButton(
                            onClick = { showLyrics = true },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInFull,
                                contentDescription = "Expandir letras completas",
                                tint = SpotifyWhite,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }

                    if (trackLyrics == null) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = SpotifyGreen, modifier = Modifier.size(28.dp))
                        }
                    } else if (lyricsLines.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Text(
                                text = if (trackLyrics?.isInstrumental == true)
                                    "Esta canción es instrumental (\u266a)"
                                else
                                    "No hay letras disponibles para esta canción",
                                color = SpotifySubtext,
                                fontSize = 14.sp
                            )
                        }
                    } else {
                        LazyColumn(
                            state = lyricsListState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            itemsIndexed(lyricsLines) { index, line ->
                                val isActive = index == activeIndex
                                Text(
                                    text = line.text,
                                    color = if (isActive) SpotifyWhite else SpotifyWhite.copy(alpha = 0.45f),
                                    fontSize = if (isActive) 20.sp else 16.sp,
                                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                                    lineHeight = if (isActive) 28.sp else 22.sp,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            if (trackLyrics?.isSynced == true) {
                                                onSeek(line.timeMs)
                                            }
                                        }
                                        .padding(vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

private fun formatTime(ms: Long): String {
    val totalSec = ms / 1000
    val min = totalSec / 60
    val sec = totalSec % 60
    return String.format("%d:%02d", min, sec)
}
