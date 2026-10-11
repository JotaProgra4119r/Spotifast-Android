package rocks.spotifast.mobile.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import rocks.spotifast.mobile.data.api.models.UserProfile
import rocks.spotifast.mobile.ui.theme.*

@Composable
fun ProfileDrawer(
    userProfile: UserProfile?,
    onSettingsClick: () -> Unit,
    onClose: () -> Unit
) {
    val displayName = userProfile?.displayName ?: "Usuario"
    val avatarUrl = userProfile?.images?.firstOrNull()?.url
    val plan = if (userProfile?.product == "premium") "Spotify Premium" else "Spotify Free"

    Surface(
        modifier = Modifier
            .fillMaxHeight()
            .width(320.dp),
        color = SpotifyDarkGray
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
        ) {
            item {
                Spacer(modifier = Modifier.height(24.dp))

                // Avatar y Nombre de usuario real
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (!avatarUrl.isNullOrEmpty()) {
                        AsyncImage(
                            model = avatarUrl,
                            contentDescription = displayName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(CircleShape)
                        )
                    } else {
                        Surface(
                            shape = CircleShape,
                            color = SpotifyCardGray,
                            modifier = Modifier.size(54.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    text = displayName.take(1).uppercase(),
                                    color = SpotifyWhite,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 22.sp
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column {
                        Text(
                            text = displayName,
                            color = SpotifyWhite,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            text = plan,
                            color = SpotifyGreen,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
                HorizontalDivider(color = SpotifyBorder)
                Spacer(modifier = Modifier.height(12.dp))
            }

            // Opciones del menú lateral
            item {
                DrawerMenuItem(Icons.Default.WorkspacePremium, "Tu plan ($plan)")
                DrawerMenuItem(Icons.Default.History, "Escuchado recientemente")
                DrawerMenuItem(Icons.Default.Settings, "Configuración y privacidad", onClick = onSettingsClick)
                DrawerMenuItem(Icons.Default.Info, "Acerca de Spotifast Mobile")
            }
        }
    }
}

@Composable
fun DrawerMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit = {}
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = SpotifyWhite, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Text(text = title, color = SpotifyWhite, fontSize = 15.sp)
    }
}
