package rocks.spotifast.mobile.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LibraryMusic
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import rocks.spotifast.mobile.ui.theme.SpotifyBlack
import rocks.spotifast.mobile.ui.theme.SpotifyGreen
import rocks.spotifast.mobile.ui.theme.SpotifySubtext
import rocks.spotifast.mobile.ui.theme.SpotifyWhite

enum class Screen(val title: String) {
    HOME("Inicio"),
    SEARCH("Buscar"),
    LIBRARY("Tu biblioteca")
}

@Composable
fun BottomNavBar(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier.fillMaxWidth(),
        containerColor = SpotifyBlack.copy(alpha = 0.95f),
        contentColor = SpotifyWhite
    ) {
        NavigationBarItem(
            selected = currentScreen == Screen.HOME,
            onClick = { onScreenSelected(Screen.HOME) },
            icon = { Icon(Icons.Default.Home, contentDescription = "Inicio") },
            label = { Text("Inicio") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SpotifyWhite,
                selectedTextColor = SpotifyWhite,
                unselectedIconColor = SpotifySubtext,
                unselectedTextColor = SpotifySubtext,
                indicatorColor = Color.Transparent
            )
        )

        NavigationBarItem(
            selected = currentScreen == Screen.SEARCH,
            onClick = { onScreenSelected(Screen.SEARCH) },
            icon = { Icon(Icons.Default.Search, contentDescription = "Buscar") },
            label = { Text("Buscar") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SpotifyWhite,
                selectedTextColor = SpotifyWhite,
                unselectedIconColor = SpotifySubtext,
                unselectedTextColor = SpotifySubtext,
                indicatorColor = Color.Transparent
            )
        )

        NavigationBarItem(
            selected = currentScreen == Screen.LIBRARY,
            onClick = { onScreenSelected(Screen.LIBRARY) },
            icon = { Icon(Icons.Default.LibraryMusic, contentDescription = "Tu biblioteca") },
            label = { Text("Tu biblioteca") },
            colors = NavigationBarItemDefaults.colors(
                selectedIconColor = SpotifyWhite,
                selectedTextColor = SpotifyWhite,
                unselectedIconColor = SpotifySubtext,
                unselectedTextColor = SpotifySubtext,
                indicatorColor = Color.Transparent
            )
        )
    }
}
