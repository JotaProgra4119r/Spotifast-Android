package rocks.spotifast.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import rocks.spotifast.mobile.SpotifastApp
import rocks.spotifast.mobile.data.api.models.Track
import rocks.spotifast.mobile.data.local.VisualizerType

@Composable
fun WinampPlayerDialog(
    track: Track?,
    isPlaying: Boolean,
    progressMs: Long,
    durationMs: Long,
    onDismiss: () -> Unit,
    onPlayPause: () -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit,
    onSeek: (Long) -> Unit
) {
    if (track == null) return

    val playbackManager = SpotifastApp.instance.playbackManager
    val settingsManager = SpotifastApp.instance.settingsManager
    var isShaded by remember { mutableStateOf(false) }
    var showEqualizer by remember { mutableStateOf(false) }

    if (showEqualizer) {
        EqualizerSheet(onDismiss = { showEqualizer = false })
    }

    // Colores clásicos de Winamp 2.91
    val chassisColor = Color(0xFF26262A)
    val bezelBorder = Color(0xFF4C4C54)
    val screenBg = Color(0xFF000000)
    val ledGreen = Color(0xFF00FF00)
    val winampBlue = Color(0xFF214478)

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(4.dp),
            color = chassisColor,
            modifier = Modifier
                .width(340.dp)
                .border(2.dp, bezelBorder, RoundedCornerShape(4.dp))
        ) {
            Column(modifier = Modifier.padding(6.dp)) {
                // Barra de título Winamp
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(winampBlue)
                        .clickable { isShaded = !isShaded }
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "WINAMP - SPOTIFAST v0.36",
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace
                    )

                    Row {
                        Text(
                            text = if (isShaded) "\u25BC" else "\u25B2",
                            color = Color.LightGray,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .clickable { isShaded = !isShaded }
                                .padding(horizontal = 4.dp)
                        )
                        Text(
                            text = "\u2715",
                            color = Color.LightGray,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .clickable { onDismiss() }
                                .padding(horizontal = 4.dp)
                        )
                    }
                }

                if (!isShaded) {
                    Spacer(modifier = Modifier.height(6.dp))

                    // Pantalla LED verde principal (Pantalla de información y visualizador)
                    Surface(
                        color = screenBg,
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.dp, Color(0xFF181818))
                            .padding(4.dp)
                    ) {
                        Column {
                            // Tiempo transcurrido LED
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                val min = (progressMs / 1000) / 60
                                val sec = (progressMs / 1000) % 60
                                Text(
                                    text = String.format("%02d:%02d", min, sec),
                                    color = ledGreen,
                                    fontSize = 20.sp,
                                    fontWeight = FontWeight.Bold,
                                    fontFamily = FontFamily.Monospace
                                )

                                Text(
                                    text = "320 kbps  44 kHz",
                                    color = Color(0xFF00B000),
                                    fontSize = 10.sp,
                                    fontFamily = FontFamily.Monospace
                                )
                            }

                            // Ticker de título de canción
                            Text(
                                text = "1. ${track.getArtistNames()} - ${track.name}",
                                color = ledGreen,
                                fontSize = 11.sp,
                                fontFamily = FontFamily.Monospace,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(vertical = 2.dp)
                            )

                            // Espectro de barras reactivo integrado
                            VisualizerView(
                                type = VisualizerType.SPECTRUM_BARS,
                                isPlaying = isPlaying,
                                modifier = Modifier.height(36.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(6.dp))

                    // Slider de posición
                    val total = if (durationMs > 0) durationMs else track.durationMs
                    var sliderPos by remember(progressMs) { mutableFloatStateOf(progressMs.toFloat()) }
                    Slider(
                        value = sliderPos,
                        onValueChange = { sliderPos = it },
                        onValueChangeFinished = { onSeek(sliderPos.toLong()) },
                        valueRange = 0f..total.toFloat().coerceAtLeast(1f),
                        colors = SliderDefaults.colors(thumbColor = Color.LightGray, activeTrackColor = Color.Gray),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(18.dp)
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    // Botones de transporte retro
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WinampButton("PREV") { onPrevious() }
                        WinampButton(if (isPlaying) "PAUSE" else "PLAY") { onPlayPause() }
                        WinampButton("STOP") { playbackManager.pause() }
                        WinampButton("NEXT") { onNext() }
                        WinampButton("EQ") { showEqualizer = true }
                    }
                }
            }
        }
    }
}

@Composable
fun WinampButton(
    label: String,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        color = Color(0xFF383840),
        shape = RoundedCornerShape(2.dp),
        modifier = Modifier
            .border(1.dp, Color(0xFF5A5A66), RoundedCornerShape(2.dp))
            .height(28.dp)
            .padding(horizontal = 2.dp)
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 6.dp)) {
            Text(
                text = label,
                color = Color.LightGray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                fontFamily = FontFamily.Monospace
            )
        }
    }
}
