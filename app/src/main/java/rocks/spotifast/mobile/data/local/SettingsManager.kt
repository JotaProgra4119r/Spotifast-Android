package rocks.spotifast.mobile.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemePreset {
    SPOTIFY_DARK,
    OLED_PURE_BLACK,
    WINAMP_CLASSIC,
    CYBERPUNK_NEON
}

enum class ProxyType {
    DIRECT,
    SYSTEM,
    HTTP,
    SOCKS5
}

enum class VisualizerType {
    OFF,
    SPECTRUM_BARS,
    OSCILLOSCOPE,
    MILKDROP_WAVES
}

class SettingsManager(context: Context) {

    private val prefs: SharedPreferences = context.getSharedPreferences("spotifast_settings", Context.MODE_PRIVATE)

    // Calidad de audio
    private val _streamingBitrate = MutableStateFlow(prefs.getInt("streaming_bitrate", 320))
    val streamingBitrate: StateFlow<Int> = _streamingBitrate.asStateFlow()

    // Calidad de descarga
    private val _downloadBitrate = MutableStateFlow(prefs.getInt("download_bitrate", 320))
    val downloadBitrate: StateFlow<Int> = _downloadBitrate.asStateFlow()

    // Reproducción
    private val _gaplessPlayback = MutableStateFlow(prefs.getBoolean("gapless_playback", true))
    val gaplessPlayback: StateFlow<Boolean> = _gaplessPlayback.asStateFlow()

    private val _normalizeVolume = MutableStateFlow(prefs.getBoolean("normalize_volume", true))
    val normalizeVolume: StateFlow<Boolean> = _normalizeVolume.asStateFlow()

    private val _crossfadeSeconds = MutableStateFlow(prefs.getInt("crossfade_sec", 0))
    val crossfadeSeconds: StateFlow<Int> = _crossfadeSeconds.asStateFlow()

    private val _monoAudio = MutableStateFlow(prefs.getBoolean("mono_audio", false))
    val monoAudio: StateFlow<Boolean> = _monoAudio.asStateFlow()

    // Modos de Spotify
    private val _offlineMode = MutableStateFlow(prefs.getBoolean("offline_mode", false))
    val offlineMode: StateFlow<Boolean> = _offlineMode.asStateFlow()

    private val _privateSession = MutableStateFlow(prefs.getBoolean("private_session", false))
    val privateSession: StateFlow<Boolean> = _privateSession.asStateFlow()

    private val _dataSaver = MutableStateFlow(prefs.getBoolean("data_saver", false))
    val dataSaver: StateFlow<Boolean> = _dataSaver.asStateFlow()

    // Video y Canvas
    private val _videoCanvasEnabled = MutableStateFlow(prefs.getBoolean("video_canvas", true))
    val videoCanvasEnabled: StateFlow<Boolean> = _videoCanvasEnabled.asStateFlow()

    // Visualizador y MilkDrop
    private val _visualizerType = MutableStateFlow(
        VisualizerType.valueOf(prefs.getString("visualizer_type", VisualizerType.SPECTRUM_BARS.name) ?: VisualizerType.SPECTRUM_BARS.name)
    )
    val visualizerType: StateFlow<VisualizerType> = _visualizerType.asStateFlow()

    // Tema y apariencia
    private val _themePreset = MutableStateFlow(
        ThemePreset.valueOf(prefs.getString("theme_preset", ThemePreset.SPOTIFY_DARK.name) ?: ThemePreset.SPOTIFY_DARK.name)
    )
    val themePreset: StateFlow<ThemePreset> = _themePreset.asStateFlow()

    private val _accentFromArt = MutableStateFlow(prefs.getBoolean("accent_from_art", true))
    val accentFromArt: StateFlow<Boolean> = _accentFromArt.asStateFlow()

    // Proxy
    private val _proxyType = MutableStateFlow(
        ProxyType.valueOf(prefs.getString("proxy_type", ProxyType.DIRECT.name) ?: ProxyType.DIRECT.name)
    )
    val proxyType: StateFlow<ProxyType> = _proxyType.asStateFlow()

    private val _proxyHost = MutableStateFlow(prefs.getString("proxy_host", "") ?: "")
    val proxyHost: StateFlow<String> = _proxyHost.asStateFlow()

    private val _proxyPort = MutableStateFlow(prefs.getString("proxy_port", "8080") ?: "8080")
    val proxyPort: StateFlow<String> = _proxyPort.asStateFlow()

    // Notificaciones
    private val _notificationsEnabled = MutableStateFlow(prefs.getBoolean("notifications_enabled", true))
    val notificationsEnabled: StateFlow<Boolean> = _notificationsEnabled.asStateFlow()

    fun setStreamingBitrate(kbps: Int) {
        _streamingBitrate.value = kbps
        prefs.edit().putInt("streaming_bitrate", kbps).apply()
    }

    fun setDownloadBitrate(kbps: Int) {
        _downloadBitrate.value = kbps
        prefs.edit().putInt("download_bitrate", kbps).apply()
    }

    fun setGaplessPlayback(enabled: Boolean) {
        _gaplessPlayback.value = enabled
        prefs.edit().putBoolean("gapless_playback", enabled).apply()
    }

    fun setNormalizeVolume(enabled: Boolean) {
        _normalizeVolume.value = enabled
        prefs.edit().putBoolean("normalize_volume", enabled).apply()
    }

    fun setCrossfadeSeconds(sec: Int) {
        _crossfadeSeconds.value = sec
        prefs.edit().putInt("crossfade_sec", sec).apply()
    }

    fun setMonoAudio(enabled: Boolean) {
        _monoAudio.value = enabled
        prefs.edit().putBoolean("mono_audio", enabled).apply()
    }

    fun setOfflineMode(enabled: Boolean) {
        _offlineMode.value = enabled
        prefs.edit().putBoolean("offline_mode", enabled).apply()
    }

    fun setPrivateSession(enabled: Boolean) {
        _privateSession.value = enabled
        prefs.edit().putBoolean("private_session", enabled).apply()
    }

    fun setDataSaver(enabled: Boolean) {
        _dataSaver.value = enabled
        prefs.edit().putBoolean("data_saver", enabled).apply()
    }

    fun setVideoCanvasEnabled(enabled: Boolean) {
        _videoCanvasEnabled.value = enabled
        prefs.edit().putBoolean("video_canvas", enabled).apply()
    }

    fun setVisualizerType(type: VisualizerType) {
        _visualizerType.value = type
        prefs.edit().putString("visualizer_type", type.name).apply()
    }

    fun setThemePreset(preset: ThemePreset) {
        _themePreset.value = preset
        prefs.edit().putString("theme_preset", preset.name).apply()
    }

    fun setAccentFromArt(enabled: Boolean) {
        _accentFromArt.value = enabled
        prefs.edit().putBoolean("accent_from_art", enabled).apply()
    }

    fun setProxyConfig(type: ProxyType, host: String, port: String) {
        _proxyType.value = type
        _proxyHost.value = host
        _proxyPort.value = port
        prefs.edit()
            .putString("proxy_type", type.name)
            .putString("proxy_host", host)
            .putString("proxy_port", port)
            .apply()
    }

    fun setNotificationsEnabled(enabled: Boolean) {
        _notificationsEnabled.value = enabled
        prefs.edit().putBoolean("notifications_enabled", enabled).apply()
    }
}
