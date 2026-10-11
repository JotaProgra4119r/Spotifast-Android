package rocks.spotifast.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.QueueMusic
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import rocks.spotifast.mobile.SpotifastApp
import rocks.spotifast.mobile.data.api.models.Track
import rocks.spotifast.mobile.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun QueueSheet(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playbackManager = SpotifastApp.instance.playbackManager
    val currentTrack by playbackManager.currentTrack.collectAsState()
    val queue by playbackManager.queue.collectAsState()

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
                    .padding(bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.QueueMusic,
                        contentDescription = null,
                        tint = SpotifyGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Cola de reproducción",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = SpotifyWhite
                    )
                }

                IconButton(onClick = onDismiss) {
                    Icon(Icons.Default.Close, contentDescription = "Cerrar", tint = SpotifyWhite)
                }
            }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(vertical = 4.dp)
            ) {
                // Reproduciendo ahora
                currentTrack?.let { cur ->
                    item {
                        Text(
                            text = "Reproduciendo ahora",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = SpotifyGreen,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        TrackListItem(
                            track = cur,
                            onClick = {}
                        )
                        Spacer(modifier = Modifier.height(18.dp))
                    }
                }

                // Siguientes en la cola
                item {
                    Text(
                        text = "A continuación (${queue.size})",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = SpotifyWhite,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                }

                if (queue.isEmpty()) {
                    item {
                        Text(
                            text = "La cola de reproducción está vacía.",
                            color = SpotifySubtext,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(vertical = 12.dp)
                        )
                    }
                } else {
                    itemsIndexed(queue) { index, track ->
                        TrackListItem(
                            track = track,
                            onClick = {
                                playbackManager.playTrack(track, queue)
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(60.dp))
                }
            }
        }
    }
}
