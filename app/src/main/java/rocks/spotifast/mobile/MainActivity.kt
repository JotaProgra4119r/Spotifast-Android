package rocks.spotifast.mobile

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import rocks.spotifast.mobile.data.api.SpotifyClient
import rocks.spotifast.mobile.data.api.models.Playlist
import rocks.spotifast.mobile.data.api.models.UserProfile
import rocks.spotifast.mobile.ui.components.*
import rocks.spotifast.mobile.ui.screens.*
import rocks.spotifast.mobile.ui.theme.*

class MainActivity : ComponentActivity() {

    private var isWaitingForBrowserState = mutableStateOf(false)
    private var currentAuthUrlState = mutableStateOf<String?>(null)

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val audioGranted = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissions[Manifest.permission.READ_MEDIA_AUDIO] == true
        } else {
            permissions[Manifest.permission.READ_EXTERNAL_STORAGE] == true
        }
        if (audioGranted) {
            lifecycleScope.launch(Dispatchers.IO) {
                SpotifastApp.instance.localMediaManager.scanLocalTracks()
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleAuthRedirect(intent)
        requestRequiredPermissions()

        setContent {
            SpotifastTheme {
                val authManager = SpotifastApp.instance.authManager
                val isLoggedIn by authManager.isLoggedIn.collectAsState()
                val isWaiting by isWaitingForBrowserState
                val authUrl by currentAuthUrlState

                if (!isLoggedIn) {
                    LoginScreen(
                        onStartLogin = { clientId, redirectUri ->
                            launchSpotifyLogin(clientId, redirectUri)
                        },
                        onCancelLogin = {
                            authManager.cancelAuthFlow()
                            isWaitingForBrowserState.value = false
                            currentAuthUrlState.value = null
                        },
                        isWaitingForBrowser = isWaiting,
                        authUrl = authUrl
                    )
                } else {
                    MainAppResponsiveContainer(
                        onLogoutRequested = {
                            authManager.clearSession()
                            Toast.makeText(this@MainActivity, "Sesión cerrada correctamente", Toast.LENGTH_SHORT).show()
                        }
                    )
                }
            }
        }
    }

    private fun requestRequiredPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_MEDIA_AUDIO)
            }
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.READ_EXTERNAL_STORAGE)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        } else {
            lifecycleScope.launch(Dispatchers.IO) {
                SpotifastApp.instance.localMediaManager.scanLocalTracks()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        handleAuthRedirect(intent)
    }

    private fun launchSpotifyLogin(clientId: String?, redirectUri: String) {
        val authManager = SpotifastApp.instance.authManager
        isWaitingForBrowserState.value = true

        val url = authManager.startAuthFlow(
            customClientId = clientId,
            redirectUri = redirectUri
        ) { code, error ->
            lifecycleScope.launch(Dispatchers.Main) {
                if (!error.isNullOrEmpty()) {
                    isWaitingForBrowserState.value = false
                    Toast.makeText(this@MainActivity, "Error de Spotify: $error", Toast.LENGTH_LONG).show()
                    return@launch
                }

                if (!code.isNullOrEmpty()) {
                    processAuthCode(code)
                }
            }
        }

        currentAuthUrlState.value = url

        try {
            val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                addCategory(Intent.CATEGORY_BROWSABLE)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            startActivity(browserIntent)
        } catch (e: Exception) {
            Toast.makeText(this, "No se pudo abrir el navegador: ${e.localizedMessage}", Toast.LENGTH_LONG).show()
        }
    }

    private fun handleAuthRedirect(intent: Intent?) {
        val uri = intent?.data ?: return
        if (uri.scheme == "spotifast" && uri.host == "callback") {
            val code = uri.getQueryParameter("code")
            val error = uri.getQueryParameter("error")

            if (!error.isNullOrEmpty()) {
                isWaitingForBrowserState.value = false
                Toast.makeText(this, "Error de autorización: $error", Toast.LENGTH_LONG).show()
                return
            }

            if (!code.isNullOrEmpty()) {
                processAuthCode(code)
            }
        }
    }

    private fun processAuthCode(code: String) {
        lifecycleScope.launch {
            val authManager = SpotifastApp.instance.authManager
            val success = authManager.exchangeCodeForToken(code)
            isWaitingForBrowserState.value = false
            currentAuthUrlState.value = null

            if (success) {
                Toast.makeText(this@MainActivity, "¡Sesión iniciada con éxito!", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this@MainActivity, "Fallo al intercambiar credenciales", Toast.LENGTH_LONG).show()
            }
        }
    }
}

enum class NavigationScreen {
    HOME,
    SEARCH,
    LIBRARY,
    SETTINGS,
    PLAYLIST_DETAIL,
    LIKED_SONGS
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppResponsiveContainer(
    onLogoutRequested: () -> Unit
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isWideScreen = configuration.screenWidthDp >= 600 || isLandscape

    var currentScreen by remember { mutableStateOf(NavigationScreen.HOME) }
    var selectedPlaylist by remember { mutableStateOf<Playlist?>(null) }
    var userProfile by remember { mutableStateOf<UserProfile?>(null) }

    LaunchedEffect(Unit) {
        try {
            val response = SpotifyClient.apiService.getMe()
            if (response.isSuccessful) {
                userProfile = response.body()
            }
        } catch (_: Exception) {}
    }

    if (isWideScreen) {
        // Modo horizontal / Tableta: Shell 1:1 de escritorio Spotifast
        DesktopTabletShell(
            userProfile = userProfile,
            currentScreen = currentScreen,
            selectedPlaylist = selectedPlaylist,
            onNavigate = { currentScreen = it },
            onPlaylistSelected = {
                selectedPlaylist = it
                currentScreen = NavigationScreen.PLAYLIST_DETAIL
            },
            onLogoutRequested = onLogoutRequested
        )
    } else {
        // Modo vertical / Móvil: Shell con navegación inferior y drawer
        MainAppScaffold(
            currentScreen = currentScreen,
            onScreenChange = { currentScreen = it },
            selectedPlaylist = selectedPlaylist,
            onPlaylistChange = { selectedPlaylist = it },
            userProfile = userProfile,
            onLogoutRequested = onLogoutRequested
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainAppScaffold(
    currentScreen: NavigationScreen,
    onScreenChange: (NavigationScreen) -> Unit,
    selectedPlaylist: Playlist?,
    onPlaylistChange: (Playlist?) -> Unit,
    userProfile: UserProfile?,
    onLogoutRequested: () -> Unit
) {
    val playbackManager = SpotifastApp.instance.playbackManager

    val currentTrack by playbackManager.currentTrack.collectAsState()
    val isPlaying by playbackManager.isPlaying.collectAsState()
    val progressMs by playbackManager.progressMs.collectAsState()
    val durationMs by playbackManager.durationMs.collectAsState()
    val connectedDevice by playbackManager.connectedDeviceName.collectAsState()
    val isShuffle by playbackManager.isShuffle.collectAsState()
    val repeatMode by playbackManager.repeatMode.collectAsState()

    var isFullscreenPlayerOpen by remember { mutableStateOf(false) }
    var showDevicesSheet by remember { mutableStateOf(false) }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()

    // Manejo de botón ATRÁS
    BackHandler(enabled = isFullscreenPlayerOpen) {
        isFullscreenPlayerOpen = false
    }
    BackHandler(enabled = !isFullscreenPlayerOpen && drawerState.isOpen) {
        scope.launch { drawerState.close() }
    }
    BackHandler(enabled = !isFullscreenPlayerOpen && !drawerState.isOpen && currentScreen != NavigationScreen.HOME) {
        onScreenChange(NavigationScreen.HOME)
    }

    if (showDevicesSheet) {
        DevicesSheet(onDismiss = { showDevicesSheet = false })
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ProfileDrawer(
                userProfile = userProfile,
                onSettingsClick = {
                    scope.launch { drawerState.close() }
                    onScreenChange(NavigationScreen.SETTINGS)
                },
                onClose = { scope.launch { drawerState.close() } }
            )
        }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(SpotifyBlack)
        ) {
            Scaffold(
                bottomBar = {
                    Column {
                        // Mini reproductor flotante
                        if (currentTrack != null && !isFullscreenPlayerOpen) {
                            val progressFraction = if (durationMs > 0) progressMs.toFloat() / durationMs.toFloat() else 0f
                            MiniPlayer(
                                track = currentTrack,
                                isPlaying = isPlaying,
                                progressFraction = progressFraction,
                                connectedDevice = connectedDevice,
                                onPlayPauseClick = { playbackManager.togglePlayPause() },
                                onClick = { isFullscreenPlayerOpen = true },
                                onDevicesClick = { showDevicesSheet = true }
                            )
                        }

                        // Barra de navegación inferior
                        val navScreen = when (currentScreen) {
                            NavigationScreen.SEARCH -> Screen.SEARCH
                            NavigationScreen.LIBRARY -> Screen.LIBRARY
                            else -> Screen.HOME
                        }
                        BottomNavBar(
                            currentScreen = navScreen,
                            onScreenSelected = { screen ->
                                val target = when (screen) {
                                    Screen.SEARCH -> NavigationScreen.SEARCH
                                    Screen.LIBRARY -> NavigationScreen.LIBRARY
                                    else -> NavigationScreen.HOME
                                }
                                onScreenChange(target)
                            }
                        )
                    }
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentScreen) {
                        NavigationScreen.HOME -> {
                            HomeScreen(
                                userProfile = userProfile,
                                onProfileClick = { scope.launch { drawerState.open() } },
                                onLikedSongsClick = { onScreenChange(NavigationScreen.LIKED_SONGS) },
                                onPlaylistClick = { playlist ->
                                    onPlaylistChange(playlist)
                                    onScreenChange(NavigationScreen.PLAYLIST_DETAIL)
                                },
                                onTrackClick = { track -> playbackManager.playTrack(track) }
                            )
                        }
                        NavigationScreen.SEARCH -> {
                            SearchScreen(
                                userProfile = userProfile,
                                onProfileClick = { scope.launch { drawerState.open() } },
                                onTrackClick = { track, list -> playbackManager.playTrack(track, list) }
                            )
                        }
                        NavigationScreen.LIBRARY -> {
                            LibraryScreen(
                                onBackClick = { onScreenChange(NavigationScreen.HOME) },
                                onPlaylistClick = { playlist ->
                                    onPlaylistChange(playlist)
                                    onScreenChange(NavigationScreen.PLAYLIST_DETAIL)
                                },
                                onTrackClick = { track, list -> playbackManager.playTrack(track, list) }
                            )
                        }
                        NavigationScreen.PLAYLIST_DETAIL -> {
                            selectedPlaylist?.let { pl ->
                                PlaylistDetailScreen(
                                    playlist = pl,
                                    onBackClick = { onScreenChange(NavigationScreen.HOME) },
                                    onTrackClick = { track, list -> playbackManager.playTrack(track, list) },
                                    onPlayAllClick = { plItem, tracks ->
                                        playbackManager.playPlaylistContext(plItem.uri, tracksFallback = tracks)
                                    }
                                )
                            } ?: run {
                                onScreenChange(NavigationScreen.HOME)
                            }
                        }
                        NavigationScreen.LIKED_SONGS -> {
                            LikedSongsScreen(
                                userProfile = userProfile,
                                onBackClick = { onScreenChange(NavigationScreen.HOME) },
                                onTrackClick = { track, list -> playbackManager.playTrack(track, list) },
                                onPlayAllClick = { list ->
                                    if (list.isNotEmpty()) playbackManager.playTrack(list.first(), list)
                                }
                            )
                        }
                        NavigationScreen.SETTINGS -> {
                            SettingsScreen(
                                userProfile = userProfile,
                                onBackClick = { onScreenChange(NavigationScreen.HOME) },
                                onLogoutClick = { onLogoutRequested() }
                            )
                        }
                    }
                }
            }

            // Reproductor completo animado
            AnimatedVisibility(
                visible = isFullscreenPlayerOpen,
                enter = slideInVertically(initialOffsetY = { it }),
                exit = slideOutVertically(targetOffsetY = { it })
            ) {
                FullscreenPlayer(
                    track = currentTrack,
                    isPlaying = isPlaying,
                    progressMs = progressMs,
                    durationMs = durationMs,
                    connectedDevice = connectedDevice,
                    isShuffle = isShuffle,
                    repeatMode = repeatMode,
                    onDismiss = { isFullscreenPlayerOpen = false },
                    onPlayPause = { playbackManager.togglePlayPause() },
                    onNext = { playbackManager.next() },
                    onPrevious = { playbackManager.previous() },
                    onSeek = { playbackManager.seekTo(it) },
                    onToggleShuffle = { playbackManager.toggleShuffle() },
                    onCycleRepeat = { playbackManager.cycleRepeat() },
                    onDevicesClick = { showDevicesSheet = true }
                )
            }
        }
    }
}
