package rocks.spotifast.mobile.ui.screens

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import rocks.spotifast.mobile.SpotifastApp
import rocks.spotifast.mobile.data.api.models.UserProfile
import rocks.spotifast.mobile.data.local.ProxyType
import rocks.spotifast.mobile.data.local.ThemePreset
import rocks.spotifast.mobile.data.local.VisualizerType
import rocks.spotifast.mobile.ui.components.DevicesSheet
import rocks.spotifast.mobile.ui.components.EqualizerSheet
import rocks.spotifast.mobile.ui.theme.*

@Composable
fun SettingsScreen(
    userProfile: UserProfile?,
    onBackClick: () -> Unit,
    onLogoutClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val authManager = SpotifastApp.instance.authManager
    val cacheManager = SpotifastApp.instance.cacheManager
    val localMediaManager = SpotifastApp.instance.localMediaManager
    val playbackManager = SpotifastApp.instance.playbackManager
    val settingsManager = SpotifastApp.instance.settingsManager

    val streamingBitrate by settingsManager.streamingBitrate.collectAsState()
    val gapless by settingsManager.gaplessPlayback.collectAsState()
    val normalizeVolume by settingsManager.normalizeVolume.collectAsState()
    val crossfade by settingsManager.crossfadeSeconds.collectAsState()
    val monoAudio by settingsManager.monoAudio.collectAsState()
    val offlineMode by settingsManager.offlineMode.collectAsState()
    val privateSession by settingsManager.privateSession.collectAsState()
    val dataSaver by settingsManager.dataSaver.collectAsState()
    val videoCanvas by settingsManager.videoCanvasEnabled.collectAsState()
    val visualizerType by settingsManager.visualizerType.collectAsState()
    val themePreset by settingsManager.themePreset.collectAsState()
    val accentFromArt by settingsManager.accentFromArt.collectAsState()
    val proxyType by settingsManager.proxyType.collectAsState()
    val proxyHost by settingsManager.proxyHost.collectAsState()
    val proxyPort by settingsManager.proxyPort.collectAsState()
    val notificationsEnabled by settingsManager.notificationsEnabled.collectAsState()

    val connectedDeviceName by playbackManager.connectedDeviceName.collectAsState()

    var customClientId by remember { mutableStateOf(authManager.getCustomClientId() ?: "") }
    var cacheSizeMb by remember { mutableStateOf(cacheManager.getCacheSizeMb()) }
    var proxyHostInput by remember { mutableStateOf(proxyHost) }
    var proxyPortInput by remember { mutableStateOf(proxyPort) }

    var showDevicesSheet by remember { mutableStateOf(false) }
    var showEqualizerSheet by remember { mutableStateOf(false) }
    var showLogoutDialog by remember { mutableStateOf(false) }

    if (showDevicesSheet) {
        DevicesSheet(onDismiss = { showDevicesSheet = false })
    }

    if (showEqualizerSheet) {
        EqualizerSheet(onDismiss = { showEqualizerSheet = false })
    }

    if (showLogoutDialog) {
        AlertDialog(
            onDismissRequest = { showLogoutDialog = false },
            title = { Text("¿Cerrar sesión?") },
            text = { Text("Se borrarán las credenciales locales de Spotify. Tendrás que iniciar sesión de nuevo.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showLogoutDialog = false
                        onLogoutClick()
                    }
                ) {
                    Text("Cerrar sesión", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showLogoutDialog = false }) {
                    Text("Cancelar", color = SpotifyWhite)
                }
            },
            containerColor = SpotifyDarkGray,
            titleContentColor = SpotifyWhite,
            textContentColor = SpotifySubtext
        )
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SpotifyBlack)
            .padding(horizontal = 16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            // Cabecera
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(onClick = onBackClick) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Atrás",
                        tint = SpotifyWhite,
                        modifier = Modifier.size(28.dp)
                    )
                }
                Spacer(modifier = Modifier.width(8.dp))
                Text("Configuración", color = SpotifyWhite, fontSize = 24.sp, fontWeight = FontWeight.Bold)
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // 1. Cuenta
        item {
            val name = userProfile?.displayName ?: "Usuario de Spotify"
            val email = userProfile?.email ?: ""
            val plan = if (userProfile?.product == "premium") "Spotify Premium" else "Spotify Free"
            val country = userProfile?.country ?: ""

            SectionTitle("Cuenta")
            SettingsCard {
                SettingsCategory(
                    icon = Icons.Default.Person,
                    title = "Perfil de Spotify",
                    subtitle = "$name • $plan • $country" + (if (email.isNotEmpty()) "\n$email" else "")
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 2. Dispositivos Connect
        item {
            SectionTitle("Dispositivos")
            SettingsCard {
                SettingsCategory(
                    icon = Icons.Default.Devices,
                    title = "Spotify Connect",
                    subtitle = "Dispositivo actual: ${connectedDeviceName ?: "Buscando..."}",
                    onClick = { showDevicesSheet = true }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 3. Modos de Spotify (Offline, Privacidad, Ahorro de datos)
        item {
            SectionTitle("Modos de Spotify")
            SettingsCard {
                SettingsToggleRow(
                    icon = Icons.Default.CloudOff,
                    title = "Modo sin conexión (Offline)",
                    subtitle = "Solo reproduce música descargada y archivos locales del teléfono.",
                    checked = offlineMode,
                    onCheckedChange = { settingsManager.setOfflineMode(it) }
                )
                Divider(color = SpotifyCardGray, thickness = 0.5.dp)
                SettingsToggleRow(
                    icon = Icons.Default.Security,
                    title = "Sesión privada",
                    subtitle = "Inicia una sesión privada temporal para no compartir tu actividad.",
                    checked = privateSession,
                    onCheckedChange = { settingsManager.setPrivateSession(it) }
                )
                Divider(color = SpotifyCardGray, thickness = 0.5.dp)
                SettingsToggleRow(
                    icon = Icons.Default.DataSaverOn,
                    title = "Modo ahorro de datos",
                    subtitle = "Reduce el uso de red móvil y desactiva cargas pesadas en streaming.",
                    checked = dataSaver,
                    onCheckedChange = { settingsManager.setDataSaver(it) }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 4. Calidad de audio
        item {
            SectionTitle("Calidad de audio")
            SettingsCard {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.HighQuality, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(10.dp))
                        Text("Calidad de transmisión (Bitrate)", color = SpotifyWhite, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "96 kbps (Normal) • 160 kbps (Alta) • 320 kbps (Muy alta)",
                        color = SpotifySubtext,
                        fontSize = 12.sp
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(96, 160, 320).forEach { bitrate ->
                            FilterChip(
                                selected = streamingBitrate == bitrate,
                                onClick = { settingsManager.setStreamingBitrate(bitrate) },
                                label = { Text("${bitrate}k", fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SpotifyGreen,
                                    selectedLabelColor = SpotifyBlack,
                                    containerColor = SpotifyBlack,
                                    labelColor = SpotifyWhite
                                )
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 5. Ecualizador & Sonido
        item {
            SectionTitle("Sonido y Ecualizador")
            SettingsCard {
                SettingsCategory(
                    icon = Icons.Default.Tune,
                    title = "Ecualizador de audio (10 bandas)",
                    subtitle = "Ajusta frecuencias, Bass Boost, sonido 3D y preajustes (Rock, Pop, etc.)",
                    onClick = { showEqualizerSheet = true }
                )
                Divider(color = SpotifyCardGray, thickness = 0.5.dp)
                SettingsToggleRow(
                    icon = Icons.Default.GraphicEq,
                    title = "Normalizar volumen",
                    subtitle = "Establece el mismo nivel de volumen para todas las canciones.",
                    checked = normalizeVolume,
                    onCheckedChange = { settingsManager.setNormalizeVolume(it) }
                )
                Divider(color = SpotifyCardGray, thickness = 0.5.dp)
                SettingsToggleRow(
                    icon = Icons.Default.VolumeMute,
                    title = "Audio mono",
                    subtitle = "Reproduce el mismo sonido por los altavoces izquierdo y derecho.",
                    checked = monoAudio,
                    onCheckedChange = { settingsManager.setMonoAudio(it) }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 6. Reproducción y Fundido (Crossfade, Gapless, Automix)
        item {
            SectionTitle("Reproducción")
            SettingsCard {
                SettingsToggleRow(
                    icon = Icons.Default.Shuffle,
                    title = "Reproducción sin pausas (Gapless)",
                    subtitle = "Elimina los silencios entre pistas continuas.",
                    checked = gapless,
                    onCheckedChange = { settingsManager.setGaplessPlayback(it) }
                )
                Divider(color = SpotifyCardGray, thickness = 0.5.dp)
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Fundido de canciones (Crossfade)", color = SpotifyWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        Text("${crossfade}s", color = SpotifyGreen, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = crossfade.toFloat(),
                        onValueChange = { settingsManager.setCrossfadeSeconds(it.toInt()) },
                        valueRange = 0f..12f,
                        steps = 11,
                        colors = SliderDefaults.colors(thumbColor = SpotifyGreen, activeTrackColor = SpotifyGreen)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 7. Visualizador & MilkDrop
        item {
            SectionTitle("Visualizador & MilkDrop")
            SettingsCard {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Modo visualizador en reproductor", color = SpotifyWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text("Sigue la señal post-equalizer y pre-volume como en Spotifast desktop.", color = SpotifySubtext, fontSize = 12.sp)
                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        VisualizerType.values().forEach { vType ->
                            val label = when (vType) {
                                VisualizerType.OFF -> "Desactivado"
                                VisualizerType.SPECTRUM_BARS -> "Barras (Winamp)"
                                VisualizerType.OSCILLOSCOPE -> "Osciloscopio"
                                VisualizerType.MILKDROP_WAVES -> "MilkDrop Neón"
                            }
                            FilterChip(
                                selected = visualizerType == vType,
                                onClick = { settingsManager.setVisualizerType(vType) },
                                label = { Text(label, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SpotifyGreen,
                                    selectedLabelColor = SpotifyBlack,
                                    containerColor = SpotifyBlack,
                                    labelColor = SpotifyWhite
                                )
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 8. Controles de Video / Canvas
        item {
            SectionTitle("Video y Canvas")
            SettingsCard {
                SettingsToggleRow(
                    icon = Icons.Default.SmartDisplay,
                    title = "Canvas de video",
                    subtitle = "Muestra efectos visuales en bucle en canciones con video disponible.",
                    checked = videoCanvas,
                    onCheckedChange = { settingsManager.setVideoCanvasEnabled(it) }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 9. Almacenamiento y Archivos Locales del Teléfono
        item {
            SectionTitle("Almacenamiento y Archivos Locales")
            SettingsCard {
                SettingsCategory(
                    icon = Icons.Default.FolderOpen,
                    title = "Buscar canciones descargadas en el teléfono",
                    subtitle = "Escanea el almacenamiento interno y SD de este Oppo A58 en busca de música local.",
                    onClick = {
                        scope.launch {
                            val found = localMediaManager.scanLocalTracks()
                            Toast.makeText(context, "Se encontraron ${found.size} canciones locales", Toast.LENGTH_SHORT).show()
                        }
                    }
                )
                Divider(color = SpotifyCardGray, thickness = 0.5.dp)
                SettingsCategory(
                    icon = Icons.Default.Storage,
                    title = "Caché de audio",
                    subtitle = "$cacheSizeMb MB en uso • Toca para limpiar caché",
                    onClick = {
                        cacheManager.clearCache()
                        cacheSizeMb = 0
                        Toast.makeText(context, "Caché liberada correctamente", Toast.LENGTH_SHORT).show()
                    }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 10. Proxy de red (Idéntico a Spotifast desktop src/settings.rs)
        item {
            SectionTitle("Proxy de red")
            SettingsCard {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Modo de Proxy", color = SpotifyWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            ProxyType.DIRECT to "Directo",
                            ProxyType.SYSTEM to "Sistema",
                            ProxyType.HTTP to "HTTP",
                            ProxyType.SOCKS5 to "SOCKS5"
                        ).forEach { (pType, pLabel) ->
                            FilterChip(
                                selected = proxyType == pType,
                                onClick = { settingsManager.setProxyConfig(pType, proxyHostInput, proxyPortInput) },
                                label = { Text(pLabel, fontSize = 11.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SpotifyGreen,
                                    selectedLabelColor = SpotifyBlack,
                                    containerColor = SpotifyBlack,
                                    labelColor = SpotifyWhite
                                )
                            )
                        }
                    }

                    if (proxyType == ProxyType.HTTP || proxyType == ProxyType.SOCKS5) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OutlinedTextField(
                                value = proxyHostInput,
                                onValueChange = { proxyHostInput = it },
                                label = { Text("Host", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(2f)
                            )
                            OutlinedTextField(
                                value = proxyPortInput,
                                onValueChange = { proxyPortInput = it },
                                label = { Text("Puerto", fontSize = 11.sp) },
                                singleLine = true,
                                modifier = Modifier.weight(1f)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Button(
                            onClick = {
                                settingsManager.setProxyConfig(proxyType, proxyHostInput.trim(), proxyPortInput.trim())
                                Toast.makeText(context, "Configuración de proxy guardada", Toast.LENGTH_SHORT).show()
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen),
                            modifier = Modifier.align(Alignment.End)
                        ) {
                            Text("Aplicar Proxy", color = SpotifyBlack, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 11. Apariencia y Skins
        item {
            SectionTitle("Apariencia & Skins")
            SettingsCard {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("Tema visual", color = SpotifyWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            ThemePreset.SPOTIFY_DARK to "Spotify Dark",
                            ThemePreset.OLED_PURE_BLACK to "OLED Puro",
                            ThemePreset.WINAMP_CLASSIC to "Winamp Classic",
                            ThemePreset.CYBERPUNK_NEON to "Cyberpunk"
                        ).forEach { (tPreset, tLabel) ->
                            FilterChip(
                                selected = themePreset == tPreset,
                                onClick = { settingsManager.setThemePreset(tPreset) },
                                label = { Text(tLabel, fontSize = 12.sp) },
                                colors = FilterChipDefaults.filterChipColors(
                                    selectedContainerColor = SpotifyGreen,
                                    selectedLabelColor = SpotifyBlack,
                                    containerColor = SpotifyBlack,
                                    labelColor = SpotifyWhite
                                )
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Divider(color = SpotifyCardGray, thickness = 0.5.dp)
                    Spacer(modifier = Modifier.height(6.dp))

                    SettingsToggleRow(
                        icon = Icons.Default.Palette,
                        title = "Acento dinámico según carátula",
                        subtitle = "Adapta los colores del reproductor a la carátula del álbum en reproducción.",
                        checked = accentFromArt,
                        onCheckedChange = { settingsManager.setAccentFromArt(it) }
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 12. Notificaciones
        item {
            SectionTitle("Notificaciones")
            SettingsCard {
                SettingsToggleRow(
                    icon = Icons.Default.Notifications,
                    title = "Notificación persistente de audio",
                    subtitle = "Muestra la carátula y los controles de reproducción en la barra de notificaciones y pantalla de bloqueo.",
                    checked = notificationsEnabled,
                    onCheckedChange = { settingsManager.setNotificationsEnabled(it) }
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 13. Client ID Personal
        item {
            SectionTitle("Spotify Client ID Privado")
            SettingsCard {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = if (authManager.getCustomClientId().isNullOrEmpty())
                            "Usando App compartida de Spotifast"
                        else
                            "Usando tu Client ID personal (Guardado de forma privada en el dispositivo)",
                        color = SpotifySubtext,
                        fontSize = 12.sp
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = customClientId,
                        onValueChange = { customClientId = it },
                        placeholder = { Text("Pega tu Client ID aquí") },
                        label = { Text("Client ID personal", fontSize = 12.sp) },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = SpotifyGreen,
                            focusedTextColor = SpotifyWhite,
                            unfocusedTextColor = SpotifyWhite,
                            unfocusedBorderColor = SpotifySubtext
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(
                            onClick = {
                                authManager.setCustomClientId(null)
                                customClientId = ""
                                Toast.makeText(context, "Restablecido a app compartida", Toast.LENGTH_SHORT).show()
                            }
                        ) {
                            Text("Restablecer", color = SpotifySubtext)
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                val clean = customClientId.trim()
                                if (clean.isNotEmpty()) {
                                    authManager.setCustomClientId(clean)
                                    Toast.makeText(context, "Client ID guardado en tu dispositivo", Toast.LENGTH_SHORT).show()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = SpotifyGreen)
                        ) {
                            Text("Guardar", color = SpotifyBlack, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 14. Acerca de
        item {
            SectionTitle("Acerca de")
            SettingsCard {
                SettingsCategory(
                    icon = Icons.Default.Info,
                    title = "Spotifast Mobile v0.36.0",
                    subtitle = "Port nativo de Spotifast para Android 15 (Oppo A58 / ColorOS 15)\nBasado en crmne/spotifast • Sin telemetría"
                )
            }
            Spacer(modifier = Modifier.height(24.dp))

            // Botón de Cerrar Sesión
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                Button(
                    onClick = { showLogoutDialog = true },
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .height(46.dp)
                ) {
                    Icon(Icons.Default.Logout, contentDescription = null, tint = SpotifyWhite)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Cerrar sesión", color = SpotifyWhite, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun SectionTitle(text: String) {
    Text(
        text = text,
        color = SpotifyWhite,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 8.dp, start = 4.dp)
    )
}

@Composable
fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = SpotifyDarkGray,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            content()
        }
    }
}

@Composable
fun SettingsCategory(
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = SpotifyWhite, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = SpotifySubtext, fontSize = 12.sp, lineHeight = 16.sp)
        }
    }
}

@Composable
fun SettingsToggleRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onCheckedChange(!checked) }
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, color = SpotifyWhite, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = SpotifySubtext, fontSize = 11.sp, lineHeight = 15.sp)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = SpotifyBlack,
                checkedTrackColor = SpotifyGreen
            )
        )
    }
}
