package rocks.spotifast.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GraphicEq
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import rocks.spotifast.mobile.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EqualizerSheet(
    onDismiss: () -> Unit
) {
    var isEnabled by remember { mutableStateOf(true) }
    var selectedPreset by remember { mutableStateOf("Rock") }

    // Bandas de frecuencia idénticas a Spotifast desktop src/eq.rs
    val bands = listOf("32 Hz", "64 Hz", "125 Hz", "250 Hz", "500 Hz", "1 kHz", "2 kHz", "4 kHz", "8 kHz", "16 kHz")
    val bandValues = remember {
        mutableStateMapOf(
            0 to 4.5f,
            1 to 3.0f,
            2 to -1.0f,
            3 to -2.0f,
            4 to 1.5f,
            5 to 3.0f,
            6 to 5.0f,
            7 to 4.0f,
            8 to 3.5f,
            9 to 2.0f
        )
    }

    var preAmp by remember { mutableFloatStateOf(0.0f) }
    var bassBoost by remember { mutableFloatStateOf(65.0f) }
    var surroundEffect by remember { mutableFloatStateOf(30.0f) }

    val presets = listOf("Plano", "Rock", "Pop", "Jazz", "Bass Boost", "Clásica", "Electrónica", "Acústica", "Vocal")

    fun applyPreset(preset: String) {
        selectedPreset = preset
        when (preset) {
            "Plano" -> { (0..9).forEach { bandValues[it] = 0f }; preAmp = 0f }
            "Rock" -> {
                bandValues[0] = 5f; bandValues[1] = 3.5f; bandValues[2] = -1f; bandValues[3] = -2f
                bandValues[4] = 0f; bandValues[5] = 2.5f; bandValues[6] = 4f; bandValues[7] = 5f
                bandValues[8] = 4.5f; bandValues[9] = 3.5f
            }
            "Pop" -> {
                bandValues[0] = -1f; bandValues[1] = 1.5f; bandValues[2] = 4f; bandValues[3] = 4.5f
                bandValues[4] = 3f; bandValues[5] = 0f; bandValues[6] = -1.5f; bandValues[7] = 1f
                bandValues[8] = 2.5f; bandValues[9] = 3f
            }
            "Bass Boost" -> {
                bandValues[0] = 8f; bandValues[1] = 7f; bandValues[2] = 5.5f; bandValues[3] = 3f
                bandValues[4] = 1f; bandValues[5] = 0f; bandValues[6] = 0f; bandValues[7] = 0f
                bandValues[8] = 0f; bandValues[9] = 0f; bassBoost = 90f
            }
            "Vocal" -> {
                bandValues[0] = -2f; bandValues[1] = -1f; bandValues[2] = 1f; bandValues[3] = 3.5f
                bandValues[4] = 5f; bandValues[5] = 4.5f; bandValues[6] = 3f; bandValues[7] = 1f
                bandValues[8] = 0f; bandValues[9] = -1f
            }
            else -> { (0..9).forEach { bandValues[it] = 0f } }
        }
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
                .verticalScroll(rememberScrollState())
        ) {
            // Cabecera con switch de encendido
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Tune,
                        contentDescription = null,
                        tint = SpotifyGreen,
                        modifier = Modifier.size(24.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Ecualizador de audio Spotifast",
                        color = SpotifyWhite,
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                Switch(
                    checked = isEnabled,
                    onCheckedChange = { isEnabled = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = SpotifyBlack,
                        checkedTrackColor = SpotifyGreen
                    )
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Selector de Presets
            Text(text = "Preajustes", color = SpotifySubtext, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                presets.forEach { preset ->
                    FilterChip(
                        selected = selectedPreset == preset,
                        onClick = { applyPreset(preset) },
                        label = { Text(preset, fontSize = 12.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SpotifyGreen,
                            selectedLabelColor = SpotifyBlack,
                            containerColor = SpotifyCardGray,
                            labelColor = SpotifyWhite
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Deslizadores de las 10 bandas
            Text(text = "Respuesta en frecuencia (10 bandas)", color = SpotifySubtext, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))

            Surface(
                shape = RoundedCornerShape(10.dp),
                color = SpotifyBlack,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    bands.forEachIndexed { index, bandName ->
                        val value = bandValues[index] ?: 0f
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = bandName,
                                color = SpotifyWhite,
                                fontSize = 12.sp,
                                modifier = Modifier.width(60.dp)
                            )
                            Slider(
                                value = value,
                                onValueChange = { bandValues[index] = it },
                                valueRange = -12f..12f,
                                enabled = isEnabled,
                                colors = SliderDefaults.colors(
                                    thumbColor = SpotifyGreen,
                                    activeTrackColor = SpotifyGreen,
                                    inactiveTrackColor = SpotifyCardGray
                                ),
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = "${if (value >= 0) "+" else ""}${String.format("%.1f", value)} dB",
                                color = SpotifySubtext,
                                fontSize = 11.sp,
                                modifier = Modifier.width(55.dp),
                                textAlign = androidx.compose.ui.text.style.TextAlign.End
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // Refuerzo de graves y sonido envolvente
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SpotifyCardGray,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "Bass Boost", color = SpotifyWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Slider(
                            value = bassBoost,
                            onValueChange = { bassBoost = it },
                            valueRange = 0f..100f,
                            enabled = isEnabled,
                            colors = SliderDefaults.colors(thumbColor = SpotifyGreen, activeTrackColor = SpotifyGreen)
                        )
                        Text(text = "${bassBoost.toInt()}%", color = SpotifySubtext, fontSize = 11.sp)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = SpotifyCardGray,
                    modifier = Modifier.weight(1f)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(text = "Sonido 3D / Surround", color = SpotifyWhite, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        Slider(
                            value = surroundEffect,
                            onValueChange = { surroundEffect = it },
                            valueRange = 0f..100f,
                            enabled = isEnabled,
                            colors = SliderDefaults.colors(thumbColor = SpotifyGreen, activeTrackColor = SpotifyGreen)
                        )
                        Text(text = "${surroundEffect.toInt()}%", color = SpotifySubtext, fontSize = 11.sp)
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
