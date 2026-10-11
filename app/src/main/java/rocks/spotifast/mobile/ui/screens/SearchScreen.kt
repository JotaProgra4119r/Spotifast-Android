package rocks.spotifast.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import rocks.spotifast.mobile.SpotifastApp
import rocks.spotifast.mobile.data.api.SpotifyClient
import rocks.spotifast.mobile.data.api.models.Track
import rocks.spotifast.mobile.data.api.models.UserProfile
import rocks.spotifast.mobile.ui.components.CategoryCard
import rocks.spotifast.mobile.ui.components.TrackListItem
import rocks.spotifast.mobile.ui.theme.*

@Composable
fun SearchScreen(
    userProfile: UserProfile?,
    onProfileClick: () -> Unit,
    onTrackClick: (Track, List<Track>) -> Unit,
    modifier: Modifier = Modifier
) {
    var searchQuery by remember { mutableStateOf("") }
    var searchResults by remember { mutableStateOf<List<Track>>(emptyList()) }
    var localMatches by remember { mutableStateOf<List<Track>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }
    var searchError by remember { mutableStateOf<String?>(null) }

    val localTracks by SpotifastApp.instance.localMediaManager.localTracks.collectAsState()

    val quickChips = listOf("Pop", "Rock", "Hits", "Reggaeton", "Hip Hop", "Electrónica", "Acústico")

    // Búsqueda en vivo con debounce de 350 ms idéntica a Spotifast desktop
    LaunchedEffect(searchQuery, localTracks) {
        val query = searchQuery.trim()
        if (query.isNotEmpty()) {
            // Filtrar archivos locales instantáneamente
            localMatches = localTracks.filter {
                it.name.contains(query, ignoreCase = true) ||
                        it.getArtistNames().contains(query, ignoreCase = true) ||
                        (it.album?.name?.contains(query, ignoreCase = true) == true)
            }

            delay(350)
            isLoading = true
            searchError = null
            try {
                // Spotify Developer limit = 10 para evitar Error HTTP 400
                val res = SpotifyClient.apiService.search(
                    query = query,
                    type = "track",
                    limit = 10
                )
                if (res.isSuccessful && res.body() != null) {
                    searchResults = res.body()?.tracks?.items ?: emptyList()
                } else {
                    searchError = if (res.code() == 400) {
                        "Consulta no válida en Spotify. Prueba con otro término."
                    } else {
                        "Error en búsqueda Spotify (${res.code()})"
                    }
                }
            } catch (e: Exception) {
                searchError = "Modo sin conexión a Spotify: ${e.localizedMessage}"
            } finally {
                isLoading = false
            }
        } else {
            searchResults = emptyList()
            localMatches = emptyList()
            searchError = null
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(SpotifyBlack)
            .padding(horizontal = 16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            // Cabecera: Avatar y Título "Buscar"
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val avatarUrl = userProfile?.getAvatarUrl()
                if (!avatarUrl.isNullOrEmpty()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "Perfil",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .clickable { onProfileClick() }
                    )
                } else {
                    Surface(
                        shape = CircleShape,
                        color = SpotifyCardGray,
                        modifier = Modifier
                            .size(36.dp)
                            .clickable { onProfileClick() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("U", color = SpotifyWhite, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Buscar",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = SpotifyWhite
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Barra de búsqueda interactiva
            TextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = { Text("¿Qué quieres escuchar?", color = SpotifySubtext, fontSize = 15.sp) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = SpotifyBlack) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(Icons.Default.Clear, contentDescription = "Limpiar", tint = SpotifyBlack)
                        }
                    }
                },
                singleLine = true,
                shape = RoundedCornerShape(8.dp),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = SpotifyWhite,
                    unfocusedContainerColor = SpotifyWhite,
                    focusedTextColor = SpotifyBlack,
                    unfocusedTextColor = SpotifyBlack,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                ),
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Chips rápidos de géneros y éxitos solicitados por el usuario
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                quickChips.forEach { chip ->
                    FilterChip(
                        selected = searchQuery.equals(chip, ignoreCase = true),
                        onClick = { searchQuery = chip },
                        label = { Text(chip, fontSize = 13.sp) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = SpotifyGreen,
                            selectedLabelColor = SpotifyBlack,
                            containerColor = SpotifyCardGray,
                            labelColor = SpotifyWhite
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))
        }

        if (searchQuery.isNotEmpty()) {
            // Mostrar coincidencias locales descargadas primero si existen
            if (localMatches.isNotEmpty()) {
                item {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(vertical = 6.dp)
                    ) {
                        Icon(Icons.Default.Storage, contentDescription = null, tint = SpotifyGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "En tu dispositivo (${localMatches.size})",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = SpotifyGreen
                        )
                    }
                }

                items(localMatches) { track ->
                    TrackListItem(
                        track = track,
                        isDownloaded = true,
                        onClick = { onTrackClick(track, localMatches) }
                    )
                }
            }

            if (isLoading && searchResults.isEmpty()) {
                item {
                    Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SpotifyGreen)
                    }
                }
            } else if (searchError != null && localMatches.isEmpty()) {
                item {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = SpotifyDarkGray,
                        modifier = Modifier.fillMaxWidth().padding(8.dp)
                    ) {
                        Text(
                            text = searchError ?: "",
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                }
            } else if (searchResults.isEmpty() && localMatches.isEmpty()) {
                item {
                    Text(
                        text = "No se encontraron resultados para '$searchQuery'",
                        color = SpotifySubtext,
                        fontSize = 14.sp,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            } else if (searchResults.isNotEmpty()) {
                item {
                    Text(
                        text = "En Spotify",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = SpotifyWhite,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                items(searchResults) { track ->
                    TrackListItem(
                        track = track,
                        onClick = { onTrackClick(track, searchResults) }
                    )
                }
            }
        } else {
            // Categorías de exploración
            item {
                Text("Explorar géneros", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = SpotifyWhite)
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CategoryCard("Música", TagPink, Modifier.weight(1f).clickable { searchQuery = "Música" })
                    CategoryCard("Podcasts", TagGreen, Modifier.weight(1f).clickable { searchQuery = "Podcasts" })
                }
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    CategoryCard("Pop", TagBlue, Modifier.weight(1f).clickable { searchQuery = "Pop" })
                    CategoryCard("Rock", TagPurple, Modifier.weight(1f).clickable { searchQuery = "Rock" })
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}
