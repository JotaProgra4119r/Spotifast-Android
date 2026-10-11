package rocks.spotifast.mobile

import org.junit.Assert.*
import org.junit.Test
import rocks.spotifast.mobile.data.api.models.*
import rocks.spotifast.mobile.data.local.ProxyType
import rocks.spotifast.mobile.data.local.ThemePreset
import rocks.spotifast.mobile.data.local.VisualizerType
import rocks.spotifast.mobile.data.lyrics.LyricLine
import rocks.spotifast.mobile.data.lyrics.TrackLyrics

class PortParityTest {

    @Test
    fun testTrackModelParityWithDesktop() {
        val track = Track(
            id = "test_track_1",
            name = "Bohemian Rhapsody",
            uri = "spotify:track:test_track_1",
            durationMs = 354000L,
            previewUrl = null,
            artists = listOf(Artist(id = "artist_1", name = "Queen", uri = "spotify:artist:artist_1")),
            album = Album(
                id = "album_1",
                name = "A Night at the Opera",
                uri = "spotify:album:album_1",
                images = listOf(SpotifyImage("https://example.com/cover.jpg", 300, 300)),
                releaseDate = "1975-11-21"
            ),
            isPlayable = true,
            isLocal = false,
            isDownloaded = false
        )

        assertEquals("Queen", track.getArtistNames())
        assertEquals("https://example.com/cover.jpg", track.getCoverUrl())
        assertFalse(track.isLocal)
    }

    @Test
    fun testLyricsParityWithDesktopLrclib() {
        val sampleLines = listOf(
            LyricLine(0L, "Is this the real life?"),
            LyricLine(3500L, "Is this just fantasy?"),
            LyricLine(7000L, "Caught in a landslide")
        )
        val lyrics = TrackLyrics(
            lines = sampleLines,
            isSynced = true,
            isInstrumental = false
        )

        assertTrue(lyrics.isSynced)
        assertFalse(lyrics.isInstrumental)
        assertEquals(3, lyrics.lines.size)
        assertEquals("Is this just fantasy?", lyrics.lines[1].text)
        assertEquals(3500L, lyrics.lines[1].timeMs)
    }

    @Test
    fun testVisualizerModesParityWithDesktopVisRs() {
        // Desktop src/vis.rs tiene: Off, Spectrum Bars (19 bandas Winamp), Oscilloscope Scope, MilkDrop
        val modes = VisualizerType.values()
        assertEquals(4, modes.size)
        assertTrue(modes.contains(VisualizerType.OFF))
        assertTrue(modes.contains(VisualizerType.SPECTRUM_BARS))
        assertTrue(modes.contains(VisualizerType.OSCILLOSCOPE))
        assertTrue(modes.contains(VisualizerType.MILKDROP_WAVES))
    }

    @Test
    fun testProxyModesParityWithDesktopSettingsRs() {
        // Desktop src/settings.rs enum ProxyMode: Off (Direct), System, Http, Socks
        val proxyTypes = ProxyType.values()
        assertEquals(4, proxyTypes.size)
        assertTrue(proxyTypes.contains(ProxyType.DIRECT))
        assertTrue(proxyTypes.contains(ProxyType.SYSTEM))
        assertTrue(proxyTypes.contains(ProxyType.HTTP))
        assertTrue(proxyTypes.contains(ProxyType.SOCKS5))
    }

    @Test
    fun testThemePresetsParityWithDesktopThemeRs() {
        val themes = ThemePreset.values()
        assertEquals(4, themes.size)
        assertTrue(themes.contains(ThemePreset.SPOTIFY_DARK))
        assertTrue(themes.contains(ThemePreset.OLED_PURE_BLACK))
        assertTrue(themes.contains(ThemePreset.WINAMP_CLASSIC))
        assertTrue(themes.contains(ThemePreset.CYBERPUNK_NEON))
    }
}
