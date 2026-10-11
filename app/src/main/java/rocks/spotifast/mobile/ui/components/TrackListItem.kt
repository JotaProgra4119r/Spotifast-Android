package rocks.spotifast.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowCircleDown
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
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
import rocks.spotifast.mobile.data.api.models.Track
import rocks.spotifast.mobile.ui.theme.*

@Composable
fun TrackListItem(
    track: Track,
    isDownloaded: Boolean = false,
    onClick: () -> Unit,
    onToggleDownload: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Carátula real del álbum
        val coverUrl = track.getCoverUrl()
        AsyncImage(
            model = coverUrl,
            contentDescription = track.name,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(48.dp)
                .clip(RoundedCornerShape(4.dp))
                .background(SpotifyCardGray)
        )

        Spacer(modifier = Modifier.width(12.dp))

        // Título de la pista y artistas
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = track.name,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = SpotifyWhite,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isDownloaded) {
                    Icon(
                        imageVector = Icons.Default.ArrowCircleDown,
                        contentDescription = "Descargada",
                        tint = SpotifyGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                }
                Text(
                    text = track.getArtistNames(),
                    fontSize = 13.sp,
                    color = SpotifySubtext,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        IconButton(onClick = { onToggleDownload?.invoke() }) {
            Icon(
                imageVector = Icons.Default.MoreVert,
                contentDescription = "Opciones",
                tint = SpotifySubtext
            )
        }
    }
}
