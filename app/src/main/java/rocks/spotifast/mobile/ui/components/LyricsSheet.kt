package rocks.spotifast.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import rocks.spotifast.mobile.SpotifastApp
import rocks.spotifast.mobile.data.api.models.Track
import rocks.spotifast.mobile.data.lyrics.LyricsManager
import rocks.spotifast.mobile.data.lyrics.TrackLyrics
import rocks.spotifast.mobile.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LyricsSheet(
    track: Track,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playbackManager = SpotifastApp.instance.playbackManager
    val progressMs by playbackManager.progressMs.collectAsState()

    var lyricsData by remember { mutableStateOf<TrackLyrics?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    val listState = rememberLazyListState()

    LaunchedEffect(track.id) {
        isLoading = true
        lyricsData = LyricsManager.fetchLyrics(
            artist = track.getArtistNames(),
            title = track.name,
            album = track.album?.name,
            durationMs = track.durationMs
        )
        isLoading = false
    }

    // Calcular línea activa actual
    val activeIndex = remember(progressMs, lyricsData) {
        val lines = lyricsData?.lines ?: emptyList()
        if (lines.isEmpty() || !lyricsData!!.isSynced) -1
        else {
            val idx = lines.indexOfLast { it.timeMs <= progressMs }
            if (idx >= 0) idx else 0
        }
    }

    // Auto-scroll suave a la línea activa
    LaunchedEffect(activeIndex) {
        if (activeIndex >= 0 && (lyricsData?.lines?.size ?: 0) > activeIndex) {
            listState.animateScrollToItem((activeIndex - 2).coerceAtLeast(0))
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SpotifyDarkGray,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp)
        ) {
            // Cabecera
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = null,
                        tint = SpotifyGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Letras sincronizadas",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.Bold,
                            color = SpotifyWhite
                        )
                        Text(
                            text = "${track.name} • ${track.getArtistNames()}",
                            fontSize = 12.sp,
                            color = SpotifySubtext,
                            maxLines = 1
                        )
                    }
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = SpotifyWhite)
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = SpotifyGreen)
                }
            } else if (lyricsData == null || lyricsData!!.lines.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (lyricsData?.isInstrumental == true)
                            "Esta pista es instrumental (\u266a)"
                        else
                            "No se encontraron letras disponibles para esta canción.",
                        color = SpotifySubtext,
                        fontSize = 15.sp
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(vertical = 12.dp)
                ) {
                    itemsIndexed(lyricsData!!.lines) { index, line ->
                        val isActive = index == activeIndex
                        Text(
                            text = line.text,
                            color = if (isActive) SpotifyWhite else Color(0xFF6B6B6B),
                            fontSize = if (isActive) 22.sp else 17.sp,
                            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium,
                            lineHeight = if (isActive) 30.sp else 24.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    if (lyricsData!!.isSynced) {
                                        playbackManager.seekTo(line.timeMs)
                                    }
                                }
                                .padding(vertical = 8.dp)
                        )
                    }

                    item {
                        Spacer(modifier = Modifier.height(60.dp))
                    }
                }
            }
        }
    }
}
