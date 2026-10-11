package rocks.spotifast.mobile.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import rocks.spotifast.mobile.SpotifastApp
import rocks.spotifast.mobile.data.local.ThemePreset

data class SpotifastColors(
    val background: Color,
    val surface: Color,
    val card: Color,
    val cardHover: Color,
    val accent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val border: Color
)

val SpotifyDarkColors = SpotifastColors(
    background = Color(0xFF121212),
    surface = Color(0xFF181818),
    card = Color(0xFF242424),
    cardHover = Color(0xFF2E2E2E),
    accent = Color(0xFF1DB954),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFB3B3B3),
    border = Color(0xFF282828)
)

val OledBlackColors = SpotifastColors(
    background = Color(0xFF000000),
    surface = Color(0xFF0A0A0A),
    card = Color(0xFF141414),
    cardHover = Color(0xFF222222),
    accent = Color(0xFF1ED760),
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFA5A5A5),
    border = Color(0xFF1E1E1E)
)

val WinampClassicColors = SpotifastColors(
    background = Color(0xFF23272A),
    surface = Color(0xFF2B2F34),
    card = Color(0xFF34383E),
    cardHover = Color(0xFF41464D),
    accent = Color(0xFF00FF66), // LED Verde clásico de Winamp
    textPrimary = Color(0xFF00FF66),
    textSecondary = Color(0xFF99AAB5),
    border = Color(0xFF5865F2)
)

val CyberpunkNeonColors = SpotifastColors(
    background = Color(0xFF0D0221),
    surface = Color(0xFF15082C),
    card = Color(0xFF220D45),
    cardHover = Color(0xFF2F145C),
    accent = Color(0xFF00F0FF), // Cyan neón
    textPrimary = Color(0xFFFFFFFF),
    textSecondary = Color(0xFFFF007F), // Magenta neón
    border = Color(0xFF711C91)
)

val LocalSpotifastColors = staticCompositionLocalOf { SpotifyDarkColors }

@Composable
fun SpotifastTheme(content: @Composable () -> Unit) {
    val themePreset by SpotifastApp.instance.settingsManager.themePreset.collectAsState()

    val colors = when (themePreset) {
        ThemePreset.SPOTIFY_DARK -> SpotifyDarkColors
        ThemePreset.OLED_PURE_BLACK -> OledBlackColors
        ThemePreset.WINAMP_CLASSIC -> WinampClassicColors
        ThemePreset.CYBERPUNK_NEON -> CyberpunkNeonColors
    }

    val materialScheme = darkColorScheme(
        primary = colors.accent,
        onPrimary = colors.background,
        surface = colors.surface,
        onSurface = colors.textPrimary,
        background = colors.background,
        onBackground = colors.textPrimary,
        surfaceVariant = colors.card,
        onSurfaceVariant = colors.textSecondary
    )

    CompositionLocalProvider(LocalSpotifastColors provides colors) {
        MaterialTheme(
            colorScheme = materialScheme,
            content = content
        )
    }
}
