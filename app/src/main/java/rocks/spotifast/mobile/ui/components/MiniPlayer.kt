package rocks.spotifast.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import androidx.compose.ui.graphics.Color
import rocks.spotifast.mobile.data.api.models.Track
import rocks.spotifast.mobile.ui.theme.*

@Composable
fun MiniPlayer(
    track: Track?,
    isPlaying: Boolean,
    progressFraction: Float,
    connectedDevice: String?,
    onPlayPauseClick: () -> Unit,
    onClick: () -> Unit,
    onDevicesClick: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    if (track == null) return

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() },
        color = Color(0xFF1E2838),
        tonalElevation = 6.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Carátula del álbum
                AsyncImage(
                    model = track.getCoverUrl(300),
                    contentDescription = track.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(44.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(SpotifyCardGray)
                )

                Spacer(modifier = Modifier.width(10.dp))

                // Nombre y artista / dispositivo
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = track.name,
                        color = SpotifyWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (connectedDevice != null) {
                            Icon(
                                imageVector = Icons.Default.Devices,
                                contentDescription = null,
                                tint = SpotifyGreen,
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "$connectedDevice • ${track.getArtistNames()}",
                                color = SpotifyGreen,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        } else {
                            Text(
                                text = track.getArtistNames(),
                                color = SpotifySubtext,
                                fontSize = 11.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Botón de dispositivo / Spotify Connect
                IconButton(onClick = onDevicesClick) {
                    Icon(
                        imageVector = Icons.Default.Devices,
                        contentDescription = "Dispositivos",
                        tint = SpotifyGreen,
                        modifier = Modifier.size(20.dp)
                    )
                }

                // Check verde de guardado en "Tus me gusta"
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Guardado",
                    tint = SpotifyGreen,
                    modifier = Modifier.size(22.dp)
                )

                Spacer(modifier = Modifier.width(4.dp))

                // Play / Pause
                IconButton(onClick = onPlayPauseClick) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                        contentDescription = if (isPlaying) "Pausar" else "Reproducir",
                        tint = SpotifyWhite,
                        modifier = Modifier.size(26.dp)
                    )
                }
            }

            // Barra de progreso inferior fina
            LinearProgressIndicator(
                progress = { progressFraction.coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp),
                color = SpotifyWhite,
                trackColor = SpotifyCardGray
            )
        }
    }
}
