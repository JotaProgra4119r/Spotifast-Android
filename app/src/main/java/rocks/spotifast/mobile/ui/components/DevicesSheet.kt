package rocks.spotifast.mobile.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import rocks.spotifast.mobile.SpotifastApp
import rocks.spotifast.mobile.data.api.models.Device
import rocks.spotifast.mobile.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesSheet(
    onDismiss: () -> Unit
) {
    val playbackManager = SpotifastApp.instance.playbackManager
    val availableDevices by playbackManager.availableDevices.collectAsState()
    val connectedDevice by playbackManager.connectedDevice.collectAsState()

    LaunchedEffect(Unit) {
        playbackManager.refreshDevices()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = SpotifyDarkGray,
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Devices,
                        contentDescription = null,
                        tint = SpotifyGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Dispositivos Spotify Connect",
                        color = SpotifyWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = { playbackManager.refreshDevices() }) {
                    Icon(Icons.Default.Refresh, contentDescription = "Actualizar", tint = SpotifySubtext)
                }
            }

            Text(
                text = "Transfiere la reproducción a cualquier dispositivo disponible en tu cuenta o red local.",
                color = SpotifySubtext,
                fontSize = 12.sp,
                modifier = Modifier.padding(top = 4.dp, bottom = 16.dp)
            )

            if (availableDevices.isEmpty()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SpotifyCardGray,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "No se encontraron otros dispositivos activos.",
                            color = SpotifySubtext,
                            fontSize = 13.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Abre Spotify en tu PC, altavoz o TV para que aparezcan aquí.",
                            color = SpotifySubtext,
                            fontSize = 11.sp
                        )
                    }
                }
            } else {
                LazyColumn(modifier = Modifier.fillMaxWidth()) {
                    items(availableDevices) { device ->
                        val isCurrent = device.id == connectedDevice?.id || device.isActive

                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    playbackManager.transferToDevice(device)
                                    onDismiss()
                                }
                                .padding(vertical = 12.dp, horizontal = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = getDeviceIcon(device.type),
                                contentDescription = null,
                                tint = if (isCurrent) SpotifyGreen else SpotifyWhite,
                                modifier = Modifier.size(28.dp)
                            )

                            Spacer(modifier = Modifier.width(16.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = device.name,
                                    color = if (isCurrent) SpotifyGreen else SpotifyWhite,
                                    fontSize = 15.sp,
                                    fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium
                                )
                                Text(
                                    text = if (isCurrent) "✓ Escuchando ahora" else "Spotify Connect • ${device.type}",
                                    color = if (isCurrent) SpotifyGreen else SpotifySubtext,
                                    fontSize = 12.sp
                                )
                            }

                            if (device.volumePercent != null) {
                                Text(
                                    text = "${device.volumePercent}%",
                                    color = SpotifySubtext,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }
    }
}

fun getDeviceIcon(type: String): ImageVector {
    return when (type.lowercase()) {
        "computer" -> Icons.Default.Laptop
        "smartphone" -> Icons.Default.PhoneAndroid
        "tablet" -> Icons.Default.Tablet
        "tv" -> Icons.Default.Tv
        "cast_audio", "cast_video" -> Icons.Default.Cast
        "speaker" -> Icons.Default.Speaker
        else -> Icons.Default.Speaker
    }
}
