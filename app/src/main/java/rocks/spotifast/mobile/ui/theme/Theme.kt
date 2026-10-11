package rocks.spotifast.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = SpotifyGreen,
    onPrimary = SpotifyBlack,
    surface = SpotifyBlack,
    onSurface = SpotifyWhite,
    background = SpotifyBlack,
    onBackground = SpotifyWhite,
    surfaceVariant = SpotifyCardGray,
    onSurfaceVariant = SpotifySubtext
)

@Composable
fun SpotifastTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
