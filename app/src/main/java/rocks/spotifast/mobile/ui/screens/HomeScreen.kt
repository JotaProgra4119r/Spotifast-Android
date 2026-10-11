package rocks.spotifast.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import rocks.spotifast.mobile.data.api.SpotifyClient
import rocks.spotifast.mobile.data.api.models.Playlist
import rocks.spotifast.mobile.data.api.models.Track
import rocks.spotifast.mobile.data.api.models.UserProfile
import rocks.spotifast.mobile.ui.theme.*

@Composable
fun HomeScreen(
    userProfile: UserProfile?,
    onProfileClick: () -> Unit,
    onLikedSongsClick: () -> Unit,
    onPlaylistClick: (Playlist) -> Unit,
    onTrackClick: (Track) -> Unit,
    modifier: Modifier = Modifier
) {
    var playlists by remember { mutableStateOf<List<Playlist>>(emptyList()) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        isLoading = true
        try {
            val response = SpotifyClient.apiService.getUserPlaylists(limit = 50)
            if (response.isSuccessful && response.body() != null) {
                playlists = response.body()!!.items
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
            .background(SpotifyBlack)
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
                        color = SpotifyCardGray,
                        modifier = Modifier
                            .size(38.dp)
                            .clickable { onProfileClick() }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = (userProfile?.displayName ?: "U").take(1).uppercase(),
                                color = SpotifyWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 16.sp
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Text(
                    text = "Hola, ${userProfile?.displayName?.split(" ")?.firstOrNull() ?: ""}",
                    color = SpotifyWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(20.dp))
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
                        color = SpotifyCardGray,
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .clickable { onLikedSongsClick() }
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Surface(
                                color = SpotifyGreen,
                                modifier = Modifier
                                    .width(56.dp)
                                    .fillMaxHeight()
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(Icons.Default.Favorite, contentDescription = null, tint = SpotifyBlack)
                                }
                            }
                            Text(
                                text = "Tus me gusta",
                                color = SpotifyWhite,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp)
                            )
                        }
                    }

                    if (playlists.isNotEmpty()) {
                        val firstPl = playlists.first()
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
                if (playlists.size > 1) {
                    playlists.drop(1).take(7).chunked(2).forEach { pair ->
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

        // Título: Tus Listas
        item {
            Text(
                text = "Tus listas de reproducción",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = SpotifyWhite,
                modifier = Modifier.padding(bottom = 12.dp)
            )
        }

        if (isLoading) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = SpotifyGreen)
                }
            }
        } else {
            items(playlists) { playlist ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                        .clip(RoundedCornerShape(4.dp))
                        .clickable { onPlaylistClick(playlist) },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val cover = playlist.getCoverUrl(300)
                    if (!cover.isNullOrEmpty()) {
                        AsyncImage(
                            model = cover,
                            contentDescription = playlist.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(SpotifyCardGray)
                        )
                    } else {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = SpotifyCardGray,
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.MusicNote, contentDescription = null, tint = SpotifySubtext)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = playlist.name,
                            color = SpotifyWhite,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "Playlist • ${playlist.tracks?.total ?: 0} canciones",
                            color = SpotifySubtext,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(80.dp))
        }
    }
}

@Composable
fun RealPlaylistCard(
    playlist: Playlist,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = SpotifyCardGray,
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
                        .background(SpotifyCardGray)
                )
            } else {
                Surface(
                    color = SpotifyCardHover,
                    modifier = Modifier
                        .width(56.dp)
                        .fillMaxHeight()
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = SpotifySubtext)
                    }
                }
            }

            Text(
                text = playlist.name,
                color = SpotifyWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 8.dp)
            )
        }
    }
}
