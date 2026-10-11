package rocks.spotifast.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import rocks.spotifast.mobile.data.api.SpotifyClient
import rocks.spotifast.mobile.data.api.models.*
import rocks.spotifast.mobile.ui.theme.*

data class MadeForYouItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val coverUrl: String,
    val uri: String
)

@Composable
fun HomeScreen(
    userProfile: UserProfile?,
    onProfileClick: () -> Unit,
    onLikedSongsClick: () -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onTrackClick: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    var userPlaylists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var recentlyPlayedTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var topArtists by remember { mutableStateOf<List<Artist>>(emptyList()) }
    var topTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var recommendedTracks by remember { mutableStateOf<List<Track>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    var madeForYouList by remember {
        mutableStateOf(
            listOf(
                MadeForYouItem(
                    id = "dw",
                    title = "Discover Weekly",
                    subtitle = "Tus descubrimientos semanales y joyas ocultas",
                    coverUrl = "https://misc.scdn.co/personalized/default-cheetah/discover-weekly_640.jpg",
                    uri = "spotify:playlist:37i9dQZEVXcQ9JaJVbyA7e"
                ),
                MadeForYouItem(
                    id = "rr",
                    title = "Release Radar",
                    subtitle = "Últimos lanzamientos de tus artistas seguidos",
                    coverUrl = "https://misc.scdn.co/personalized/default-cheetah/release-radar_640.jpg",
                    uri = "spotify:playlist:37i9dQZEVXbo6gZ4u35r3M"
                ),
                MadeForYouItem(
                    id = "dm1",
                    title = "Daily Mix 1",
                    subtitle = "Mix personalizado para ti",
                    coverUrl = "https://dailymix-images.scdn.co/v2/img/ab6761610000e5eb5704a64f34fe29ff73ab56bb/1/en/default",
                    uri = "spotify:playlist:37i9dQZF1E36gC9fB4h1A1"
                ),
                MadeForYouItem(
                    id = "dm2",
                    title = "Daily Mix 2",
                    subtitle = "Mix con tus canciones y artistas frecuentes",
                    coverUrl = "https://dailymix-images.scdn.co/v2/img/ab6761610000e5ebc36dd9eb55fb0db4911f25dd/2/en/default",
                    uri = "spotify:playlist:37i9dQZF1E37xR7fB1Tz2M"
                )
            )
        )
    }

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            coroutineScope {
                // 1. Playlists del usuario para accesos directos
                val plDeferred = async {
                    try { SpotifyClient.apiService.getUserPlaylists(limit = 30).body()?.items ?: emptyList() } catch (_: Exception) { emptyList() }
                }

                // 2. Reproducido recientemente (Screenshot 1)
                val recentsDeferred = async {
                    try {
                        val res = SpotifyClient.apiService.getRecentlyPlayed(limit = 20)
                        res.body()?.items?.map { it.track }?.distinctBy { it.id } ?: emptyList()
                    } catch (_: Exception) { emptyList() }
                }

                // 3. Top artistas (Screenshot 1)
                val artistsDeferred = async {
                    try { SpotifyClient.apiService.getTopArtists(limit = 15).body()?.items ?: emptyList() } catch (_: Exception) { emptyList() }
                }

                // 4. Top canciones (Screenshot 3)
                val topTracksDeferred = async {
                    try { SpotifyClient.apiService.getTopTracks(limit = 10).body()?.items ?: emptyList() } catch (_: Exception) { emptyList() }
                }

                // 5. Recomendaciones (Screenshot 3)
                val recsDeferred = async {
                    try {
                        SpotifyClient.apiService.getRecommendations(seedGenres = "pop,latin,rock", limit = 10).body()?.tracks ?: emptyList()
                    } catch (_: Exception) { emptyList() }
                }

                // 6. Búsqueda de mixes dinámicos
                val searchMixesDeferred = async {
                    try {
                        SpotifyClient.apiService.search(query = "Daily Mix", type = "playlist", limit = 10).body()?.playlists?.items ?: emptyList()
                    } catch (_: Exception) { emptyList() }
                }

                val allPlaylists = plDeferred.await()
                userPlaylists = allPlaylists
                recentlyPlayedTracks = recentsDeferred.await()
                topArtists = artistsDeferred.await()
                topTracks = topTracksDeferred.await()
                recommendedTracks = recsDeferred.await()

                // Filtrar o componer mixes dinámicos del usuario
                val dynamicMixes = mutableListOf<MadeForYouItem>()
                val userMixes = allPlaylists.filter { pl ->
                    val n = pl.name.lowercase()
                    n.contains("mix") || n.contains("discover") || n.contains("radar") || n.contains("descubrimiento") || pl.owner?.id == "spotify"
                }
                userMixes.forEach { pl ->
                    dynamicMixes.add(
                        MadeForYouItem(
                            id = pl.id,
                            title = pl.name,
                            subtitle = pl.description ?: "Por ${pl.owner?.displayName ?: "Spotify"}",
                            coverUrl = pl.getCoverUrl(300) ?: "",
                            uri = pl.uri
                        )
                    )
                }

                val searchedMixes = searchMixesDeferred.await()
                searchedMixes.forEach { pl ->
                    if (dynamicMixes.none { it.id == pl.id }) {
                        dynamicMixes.add(
                            MadeForYouItem(
                                id = pl.id,
                                title = pl.name,
                                subtitle = pl.description ?: "Por ${pl.owner?.displayName ?: "Spotify"}",
                                coverUrl = pl.getCoverUrl(300) ?: "",
                                uri = pl.uri
                            )
                        )
                    }
                }

                if (dynamicMixes.isNotEmpty()) {
                    madeForYouList = dynamicMixes.take(10)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            isLoading = false
        }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(LocalSpotifastColors.current.background)
            .padding(horizontal = 16.dp)
    ) {
        item {
            Spacer(modifier = Modifier.height(16.dp))

            // Barra superior con avatar y saludo
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
                            .size(38.dp)
                            .clip(CircleShape)
                            .clickable { onProfileClick() }
                    )
                } else {
                    Surface(
                        shape = CircleShape,
                        color = LocalSpotifastColors.current.card,
                        modifier = Modifier
                            .size(38.dp)
                            .clickable { onProfileClick() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = (userProfile?.displayName ?: "U").take(1).uppercase(),
                                color = LocalSpotifastColors.current.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Hola, ${userProfile?.displayName?.split(" ")?.firstOrNull() ?: ""}",
                    color = LocalSpotifastColors.current.textPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(16.dp))
        }

        // Accesos rápidos (Tus me gusta + Playlists del usuario idéntico a quick_access en desktop)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                // Fila 1: Tarjeta especial de "Tus me gusta"
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = LocalSpotifastColors.current.card,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clickable { onLikedSongsClick() }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = LocalSpotifastColors.current.accent,
                                modifier = Modifier
                                    .width(56.dp)
                                    .fillMaxHeight()
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Favorite, contentDescription = null, tint = LocalSpotifastColors.current.background)
                                }
                            }
                            Text(
                                text = "Tus me gusta",
                                color = LocalSpotifastColors.current.textPrimary,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }

                    if (userPlaylists.isNotEmpty()) {
                        val firstPl = userPlaylists.first()
                        RealPlaylistCard(
                            playlist = firstPl,
                            onClick = { onPlaylistClick(firstPl) },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }

                // Cuadrícula de playlists
                if (userPlaylists.size > 1) {
                    userPlaylists.drop(1).take(5).chunked(2).forEach { pair ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            pair.forEach { pl ->
                                RealPlaylistCard(
                                    playlist = pl,
                                    onClick = { onPlaylistClick(pl) },
                                    modifier = Modifier.weight(1f)
                                )
                            }
                            if (pair.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(28.dp))
        }

        // 1. SHELF: "Hecho para ti" (Discover Weekly, Release Radar, Daily Mix 1-4 idéntico a Captura 1)
        item {
            ShelfHeader("Hecho para ti")
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(vertical = 4.dp)
            ) {
                items(madeForYouList) { item ->
                    CardShelfItem(
                        title = item.title,
                        subtitle = item.subtitle,
                        imageUrl = item.coverUrl,
                        onClick = {
                            val fakePl = Playlist(
                                id = item.id,
                                name = item.title,
                                description = item.subtitle,
                                uri = item.uri,
                                images = listOf(SpotifyImage(url = item.coverUrl, height = 300, width = 300)),
                                owner = PlaylistOwner(id = "spotify", displayName = "Spotify"),
                                tracks = PlaylistTracksInfo(total = 50)
                            )
                            onPlaylistClick(fakePl)
                        }
                    )
                }
            }
            Spacer(modifier = Modifier.height(28.dp))
        }

        // 2. SHELF: "Reproducido recientemente" (Captura 1)
        if (recentlyPlayedTracks.isNotEmpty()) {
            item {
                ShelfHeader("Reproducido recientemente")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(recentlyPlayedTracks) { track ->
                        CardShelfItem(
                            title = track.name,
                            subtitle = track.getArtistNames(),
                            imageUrl = track.getCoverUrl(300),
                            onClick = { onTrackClick(track) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }
        }

        // 3. SHELF: "Tus artistas más escuchados" (Captura 1: Fotos circulares)
        if (topArtists.isNotEmpty()) {
            item {
                ShelfHeader("Tus artistas más escuchados")
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(vertical = 4.dp)
                ) {
                    items(topArtists) { artist ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .width(115.dp)
                                .clickable {
                                    // Búsqueda de pistas del artista
                                }
                        ) {
                            val imgUrl = artist.getImageUrl()
                            if (!imgUrl.isNullOrEmpty()) {
                                AsyncImage(
                                    model = imgUrl,
                                    contentDescription = artist.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(105.dp)
                                        .clip(CircleShape)
                                        .background(LocalSpotifastColors.current.card)
                                )
                            } else {
                                Surface(
                                    shape = CircleShape,
                                    color = LocalSpotifastColors.current.card,
                                    modifier = Modifier.size(105.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(Icons.Default.Person, contentDescription = null, tint = LocalSpotifastColors.current.textSecondary, modifier = Modifier.size(48.dp))
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.height(8.dp))

                            Text(
                                text = artist.name,
                                color = LocalSpotifastColors.current.textPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Artista",
                                color = LocalSpotifastColors.current.textSecondary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }
        }

        // 4. SHELF: "Tus canciones más escuchadas" (Tabla 1 a 10 idéntica a Captura 3)
        if (topTracks.isNotEmpty()) {
            item {
                ShelfHeader("Tus canciones más escuchadas")
                Spacer(modifier = Modifier.height(4.dp))
            }

            itemsIndexed(topTracks) { index, track ->
                RankedTrackRow(
                    rank = index + 1,
                    track = track,
                    onClick = { onTrackClick(track) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(28.dp))
            }
        }

        // 5. SHELF: "Recomendado para ti" (Captura 3)
        if (recommendedTracks.isNotEmpty()) {
            item {
                ShelfHeader("Recomendado para ti")
                Spacer(modifier = Modifier.height(4.dp))
            }

            itemsIndexed(recommendedTracks.take(5)) { index, track ->
                RankedTrackRow(
                    rank = index + 1,
                    track = track,
                    onClick = { onTrackClick(track) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun ShelfHeader(title: String) {
    Text(
        text = title,
        color = LocalSpotifastColors.current.textPrimary,
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.padding(bottom = 12.dp)
    )
}

@Composable
fun CardShelfItem(
    title: String,
    subtitle: String,
    imageUrl: String?,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(135.dp)
            .clip(RoundedCornerShape(8.dp))
            .clickable { onClick() }
            .padding(4.dp)
    ) {
        if (!imageUrl.isNullOrEmpty()) {
            AsyncImage(
                model = imageUrl,
                contentDescription = title,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(127.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(LocalSpotifastColors.current.card)
            )
        } else {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = LocalSpotifastColors.current.card,
                modifier = Modifier.size(127.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = LocalSpotifastColors.current.textSecondary, modifier = Modifier.size(40.dp))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = title,
            color = LocalSpotifastColors.current.textPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = subtitle,
            color = LocalSpotifastColors.current.textSecondary,
            fontSize = 11.sp,
            lineHeight = 14.sp,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}

@Composable
fun RankedTrackRow(
    rank: Int,
    track: Track,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(6.dp))
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Número de posición (1..10)
        Text(
            text = "$rank",
            color = LocalSpotifastColors.current.textSecondary,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp,
            modifier = Modifier.width(28.dp)
        )

        // Portada
        val cover = track.getCoverUrl(300)
        if (!cover.isNullOrEmpty()) {
            AsyncImage(
                model = cover,
                contentDescription = track.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(46.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(LocalSpotifastColors.current.card)
            )
        } else {
            Surface(
                shape = RoundedCornerShape(4.dp),
                color = LocalSpotifastColors.current.card,
                modifier = Modifier.size(46.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Default.MusicNote, contentDescription = null, tint = LocalSpotifastColors.current.textSecondary)
                }
            }
        }

        Spacer(modifier = Modifier.width(12.dp))

        // Título y Artista
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.name,
                color = LocalSpotifastColors.current.textPrimary,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "${track.getArtistNames()} • ${track.album?.name ?: ""}",
                color = LocalSpotifastColors.current.textSecondary,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }

        Spacer(modifier = Modifier.width(8.dp))

        // Icono de favorito
        Icon(
            imageVector = Icons.Default.Favorite,
            contentDescription = null,
            tint = LocalSpotifastColors.current.accent,
            modifier = Modifier.size(16.dp)
        )

        Spacer(modifier = Modifier.width(10.dp))

        // Duración
        Text(
            text = formatDuration(track.durationMs),
            color = LocalSpotifastColors.current.textSecondary,
            fontSize = 12.sp
        )
    }
}

private fun formatDuration(ms: Long): String {
    val sec = (ms / 1000) % 60
    val min = (ms / (1000 * 60))
    return String.format("%d:%02d", min, sec)
}

@Composable
fun RealPlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = LocalSpotifastColors.current.card,
        modifier = modifier
            .height(56.dp)
            .clickable { onClick() }
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            val cover = playlist.getCoverUrl(300)
            if (!cover.isNullOrEmpty()) {
                AsyncImage(
                    model = cover,
                    contentDescription = playlist.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(56.dp)
                        .fillMaxHeight()
                        .background(LocalSpotifastColors.current.card)
                )
            } else {
                Surface(
                    color = LocalSpotifastColors.current.cardHover,
                    modifier = Modifier
                        .width(56.dp)
                        .fillMaxHeight()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = LocalSpotifastColors.current.textSecondary)
                    }
                }
            }

            Text(
                text = playlist.name,
                color = LocalSpotifastColors.current.textPrimary,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}
